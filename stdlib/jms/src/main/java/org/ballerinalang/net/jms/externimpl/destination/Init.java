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

package org.ballerinalang.net.jms.externimpl.destination;

import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;
import org.ballerinalang.net.jms.utils.JmsUtils;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Initializes the JMS Destination object.
 *
 * @since 1.0
 */
@BallerinaFunction(orgName = JmsConstants.BALLERINAX, packageName = JmsConstants.JAVA_JMS,
                   functionName = "init",
                   receiver = @Receiver(type = TypeKind.OBJECT, structType = JmsConstants.DESTINATION_OBJ_NAME,
                                        structPackage = JmsConstants.PROTOCOL_PACKAGE_JMS))
public class Init {

    public static Object init(Strand strand, ObjectValue destObj) {

        Session session = JmsUtils.getSession(destObj);
        try {
            String destType = destObj.getStringValue("destinationType");
            String destName = destObj.getStringValue("destinationName");

            if (destType.equals(JmsConstants.DESTINATION_TYPE_QUEUE)) {
                createQueue(session, destName);
            } else if (destType.equals(JmsConstants.DESTINATION_TYPE_TOPIC)) {
                createTopic(session, destName);
            } else {
                return BallerinaAdapter.getError("Unsupported Destination type");
            }

        } catch (JMSException e) {
            return BallerinaAdapter.getError("Failed to create queue destination.", e);
        }
        return null;
    }

    private static ObjectValue createQueue(Session session, String queueName) throws JMSException {
        if (queueName == null) {
            return JmsUtils.populateAndGetDestinationObj(session.createTemporaryQueue());
        }
        return JmsUtils.populateAndGetDestinationObj(session.createQueue(queueName));
    }

    private static Object createTopic(Session session, String topicName) throws JMSException {
        if (topicName == null) {
            return JmsUtils.populateAndGetDestinationObj(session.createTemporaryTopic());
        }
        return JmsUtils.populateAndGetDestinationObj(session.createTopic(topicName));
    }

    private Init() {
    }
}
