package org.xbib.net.http.server.persist.memory;

import org.xbib.net.http.server.persist.Codec;

import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryPropertiesCodec implements Codec<Map<String, Object>> {

    private final ReentrantReadWriteLock lock;

    private static final Map<String, Object> store = new HashMap<>();

    public MemoryPropertiesCodec() {
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Map<String, Object> create(String key) throws IOException {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<String, Object> read(String key) throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            Properties properties = new Properties();
            properties.putAll((Map<?, ?>) store.get(key));
            return toMap(properties);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String key, Map<String, Object> data) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            Properties properties = toProperties(data);
            store.put(key, properties);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    @Override
    public void purge(long expiredAfterSeconds) {
        // unable to purge
    }

    private Map<String, Object> toMap(Properties properties) {
        Map<String, Object> map = new LinkedHashMap<>();
        properties.forEach((k, v) -> map.put(k.toString(), v));
        return map;
    }

    private Properties toProperties(Map<String, Object> map) {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
