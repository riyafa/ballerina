// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;

//////////////////////////////////
/// WebSocket Service Endpoint ///
//////////////////////////////////
# Represents a WebSocket service endpoint.
#
# + id - The connection ID
# + negotiatedSubProtocol - The subprotocols negotiated with the client
# + isSecure - `true` if the connection is secure
# + isOpen - `true` if the connection is open
# + attributes - A `map` to store connection related attributes
public type Listener object {

    @readonly public string id;
    @readonly public string negotiatedSubProtocol;
    @readonly public boolean isSecure;
    @readonly public boolean isOpen;
    @readonly public map attributes;

    private Connector conn;
    private http:ServiceEndpointConfiguration config;
    private http:Listener httpEndpoint;

    public new() {
    }

    # Gets invoked during package initialization to initialize the endpoint.
    #
    # + c - The `ServiceEndpointConfiguration` of the endpoint
    public function init(ServiceEndpointConfiguration c) {
        self.config = c;
        httpEndpoint.init(c);
    }

    # Gets invoked when binding a service to the endpoint.
    #
    # + serviceType - The service type
    public function register(typedesc serviceType) {
        httpEndpoint.register(serviceType);
    }

    # Starts the registered service.
    public function start() {
        httpEndpoint.start();
    }

    # Returns a WebSocket actions provider which can be used to communicate with the remote host.
    #
    # + return - The connector that listener endpoint uses
    public function getCallerActions() returns (Connector) {
        return conn;
    }

    # Stops the registered service.
    public function stop() {
        Connector webSocketConnector = getCallerActions();
        check webSocketConnector.close(statusCode = 1001, reason = "going away", timeoutInSecs = 0);
        httpEndpoint.stop();
    }
};
