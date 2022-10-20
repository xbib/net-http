package org.xbib.net.http.server.ldap;

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class Krb5LoginConfiguration extends Configuration {

    private static final AppConfigurationEntry[] CONFIG_LIST = new AppConfigurationEntry[1];

    static {
        String loginModule = "com.sun.security.auth.module.Krb5LoginModule";
        AppConfigurationEntry.LoginModuleControlFlag flag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        CONFIG_LIST[0] = new AppConfigurationEntry(loginModule, flag, new HashMap<>());
    }

    public Krb5LoginConfiguration() {
        super();
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
        return CONFIG_LIST.clone();
    }

    @Override
    public void refresh() {
    }
}
