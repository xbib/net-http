package org.xbib.net.http.server.persist.file;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.xbib.net.http.server.persist.Codec;

public class FilePropertiesCodec implements Codec<Map<String, Object>> {

    private final ReentrantReadWriteLock lock;

    private final String root;

    public FilePropertiesCodec(String root) {
        this.root = root;
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
            Path path = openOrCreate(key);
            Properties properties = new Properties();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    properties.load(reader);
                }
            }
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
            Path path = openOrCreate(key);
            Properties properties = toProperties(data);
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, null);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        try {
            Path path = openOrCreate(key);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            //
        }
    }

    @Override
    public void purge(long expiredAfterSeconds) {
        // unable to purge
    }

    private Path openOrCreate(String key) throws IOException {
        Path path = Paths.get(root);
        Files.createDirectories(path);
        return path.resolve(key);
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
