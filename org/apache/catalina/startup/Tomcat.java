package org.apache.catalina.startup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.NamingContextListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

public class Tomcat
{
  protected Server server;
  protected Service service;
  protected Engine engine;
  protected Connector connector;
  protected Host host;
  protected int port = 8080;
  protected String hostname = "localhost";
  protected String basedir;
  @Deprecated
  protected Realm defaultRealm;
  private final Map<String, String> userPass = new HashMap();
  private final Map<String, List<String>> userRoles = new HashMap();
  private final Map<String, Principal> userPrincipals = new HashMap();
  
  public Tomcat() {}
  
  public void setBaseDir(String basedir)
  {
    this.basedir = basedir;
  }
  
  public void setPort(int port)
  {
    this.port = port;
  }
  
  public void setHostname(String s)
  {
    this.hostname = s;
  }
  
  public Context addWebapp(String contextPath, String baseDir)
    throws ServletException
  {
    return addWebapp(getHost(), contextPath, baseDir);
  }
  
  public Context addContext(String contextPath, String baseDir)
  {
    return addContext(getHost(), contextPath, baseDir);
  }
  
  public Wrapper addServlet(String contextPath, String servletName, String servletClass)
  {
    Container ctx = getHost().findChild(contextPath);
    return addServlet((Context)ctx, servletName, servletClass);
  }
  
  public static Wrapper addServlet(Context ctx, String servletName, String servletClass)
  {
    Wrapper sw = ctx.createWrapper();
    sw.setServletClass(servletClass);
    sw.setName(servletName);
    ctx.addChild(sw);
    
    return sw;
  }
  
  public Wrapper addServlet(String contextPath, String servletName, Servlet servlet)
  {
    Container ctx = getHost().findChild(contextPath);
    return addServlet((Context)ctx, servletName, servlet);
  }
  
  public static Wrapper addServlet(Context ctx, String servletName, Servlet servlet)
  {
    Wrapper sw = new ExistingStandardWrapper(servlet);
    sw.setName(servletName);
    ctx.addChild(sw);
    
    return sw;
  }
  
  public void init()
    throws LifecycleException
  {
    getServer();
    getConnector();
    this.server.init();
  }
  
  public void start()
    throws LifecycleException
  {
    getServer();
    getConnector();
    this.server.start();
  }
  
  public void stop()
    throws LifecycleException
  {
    getServer();
    this.server.stop();
  }
  
  public void destroy()
    throws LifecycleException
  {
    getServer();
    this.server.destroy();
  }
  
  public void addUser(String user, String pass)
  {
    this.userPass.put(user, pass);
  }
  
  public void addRole(String user, String role)
  {
    List<String> roles = (List)this.userRoles.get(user);
    if (roles == null)
    {
      roles = new ArrayList();
      this.userRoles.put(user, roles);
    }
    roles.add(role);
  }
  
  public Connector getConnector()
  {
    getServer();
    if (this.connector != null) {
      return this.connector;
    }
    this.connector = new Connector("HTTP/1.1");
    
    this.connector.setPort(this.port);
    this.service.addConnector(this.connector);
    return this.connector;
  }
  
  public void setConnector(Connector connector)
  {
    this.connector = connector;
  }
  
  public Service getService()
  {
    getServer();
    return this.service;
  }
  
  public void setHost(Host host)
  {
    this.host = host;
  }
  
  public Host getHost()
  {
    if (this.host == null)
    {
      this.host = new StandardHost();
      this.host.setName(this.hostname);
      
      getEngine().addChild(this.host);
    }
    return this.host;
  }
  
  @Deprecated
  public void setDefaultRealm(Realm realm)
  {
    this.defaultRealm = realm;
  }
  
  public Engine getEngine()
  {
    if (this.engine == null)
    {
      getServer();
      this.engine = new StandardEngine();
      this.engine.setName("Tomcat");
      this.engine.setDefaultHost(this.hostname);
      if (this.defaultRealm == null) {
        initSimpleAuth();
      }
      this.engine.setRealm(this.defaultRealm);
      this.service.setContainer(this.engine);
    }
    return this.engine;
  }
  
  public Server getServer()
  {
    if (this.server != null) {
      return this.server;
    }
    initBaseDir();
    
    System.setProperty("catalina.useNaming", "false");
    
    this.server = new StandardServer();
    this.server.setPort(-1);
    
    this.service = new StandardService();
    this.service.setName("Tomcat");
    this.server.addService(this.service);
    return this.server;
  }
  
  public Context addContext(Host host, String contextPath, String dir)
  {
    return addContext(host, contextPath, contextPath, dir);
  }
  
  public Context addContext(Host host, String contextPath, String contextName, String dir)
  {
    silence(host, contextPath);
    Context ctx = createContext(host, contextPath);
    ctx.setName(contextName);
    ctx.setPath(contextPath);
    ctx.setDocBase(dir);
    ctx.addLifecycleListener(new FixContextListener());
    if (host == null) {
      getHost().addChild(ctx);
    } else {
      host.addChild(ctx);
    }
    return ctx;
  }
  
  public Context addWebapp(Host host, String url, String path)
  {
    return addWebapp(host, url, url, path);
  }
  
  public Context addWebapp(Host host, String url, String name, String path)
  {
    silence(host, url);
    
    Context ctx = createContext(host, url);
    ctx.setName(name);
    ctx.setPath(url);
    ctx.setDocBase(path);
    
    ctx.addLifecycleListener(new DefaultWebXmlListener());
    ctx.setConfigFile(getWebappConfigFile(path, url));
    
    ContextConfig ctxCfg = new ContextConfig();
    ctx.addLifecycleListener(ctxCfg);
    
    ctxCfg.setDefaultWebXml(noDefaultWebXmlPath());
    if (host == null) {
      getHost().addChild(ctx);
    } else {
      host.addChild(ctx);
    }
    return ctx;
  }
  
  public LifecycleListener getDefaultWebXmlListener()
  {
    return new DefaultWebXmlListener();
  }
  
  public String noDefaultWebXmlPath()
  {
    return "org/apache/catalina/startup/NO_DEFAULT_XML";
  }
  
  @Deprecated
  public Realm getDefaultRealm()
  {
    if (this.defaultRealm == null) {
      initSimpleAuth();
    }
    return this.defaultRealm;
  }
  
  @Deprecated
  protected void initSimpleAuth()
  {
    this.defaultRealm = new RealmBase()
    {
      protected String getName()
      {
        return "Simple";
      }
      
      protected String getPassword(String username)
      {
        return (String)Tomcat.this.userPass.get(username);
      }
      
      protected Principal getPrincipal(String username)
      {
        Principal p = (Principal)Tomcat.this.userPrincipals.get(username);
        if (p == null)
        {
          String pass = (String)Tomcat.this.userPass.get(username);
          if (pass != null)
          {
            p = new GenericPrincipal(username, pass, (List)Tomcat.this.userRoles.get(username));
            
            Tomcat.this.userPrincipals.put(username, p);
          }
        }
        return p;
      }
    };
  }
  
  protected void initBaseDir()
  {
    String catalinaHome = System.getProperty("catalina.home");
    if (this.basedir == null) {
      this.basedir = System.getProperty("catalina.base");
    }
    if (this.basedir == null) {
      this.basedir = catalinaHome;
    }
    if (this.basedir == null)
    {
      this.basedir = (System.getProperty("user.dir") + "/tomcat." + this.port);
      
      File home = new File(this.basedir);
      home.mkdir();
      if (!home.isAbsolute()) {
        try
        {
          this.basedir = home.getCanonicalPath();
        }
        catch (IOException e)
        {
          this.basedir = home.getAbsolutePath();
        }
      }
    }
    if (catalinaHome == null) {
      System.setProperty("catalina.home", this.basedir);
    }
    System.setProperty("catalina.base", this.basedir);
  }
  
  static final String[] silences = { "org.apache.coyote.http11.Http11Protocol", "org.apache.catalina.core.StandardService", "org.apache.catalina.core.StandardEngine", "org.apache.catalina.startup.ContextConfig", "org.apache.catalina.core.ApplicationContext", "org.apache.catalina.core.AprLifecycleListener" };
  
  public void setSilent(boolean silent)
  {
    for (String s : silences) {
      if (silent) {
        Logger.getLogger(s).setLevel(Level.WARNING);
      } else {
        Logger.getLogger(s).setLevel(Level.INFO);
      }
    }
  }
  
  private void silence(Host host, String ctx)
  {
    Logger.getLogger(getLoggerName(host, ctx)).setLevel(Level.WARNING);
  }
  
  private String getLoggerName(Host host, String ctx)
  {
    String loggerName = "org.apache.catalina.core.ContainerBase.[default].[";
    if (host == null) {
      loggerName = loggerName + getHost().getName();
    } else {
      loggerName = loggerName + host.getName();
    }
    loggerName = loggerName + "].[";
    loggerName = loggerName + ctx;
    loggerName = loggerName + "]";
    return loggerName;
  }
  
  private Context createContext(Host host, String url)
  {
    String contextClass = StandardContext.class.getName();
    if (host == null) {
      host = getHost();
    }
    if ((host instanceof StandardHost)) {
      contextClass = ((StandardHost)host).getContextClass();
    }
    try
    {
      return (Context)Class.forName(contextClass).getConstructor(new Class[0]).newInstance(new Object[0]);
    }
    catch (InstantiationException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (IllegalArgumentException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (InvocationTargetException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (NoSuchMethodException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (SecurityException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
    catch (ClassNotFoundException e)
    {
      throw new IllegalArgumentException("Can't instantiate context-class " + contextClass + " for host " + host + " and url " + url, e);
    }
  }
  
  public void enableNaming()
  {
    getServer();
    this.server.addLifecycleListener(new NamingContextListener());
    
    System.setProperty("catalina.useNaming", "true");
    
    String value = "org.apache.naming";
    String oldValue = System.getProperty("java.naming.factory.url.pkgs");
    if (oldValue != null) {
      if (oldValue.contains(value)) {
        value = oldValue;
      } else {
        value = value + ":" + oldValue;
      }
    }
    System.setProperty("java.naming.factory.url.pkgs", value);
    
    value = System.getProperty("java.naming.factory.initial");
    if (value == null) {
      System.setProperty("java.naming.factory.initial", "org.apache.naming.java.javaURLContextFactory");
    }
  }
  
  public void initWebappDefaults(String contextPath)
  {
    Container ctx = getHost().findChild(contextPath);
    initWebappDefaults((Context)ctx);
  }
  
  public static void initWebappDefaults(Context ctx)
  {
    Wrapper servlet = addServlet(ctx, "default", "org.apache.catalina.servlets.DefaultServlet");
    
    servlet.setLoadOnStartup(1);
    servlet.setOverridable(true);
    
    servlet = addServlet(ctx, "jsp", "org.apache.jasper.servlet.JspServlet");
    
    servlet.addInitParameter("fork", "false");
    servlet.setLoadOnStartup(3);
    servlet.setOverridable(true);
    
    ctx.addServletMapping("/", "default");
    ctx.addServletMapping("*.jsp", "jsp");
    ctx.addServletMapping("*.jspx", "jsp");
    
    ctx.setSessionTimeout(30);
    for (int i = 0; i < DEFAULT_MIME_MAPPINGS.length;) {
      ctx.addMimeMapping(DEFAULT_MIME_MAPPINGS[(i++)], DEFAULT_MIME_MAPPINGS[(i++)]);
    }
    ctx.addWelcomeFile("index.html");
    ctx.addWelcomeFile("index.htm");
    ctx.addWelcomeFile("index.jsp");
  }
  
  public static class FixContextListener
    implements LifecycleListener
  {
    public FixContextListener() {}
    
    public void lifecycleEvent(LifecycleEvent event)
    {
      try
      {
        Context context = (Context)event.getLifecycle();
        if (event.getType().equals("configure_start")) {
          context.setConfigured(true);
        }
        if (context.getLoginConfig() == null)
        {
          context.setLoginConfig(new LoginConfig("NONE", null, null, null));
          
          context.getPipeline().addValve(new NonLoginAuthenticator());
        }
      }
      catch (ClassCastException e) {}
    }
  }
  
  public static class DefaultWebXmlListener
    implements LifecycleListener
  {
    public DefaultWebXmlListener() {}
    
    public void lifecycleEvent(LifecycleEvent event)
    {
      if ("before_start".equals(event.getType())) {
        Tomcat.initWebappDefaults((Context)event.getLifecycle());
      }
    }
  }
  
  public static class ExistingStandardWrapper
    extends StandardWrapper
  {
    private final Servlet existing;
    
    public ExistingStandardWrapper(Servlet existing)
    {
      this.existing = existing;
      if ((existing instanceof SingleThreadModel))
      {
        this.singleThreadModel = true;
        this.instancePool = new Stack();
      }
    }
    
    public synchronized Servlet loadServlet()
      throws ServletException
    {
      if (this.singleThreadModel)
      {
        Servlet instance;
        try
        {
          instance = (Servlet)this.existing.getClass().newInstance();
        }
        catch (InstantiationException e)
        {
          throw new ServletException(e);
        }
        catch (IllegalAccessException e)
        {
          throw new ServletException(e);
        }
        instance.init(this.facade);
        return instance;
      }
      if (!this.instanceInitialized)
      {
        this.existing.init(this.facade);
        this.instanceInitialized = true;
      }
      return this.existing;
    }
    
    public long getAvailable()
    {
      return 0L;
    }
    
    public boolean isUnavailable()
    {
      return false;
    }
    
    public Servlet getServlet()
    {
      return this.existing;
    }
    
    public String getServletClass()
    {
      return this.existing.getClass().getName();
    }
  }
  
  private static final String[] DEFAULT_MIME_MAPPINGS = { "abs", "audio/x-mpeg", "ai", "application/postscript", "aif", "audio/x-aiff", "aifc", "audio/x-aiff", "aiff", "audio/x-aiff", "aim", "application/x-aim", "art", "image/x-jg", "asf", "video/x-ms-asf", "asx", "video/x-ms-asf", "au", "audio/basic", "avi", "video/x-msvideo", "avx", "video/x-rad-screenplay", "bcpio", "application/x-bcpio", "bin", "application/octet-stream", "bmp", "image/bmp", "body", "text/html", "cdf", "application/x-cdf", "cer", "application/pkix-cert", "class", "application/java", "cpio", "application/x-cpio", "csh", "application/x-csh", "css", "text/css", "dib", "image/bmp", "doc", "application/msword", "dtd", "application/xml-dtd", "dv", "video/x-dv", "dvi", "application/x-dvi", "eps", "application/postscript", "etx", "text/x-setext", "exe", "application/octet-stream", "gif", "image/gif", "gtar", "application/x-gtar", "gz", "application/x-gzip", "hdf", "application/x-hdf", "hqx", "application/mac-binhex40", "htc", "text/x-component", "htm", "text/html", "html", "text/html", "ief", "image/ief", "jad", "text/vnd.sun.j2me.app-descriptor", "jar", "application/java-archive", "java", "text/x-java-source", "jnlp", "application/x-java-jnlp-file", "jpe", "image/jpeg", "jpeg", "image/jpeg", "jpg", "image/jpeg", "js", "application/javascript", "jsf", "text/plain", "jspf", "text/plain", "kar", "audio/midi", "latex", "application/x-latex", "m3u", "audio/x-mpegurl", "mac", "image/x-macpaint", "man", "text/troff", "mathml", "application/mathml+xml", "me", "text/troff", "mid", "audio/midi", "midi", "audio/midi", "mif", "application/x-mif", "mov", "video/quicktime", "movie", "video/x-sgi-movie", "mp1", "audio/mpeg", "mp2", "audio/mpeg", "mp3", "audio/mpeg", "mp4", "video/mp4", "mpa", "audio/mpeg", "mpe", "video/mpeg", "mpeg", "video/mpeg", "mpega", "audio/x-mpeg", "mpg", "video/mpeg", "mpv2", "video/mpeg2", "nc", "application/x-netcdf", "oda", "application/oda", "odb", "application/vnd.oasis.opendocument.database", "odc", "application/vnd.oasis.opendocument.chart", "odf", "application/vnd.oasis.opendocument.formula", "odg", "application/vnd.oasis.opendocument.graphics", "odi", "application/vnd.oasis.opendocument.image", "odm", "application/vnd.oasis.opendocument.text-master", "odp", "application/vnd.oasis.opendocument.presentation", "ods", "application/vnd.oasis.opendocument.spreadsheet", "odt", "application/vnd.oasis.opendocument.text", "otg", "application/vnd.oasis.opendocument.graphics-template", "oth", "application/vnd.oasis.opendocument.text-web", "otp", "application/vnd.oasis.opendocument.presentation-template", "ots", "application/vnd.oasis.opendocument.spreadsheet-template ", "ott", "application/vnd.oasis.opendocument.text-template", "ogx", "application/ogg", "ogv", "video/ogg", "oga", "audio/ogg", "ogg", "audio/ogg", "spx", "audio/ogg", "flac", "audio/flac", "anx", "application/annodex", "axa", "audio/annodex", "axv", "video/annodex", "xspf", "application/xspf+xml", "pbm", "image/x-portable-bitmap", "pct", "image/pict", "pdf", "application/pdf", "pgm", "image/x-portable-graymap", "pic", "image/pict", "pict", "image/pict", "pls", "audio/x-scpls", "png", "image/png", "pnm", "image/x-portable-anymap", "pnt", "image/x-macpaint", "ppm", "image/x-portable-pixmap", "ppt", "application/vnd.ms-powerpoint", "pps", "application/vnd.ms-powerpoint", "ps", "application/postscript", "psd", "image/vnd.adobe.photoshop", "qt", "video/quicktime", "qti", "image/x-quicktime", "qtif", "image/x-quicktime", "ras", "image/x-cmu-raster", "rdf", "application/rdf+xml", "rgb", "image/x-rgb", "rm", "application/vnd.rn-realmedia", "roff", "text/troff", "rtf", "application/rtf", "rtx", "text/richtext", "sh", "application/x-sh", "shar", "application/x-shar", "sit", "application/x-stuffit", "snd", "audio/basic", "src", "application/x-wais-source", "sv4cpio", "application/x-sv4cpio", "sv4crc", "application/x-sv4crc", "svg", "image/svg+xml", "svgz", "image/svg+xml", "swf", "application/x-shockwave-flash", "t", "text/troff", "tar", "application/x-tar", "tcl", "application/x-tcl", "tex", "application/x-tex", "texi", "application/x-texinfo", "texinfo", "application/x-texinfo", "tif", "image/tiff", "tiff", "image/tiff", "tr", "text/troff", "tsv", "text/tab-separated-values", "txt", "text/plain", "ulw", "audio/basic", "ustar", "application/x-ustar", "vxml", "application/voicexml+xml", "xbm", "image/x-xbitmap", "xht", "application/xhtml+xml", "xhtml", "application/xhtml+xml", "xls", "application/vnd.ms-excel", "xml", "application/xml", "xpm", "image/x-xpixmap", "xsl", "application/xml", "xslt", "application/xslt+xml", "xul", "application/vnd.mozilla.xul+xml", "xwd", "image/x-xwindowdump", "vsd", "application/vnd.visio", "wav", "audio/x-wav", "wbmp", "image/vnd.wap.wbmp", "wml", "text/vnd.wap.wml", "wmlc", "application/vnd.wap.wmlc", "wmls", "text/vnd.wap.wmlsc", "wmlscriptc", "application/vnd.wap.wmlscriptc", "wmv", "video/x-ms-wmv", "wrl", "model/vrml", "wspolicy", "application/wspolicy+xml", "Z", "application/x-compress", "z", "application/x-compress", "zip", "application/zip" };
  
  protected URL getWebappConfigFile(String path, String url)
  {
    File docBase = new File(path);
    if (docBase.isDirectory()) {
      return getWebappConfigFileFromDirectory(docBase, url);
    }
    return getWebappConfigFileFromJar(docBase, url);
  }
  
  private URL getWebappConfigFileFromDirectory(File docBase, String url)
  {
    URL result = null;
    File webAppContextXml = new File(docBase, "META-INF/context.xml");
    if (webAppContextXml.exists()) {
      try
      {
        result = webAppContextXml.toURI().toURL();
      }
      catch (MalformedURLException e)
      {
        Logger.getLogger(getLoggerName(getHost(), url)).log(Level.WARNING, "Unable to determine web application context.xml " + docBase, e);
      }
    }
    return result;
  }
  
  private URL getWebappConfigFileFromJar(File docBase, String url)
  {

    JarFile jar = null;
    try
    {
      jar = new JarFile(docBase);
      JarEntry entry = jar.getJarEntry("META-INF/context.xml");
      if (entry != null) {}
      return new URL("jar:" + docBase.toURI().toString() + "!/" + "META-INF/context.xml");
    }
    catch (IOException e)
    {
      Logger.getLogger(getLoggerName(getHost(), url)).log(Level.WARNING, "Unable to determine web application context.xml " + docBase, e);
    }
    finally
    {
      if (jar != null) {
        try
        {
          jar.close();
        }
        catch (IOException e) {}
      }
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\Tomcat.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */