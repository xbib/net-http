package org.xbib.net.http.server.session.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.BaseSession;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.SessionListener;
import org.xbib.net.util.JsonUtil;

public class JdbcSessionCodec implements Codec<Session>, Closeable {

    private final SessionListener sessionListener;

    private final int sessionCacheSize;

    private final Duration sessionDuration;

    private final DataSource dataSource;

    private final String readSessionStringStatement;

    private final String writeSessionStringStatement;

    private final String deleteSessionStringStatement;

    private final String purgeSessionStringStatement;

    private final ScheduledExecutorService scheduledExecutorService;

    public JdbcSessionCodec(HttpServerContext httpServerContext,
                            SessionListener sessionListener,
                            int sessionCacheSize,
                            Duration sessionDuration,
                            DataSource dataSource,
                            String readSessionStringStatement,
                            String writeSessionStringStatement,
                            String deleteSessionStringStatement,
                            String purgeSessionStringStatement) {
        this.sessionListener = sessionListener;
        this.sessionCacheSize = sessionCacheSize;
        this.sessionDuration = sessionDuration;
        this.dataSource = dataSource;
        this.readSessionStringStatement = readSessionStringStatement;
        this.writeSessionStringStatement = writeSessionStringStatement;
        this.deleteSessionStringStatement = deleteSessionStringStatement;
        this.purgeSessionStringStatement = purgeSessionStringStatement;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.schedule(() -> purgeDatabase(sessionDuration.getSeconds()), 0L, TimeUnit.SECONDS);
    }

    @Override
    public Session create(String key) throws IOException {
        return new BaseSession(sessionListener, sessionCacheSize, key, true, sessionDuration);
    }

    @Override
    public Session read(String key) throws IOException {
        Session session;
        try {
            Map<String, Object> map = JsonUtil.toMap(readString(key));
            if (map != null) {
                session = new BaseSession(sessionListener, sessionCacheSize, key, false, sessionDuration);
                session.putAll(map);
                return session;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Override
    public void write(String key, Session session) throws IOException {
        if (session != null) {
            try {
                writeString(key, JsonUtil.toString(session));
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void remove(String key) throws IOException {
        if (key != null) {
            try {
                deleteString(key);
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void purge(long expiredAfterSeconds) {
        if (expiredAfterSeconds > 0L) {
            purgeDatabase(expiredAfterSeconds);
        }
    }

    private String readString(String key) throws SQLException {
        List<String> list = new ArrayList<>();
        Connection connection = dataSource.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(readSessionStringStatement)) {
            preparedStatement.setString(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            resultSet.close();
        }
        return list.size() > 1 ? list.get(0) : null;
    }

    private void writeString(String key, String value) throws SQLException {
        Connection connection = dataSource.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(writeSessionStringStatement)) {
            preparedStatement.setString(1, key); // key
            preparedStatement.setString(2, value); // value
            preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now())); // created
            preparedStatement.execute();
        }
    }

    private void deleteString(String key) throws SQLException {
        Connection connection = dataSource.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSessionStringStatement)) {
            preparedStatement.setString(1, key);
            preparedStatement.execute();
        }
    }

    private void purgeDatabase(long seconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredBefore = now.minusSeconds(seconds);
        List<String> list = new ArrayList<>();
        try {
            Connection connection = dataSource.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(purgeSessionStringStatement)) {
                preparedStatement.setTimestamp(1, Timestamp.valueOf(expiredBefore)); // created
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    list.add(resultSet.getString(1));
                }
                resultSet.close();
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSessionStringStatement)) {
                for (String key : list) {
                    if (key != null) {
                        preparedStatement.setString(1, key);
                        preparedStatement.execute();
                    }
                }
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public void close() throws IOException {
        this.scheduledExecutorService.shutdown();
    }
}
