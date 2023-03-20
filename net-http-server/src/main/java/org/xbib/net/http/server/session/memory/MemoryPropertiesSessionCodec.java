package org.xbib.net.http.server.session.memory;

import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.BaseSession;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.SessionListener;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryPropertiesSessionCodec implements Codec<Session> {

    private static final Map<String, Object> store = new HashMap<>();

    private final String name;

    private final ReentrantReadWriteLock lock;

    private final SessionListener sessionListener;

    private final int sessionCacheSize;

    private final Duration sessionDuration;

    public MemoryPropertiesSessionCodec(String name,
                                        SessionListener sessionListener,
                                        int sessionCacheSize,
                                        Duration sessionDuration) {
        this.name = name;
        this.sessionListener = sessionListener;
        this.sessionCacheSize = sessionCacheSize;
        this.sessionDuration = sessionDuration;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Session create(String key) throws IOException {
        return new BaseSession(sessionListener, sessionCacheSize, name, key, true, sessionDuration);
    }

    @Override
    public Session read(String key) throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            Properties properties = new Properties();
            if (store.containsKey(key)) {
                properties.putAll((Map<?, ?>) store.get(key));
            }
            return toSession(key, properties);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String key, Session session) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            Properties properties = toProperties(session);
            store.put(key, properties);
        } finally {
            writeLock.unlock();
        }
    }

    private Session toSession(String key, Properties properties) {
        Session session = new BaseSession(sessionListener, sessionCacheSize, name, key, false, sessionDuration);
        properties.forEach((k, v) -> session.put(k.toString(), v));
        return session;
    }

    private Properties toProperties(Map<String, Object> map) {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    @Override
    public void remove(String key) throws IOException {
        store.remove(key);
    }

    @Override
    public void purge(long expiredAfterSeconds) throws IOException {
        if (expiredAfterSeconds > 0L) {
            for (Map.Entry<String, Object> entry : store.entrySet()) {
                Session session = toSession(entry.getKey(), (Properties) entry.getValue());
                if (session.isExpired()) {
                    remove(entry.getKey());
                }
            }
        }
    }
}
