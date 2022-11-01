package org.xbib.net.http.server.application.database;

import org.xbib.jdbc.connection.pool.PoolConfig;
import org.xbib.jdbc.connection.pool.PoolDataSource;
import org.xbib.jdbc.query.DatabaseProvider;
import org.xbib.jdbc.query.Flavor;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.BaseApplicationModule;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.HttpService;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseApplicationModule extends BaseApplicationModule {

    private static final Logger logger = Logger.getLogger(DatabaseApplicationModule.class.getName());

    private DataSource dataSource;

    private DatabaseProvider databaseProvider;

    public DatabaseApplicationModule() {
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public void onOpen(Application application) throws Exception {
        this.dataSource = createDataSource();
        String flavor = System.getProperty("database.flavor");
        this.databaseProvider = flavor != null ?
                DatabaseProvider.builder(dataSource, Flavor.valueOf(flavor)).build() : null;
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService) {
        if (dataSource != null) {
            httpServerContext.attributes().put("datasource", dataSource);
        }
        if (databaseProvider != null) {
            httpServerContext.attributes().put("databaseprovider", databaseProvider);
        }
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest) {
        // nothing
    }

    private DataSource createDataSource() throws Exception {
        Properties properties = new Properties();
        System.getProperties().forEach((key, value) -> {
            if (key.toString().startsWith("database.") && !key.toString().equals("database.flavor")) {
                properties.setProperty(key.toString().substring(9), value.toString());
            }
        });
        if (!properties.containsKey("url")) {
            throw new IllegalArgumentException("no database.url in system properties given");
        }
        if (!properties.containsKey("user")) {
            logger.log(Level.WARNING, "no database.user in system properties given");
        }
        PoolConfig config = new PoolConfig(properties);
        config.setUrl(properties.getProperty("url"));
        config.setPoolName("net-http-database");
        config.setMaximumPoolSize(getAsInt(properties, "poolsize", 4));
        config.setMaxLifetime(getAsLong(properties, "maxlifetime", 600L * 1000L)); // 10 minutes
        config.setConnectionTimeout(getAsLong(properties, "timeout", 15L * 1000L)); // 15 seconds
        config.setHousekeepingPeriodMs(getAsLong(properties, "housekeeping", 600L * 1000L)); // 10 minutes
        config.setAutoCommit(getAsBoolean(properties, "autocommit", true));
        return new PoolDataSource(config);
    }

    private int getAsInt(Properties properties, String key, int defaultValue) {
        if (properties.containsKey(key)) {
            return Integer.parseInt(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    private long getAsLong(Properties properties, String key, long defaultValue) {
        if (properties.containsKey(key)) {
            return Long.parseLong(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    private boolean getAsBoolean(Properties properties, String key, boolean defaultValue) {
        if (properties.containsKey(key)) {
            return Boolean.parseBoolean(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }
}
