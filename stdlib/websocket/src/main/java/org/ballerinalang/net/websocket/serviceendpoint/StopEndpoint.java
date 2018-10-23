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
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.util.BLangConstants;
import org.wso2.transport.http.netty.contract.ServerConnector;

/**
 * Stop the listener.
 *
 * @since 0.966
 */

@BallerinaFunction(
        orgName = "ballerina", packageName = WebSocketConstants.WEBSOCKET,
        functionName = "stopEndpoint",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = WebSocketConstants.WEBSOCKET_LISTENER,
                             structPackage = WebSocketConstants.WEBSOCKET_PACKAGE),
        isPublic = true
)
public class StopEndpoint extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        Struct serverEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        ServerConnector serverConnector = (ServerConnector) serverEndpoint.getNativeData(
                WebSocketConstants.HTTP_SERVER_CONNECTOR);
        serverConnector.stop();
        serverEndpoint.addNativeData(WebSocketConstants.CONNECTOR_STARTED, false);
        serverEndpoint.addNativeData(WebSocketConstants.WEBSOCKET_SERVICE_REGISTRY, null);
        context.setReturnValues();
    }
}
