package org.xbib.net.http.server.application.database;

import org.xbib.jdbc.connection.pool.PoolConfig;
import org.xbib.jdbc.connection.pool.PoolDataSource;
import org.xbib.jdbc.query.DatabaseProvider;
import org.xbib.jdbc.query.Flavor;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.application.BaseApplicationModule;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.settings.Settings;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseApplicationModule extends BaseApplicationModule {

    private static final Logger logger = Logger.getLogger(DatabaseApplicationModule.class.getName());

    private final DataSource dataSource;

    private final DatabaseProvider databaseProvider;

    public DatabaseApplicationModule(Application application, String name, Settings settings) {
        super(application, name, settings);
        this.dataSource = createDataSource();
        String flavor = System.getProperty("database.flavor");
        this.databaseProvider = flavor != null ?
                DatabaseProvider.builder(dataSource, Flavor.valueOf(flavor)).build() : null;
    }

    @Override
    public void onOpen(HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest) {
        if (dataSource != null) {
            httpServerContext.getAttributes().put("datasource", dataSource);
        }
        if (databaseProvider != null) {
            httpServerContext.getAttributes().put("databaseprovider", databaseProvider);
        }
    }

    private DataSource createDataSource() {
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
        try {
            return new PoolDataSource(config);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
