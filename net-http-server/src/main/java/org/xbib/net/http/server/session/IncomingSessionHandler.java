package org.xbib.net.http.server.session;

import org.xbib.net.PercentDecoder;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.auth.BaseUserProfile;
import org.xbib.net.UserProfile;
import org.xbib.net.http.server.cookie.CookieSignatureException;
import org.xbib.net.http.server.cookie.CookieSignatureUtil;
import org.xbib.net.http.server.persist.Codec;

import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.util.RandomUtil;

public class IncomingSessionHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(IncomingSessionHandler.class.getName());

    private final String sessionSecret;

    private final String sessionCookieAlgorithm;

    private final String sessionCookieName;

    private final Codec<Session> sessionCodec;

    private final String sessionUserName;

    private final String sessionEffectiveUserName;

    /**
     * These suffixes disable incoming session creation.
     */
    private final Set<String> suffixes;

    public IncomingSessionHandler(String sessionSecret,
                                  String sessionCookieAlgorithm,
                                  String sessionCookieName,
                                  Codec<Session> sessionCodec,
                                  Set<String> suffixes,
                                  String sessionUserName,
                                  String sessionEffectiveUserName) {
        this.sessionSecret = sessionSecret;
        this.sessionCookieAlgorithm = sessionCookieAlgorithm;
        this.sessionCookieName = sessionCookieName;
        this.sessionCodec = sessionCodec;
        this.suffixes = suffixes;
        this.sessionUserName = sessionUserName;
        this.sessionEffectiveUserName = sessionEffectiveUserName;
    }

    @Override
    public void handle(HttpServerContext context) throws HttpException {
        String suffix = SessionUtil.extractExtension(context.request().getRequestPath());
        if (suffix != null && suffixes.contains(suffix)) {
            return;
        }
        Session session = null;
        CookieBox cookieBox = context.attributes().get(CookieBox.class, "incomingcookies");
        if (cookieBox != null) {
            for (Cookie cookie : cookieBox) {
                if (cookie.name().equals(sessionCookieName)) {
                    if (session == null) {
                        try {
                            Map<String, Object> payload = decodeCookie(cookie);
                            session = toSession(payload);
                            UserProfile userProfile = newUserProfile(payload, session);
                            if (userProfile != null) {
                                context.attributes().put("userprofile", userProfile);
                            }
                        } catch (CookieSignatureException e) {
                            // set exception in context to discard broken cookie later and render exception message
                            context.attributes().put("_throwable", e);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, e.getMessage(), e);
                            throw new HttpException("unable to create session", context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        logger.log(Level.WARNING, "received extra session cookie, something is wrong, ignoring");
                    }
                }
            }
        }
        if (session == null) {
            try {
                session = sessionCodec.create(RandomUtil.randomString(32));
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new HttpException("unable to create session", context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
        context.attributes().put("session", session);

    }

    private Map<String, Object> decodeCookie(Cookie cookie) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException, CookieSignatureException {
        PercentDecoder percentDecoder = new PercentDecoder(StandardCharsets.ISO_8859_1.newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE));
        String value = percentDecoder.decode(cookie.value());
        String[] s = value.split(":", 3);
        if (s.length != 3) {
            throw new IllegalArgumentException("cookie format problem, not 3 groups: " + cookie.value());
        }
        String id = s[0];
        String payload = s[1];
        String sig = s[2];
        String mysig = CookieSignatureUtil.hmac(payload, sessionSecret, sessionCookieAlgorithm);
        if (!sig.equals(mysig)) {
            logger.log(Level.SEVERE, MessageFormat.format("signature in cookie does not match. algo={1} secret={2} payload={3} sig={4} mysig={5}",
                    sessionCookieAlgorithm, sessionSecret, payload, sig, mysig));
            throw new CookieSignatureException("cookie security problem");
        }
        Map<String, Object> map = CookieSignatureUtil.toMap(payload);
        return Map.of("id", id, "payload", payload, "map", map);
    }

    protected Session toSession(Map<String, Object> map) {
        return toSession((String) map.get("id"), map);
    }

    protected Session toSession(String id, Map<String, Object> map) {
        Session session = null;
        try {
            session = sessionCodec.read(id);
            if (session != null && map != null) {
                session.putAll(map);
            }
        } catch (Exception e) {
            logger.log(Level.FINEST, "unable to read session, id = " + id, e);
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    protected UserProfile newUserProfile(Map<String, Object> map, Session session) {
        UserProfile userProfile = new BaseUserProfile();
        Map<String, Object> m = (Map<String, Object>) map.get("map");
        if (m == null) {
            return userProfile;
        }
        if (m.containsKey(sessionUserName)) {
            userProfile.setUserId((String) m.get(sessionUserName));
        }
        if (m.containsKey(sessionEffectiveUserName)) {
            userProfile.setEffectiveUserId((String) m.get(sessionEffectiveUserName));
        }
        if (session != null && userProfile.getUserId() != null) {
            session.put("user_id", userProfile.getUserId());
            session.put("e_user_id", userProfile.getUserId());
        }
        return userProfile;
    }
}
