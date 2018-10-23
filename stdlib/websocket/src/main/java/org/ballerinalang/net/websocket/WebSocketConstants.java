/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.websocket;

import static org.ballerinalang.util.BLangConstants.BALLERINA_PACKAGE_PREFIX;

/**
 * Constants of WebSocket.
 */
public class WebSocketConstants {


    public static final String WEBSOCKET = "websocket";
    public static final String WEBSOCKET_LISTENER = "Listener";
    //ballerina/websocket:Listener
    public static final String WEBSOCKET_SERVICE_ENDPOINT_NAME =
            BALLERINA_PACKAGE_PREFIX  + WEBSOCKET + ":" + WEBSOCKET_LISTENER;
    public static final String WEBSOCKET_CLIENT_ENDPOINT = "Client";
    public static final String WEBSOCKET_CLIENT_ENDPOINT_NAME = "ballerina/http:WebSocketClient";
    public static final String CONNECTOR_STARTED = "CONNECTOR_STARTED";
    public static final String WEBSOCKET_PACKAGE = BALLERINA_PACKAGE_PREFIX + WEBSOCKET;

    public static final String WSS_ENDPOINT_STARTED = "[" + WEBSOCKET_PACKAGE + "] started WSS endpoint ";
    public static final String WS_ENDPOINT_STARTED = "[" + WEBSOCKET_PACKAGE + "] started WS endpoint ";
    public static final String WSS_ENDPOINT_STOPPED = "[" + WEBSOCKET_PACKAGE + "] stopped WSS endpoint ";
    public static final String WS_ENDPOINT_STOPPED = "[" + WEBSOCKET_PACKAGE + "] stopped WS endpoint ";

    public static final String WEBSOCKET_CONNECTOR = "WebSocketConnector";
    public static final String WEBSOCKET_SERVICE = "WebSocketService";
    public static final String WEBSOCKET_CLIENT_SERVICE = "WebSocketClientService";

    public static final String SERVICE_ENDPOINT_CONFIG = "config";
    public static final String CLIENT_ENDPOINT_CONFIG = "config";
    public static final String SERVICE_ENDPOINT_CONNECTION_FIELD = "conn";
    public static final String ENDPOINT_CONFIG_HOST = "host";
    public static final String ENDPOINT_CONFIG_PORT = "port";
    public static final String ENDPOINT_CONFIG_SECURE_SOCKET = "secureSocket";
    public static final String ENDPOINT_CONFIG_TIMEOUT = "timeoutMillis";
    public static final String SECURE_SOCKET_CONFIG_TRUST_STORE = "trustStore";
    public static final String SECURE_SOCKET_CONFIG_KEY_STORE = "keyStore";
    public static final String SECURE_SOCKET_CONFIG_PROTOCOLS = "protocol";
    public static final String SECURE_SOCKET_CONFIG_VALIDATE_CERT = "certValidation";
    public static final String SECURE_SOCKET_CONFIG_OCSP_STAPLING = "ocspStapling";
    public static final String SECURE_SOCKET_CONFIG_KEY = "keyFile";
    public static final String SECURE_SOCKET_CONFIG_CERTIFICATE = "certFile";
    public static final String SECURE_SOCKET_CONFIG_TRUST_CERTIFICATES = "trustedCertFile";
    public static final String SECURE_SOCKET_CONFIG_KEY_PASSWORD = "keyPassword";
    public static final String SECURE_SOCKET_CONFIG_SSL_VERIFY_CLIENT = "sslVerifyClient";
    public static final String SECURE_SOCKET_CONFIG_CIPHERS = "ciphers";
    public static final String SECURE_SOCKET_CONFIG_ENABLE_SESSION_CREATION = "shareSession";
    public static final String TRUST_STORE_CONFIG_FILE_PATH = "path";
    public static final String TRUST_STORE_CONFIG_PASSWORD = "password";
    public static final String ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS = "sslEnabledProtocols";
    public static final String PROTOCOL_CONFIG_VERSION = "name";
    public static final String PROTOCOL_CONFIG_ENABLED_PROTOCOLS = "versions";
    public static final String VALIDATE_CERT_ENABLE = "enable";
    public static final String VALIDATE_CERT_CONFIG_CACHE_SIZE = "cacheSize";
    public static final String VALIDATE_CERT_CONFIG_CACHE_VALIDITY_PERIOD = "cacheValidityPeriod";

    public static final String PKCS_STORE_TYPE = "PKCS12";
    public static final String HTTP_SERVER_CONNECTOR = "HTTP_SERVER_CONNECTOR";
    public static final String WEBSOCKET_SERVICE_REGISTRY = "WS_SERVICE_REGISTRY";
    public static final String DEFAULT_BASE_PATH = "/";
    public static final String QUERY_STR = "QUERY_STR";
    public static final String RESOURCE_ARGS = "RESOURCE_ARGS";
    public static final String UTF_8 = "UTF-8";

    public static final String HTTP_DEFAULT_HOST = "0.0.0.0";
    public static final String PROTOCOL_HTTPS = "https";

    public static final String WEBSOCKET_ANNOTATION_CONFIGURATION = "WebSocketServiceConfig";
    public static final String ANNOTATION_ATTR_PATH = "path";
    public static final String ANNOTATION_ATTR_SUB_PROTOCOLS = "subProtocols";
    public static final String ANNOTATION_ATTR_IDLE_TIMEOUT = "idleTimeoutInSeconds";
    public static final String ANNOTATION_ATTR_MAX_FRAME_SIZE = "maxFrameSize";

    public static final String RESOURCE_NAME_ON_OPEN = "onOpen";
    public static final String RESOURCE_NAME_ON_TEXT = "onText";
    public static final String RESOURCE_NAME_ON_BINARY = "onBinary";
    public static final String RESOURCE_NAME_ON_PING = "onPing";
    public static final String RESOURCE_NAME_ON_PONG = "onPong";
    public static final String RESOURCE_NAME_ON_CLOSE = "onClose";
    public static final String RESOURCE_NAME_ON_IDLE_TIMEOUT = "onIdleTimeout";
    public static final String RESOURCE_NAME_ON_ERROR = "onError";

    public static final String WEBSOCKET_MESSAGE = "WEBSOCKET_MESSAGE";

    public static final String NATIVE_DATA_WEBSOCKET_CONNECTION_INFO = "NATIVE_DATA_WEBSOCKET_CONNECTION_INFO";

    public static final String NATIVE_DATA_QUERY_PARAMS = "NATIVE_DATA_QUERY_PARAMS";

    public static final String CLIENT_URL_CONFIG = "url";
    public static final String CLIENT_SERVICE_CONFIG = "callbackService";
    public static final String CLIENT_SUB_PROTOCOLS_CONFIG = "subProtocols";
    public static final String CLIENT_CUSTOM_HEADERS_CONFIG = "customHeaders";
    public static final String CLIENT_IDLE_TIMOUT_CONFIG = "idleTimeoutInSeconds";
    public static final String CLIENT_READY_ON_CONNECT = "readyOnConnect";
    public static final String CLIENT_CONNECTOR_CONFIGS = "clientEndpointConfigs";
    public static final String WEBSOCKET_UPGRADE_SERVICE_CONFIG = "upgradeService";

    // WebSocketListener struct field names
    public static final String LISTENER_ID_FIELD = "id";
    public static final String LISTENER_NEGOTIATED_SUBPROTOCOLS_FIELD = "negotiatedSubProtocol";
    public static final String LISTENER_IS_SECURE_FIELD = "isSecure";
    public static final String LISTENER_IS_OPEN_FIELD = "isOpen";
    public static final String LISTENER_ATTRIBUTES_FIELD = "attributes";
    public static final String LISTENER_CONFIG_FIELD = "config";
    public static final String LISTENER_CONNECTOR_FIELD = "conn";
    public static final int LISTENER_HTTP_ENDPOINT_FIELD = 3;

    // WebSocketClient struct field names
    public static final String CLIENT_RESPONSE_FIELD = "response";
    public static final String CLIENT_CONNECTOR_FIELD = "conn";

    // WebSocketConnector
    public static final String CONNECTOR_IS_READY_FIELD = "isReady";

    public static final int STATUS_CODE_ABNORMAL_CLOSURE = 1006;
    public static final int STATUS_CODE_FOR_NO_STATUS_CODE_PRESENT = 1005;

    private WebSocketConstants() {
    }
}
