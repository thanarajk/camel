/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.rabbitmq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.Connection;

import org.apache.camel.Processor;
import org.apache.camel.Suspendable;
import org.apache.camel.impl.DefaultConsumer;

public class RabbitMQConsumer extends DefaultConsumer implements Suspendable {
    private ExecutorService executor;
    private Connection conn;
    private int closeTimeout = 30 * 1000;
    private final RabbitMQEndpoint endpoint;

    /**
     * Task in charge of starting consumer
     */
    private StartConsumerCallable startConsumerCallable;

    /**
     * Running consumers
     */
    private final List<RabbitConsumer> consumers = new ArrayList<RabbitConsumer>();

    public RabbitMQConsumer(RabbitMQEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    public RabbitMQEndpoint getEndpoint() {
        return (RabbitMQEndpoint)super.getEndpoint();
    }

    /**
     * Open connection
     */
    private void openConnection() throws IOException, TimeoutException {
        log.trace("Creating connection...");
        this.conn = getEndpoint().connect(executor);
        log.debug("Created connection: {}", conn);
    }

    /**
     * Returns the exiting open connection or opens a new one
     * @throws IOException
     * @throws TimeoutException
     */
    protected synchronized Connection getConnection() throws IOException, TimeoutException {
        if (this.conn != null && this.conn.isOpen()) {
            return this.conn;
        }
        log.debug("The existing connection is closed");
        openConnection();
        return this.conn;
    }


    /**
     * Add a consumer thread for given channel
     */
    private void startConsumers() throws IOException {

        // Create consumers but don't start yet
        for (int i = 0; i < endpoint.getConcurrentConsumers(); i++) {
            createConsumer();
        }

        // Try starting consumers (which will fail if RabbitMQ can't connect)
        try {
            for (RabbitConsumer consumer : this.consumers) {
                consumer.start();
            }
        } catch (Exception e) {
            log.info("Connection failed, will start background thread to retry!", e);
            reconnect();
        }
    }

    /**
     * Add a consumer thread for given channel
     */
    private void createConsumer() throws IOException {
        RabbitConsumer consumer = new RabbitConsumer(this);
        this.consumers.add(consumer);
    }

    private synchronized void reconnect() {
        if (startConsumerCallable != null) {
            return;
        }
        // Open connection, and start message listener in background
        Integer networkRecoveryInterval = getEndpoint().getNetworkRecoveryInterval();
        final long connectionRetryInterval = networkRecoveryInterval != null && networkRecoveryInterval > 0 ? networkRecoveryInterval : 100L;
        startConsumerCallable = new StartConsumerCallable(connectionRetryInterval);
        executor.submit(startConsumerCallable);
    }

    /**
     * If needed, close Connection and Channels
     */
    private void closeConnectionAndChannel() throws IOException, TimeoutException {
        if (startConsumerCallable != null) {
            startConsumerCallable.stop();
        }
        for (RabbitConsumer consumer : this.consumers) {
            try {
                consumer.stop();
            } catch (TimeoutException e) {
                log.error("Timeout occured");
                throw e;
            }
        }
        this.consumers.clear();
        if (conn != null) {
            log.debug("Closing connection: {} with timeout: {} ms.", conn, closeTimeout);
            conn.close(closeTimeout);
            conn = null;
        }
    }

    @Override
    protected void doSuspend() throws Exception {
        closeConnectionAndChannel();
    }

    @Override
    protected void doResume() throws Exception {
        reconnect();
    }

    @Override
    protected void doStart() throws Exception {
        executor = endpoint.createExecutor();
        log.debug("Using executor {}", executor);
        startConsumers();
    }

    @Override
    protected void doStop() throws Exception {
        closeConnectionAndChannel();

        if (executor != null) {
            if (endpoint != null && endpoint.getCamelContext() != null) {
                endpoint.getCamelContext().getExecutorServiceManager().shutdownNow(executor);
            } else {
                executor.shutdownNow();
            }
            executor = null;
        }
    }



    /**
     * Task in charge of opening connection and adding listener when consumer is
     * started and broker is not available.
     */
    private class StartConsumerCallable implements Callable<Void> {
        private final long connectionRetryInterval;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public StartConsumerCallable(long connectionRetryInterval) {
            this.connectionRetryInterval = connectionRetryInterval;
        }

        public void stop() {
            running.set(false);
            RabbitMQConsumer.this.startConsumerCallable = null;
        }

        @Override
        public Void call() throws Exception {
            boolean connectionFailed = true;
            // Reconnection loop
            while (running.get() && connectionFailed) {
                try {
                    for (RabbitConsumer consumer : consumers) {
                        consumer.reconnect();
                    }
                    connectionFailed = false;
                } catch (Exception e) {
                    log.info("Connection failed, will retry in " + connectionRetryInterval + "ms", e);
                    Thread.sleep(connectionRetryInterval);
                }
            }
            if (!connectionFailed) {
                startConsumers();
            }
            stop();
            return null;
        }
    }
}
