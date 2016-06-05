package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Binding;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.deploy.ServletDef;
import org.apache.catalina.deploy.WebXml;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.util.Introspection;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.resources.DirContextURLConnection;
import org.apache.naming.resources.FileDirContext;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.scan.Jar;
import org.apache.tomcat.util.scan.JarFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class ContextConfig
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(ContextConfig.class);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  protected static final LoginConfig DUMMY_LOGIN_CONFIG = new LoginConfig("NONE", null, null, null);
  protected static final Properties authenticators;
  private static final Set<String> pluggabilityJarsToSkip = new HashSet();
  
  static
  {
	  // Load our mapping properties for the standard authenticators
      Properties props = new Properties();
      InputStream is = null;
      try {
          is = ContextConfig.class.getClassLoader().getResourceAsStream(
                  "org/apache/catalina/startup/Authenticators.properties");
          if (is != null) {
              props.load(is);
          }
      } catch (IOException ioe) {
          props = null;
      } finally {
          if (is != null) {
              try {
                  is.close();
              } catch (IOException e) {
              }
          }
      }
      authenticators = props;
      // Load the list of JARS to skip
      addJarsToSkip(Constants.DEFAULT_JARS_TO_SKIP);
      addJarsToSkip(Constants.PLUGGABILITY_JARS_TO_SKIP);
  }
  
  private static void addJarsToSkip(String systemPropertyName)
  {
    String jarList = System.getProperty(systemPropertyName);
    if (jarList != null)
    {
      StringTokenizer tokenizer = new StringTokenizer(jarList, ",");
      while (tokenizer.hasMoreElements())
      {
        String token = tokenizer.nextToken().trim();
        if (token.length() > 0) {
          pluggabilityJarsToSkip.add(token);
        }
      }
    }
  }
  
  protected static long deploymentCount = 0L;
  protected static final Map<Host, DefaultWebXmlCacheEntry> hostWebXmlCache = new ConcurrentHashMap();
  private static final Set<ServletContainerInitializer> EMPTY_SCI_SET = Collections.emptySet();
  protected Map<String, Authenticator> customAuthenticators;
  protected Context context;
  @Deprecated
  protected String defaultContextXml;
  protected String defaultWebXml;
  protected boolean ok;
  protected String originalDocBase;
  private File antiLockingDocBase;
  protected final Map<ServletContainerInitializer, Set<Class<?>>> initializerClassMap;
  protected final Map<Class<?>, Set<ServletContainerInitializer>> typeInitializerMap;
  protected final Map<String, JavaClassCacheEntry> javaClassCache;
  protected boolean handlesTypesAnnotations;
  protected boolean handlesTypesNonAnnotations;
  protected Digester webDigester;
  protected WebRuleSet webRuleSet;
  protected Digester webFragmentDigester;
  protected WebRuleSet webFragmentRuleSet;
  
  public ContextConfig()
  {
    this.context = null;
    
    this.defaultContextXml = null;
    
    this.defaultWebXml = null;
    
    this.ok = false;
    
    this.originalDocBase = null;
    
    this.antiLockingDocBase = null;
    
    this.initializerClassMap = new LinkedHashMap();
    
    this.typeInitializerMap = new HashMap();
    
    this.javaClassCache = new HashMap();
    
    this.handlesTypesAnnotations = false;
    
    this.handlesTypesNonAnnotations = false;
    
    this.webDigester = null;
    this.webRuleSet = null;
    
    this.webFragmentDigester = null;
    this.webFragmentRuleSet = null;
  }
  
  public String getDefaultWebXml()
  {
    if (this.defaultWebXml == null) {
      this.defaultWebXml = "conf/web.xml";
    }
    return this.defaultWebXml;
  }
  
  public void setDefaultWebXml(String path)
  {
    this.defaultWebXml = path;
  }
  
  @Deprecated
  public String getDefaultContextXml()
  {
    if (this.defaultContextXml == null) {
      this.defaultContextXml = "conf/context.xml";
    }
    return this.defaultContextXml;
  }
  
  @Deprecated
  public void setDefaultContextXml(String path)
  {
    this.defaultContextXml = path;
  }
  
  public void setCustomAuthenticators(Map<String, Authenticator> customAuthenticators)
  {
    this.customAuthenticators = customAuthenticators;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      this.context = ((Context)event.getLifecycle());
    }
    catch (ClassCastException e)
    {
      log.error(sm.getString("contextConfig.cce", new Object[] { event.getLifecycle() }), e);
      return;
    }
    if (event.getType().equals("configure_start")) {
      configureStart();
    } else if (event.getType().equals("before_start")) {
      beforeStart();
    } else if (event.getType().equals("after_start"))
    {
      if (this.originalDocBase != null) {
        this.context.setDocBase(this.originalDocBase);
      }
    }
    else if (event.getType().equals("configure_stop")) {
      configureStop();
    } else if (event.getType().equals("after_init")) {
      init();
    } else if (event.getType().equals("after_destroy")) {
      destroy();
    }
  }
  
  protected void applicationAnnotationsConfig()
  {
    long t1 = System.currentTimeMillis();
    
    WebAnnotationSet.loadApplicationAnnotations(this.context);
    
    long t2 = System.currentTimeMillis();
    if ((this.context instanceof StandardContext)) {
      ((StandardContext)this.context).setStartupTime(t2 - t1 + ((StandardContext)this.context).getStartupTime());
    }
  }
  
  protected void authenticatorConfig()
  {
    LoginConfig loginConfig = this.context.getLoginConfig();
    
    SecurityConstraint[] constraints = this.context.findConstraints();
    if ((this.context.getIgnoreAnnotations()) && ((constraints == null) || (constraints.length == 0)) && (!this.context.getPreemptiveAuthentication())) {
      return;
    }
    if (loginConfig == null)
    {
      loginConfig = DUMMY_LOGIN_CONFIG;
      this.context.setLoginConfig(loginConfig);
    }
    if (this.context.getAuthenticator() != null) {
      return;
    }
    if (!(this.context instanceof ContainerBase)) {
      return;
    }
    if (this.context.getRealm() == null)
    {
      log.error(sm.getString("contextConfig.missingRealm"));
      this.ok = false;
      return;
    }
    Valve authenticator = null;
    if (this.customAuthenticators != null) {
      authenticator = (Valve)this.customAuthenticators.get(loginConfig.getAuthMethod());
    }
    if (authenticator == null)
    {
      if (authenticators == null)
      {
        log.error(sm.getString("contextConfig.authenticatorResources"));
        this.ok = false;
        return;
      }
      String authenticatorName = null;
      authenticatorName = authenticators.getProperty(loginConfig.getAuthMethod());
      if (authenticatorName == null)
      {
        log.error(sm.getString("contextConfig.authenticatorMissing", new Object[] { loginConfig.getAuthMethod() }));
        
        this.ok = false;
        return;
      }
      try
      {
        Class<?> authenticatorClass = Class.forName(authenticatorName);
        authenticator = (Valve)authenticatorClass.newInstance();
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        log.error(sm.getString("contextConfig.authenticatorInstantiate", new Object[] { authenticatorName }), t);
        
        this.ok = false;
      }
    }
    if ((authenticator != null) && ((this.context instanceof ContainerBase)))
    {
      Pipeline pipeline = ((ContainerBase)this.context).getPipeline();
      if (pipeline != null)
      {
        ((ContainerBase)this.context).getPipeline().addValve(authenticator);
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("contextConfig.authenticatorConfigured", new Object[] { loginConfig.getAuthMethod() }));
        }
      }
    }
  }
  
  public void createWebXmlDigester(boolean namespaceAware, boolean validation)
  {
    boolean blockExternal = this.context.getXmlBlockExternal();
    
    this.webRuleSet = new WebRuleSet(false);
    this.webDigester = DigesterFactory.newDigester(validation, namespaceAware, this.webRuleSet, blockExternal);
    
    this.webDigester.getParser();
    
    this.webFragmentRuleSet = new WebRuleSet(true);
    this.webFragmentDigester = DigesterFactory.newDigester(validation, namespaceAware, this.webFragmentRuleSet, blockExternal);
    
    this.webFragmentDigester.getParser();
  }
  
  protected Digester createContextDigester()
  {
    Digester digester = new Digester();
    digester.setValidating(false);
    digester.setRulesValidation(true);
    HashMap<Class<?>, List<String>> fakeAttributes = new HashMap();
    
    ArrayList<String> attrs = new ArrayList();
    attrs.add("className");
    fakeAttributes.put(Object.class, attrs);
    digester.setFakeAttributes(fakeAttributes);
    RuleSet contextRuleSet = new ContextRuleSet("", false);
    digester.addRuleSet(contextRuleSet);
    RuleSet namingRuleSet = new NamingRuleSet("Context/");
    digester.addRuleSet(namingRuleSet);
    return digester;
  }
  
  protected String getBaseDir()
  {
    Container engineC = this.context.getParent().getParent();
    if ((engineC instanceof StandardEngine)) {
      return ((StandardEngine)engineC).getBaseDir();
    }
    return System.getProperty("catalina.base");
  }
  
  protected void contextConfig(Digester digester)
  {
    if ((this.defaultContextXml == null) && ((this.context instanceof StandardContext))) {
      this.defaultContextXml = ((StandardContext)this.context).getDefaultContextXml();
    }
    if (this.defaultContextXml == null) {
      getDefaultContextXml();
    }
    if (!this.context.getOverride())
    {
      File defaultContextFile = new File(this.defaultContextXml);
      if (!defaultContextFile.isAbsolute()) {
        defaultContextFile = new File(getBaseDir(), this.defaultContextXml);
      }
      if (defaultContextFile.exists()) {
        try
        {
          URL defaultContextUrl = defaultContextFile.toURI().toURL();
          processContextConfig(digester, defaultContextUrl);
        }
        catch (MalformedURLException e)
        {
          log.error(sm.getString("contextConfig.badUrl", new Object[] { defaultContextFile }), e);
        }
      }
      File hostContextFile = new File(getHostConfigBase(), "context.xml.default");
      if (hostContextFile.exists()) {
        try
        {
          URL hostContextUrl = hostContextFile.toURI().toURL();
          processContextConfig(digester, hostContextUrl);
        }
        catch (MalformedURLException e)
        {
          log.error(sm.getString("contextConfig.badUrl", new Object[] { hostContextFile }), e);
        }
      }
    }
    if (this.context.getConfigFile() != null) {
      processContextConfig(digester, this.context.getConfigFile());
    }
  }
  
  protected void processContextConfig(Digester digester, URL contextXml)
  {
    if (log.isDebugEnabled()) {
      log.debug("Processing context [" + this.context.getName() + "] configuration file [" + contextXml + "]");
    }
    InputSource source = null;
    InputStream stream = null;
    try
    {
      source = new InputSource(contextXml.toString());
      URLConnection xmlConn = contextXml.openConnection();
      xmlConn.setUseCaches(false);
      stream = xmlConn.getInputStream();
    }
    catch (Exception e)
    {
      log.error(sm.getString("contextConfig.contextMissing", new Object[] { contextXml }), e);
    }
    if (source == null) {
      return;
    }
    try
    {
      source.setByteStream(stream);
      digester.setClassLoader(getClass().getClassLoader());
      digester.setUseContextClassLoader(false);
      digester.push(this.context.getParent());
      digester.push(this.context);
      XmlErrorHandler errorHandler = new XmlErrorHandler();
      digester.setErrorHandler(errorHandler);
      digester.parse(source);
      if ((errorHandler.getWarnings().size() > 0) || (errorHandler.getErrors().size() > 0))
      {
        errorHandler.logFindings(log, contextXml.toString());
        this.ok = false;
      }
      if (log.isDebugEnabled()) {
        log.debug("Successfully processed context [" + this.context.getName() + "] configuration file [" + contextXml + "]");
      }
      return;
    }
    catch (SAXParseException e)
    {
      log.error(sm.getString("contextConfig.contextParse", new Object[] { this.context.getName() }), e);
      
      log.error(sm.getString("contextConfig.defaultPosition", new Object[] { "" + e.getLineNumber(), "" + e.getColumnNumber() }));
      
      this.ok = false;
    }
    catch (Exception e)
    {
      log.error(sm.getString("contextConfig.contextParse", new Object[] { this.context.getName() }), e);
      
      this.ok = false;
    }
    finally
    {
      try
      {
        if (stream != null) {
          stream.close();
        }
      }
      catch (IOException e)
      {
        log.error(sm.getString("contextConfig.contextClose"), e);
      }
    }
  }
  
  protected void fixDocBase()
    throws IOException
  {
    Host host = (Host)this.context.getParent();
    String appBase = host.getAppBase();
    
    File canonicalAppBase = new File(appBase);
    if (canonicalAppBase.isAbsolute()) {
      canonicalAppBase = canonicalAppBase.getCanonicalFile();
    } else {
      canonicalAppBase = new File(getBaseDir(), appBase).getCanonicalFile();
    }
    String docBase = this.context.getDocBase();
    if (docBase == null)
    {
      String path = this.context.getPath();
      if (path == null) {
        return;
      }
      ContextName cn = new ContextName(path, this.context.getWebappVersion());
      docBase = cn.getBaseName();
    }
    File file = new File(docBase);
    if (!file.isAbsolute()) {
      docBase = new File(canonicalAppBase, docBase).getPath();
    } else {
      docBase = file.getCanonicalPath();
    }
    file = new File(docBase);
    String origDocBase = docBase;
    
    ContextName cn = new ContextName(this.context.getPath(), this.context.getWebappVersion());
    
    String pathName = cn.getBaseName();
    
    boolean unpackWARs = true;
    if ((host instanceof StandardHost))
    {
      unpackWARs = ((StandardHost)host).isUnpackWARs();
      if ((unpackWARs) && ((this.context instanceof StandardContext))) {
        unpackWARs = ((StandardContext)this.context).getUnpackWAR();
      }
    }
    if ((docBase.toLowerCase(Locale.ENGLISH).endsWith(".war")) && (!file.isDirectory()))
    {
      if (unpackWARs)
      {
        URL war = new URL("jar:" + new File(docBase).toURI().toURL() + "!/");
        docBase = ExpandWar.expand(host, war, pathName);
        file = new File(docBase);
        docBase = file.getCanonicalPath();
        if ((this.context instanceof StandardContext)) {
          ((StandardContext)this.context).setOriginalDocBase(origDocBase);
        }
      }
      else
      {
        URL war = new URL("jar:" + new File(docBase).toURI().toURL() + "!/");
        
        ExpandWar.validate(host, war, pathName);
      }
    }
    else
    {
      File docDir = new File(docBase);
      if (!docDir.exists())
      {
        File warFile = new File(docBase + ".war");
        if (warFile.exists())
        {
          URL war = new URL("jar:" + warFile.toURI().toURL() + "!/");
          if (unpackWARs)
          {
            docBase = ExpandWar.expand(host, war, pathName);
            file = new File(docBase);
            docBase = file.getCanonicalPath();
          }
          else
          {
            docBase = warFile.getCanonicalPath();
            ExpandWar.validate(host, war, pathName);
          }
        }
        if ((this.context instanceof StandardContext)) {
          ((StandardContext)this.context).setOriginalDocBase(origDocBase);
        }
      }
    }
    if (docBase.startsWith(canonicalAppBase.getPath() + File.separatorChar))
    {
      docBase = docBase.substring(canonicalAppBase.getPath().length());
      docBase = docBase.replace(File.separatorChar, '/');
      if (docBase.startsWith("/")) {
        docBase = docBase.substring(1);
      }
    }
    else
    {
      docBase = docBase.replace(File.separatorChar, '/');
    }
    this.context.setDocBase(docBase);
  }
  
  protected void antiLocking()
  {
    if (((this.context instanceof StandardContext)) && (((StandardContext)this.context).getAntiResourceLocking()))
    {
      Host host = (Host)this.context.getParent();
      String appBase = host.getAppBase();
      String docBase = this.context.getDocBase();
      if (docBase == null) {
        return;
      }
      this.originalDocBase = docBase;
      
      File docBaseFile = new File(docBase);
      if (!docBaseFile.isAbsolute())
      {
        File file = new File(appBase);
        if (!file.isAbsolute()) {
          file = new File(getBaseDir(), appBase);
        }
        docBaseFile = new File(file, docBase);
      }
      String path = this.context.getPath();
      if (path == null) {
        return;
      }
      ContextName cn = new ContextName(path, this.context.getWebappVersion());
      docBase = cn.getBaseName();
      if (this.originalDocBase.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
        this.antiLockingDocBase = new File(System.getProperty("java.io.tmpdir"), deploymentCount++ + "-" + docBase + ".war");
      } else {
        this.antiLockingDocBase = new File(System.getProperty("java.io.tmpdir"), deploymentCount++ + "-" + docBase);
      }
      this.antiLockingDocBase = this.antiLockingDocBase.getAbsoluteFile();
      if (log.isDebugEnabled()) {
        log.debug("Anti locking context[" + this.context.getName() + "] setting docBase to " + this.antiLockingDocBase.getPath());
      }
      ExpandWar.delete(this.antiLockingDocBase);
      if (ExpandWar.copy(docBaseFile, this.antiLockingDocBase)) {
        this.context.setDocBase(this.antiLockingDocBase.getPath());
      }
    }
  }
  
  protected void init()
  {
    Digester contextDigester = createContextDigester();
    contextDigester.getParser();
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.init"));
    }
    this.context.setConfigured(false);
    this.ok = true;
    
    contextConfig(contextDigester);
    
    createWebXmlDigester(this.context.getXmlNamespaceAware(), this.context.getXmlValidation());
  }
  
  protected synchronized void beforeStart()
  {
    try
    {
      fixDocBase();
    }
    catch (IOException e)
    {
      log.error(sm.getString("contextConfig.fixDocBase", new Object[] { this.context.getName() }), e);
    }
    antiLocking();
  }
  
  protected synchronized void configureStart()
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.start"));
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.xmlSettings", new Object[] { this.context.getName(), Boolean.valueOf(this.context.getXmlValidation()), Boolean.valueOf(this.context.getXmlNamespaceAware()) }));
    }
    webConfig();
    if (!this.context.getIgnoreAnnotations()) {
      applicationAnnotationsConfig();
    }
    if (this.ok) {
      validateSecurityRoles();
    }
    if (this.ok) {
      authenticatorConfig();
    }
    if ((log.isDebugEnabled()) && ((this.context instanceof ContainerBase)))
    {
      log.debug("Pipeline Configuration:");
      Pipeline pipeline = ((ContainerBase)this.context).getPipeline();
      Valve[] valves = null;
      if (pipeline != null) {
        valves = pipeline.getValves();
      }
      if (valves != null) {
        for (int i = 0; i < valves.length; i++) {
          log.debug("  " + valves[i].getInfo());
        }
      }
      log.debug("======================");
    }
    if (this.ok)
    {
      this.context.setConfigured(true);
    }
    else
    {
      log.error(sm.getString("contextConfig.unavailable"));
      this.context.setConfigured(false);
    }
  }
  
  protected synchronized void configureStop()
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.stop"));
    }
    Container[] children = this.context.findChildren();
    for (int i = 0; i < children.length; i++) {
      this.context.removeChild(children[i]);
    }
    SecurityConstraint[] securityConstraints = this.context.findConstraints();
    int i;
    for (i = 0; i < securityConstraints.length; i++) {
      this.context.removeConstraint(securityConstraints[i]);
    }
    ErrorPage[] errorPages = this.context.findErrorPages();
    for (i = 0; i < errorPages.length; i++) {
      this.context.removeErrorPage(errorPages[i]);
    }
    FilterDef[] filterDefs = this.context.findFilterDefs();
    for (i = 0; i < filterDefs.length; i++) {
      this.context.removeFilterDef(filterDefs[i]);
    }
    FilterMap[] filterMaps = this.context.findFilterMaps();
    for (i = 0; i < filterMaps.length; i++) {
      this.context.removeFilterMap(filterMaps[i]);
    }
    String[] mimeMappings = this.context.findMimeMappings();
    for (i = 0; i < mimeMappings.length; i++) {
      this.context.removeMimeMapping(mimeMappings[i]);
    }
    String[] parameters = this.context.findParameters();
    for (i = 0; i < parameters.length; i++) {
      this.context.removeParameter(parameters[i]);
    }
    String[] securityRoles = this.context.findSecurityRoles();
    for (i = 0; i < securityRoles.length; i++) {
      this.context.removeSecurityRole(securityRoles[i]);
    }
    String[] servletMappings = this.context.findServletMappings();
    for (i = 0; i < servletMappings.length; i++) {
      this.context.removeServletMapping(servletMappings[i]);
    }
    String[] welcomeFiles = this.context.findWelcomeFiles();
    for (i = 0; i < welcomeFiles.length; i++) {
      this.context.removeWelcomeFile(welcomeFiles[i]);
    }
    String[] wrapperLifecycles = this.context.findWrapperLifecycles();
    for (i = 0; i < wrapperLifecycles.length; i++) {
      this.context.removeWrapperLifecycle(wrapperLifecycles[i]);
    }
    String[] wrapperListeners = this.context.findWrapperListeners();
    for (i = 0; i < wrapperListeners.length; i++) {
      this.context.removeWrapperListener(wrapperListeners[i]);
    }
    if (this.antiLockingDocBase != null) {
      ExpandWar.delete(this.antiLockingDocBase, false);
    }
    this.initializerClassMap.clear();
    this.typeInitializerMap.clear();
    
    this.ok = true;
  }
  
  protected synchronized void destroy()
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.destroy"));
    }
    Server s = getServer();
    if ((s != null) && (!s.getState().isAvailable())) {
      return;
    }
    if ((this.context instanceof StandardContext))
    {
      String workDir = ((StandardContext)this.context).getWorkPath();
      if (workDir != null) {
        ExpandWar.delete(new File(workDir));
      }
    }
  }
  
  private Server getServer()
  {
    Container c = this.context;
    while ((c != null) && (!(c instanceof Engine))) {
      c = c.getParent();
    }
    if (c == null) {
      return null;
    }
    Service s = ((Engine)c).getService();
    if (s == null) {
      return null;
    }
    return s.getServer();
  }
  
  protected void validateSecurityRoles()
  {
    SecurityConstraint[] constraints = this.context.findConstraints();
    for (int i = 0; i < constraints.length; i++)
    {
      String[] roles = constraints[i].findAuthRoles();
      for (int j = 0; j < roles.length; j++) {
        if ((!"*".equals(roles[j])) && (!this.context.findSecurityRole(roles[j])))
        {
          log.warn(sm.getString("contextConfig.role.auth", new Object[] { roles[j] }));
          this.context.addSecurityRole(roles[j]);
        }
      }
    }
    Container[] wrappers = this.context.findChildren();
    for (int i = 0; i < wrappers.length; i++)
    {
      Wrapper wrapper = (Wrapper)wrappers[i];
      String runAs = wrapper.getRunAs();
      if ((runAs != null) && (!this.context.findSecurityRole(runAs)))
      {
        log.warn(sm.getString("contextConfig.role.runas", new Object[] { runAs }));
        this.context.addSecurityRole(runAs);
      }
      String[] names = wrapper.findSecurityReferences();
      for (int j = 0; j < names.length; j++)
      {
        String link = wrapper.findSecurityReference(names[j]);
        if ((link != null) && (!this.context.findSecurityRole(link)))
        {
          log.warn(sm.getString("contextConfig.role.link", new Object[] { link }));
          this.context.addSecurityRole(link);
        }
      }
    }
  }
  
  @Deprecated
  protected File getConfigBase()
  {
    File configBase = new File(getBaseDir(), "conf");
    if (!configBase.exists()) {
      return null;
    }
    return configBase;
  }
  
  protected File getHostConfigBase()
  {
    File file = null;
    Container container = this.context;
    Host host = null;
    Engine engine = null;
    while (container != null)
    {
      if ((container instanceof Host)) {
        host = (Host)container;
      }
      if ((container instanceof Engine)) {
        engine = (Engine)container;
      }
      container = container.getParent();
    }
    if ((host != null) && (host.getXmlBase() != null))
    {
      String xmlBase = host.getXmlBase();
      file = new File(xmlBase);
      if (!file.isAbsolute()) {
        file = new File(getBaseDir(), xmlBase);
      }
    }
    else
    {
      StringBuilder result = new StringBuilder();
      if (engine != null) {
        result.append(engine.getName()).append('/');
      }
      if (host != null) {
        result.append(host.getName()).append('/');
      }
      file = new File(getConfigBase(), result.toString());
    }
    try
    {
      return file.getCanonicalFile();
    }
    catch (IOException e) {}
    return file;
  }
  
  protected void webConfig()
  {
    Set<WebXml> defaults = new HashSet();
    defaults.add(getDefaultWebXmlFragment());
    
    WebXml webXml = createWebXml();
    
    InputSource contextWebXml = getContextWebXmlSource();
    parseWebXml(contextWebXml, webXml, false);
    
    ServletContext sContext = this.context.getServletContext();
    
    Map<String, WebXml> fragments = processJarsForWebFragments(webXml);
    
    Set<WebXml> orderedFragments = null;
    orderedFragments = WebXml.orderWebFragments(webXml, fragments, sContext);
    if (this.ok) {
      processServletContainerInitializers(this.context.getServletContext());
    }
    if ((!webXml.isMetadataComplete()) || (this.typeInitializerMap.size() > 0))
    {
      if (this.ok)
      {
        NamingEnumeration<Binding> listBindings = null;
        try
        {
          try
          {
            listBindings = this.context.getResources().listBindings("/WEB-INF/classes");
          }
          catch (NameNotFoundException ignore) {}
          while ((listBindings != null) && (listBindings.hasMoreElements()))
          {
            Binding binding = (Binding)listBindings.nextElement();
            if ((binding.getObject() instanceof FileDirContext))
            {
              File webInfClassDir = new File(((FileDirContext)binding.getObject()).getDocBase());
              
              processAnnotationsFile(webInfClassDir, webXml, webXml.isMetadataComplete());
            }
            else
            {
              String resource = "/WEB-INF/classes/" + binding.getName();
              try
              {
                URL url = sContext.getResource(resource);
                processAnnotationsUrl(url, webXml, webXml.isMetadataComplete());
              }
              catch (MalformedURLException e)
              {
                log.error(sm.getString("contextConfig.webinfClassesUrl", new Object[] { resource }), e);
              }
            }
          }
        }
        catch (NamingException e)
        {
          log.error(sm.getString("contextConfig.webinfClassesUrl", new Object[] { "/WEB-INF/classes" }), e);
        }
      }
      if (this.ok) {
        processAnnotations(orderedFragments, webXml.isMetadataComplete());
      }
      this.javaClassCache.clear();
    }
    if (!webXml.isMetadataComplete())
    {
      if (this.ok) {
        this.ok = webXml.merge(orderedFragments);
      }
      webXml.merge(defaults);
      if (this.ok) {
        convertJsps(webXml);
      }
      if (this.ok) {
        webXml.configureContext(this.context);
      }
    }
    else
    {
      webXml.merge(defaults);
      convertJsps(webXml);
      webXml.configureContext(this.context);
    }
    String mergedWebXml = webXml.toXml();
    sContext.setAttribute("org.apache.tomcat.util.scan.MergedWebXml", mergedWebXml);
    if (this.context.getLogEffectiveWebXml()) {
      log.info("web.xml:\n" + mergedWebXml);
    }
    if (this.ok)
    {
      Set<WebXml> resourceJars = new LinkedHashSet();
      if (orderedFragments != null) {
        for (WebXml fragment : orderedFragments) {
          resourceJars.add(fragment);
        }
      }
      for (WebXml fragment : fragments.values()) {
        if (!resourceJars.contains(fragment)) {
          resourceJars.add(fragment);
        }
      }
      processResourceJARs(resourceJars);
    }
    if (this.ok) {
      for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : this.initializerClassMap.entrySet()) {
        if (((Set)entry.getValue()).isEmpty()) {
          this.context.addServletContainerInitializer((ServletContainerInitializer)entry.getKey(), null);
        } else {
          this.context.addServletContainerInitializer((ServletContainerInitializer)entry.getKey(), (Set)entry.getValue());
        }
      }
    }
  }
  
  private WebXml getDefaultWebXmlFragment()
  {
	     // Host should never be null
      Host host = (Host) context.getParent();

      DefaultWebXmlCacheEntry entry = hostWebXmlCache.get(host);

      InputSource globalWebXml = getGlobalWebXmlSource();
      InputSource hostWebXml = getHostWebXmlSource();

      long globalTimeStamp = 0;
      long hostTimeStamp = 0;

      if (globalWebXml != null) {
          URLConnection uc = null;
          try {
              URL url = new URL(globalWebXml.getSystemId());
              uc = url.openConnection();
              globalTimeStamp = uc.getLastModified();
          } catch (IOException e) {
              globalTimeStamp = -1;
          } finally {
              if (uc != null) {
                  try {
                      uc.getInputStream().close();
                  } catch (IOException e) {
                      ExceptionUtils.handleThrowable(e);
                      globalTimeStamp = -1;
                  }
              }
          }
      }

      if (hostWebXml != null) {
          URLConnection uc = null;
          try {
              URL url = new URL(hostWebXml.getSystemId());
              uc = url.openConnection();
              hostTimeStamp = uc.getLastModified();
          } catch (IOException e) {
              hostTimeStamp = -1;
          } finally {
              if (uc != null) {
                  try {
                      uc.getInputStream().close();
                  } catch (IOException e) {
                      ExceptionUtils.handleThrowable(e);
                      hostTimeStamp = -1;
                  }
              }
          }
      }

      if (entry != null && entry.getGlobalTimeStamp() == globalTimeStamp &&
              entry.getHostTimeStamp() == hostTimeStamp) {
          return entry.getWebXml();
      }

      // Parsing global web.xml is relatively expensive. Use a sync block to
      // make sure it only happens once. Use the pipeline since a lock will
      // already be held on the host by another thread
      synchronized (host.getPipeline()) {
          entry = hostWebXmlCache.get(host);
          if (entry != null && entry.getGlobalTimeStamp() == globalTimeStamp &&
                  entry.getHostTimeStamp() == hostTimeStamp) {
              return entry.getWebXml();
          }

          WebXml webXmlDefaultFragment = createWebXml();
          webXmlDefaultFragment.setOverridable(true);
          // Set to distributable else every app will be prevented from being
          // distributable when the default fragment is merged with the main
          // web.xml
          webXmlDefaultFragment.setDistributable(true);
          // When merging, the default welcome files are only used if the app has
          // not defined any welcomes files.
          webXmlDefaultFragment.setAlwaysAddWelcomeFiles(false);

          // Parse global web.xml if present
          if (globalWebXml == null) {
              // This is unusual enough to log
              log.info(sm.getString("contextConfig.defaultMissing"));
          } else {
              parseWebXml(globalWebXml, webXmlDefaultFragment, false);
          }

          // Parse host level web.xml if present
          // Additive apart from welcome pages
          webXmlDefaultFragment.setReplaceWelcomeFiles(true);

          parseWebXml(hostWebXml, webXmlDefaultFragment, false);

          // Don't update the cache if an error occurs
          if (globalTimeStamp != -1 && hostTimeStamp != -1) {
              entry = new DefaultWebXmlCacheEntry(webXmlDefaultFragment,
                      globalTimeStamp, hostTimeStamp);
              hostWebXmlCache.put(host, entry);
          }

          return webXmlDefaultFragment;
      }
  }
  
  private void convertJsps(WebXml webXml)
  {
    ServletDef jspServlet = (ServletDef)webXml.getServlets().get("jsp");
    Map<String, String> jspInitParams;
    if (jspServlet == null)
    {
      jspInitParams = new HashMap();
      Wrapper w = (Wrapper)this.context.findChild("jsp");
      if (w != null)
      {
        String[] params = w.findInitParameters();
        for (String param : params) {
          jspInitParams.put(param, w.findInitParameter(param));
        }
      }
    }
    else
    {
      jspInitParams = jspServlet.getParameterMap();
    }
    for (ServletDef servletDef : webXml.getServlets().values()) {
      if (servletDef.getJspFile() != null) {
        convertJsp(servletDef, jspInitParams);
      }
    }
  }
  
  private void convertJsp(ServletDef servletDef, Map<String, String> jspInitParams)
  {
    servletDef.setServletClass("org.apache.jasper.servlet.JspServlet");
    String jspFile = servletDef.getJspFile();
    if ((jspFile != null) && (!jspFile.startsWith("/"))) {
      if (this.context.isServlet22())
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("contextConfig.jspFile.warning", new Object[] { jspFile }));
        }
        jspFile = "/" + jspFile;
      }
      else
      {
        throw new IllegalArgumentException(sm.getString("contextConfig.jspFile.error", new Object[] { jspFile }));
      }
    }
    servletDef.getParameterMap().put("jspFile", jspFile);
    servletDef.setJspFile(null);
    for (Map.Entry<String, String> initParam : jspInitParams.entrySet()) {
      servletDef.addInitParameter((String)initParam.getKey(), (String)initParam.getValue());
    }
  }
  
  protected WebXml createWebXml()
  {
    return new WebXml();
  }
  
  protected void processServletContainerInitializers(ServletContext servletContext)
  {

      List<ServletContainerInitializer> detectedScis;
      try {
          WebappServiceLoader<ServletContainerInitializer> loader =
                  new WebappServiceLoader<ServletContainerInitializer>(
                         (ServletContext) context,null);
          detectedScis = loader.load(ServletContainerInitializer.class);
      } catch (IOException e) {
          log.error(sm.getString(
                  "contextConfig.servletContainerInitializerFail",
                  context.getName()),
              e);
          ok = false;
          return;
      }

      for (ServletContainerInitializer sci : detectedScis) {
          initializerClassMap.put(sci, new HashSet<Class<?>>());

          HandlesTypes ht;
          try {
              ht = sci.getClass().getAnnotation(HandlesTypes.class);
          } catch (Exception e) {
              if (log.isDebugEnabled()) {
                  log.info(sm.getString("contextConfig.sci.debug",
                          sci.getClass().getName()),
                          e);
              } else {
                  log.info(sm.getString("contextConfig.sci.info",
                          sci.getClass().getName()));
              }
              continue;
          }
          if (ht == null) {
              continue;
          }
          Class<?>[] types = ht.value();
          if (types == null) {
              continue;
          }

          for (Class<?> type : types) {
              if (type.isAnnotation()) {
                  handlesTypesAnnotations = true;
              } else {
                  handlesTypesNonAnnotations = true;
              }
              Set<ServletContainerInitializer> scis =
                      typeInitializerMap.get(type);
              if (scis == null) {
                  scis = new HashSet<ServletContainerInitializer>();
                  typeInitializerMap.put(type, scis);
              }
              scis.add(sci);
          }
      }
  }
  
  protected void processResourceJARs(Set<WebXml> fragments)
  {
    for (WebXml fragment : fragments)
    {
      URL url = fragment.getURL();
      Jar jar = null;
      try
      {
        if ("jar".equals(url.getProtocol()))
        {
          jar = JarFactory.newInstance(url);
          jar.nextEntry();
          String entryName = jar.getEntryName();
          while (entryName != null)
          {
            if (entryName.startsWith("META-INF/resources/"))
            {
              this.context.addResourceJarUrl(url);
              break;
            }
            jar.nextEntry();
            entryName = jar.getEntryName();
          }
        }
        else if ("file".equals(url.getProtocol()))
        {
          FileDirContext fileDirContext = new FileDirContext();
          fileDirContext.setDocBase(new File(url.toURI()).getAbsolutePath());
          try
          {
            fileDirContext.lookup("META-INF/resources/");
            if ((this.context instanceof StandardContext)) {
              ((StandardContext)this.context).addResourcesDirContext(fileDirContext);
            }
          }
          catch (NamingException e) {}
        }
      }
      catch (IOException ioe)
      {
        log.error(sm.getString("contextConfig.resourceJarFail", new Object[] { url, this.context.getName() }));
      }
      catch (URISyntaxException e)
      {
        log.error(sm.getString("contextConfig.resourceJarFail", new Object[] { url, this.context.getName() }));
      }
      finally
      {
        if (jar != null) {
          jar.close();
        }
      }
    }
  }
  
  protected InputSource getGlobalWebXmlSource()
  {
    if ((this.defaultWebXml == null) && ((this.context instanceof StandardContext))) {
      this.defaultWebXml = ((StandardContext)this.context).getDefaultWebXml();
    }
    if (this.defaultWebXml == null) {
      getDefaultWebXml();
    }
    if ("org/apache/catalina/startup/NO_DEFAULT_XML".equals(this.defaultWebXml)) {
      return null;
    }
    return getWebXmlSource(this.defaultWebXml, getBaseDir());
  }
  
  protected InputSource getHostWebXmlSource()
  {
    File hostConfigBase = getHostConfigBase();
    if (!hostConfigBase.exists()) {
      return null;
    }
    return getWebXmlSource("web.xml.default", hostConfigBase.getPath());
  }
  
  protected InputSource getContextWebXmlSource()
  {
    InputStream stream = null;
    InputSource source = null;
    URL url = null;
    
    String altDDName = null;
    
    ServletContext servletContext = this.context.getServletContext();
    if (servletContext != null)
    {
      altDDName = (String)servletContext.getAttribute("org.apache.catalina.deploy.alt_dd");
      if (altDDName != null)
      {
        try
        {
          stream = new FileInputStream(altDDName);
          url = new File(altDDName).toURI().toURL();
        }
        catch (FileNotFoundException e)
        {
          log.error(sm.getString("contextConfig.altDDNotFound", new Object[] { altDDName }));
        }
        catch (MalformedURLException e)
        {
          log.error(sm.getString("contextConfig.applicationUrl"));
        }
      }
      else
      {
        stream = servletContext.getResourceAsStream("/WEB-INF/web.xml");
        try
        {
          url = servletContext.getResource("/WEB-INF/web.xml");
        }
        catch (MalformedURLException e)
        {
          log.error(sm.getString("contextConfig.applicationUrl"));
        }
      }
    }
    if ((stream == null) || (url == null))
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("contextConfig.applicationMissing") + " " + this.context);
      }
    }
    else
    {
      source = new InputSource(url.toExternalForm());
      source.setByteStream(stream);
    }
    return source;
  }
  
  protected InputSource getWebXmlSource(String filename, String path)
  {
    File file = new File(filename);
    if (!file.isAbsolute()) {
      file = new File(path, filename);
    }
    InputStream stream = null;
    InputSource source = null;
    try
    {
      if (!file.exists())
      {
        stream = getClass().getClassLoader().getResourceAsStream(filename);
        if (stream != null) {
          source = new InputSource(getClass().getClassLoader().getResource(filename).toURI().toString());
        }
      }
      else
      {
        source = new InputSource(file.getAbsoluteFile().toURI().toString());
        stream = new FileInputStream(file);
      }
      if ((stream != null) && (source != null)) {
        source.setByteStream(stream);
      }
    }
    catch (Exception e)
    {
      log.error(sm.getString("contextConfig.defaultError", new Object[] { filename, file }), e);
    }
    return source;
  }
  
  protected void parseWebXml(InputSource source, WebXml dest, boolean fragment)
  {
    if (source == null) {
      return;
    }
    XmlErrorHandler handler = new XmlErrorHandler();
    WebRuleSet ruleSet;
    Digester digester;

    if (fragment)
    {
      digester = this.webFragmentDigester;
      ruleSet = this.webFragmentRuleSet;
    }
    else
    {
      digester = this.webDigester;
      ruleSet = this.webRuleSet;
    }
    digester.push(dest);
    digester.setErrorHandler(handler);
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("contextConfig.applicationStart", new Object[] { source.getSystemId() }));
    }
    try
    {
      digester.parse(source);
      if ((handler.getWarnings().size() > 0) || (handler.getErrors().size() > 0))
      {
        this.ok = false;
        handler.logFindings(log, source.getSystemId());
      }
    }
    catch (SAXParseException e)
    {
      InputStream is;
      log.error(sm.getString("contextConfig.applicationParse", new Object[] { source.getSystemId() }), e);
      
      log.error(sm.getString("contextConfig.applicationPosition", new Object[] { "" + e.getLineNumber(), "" + e.getColumnNumber() }));
      
      this.ok = false;
    }
    catch (Exception e)
    {
      InputStream is;
      log.error(sm.getString("contextConfig.applicationParse", new Object[] { source.getSystemId() }), e);
      
      this.ok = false;
    }
    finally
    {
      InputStream is;
      digester.reset();
      ruleSet.recycle();
      
      is = source.getByteStream();
      if (is != null) {
        try
        {
          is.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
      }
    }
  }
  
  protected Map<String, WebXml> processJarsForWebFragments(WebXml application)
  {
    JarScanner jarScanner = this.context.getJarScanner();
    
    boolean parseRequired = true;
    Set<String> absoluteOrder = application.getAbsoluteOrdering();
    if ((absoluteOrder != null) && (absoluteOrder.isEmpty()) && (!this.context.getXmlValidation())) {
      parseRequired = false;
    }
    FragmentJarScannerCallback callback = new FragmentJarScannerCallback(parseRequired);
    
    jarScanner.scan(this.context.getServletContext(), this.context.getLoader().getClassLoader(), callback, pluggabilityJarsToSkip);
    
    return callback.getFragments();
  }
  
  protected void processAnnotations(Set<WebXml> fragments, boolean handlesTypesOnly)
  {
    for (WebXml fragment : fragments)
    {
      WebXml annotations = new WebXml();
      
      annotations.setDistributable(true);
      URL url = fragment.getURL();
      processAnnotationsUrl(url, annotations, (handlesTypesOnly) || (fragment.isMetadataComplete()));
      
      Set<WebXml> set = new HashSet();
      set.add(annotations);
      
      fragment.merge(set);
    }
  }
  
  protected void processAnnotationsUrl(URL url, WebXml fragment, boolean handlesTypesOnly)
  {
    if (url == null) {
      return;
    }
    if ("jar".equals(url.getProtocol())) {
      processAnnotationsJar(url, fragment, handlesTypesOnly);
    } else if ("jndi".equals(url.getProtocol())) {
      processAnnotationsJndi(url, fragment, handlesTypesOnly);
    } else if ("file".equals(url.getProtocol())) {
      try
      {
        processAnnotationsFile(new File(url.toURI()), fragment, handlesTypesOnly);
      }
      catch (URISyntaxException e)
      {
        log.error(sm.getString("contextConfig.fileUrl", new Object[] { url }), e);
      }
    } else {
      log.error(sm.getString("contextConfig.unknownUrlProtocol", new Object[] { url.getProtocol(), url }));
    }
  }
  
  protected void processAnnotationsJar(URL url, WebXml fragment, boolean handlesTypesOnly)
  {
    Jar jar = null;
    try
    {
      jar = JarFactory.newInstance(url);
      
      jar.nextEntry();
      String entryName = jar.getEntryName();
      while (entryName != null)
      {
        if (entryName.endsWith(".class"))
        {
          InputStream is = null;
          try
          {
            is = jar.getEntryInputStream();
            processAnnotationsStream(is, fragment, handlesTypesOnly);
            if (is != null) {
              try
              {
                is.close();
              }
              catch (IOException ioe) {}
            }
            jar.nextEntry();
          }
          catch (IOException e)
          {
            log.error(sm.getString("contextConfig.inputStreamJar", new Object[] { entryName, url }), e);
          }
          catch (ClassFormatException e)
          {
            log.error(sm.getString("contextConfig.inputStreamJar", new Object[] { entryName, url }), e);
          }
          finally
          {
            if (is != null) {
              try
              {
                is.close();
              }
              catch (IOException ioe) {}
            }
          }
        }
        entryName = jar.getEntryName();
      }
    }
    catch (IOException e)
    {
      log.error(sm.getString("contextConfig.jarFile", new Object[] { url }), e);
    }
    finally
    {
      if (jar != null) {
        jar.close();
      }
    }
  }
  
  protected void processAnnotationsJndi(URL url, WebXml fragment, boolean handlesTypesOnly)
  {
    try
    {
      URLConnection urlConn = url.openConnection();
      if (!(urlConn instanceof DirContextURLConnection))
      {
        sm.getString("contextConfig.jndiUrlNotDirContextConn", new Object[] { url });
        return;
      }
      DirContextURLConnection dcUrlConn = (DirContextURLConnection)urlConn;
      dcUrlConn.setUseCaches(false);
      
      String type = dcUrlConn.getHeaderField("resourcetype");
      if ("<collection/>".equals(type))
      {
        Enumeration<String> dirs = dcUrlConn.list();
        while (dirs.hasMoreElements())
        {
          String dir = (String)dirs.nextElement();
          URL dirUrl = new URL(url.toString() + '/' + dir);
          processAnnotationsJndi(dirUrl, fragment, handlesTypesOnly);
        }
      }
      else if (url.getPath().endsWith(".class"))
      {
        InputStream is = null;
        try
        {
          is = dcUrlConn.getInputStream();
          processAnnotationsStream(is, fragment, handlesTypesOnly);
        }
        catch (IOException e)
        {
          log.error(sm.getString("contextConfig.inputStreamJndi", new Object[] { url }), e);
        }
        catch (ClassFormatException e)
        {
          log.error(sm.getString("contextConfig.inputStreamJndi", new Object[] { url }), e);
        }
        finally
        {
          if (is != null) {
            try
            {
              is.close();
            }
            catch (Throwable t)
            {
              ExceptionUtils.handleThrowable(t);
            }
          }
        }
      }
      return;
    }
    catch (IOException e)
    {
      log.error(sm.getString("contextConfig.jndiUrl", new Object[] { url }), e);
    }
  }
  
  protected void processAnnotationsFile(File file, WebXml fragment, boolean handlesTypesOnly)
  {
    if (file.isDirectory())
    {
      String[] dirs = file.list();
      for (String dir : dirs) {
        processAnnotationsFile(new File(file, dir), fragment, handlesTypesOnly);
      }
    }
    else if ((file.canRead()) && (file.getName().endsWith(".class")))
    {
      FileInputStream fis = null;
      try
      {
        fis = new FileInputStream(file);
        processAnnotationsStream(fis, fragment, handlesTypesOnly);
      }
      catch (IOException e)
      {
        log.error(sm.getString("contextConfig.inputStreamFile", new Object[] { file.getAbsolutePath() }), e);
      }
      catch (ClassFormatException e)
      {
        log.error(sm.getString("contextConfig.inputStreamFile", new Object[] { file.getAbsolutePath() }), e);
      }
      finally
      {
        if (fis != null) {
          try
          {
            fis.close();
          }
          catch (Throwable t)
          {
            ExceptionUtils.handleThrowable(t);
          }
        }
      }
    }
  }
  
  protected void processAnnotationsStream(InputStream is, WebXml fragment, boolean handlesTypesOnly)
    throws ClassFormatException, IOException
  {
    ClassParser parser = new ClassParser(is);
    JavaClass clazz = parser.parse();
    checkHandlesTypes(clazz);
    if (handlesTypesOnly) {
      return;
    }
    String className = clazz.getClassName();
    
    AnnotationEntry[] annotationsEntries = clazz.getAnnotationEntries();
    if (annotationsEntries != null) {
      for (AnnotationEntry ae : annotationsEntries)
      {
        String type = ae.getAnnotationType();
        if ("Ljavax/servlet/annotation/WebServlet;".equals(type)) {
          processAnnotationWebServlet(className, ae, fragment);
        } else if ("Ljavax/servlet/annotation/WebFilter;".equals(type)) {
          processAnnotationWebFilter(className, ae, fragment);
        } else if ("Ljavax/servlet/annotation/WebListener;".equals(type)) {
          fragment.addListener(className);
        }
      }
    }
  }
  
  protected void checkHandlesTypes(JavaClass javaClass)
  {
    if (this.typeInitializerMap.size() == 0) {
      return;
    }
    if ((javaClass.getAccessFlags() & 0x2000) > 0) {
      return;
    }
    String className = javaClass.getClassName();
    
    Class<?> clazz = null;
    if (this.handlesTypesNonAnnotations)
    {
      populateJavaClassCache(className, javaClass);
      JavaClassCacheEntry entry = (JavaClassCacheEntry)this.javaClassCache.get(className);
      if (entry.getSciSet() == null) {
        try
        {
          populateSCIsForCacheEntry(entry);
        }
        catch (StackOverflowError soe)
        {
          throw new IllegalStateException(sm.getString("contextConfig.annotationsStackOverflow", new Object[] { this.context.getName(), classHierarchyToString(className, entry) }));
        }
      }
      if (!entry.getSciSet().isEmpty())
      {
        clazz = Introspection.loadClass(this.context, className);
        if (clazz == null) {
          return;
        }
        for (ServletContainerInitializer sci : entry.getSciSet())
        {
          Set<Class<?>> classes = (Set)this.initializerClassMap.get(sci);
          if (classes == null)
          {
            classes = new HashSet();
            this.initializerClassMap.put(sci, classes);
          }
          classes.add(clazz);
        }
      }
    }
   
    if (handlesTypesAnnotations) {
        for (Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry :
                typeInitializerMap.entrySet()) {
            if (entry.getKey().isAnnotation()) {
                AnnotationEntry[] annotationEntries = javaClass.getAnnotationEntries();
                if (annotationEntries != null) {
                    for (AnnotationEntry annotationEntry : annotationEntries) {
                        if (entry.getKey().getName().equals(
                                getClassName(annotationEntry.getAnnotationType()))) {
                            if (clazz == null) {
                                clazz = Introspection.loadClass(
                                        context, className);
                                if (clazz == null) {
                                    // Can't load the class so no point
                                    // continuing
                                    return;
                                }
                            }
                            for (ServletContainerInitializer sci : entry.getValue()) {
                                initializerClassMap.get(sci).add(clazz);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
  }
  
  private String classHierarchyToString(String className, JavaClassCacheEntry entry)
  {
    JavaClassCacheEntry start = entry;
    StringBuilder msg = new StringBuilder(className);
    msg.append("->");
    
    String parentName = entry.getSuperclassName();
    JavaClassCacheEntry parent = (JavaClassCacheEntry)this.javaClassCache.get(parentName);
    int count = 0;
    while ((count < 100) && (parent != null) && (parent != start))
    {
      msg.append(parentName);
      msg.append("->");
      
      count++;
      parentName = parent.getSuperclassName();
      parent = (JavaClassCacheEntry)this.javaClassCache.get(parentName);
    }
    msg.append(parentName);
    
    return msg.toString();
  }
  
  private void populateJavaClassCache(String className, JavaClass javaClass)
  {
    if (this.javaClassCache.containsKey(className)) {
      return;
    }
    this.javaClassCache.put(className, new JavaClassCacheEntry(javaClass));
    
    populateJavaClassCache(javaClass.getSuperclassName());
    for (String iterface : javaClass.getInterfaceNames()) {
      populateJavaClassCache(iterface);
    }
  }
  
  private void populateJavaClassCache(String className)
  {
    if (!this.javaClassCache.containsKey(className))
    {
      String name = className.replace('.', '/') + ".class";
      InputStream is = this.context.getLoader().getClassLoader().getResourceAsStream(name);
      if (is == null) {
        return;
      }
      ClassParser parser = new ClassParser(is);
      try
      {
        JavaClass clazz = parser.parse();
        populateJavaClassCache(clazz.getClassName(), clazz);
      }
      catch (ClassFormatException e)
      {
        log.debug(sm.getString("contextConfig.invalidSciHandlesTypes", new Object[] { className }), e);
      }
      catch (IOException e)
      {
        log.debug(sm.getString("contextConfig.invalidSciHandlesTypes", new Object[] { className }), e);
      }
      finally
      {
        try
        {
          is.close();
        }
        catch (IOException e) {}
      }
    }
  }
  
  private void populateSCIsForCacheEntry(JavaClassCacheEntry cacheEntry)
  {
    Set<ServletContainerInitializer> result = new HashSet();
    
    String superClassName = cacheEntry.getSuperclassName();
    JavaClassCacheEntry superClassCacheEntry = (JavaClassCacheEntry)this.javaClassCache.get(superClassName);
    if (cacheEntry.equals(superClassCacheEntry))
    {
      cacheEntry.setSciSet(EMPTY_SCI_SET);
      return;
    }
    if (superClassCacheEntry != null)
    {
      if (superClassCacheEntry.getSciSet() == null) {
        populateSCIsForCacheEntry(superClassCacheEntry);
      }
      result.addAll(superClassCacheEntry.getSciSet());
    }
    result.addAll(getSCIsForClass(superClassName));
    for (String interfaceName : cacheEntry.getInterfaceNames())
    {
      JavaClassCacheEntry interfaceEntry = (JavaClassCacheEntry)this.javaClassCache.get(interfaceName);
      if (interfaceEntry != null)
      {
        if (interfaceEntry.getSciSet() == null) {
          populateSCIsForCacheEntry(interfaceEntry);
        }
        result.addAll(interfaceEntry.getSciSet());
      }
      result.addAll(getSCIsForClass(interfaceName));
    }
    cacheEntry.setSciSet(result.isEmpty() ? EMPTY_SCI_SET : result);
  }
  
  private Set<ServletContainerInitializer> getSCIsForClass(String className)
  {
    for (Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry : this.typeInitializerMap.entrySet())
    {
      Class<?> clazz = (Class)entry.getKey();
      if ((!clazz.isAnnotation()) && 
        (clazz.getName().equals(className))) {
        return (Set)entry.getValue();
      }
    }
    return EMPTY_SCI_SET;
  }
  
  private static final String getClassName(String internalForm)
  {
    if (!internalForm.startsWith("L")) {
      return internalForm;
    }
    return internalForm.substring(1, internalForm.length() - 1).replace('/', '.');
  }
  
  protected void processAnnotationWebServlet(String className, AnnotationEntry ae, WebXml fragment)
  {
    String servletName = null;
    
    List<ElementValuePair> evps = ae.getElementValuePairs();
    for (ElementValuePair evp : evps)
    {
      String name = evp.getNameString();
      if ("name".equals(name))
      {
        servletName = evp.getValue().stringifyValue();
        break;
      }
    }
    if (servletName == null) {
      servletName = className;
    }
    ServletDef servletDef = (ServletDef)fragment.getServlets().get(servletName);
    boolean isWebXMLservletDef;

    if (servletDef == null)
    {
      servletDef = new ServletDef();
      servletDef.setServletName(servletName);
      servletDef.setServletClass(className);
      isWebXMLservletDef = false;
    }
    else
    {
      isWebXMLservletDef = true;
    }
    boolean urlPatternsSet = false;
    String[] urlPatterns = null;
    for (ElementValuePair evp : evps)
    {
      String name = evp.getNameString();
      if (("value".equals(name)) || ("urlPatterns".equals(name)))
      {
        if (urlPatternsSet) {
          throw new IllegalArgumentException(sm.getString("contextConfig.urlPatternValue", new Object[] { className }));
        }
        urlPatternsSet = true;
        urlPatterns = processAnnotationsStringArray(evp.getValue());
      }
      else if ("description".equals(name))
      {
        if (servletDef.getDescription() == null) {
          servletDef.setDescription(evp.getValue().stringifyValue());
        }
      }
      else if ("displayName".equals(name))
      {
        if (servletDef.getDisplayName() == null) {
          servletDef.setDisplayName(evp.getValue().stringifyValue());
        }
      }
      else if ("largeIcon".equals(name))
      {
        if (servletDef.getLargeIcon() == null) {
          servletDef.setLargeIcon(evp.getValue().stringifyValue());
        }
      }
      else if ("smallIcon".equals(name))
      {
        if (servletDef.getSmallIcon() == null) {
          servletDef.setSmallIcon(evp.getValue().stringifyValue());
        }
      }
      else if ("asyncSupported".equals(name))
      {
        if (servletDef.getAsyncSupported() == null) {
          servletDef.setAsyncSupported(evp.getValue().stringifyValue());
        }
      }
      else if ("loadOnStartup".equals(name))
      {
        if (servletDef.getLoadOnStartup() == null) {
          servletDef.setLoadOnStartup(evp.getValue().stringifyValue());
        }
      }
      else if ("initParams".equals(name))
      {
        Map<String, String> initParams = processAnnotationWebInitParams(evp.getValue());
        Map<String, String> webXMLInitParams;
        if (isWebXMLservletDef)
        {
          webXMLInitParams = servletDef.getParameterMap();
          for (Map.Entry<String, String> entry : initParams.entrySet()) {
            if (webXMLInitParams.get(entry.getKey()) == null) {
              servletDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
            }
          }
        }
        else
        {
          for (Map.Entry<String, String> entry : initParams.entrySet()) {
            servletDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
          }
        }
      }
    }
    if ((!isWebXMLservletDef) && (urlPatterns != null)) {
      fragment.addServlet(servletDef);
    }
    if ((urlPatterns != null) && 
      (!fragment.getServletMappings().containsValue(servletName))) {
      for (String urlPattern : urlPatterns) {
        fragment.addServletMapping(urlPattern, servletName);
      }
    }
  }
  
  protected void processAnnotationWebFilter(String className, AnnotationEntry ae, WebXml fragment)
  {
    String filterName = null;
    
    List<ElementValuePair> evps = ae.getElementValuePairs();
    for (ElementValuePair evp : evps)
    {
      String name = evp.getNameString();
      if ("filterName".equals(name))
      {
        filterName = evp.getValue().stringifyValue();
        break;
      }
    }
    if (filterName == null) {
      filterName = className;
    }
    FilterDef filterDef = (FilterDef)fragment.getFilters().get(filterName);
    FilterMap filterMap = new FilterMap();
    boolean isWebXMLfilterDef;

    if (filterDef == null)
    {
      filterDef = new FilterDef();
      filterDef.setFilterName(filterName);
      filterDef.setFilterClass(className);
      isWebXMLfilterDef = false;
    }
    else
    {
      isWebXMLfilterDef = true;
    }
    boolean urlPatternsSet = false;
    boolean servletNamesSet = false;
    boolean dispatchTypesSet = false;
    String[] urlPatterns = null;
    for (ElementValuePair evp : evps)
    {
      String name = evp.getNameString();
      if (("value".equals(name)) || ("urlPatterns".equals(name)))
      {
        if (urlPatternsSet) {
          throw new IllegalArgumentException(sm.getString("contextConfig.urlPatternValue", new Object[] { className }));
        }
        urlPatterns = processAnnotationsStringArray(evp.getValue());
        urlPatternsSet = urlPatterns.length > 0;
        for (String urlPattern : urlPatterns) {
          filterMap.addURLPattern(urlPattern);
        }
      }
      else if ("servletNames".equals(name))
      {
        String[] servletNames = processAnnotationsStringArray(evp.getValue());
        
        servletNamesSet = servletNames.length > 0;
        for (String servletName : servletNames) {
          filterMap.addServletName(servletName);
        }
      }
      else if ("dispatcherTypes".equals(name))
      {
        String[] dispatcherTypes = processAnnotationsStringArray(evp.getValue());
        
        dispatchTypesSet = dispatcherTypes.length > 0;
        for (String dispatcherType : dispatcherTypes) {
          filterMap.setDispatcher(dispatcherType);
        }
      }
      else if ("description".equals(name))
      {
        if (filterDef.getDescription() == null) {
          filterDef.setDescription(evp.getValue().stringifyValue());
        }
      }
      else if ("displayName".equals(name))
      {
        if (filterDef.getDisplayName() == null) {
          filterDef.setDisplayName(evp.getValue().stringifyValue());
        }
      }
      else if ("largeIcon".equals(name))
      {
        if (filterDef.getLargeIcon() == null) {
          filterDef.setLargeIcon(evp.getValue().stringifyValue());
        }
      }
      else if ("smallIcon".equals(name))
      {
        if (filterDef.getSmallIcon() == null) {
          filterDef.setSmallIcon(evp.getValue().stringifyValue());
        }
      }
      else if ("asyncSupported".equals(name))
      {
        if (filterDef.getAsyncSupported() == null) {
          filterDef.setAsyncSupported(evp.getValue().stringifyValue());
        }
      }
      else if ("initParams".equals(name))
      {
        Map<String, String> initParams = processAnnotationWebInitParams(evp.getValue());
        Map<String, String> webXMLInitParams;
        if (isWebXMLfilterDef)
        {
          webXMLInitParams = filterDef.getParameterMap();
          for (Map.Entry<String, String> entry : initParams.entrySet()) {
            if (webXMLInitParams.get(entry.getKey()) == null) {
              filterDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
            }
          }
        }
        else
        {
          for (Map.Entry<String, String> entry : initParams.entrySet()) {
            filterDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
          }
        }
      }
    }
    if (!isWebXMLfilterDef)
    {
      fragment.addFilter(filterDef);
      if ((urlPatternsSet) || (servletNamesSet))
      {
        filterMap.setFilterName(filterName);
        fragment.addFilterMapping(filterMap);
      }
    }
    if ((urlPatternsSet) || (dispatchTypesSet))
    {
      Set<FilterMap> fmap = fragment.getFilterMappings();
      FilterMap descMap = null;
      for (FilterMap map : fmap) {
        if (filterName.equals(map.getFilterName()))
        {
          descMap = map;
          break;
        }
      }
      if (descMap != null)
      {
        String[] urlsPatterns = descMap.getURLPatterns();
        if ((urlPatternsSet) && ((urlsPatterns == null) || (urlsPatterns.length == 0))) {
          for (String urlPattern : filterMap.getURLPatterns()) {
            descMap.addURLPattern(urlPattern);
          }
        }
        String[] dispatcherNames = descMap.getDispatcherNames();
        if ((dispatchTypesSet) && ((dispatcherNames == null) || (dispatcherNames.length == 0))) {
          for (String dis : filterMap.getDispatcherNames()) {
            descMap.setDispatcher(dis);
          }
        }
      }
    }
  }
  
  protected String[] processAnnotationsStringArray(ElementValue ev)
  {
    ArrayList<String> values = new ArrayList();
    if ((ev instanceof ArrayElementValue))
    {
      ElementValue[] arrayValues = ((ArrayElementValue)ev).getElementValuesArray();
      for (ElementValue value : arrayValues) {
        values.add(value.stringifyValue());
      }
    }
    else
    {
      values.add(ev.stringifyValue());
    }
    String[] result = new String[values.size()];
    return (String[])values.toArray(result);
  }
  
  protected Map<String, String> processAnnotationWebInitParams(ElementValue ev)
  {
    Map<String, String> result = new HashMap();
    if ((ev instanceof ArrayElementValue))
    {
      ElementValue[] arrayValues = ((ArrayElementValue)ev).getElementValuesArray();
      for (ElementValue value : arrayValues) {
        if ((value instanceof AnnotationElementValue))
        {
          List<ElementValuePair> evps = ((AnnotationElementValue)value).getAnnotationEntry().getElementValuePairs();
          
          String initParamName = null;
          String initParamValue = null;
          for (ElementValuePair evp : evps) {
            if ("name".equals(evp.getNameString())) {
              initParamName = evp.getValue().stringifyValue();
            } else if ("value".equals(evp.getNameString())) {
              initParamValue = evp.getValue().stringifyValue();
            }
          }
          result.put(initParamName, initParamValue);
        }
      }
    }
    return result;
  }
  
  private class FragmentJarScannerCallback
    implements JarScannerCallback
  {
    private static final String FRAGMENT_LOCATION = "META-INF/web-fragment.xml";
    private Map<String, WebXml> fragments = new HashMap();
    private final boolean parseRequired;
    
    public FragmentJarScannerCallback(boolean parseRequired)
    {
      this.parseRequired = parseRequired;
    }
    
    public void scan(JarURLConnection jarConn)
      throws IOException
    {
      URL url = jarConn.getURL();
      URL resourceURL = jarConn.getJarFileURL();
      Jar jar = null;
      InputStream is = null;
      WebXml fragment = new WebXml();
      try
      {
        jar = JarFactory.newInstance(url);
        if ((this.parseRequired) || (ContextConfig.this.context.getXmlValidation())) {
          is = jar.getInputStream("META-INF/web-fragment.xml");
        }
        if (is == null)
        {
          fragment.setDistributable(true);
        }
        else
        {
          InputSource source = new InputSource("jar:" + resourceURL.toString() + "!/" + "META-INF/web-fragment.xml");
          
          source.setByteStream(is);
          ContextConfig.this.parseWebXml(source, fragment, true);
        }
      }
      finally
      {
        if (jar != null) {
          jar.close();
        }
        fragment.setURL(url);
        if (fragment.getName() == null) {
          fragment.setName(fragment.getURL().toString());
        }
        fragment.setJarName(extractJarFileName(url));
        this.fragments.put(fragment.getName(), fragment);
      }
    }
    
    private String extractJarFileName(URL input)
    {
      String url = input.toString();
      if (url.endsWith("!/")) {
        url = url.substring(0, url.length() - 2);
      }
      return url.substring(url.lastIndexOf('/') + 1);
    }
    
    public void scan(File file)
      throws IOException
    {
      InputStream stream = null;
      WebXml fragment = new WebXml();
      try
      {
        File fragmentFile = new File(file, "META-INF/web-fragment.xml");
        if (fragmentFile.isFile())
        {
          stream = new FileInputStream(fragmentFile);
          InputSource source = new InputSource(fragmentFile.toURI().toURL().toString());
          
          source.setByteStream(stream);
          ContextConfig.this.parseWebXml(source, fragment, true);
        }
        else
        {
          fragment.setDistributable(true);
        }
      }
      finally
      {
        if (stream != null) {
          try
          {
            stream.close();
          }
          catch (IOException e) {}
        }
        fragment.setURL(file.toURI().toURL());
        if (fragment.getName() == null) {
          fragment.setName(fragment.getURL().toString());
        }
        fragment.setJarName(file.getName());
        this.fragments.put(fragment.getName(), fragment);
      }
    }
    
    public Map<String, WebXml> getFragments()
    {
      return this.fragments;
    }
  }
  
  private static class DefaultWebXmlCacheEntry
  {
    private final WebXml webXml;
    private final long globalTimeStamp;
    private final long hostTimeStamp;
    
    public DefaultWebXmlCacheEntry(WebXml webXml, long globalTimeStamp, long hostTimeStamp)
    {
      this.webXml = webXml;
      this.globalTimeStamp = globalTimeStamp;
      this.hostTimeStamp = hostTimeStamp;
    }
    
    public WebXml getWebXml()
    {
      return this.webXml;
    }
    
    public long getGlobalTimeStamp()
    {
      return this.globalTimeStamp;
    }
    
    public long getHostTimeStamp()
    {
      return this.hostTimeStamp;
    }
  }
  
  private static class JavaClassCacheEntry
  {
    public final String superclassName;
    public final String[] interfaceNames;
    private Set<ServletContainerInitializer> sciSet = null;
    
    public JavaClassCacheEntry(JavaClass javaClass)
    {
      this.superclassName = javaClass.getSuperclassName();
      this.interfaceNames = javaClass.getInterfaceNames();
    }
    
    public String getSuperclassName()
    {
      return this.superclassName;
    }
    
    public String[] getInterfaceNames()
    {
      return this.interfaceNames;
    }
    
    public Set<ServletContainerInitializer> getSciSet()
    {
      return this.sciSet;
    }
    
    public void setSciSet(Set<ServletContainerInitializer> sciSet)
    {
      this.sciSet = sciSet;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\ContextConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */