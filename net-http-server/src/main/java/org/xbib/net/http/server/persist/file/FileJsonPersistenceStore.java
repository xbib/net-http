package org.xbib.net.http.server.persist.file;

import java.util.Map;
import org.xbib.net.http.server.persist.AbstractPersistenceStore;
import org.xbib.net.http.server.persist.Codec;

@SuppressWarnings("serial")
public class FileJsonPersistenceStore extends AbstractPersistenceStore {

    public FileJsonPersistenceStore(String name) {
        this("/var/tmp/net-http-server-store", name);
    }

    public FileJsonPersistenceStore(String root, String storeName) {
        this(new FileJsonCodec(root), storeName);
    }

    public FileJsonPersistenceStore(Codec<Map<String, Object>> codec, String storeName) {
        super(codec, storeName);
    }
}
