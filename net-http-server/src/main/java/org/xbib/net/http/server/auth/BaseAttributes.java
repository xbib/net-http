package org.xbib.net.http.server.auth;

import java.util.LinkedHashMap;
import org.xbib.net.Attributes;

@SuppressWarnings("serial")
public class BaseAttributes extends LinkedHashMap<String, Object> implements Attributes {

    public BaseAttributes() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> cl, String key) {
        return (T) super.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> cl, String key, T defaultValue) {
        return containsKey (key) ? (T) super.get(key) : defaultValue;
    }
}
