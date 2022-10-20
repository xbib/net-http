package org.xbib.net.http.server.persist;

import java.io.IOException;
import java.util.Map;

public interface PersistenceStore<K, V> extends Map<K, V> {

    Codec<Map<K,V>> getCodec();

    void load() throws IOException;

    void insertValue(K key, V value) throws IOException;

    void removeValue(K key, V value) throws IOException;

}
