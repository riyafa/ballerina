/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.websocket;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.config.ConfigRegistry;
import org.ballerinalang.connector.api.Annotation;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.ParamDetail;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.connector.api.Value;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.services.ErrorHandlerUtils;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.config.KeepAliveConfig;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.Parameter;
import org.wso2.transport.http.netty.contract.config.RequestSizeValidationConfig;
import org.wso2.transport.http.netty.contract.config.SslConfiguration;
import org.wso2.transport.http.netty.contract.websocket.ServerHandshakeFuture;
import org.wso2.transport.http.netty.contract.websocket.ServerHandshakeListener;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;
import org.wso2.transport.http.netty.contract.websocket.WebSocketHandshaker;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.ballerinalang.mime.util.MimeConstants.ENTITY_HEADERS;
import static org.ballerinalang.mime.util.MimeConstants.IS_BODY_BYTE_CHANNEL_ALREADY_SET;
import static org.ballerinalang.mime.util.MimeConstants.MEDIA_TYPE;
import static org.ballerinalang.mime.util.MimeConstants.RESPONSE_ENTITY_FIELD;
import static org.ballerinalang.net.websocket.WebSocketConstants.ENTITY;
import static org.ballerinalang.net.websocket.WebSocketConstants.PROTOCOL_PACKAGE_HTTP;
import static org.ballerinalang.net.websocket.WebSocketConstants.RESPONSE;
import static org.ballerinalang.net.websocket.WebSocketConstants.TRUSTORE_CONFIG_FILE_PATH;
import static org.ballerinalang.net.websocket.WebSocketConstants.TRUSTORE_CONFIG_PASSWORD;
import static org.ballerinalang.net.websocket.WebSocketConstants.WEBSOCKET_PACKAGE;
import static org.ballerinalang.mime.util.MimeConstants.PROTOCOL_PACKAGE_MIME;
import static org.ballerinalang.runtime.Constants.BALLERINA_VERSION;


/**
 * Utility class for websockets.
 */
public class WebSocketUtil {

    static Annotation getServiceConfigAnnotation(Service service) {
        List<Annotation> annotationList = service
                .getAnnotationList(WEBSOCKET_PACKAGE, WebSocketConstants.ANN_NAME_SERVICE_CONFIG);

        if (annotationList == null) {
            return null;
        }
        return annotationList.isEmpty() ? null : annotationList.get(0);
    }

    public static void handleHandshake(WebSocketService wsService, WebSocketConnectionManager connectionManager,
                                       HttpHeaders headers, WebSocketHandshaker webSocketHandshaker, Context context,
                                       CallableUnitCallback callback) {
        String[] subProtocols = wsService.getNegotiableSubProtocols();
        int idleTimeoutInSeconds = wsService.getIdleTimeoutInSeconds();
        int maxFrameSize = wsService.getMaxFrameSize();
        ServerHandshakeFuture future = webSocketHandshaker.handshake(subProtocols, true, idleTimeoutInSeconds * 1000,
                                                                     headers, maxFrameSize);
        future.setHandshakeListener(new ServerHandshakeListener() {
            @Override
            public void onSuccess(WebSocketConnection webSocketConnection) {
                BMap<String, BValue> webSocketEndpoint = BLangConnectorSPIUtil.createObject(
                        wsService.getServiceInfo().getPackageInfo().getProgramFile(), WEBSOCKET_PACKAGE,
                        WebSocketConstants.WEBSOCKET_ENDPOINT);
                BMap<String, BValue> webSocketConnector = BLangConnectorSPIUtil.createObject(
                        wsService.getServiceInfo().getPackageInfo().getProgramFile(), WEBSOCKET_PACKAGE,
                        WebSocketConstants.WEBSOCKET_CONNECTOR);

                webSocketEndpoint.put(WebSocketConstants.LISTENER_CONNECTOR_FIELD, webSocketConnector);
                populateEndpoint(webSocketConnection, webSocketEndpoint);
                WebSocketOpenConnectionInfo connectionInfo =
                        new WebSocketOpenConnectionInfo(wsService, webSocketConnection, webSocketEndpoint, context);
                connectionManager.addConnection(webSocketConnection.getChannelId(), connectionInfo);
                webSocketConnector.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO,
                                                 connectionInfo);
                if (context != null && callback != null) {
                    context.setReturnValues(webSocketEndpoint);
                    callback.notifySuccess();
                } else {
                    Resource onOpenResource = wsService.getResourceByName(WebSocketConstants.RESOURCE_NAME_ON_OPEN);
                    if (onOpenResource != null) {
                        executeOnOpenResource(onOpenResource, webSocketEndpoint, webSocketConnection);
                    } else {
                        readFirstFrame(webSocketConnection, webSocketConnector);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (context != null && callback != null) {
                    callback.notifyFailure(BLangVMErrors.createError(context, "Unable to complete handshake:" +
                            throwable.getMessage()));
                } else {
                    throw new BallerinaConnectorException("Unable to complete handshake", throwable);
                }
            }
        });
    }

    public static void executeOnOpenResource(Resource onOpenResource, BMap<String, BValue> webSocketEndpoint,
                                             WebSocketConnection webSocketConnection) {
        List<ParamDetail> paramDetails =
                onOpenResource.getParamDetails();
        BValue[] bValues = new BValue[paramDetails.size()];
        bValues[0] = webSocketEndpoint;
        BMap<String, BValue> webSocketConnector =
                (BMap<String, BValue>) webSocketEndpoint.get(WebSocketConstants.LISTENER_CONNECTOR_FIELD);

        CallableUnitCallback onOpenCallableUnitCallback = new CallableUnitCallback() {
            @Override
            public void notifySuccess() {
                boolean isReady = ((BBoolean) webSocketConnector.get(WebSocketConstants.CONNECTOR_IS_READY_FIELD))
                        .booleanValue();
                if (!isReady) {
                    readFirstFrame(webSocketConnection, webSocketConnector);
                }
            }

            @Override
            public void notifyFailure(BMap<String, BValue> error) {
                boolean isReady = ((BBoolean) webSocketConnector.get(WebSocketConstants.CONNECTOR_IS_READY_FIELD))
                        .booleanValue();
                if (!isReady) {
                    readFirstFrame(webSocketConnection, webSocketConnector);
                }
                ErrorHandlerUtils.printError("error: " + BLangVMErrors.getPrintableStackTrace(error));
                closeDuringUnexpectedCondition(webSocketConnection);
            }
        };
        Executor.submit(onOpenResource, onOpenCallableUnitCallback, null, null, bValues);
    }

    public static void populateEndpoint(WebSocketConnection webSocketConnection,
                                        BMap<String, BValue> webSocketEndpoint) {
        webSocketEndpoint.put(WebSocketConstants.LISTENER_ID_FIELD, new BString(webSocketConnection.getChannelId()));
        webSocketEndpoint.put(WebSocketConstants.LISTENER_NEGOTIATED_SUBPROTOCOLS_FIELD,
                              new BString(webSocketConnection.getNegotiatedSubProtocol()));
        webSocketEndpoint.put(WebSocketConstants.LISTENER_IS_SECURE_FIELD,
                              new BBoolean(webSocketConnection.isSecure()));
        webSocketEndpoint.put(WebSocketConstants.LISTENER_IS_OPEN_FIELD,
                              new BBoolean(webSocketConnection.isOpen()));
    }

    public static void handleWebSocketCallback(Context context, CallableUnitCallback callback,
                                               ChannelFuture webSocketChannelFuture) {
        webSocketChannelFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                context.setReturnValues(WebSocketUtil.getError(context, cause));
            } else {
                context.setReturnValues();
            }
            callback.notifySuccess();
        });
    }

    public static void readFirstFrame(WebSocketConnection webSocketConnection,
                                      BMap<String, BValue> webSocketConnector) {
        webSocketConnection.readNextFrame();
        webSocketConnector.put(WebSocketConstants.CONNECTOR_IS_READY_FIELD, new BBoolean(true));
    }

    /**
     * Closes the connection with the unexpected failure status code.
     *
     * @param webSocketConnection the websocket connection to be closed.
     */
    static void closeDuringUnexpectedCondition(WebSocketConnection webSocketConnection) {
        webSocketConnection.terminateConnection(1011, "Unexpected condition");

    }

    public static void setListenerOpenField(WebSocketOpenConnectionInfo connectionInfo) {
        connectionInfo.getWebSocketEndpoint().put(WebSocketConstants.LISTENER_IS_OPEN_FIELD,
                                                  new BBoolean(connectionInfo.getWebSocketConnection().isOpen()));
    }

    /**
     * Get error struct from throwable.
     *
     * @param context   Represent ballerina context
     * @param throwable Throwable representing the error.
     * @return Error struct
     */
    public static BMap<String, BValue> getError(Context context, Throwable throwable) {
        if (throwable.getMessage() == null) {
            return BLangVMErrors.createError(context, "An unexpected error occurred.");
        } else {
            return BLangVMErrors.createError(context, throwable.getMessage());
        }
    }

    public static void populateSSLConfiguration(SslConfiguration sslConfiguration, Struct secureSocket) {
        Struct trustStore = secureSocket.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_TRUST_STORE);
        Struct keyStore = secureSocket.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY_STORE);
        Struct protocols = secureSocket.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_PROTOCOLS);
        Struct validateCert = secureSocket.getStructField(WebSocketConstants.SECURE_SOCKET_CONFIG_VALIDATE_CERT);
        String keyFile = secureSocket.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY);
        String certFile = secureSocket.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_CERTIFICATE);
        String trustCerts = secureSocket.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_TRUST_CERTIFICATES);
        String keyPassword = secureSocket.getStringField(WebSocketConstants.SECURE_SOCKET_CONFIG_KEY_PASSWORD);
        List<Parameter> clientParams = new ArrayList<>();
        if (trustStore != null && StringUtils.isNotBlank(trustCerts)) {
            throw new BallerinaException("Cannot configure both trustStore and trustCerts at the same time.");
        }
        if (trustStore != null) {
            String trustStoreFile = trustStore.getStringField(TRUSTORE_CONFIG_FILE_PATH);
            if (StringUtils.isNotBlank(trustStoreFile)) {
                sslConfiguration.setTrustStoreFile(trustStoreFile);
            }
            String trustStorePassword = trustStore.getStringField(TRUSTORE_CONFIG_PASSWORD);
            if (StringUtils.isNotBlank(trustStorePassword)) {
                sslConfiguration.setTrustStorePass(trustStorePassword);
            }
        } else if (StringUtils.isNotBlank(trustCerts)) {
            sslConfiguration.setClientTrustCertificates(trustCerts);
        }
        if (keyStore != null && StringUtils.isNotBlank(keyFile)) {
            throw new BallerinaException("Cannot configure both keyStore and keyFile.");
        } else if (StringUtils.isNotBlank(keyFile) && StringUtils.isBlank(certFile)) {
            throw new BallerinaException("Need to configure certFile containing client ssl certificates.");
        }
        if (keyStore != null) {
            String keyStoreFile = keyStore.getStringField(TRUSTORE_CONFIG_FILE_PATH);
            if (StringUtils.isNotBlank(keyStoreFile)) {
                sslConfiguration.setKeyStoreFile(keyStoreFile);
            }
            String keyStorePassword = keyStore.getStringField(TRUSTORE_CONFIG_PASSWORD);
            if (StringUtils.isNotBlank(keyStorePassword)) {
                sslConfiguration.setKeyStorePass(keyStorePassword);
            }
        } else if (StringUtils.isNotBlank(keyFile)) {
            sslConfiguration.setClientKeyFile(keyFile);
            sslConfiguration.setClientCertificates(certFile);
            if (StringUtils.isNotBlank(keyPassword)) {
                sslConfiguration.setClientKeyPassword(keyPassword);
            }
        }
        if (protocols != null) {
            List<Value> sslEnabledProtocolsValueList = Arrays
                    .asList(protocols.getArrayField(WebSocketConstants.PROTOCOL_CONFIG_ENABLED_PROTOCOLS));
            if (sslEnabledProtocolsValueList.size() > 0) {
                String sslEnabledProtocols = sslEnabledProtocolsValueList.stream().map(Value::getStringValue)
                        .collect(Collectors.joining(",", "", ""));
                Parameter clientProtocols = new Parameter(WebSocketConstants.SSL_ENABLED_PROTOCOLS,
                                                          sslEnabledProtocols);
                clientParams.add(clientProtocols);
            }
            String sslProtocol = protocols.getStringField(WebSocketConstants.PROTOCOL_CONFIG_VERSION);
            if (StringUtils.isNotBlank(sslProtocol)) {
                sslConfiguration.setSSLProtocol(sslProtocol);
            }
        }

        if (validateCert != null) {
            boolean validateCertEnabled = validateCert.getBooleanField(WebSocketConstants.PROTOCOL_CONFIG_ENABLE);
            int cacheSize = (int) validateCert.getIntField(WebSocketConstants.SSL_CONFIG_CACHE_SIZE);
            int cacheValidityPeriod = (int) validateCert
                    .getIntField(WebSocketConstants.SSL_CONFIG_CACHE_VALIDITY_PERIOD);
            sslConfiguration.setValidateCertEnabled(validateCertEnabled);
            if (cacheValidityPeriod != 0) {
                sslConfiguration.setCacheValidityPeriod(cacheValidityPeriod);
            }
            if (cacheSize != 0) {
                sslConfiguration.setCacheSize(cacheSize);
            }
        }
        boolean hostNameVerificationEnabled = secureSocket
                .getBooleanField(WebSocketConstants.SSL_CONFIG_HOST_NAME_VERIFICATION_ENABLED);
        boolean ocspStaplingEnabled = secureSocket.getBooleanField(
                WebSocketConstants.SECURE_SOCKET_CONFIG_OCSP_STAPLING);
        sslConfiguration.setOcspStaplingEnabled(ocspStaplingEnabled);
        sslConfiguration.setHostNameVerificationEnabled(hostNameVerificationEnabled);

        List<Value> ciphersValueList = Arrays
                .asList(secureSocket.getArrayField(WebSocketConstants.SSL_CONFIG_CIPHERS));
        if (ciphersValueList.size() > 0) {
            String ciphers = ciphersValueList.stream().map(Value::getStringValue)
                    .collect(Collectors.joining(",", "", ""));
            Parameter clientCiphers = new Parameter(WebSocketConstants.CIPHERS, ciphers);
            clientParams.add(clientCiphers);
        }
        String enableSessionCreation = String.valueOf(
                secureSocket.getBooleanField(WebSocketConstants.SSL_CONFIG_ENABLE_SESSION_CREATION));
        Parameter clientEnableSessionCreation = new Parameter(WebSocketConstants.SSL_CONFIG_ENABLE_SESSION_CREATION,
                                                              enableSessionCreation);
        clientParams.add(clientEnableSessionCreation);
        if (!clientParams.isEmpty()) {
            sslConfiguration.setParameters(clientParams);
        }
    }

    public static void setDefaultTrustStore(SslConfiguration sslConfiguration) {
        sslConfiguration.setTrustStoreFile(String.valueOf(
                Paths.get(System.getProperty("ballerina.home"), "bre", "security", "ballerinaTruststore.p12")));
        sslConfiguration.setTrustStorePass("ballerina");
    }

    /**
     * Creates InResponse using the native {@code HttpCarbonMessage}.
     *
     * @param context           ballerina context
     * @param httpCarbonMessage the HttpCarbonMessage
     * @return the Response struct
     */
    public static BMap<String, BValue> createResponseStruct(Context context, HttpCarbonMessage httpCarbonMessage) {
        BMap<String, BValue> responseStruct = BLangConnectorSPIUtil.createBStruct(context,
                                                                                  PROTOCOL_PACKAGE_HTTP, RESPONSE);
        BMap<String, BValue> entity =
                BLangConnectorSPIUtil.createBStruct(context, PROTOCOL_PACKAGE_MIME, ENTITY);
        BMap<String, BValue> mediaType =
                BLangConnectorSPIUtil.createBStruct(context, PROTOCOL_PACKAGE_MIME, MEDIA_TYPE);

        populateInboundResponse(responseStruct, entity, mediaType, context.getProgramFile(),
                                httpCarbonMessage);
        return responseStruct;
    }

    /**
     * Populate inbound response with headers and entity.
     *
     * @param inboundResponse    Ballerina struct to represent response
     * @param entity             Entity of the response
     * @param mediaType          Content type of the response
     * @param programFile        Cache control struct which holds the cache control directives related to the
     *                           response
     * @param inboundResponseMsg Represent carbon message.
     */
    public static void populateInboundResponse(BMap<String, BValue> inboundResponse, BMap<String, BValue> entity,
                                               BMap<String, BValue> mediaType, ProgramFile programFile,
                                               HttpCarbonMessage inboundResponseMsg) {
        inboundResponse.addNativeData(WebSocketConstants.TRANSPORT_MESSAGE, inboundResponseMsg);
        int statusCode = (Integer) inboundResponseMsg.getProperty(WebSocketConstants.HTTP_STATUS_CODE);
        inboundResponse.put(WebSocketConstants.RESPONSE_STATUS_CODE_FIELD, new BInteger(statusCode));
        inboundResponse.put(WebSocketConstants.RESPONSE_REASON_PHRASE_FIELD,
                            new BString(HttpResponseStatus.valueOf(statusCode).reasonPhrase()));

        if (inboundResponseMsg.getHeader(HttpHeaderNames.SERVER.toString()) != null) {
            inboundResponse.put(WebSocketConstants.RESPONSE_SERVER_FIELD,
                                new BString(inboundResponseMsg.getHeader(HttpHeaderNames.SERVER.toString())));
            inboundResponseMsg.removeHeader(HttpHeaderNames.SERVER.toString());
        }

        if (inboundResponseMsg.getProperty(WebSocketConstants.RESOLVED_REQUESTED_URI) != null) {
            inboundResponse.put(WebSocketConstants.RESOLVED_REQUESTED_URI_FIELD,
                                new BString(inboundResponseMsg.getProperty(WebSocketConstants.RESOLVED_REQUESTED_URI)
                                                    .toString()));
        }
        populateEntity(entity, mediaType, inboundResponseMsg);
        inboundResponse.put(RESPONSE_ENTITY_FIELD, entity);
        inboundResponse.addNativeData(IS_BODY_BYTE_CHANNEL_ALREADY_SET, false);
    }

    /**
     * Populate entity with headers, content-type and content-length.
     *
     * @param entity    Represent an entity struct
     * @param mediaType mediaType struct that needs to be set to the entity
     * @param cMsg      Represent a carbon message
     */
    private static void populateEntity(BMap<String, BValue> entity, BMap<String, BValue> mediaType,
                                       HttpCarbonMessage cMsg) {
        String contentType = cMsg.getHeader(HttpHeaderNames.CONTENT_TYPE.toString());
        MimeUtil.setContentType(mediaType, entity, contentType);
        long contentLength = -1;
        String lengthStr = cMsg.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
        try {
            contentLength = lengthStr != null ? Long.parseLong(lengthStr) : contentLength;
            MimeUtil.setContentLength(entity, contentLength);
        } catch (NumberFormatException e) {
            throw new BallerinaException("Invalid content length");
        }
        entity.addNativeData(ENTITY_HEADERS, cMsg.getHeaders());
    }

    private static final ConfigRegistry configRegistry = ConfigRegistry.getInstance();

    public static ListenerConfiguration getListenerConfig(Struct endpointConfig) {
        String host = endpointConfig.getStringField(WebSocketConstants.ENDPOINT_CONFIG_HOST);
        long port = endpointConfig.getIntField(WebSocketConstants.ENDPOINT_CONFIG_PORT);
        String keepAlive = endpointConfig.getRefField(WebSocketConstants.ENDPOINT_CONFIG_KEEP_ALIVE).getStringValue();
        Struct sslConfig = endpointConfig.getStructField(WebSocketConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String httpVersion = endpointConfig.getStringField(WebSocketConstants.ENDPOINT_CONFIG_VERSION);
        Struct requestLimits = endpointConfig.getStructField(WebSocketConstants.ENDPOINT_REQUEST_LIMITS);
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

        listenerConfiguration.setKeepAliveConfig(getKeepAliveConfig(keepAlive));

        // Set Request validation limits.
        if (requestLimits != null) {
            setRequestSizeValidationConfig(requestLimits, listenerConfiguration);
        }

        if (idleTimeout < 0) {
            throw new BallerinaConnectorException("Idle timeout cannot be negative. If you want to disable the " +
                                                          "timeout please use value 0");
        }
        listenerConfiguration.setSocketIdleTimeout(Math.toIntExact(idleTimeout));

        // Set HTTP version
        if (httpVersion != null) {
            listenerConfiguration.setVersion(httpVersion);
        }

        listenerConfiguration.setServerHeader(getServerName());

        if (sslConfig != null) {
            return setSslConfig(sslConfig, listenerConfiguration);
        }

        listenerConfiguration.setPipeliningNeeded(true); //Pipelining is enabled all the time
        listenerConfiguration.setPipeliningLimit(endpointConfig.getIntField(
                WebSocketConstants.PIPELINING_REQUEST_LIMIT));

        return listenerConfiguration;
    }

    private static void setRequestSizeValidationConfig(Struct requestLimits, ListenerConfiguration listenerConfiguration) {
        long maxUriLength = requestLimits.getIntField(WebSocketConstants.REQUEST_LIMITS_MAXIMUM_URL_LENGTH);
        long maxHeaderSize = requestLimits.getIntField(WebSocketConstants.REQUEST_LIMITS_MAXIMUM_HEADER_SIZE);
        long maxEntityBodySize = requestLimits.getIntField(WebSocketConstants.REQUEST_LIMITS_MAXIMUM_ENTITY_BODY_SIZE);
        RequestSizeValidationConfig requestSizeValidationConfig = listenerConfiguration
                .getRequestSizeValidationConfig();

        if (maxUriLength != -1) {
            if (maxUriLength >= 0) {
                requestSizeValidationConfig.setMaxUriLength(Math.toIntExact(maxUriLength));
            } else {
                throw new BallerinaConnectorException("Invalid configuration found for maxUriLength : " + maxUriLength);
            }
        }

        if (maxHeaderSize != -1) {
            if (maxHeaderSize >= 0) {
                requestSizeValidationConfig.setMaxHeaderSize(Math.toIntExact(maxHeaderSize));
            } else {
                throw new BallerinaConnectorException(
                        "Invalid configuration found for maxHeaderSize : " + maxHeaderSize);
            }
        }

        if (maxEntityBodySize != -1) {
            if (maxEntityBodySize >= 0) {
                requestSizeValidationConfig.setMaxEntityBodySize(maxEntityBodySize);
            } else {
                throw new BallerinaConnectorException(
                        "Invalid configuration found for maxEntityBodySize : " + maxEntityBodySize);
            }
        }
    }

    private static String getServerName() {
        String userAgent;
        String version = System.getProperty(BALLERINA_VERSION);
        if (version != null) {
            userAgent = "ballerina/" + version;
        } else {
            userAgent = "ballerina";
        }
        return userAgent;
    }

    private static ListenerConfiguration setSslConfig(Struct sslConfig, ListenerConfiguration listenerConfiguration) {
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
            String keyStoreFile = keyStore.getStringField(TRUSTORE_CONFIG_FILE_PATH);
            if (StringUtils.isBlank(keyStoreFile)) {
                throw new BallerinaException("Keystore file location must be provided for secure connection.");
            }
            String keyStorePassword = keyStore.getStringField(TRUSTORE_CONFIG_PASSWORD);
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
        String sslVerifyClient = sslConfig.getStringField(WebSocketConstants.SSL_CONFIG_SSL_VERIFY_CLIENT);
        listenerConfiguration.setVerifyClient(sslVerifyClient);
        if (trustStore == null && StringUtils.isNotBlank(sslVerifyClient) && StringUtils.isBlank(trustCerts)) {
            throw new BallerinaException(
                    "Truststore location or trustCertificates must be provided to enable Mutual SSL");
        }
        if (trustStore != null) {
            String trustStoreFile = trustStore.getStringField(TRUSTORE_CONFIG_FILE_PATH);
            String trustStorePassword = trustStore.getStringField(TRUSTORE_CONFIG_PASSWORD);
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
            List<Value> sslEnabledProtocolsValueList = Arrays.asList(protocols.getArrayField(WebSocketConstants.PROTOCOL_CONFIG_ENABLED_PROTOCOLS));
            if (!sslEnabledProtocolsValueList.isEmpty()) {
                String sslEnabledProtocols = sslEnabledProtocolsValueList.stream().map(Value::getStringValue)
                        .collect(Collectors.joining(",", "", ""));
                serverParameters = new Parameter(WebSocketConstants.PROTOCOL_CONFIG_ENABLED_PROTOCOLS, sslEnabledProtocols);
                serverParamList.add(serverParameters);
            }

            String sslProtocol = protocols.getStringField(WebSocketConstants.PROTOCOL_CONFIG_VERSION);
            if (StringUtils.isNotBlank(sslProtocol)) {
                listenerConfiguration.setSSLProtocol(sslProtocol);
            }
        }

        List<Value> ciphersValueList = Arrays.asList(sslConfig.getArrayField(WebSocketConstants.SSL_CONFIG_CIPHERS));
        if (!ciphersValueList.isEmpty()) {
            String ciphers = ciphersValueList.stream().map(Value::getStringValue)
                    .collect(Collectors.joining(",", "", ""));
            serverParameters = new Parameter(WebSocketConstants.CIPHERS, ciphers);
            serverParamList.add(serverParameters);
        }
        if (validateCert != null) {
            boolean validateCertificateEnabled = validateCert.getBooleanField(WebSocketConstants.PROTOCOL_CONFIG_ENABLE);
            long cacheSize = validateCert.getIntField(WebSocketConstants.SSL_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = validateCert.getIntField(WebSocketConstants.SSL_CONFIG_CACHE_VALIDITY_PERIOD);
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
            boolean ocspStaplingEnabled = ocspStapling.getBooleanField(WebSocketConstants.PROTOCOL_CONFIG_ENABLE);
            listenerConfiguration.setOcspStaplingEnabled(ocspStaplingEnabled);
            long cacheSize = ocspStapling.getIntField(WebSocketConstants.SSL_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = ocspStapling.getIntField(WebSocketConstants.SSL_CONFIG_CACHE_VALIDITY_PERIOD);
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
                .valueOf(sslConfig.getBooleanField(WebSocketConstants.SSL_CONFIG_ENABLE_SESSION_CREATION));
        Parameter enableSessionCreationParam = new Parameter(WebSocketConstants.SSL_CONFIG_ENABLE_SESSION_CREATION,
                                                             serverEnableSessionCreation);
        serverParamList.add(enableSessionCreationParam);
        if (!serverParamList.isEmpty()) {
            listenerConfiguration.setParameters(serverParamList);
        }

        listenerConfiguration
                .setId(getListenerInterface(listenerConfiguration.getHost(), listenerConfiguration.getPort()));

        return listenerConfiguration;
    }

    public static String getListenerInterface(String host, int port) {
        host = host != null ? host : "0.0.0.0";
        return host + ":" + port;
    }

    public static HttpWsConnectorFactory createHttpWsConnectionFactory() {
        return new DefaultHttpWsConnectorFactory();
    }

    public static KeepAliveConfig getKeepAliveConfig(String keepAliveConfig) {
        switch (keepAliveConfig) {
            case WebSocketConstants.AUTO:
                return KeepAliveConfig.AUTO;
            case WebSocketConstants.ALWAYS:
                return KeepAliveConfig.ALWAYS;
            case WebSocketConstants.NEVER:
                return KeepAliveConfig.NEVER;
            default:
                throw new BallerinaConnectorException(
                        "Invalid configuration found for Keep-Alive: " + keepAliveConfig);
        }
    }

    public static boolean isConnectorStarted(Struct serviceEndpoint) {
        return serviceEndpoint.getNativeData(WebSocketConstants.CONNECTOR_STARTED) != null &&
                (Boolean) serviceEndpoint.getNativeData(WebSocketConstants.CONNECTOR_STARTED);
    }

    public static ServerConnector getServerConnector(Struct serviceEndpoint) {
        return (ServerConnector) serviceEndpoint.getNativeData(WebSocketConstants.HTTP_SERVER_CONNECTOR);
    }

    private WebSocketUtil() {
    }
}
