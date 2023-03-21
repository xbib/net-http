package org.xbib.net.http.server.route;

import java.util.Collection;
import java.util.Set;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BaseHttpRoute implements HttpRoute {

    private final HttpAddress httpAddress;

    private final Collection<HttpMethod> httpMethods;

    private final String path;

    private final List<RouteSegment> segments;

    private final String sortKey;

    public BaseHttpRoute(HttpAddress httpAddress, HttpMethod httpMethod, String path) {
        this(httpAddress, Set.of(httpMethod), path, false);
    }

    public BaseHttpRoute(HttpAddress httpAddress, Collection<HttpMethod> httpMethods, String path, boolean onlyStrings) {
        Objects.requireNonNull(httpAddress, "address");
        Objects.requireNonNull(httpMethods, "methods");
        Objects.requireNonNull(path, "path");
        this.httpAddress = httpAddress;
        this.httpMethods = httpMethods;
        this.path = path;
        this.segments = onlyStrings ? createStringSegments(path): createSegments(path);
        this.sortKey = createSortKey();
    }

    @Override
    public HttpAddress getHttpAddress() {
        return httpAddress;
    }

    @Override
    public Collection<HttpMethod> getHttpMethods() {
        return httpMethods;
    }

    @Override
    public String getPath() {
        return path;
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    @Override
    public boolean matches(ParameterBuilder parameterBuilder, HttpRoute requestedRoute) {
        if (!(requestedRoute instanceof BaseHttpRoute baseHttpRoute)) {
            return false;
        }
        if (!httpAddress.equals(baseHttpRoute.getHttpAddress())) {
            return false;
        }
        HttpMethod requestedMethod = requestedRoute.getHttpMethods().iterator().next();
        if (!baseHttpRoute.getHttpMethods().contains(requestedMethod)) {
            return false;
        }
        List<RouteSegment> requestedSegments = baseHttpRoute.getSegments();
        // special case: empty segments match
        if (requestedSegments.isEmpty() && segments.isEmpty()) {
            return true;
        }
        // special case: single segment with pattern to match, we must ignore the incoming segments
        if (segments.size() == 1 && segments.get(0) instanceof PatternSegment) {
            MatchResult matchResult = segments.get(0).match(new StringSegment(requestedRoute.getPath()));
            if (matchResult == MatchResult.TRUE) {
                return true;
            }
        }
        List<MatchResult> matchResults = new ArrayList<>();
        MatchResult matchResult;
        if (segments.size() >= requestedSegments.size()) {
            int i = 0;
            while (i < segments.size()) {
                if (i >= requestedSegments.size()) {
                    // special case: catch_all after prefixes have matched is OK
                    if (!(segments.get(i) instanceof CatchAllSegment)) {
                        matchResults.add(MatchResult.FALSE);
                    }
                    break;
                }
                matchResult = segments.get(i).match(requestedSegments.get(i));
                if (matchResult instanceof ValueMatchResult) {
                    ParameterSegment parameterSegment = (ParameterSegment) segments.get(i);
                    parameterBuilder.add(parameterSegment.getName(), ((ValueMatchResult) matchResult).getValue());
                }
                if (matchResult == MatchResult.ALWAYS) {
                    break;
                }
                matchResults.add(matchResult);
                i++;
            }
        } else {
            int i = 0;
            while (i < requestedSegments.size()) {
                if (i >= segments.size()) {
                    // never match on short patterns
                    matchResults.add(MatchResult.FALSE);
                    break;
                }
                matchResult = requestedSegments.get(i).match(segments.get(i));
                if (matchResult instanceof ValueMatchResult) {
                    ParameterSegment parameterSegment = (ParameterSegment) segments.get(i);
                    parameterBuilder.add(parameterSegment.getName(), ((ValueMatchResult) matchResult).getValue());
                }
                if (matchResult == MatchResult.ALWAYS) {
                    break;
                }
                matchResults.add(matchResult);
                i++;
            }
        }
        return matchResults.stream().noneMatch(p -> p == MatchResult.FALSE);
    }

    @Override
    public String getSortKey() {
        return sortKey;
    }

    @Override
    public String toString() {
        return httpAddress + "/" + httpMethods + "/" + segments.stream().map(Object::toString).collect(Collectors.joining("/"));
    }

    private static List<RouteSegment> createSegments(String path) {
        if (path.startsWith("glob:")) {
            return List.of(new PatternSegment(path));
        }
        List<RouteSegment> list = new ArrayList<>();
        for (String s : path.split("/")) {
            if (s.isEmpty()) {
                continue;
            }
            if (isCatchAll(s)) {
                list.add(CATCHALL);
            } else if (isParameter(s)) {
                list.add(new ParameterSegment(s));
            } else if (isPattern(s)) {
                list.add(new PatternSegment(s));
            } else {
                list.add(new StringSegment(s));
            }
        }
        return list;
    }

    private static List<RouteSegment> createStringSegments(String path) {
        List<RouteSegment> list = new ArrayList<>();
        for (String s : path.split("/")) {
            if (s.isEmpty()) {
                continue;
            }
            list.add(new StringSegment(s));
        }
        return list;
    }

    private String createSortKey() {
        StringBuilder sb = new StringBuilder();
        if (segments.size() == 1 && (segments.get(0) instanceof PatternSegment || segments.get(0) instanceof CatchAllSegment)) {
            sb.append("1");
        } else {
            sb.append("0");
        }
        sb.append(String.format("%03d", path.length()));
        return sb.toString();
    }

    private static boolean isCatchAll(String s) {
        return "**".equals(s);
    }

    private static boolean isParameter(String s) {
        return s.startsWith("{") && s.endsWith("}");
    }

    private static boolean isPattern(String s) {
        return s.startsWith("glob:");
    }

    public interface RouteSegment {
        MatchResult match(RouteSegment segment);

    }

    private static class CatchAllSegment implements RouteSegment {

        @Override
        public MatchResult match(RouteSegment segment) {
            return MatchResult.ALWAYS;
        }

        @Override
        public String toString() {
            return "CATCH_ALL[**]";
        }
    }

    private static class StringSegment implements RouteSegment {

        private final String string;

        StringSegment(String string) {
            this.string = string;
        }

        @Override
        public MatchResult match(RouteSegment segment) {
            if (segment instanceof StringSegment) {
                return string.equals(((StringSegment) segment).string) ? MatchResult.TRUE : MatchResult.FALSE;
            } else if (segment instanceof CatchAllSegment) {
                return MatchResult.ALWAYS;
            } else if (segment instanceof PatternSegment) {
                Path path = Paths.get(string);
                return ((PatternSegment) segment).pathMatcher.matches(path) ? MatchResult.TRUE : MatchResult.FALSE;
            } else if (segment instanceof ParameterSegment) {
                ValueMatchResult matchResult = new ValueMatchResult();
                matchResult.setValue(string);
                return matchResult;
            }
            return MatchResult.FALSE;
        }

        @Override
        public String toString() {
            return "STRING:[" + string + "]";
        }
    }

    private static class PatternSegment implements RouteSegment {

        private final PathMatcher pathMatcher;

        PatternSegment(String regex) {
            this.pathMatcher = FileSystems.getDefault().getPathMatcher(regex);
        }

        @Override
        public MatchResult match(RouteSegment segment) {
            if (segment instanceof StringSegment) {
                Path path = Paths.get(((StringSegment) segment).string);
                return pathMatcher.matches(path) ? MatchResult.TRUE : MatchResult.FALSE;
            } else if (segment instanceof CatchAllSegment) {
                return MatchResult.ALWAYS;
            } else if (segment instanceof PatternSegment) {
                return MatchResult.FALSE;
            } else if (segment instanceof ParameterSegment) {
                return MatchResult.FALSE;
            }
            return MatchResult.FALSE;
        }

        @Override
        public String toString() {
            return "PATTERN:[" + pathMatcher + "]";
        }
    }

    private static class ParameterSegment implements RouteSegment {

        private final String name;

        ParameterSegment(String name) {
            this.name = name.substring(1, name.length() -1);
        }

        public String getName() {
            return name;
        }

        @Override
        public MatchResult match(RouteSegment segment) {
            ValueMatchResult matchResult = new ValueMatchResult();
            if (segment instanceof StringSegment) {
                matchResult.setValue(((StringSegment) segment).string);
            }
            return matchResult;
        }

        @Override
        public String toString() {
            return "PARAMETER:{" + name + "}";
        }
    }

    public interface MatchResult {
        MatchResult TRUE = new TrueMatchResult();

        MatchResult FALSE = new FalseMatchResult();

        MatchResult ALWAYS = new AlwaysMatchResult();
    }

    private static class TrueMatchResult implements MatchResult {
        @Override
        public String toString() {
            return "TRUE";
        }
    }

    private static class FalseMatchResult implements MatchResult {
        @Override
        public String toString() {
            return "FALSE";
        }
    }

    private static class AlwaysMatchResult implements MatchResult {
        @Override
        public String toString() {
            return "ALWAYS";
        }
    }

    private static class ValueMatchResult implements MatchResult {
        String value;
        void setValue(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "VALUE";
        }
    }

    private static final CatchAllSegment CATCHALL = new CatchAllSegment();
}
