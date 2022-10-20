package org.xbib.net.http.server.application.web;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.NetworkClass;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.BaseHttpSecurityDomain;
import org.xbib.net.http.server.HttpSecurityDomain;
import org.xbib.net.http.server.HttpService;
import org.xbib.net.http.server.auth.BasicAuthenticationHandler;
import org.xbib.net.http.server.auth.FormAuthenticationHandler;
import org.xbib.net.http.server.ldap.LdapContextFactory;
import org.xbib.net.http.server.ldap.LdapGroupMapping;
import org.xbib.net.http.server.ldap.LdapRealm;
import org.xbib.net.http.server.ldap.LdapUserMapping;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.BaseHttpService;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.buffer.NettyDataBufferFactory;
import org.xbib.net.http.server.netty.secure.HttpsAddress;
import org.xbib.net.http.server.netty.secure.HttpsRequest;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.resource.ClassLoaderResourceHandler;
import org.xbib.net.http.template.groovy.GroovyInternalServerErrorHandler;
import org.xbib.net.http.template.groovy.GroovyHttpStatusHandler;
import org.xbib.net.http.template.groovy.GroovyTemplateResourceHandler;
import org.xbib.net.http.template.groovy.GroovyTemplateService;
import org.xbib.net.mime.stream.Hex;
import org.xbib.settings.Settings;

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
                    ctx.response()
                            .setResponseStatus(HttpResponseStatus.OK)
                            .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                            .setCharset(StandardCharsets.UTF_8);
                    ctx.write("secure domain: " +
                            " SNI host = " + ctx.httpRequest().as(HttpsRequest.class).getSNIHost() +
                            " SSL session = " + ctx.httpRequest().as(HttpsRequest.class).getSSLSession() +
                            " base URL = " + ctx.httpRequest().getBaseURL() +
                            " parameter = " + ctx.httpRequest().getParameter().allToString() +
                            " attributes = " + ctx.attributes() +
                            " local address = " + ctx.httpRequest().getLocalAddress() +
                            " remote address = " + ctx.httpRequest().getRemoteAddress());
                    ctx.done();
                })
                .build();

        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(WebApplication.builder()
                        .setSettings(settings)
                        .setSecret("1088e6b7ad58d64d09961e1357bf95544447051c6ad1332cd626e3a33bb5786b")
                        .setRouter(BaseHttpRouter.builder()
                                .setHandler(400, new GroovyHttpStatusHandler(HttpResponseStatus.BAD_REQUEST, "Bad request", "400.gtpl"))
                                .setHandler(401, new GroovyHttpStatusHandler(HttpResponseStatus.UNAUTHORIZED, "Unauthorized", "401.gtpl"))
                                .setHandler(403, new GroovyHttpStatusHandler(HttpResponseStatus.FORBIDDEN, "Forbidden", "403.gtpl"))
                                .setHandler(404, new GroovyHttpStatusHandler(HttpResponseStatus.NOT_FOUND, "Not found", "404.gtpl"))
                                .setHandler(500, new GroovyInternalServerErrorHandler("500.gtpl"))
                                .addDomain(BaseHttpDomain.builder()
                                        .setHttpAddress(httpsAddress)
                                        .addService(BaseHttpService.builder()
                                                .setPath("/favicon.ico")
                                                .setHandler(ctx -> {
                                                    ctx.response()
                                                            .setResponseStatus(HttpResponseStatus.OK)
                                                            .setHeader(HttpHeaderNames.CONTENT_TYPE, "image/x-icon")
                                                            .write(NettyDataBufferFactory.getInstance().wrap(Hex.fromHex(hexFavIcon)))
                                                            .build();
                                                    ctx.done();
                                                })
                                                .build())
                                        .addService(BaseHttpService.builder()
                                                .setPath("/webjars/**")
                                                .setHandler(new ClassLoaderResourceHandler(Bootstrap.class.getClassLoader(), "META-INF/resources/"))
                                                .build())
                                        .addService(httpService)
                                        .addService(GroovyTemplateService.builder()
                                                .setTemplateName("index.gtpl")
                                                .setSecurityDomain(securityDomain)
                                                .setPath("glob:**")
                                                .setHandler(new GroovyTemplateResourceHandler())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build()) {
            server.bind();
            server.loop();
        }
        return 0;
    }


    private static final String hexFavIcon =
            "00000100010010100000010020006804000016000000280000001000000020000000010020000000000040040000130b0000130b0000000000000000000000000000000000000000000000000000000000005140322f62524a536050475f5140322f5140320c000000000000000000000000000000000000000000000000000000000000000000000000fffffe40b1a9a5c76f605bff6f605bff6f605bff6f605bff6b5b55f38f7d71ff978473bf877465100000000000000000000000000000000000000000fffffe8ff2eeeafff2f0eeffb7b0adff786a65ff6f605bff6f605bff6f605bff6f605bff72625af77f6d60bf00000000000000000000000000000000fffffe8fd9ccc0fffbfaf9fffbfaf8fffbfaf8fff6f5f5ff9c928eff6f605bff786a65ff6f605bff6f605bff716159ff6d5d4f9f0000000000000000fffffe40e7e0d8ffcfbfb2ffc4b2a0ffcebdafffccbbabffe1d6ceffe7ded7ffd2cdccff786a65ff6f605bff6f605bff6c5c54ff6a5a4ffb6050425000000000faf9f6afd3c5b8ffd4c6baffcfc1b2ffd5c7bbffcab9a9ffe7ded7fff2f0efff786a65ff6f605bff6f605bff6f605bff6c5c53ff6b5b53fb5d4d3fdf00000000e9e2dbffd8ccc0ffdcd1c5ffdcd1c6ffd4c7b9ffdacec3ffffffffffd2cdccff6f605bff6f605bff6f605bff6f605bff6a5a4fff6b5b52ff5b4b3eef53443660ece7e0ffdbd0c5ffe0d6ccffe9e2d9ffe3dad1fff1ece8ffffffffffc0b9b7ff6f605bff6f605bff6f605bff6f605bff685749ff6a5a4fff5e4e43cf5142348fefe9e3ffe0d7ccffe9e1d9ffeae3dbffe3dbd0fff4f0ecffffffffffdbd7d6ff6f605bff6f605bff6f605bff6d5e56ff675648ff6a5a4fff5e4f44af514233bfefeae4ffe8e0d7ffebe5ddffede8dfffebe5dcfff2eee8ffffffffffffffffff817470ff6f605bff6f605aff69594dff685749ff6b5b52ff5c4d429f5142338ff9f7f4afeae3dbffe7e1d6ffece7ddffefebe3fff1ece7ffffffffffffffffffdbd7d6ff6f605bff6a5a4fff68574bff6a5a50ff6b5b54f7554638574f403150fffffe40f1ede7ffece7dfffebe5dcffeae3dbfffbf9f7fffffffffffffffffff8f5f3ff83756dff69584dff69584cff6b5b52ff675750af000000004e3f304000000000fcfbf99fede7e0ffe4dbd1fff6f3effffffffffff9f8f6ffe1d7ceffdbcfc5ff9b8a7cff68574aff6a594eff6b5b54e751403218000000004e3f30100000000000000000fbfaf89fece6dfffeee9e3ffe4dbd2ffd8cbbfffd8ccc0ffcdbcadff6f5e52ff6a5a50ff6b5b54e75d4d4328000000000000000000000000000000000000000000000000fffffe60f6f4f0cff1ece7ffe2d9d0ffdbcec3ffccc1b7ff6a5a52f7675750af51403218000000000000000000000000000000000000000000000000000000000000000000000000fffffe10fffffe40fffffe40fffffe205140320c000000000000000000000000000000000000000000000000f83f9407e0070000c0079807800300000001603f0001603f0000603f0000603f0000e13e0000e23e0000e23e0002e23e8002e23ec007e33ee00fe33ef83fe33";
}
