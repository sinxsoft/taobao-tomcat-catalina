package org.apache.catalina.authenticator;

public class Constants
{
  public static final String Package = "org.apache.catalina.authenticator";
  @Deprecated
  public static final String BASIC_METHOD = "BASIC";
  @Deprecated
  public static final String CERT_METHOD = "CLIENT_CERT";
  @Deprecated
  public static final String DIGEST_METHOD = "DIGEST";
  @Deprecated
  public static final String FORM_METHOD = "FORM";
  public static final String SPNEGO_METHOD = "SPNEGO";
  public static final String FORM_ACTION = "/j_security_check";
  public static final String FORM_PASSWORD = "j_password";
  public static final String FORM_USERNAME = "j_username";
  public static final String KRB5_CONF_PROPERTY = "java.security.krb5.conf";
  public static final String DEFAULT_KRB5_CONF = "conf/krb5.ini";
  public static final String JAAS_CONF_PROPERTY = "java.security.auth.login.config";
  public static final String DEFAULT_JAAS_CONF = "conf/jaas.conf";
  public static final String DEFAULT_LOGIN_MODULE_NAME = "com.sun.security.jgss.krb5.accept";
  public static final String USE_SUBJECT_CREDS_ONLY_PROPERTY = "javax.security.auth.useSubjectCredsOnly";
  public static final String SINGLE_SIGN_ON_COOKIE = System.getProperty("org.apache.catalina.authenticator.Constants.SSO_SESSION_COOKIE_NAME", "JSESSIONIDSSO");
  public static final String REQ_SSOID_NOTE = "org.apache.catalina.request.SSOID";
  public static final String SESS_PASSWORD_NOTE = "org.apache.catalina.session.PASSWORD";
  public static final String SESS_USERNAME_NOTE = "org.apache.catalina.session.USERNAME";
  public static final String FORM_PRINCIPAL_NOTE = "org.apache.catalina.authenticator.PRINCIPAL";
  public static final String FORM_REQUEST_NOTE = "org.apache.catalina.authenticator.REQUEST";
  
  public Constants() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\Constants.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */