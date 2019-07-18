/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;
import org.ballerinalang.net.jms.utils.JmsUtils;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Initializes a JMS consumer.
 *
 * @since 1.0
 */
@BallerinaFunction(orgName = JmsConstants.BALLERINAX, packageName = JmsConstants.JAVA_JMS,
                   functionName = "init",
                   receiver = @Receiver(type = TypeKind.OBJECT, structType = JmsConstants.CONSUMER_OBJ_NAME,
                                        structPackage = JmsConstants.PROTOCOL_PACKAGE_JMS))
public class Init {

    public static Object init(Strand strand, ObjectValue consumerObj) {

        Session session = JmsUtils.getSession(consumerObj);
        MapValue<String, Object> config = JmsUtils.getConfig(consumerObj);
        try {
            ObjectValue destObj = config.getObjectValue("destination");
            Destination destination = (Destination) destObj.getNativeData(JmsConstants.JMS_DESTINATION_OBJECT);
            String messageSelector = config.getStringValue("messageSelector");
            boolean noLocal = config.getBooleanValue("noLocal");
            String durableId = config.getStringValue("durableId");
            String sharedSubscriptionName = config.getStringValue("sharedSubscriptionName");
            MessageConsumer consumer;
            if (destination instanceof Topic) {
                if (JmsUtils.isNullOrEmptyAfterTrim(durableId) && JmsUtils.isNullOrEmptyAfterTrim(
                        sharedSubscriptionName)) {
                    consumer = session.createConsumer(destination, messageSelector, noLocal);
                } else if (JmsUtils.isNullOrEmptyAfterTrim(durableId)) {
                    consumer = session.createSharedConsumer((Topic) destination, sharedSubscriptionName,
                                                            messageSelector);
                } else if (JmsUtils.isNullOrEmptyAfterTrim(sharedSubscriptionName)) {
                    consumer = session.createDurableConsumer((Topic) destination, durableId, messageSelector, noLocal);
                } else {
                    consumer = session.createSharedDurableConsumer((Topic) destination, durableId, messageSelector);
                }
            } else {
                consumer = session.createConsumer(destination, messageSelector);
            }
            consumerObj.addNativeData(JmsConstants.JMS_CONSUMER_OBJECT, consumer);
            return null;
        } catch (JMSException e) {
            return BallerinaAdapter.getError("Failed to create queue destination.", e);
        }
    }

    private Init() {
    }
}
