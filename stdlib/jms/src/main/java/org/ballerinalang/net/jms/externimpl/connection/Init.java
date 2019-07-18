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

package org.ballerinalang.net.jms.externimpl.connection;

import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.jms.JmsConstants;
import org.ballerinalang.net.jms.utils.JmsUtils;
import org.ballerinalang.net.jms.LoggingExceptionListener;
import org.ballerinalang.net.jms.utils.BallerinaAdapter;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Connection init function for JMS connection endpoint.
 *
 * @since 0.970
 */
@BallerinaFunction(
        orgName = JmsConstants.BALLERINAX, packageName = JmsConstants.JAVA_JMS,
        functionName = "createConnection",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = JmsConstants.CONNECTION_OBJ_NAME,
                             structPackage = JmsConstants.PROTOCOL_PACKAGE_JMS)
)
public class Init {
    private static final Logger LOGGER = LoggerFactory.getLogger(Init.class);

    public static Object init(Strand strand, ObjectValue connectionObject, MapValue connectionConfig) {

        Connection connection = createConnection(connectionConfig);
        try {
            if (connection.getClientID() == null) {
                connection.setClientID(UUID.randomUUID().toString());
            }
            connection.setExceptionListener(new LoggingExceptionListener());
            connection.start();
        } catch (JMSException e) {
            return BallerinaAdapter.getError("Error occurred while starting connection.", e);
        }
        connectionObject.addNativeData(JmsConstants.JMS_CONNECTION, connection);
        return null;
    }

    private static Connection createConnection(MapValue connectionConfig) {
        Map<String, String> configParams = new HashMap<>();

        String initialContextFactory = connectionConfig.getStringValue(JmsConstants.ALIAS_INITIAL_CONTEXT_FACTORY);
        configParams.put(JmsConstants.ALIAS_INITIAL_CONTEXT_FACTORY, initialContextFactory);

        String providerUrl = connectionConfig.getStringValue(JmsConstants.ALIAS_PROVIDER_URL);
        configParams.put(JmsConstants.ALIAS_PROVIDER_URL, providerUrl);

        String factoryName = connectionConfig.getStringValue(JmsConstants.ALIAS_CONNECTION_FACTORY_NAME);
        configParams.put(JmsConstants.ALIAS_CONNECTION_FACTORY_NAME, factoryName);

        preProcessIfWso2MB(configParams);
        updateMappedParameters(configParams);

        Properties properties = new Properties();
        configParams.forEach(properties::put);

        //check for additional jndi properties
        @SuppressWarnings(JmsConstants.UNCHECKED)
        MapValue<String, Object> props = (MapValue<String, Object>) connectionConfig.getMapValue(
                JmsConstants.PROPERTIES_MAP);
        for (String key : props.getKeys()) {
            properties.put(key, props.get(key));
        }

        try {
            InitialContext initialContext = new InitialContext(properties);
            ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(factoryName);
            String username = null;
            String password = null;
            if (connectionConfig.get(JmsConstants.ALIAS_USERNAME) != null &&
                    connectionConfig.get(JmsConstants.ALIAS_PASSWORD) != null) {
                username = connectionConfig.getStringValue(JmsConstants.ALIAS_USERNAME);
                password = connectionConfig.getStringValue(JmsConstants.ALIAS_PASSWORD);
            }

            if (!JmsUtils.isNullOrEmptyAfterTrim(username) && password != null) {
                return connectionFactory.createConnection(username, password);
            } else {
                return connectionFactory.createConnection();
            }
        } catch (NamingException | JMSException e) {
            String message = "Error while connecting to broker.";
            LOGGER.error(message, e);
            throw new BallerinaException(message + " " + e.getMessage(), e);
        }
    }

    private static void preProcessIfWso2MB(Map<String, String> configParams) {
        String initialConnectionFactoryName = configParams.get(JmsConstants.ALIAS_INITIAL_CONTEXT_FACTORY);
        if (JmsConstants.BMB_ICF_ALIAS.equalsIgnoreCase(initialConnectionFactoryName)
                || JmsConstants.MB_ICF_ALIAS.equalsIgnoreCase(initialConnectionFactoryName)) {

            configParams.put(JmsConstants.ALIAS_INITIAL_CONTEXT_FACTORY, JmsConstants.MB_ICF_NAME);
            String connectionFactoryName = configParams.get(JmsConstants.ALIAS_CONNECTION_FACTORY_NAME);
            if (configParams.get(JmsConstants.ALIAS_PROVIDER_URL) != null) {
                System.setProperty("qpid.dest_syntax", "BURL");
                if (!JmsUtils.isNullOrEmptyAfterTrim(connectionFactoryName)) {
                    configParams.put(JmsConstants.MB_CF_NAME_PREFIX + connectionFactoryName,
                                     configParams.get(JmsConstants.ALIAS_PROVIDER_URL));
                    configParams.remove(JmsConstants.ALIAS_PROVIDER_URL);
                } else {
                    throw new BallerinaException(
                            JmsConstants.ALIAS_CONNECTION_FACTORY_NAME + " property should be set");
                }
            } else if (configParams.get(JmsConstants.CONFIG_FILE_PATH) != null) {
                configParams.put(JmsConstants.ALIAS_PROVIDER_URL, configParams.get(JmsConstants.CONFIG_FILE_PATH));
                configParams.remove(JmsConstants.CONFIG_FILE_PATH);
            }
        }
    }

    private static void updateMappedParameters(Map<String, String> configParams) {
        Iterator<Map.Entry<String, String>> iterator = configParams.entrySet().iterator();
        Map<String, String> tempMap = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String mappedParam = JmsConstants.MAPPING_PARAMETERS.get(entry.getKey());
            if (mappedParam != null) {
                tempMap.put(mappedParam, entry.getValue());
                iterator.remove();
            }
        }
        configParams.putAll(tempMap);
    }

    private Init() {
    }
}
