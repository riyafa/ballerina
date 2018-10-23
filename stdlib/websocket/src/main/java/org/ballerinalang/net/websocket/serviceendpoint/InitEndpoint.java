/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websocket.serviceendpoint;

import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.config.ConfigRegistry;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.connector.api.Value;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.websocket.HttpConnectionManager;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketServicesRegistry;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.util.BLangConstants;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.ballerinalang.runtime.Constants.BALLERINA_VERSION;

/**
 * Initialize the Listener.
 *
 * @since 0.966
 */

@BallerinaFunction(
        orgName = "ballerina", packageName = WebSocketConstants.WEBSOCKET,
        functionName = "initEndpoint",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = WebSocketConstants.WEBSOCKET_LISTENER,
                             structPackage = WebSocketConstants.WEBSOCKET_PACKAGE), isPublic = true)
public class InitEndpoint extends BlockingNativeCallableUnit {

    private static final ConfigRegistry configRegistry = ConfigRegistry.getInstance();

    @Override
    public void execute(Context context) {
        try {
            Struct serviceEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);

            // Creating server connector
            Struct serviceEndpointConfig = serviceEndpoint.getStructField(WebSocketConstants.SERVICE_ENDPOINT_CONFIG);
            ListenerConfiguration listenerConfiguration = getListenerConfig(serviceEndpointConfig);
            ServerConnector httpServerConnector =
                    HttpConnectionManager.getInstance().createHttpServerConnector(listenerConfiguration);
            serviceEndpoint.addNativeData(WebSocketConstants.HTTP_SERVER_CONNECTOR, httpServerConnector);

            //Adding service registries to native data
            WebSocketServicesRegistry webSocketServicesRegistry = new WebSocketServicesRegistry();
            serviceEndpoint.addNativeData(WebSocketConstants.WEBSOCKET_SERVICE_REGISTRY, webSocketServicesRegistry);

            context.setReturnValues((BValue) null);
        } catch (Exception e) {
            BMap<String, BValue> errorStruct = WebSocketUtil.getError(context, e);
            context.setReturnValues(errorStruct);
        }

    }

    private ListenerConfiguration getListenerConfig(Struct endpointConfig) {
        String host = endpointConfig.getStringField(WebSocketConstants.ENDPOINT_CONFIG_HOST);
        long port = endpointConfig.getIntField(WebSocketConstants.ENDPOINT_CONFIG_PORT);
        Struct sslConfig = endpointConfig.getStructField(WebSocketConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        long idleTimeout = endpointConfig.getIntField(WebSocketConstants.ENDPOINT_CONFIG_TIMEOUT);

        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();

        if (host == null || host.trim().isEmpty()) {
            listenerConfiguration.setHost(
                    configRegistry.getConfigOrDefault("b7a.http.host", WebSocketConstants.HTTP_DEFAULT_HOST));
        } else {
            listenerConfiguration.setHost(host);
        }

        if (port == 0) {
            throw new BallerinaConnectorException("Listener port is not defined!");
        }
        listenerConfiguration.setPort(Math.toIntExact(port));

        if (idleTimeout < 0) {
            throw new BallerinaConnectorException("Idle timeout cannot be negative. If you want to disable the " +
                                                          "timeout please use value 0");
        }
        listenerConfiguration.setSocketIdleTimeout(Math.toIntExact(idleTimeout));

        // Set HTTP version
        listenerConfiguration.setVersion("1.1");

        listenerConfiguration.setServerHeader(getServerName());

        if (sslConfig != null) {
            return setSslConfig(sslConfig, listenerConfiguration);
        }

        listenerConfiguration.setPipeliningNeeded(false);

        return listenerConfiguration;
    }

    private String getServerName() {
        String userAgent;
        String version = System.getProperty(BALLERINA_VERSION);
        if (version != null) {
            userAgent = "ballerina/" + version;
        } else {
            userAgent = "ballerina";
        }
        return userAgent;
    }

    private ListenerConfiguration setSslConfig(Struct sslConfig, ListenerConfiguration listenerConfiguration) {
        listenerConfiguration.setScheme(WebSocketConstants.PROTOCOL_HTTPS);
        Struct trustStore = sslConfig.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_TRUST_STORE);
        Struct keyStore = sslConfig.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY_STORE);
        Struct protocols = sslConfig.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_PROTOCOLS);
        Struct validateCert = sslConfig.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_VALIDATE_CERT);
        Struct ocspStapling = sslConfig.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_OCSP_STAPLING);
        String keyFile = sslConfig.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY);
        String certFile = sslConfig.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_CERTIFICATE);
        String trustCerts = sslConfig.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_TRUST_CERTIFICATES);
        String keyPassword = sslConfig.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY_PASSWORD);

        if (keyStore != null && StringUtils.isNotBlank(keyFile)) {
            throw new BallerinaException("Cannot configure both keyStore and keyFile at the same time.");
        } else if (keyStore == null && (StringUtils.isBlank(keyFile) || StringUtils.isBlank(certFile))) {
            throw new BallerinaException("Either keystore or certificateKey and server certificates must be provided "
                                                 + "for secure connection");
        }
        if (keyStore != null) {
            String keyStoreFile = keyStore.getStringField(WebSocketConstants.TRUST_STORE_CONFIG_FILE_PATH);
            if (StringUtils.isBlank(keyStoreFile)) {
                throw new BallerinaException("Keystore file location must be provided for secure connection.");
            }
            String keyStorePassword = keyStore.getStringField(WebSocketConstants.TRUST_STORE_CONFIG_PASSWORD);
            if (StringUtils.isBlank(keyStorePassword)) {
                throw new BallerinaException("Keystore password must be provided for secure connection");
            }
            listenerConfiguration.setKeyStoreFile(keyStoreFile);
            listenerConfiguration.setKeyStorePass(keyStorePassword);
        } else {
            listenerConfiguration.setServerKeyFile(keyFile);
            listenerConfiguration.setServerCertificates(certFile);
            if (StringUtils.isNotBlank(keyPassword)) {
                listenerConfiguration.setServerKeyPassword(keyPassword);
            }
        }
        String sslVerifyClient = sslConfig.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_SSL_VERIFY_CLIENT);
        listenerConfiguration.setVerifyClient(sslVerifyClient);
        if (trustStore == null && StringUtils.isNotBlank(sslVerifyClient) && StringUtils.isBlank(trustCerts)) {
            throw new BallerinaException(
                    "Truststore location or trustCertificates must be provided to enable Mutual SSL");
        }
        if (trustStore != null) {
            String trustStoreFile = trustStore.getStringField(WebSocketConstants.TRUST_STORE_CONFIG_FILE_PATH);
            String trustStorePassword = trustStore.getStringField(WebSocketConstants.TRUST_STORE_CONFIG_PASSWORD);
            if (StringUtils.isBlank(trustStoreFile) && StringUtils.isNotBlank(sslVerifyClient)) {
                throw new BallerinaException("Truststore location must be provided to enable Mutual SSL");
            }
            if (StringUtils.isBlank(trustStorePassword) && StringUtils.isNotBlank(sslVerifyClient)) {
                throw new BallerinaException("Truststore password value must be provided to enable Mutual SSL");
            }
            listenerConfiguration.setTrustStoreFile(trustStoreFile);
            listenerConfiguration.setTrustStorePass(trustStorePassword);
        } else if (StringUtils.isNotBlank(trustCerts)) {
            listenerConfiguration.setServerTrustCertificates(trustCerts);
        }
        List<Parameter> serverParamList = new ArrayList<>();
        Parameter serverParameters;
        if (protocols != null) {
            List<Value> sslEnabledProtocolsValueList = Arrays.asList(
                    protocols.getArrayField(WebSocketConstants.PROTOCOL_CONFIG_ENABLED_PROTOCOLS));
            if (!sslEnabledProtocolsValueList.isEmpty()) {
                String sslEnabledProtocols = sslEnabledProtocolsValueList.stream().map(Value::getStringValue)
                        .collect(Collectors.joining(",", "", ""));
                serverParameters = new Parameter(WebSocketConstants.ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS,
                                                 sslEnabledProtocols);
                serverParamList.add(serverParameters);
            }

            String sslProtocol = protocols.getStringField(WebSocketConstants.PROTOCOL_CONFIG_VERSION);
            if (StringUtils.isNotBlank(sslProtocol)) {
                listenerConfiguration.setSSLProtocol(sslProtocol);
            }
        }

        List<Value> ciphersValueList = Arrays.asList(
                sslConfig.getArrayField(WebSocketConstants.SECURE_SOCKET_CONFIG_CIPHERS));
        if (!ciphersValueList.isEmpty()) {
            String ciphers = ciphersValueList.stream().map(Value::getStringValue)
                    .collect(Collectors.joining(",", "", ""));
            serverParameters = new Parameter(WebSocketConstants.SECURE_SOCKET_CONFIG_CIPHERS, ciphers);
            serverParamList.add(serverParameters);
        }
        if (validateCert != null) {
            boolean validateCertificateEnabled = validateCert.getBooleanField(WebSocketConstants.VALIDATE_CERT_ENABLE);
            long cacheSize = validateCert.getIntField(WebSocketConstants.VALIDATE_CERT_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = validateCert.getIntField(
                    WebSocketConstants.VALIDATE_CERT_CONFIG_CACHE_VALIDITY_PERIOD);
            listenerConfiguration.setValidateCertEnabled(validateCertificateEnabled);
            if (validateCertificateEnabled) {
                if (cacheSize != 0) {
                    listenerConfiguration.setCacheSize(Math.toIntExact(cacheSize));
                }
                if (cacheValidationPeriod != 0) {
                    listenerConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheValidationPeriod));
                }
            }
        }
        if (ocspStapling != null) {
            boolean ocspStaplingEnabled = ocspStapling.getBooleanField(WebSocketConstants.VALIDATE_CERT_ENABLE);
            listenerConfiguration.setOcspStaplingEnabled(ocspStaplingEnabled);
            long cacheSize = ocspStapling.getIntField(WebSocketConstants.VALIDATE_CERT_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = ocspStapling.getIntField(
                    WebSocketConstants.VALIDATE_CERT_CONFIG_CACHE_VALIDITY_PERIOD);
            listenerConfiguration.setValidateCertEnabled(ocspStaplingEnabled);
            if (ocspStaplingEnabled) {
                if (cacheSize != 0) {
                    listenerConfiguration.setCacheSize(Math.toIntExact(cacheSize));
                }
                if (cacheValidationPeriod != 0) {
                    listenerConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheValidationPeriod));
                }
            }
        }
        listenerConfiguration.setTLSStoreType(WebSocketConstants.PKCS_STORE_TYPE);
        String serverEnableSessionCreation = String
                .valueOf(sslConfig.getBooleanField(WebSocketConstants.SECURE_SOCKET_CONFIG_ENABLE_SESSION_CREATION));
        Parameter enableSessionCreationParam = new Parameter(
                WebSocketConstants.SECURE_SOCKET_CONFIG_ENABLE_SESSION_CREATION,
                serverEnableSessionCreation);
        serverParamList.add(enableSessionCreationParam);
        if (!serverParamList.isEmpty()) {
            listenerConfiguration.setParameters(serverParamList);
        }

        listenerConfiguration
                .setId(WebSocketUtil
                               .getListenerInterface(listenerConfiguration.getHost(), listenerConfiguration.getPort()));

        return listenerConfiguration;
    }
}
