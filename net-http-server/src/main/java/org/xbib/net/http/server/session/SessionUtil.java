package org.xbib.net.http.server.session;

import java.util.Locale;

public class SessionUtil {

    private SessionUtil() {
    }

    public static String extractExtension(String path) {
        if (path == null) {
            return null;
        }
        String s = path;
        if (s.endsWith(".gz")) {
            s = s.substring(0, s.lastIndexOf('.'));
        }
        int dotIdx = s.lastIndexOf('.');
        int slashIdx = s.lastIndexOf('/');
        if (dotIdx < 0 || slashIdx > dotIdx) {
            return null;
        }
        return s.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }
}
