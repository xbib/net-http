package org.xbib.net.http.server.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.xbib.datastructures.common.LRUCache;

public class BaseSession implements Session {

    static String CREATED_FIELD = "_created_";

    static String LAST_MODIFIED_FIELD = "_lastmodified_";

    static String CACHE_PREFIX = "_cache_";

    private final SessionListener sessionListener;

    private final String name;

    private final String id;

    private final Duration lifetime;

    private final Map<String, Object> map;

    private final int cacheSize;

    private boolean valid;

    public BaseSession(SessionListener sessionListener,
                       int cacheSize,
                       String name,
                       String id,
                       boolean create,
                       Duration lifetime) {
        this.cacheSize = cacheSize;
        this.sessionListener = sessionListener;
        this.name = name;
        this.id = id;
        this.lifetime = lifetime;
        this.map = new LinkedHashMap<>();
        this.valid = !lifetime.isNegative() && !lifetime.isZero();
        Instant now = Instant.now();
        if (create) {
            put(CREATED_FIELD, now.toString());
            if (sessionListener != null) {
                sessionListener.onCreated(this);
            }
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void invalidate() {
        clear();
        valid = false;
        if (sessionListener != null) {
            sessionListener.onDestroy(this);
        }
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean isExpired() {
        String string = (String) get(LAST_MODIFIED_FIELD);
        if (string == null) {
            string = (String) get(CREATED_FIELD);
        }
        if (string == null) {
            return false;
        }
        Instant now = Instant.now();
        Instant lastModified = Instant.parse(string);
        return Duration.between(lastModified, now).compareTo(lifetime) > 0;
    }

    @Override
    public boolean hasPayload() {
        return !isEmpty() &&
                !(size() == 1 && containsKey(CREATED_FIELD)) &&
                !(size() == 2 && containsKey(CREATED_FIELD) && containsKey(LAST_MODIFIED_FIELD));
    }

    @Override
    public Duration getAge() {
        Instant instant = containsKey(CREATED_FIELD) ? Instant.parse(get(CREATED_FIELD).toString()) : Instant.now();
        return Duration.between(instant, Instant.now());
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String key, Object value) {
        Object v = value;
        if (key.startsWith(CACHE_PREFIX)) {
            if (value instanceof Map) {
                v = newCache((Map<String, Object>) value);
            } else if (value == null) {
                throw new IllegalArgumentException("null not allowed for session cache: key = " + key);
            } else {
                throw new IllegalArgumentException("only a map allowed for session cache: key = " + key + " value class = " + value.getClass().getName());
            }
        }
        return map.put(key, v);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void putAll(Map map) {
        if (map == null) {
            throw new NullPointerException("unexpected null map for putAll");
        }
        this.map.putAll(map);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public void setLastModified() {
        put(LAST_MODIFIED_FIELD, Instant.now().toString());
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void putCache(String cacheName, String token, Map<String, Object> map) {
        getCache(cacheName).put(token, map);
    }

    @SuppressWarnings("unchecked")
    public void putCache(String cacheName, String token, String key, Object value) {
        Map<String, Object> cache = (Map<String, Object>) getCache(cacheName).get(token);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public Object getCache(String cacheName, String token, String key) {
        Map<String, Object> cache = (Map<String, Object>) getCache(cacheName).get(token);
        return cache != null ? cache.get(key) : null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCache(String cacheName, String token) {
        return (Map<String, Object>) getCache(cacheName).getOrDefault(token, new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCache(String cacheName) {
        map.computeIfAbsent(CACHE_PREFIX + cacheName, s -> newCache());
        return (Map<String, Object>) map.get(CACHE_PREFIX + cacheName);
    }

    private Map<String, Object> newCache(Map<String, Object> map) {
        Map<String, Object> cache = newCache();
        cache.putAll(map);
        return cache;
    }

    private Map<String, Object> newCache() {
        return new LRUCache<>(cacheSize);
    }
}
