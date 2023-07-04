package org.xbib.net.http.server.application.web.j2html;

import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.NetworkClass;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.j2html.J2HtmlResourceHandler;
import org.xbib.net.http.j2html.J2HtmlService;
import org.xbib.net.http.server.application.web.WebApplication;
import org.xbib.net.http.server.auth.BasicAuthenticationHandler;
import org.xbib.net.http.server.auth.FormAuthenticationHandler;
import org.xbib.net.http.server.domain.BaseHttpDomain;
import org.xbib.net.http.server.domain.BaseHttpSecurityDomain;
import org.xbib.net.http.server.domain.HttpSecurityDomain;
import org.xbib.net.http.server.executor.BaseExecutor;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.ldap.LdapContextFactory;
import org.xbib.net.http.server.ldap.LdapGroupMapping;
import org.xbib.net.http.server.ldap.LdapRealm;
import org.xbib.net.http.server.ldap.LdapUserMapping;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.buffer.NettyDataBufferFactory;
import org.xbib.net.http.server.netty.secure.HttpsAddress;
import org.xbib.net.http.server.netty.secure.HttpsRequest;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.resource.ClassLoaderResourceHandler;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.settings.Settings;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public final class Bootstrap {

    private static final ConfigLogger bootLogger;

    static {
        // early loading of boot logger during static initialization block
        ServiceLoader<ConfigLogger> serviceLoader = ServiceLoader.load(ConfigLogger.class);
        Optional<ConfigLogger> optionalBootLogger = serviceLoader.findFirst();
        bootLogger = optionalBootLogger.orElse(new SystemConfigLogger());
    }

    private Bootstrap() {
    }

    public static void main(String[] args) throws Exception {
        String profile = args.length > 0 ? args[0] : System.getProperty("application.profile");
        ConfigParams configParams;
        ConfigLoader configLoader;
        Settings settings;
        configParams = new ConfigParams()
                .withArgs(args)
                .withDirectoryName("application")
                .withFileNamesWithoutSuffix(profile)
                .withSystemEnvironment()
                .withSystemProperties();
        configLoader = ConfigLoader.getInstance()
                .withLogger(bootLogger);
        settings = configLoader.load(configParams);
        int rc = 1;
        try {
            rc = runApplication(settings);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            // always hard-exit the JVM, maybe there are threads hanging
            System.exit(rc);
        }
    }

    private static int runApplication(Settings settings) throws Exception {
        HttpsAddress httpsAddress = HttpsAddress.builder()
                .setSecure(true)
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost("localhost")
                .setPort(8443)
                .setSelfCert("localhost")
                .build();
        NettyHttpsServerConfig serverConfig = new NettyHttpsServerConfig();
        serverConfig.setServerName("WebApplication", Bootstrap.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.SITE);
        serverConfig.setDebug(true);

        Map<String, LdapContextFactory> contextFactories = new HashMap<>();
        LdapContextFactory contextFactory = new LdapContextFactory("simple",
                "com.sun.jndi.ldap.LdapCtxFactory",
                null,
                "ldap://localhost:1389",
                false,
                null,
                null,
                "follow"
        );
        contextFactories.put("default", contextFactory);
        Map<String, LdapUserMapping> userMappings = new HashMap<>();
        LdapUserMapping userMapping = new LdapUserMapping("ou=People,dc=example.org",
                "(&(objectclass=posixAccount)(uid:caseExactMatch:={0}))",
                "uid",
                "cn"
        );
        userMappings.put("default", userMapping);
        Map<String, LdapGroupMapping> groupMappings = new HashMap<>();
        LdapGroupMapping groupMapping = new LdapGroupMapping("ou=group,dc=example.org",
                "cn",
                "(&(objectclass=posixGroup)(memberUid:caseExactMatch:={0}))",
                new String[] { "uid" }
        );
        groupMappings.put("default", groupMapping);
        LdapRealm ldapRealm = new LdapRealm("Web Application Realm", contextFactories, userMappings, groupMappings);

        BasicAuthenticationHandler basicAuthenticationHandler =
                new BasicAuthenticationHandler(ldapRealm);

        FormAuthenticationHandler formAuthenticationHandler =
                new FormAuthenticationHandler("j_username", "j_password", "j_remember",
                        "demo/auth/form/index.gtpl", ldapRealm);

        HttpSecurityDomain securityDomain = BaseHttpSecurityDomain.builder()
                .setSecurityRealm(ldapRealm)
                .setHandlers(formAuthenticationHandler)
                .build();

        HttpService httpService = BaseHttpService.builder()
                .setPath("/secure")
                .setHandler(ctx -> {
                    ctx.status(HttpResponseStatus.OK)
                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                            .charset(StandardCharsets.UTF_8)
                            .body("secure domain: " +
                            " SNI host = " + ctx.getRequest().as(HttpsRequest.class).getSNIHost() +
                            " SSL session = " + ctx.getRequest().as(HttpsRequest.class).getSSLSession() +
                            " base URL = " + ctx.getRequest().getBaseURL() +
                            " parameter = " + ctx.getRequest().getParameter().allToString() +
                            " attributes = " + ctx.getAttributes() +
                            " local address = " + ctx.getRequest().getLocalAddress() +
                            " remote address = " + ctx.getRequest().getRemoteAddress());
                    ctx.done();
                })
                .build();

        HttpRouter httpRouter = BaseHttpRouter.builder()
                .setHandler(500, new InternalServerErrorHandler())
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpsAddress)
                        .addService(BaseHttpService.builder()
                                .setPath("/favicon.ico")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, "image/x-icon")
                                            .body(NettyDataBufferFactory.getInstance().wrap(fromHex(hexFavIcon)))
                                            .done();
                                })
                                .build())
                        .addService(BaseHttpService.builder()
                                .setPath("/webjars/**")
                                .setHandler(new ClassLoaderResourceHandler(Bootstrap.class.getClassLoader(), "META-INF/resources/"))
                                .build())
                        .addService(httpService)
                        .addService(J2HtmlService.builder()
                                .setPath("glob:**")
                                .setHandler(new J2HtmlResourceHandler())
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        WebApplication application = WebApplication.builder()
                .setSettings(settings)
                .setSecret("1088e6b7ad58d64d09961e1357bf95544447051c6ad1332cd626e3a33bb5786b")
                .setExecutor(executor)
                .setRouter(httpRouter)
                .build();

        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(application)
                .build()) {
            server.bind();
            server.loop();
        }

        return 0;
    }


    private static final String hexFavIcon =
            "000001000100101000000100200068040000160000002800000010000000" +
                    "200000000100200000000000000000000000000000000000000000000000" +
                    "000099330000993300009933000099330000993300009933000099330000" +
                    "993300009933000099330000993300009933000099330000993300009933" +
                    "00009933000099330000993300009933000099330000993300ff993300ff" +
                    "993300ff993300ff993300ff993300ff993300ff993300ff993300ff9933" +
                    "0000993300009933000099330000993300009933000099330000993300ff" +
                    "993300ff993300ff993300ff993300ff993300ff993300ff993300ff9933" +
                    "00ff99330000993300009933000099330000993300009933000099330000" +
                    "993300ff9933000099330000993300009933000099330000993300009933" +
                    "00009933000099330000993300009933000099330000993300ff993300ff" +
                    "99330000993300ff99330000993300ff993300ff993300ff993300ff9933" +
                    "00ff993300ff993300ff993300ff993300ff9933000099330000993300ff" +
                    "993300ff99330000993300ff99330000993300ff993300ff993300ff9933" +
                    "00ff993300ff993300ff993300ff993300ff993300ff9933000000000000" +
                    "993300ff993300ff00000000993300ff0000000000000000000000000000" +
                    "00000000000000000000000000000000000099330000993300ff99330000" +
                    "00000000993300ff993300ff00000000993300ff00000000000000000000" +
                    "0000000000000000000000000000993300ff993300ff00000000993300ff" +
                    "0000000000000000993300ff993300ff00000000993300ff993300009933" +
                    "000099330000993300009933000099330000993300ff993300ff99330000" +
                    "993300ff9933000000000000993300ff993300ff00000000993300ff9933" +
                    "00009933000099330000993300009933000099330000993300ff993300ff" +
                    "99330000993300ff9933000000000000993300ff993300ff000000009933" +
                    "00ff993300009933000099330000993300009933000099330000993300ff" +
                    "993300ff99330000993300ff9933000000000000993300ff993300ff0000" +
                    "000099330000993300009933000099330000993300009933000099330000" +
                    "993300ff993300ff99330000993300ff9933000000000000993300ff9933" +
                    "00ff00000000993300ff993300ff993300ff993300ff993300ff993300ff" +
                    "993300ff993300ff993300ff99330000993300ff99330000000000009933" +
                    "00ff993300ff000000009933000099330000993300009933000099330000" +
                    "993300009933000099330000993300009933000099330000993300000000" +
                    "0000993300ff993300ff993300ff993300ff993300ff993300ff993300ff" +
                    "993300ff993300ff993300ff993300ff993300ff99330000993300009933" +
                    "000000000000000000000000000000000000993300009933000099330000" +
                    "993300009933000099330000993300009933000099330000993300009933" +
                    "000099330000ffff0000f0070000f0070000f7ff00009401000094010000" +
                    "97fd000097e5000097e5000097e5000097e500009fe50000900500009fff" +
                    "000080070000ffff0000";


    private static byte[] fromHex(String hex) {
        Objects.requireNonNull(hex);
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) fromHex(hex.charAt(i), hex.charAt(i + 1));
        }
        return data;
    }

    private static int fromHex(int b1, int b2) {
        int i1 = Character.digit(b1, 16);
        if (i1 == -1) {
            throw new IllegalArgumentException("invalid character in hexadecimal: " + b1);
        }
        int i2 = Character.digit(b2, 16);
        if (i2 == -1) {
            throw new IllegalArgumentException("invalid character in hexadecimal: " + b2);
        }
        return (i1 << 4) + i2;
    }
}
