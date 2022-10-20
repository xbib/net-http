package org.xbib.net.http;

import org.xbib.datastructures.common.Pair;
import org.xbib.net.Headers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class HttpHeaders implements Headers {

    private final List<Pair<String, String>> list;

    public HttpHeaders() {
        this.list = new ArrayList<>();
    }

    public HttpHeaders(List<Pair<String, String>> list) {
        this.list = list;
    }

    public static HttpHeaders of(List<Pair<String, String>> list) {
        return new HttpHeaders(list);
    }

    public static HttpHeaders of(HttpHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.entries().forEach(e -> httpHeaders.add(e.getKey(), e.getValue()));
        return httpHeaders;
    }

    public HttpHeaders add(CharSequence name, String value) {
        if (name != null && value != null) {
            list.add(Pair.of(name.toString(), value));
        }
        return this;
    }

    public HttpHeaders add(CharSequence name, Iterable<?> values) {
        values.forEach(v -> {
            if (v != null) {
                list.add(Pair.of(name.toString(), v.toString()));
            }
        });
        return this;
    }

    public HttpHeaders set(CharSequence name, String value) {
        if (name != null && value != null) {
            List<Pair<String, String>> list = this.list.stream()
                    .filter(e -> !e.getKey().equalsIgnoreCase(name.toString())).collect(Collectors.toList());
            this.list.clear();
            this.list.addAll(list);
        }
        return add(name, value);
    }

    public boolean containsHeader(CharSequence name) {
        String k = name.toString();
        Pair<String, String> me = list.stream().filter(e -> e.getKey().equalsIgnoreCase(k)).findFirst().orElse(null);
        return me != null;
    }

    public void remove(CharSequence name) {
        this.list.removeIf(pair -> pair.getKey().equals(name.toString()));
    }

    @Override
    public String get(CharSequence header) {
        String k = header.toString();
        return list.stream().filter(e -> e.getKey().equalsIgnoreCase(k))
                .map(Pair::getValue).findFirst().orElse(null);
    }

    @Override
    public List<String> getAll(CharSequence header) {
        String k = header.toString();
        return list.stream().filter(e -> e.getKey().equalsIgnoreCase(k))
                .map(Pair::getValue).collect(Collectors.toList());
    }

    @Override
    public List<Pair<String, String>> entries() {
        return list;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
