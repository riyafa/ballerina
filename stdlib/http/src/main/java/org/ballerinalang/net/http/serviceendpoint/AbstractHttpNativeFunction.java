package org.ballerinalang.net.http.serviceendpoint;

import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.net.http.HTTPServicesRegistry;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.WebSocketServicesRegistry;
import org.wso2.transport.http.netty.contract.ServerConnector;

/**
 * Includes common functions to all the actions.
 *
 * @since 0.966
 */
public abstract class AbstractHttpNativeFunction extends BlockingNativeCallableUnit {

    protected HTTPServicesRegistry getHttpServicesRegistry(Struct serviceEndpoint) {
        return (HTTPServicesRegistry) serviceEndpoint.getNativeData(HttpConstants.HTTP_SERVICE_REGISTRY);
    }

    protected void resetRegistry(Struct serviceEndpoint) {
        WebSocketServicesRegistry webSocketServicesRegistry = new WebSocketServicesRegistry();
        HTTPServicesRegistry httpServicesRegistry = new HTTPServicesRegistry(webSocketServicesRegistry);
        serviceEndpoint.addNativeData(HttpConstants.HTTP_SERVICE_REGISTRY, httpServicesRegistry);
        serviceEndpoint.addNativeData(HttpConstants.WS_SERVICE_REGISTRY, webSocketServicesRegistry);
    }
}
