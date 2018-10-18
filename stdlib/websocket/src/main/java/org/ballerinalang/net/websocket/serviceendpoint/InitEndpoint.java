/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websocket.serviceendpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.websocket.HttpConnectionManager;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketServicesRegistry;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;

/**
 * Get the ID of the connection.
 *
 * @since 0.966
 */

@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "initEndpoint",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Listener",
                             structPackage = "ballerina/http"),
        isPublic = true
)
public class InitEndpoint extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        try {
            Struct serviceEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);

            // Creating server connector
            Struct serviceEndpointConfig = serviceEndpoint.getStructField(WebSocketConstants.SERVICE_ENDPOINT_CONFIG);
            ListenerConfiguration listenerConfiguration = WebSocketUtil.getListenerConfig(serviceEndpointConfig);
            ServerConnector httpServerConnector =
                    HttpConnectionManager.getInstance().createHttpServerConnector(listenerConfiguration);
            serviceEndpoint.addNativeData(WebSocketConstants.HTTP_SERVER_CONNECTOR, httpServerConnector);

            //Adding service registries to native data
            WebSocketServicesRegistry webSocketServicesRegistry = new WebSocketServicesRegistry();
            serviceEndpoint.addNativeData(WebSocketConstants.WEBSOCKET_SERVICE_REGISTRY, webSocketServicesRegistry);


            context.setReturnValues((BValue) null);
        } catch (Exception e) {
            BMap<String, BValue> errorStruct = WebSocketUtil.getError(context, e);
            context.setReturnValues(errorStruct);
        }

    }
}
