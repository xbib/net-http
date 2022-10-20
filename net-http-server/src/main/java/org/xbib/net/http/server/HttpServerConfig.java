package org.xbib.net.http.server;

import org.xbib.net.NetworkClass;

import java.util.Optional;

public class HttpServerConfig {

    private String serverName = null;

    private NetworkClass networkClass = NetworkClass.LOOPBACK;

    /**
     * If frame logging/traffic logging is enabled or not.
     */
    private boolean debug = false;

    /**
     * Default timeout in milliseconds.
     */
    private int timeoutMillis = 30000;

    public HttpServerConfig() {
    }

    public HttpServerConfig setServerName(String serverName, String serverVendor) {
        if (serverName == null) {
            serverName = "HttpServer";
        }
        if (serverVendor == null) {
            serverVendor = "unknown";
        }
        this.serverName = String.format("%s/%s/%s Java/%s/%s/%s OS/%s/%s/%s",
                serverName, serverVendor, serverVersion(),
                System.getProperty("java.vm.name"), System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
        return this;
    }

    public String getServerName() {
        return serverName;
    }

    public HttpServerConfig setNetworkClass(NetworkClass networkClass) {
        this.networkClass = networkClass;
        return this;
    }

    public NetworkClass getNetworkClass() {
        return networkClass;
    }

    public HttpServerConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public HttpServerConfig setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    private static String serverVersion() {
        return Optional.ofNullable(HttpServerConfig.class.getPackage().getImplementationVersion())
                .orElse("unknown");
    }
}
