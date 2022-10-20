package org.xbib.net.http.client.netty;

import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamIds {

    private final AtomicInteger streamId;

    private final SortedMap<Integer, CompletableFuture<Boolean>> sortedMap;

    public StreamIds() {
        this.streamId = new AtomicInteger(3);
        this.sortedMap = new ConcurrentSkipListMap<>();
    }

    public CompletableFuture<Boolean> get(Integer key) {
        return sortedMap.get(key);
    }

    public Set<Integer> keys() {
        return sortedMap.keySet();
    }

    public Integer lastKey() {
        return sortedMap.isEmpty() ? null : sortedMap.lastKey();
    }

    public void put(Integer key, CompletableFuture<Boolean> promise) {
        sortedMap.put(key, promise);
    }

    public void remove(Integer key) {
        if (key != null) {
            sortedMap.remove(key);
        }
    }

    public Integer nextStreamId() {
        int streamId = this.streamId.getAndAdd(2);
        if (streamId == Integer.MIN_VALUE) {
            // reset if overflow, Java wraps atomic integers to Integer.MIN_VALUE
            this.streamId.set(3);
            streamId = 3;
        }
        sortedMap.put(streamId, new CompletableFuture<>());
        return streamId;
    }

    public void fail(Throwable throwable) {
        for (CompletableFuture<Boolean> promise : sortedMap.values()) {
            promise.completeExceptionally(throwable);
        }
    }

    public void close() {
        sortedMap.clear();
    }

    public boolean isClosed() {
        return sortedMap.isEmpty();
    }

    @Override
    public String toString() {
        return "StreamIds[id=" + streamId + ",map=" + sortedMap + "]";
    }
}
