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
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Message send action handler.
 */
public class ProducerInitHandler {

    private ProducerInitHandler() {
    }

    public static Object handle(ObjectValue topicPublisherObj, MapValue<String, Object> config) {
        Session session = JmsUtils.getSession(topicPublisherObj);
        Object destinationObject = topicPublisherObj.get("dest");
        MessageProducer producer;
        Destination destination = null;
        if (destinationObject != null) {
            destination = JmsUtils.getDestination((ObjectValue) destinationObject);
        }
        try {
            producer = session.createProducer(destination);
            producer.setDeliveryMode(JmsUtils.getDeliveryMode(config));
            Long priority = config.getIntValue(JmsConstants.PRIORITY);
            if (priority != null) {
                producer.setPriority(Math.toIntExact(priority));
            }
            Long timeToLiveInMilliSeconds = config.getIntValue(JmsConstants.TIME_TO_LIVE);
            if (timeToLiveInMilliSeconds != null) {
                producer.setTimeToLive(timeToLiveInMilliSeconds);
            }
            topicPublisherObj.addNativeData(JmsConstants.JMS_PRODUCER_OBJECT, producer);
            topicPublisherObj.addNativeData(JmsConstants.SESSION_CONNECTOR_OBJECT,
                                            new SessionConnector(session));
        } catch (JMSException e) {
           return BallerinaAdapter.getError("Error creating producer");
        }
        return null;
    }
}
