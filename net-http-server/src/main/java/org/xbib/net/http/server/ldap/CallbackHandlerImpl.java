package org.xbib.net.http.server.ldap;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class CallbackHandlerImpl implements CallbackHandler {

    private final String name;

    private final String password;

    public CallbackHandlerImpl(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException, IOException {
        for (Callback callBack : callbacks) {
            if (callBack instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callBack;
                nameCallback.setName(name);
            } else if (callBack instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callBack;
                passwordCallback.setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callBack, "Callback not supported");
            }
        }
    }
}
