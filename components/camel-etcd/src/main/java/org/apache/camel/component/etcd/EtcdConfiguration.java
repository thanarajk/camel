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
package org.apache.camel.component.etcd;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.util.jsse.SSLContextParameters;

@UriParams
public class EtcdConfiguration {

    @UriParam(defaultValue = EtcdConstants.ETCD_DEFAULT_URIS)
    private String uris = EtcdConstants.ETCD_DEFAULT_URIS;
    @UriParam(label = "security")
    private SSLContextParameters sslContextParameters;
    @UriParam(label = "security")
    private String userName;
    @UriParam(label = "security")
    private String password;
    @UriParam(label = "consumer")
    private boolean sendEmptyExchangeOnTimeout;
    @UriParam
    private boolean recursive;
    @UriParam(label = "producer")
    private Integer timeToLive;
    @UriParam
    private Long timeout;

    public String getUris() {
        return uris;
    }

    /**
     * To set the URIs the client connects.
     */
    public void setUris(String uris) {
        this.uris = uris;
    }

    public SSLContextParameters getSslContextParameters() {
        return sslContextParameters;
    }

    /**
     * To configure security using SSLContextParameters.
     */
    public void setSslContextParameters(SSLContextParameters sslContextParameters) {
        this.sslContextParameters = sslContextParameters;
    }

    public String getUserName() {
        return userName;
    }

    /**
     * The user name to use for basic authentication.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    /**
     * The password to use for basic authentication.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSendEmptyExchangeOnTimeout() {
        return sendEmptyExchangeOnTimeout;
    }

    /**
     * To send an empty message in case of timeout watching for a key.
     */
    public void setSendEmptyExchangeOnTimeout(boolean sendEmptyExchangeOnTimeout) {
        this.sendEmptyExchangeOnTimeout = sendEmptyExchangeOnTimeout;
    }

    public boolean isRecursive() {
        return recursive;
    }

    /**
     * To apply an action recursively.
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    /**
     * To set the lifespan of a key in milliseconds.
     */
    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Long getTimeout() {
        return timeout;
    }

    /**
     * To set the maximum time an action could take to complete.
     */
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

}
