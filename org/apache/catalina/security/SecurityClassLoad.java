package org.apache.catalina.security;

public final class SecurityClassLoad
{
  public SecurityClassLoad() {}
  
  public static void securityClassLoad(ClassLoader loader)
    throws Exception
  {
    if (System.getSecurityManager() == null) {
      return;
    }
    loadCorePackage(loader);
    loadCoyotePackage(loader);
    loadLoaderPackage(loader);
    loadRealmPackage(loader);
    loadServletsPackage(loader);
    loadSessionPackage(loader);
    loadUtilPackage(loader);
    loadValvesPackage(loader);
    loadJavaxPackage(loader);
    loadConnectorPackage(loader);
    loadTomcatPackage(loader);
  }
  
  private static final void loadCorePackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.core.";
    loader.loadClass("org.apache.catalina.core.AccessLogAdapter");
    
    loader.loadClass("org.apache.catalina.core.ApplicationContextFacade$1");
    
    loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedForward");
    
    loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedInclude");
    
    loader.loadClass("org.apache.catalina.core.AsyncContextImpl");
    
    loader.loadClass("org.apache.catalina.core.AsyncContextImpl$DebugException");
    
    loader.loadClass("org.apache.catalina.core.AsyncContextImpl$1");
    
    loader.loadClass("org.apache.catalina.core.AsyncListenerWrapper");
    
    loader.loadClass("org.apache.catalina.core.ContainerBase$PrivilegedAddChild");
    
    loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$1");
    
    loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$2");
    
    loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$3");
    
    loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$AnnotationCacheEntry");
    
    loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$AnnotationCacheEntryType");
    
    loader.loadClass("org.apache.catalina.core.ApplicationHttpRequest$AttributeNamesEnumerator");
  }
  
  private static final void loadLoaderPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.loader.";
    loader.loadClass("org.apache.catalina.loader.WebappClassLoader$PrivilegedFindResourceByName");
  }
  
  private static final void loadRealmPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.realm.";
    loader.loadClass("org.apache.catalina.realm.LockOutRealm$LockRecord");
  }
  
  private static final void loadServletsPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.servlets.";
    
    loader.loadClass("org.apache.catalina.servlets.DefaultServlet");
  }
  
  private static final void loadSessionPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.session.";
    loader.loadClass("org.apache.catalina.session.StandardSession");
    
    loader.loadClass("org.apache.catalina.session.StandardSession$1");
    
    loader.loadClass("org.apache.catalina.session.StandardManager$PrivilegedDoUnload");
  }
  
  private static final void loadUtilPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.util.";
    loader.loadClass("org.apache.catalina.util.Enumerator");
    loader.loadClass("org.apache.catalina.util.ParameterMap");
  }
  
  private static final void loadValvesPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.valves.";
    loader.loadClass("org.apache.catalina.valves.AccessLogValve$3");
  }
  
  private static final void loadCoyotePackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.coyote.";
    loader.loadClass("org.apache.coyote.http11.AbstractOutputBuffer$1");
    loader.loadClass("org.apache.coyote.http11.Constants");
    
    Class<?> clazz = loader.loadClass("org.apache.coyote.Constants");
    clazz.newInstance();
  }
  
  private static final void loadJavaxPackage(ClassLoader loader)
    throws Exception
  {
    loader.loadClass("javax.servlet.http.Cookie");
  }
  
  private static final void loadConnectorPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.catalina.connector.";
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetAttributePrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterMapPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetRequestDispatcherPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterNamesPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterValuePrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetCharacterEncodingPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetHeadersPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetHeaderNamesPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetCookiesPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetLocalePrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetLocalesPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.ResponseFacade$SetContentTypePrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.ResponseFacade$DateHeaderPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.RequestFacade$GetSessionPrivilegedAction");
    
    loader.loadClass("org.apache.catalina.connector.ResponseFacade$1");
    
    loader.loadClass("org.apache.catalina.connector.OutputBuffer$1");
    
    loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$1");
    
    loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$2");
    
    loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$3");
    
    loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$4");
    
    loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$5");
    
    loader.loadClass("org.apache.catalina.connector.InputBuffer$1");
    
    loader.loadClass("org.apache.catalina.connector.Response$1");
    
    loader.loadClass("org.apache.catalina.connector.Response$2");
    
    loader.loadClass("org.apache.catalina.connector.Response$3");
  }
  
  private static final void loadTomcatPackage(ClassLoader loader)
    throws Exception
  {
    String basePackage = "org.apache.tomcat.";
    
    loader.loadClass("org.apache.tomcat.util.buf.HexUtils");
    loader.loadClass("org.apache.tomcat.util.buf.StringCache");
    loader.loadClass("org.apache.tomcat.util.buf.StringCache$ByteEntry");
    loader.loadClass("org.apache.tomcat.util.buf.StringCache$CharEntry");
    
    loader.loadClass("org.apache.tomcat.util.http.HttpMessages");
    
    Class<?> clazz = loader.loadClass("org.apache.tomcat.util.http.FastHttpDateFormat");
    
    clazz.newInstance();
    loader.loadClass("org.apache.tomcat.util.http.HttpMessages");
    loader.loadClass("org.apache.tomcat.util.http.parser.HttpParser");
    loader.loadClass("org.apache.tomcat.util.http.parser.HttpParser$SkipConstantResult");
    loader.loadClass("org.apache.tomcat.util.http.parser.MediaType");
    loader.loadClass("org.apache.tomcat.util.http.parser.MediaTypeCache");
    
    loader.loadClass("org.apache.tomcat.util.net.Constants");
    loader.loadClass("org.apache.tomcat.util.net.NioBlockingSelector$BlockPoller$1");
    
    loader.loadClass("org.apache.tomcat.util.net.NioBlockingSelector$BlockPoller$2");
    
    loader.loadClass("org.apache.tomcat.util.net.NioBlockingSelector$BlockPoller$3");
    
    loader.loadClass("org.apache.tomcat.util.net.SSLSupport$CipherData");
    
    loader.loadClass("org.apache.tomcat.util.security.PrivilegedGetTccl");
    loader.loadClass("org.apache.tomcat.util.security.PrivilegedSetTccl");
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\security\SecurityClassLoad.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */