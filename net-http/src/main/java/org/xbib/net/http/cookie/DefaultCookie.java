package org.xbib.net.http.cookie;

import java.util.Objects;

/**
 * The default {@link Cookie} implementation.
 */
public class DefaultCookie implements Cookie {

    private final String name;

    private String value;

    private boolean wrap;

    private String domain;

    private String path;

    private long maxAge = Long.MIN_VALUE;

    private boolean secure;

    private boolean httpOnly;

    private SameSite sameSite;

    public DefaultCookie(String name) {
        this(name, null);
    }

    public DefaultCookie(String name, String value) {
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
        if (value != null) {
            setValue(value);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public DefaultCookie setValue(String value) {
        this.value = Objects.requireNonNull(value, "value");
        return this;
    }

    @Override
    public boolean wrap() {
        return wrap;
    }

    @Override
    public DefaultCookie setWrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    @Override
    public String domain() {
        return domain;
    }

    @Override
    public DefaultCookie setDomain(String domain) {
        this.domain = CookieUtil.validateAttributeValue("domain", domain);
        return this;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public DefaultCookie setPath(String path) {
        this.path = CookieUtil.validateAttributeValue("path", path);
        return this;
    }

    @Override
    public long maxAge() {
        return maxAge;
    }

    @Override
    public DefaultCookie setMaxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public DefaultCookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    @Override
    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Override
    public DefaultCookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    @Override
    public DefaultCookie setSameSite(SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    @Override
    public SameSite sameSite() {
        return sameSite;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cookie)) {
            return false;
        }
        Cookie that = (Cookie) o;
        if (!name().equals(that.name())) {
            return false;
        }
        if (path() == null) {
            if (that.path() != null) {
                return false;
            }
        } else if (that.path() == null) {
            return false;
        } else if (!path().equals(that.path())) {
            return false;
        }
        if (domain() == null) {
            if (that.domain() != null) {
                return false;
            }
        } else {
            return domain().equalsIgnoreCase(that.domain());
        }
        if (sameSite() == null) {
            return that.sameSite() == null;
        } else if (that.sameSite() == null) {
            return false;
        } else {
            return sameSite().name().equalsIgnoreCase(that.sameSite().name());
        }
    }

    @Override
    public int compareTo(Cookie c) {
        int v = name().compareTo(c.name());
        if (v != 0) {
            return v;
        }
        if (path() == null) {
            if (c.path() != null) {
                return -1;
            }
        } else if (c.path() == null) {
            return 1;
        } else {
            v = path().compareTo(c.path());
            if (v != 0) {
                return v;
            }
        }
        if (domain() == null) {
            if (c.domain() != null) {
                return -1;
            }
        } else if (c.domain() == null) {
            return 1;
        } else {
            v = domain().compareToIgnoreCase(c.domain());
            return v;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
            .append(name()).append('=').append(value());
        if (domain() != null) {
            buf.append(", domain=").append(domain());
        }
        if (path() != null) {
            buf.append(", path=").append(path());
        }
        if (maxAge() >= 0) {
            buf.append(", maxAge=").append(maxAge()).append('s');
        }
        if (isSecure()) {
            buf.append(", secure");
        }
        if (isHttpOnly()) {
            buf.append(", HTTPOnly");
        }
        if (sameSite() != null) {
            buf.append(", SameSite=").append(sameSite().name());
        }
        return buf.toString();
    }
}
