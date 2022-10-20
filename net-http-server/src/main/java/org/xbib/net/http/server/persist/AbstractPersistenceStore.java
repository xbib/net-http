package org.xbib.net.http.server.persist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("serial")
public abstract class AbstractPersistenceStore extends LinkedHashMap<String, Object> implements PersistenceStore<String, Object> {

    private final ReentrantReadWriteLock lock;

    final Codec<Map<String, Object>> codec;

    final String storeName;

    public AbstractPersistenceStore(Codec<Map<String, Object>> codec,
                                    String storeName) {
        super();
        this.codec = codec;
        this.storeName = storeName;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Codec<Map<String, Object>> getCodec() {
        return codec;
    }

    @Override
    public void load() throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            clear();
            Map<String, Object> map = codec.read(storeName);
            if (map != null) {
                putAll(map);
            }
        } finally {
            readLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insertValue(String key, Object value) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            putIfAbsent(key, new ArrayList<>());
            List<Object> list = (List<Object>) get(key);
            list.add(value);
            put(key, list);
            codec.write(storeName, this);
        } finally {
            writeLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeValue(String key, Object value) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            putIfAbsent(key, new ArrayList<>());
            List<Object> list = (List<Object>) get(key);
            list.remove(value);
            put(key, list);
            codec.write(storeName, this);
        } finally {
            writeLock.unlock();
        }
    }
}
