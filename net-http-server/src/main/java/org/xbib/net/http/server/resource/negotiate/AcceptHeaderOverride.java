package org.xbib.net.http.server.resource.negotiate;

import java.util.regex.Pattern;

public class AcceptHeaderOverride {

    private final Pattern userAgentPattern;

    private final String original;

    private final String replacement;

    public AcceptHeaderOverride(Pattern userAgentPattern, String original, String replacement) {
        this.userAgentPattern = userAgentPattern;
        this.original = original;
        this.replacement = replacement;
    }

    public boolean matches(String acceptHeader, String userAgentHeader) {
        boolean b1 = userAgentPattern == null || userAgentPattern.matcher(userAgentHeader).find();
        boolean b2 = original == null || original.equals(acceptHeader);
        return b1 && b2;
    }

    public String getReplacement() {
        return replacement;
    }
}
