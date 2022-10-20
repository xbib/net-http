package org.xbib.net.http.server.resource.negotiate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MediaRangeSpec {

    private static final Pattern tokenPattern = Pattern.compile("[\\x20-\\x7E&&[^()<>@,;:\\\"/\\[\\]?={} ]]+");

    private final String type;

    private final String subtype;

    private final Map<String,String> parameter;

    private final double quality;

    private final String mediaType;

    private MediaRangeSpec(String type, String subtype, Map<String,String> parameter, Double quality) {
        this.type = type;
        this.subtype = subtype;
        this.parameter = parameter;
        this.quality = quality;
        this.mediaType = buildMediaType();
    }

    /**
     * Parses a media type from a string such as <tt>text/html;charset=utf-8;q=0.9</tt>
     * @param mediaType mediaType
     * @return the media range spec or null
     */
    public static MediaRangeSpec parseType(String mediaType) {
        MediaRangeSpec m = parseRange(mediaType);
        return (m == null || m.isWildcardType() || m.isWildcardSubtype()) ? null : m;
    }

    /**
     * Parses a media range from a string such as <tt>text/*;charset=utf-8;q=0.9</tt>.
     * Unlike simple media types, media ranges may include wildcards.
     * @param mediaRange mediaRange
     * @return the media range spec or null
     */
    public static MediaRangeSpec parseRange(String mediaRange) {
        if (mediaRange.indexOf(';') > -1) {
            String[] m = mediaRange.split(";");
            Map<String,String> params = new LinkedHashMap<>();
            double q = 1.0;
            int pos = 1;
            while (pos < m.length) {
                int i = m[pos].indexOf('=');
                if (i > -1) {
                    String k = m[pos].substring(0, pos - 1).trim();
                    String v = m[pos].substring(i + 1).trim();
                    params.put(k, v);
                    if (k.equals("q")) {
                        try {
                            q = Double.parseDouble(v);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                pos++;
            }
            String[] types = m[0].split("/");
            return types.length > 1 ? new MediaRangeSpec(types[0], types[1], params, q) : null;
        } else {
            String[] types = mediaRange.split("/");
            return types.length > 1 ? new MediaRangeSpec(types[0], types[1], null, 1.0d) : null;
        }
    }

    /**
     * Parses a HTTP Accept header into a list of MediaRangeSpecs
     * @param accept the Accept header
     * @return A List of MediaRangeSpecs
     */
    public static List<MediaRangeSpec> parseAccept(String accept) {
        List<MediaRangeSpec>  list = new ArrayList<>();
        String[] tokens = accept.split(",");
        for (String t : tokens) {
            MediaRangeSpec mediaRangeSpec = parseRange(t);
            list.add(mediaRangeSpec);
        }
        return list;
    }

    private static String escape(String s) {
        return s.replaceAll("[\\\\\"]", "\\\\$0");
    }

    private String buildMediaType() {
        StringBuilder result = new StringBuilder();
        result.append(type).append("/").append(subtype);
        if (parameter != null) {
            for (Map.Entry<String, String> me : parameter.entrySet()) {
                result.append(";").append(me.getKey()).append("=");
                String value = me.getValue();
                if (tokenPattern.matcher(value).matches()) {
                    result.append(value);
                } else {
                    result.append("\"").append(escape(value)).append("\"");
                }
            }
        }
        return result.toString();
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Map<String,String> getParameter() {
        return parameter;
    }

    public boolean isWildcardType() {
        return "*".equals(type);
    }

    public boolean isWildcardSubtype() {
        return !isWildcardType() && "*".equals(subtype);
    }

    public double getQuality() {
        return quality;
    }

    public int getPrecedence(MediaRangeSpec range) {
        if (range.isWildcardType()) {
            return 1;
        }
        if (!range.type.equals(type)) {
            return 0;
        }
        if (range.isWildcardSubtype()) {
            return 2;
        }
        if (!range.subtype.equals(subtype)) {
            return 0;
        }
        if (range.parameter == null || range.parameter.isEmpty()) {
            return 3;
        }
        int result = 3;
        for (Map.Entry<String, String> entry : range.parameter.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (!value.equals(parameter.get(name))) {
                return 0;
            }
            result++;
        }
        return result;
    }

    public MediaRangeSpec getBestMatch(List<MediaRangeSpec> mediaRanges) {
        MediaRangeSpec result = null;
        int bestPrecedence = 0;
        for (MediaRangeSpec range : mediaRanges) {
            if (getPrecedence(range) > bestPrecedence) {
                bestPrecedence = getPrecedence(range);
                result = range;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return mediaType;
    }
}