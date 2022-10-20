package org.xbib.net.http.server.ldap;


@SuppressWarnings("serial")
public class LdapException extends RuntimeException {

  public LdapException(String message) {
    super(message);
  }

  public LdapException(String message, Throwable cause) {
    super(message, cause);
  }

}
