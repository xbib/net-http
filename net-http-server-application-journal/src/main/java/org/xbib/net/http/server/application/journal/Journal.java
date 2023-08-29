package org.xbib.net.http.server.application.journal;

import org.xbib.net.util.ExceptionFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Journal {

    private static final Logger logger = Logger.getLogger(Journal.class.getName());

    private final Path journalPath;

    private final ReentrantReadWriteLock lock;

    public Journal(String journalPathName) throws IOException {
        this.journalPath = createJournal(journalPathName);
        this.lock = new ReentrantReadWriteLock();
    }

    private static Path createJournal(String logPathName) throws IOException {
        Path logPath = Paths.get(logPathName);
        Files.createDirectories(logPath);
        if (!Files.exists(logPath) || !Files.isWritable(logPath)) {
            throw new IOException("unable to write to log path = " + logPath);
        }
        return logPath;
    }

    public void logRequest(String stamp, String request) throws IOException {
        logger.log(Level.FINE, stamp + " request = " + request);
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try (OutputStream outputStream = Files.newOutputStream(journalPath.resolve(stamp + ".log"), StandardOpenOption.CREATE)) {
            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
        } finally {
            writeLock.unlock();
        }
    }

    public void logSuccess(String stamp, String response) throws IOException {
        logger.log(Level.FINE, stamp + " response = " + response);
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        Path path = journalPath.resolve("success").resolve(stamp + ".request");
        Files.createDirectories(path.getParent());
        Files.move(journalPath.resolve(stamp + ".log"), path);
        try (OutputStream outputStream = Files.newOutputStream(journalPath.resolve("success").resolve(stamp + ".response"), StandardOpenOption.CREATE)) {
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        } finally {
            writeLock.unlock();
        }
    }

    public void logFail(String stamp, Throwable t) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        Path path = journalPath.resolve("fail").resolve(stamp + ".request");
        Files.createDirectories(path.getParent());
        Files.move(journalPath.resolve(stamp + ".log"), path);
        // save throwable in extra file
        try (OutputStream outputStream = Files.newOutputStream(journalPath.resolve("fail").resolve(stamp + ".exception"), StandardOpenOption.CREATE)) {
            outputStream.write(ExceptionFormatter.format(t).getBytes(StandardCharsets.UTF_8));
        } finally {
            writeLock.unlock();
        }
    }

    public void retry(Consumer<StampedEntry> consumer) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        PathMatcher pathMatcher = journalPath.getFileSystem().getPathMatcher("glob:*.log");
        try {
            Files.walkFileTree(journalPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes a) throws IOException {
                    if ((Files.isRegularFile(p) && pathMatcher.matches(p.getFileName()))) {
                        String stamp = p.getFileName().toString();
                        String entry = Files.readString(p);
                        consumer.accept(new StampedEntry(stamp, entry));
                        Files.delete(p);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } finally {
            writeLock.unlock();
        }
    }

    public void purgeSuccess(Instant instant) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        PathMatcher pathMatcher = journalPath.getFileSystem().getPathMatcher("glob:*.request");
        try {
            Files.walkFileTree(journalPath.resolve("success"), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes a) throws IOException {
                    if ((Files.isRegularFile(p) && pathMatcher.matches(p.getFileName()))) {
                        if (Files.getLastModifiedTime(p).toInstant().isBefore(instant)) {
                            Files.delete(p);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } finally {
            writeLock.unlock();
        }
    }

    public void purgeFail(Instant instant) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        PathMatcher pathMatcher = journalPath.getFileSystem().getPathMatcher("glob:*.request");
        try {
            Files.walkFileTree(journalPath.resolve("fail"), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes a) throws IOException {
                    if ((Files.isRegularFile(p) && pathMatcher.matches(p.getFileName()))) {
                        if (Files.getLastModifiedTime(p).toInstant().isBefore(instant)) {
                            Files.delete(p);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } finally {
            writeLock.unlock();
        }
    }

    public static class StampedEntry {

        private final String stamp;

        private final String entry;

        public StampedEntry(String stamp, String entry) {
            this.stamp = stamp;
            this.entry = entry;
        }

        public String getStamp() {
            return stamp;
        }

        public String getEntry() {
            return entry;
        }
    }
}
