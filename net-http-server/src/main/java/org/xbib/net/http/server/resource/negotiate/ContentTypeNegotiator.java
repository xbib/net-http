package org.xbib.net.http.server.resource.negotiate;

import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.server.HttpRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Taken from org.apache.any23.servlet.conneg.ContentTypeNegotiator
 */
public class ContentTypeNegotiator {

    private static final ContentTypeNegotiator contentTypeNegotiator = new ContentTypeNegotiator();

    private final List<VariantSpec> variantSpecs = new ArrayList<>();

    private final Collection<AcceptHeaderOverride> userAgentOverrides = new ArrayList<>();

    private List<MediaRangeSpec> defaultAcceptRanges = Collections.singletonList(MediaRangeSpec.parseRange("*/*"));

    private ContentTypeNegotiator() {
        setDefaultAccept("text/html");
        /*
         * Send HTML to clients that indicate they accept everything.
         * This is specifically so that cURL sees HTML, and also catches
         * various browsers that send "* / *" in some circumstances.
         */
        addUserAgentOverride(null, "*/*", "text/html");

        /*
         * MSIE (7.0) sends either \* / *, or * / * with a list of other
         * random types,
         * but always without q values. That's useless. We will simply send
         * HTML to MSIE, no matter what. Boy, do I hate IE.
         */
        addUserAgentOverride(Pattern.compile("MSIE"), null, "text/html");

        addVariant("text/html;q=0.81").addAliasMediaType("text/html;q=0.81").makeDefault();
        addVariant("text/xml;q=0.81").addAliasMediaType("text/xml;q=0.81");
        addVariant("application/json;q=0.80").addAliasMediaType("application/json;q=0.80");
        addVariant("application/xml;q=0.80").addAliasMediaType("application/xml;q=0.80");
    }

    public static String negotiateMediaType(HttpRequest request) {
        String userAgent = request.getHeaders().get(HttpHeaderNames.USER_AGENT);
        String accept = request.getHeaders().get(HttpHeaderNames.ACCEPT);
        return negotiateMediaType(userAgent, accept);
    }

    public static String negotiateMediaType(String useragent, String accept) {
        MediaRangeSpec mrs = contentTypeNegotiator.getBestMatch(accept, useragent != null ? useragent : "");
        return mrs != null ? mrs.getMediaType() : "";
    }

    public static Locale negotiateLocale(HttpRequest request) {
        String languages = request.getHeaders().get(HttpHeaderNames.ACCEPT_LANGUAGE);
        return LocaleNegotiator.findLocale(languages);
    }

    /**
     * Add variant.
     *
     * @param mediaType the media type
     */
    private VariantSpec addVariant(String mediaType) {
        VariantSpec variantSpec = new VariantSpec(mediaType);
        variantSpecs.add(variantSpec);
        return variantSpec;
    }

    /**
     * Sets an Accept header to be used as the default if a client does
     * not send an Accept header, or if the Accept header cannot be parsed.
     * Defaults to "*&slash;*"
     * @param accept the default accept header
     */
    private void setDefaultAccept(String accept) {
        this.defaultAcceptRanges = MediaRangeSpec.parseAccept(accept);
    }

    /**
     * Overrides the Accept header for certain user agents. This can be
     * used to implement special-case handling for user agents that send
     * faulty Accept headers.
     *
     * @param userAgentString      A pattern to be matched against the User-Agent header,
     *                             <tt>null</tt> means regardless of User-Agent
     * @param originalAcceptHeader Only override the Accept header if the user agent
     *                             sends this header, <tt>null</tt> means always override
     * @param newAcceptHeader      The Accept header to be used instead
     */
    private void addUserAgentOverride(Pattern userAgentString,
                                      String originalAcceptHeader,
                                      String newAcceptHeader) {
        this.userAgentOverrides.add(new AcceptHeaderOverride(userAgentString, originalAcceptHeader, newAcceptHeader));
    }

    /**
     * Get best match for an Accept header.
     * @param accept the Accept header
     * @return the media range spec
     */
    private MediaRangeSpec getBestMatch(String accept, String agent) {
        String overriddenAccept = accept;
        for (AcceptHeaderOverride override : userAgentOverrides) {
            if (override.matches(accept, agent)) {
                overriddenAccept = override.getReplacement();
                break;
            }
        }
        return new Negotiation(toAcceptRanges(overriddenAccept)).negotiate(variantSpecs);
    }

    private List<MediaRangeSpec> toAcceptRanges(String accept) {
        if (accept == null) {
            return defaultAcceptRanges;
        }
        List<MediaRangeSpec> result = MediaRangeSpec.parseAccept(accept);
        if (result.isEmpty()) {
            return defaultAcceptRanges;
        }
        return result;
    }
}
