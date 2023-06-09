package org.xbib.net.http.server.session;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.file.FileJsonSessionCodec;
import org.xbib.net.util.RandomUtil;

public class JsonSessionTest {

    private static final Logger logger = Logger.getLogger(JsonSessionTest.class.getName());

    @Test
    void testJsonSession() throws IOException {
        Codec<Session> sessionCodec = newSessionCodec();
        Session session = create(sessionCodec, () -> RandomUtil.randomString(16));
        session.put("a", "b");
        sessionCodec.write(session.id(), session);
        Session session1 = sessionCodec.read(session.id());
        logger.log(Level.INFO, session1.get("a").toString());
    }

    private Codec<Session> newSessionCodec() {
        return new FileJsonSessionCodec("SESSION-TEST", null, 1024,
                Duration.ofDays(1), Paths.get("/var/tmp/session-test"));
    }

    private Session create(Codec<Session> sessionCodec, Supplier<String> sessionIdGenerator) throws IOException {
        return sessionCodec.create(sessionIdGenerator.get());
    }
}
