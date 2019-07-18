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

package org.ballerinalang.net.jms.externimpl.common;

import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;
import org.ballerinalang.net.jms.utils.JmsUtils;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

/**
 * Message send action handler.
 */
public class SendToActionHandler {

    private SendToActionHandler() {
    }

    public static Object handle(ObjectValue sender, ObjectValue destObj, ObjectValue msgObj,
                                MapValue<String, Object> config) {

        MessageProducer messageProducer = (MessageProducer) sender.getNativeData(JmsConstants.JMS_PRODUCER_OBJECT);
//        SessionConnector sessionConnector =
//                (SessionConnector) sender.getNativeData(JmsConstants.SESSION_CONNECTOR_OBJECT);
        Message message = (Message) msgObj.getNativeData(JmsConstants.JMS_MESSAGE_OBJECT);
        Destination destination = JmsUtils.getDestination(destObj);
        try {
//            sessionConnector.handleTransactionBlock(context);
            messageProducer.send(destination, message, JmsUtils.getDeliveryMode(config),
                                 Math.toIntExact(config.getIntValue(JmsConstants.PRIORITY)),
                                 config.getIntValue(JmsConstants.TIME_TO_LIVE));


        } catch (JMSException e) {
            return BallerinaAdapter.getError("Message sending failed.", e);
        }
        return null;
    }
}
