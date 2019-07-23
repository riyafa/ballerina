/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.net.jms.externimpl.topic.listener;

import org.ballerinalang.compiler.plugins.SupportedResourceParamTypes;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.JmsServiceCompilerPlugin;

/**
 * Compiler plugin for validating Jms Listener service.
 *
 * @since 0.995.0
 */
@SupportedResourceParamTypes(
        expectedListenerType = @SupportedResourceParamTypes.Type(packageName = JmsConstants.JMS_VERSION,
                                                                 name = JmsConstants.TOPIC_LISTENER_OBJ_NAME,
                                                                 orgName = JmsConstants.BALLERINAX),
        paramTypes = {@SupportedResourceParamTypes.Type(packageName = JmsConstants.JAVA_JMS,
                                                        name = JmsConstants.MESSAGE_OBJ_NAME,
                                                        orgName = JmsConstants.BALLERINAX)})
public class JmsTopicSubscriberServiceCompilerPlugin extends JmsServiceCompilerPlugin {

}
