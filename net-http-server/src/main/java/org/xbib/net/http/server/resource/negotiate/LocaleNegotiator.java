package org.xbib.net.http.server.resource.negotiate;

import java.util.Locale;
import java.util.StringTokenizer;

public class LocaleNegotiator {

    private LocaleNegotiator() {
    }

    public static Locale findLocale(String languages) {
        Locale locale = null;
        if (languages != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(languages, ",");
            if (stringTokenizer.hasMoreTokens()) {
                String s = stringTokenizer.nextToken();
                int pos;
                String lang = s;
                if ((pos = lang.indexOf(';')) != -1) {
                    lang = lang.substring(0, pos);
                }
                lang = lang.trim();
                if ((pos = lang.indexOf('-')) == -1) {
                    locale = new Locale(lang, Locale.getDefault().getCountry());
                } else {
                    locale = new Locale(lang.substring(0, pos), lang.substring(pos + 1));
                }
            }
        }
        return locale;
    }
}
