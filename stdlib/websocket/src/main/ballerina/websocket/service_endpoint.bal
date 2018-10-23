// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.


# Provides a set of configurations for HTTP service endpoints.
#
# + host - The host name/IP of the endpoint
# + port - The port to which the endpoint should bind to
# + secureSocket - The SSL configurations for the service endpoint. This needs to be configured in order to
#                  communicate through HTTPS.
# + timeoutMillis - Period of time in milliseconds that a connection waits for a read/write operation. Use value 0 to
#                   disable timeout
public type ServiceEndpointConfiguration record {
    string host;
    int port;
    ServiceSecureSocket? secureSocket;
    int timeoutMillis = DEFAULT_LISTENER_TIMEOUT;
    !...
};

# Configures the SSL/TLS options to be used for HTTP service.
#
# + trustStore - Configures the trust store to be used
# + keyStore - Configures the key store to be used
# + certFile - A file containing the certificate of the server
# + keyFile - A file containing the private key of the server
# + keyPassword - Password of the private key if it is encrypted
# + trustedCertFile - A file containing a list of certificates or a single certificate that the server trusts
# + protocol - SSL/TLS protocol related options
# + certValidation - Certificate validation against CRL or OCSP related options
# + ciphers - List of ciphers to be used (e.g.: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
#             TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA)
# + sslVerifyClient - The type of client certificate verification
# + shareSession - Enable/disable new SSL session creation
# + ocspStapling - Enable/disable OCSP stapling
public type ServiceSecureSocket record {
    TrustStore? trustStore;
    KeyStore? keyStore;
    string certFile;
    string keyFile;
    string keyPassword;
    string trustedCertFile;
    Protocols? protocol;
    ValidateCert? certValidation;
    string[] ciphers;
    string sslVerifyClient;
    boolean shareSession = true;
    ServiceOcspStapling? ocspStapling;
    !...
};

# A record for providing trust store related configurations.
#
# + path - Path to the trust store file
# + password - Trust store password
public type TrustStore record {
    string path;
    string password;
    !...
};

# A record for providing key store related configurations.
#
# + path - Path to the key store file
# + password - Key store password
public type KeyStore record {
    string path;
    string password;
    !...
};

# A record for configuring SSL/TLS protocol and version to be used.
#
# + name - SSL Protocol to be used (e.g.: TLS1.2)
# + versions - SSL/TLS protocols to be enabled (e.g.: TLSv1,TLSv1.1,TLSv1.2)
public type Protocols record {
    string name;
    string[] versions;
    !...
};

# A record for providing configurations for certificate revocation status checks.
#
# + enable - The status of `validateCertEnabled`
# + cacheSize - Maximum size of the cache
# + cacheValidityPeriod - The time period for which a cache entry is valid
public type ValidateCert record {
    boolean enable;
    int cacheSize;
    int cacheValidityPeriod;
    !...
};

# A record for providing configurations for certificate revocation status checks.
#
# + enable - The status of OCSP stapling
# + cacheSize - Maximum size of the cache
# + cacheValidityPeriod - The time period for which a cache entry is valid
public type ServiceOcspStapling record {
    boolean enable;
    int cacheSize;
    int cacheValidityPeriod;
    !...
};

//////////////////////////////////
/// WebSocket Service Endpoint ///
//////////////////////////////////
# Represents a WebSocket service endpoint.
#
# + id - The connection ID
# + negotiatedSubProtocol - The subprotocols negotiated with the client
# + isSecure - `true` if the connection is secure
# + isOpen - `true` if the connection is open
# + attributes - A `map` to store connection related attributes
public type Listener object {

    @readonly public string id;
    @readonly public string negotiatedSubProtocol;
    @readonly public boolean isSecure;
    @readonly public boolean isOpen;
    @readonly public map attributes;

    private WebSocketConnector conn;
    private ServiceEndpointConfiguration config;

    public new() {
    }

    # Gets invoked during package initialization to initialize the endpoint.
    #
    # + c - The `ServiceEndpointConfiguration` of the endpoint
    public function init(ServiceEndpointConfiguration c) {
        self.config = c;
        var err = self.initEndpoint();
        if (err != null) {
            throw err;
        }
    }

    public extern function initEndpoint() returns error;

    # Gets invoked when binding a service to the endpoint.
    #
    # + serviceType - The type of the service to be registered
    public extern function register(typedesc serviceType);

    # Returns a WebSocket actions provider which can be used to communicate with the remote host.
    #
    # + return - The connector that listener endpoint uses
    public function getCallerActions() returns (WebSocketConnector) {
        return conn;
    }

    # Stops the registered service.
    public function stop() {
        WebSocketConnector webSocketConnector = getCallerActions();
        check webSocketConnector.close(statusCode = 1001, reason = "going away", timeoutInSecs = 0);
        stopEndpoint();
    }

    public extern function stopEndpoint();
};
