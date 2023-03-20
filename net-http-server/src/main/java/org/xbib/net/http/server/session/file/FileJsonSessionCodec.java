package org.xbib.net.http.server.session.file;

import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.BaseSession;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.SessionListener;
import org.xbib.net.util.JsonUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileJsonSessionCodec implements Codec<Session> {

    private static final Logger logger = Logger.getLogger(FileJsonSessionCodec.class.getName());

    private final String name;

    private final ReentrantReadWriteLock lock;

    private final SessionListener sessionListener;

    private final Path path;

    private final int sessionCacheSize;

    private final Duration sessionDuration;

    public FileJsonSessionCodec(String name,
                                SessionListener sessionListener,
                                int sessionCacheSize,
                                Duration sessionDuration,
                                Path path
                                ) {
        this.name = name;
        this.sessionListener = sessionListener;
        this.path = path;
        this.sessionCacheSize = sessionCacheSize;
        this.sessionDuration = sessionDuration;
        this.lock = new ReentrantReadWriteLock();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Session create(String key) throws IOException {
        return new BaseSession(sessionListener, sessionCacheSize, name, key, true, sessionDuration);
    }

    @Override
    public Session read(String key) throws IOException {
        Session session;
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            session = new BaseSession(sessionListener, sessionCacheSize, name, key, false, sessionDuration);
            Map<String, Object> map = JsonUtil.toMap(Files.readString(path.resolve(percentEncoder.encode(key))));
            session.putAll(map);
            return session;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String key, Session session) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            try (Writer writer = Files.newBufferedWriter(path.resolve(percentEncoder.encode(key)))) {
                writer.write(JsonUtil.toString(session));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Files.deleteIfExists(path.resolve(percentEncoder.encode(key)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void purge(long expiredAfterSeconds) {
        if (path != null && expiredAfterSeconds > 0L) {
            Instant instant = Instant.now();
            try (Stream<Path> stream = Files.walk(path)) {
                stream.forEach(p -> {
                    try {
                        FileTime fileTime = Files.getLastModifiedTime(p);
                        Duration duration = Duration.between(fileTime.toInstant(), instant);
                        if (duration.toSeconds() > expiredAfterSeconds) {
                            Files.delete(p);
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "i/o error while purge: " + e.getMessage(), e);
                    }
                });
            } catch (IOException e) {
                logger.log(Level.WARNING, "i/o error while purge: " + e.getMessage(), e);
            }
        }
    }
}
