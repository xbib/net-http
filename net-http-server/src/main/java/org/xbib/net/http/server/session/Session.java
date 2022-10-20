package org.xbib.net.http.server.session;

import java.time.Duration;
import java.util.Map;

public interface Session extends Map<String, Object> {

    String id();

    void invalidate();

    boolean isValid();

    boolean isExpired();

    boolean hasPayload();

    Duration getAge();
}
