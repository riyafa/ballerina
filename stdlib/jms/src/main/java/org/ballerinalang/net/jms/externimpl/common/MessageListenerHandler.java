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

import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.JmsMessageListenerImpl;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;

import java.util.function.Consumer;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

/**
 * Util class to create and register a message listener.
 *
 * @since 0.970
 */
public class MessageListenerHandler {

    private MessageListenerHandler() {
    }

    public static Object createListener(Strand strand, ObjectValue listenerObj, ObjectValue service,
                                        ObjectValue consumerObj) {
        Consumer consumer = (Consumer) consumerObj.getNativeData(JmsConstants.JMS_CONSUMER_OBJECT);
        ObjectValue sessionObj =
                (ObjectValue) listenerObj.get(JmsConstants.SESSION_FIELD_NAME);
            MessageListener listener = new JmsMessageListenerImpl(strand.scheduler, service, sessionObj);
            try {
                ((MessageConsumer) consumer).setMessageListener(listener);
            } catch (JMSException e) {
                return BallerinaAdapter.getError("Error registering the message listener for service"
                                                         + service.getType().getAnnotationKey(), e);
            }
        return null;
    }
}
