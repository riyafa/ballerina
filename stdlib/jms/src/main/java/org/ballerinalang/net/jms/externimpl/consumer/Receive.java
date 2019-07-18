/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.ballerinalang.net.jms.externimpl.consumer;

import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;
import org.ballerinalang.net.jms.utils.JmsUtils;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;

/**
 * {@code Send} is the send action implementation of the JMS Connector.
 */
@BallerinaFunction(orgName = JmsConstants.BALLERINAX,
                   packageName = JmsConstants.JAVA_JMS,
                   functionName = "receive",
                   receiver = @Receiver(type = TypeKind.OBJECT,
                                        structType = JmsConstants.CONSUMER_OBJ_NAME,
                                        structPackage = JmsConstants.PROTOCOL_PACKAGE_JMS)
)
public class Receive {

    public static Object receive(Strand strand, ObjectValue consumerObj, long timeoutInMilliSeconds) {
        MessageConsumer messageConsumer =
                (MessageConsumer) consumerObj.getNativeData(JmsConstants.JMS_CONSUMER_OBJECT);
//        SessionConnector sessionConnector =
//                (SessionConnector) connectorBObject.getNativeData(JmsConstants.SESSION_CONNECTOR_OBJECT);

        try {
//            sessionConnector.handleTransactionBlock(context);
            Message message = messageConsumer.receive(timeoutInMilliSeconds);
            if (message != null) {
                return JmsUtils.createAndPopulateMessageObject(message, (ObjectValue) consumerObj
                        .getNativeData(JmsConstants.SESSION_OBJECT));
            }
        } catch (JMSException e) {
            return BallerinaAdapter.getError("Message receiving failed.", e);
        }
        return null;
    }

    private Receive() {
    }
}
