package org.xbib.net.http.server.cookie;

import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.cookie.CookieHeaderNames;
import org.xbib.net.http.cookie.CookieUtil;
import org.xbib.net.util.DateTimeUtil;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * A <a href="http://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie encoder to be used server side,
 * so some fields are sent (Version is typically ignored).
 *
 * As Cookie merges Expires and MaxAge into one single field, only Max-Age field is sent.
 *
 * <pre>
 * // Example
 * HttpRequest.Builder req = ...
 * res.addHeader("Cookie", {@link CookieEncoder}.encode("JSESSIONID", "1234"))
 * </pre>
 *
 * @see CookieDecoder
 */
public final class CookieEncoder extends org.xbib.net.http.cookie.CookieEncoder {

    /**
     * Strict encoder that validates that name and value chars are in the valid scope
     * defined in RFC6265, and (for methods that accept multiple cookies) that only
     * one cookie is encoded with any given name. (If multiple cookies have the same
     * name, the last one is the one that is encoded.)
     */
    public static final CookieEncoder STRICT = new CookieEncoder(true);

    /**
     * Lax instance that doesn't validate name and value, and that allows multiple
     * cookies with the same name.
     */
    public static final CookieEncoder LAX = new CookieEncoder(false);

    private CookieEncoder(boolean strict) {
        super(strict);
    }

    /**
     * Encodes the specified cookie into a Set-Cookie header value.
     *
     * @param cookie the cookie
     * @return a single Set-Cookie header value
     */
    public String encode(Cookie cookie) {
        final String name = Objects.requireNonNull(cookie, "cookie").name();
        final String value = cookie.value() != null ? cookie.value() : "";
        validateCookie(name, value);
        StringBuilder stringBuilder = new StringBuilder();
        if (cookie.wrap()) {
            CookieUtil.addQuoted(stringBuilder, name, value);
        } else {
            CookieUtil.add(stringBuilder, name, value);
        }
        if (cookie.maxAge() != Long.MIN_VALUE) {
            CookieUtil.add(stringBuilder, CookieHeaderNames.MAX_AGE, cookie.maxAge());
            Instant expires = Instant.ofEpochMilli(cookie.maxAge() * 1000 + System.currentTimeMillis());
            stringBuilder.append(CookieHeaderNames.EXPIRES);
            stringBuilder.append(CookieUtil.EQUALS);
            stringBuilder.append(DateTimeUtil.formatRfc1123(expires.toEpochMilli()));
            stringBuilder.append(CookieUtil.SEMICOLON);
            stringBuilder.append(CookieUtil.SP);
        }
        if (cookie.path() != null) {
            CookieUtil.add(stringBuilder, CookieHeaderNames.PATH, cookie.path());
        }
        if (cookie.domain() != null) {
            CookieUtil.add(stringBuilder, CookieHeaderNames.DOMAIN, cookie.domain());
        }
        if (cookie.isSecure()) {
            CookieUtil.add(stringBuilder, CookieHeaderNames.SECURE);
        }
        if (cookie.isHttpOnly()) {
            CookieUtil.add(stringBuilder, CookieHeaderNames.HTTPONLY);
        }
        if (cookie.sameSite() != null) {
            String s = cookie.sameSite().name();
            CookieUtil.add(stringBuilder, CookieHeaderNames.SAMESITE,
                    s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT));
        }
        return CookieUtil.stripTrailingSeparator(stringBuilder);
    }
}
