// Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


///////////////////////////
/// Service Annotations ///
///////////////////////////

# Configurations for a WebSocket service.
#
# + endpoints - An array of endpoints the service would be attached to
# + webSocketEndpoints - An array of endpoints the service would be attached to
# + path - Path of the WebSocket service
# + subProtocols - Negotiable sub protocol by the service
# + idleTimeoutInSeconds - Idle timeout for the client connection. This can be triggered by putting
#                          an `onIdleTimeout` resource in the WebSocket service.
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes
public type WSServiceConfig record {
    Listener[] endpoints;
    WebSocketListener[] webSocketEndpoints;
    string path;
    string[] subProtocols;
    int idleTimeoutInSeconds;
    int maxFrameSize;
    !...
};

# The annotation which is used to configure a WebSocket service.
public annotation <service> ServiceConfig WSServiceConfig;
