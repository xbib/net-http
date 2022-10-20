package org.xbib.net.http.server.route.base;

import org.junit.jupiter.api.Test;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.route.BaseHttpRoute;
import org.xbib.net.http.server.route.BaseHttpRouteResolver;
import org.xbib.net.http.server.route.HttpRoute;
import org.xbib.net.http.server.route.HttpRouteResolver;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseHttpRouteResolverTest {

    @Test
    public void testEmptyRouteResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> atomicInteger.incrementAndGet());
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testNoDefinedRouteResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> atomicInteger.incrementAndGet());
        assertEquals(0, atomicInteger.get());
    }

    @Test
    public void testEmptyRouteMatchResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/path", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/path");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteMismatchResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/path1", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/path2");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(0, atomicInteger.get());
    }

    @Test
    public void testSingleRouteMismatchTooLongResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a/b");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(0, atomicInteger.get());
    }

    @Test
    public void testSingleRouteCatchAllResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/**", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/path");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteCatchAllLongPathResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/**", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a/very/long/path");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteJpegResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "glob:*.jpg", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "abc.jpg");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteGlobJpegResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "glob:**.jpg", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a/picture/abc.jpg");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testSingleRouteParameterResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/{token}", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/abcdef");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            assertEquals("[token=abcdef]", r.getParameter().allToString());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testTwoRouteParameterResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/{token}/{key}", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/abcdef/123456");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            assertEquals("[token=abcdef, key=123456]", r.getParameter().allToString());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testMultiRouteResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/**", 1)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/**", 2)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/c", 3)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, (int) r.getValue());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testMultiRouteLongestFirstResolver() {
        HttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/**", 1)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/c/**", 2)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/c/d/e/f/g", 3)
                .sort(true)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            switch (atomicInteger.get()) {
                case 0:
                    assertEquals(1, (int) r.getValue());
                    break;
            }
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void testMultiRouteLongestFirstWithGlobFirstResolver() {
        HttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/**", 4)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/c/**", 3)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/a/b/c/d/e/f/g", 2)
                .add(HttpAddress.http1("host"), HttpMethod.POST, "glob:**", 1)
                .sort(true)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/a");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            switch (atomicInteger.get()) {
                case 0:
                    assertEquals(1, (int) r.getValue());
                    break;
                case 1:
                    assertEquals(4, (int) r.getValue());
                    break;
            }
            atomicInteger.incrementAndGet();
        });
        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void testContextRouteResolver() {
        BaseHttpRouteResolver.Builder<Integer> builder = BaseHttpRouteResolver.builder();
        HttpRouteResolver<Integer> resolver = builder
                .setPrefix("/app")
                .add(HttpAddress.http1("host"), HttpMethod.POST, "/path", 1)
                .build();
        HttpRoute route = new BaseHttpRoute(HttpAddress.http1("host"), HttpMethod.POST, "/app/path");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        resolver.resolve(route, r -> {
            assertEquals(1, r.getValue());
            assertEquals("[path]", r.getContext().toString());
            atomicInteger.incrementAndGet();
        });
        assertEquals(1, atomicInteger.get());
    }
}
