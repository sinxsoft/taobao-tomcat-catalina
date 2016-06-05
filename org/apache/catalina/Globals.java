package org.apache.catalina;

import java.util.Locale;

public final class Globals
{
  public static final String ALT_DD_ATTR = "org.apache.catalina.deploy.alt_dd";
  public static final String CERTIFICATES_ATTR = "javax.servlet.request.X509Certificate";
  public static final String CIPHER_SUITE_ATTR = "javax.servlet.request.cipher_suite";
  public static final String DISPATCHER_TYPE_ATTR = "org.apache.catalina.core.DISPATCHER_TYPE";
  public static final String DISPATCHER_REQUEST_PATH_ATTR = "org.apache.catalina.core.DISPATCHER_REQUEST_PATH";
  public static final String RESOURCES_ATTR = "org.apache.catalina.resources";
  public static final String CLASS_PATH_ATTR = "org.apache.catalina.jsp_classpath";
  public static final String KEY_SIZE_ATTR = "javax.servlet.request.key_size";
  public static final String SSL_SESSION_ID_ATTR = "javax.servlet.request.ssl_session_id";
  @Deprecated
  public static final String SSL_SESSION_ID_TOMCAT_ATTR = "javax.servlet.request.ssl_session";
  public static final String SSL_SESSION_MGR_ATTR = "javax.servlet.request.ssl_session_mgr";
  @Deprecated
  public static final String MBEAN_REGISTRY_ATTR = "org.apache.catalina.Registry";
  @Deprecated
  public static final String MBEAN_SERVER_ATTR = "org.apache.catalina.MBeanServer";
  public static final String NAMED_DISPATCHER_ATTR = "org.apache.catalina.NAMED";
  public static final String SSI_FLAG_ATTR = "org.apache.catalina.ssi.SSIServlet";
  public static final String SUBJECT_ATTR = "javax.security.auth.subject";
  public static final String GSS_CREDENTIAL_ATTR = "org.apache.catalina.realm.GSS_CREDENTIAL";
  @Deprecated
  public static final String TOMCAT_CONNECTOR_ATTR_PREFIX = "org.apache.tomcat.";
  public static final String COMET_SUPPORTED_ATTR = "org.apache.tomcat.comet.support";
  public static final String COMET_TIMEOUT_SUPPORTED_ATTR = "org.apache.tomcat.comet.timeout.support";
  public static final String COMET_TIMEOUT_ATTR = "org.apache.tomcat.comet.timeout";
  public static final String SENDFILE_SUPPORTED_ATTR = "org.apache.tomcat.sendfile.support";
  public static final String SENDFILE_FILENAME_ATTR = "org.apache.tomcat.sendfile.filename";
  public static final String SENDFILE_FILE_START_ATTR = "org.apache.tomcat.sendfile.start";
  public static final String SENDFILE_FILE_END_ATTR = "org.apache.tomcat.sendfile.end";
  public static final String REMOTE_ADDR_ATTRIBUTE = "org.apache.tomcat.remoteAddr";
  public static final String ASYNC_SUPPORTED_ATTR = "org.apache.catalina.ASYNC_SUPPORTED";
  public static final String PARAMETER_PARSE_FAILED_ATTR = "org.apache.catalina.parameter_parse_failed";
  public static final boolean STRICT_SERVLET_COMPLIANCE = Boolean.valueOf(System.getProperty("org.apache.catalina.STRICT_SERVLET_COMPLIANCE", "false")).booleanValue();
  public static final boolean IS_SECURITY_ENABLED = System.getSecurityManager() != null;
  public static final String DEFAULT_MBEAN_DOMAIN = "Catalina";
  public static final String CATALINA_HOME_PROP = "catalina.home";
  public static final String CATALINA_BASE_PROP = "catalina.base";
  public static final String JASPER_XML_VALIDATION_TLD_INIT_PARAM = "org.apache.jasper.XML_VALIDATE_TLD";
  public static final String JASPER_XML_VALIDATION_INIT_PARAM = "org.apache.jasper.XML_VALIDATE";
  public static final String JASPER_XML_BLOCK_EXTERNAL_INIT_PARAM = "org.apache.jasper.XML_BLOCK_EXTERNAL";
  public static final boolean IS_ORACLE_JVM;
  public static final boolean IS_IBM_JVM;
  
  public Globals() {}
  
  static
  {
    String vendor = System.getProperty("java.vendor", "");
    vendor = vendor.toLowerCase(Locale.ENGLISH);
    if ((vendor.startsWith("oracle")) || (vendor.startsWith("sun")))
    {
      IS_ORACLE_JVM = true;
      IS_IBM_JVM = false;
    }
    else if (vendor.contains("ibm"))
    {
      IS_ORACLE_JVM = false;
      IS_IBM_JVM = true;
    }
    else
    {
      IS_ORACLE_JVM = false;
      IS_IBM_JVM = false;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Globals.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */