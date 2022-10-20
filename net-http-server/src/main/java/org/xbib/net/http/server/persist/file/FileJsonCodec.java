package org.xbib.net.http.server.persist.file;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.util.JsonUtil;

public class FileJsonCodec implements Codec<Map<String, Object>> {

    private final ReentrantReadWriteLock lock;

    private final String root;

    public FileJsonCodec(String root) {
        this.root = root;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Map<String, Object> create(String key) throws IOException {
        return JsonUtil.toMap("{}");
    }

    @Override
    public Map<String, Object> read(String key) throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            Path path = openOrCreate(key);
            return Files.exists(path) ? JsonUtil.toMap(Files.readString(path)) : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String key, Map<String, Object> data) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            Path p = openOrCreate(key);
            try (Writer writer = Files.newBufferedWriter(p)) {
                writer.write(JsonUtil.toString(data));
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
}
