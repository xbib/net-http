package org.xbib.net.http.server.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;
import org.xbib.net.UserProfile;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.cookie.DefaultCookie;
import org.xbib.net.http.cookie.SameSite;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.cookie.CookieSignatureException;
import org.xbib.net.http.server.cookie.CookieSignatureUtil;

public class OutgoingSessionHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(OutgoingSessionHandler.class.getName());

    private final String sessionSecret;

    private final String sessionCookieAlgorithm;

    private final String sessionCookieName;

    private final Duration sessionDuration;

    private final Set<String> suffixes;

    private final String sessionUserName;

    private final String sessionEffectiveUserName;

    private final boolean httpOnly;

    private final boolean secure;

    private final SameSite sameSite;

    public OutgoingSessionHandler(String sessionSecret,
                                  String sessionCookieAlgorithm,
                                  String sessionCookieName,
                                  Set<String> suffixes,
                                  String sessionUserName,
                                  String sessionEffectiveUserName,
                                  Duration sessionDuration,
                                  boolean httpOnly,
                                  boolean secure,
                                  SameSite sameSite) {
        this.sessionSecret = sessionSecret;
        this.sessionCookieAlgorithm = sessionCookieAlgorithm;
        this.sessionCookieName = sessionCookieName;
        this.suffixes = suffixes;
        this.sessionUserName = sessionUserName;
        this.sessionEffectiveUserName = sessionEffectiveUserName;
        this.sessionDuration = sessionDuration;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public void handle(HttpRouterContext context) throws HttpException {
        if (context.getContextURL() == null) {
            // emergency message
            return;
        }
        String suffix = SessionUtil.extractExtension(context.getRequestBuilder().getRequestPath());
        if (suffix != null && suffixes.contains(suffix)) {
            logger.log(Level.FINEST, () -> "suffix " + suffix + " blocking outgoing session handling");
            return;
        }
        CookieBox cookieBox = context.getAttributes().get(CookieBox.class, "outgoingcookies");
        if (cookieBox == null) {
            cookieBox = new CookieBox();
        }
        Application application = context.getAttributes().get(Application.class, "application");
        UserProfile userProfile = context.getAttributes().get(UserProfile.class, "userprofile");
        String host = context.getContextURL().getHost();
        String path = application.getContextPath();
        Throwable throwable = context.getAttributes().get(Throwable.class, "_throwable");
        if (throwable instanceof CookieSignatureException) {
            cookieBox = new CookieBox();
            cookieBox.add(createEmptyCookie(host, path));
            return;
        }
        Session session = context.getAttributes().get(Session.class, "session");
        if (session != null) {
            try {
                if (userProfile != null) {
                    logger.log(Level.FINEST, () -> "user profile present: " + userProfile);
                    if (sessionUserName != null) {
                        session.put(sessionUserName, userProfile.getUserId());
                    }
                    if (sessionEffectiveUserName != null) {
                        session.put(sessionEffectiveUserName, userProfile.getEffectiveUserId());
                    }
                }
                Cookie cookie = encodeCookie(session, host, path);
                if (cookie != null) {
                    cookieBox.add(cookie);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new HttpException("unable to create session data for cookie", context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
        logger.log(Level.FINEST, "prepared outgoing cookies = " + cookieBox);
        context.getAttributes().put("outgoingcookies", cookieBox);
    }

    private Cookie encodeCookie(Session session, String host, String path) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException {
        if (sessionSecret == null) {
            logger.log(Level.WARNING, "no secret, no cookie");
            return null;
        }
        String id = session.id();
        if (!session.isValid()) {
            logger.log(Level.WARNING, "session " + id + " has been invalidated, returning empty cookie");
            return createEmptyCookie(host, path);
        }
        if (session.isExpired()) {
            logger.log(Level.WARNING, "session " + id + " is expired, returning empty cookie");
            session.invalidate();
            return createEmptyCookie(host, path);
        }
        Map<String, Object> map = new HashMap<>();
        if (sessionUserName != null) {
            map.put(sessionUserName, session.get(sessionUserName));
        }
        if (sessionEffectiveUserName != null) {
            map.put(sessionEffectiveUserName, session.get(sessionEffectiveUserName));
        }
        String payload = CookieSignatureUtil.toString(map);
        String sig = CookieSignatureUtil.hmac(payload, sessionSecret, sessionCookieAlgorithm);
        String cookieValue = String.join(":", id, payload, sig);
        PercentEncoder percentEncoder = PercentEncoders.getCookieEncoder(StandardCharsets.ISO_8859_1);
        DefaultCookie cookie = new DefaultCookie(sessionCookieName, percentEncoder.encode(cookieValue));
        String domain = extractDomain(host);
        if ("localhost".equals(domain)) {
            logger.log(Level.WARNING, "localhost not set as a cookie domain");
        } else {
            cookie.setDomain('.' + domain);
        }
        cookie.setPath(path);
        cookie.setMaxAge(sessionDuration.toSeconds());
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setSameSite(sameSite);
        return cookie;
    }

    private Cookie createEmptyCookie(String host, String path) {
        DefaultCookie cookie = new DefaultCookie(sessionCookieName);
        String domain = extractDomain(host);
        if ("localhost".equals(domain)) {
            logger.log(Level.WARNING, "localhost not set as a cookie domain");
        } else {
            cookie.setDomain('.' + domain);
        }
        cookie.setPath(path);
        cookie.setMaxAge(0L);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setSameSite(sameSite);
        logger.log(Level.FINEST, "baked empty cookie");
        return cookie;
    }

    private static String extractDomain(String fqdn) {
        if ("localhost".equals(fqdn)) {
            return fqdn;
        }
        // strip host name from FQDN
        int pos1 = fqdn.indexOf('.');
        int pos2 = fqdn.lastIndexOf('.');
        if (pos1 < pos2) {
            // more than one dot, strip host
            return fqdn.substring(pos1 + 1);
        }
        return fqdn;
    }
}
