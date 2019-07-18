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

package org.ballerinalang.net.jms.utils;

import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.jms.JmsConstants;

import java.util.Arrays;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Utility class for JMS related common operations.
 */
public class JmsUtils {

    /**
     * Utility class cannot be instantiated.
     */
    private JmsUtils() {
    }

    public static boolean isNullOrEmptyAfterTrim(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Extract JMS Message from the struct.
     *
     * @param msgObj the Bllerina Message object
     * @return {@link Message} instance located in struct.
     */
    public static Message getJMSMessage(ObjectValue msgObj) {
        return (Message) msgObj.getNativeData(JmsConstants.JMS_MESSAGE_OBJECT);
    }

    public static Topic getTopic(Session session, String topicPattern) throws JMSException {
        return session.createTopic(topicPattern);
    }

    /**
     * Extract JMS Destination from the Destination struct.
     *
     * @param destinationBObject Destination struct.
     * @return JMS Destination object or null.
     */
    public static Destination getDestination(ObjectValue destinationBObject) {
        return (Destination) destinationBObject.getNativeData(JmsConstants.JMS_DESTINATION_OBJECT);
    }

    public static byte[] getBytesData(ArrayValue bytesArray) {
        return Arrays.copyOf(bytesArray.getBytes(), bytesArray.size());
    }

    public static ObjectValue populateAndGetDestinationObj(Destination destination) throws JMSException {
        ObjectValue destObj;
        if (destination instanceof Queue) {
            destObj = BallerinaValues.createObjectValue(JmsConstants.PROTOCOL_INTERNAL_PACKAGE_JMS,
                                                        JmsConstants.DESTINATION_OBJ_NAME,
                                                        ((Queue) destination).getQueueName(),
                                                        JmsConstants.DESTINATION_TYPE_QUEUE);
        } else {
            destObj = BallerinaValues.createObjectValue(JmsConstants.PROTOCOL_INTERNAL_PACKAGE_JMS,
                                                        JmsConstants.DESTINATION_OBJ_NAME,
                                                        ((Topic) destination).getTopicName(),
                                                        JmsConstants.DESTINATION_TYPE_TOPIC);
        }
        destObj.addNativeData(JmsConstants.JMS_DESTINATION_OBJECT, destination);
        return destObj;
    }

    public static ObjectValue createAndPopulateMessageObject(Message jmsMessage, ObjectValue sessionObj) {
        String msgType;
        if (jmsMessage instanceof TextMessage) {
            msgType = JmsConstants.TEXT_MESSAGE;
        } else if (jmsMessage instanceof BytesMessage) {
            msgType = JmsConstants.BYTES_MESSAGE;
        } else if (jmsMessage instanceof StreamMessage) {
            msgType = JmsConstants.STREAM_MESSAGE;
        } else if (jmsMessage instanceof MapMessage) {
            msgType = JmsConstants.MAP_MESSAGE;
        } else {
            msgType = JmsConstants.MESSAGE;
        }
        ObjectValue messageObj = BallerinaValues.createObjectValue(JmsConstants.PROTOCOL_INTERNAL_PACKAGE_JMS,
                                                                   JmsConstants.MESSAGE_OBJ_NAME, sessionObj, msgType);
        messageObj.addNativeData(JmsConstants.JMS_MESSAGE_OBJECT, jmsMessage);
        return messageObj;
    }

    public static Session getSession(ObjectValue obj) {
        return (Session) obj.getObjectValue(JmsConstants.SESSION_FIELD_NAME).getNativeData(JmsConstants.JMS_SESSION);
    }

    @SuppressWarnings(JmsConstants.UNCHECKED)
    public static MapValue<String, Object> getConfig(ObjectValue obj) {
        return obj.getMapValue(JmsConstants.CONFIG_FIELD_NAME);
    }

    public static int getDeliveryMode(MapValue<String, Object> config) {
        String deliveryMode = config.getStringValue("deliveryMode");
        if (deliveryMode.equals(JmsConstants.PERSISTENT)) {
            return DeliveryMode.PERSISTENT;
        } else {
            return DeliveryMode.NON_PERSISTENT;
        }
    }
}
