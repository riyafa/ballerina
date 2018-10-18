/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.net.websocket.actions.connector;

import io.netty.channel.ChannelFuture;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BByteArray;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketOpenConnectionInfo;
import org.ballerinalang.net.websocket.WebSocketUtil;

import java.nio.ByteBuffer;

/**
 * {@code PushBinary} is the PushBinary action implementation of the WebSocket Connector.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "websocket",
        functionName = "pushBinary",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = WebSocketConstants.WEBSOCKET_CONNECTOR,
                             structPackage = WebSocketConstants.WEBSOCKET_PACKAGE),
        args = {
                @Argument(name = "wsConnector", type = TypeKind.OBJECT),
                @Argument(name = "data", type = TypeKind.ARRAY, elementType = TypeKind.BYTE),
                @Argument(name = "final", type = TypeKind.BOOLEAN)
        }
)
public class PushBinary implements NativeCallableUnit {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
        try {
            BMap<String, BValue> wsConnection = (BMap<String, BValue>) context.getRefArgument(0);
            WebSocketOpenConnectionInfo connectionInfo = (WebSocketOpenConnectionInfo) wsConnection
                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
            byte[] binaryData = ((BByteArray) context.getRefArgument(1)).getBytes();
            boolean finalFrame = context.getBooleanArgument(0);
            ChannelFuture webSocketChannelFuture =
                    connectionInfo.getWebSocketConnection().pushBinary(ByteBuffer.wrap(binaryData), finalFrame);
            WebSocketUtil.handleWebSocketCallback(context, callback, webSocketChannelFuture);
        } catch (Exception e) {
            context.setReturnValues(WebSocketUtil.getError(context, e));
            callback.notifySuccess();
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
