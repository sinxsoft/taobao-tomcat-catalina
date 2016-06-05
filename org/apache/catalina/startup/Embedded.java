package org.apache.catalina.startup;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.security.SecurityConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.res.StringManager;

@Deprecated
public class Embedded
  extends StandardService
{
  private static final Log log = LogFactory.getLog(Embedded.class);
  
  public Embedded()
  {
    this(null);
  }
  
  public Embedded(Realm realm)
  {
    setRealm(realm);
    setSecurityProtection();
  }
  
  protected boolean useNaming = true;
  protected boolean redirectStreams = true;
  protected Engine[] engines = new Engine[0];
  protected volatile HashMap<String, Authenticator> authenticators;
  protected static final String info = "org.apache.catalina.startup.Embedded/1.0";
  protected Realm realm = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  protected boolean await = false;
  
  public boolean isUseNaming()
  {
    return this.useNaming;
  }
  
  public void setUseNaming(boolean useNaming)
  {
    boolean oldUseNaming = this.useNaming;
    this.useNaming = useNaming;
    this.support.firePropertyChange("useNaming", Boolean.valueOf(oldUseNaming), Boolean.valueOf(this.useNaming));
  }
  
  public boolean isRedirectStreams()
  {
    return this.redirectStreams;
  }
  
  public void setRedirectStreams(boolean redirectStreams)
  {
    boolean oldRedirectStreams = this.redirectStreams;
    this.redirectStreams = redirectStreams;
    this.support.firePropertyChange("redirectStreams", Boolean.valueOf(oldRedirectStreams), Boolean.valueOf(this.redirectStreams));
  }
  
  public Realm getRealm()
  {
    return this.realm;
  }
  
  public void setRealm(Realm realm)
  {
    Realm oldRealm = this.realm;
    this.realm = realm;
    this.support.firePropertyChange("realm", oldRealm, this.realm);
  }
  
  public void setAwait(boolean b)
  {
    this.await = b;
  }
  
  public boolean isAwait()
  {
    return this.await;
  }
  
  public void setCatalinaHome(String s)
  {
    System.setProperty("catalina.home", s);
  }
  
  public void setCatalinaBase(String s)
  {
    System.setProperty("catalina.base", s);
  }
  
  public String getCatalinaHome()
  {
    return System.getProperty("catalina.home");
  }
  
  public String getCatalinaBase()
  {
    return System.getProperty("catalina.base");
  }
  
  public synchronized void addConnector(Connector connector)
  {
    if (log.isDebugEnabled()) {
      log.debug("Adding connector (" + connector.getInfo() + ")");
    }
    if (this.engines.length < 1) {
      throw new IllegalStateException(sm.getString("embedded.noEngines"));
    }
    super.addConnector(connector);
  }
  
  public synchronized void addEngine(Engine engine)
  {
    if (log.isDebugEnabled()) {
      log.debug("Adding engine (" + engine.getInfo() + ")");
    }
    Engine[] results = new Engine[this.engines.length + 1];
    for (int i = 0; i < this.engines.length; i++) {
      results[i] = this.engines[i];
    }
    results[this.engines.length] = engine;
    this.engines = results;
    if (getState().isAvailable()) {
      try
      {
        engine.start();
      }
      catch (LifecycleException e)
      {
        log.error("Engine.start", e);
      }
    }
    this.container = engine;
  }
  
  public Connector createConnector(InetAddress address, int port, boolean secure)
  {
    return createConnector(address != null ? address.toString() : null, port, secure);
  }
  
  public Connector createConnector(String address, int port, boolean secure)
  {
    String protocol = "http";
    if (secure) {
      protocol = "https";
    }
    return createConnector(address, port, protocol);
  }
  
  public Connector createConnector(InetAddress address, int port, String protocol)
  {
    return createConnector(address != null ? address.toString() : null, port, protocol);
  }
  
  public Connector createConnector(String address, int port, String protocol)
  {
    Connector connector = null;
    if (address != null)
    {
      int index = address.indexOf('/');
      if (index != -1) {
        address = address.substring(index + 1);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("Creating connector for address='" + (address == null ? "ALL" : address) + "' port='" + port + "' protocol='" + protocol + "'");
    }
    try
    {
      if (protocol.equals("ajp"))
      {
        connector = new Connector("org.apache.coyote.ajp.AjpProtocol");
      }
      else if (protocol.equals("memory"))
      {
        connector = new Connector("org.apache.coyote.memory.MemoryProtocolHandler");
      }
      else if (protocol.equals("http"))
      {
        connector = new Connector();
      }
      else if (protocol.equals("https"))
      {
        connector = new Connector();
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setProperty("SSLEnabled", "true");
      }
      else
      {
        connector = new Connector(protocol);
      }
      if (address != null) {
        IntrospectionUtils.setProperty(connector, "address", "" + address);
      }
      IntrospectionUtils.setProperty(connector, "port", "" + port);
    }
    catch (Exception e)
    {
      log.error("Couldn't create connector.");
    }
    return connector;
  }
  
  public Context createContext(String path, String docBase)
  {
    if (log.isDebugEnabled()) {
      log.debug("Creating context '" + path + "' with docBase '" + docBase + "'");
    }
    StandardContext context = new StandardContext();
    
    context.setDocBase(docBase);
    context.setPath(path);
    
    ContextConfig config = new ContextConfig();
    config.setCustomAuthenticators(this.authenticators);
    context.addLifecycleListener(config);
    
    return context;
  }
  
  public Engine createEngine()
  {
    if (log.isDebugEnabled()) {
      log.debug("Creating engine");
    }
    StandardEngine engine = new StandardEngine();
    
    engine.setRealm(this.realm);
    
    return engine;
  }
  
  public Host createHost(String name, String appBase)
  {
    if (log.isDebugEnabled()) {
      log.debug("Creating host '" + name + "' with appBase '" + appBase + "'");
    }
    StandardHost host = new StandardHost();
    
    host.setAppBase(appBase);
    host.setName(name);
    
    return host;
  }
  
  public Loader createLoader(ClassLoader parent)
  {
    if (log.isDebugEnabled()) {
      log.debug("Creating Loader with parent class loader '" + parent + "'");
    }
    WebappLoader loader = new WebappLoader(parent);
    return loader;
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.startup.Embedded/1.0";
  }
  
  public synchronized void removeContext(Context context)
  {
    if (log.isDebugEnabled()) {
      log.debug("Removing context[" + context.getName() + "]");
    }
    boolean found = false;
    for (int i = 0; i < this.engines.length; i++)
    {
      Container[] hosts = this.engines[i].findChildren();
      for (int j = 0; j < hosts.length; j++)
      {
        Container[] contexts = hosts[j].findChildren();
        for (int k = 0; k < contexts.length; k++) {
          if (context == (Context)contexts[k])
          {
            found = true;
            break;
          }
        }
        if (found) {
          break;
        }
      }
      if (found) {
        break;
      }
    }
    if (!found) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(" Removing this Context");
    }
    context.getParent().removeChild(context);
  }
  
  public synchronized void removeEngine(Engine engine)
  {
    if (log.isDebugEnabled()) {
      log.debug("Removing engine (" + engine.getInfo() + ")");
    }
    int j = -1;
    for (int i = 0; i < this.engines.length; i++) {
      if (engine == this.engines[i])
      {
        j = i;
        break;
      }
    }
    if (j < 0) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(" Removing related Containers");
    }
    for (;;)
    {
      int n = -1;
      for (int i = 0; i < this.connectors.length; i++) {
        if (this.connectors[i].getService().getContainer() == engine)
        {
          n = i;
          break;
        }
      }
      if (n < 0) {
        break;
      }
      removeConnector(this.connectors[n]);
    }
    if (log.isDebugEnabled()) {
      log.debug(" Stopping this Engine");
    }
    try
    {
      engine.stop();
    }
    catch (LifecycleException e)
    {
      log.error("Engine.stop", e);
    }
    if (log.isDebugEnabled()) {
      log.debug(" Removing this Engine");
    }
    int k = 0;
    Engine[] results = new Engine[this.engines.length - 1];
    for (int i = 0; i < this.engines.length; i++) {
      if (i != j) {
        results[(k++)] = this.engines[i];
      }
    }
    this.engines = results;
  }
  
  public synchronized void removeHost(Host host)
  {
    if (log.isDebugEnabled()) {
      log.debug("Removing host[" + host.getName() + "]");
    }
    boolean found = false;
    for (int i = 0; i < this.engines.length; i++)
    {
      Container[] hosts = this.engines[i].findChildren();
      for (int j = 0; j < hosts.length; j++) {
        if (host == (Host)hosts[j])
        {
          found = true;
          break;
        }
      }
      if (found) {
        break;
      }
    }
    if (!found) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(" Removing this Host");
    }
    host.getParent().removeChild(host);
  }
  
  public void addAuthenticator(Authenticator authenticator, String loginMethod)
  {
    if (!(authenticator instanceof Valve)) {
      throw new IllegalArgumentException(sm.getString("embedded.authenticatorNotInstanceOfValve"));
    }
    if (this.authenticators == null) {
      synchronized (this)
      {
        if (this.authenticators == null) {
          this.authenticators = new HashMap();
        }
      }
    }
    this.authenticators.put(loginMethod, authenticator);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (log.isInfoEnabled()) {
      log.info("Starting tomcat server");
    }
    initDirs();
    
    initNaming();
    
    setState(LifecycleState.STARTING);
    for (int i = 0; i < this.engines.length; i++) {
      this.engines[i].start();
    }
    for (int i = 0; i < this.connectors.length; i++) {
      this.connectors[i].start();
    }
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    if (log.isDebugEnabled()) {
      log.debug("Stopping embedded server");
    }
    setState(LifecycleState.STOPPING);
    for (int i = 0; i < this.connectors.length; i++) {
      this.connectors[i].stop();
    }
    for (int i = 0; i < this.engines.length; i++) {
      this.engines[i].stop();
    }
  }
  
  protected void initNaming()
  {
    if (!this.useNaming)
    {
      log.info("Catalina naming disabled");
      System.setProperty("catalina.useNaming", "false");
    }
    else
    {
      System.setProperty("catalina.useNaming", "true");
      String value = "org.apache.naming";
      String oldValue = System.getProperty("java.naming.factory.url.pkgs");
      if (oldValue != null) {
        value = value + ":" + oldValue;
      }
      System.setProperty("java.naming.factory.url.pkgs", value);
      if (log.isDebugEnabled()) {
        log.debug("Setting naming prefix=" + value);
      }
      value = System.getProperty("java.naming.factory.initial");
      if (value == null) {
        System.setProperty("java.naming.factory.initial", "org.apache.naming.java.javaURLContextFactory");
      } else {
        log.debug("INITIAL_CONTEXT_FACTORY already set " + value);
      }
    }
  }
  
  protected void initDirs()
  {
    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome == null)
    {
      String j2eeHome = System.getProperty("com.sun.enterprise.home");
      if (j2eeHome != null)
      {
        catalinaHome = System.getProperty("com.sun.enterprise.home");
      }
      else if (System.getProperty("catalina.base") != null)
      {
        catalinaHome = System.getProperty("catalina.base");
      }
      else
      {
        catalinaHome = IntrospectionUtils.guessInstall("catalina.home", "catalina.base", "catalina.jar");
        if (catalinaHome == null) {
          catalinaHome = IntrospectionUtils.guessInstall("tomcat.install", "catalina.home", "tomcat.jar");
        }
      }
    }
    if (catalinaHome == null) {
      catalinaHome = System.getProperty("user.dir");
    }
    if (catalinaHome != null)
    {
      File home = new File(catalinaHome);
      if (!home.isAbsolute()) {
        try
        {
          catalinaHome = home.getCanonicalPath();
        }
        catch (IOException e)
        {
          catalinaHome = home.getAbsolutePath();
        }
      }
      System.setProperty("catalina.home", catalinaHome);
    }
    if (System.getProperty("catalina.base") == null)
    {
      System.setProperty("catalina.base", catalinaHome);
    }
    else
    {
      String catalinaBase = System.getProperty("catalina.base");
      File base = new File(catalinaBase);
      if (!base.isAbsolute()) {
        try
        {
          catalinaBase = base.getCanonicalPath();
        }
        catch (IOException e)
        {
          catalinaBase = base.getAbsolutePath();
        }
      }
      System.setProperty("catalina.base", catalinaBase);
    }
    String temp = System.getProperty("java.io.tmpdir");
    if ((temp == null) || (!new File(temp).exists()) || (!new File(temp).isDirectory())) {
      log.error(sm.getString("embedded.notmp", new Object[] { temp }));
    }
  }
  
  protected void initStreams()
  {
    if (this.redirectStreams)
    {
      System.setOut(new SystemLogHandler(System.out));
      System.setErr(new SystemLogHandler(System.err));
    }
  }
  
  protected void setSecurityProtection()
  {
    SecurityConfig securityConfig = SecurityConfig.newInstance();
    securityConfig.setPackageDefinition();
    securityConfig.setPackageAccess();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\Embedded.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */