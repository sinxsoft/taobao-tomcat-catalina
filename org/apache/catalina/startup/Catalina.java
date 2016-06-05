package org.apache.catalina.startup;

import com.taobao.tomcat.digester.ModuleServiceCreateRule;
import com.taobao.tomcat.digester.ServerListenerCreateRule;
import com.taobao.tomcat.digester.ServerListenerSetNextRule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogManager;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.security.SecurityConfig;
import org.apache.juli.ClassLoaderLogManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.res.StringManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class Catalina
{
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  protected boolean await = false;
  protected String configFile = "conf/server.xml";
  protected ClassLoader parentClassLoader = Catalina.class.getClassLoader();
  protected Server server = null;
  @Deprecated
  protected boolean starting = false;
  @Deprecated
  protected boolean stopping = false;
  protected boolean useShutdownHook = true;
  protected Thread shutdownHook = null;
  protected boolean useNaming = true;
  
  public Catalina()
  {
    setSecurityProtection();
  }
  
  @Deprecated
  public void setConfig(String file)
  {
    this.configFile = file;
  }
  
  public void setConfigFile(String file)
  {
    this.configFile = file;
  }
  
  public String getConfigFile()
  {
    return this.configFile;
  }
  
  public void setUseShutdownHook(boolean useShutdownHook)
  {
    this.useShutdownHook = useShutdownHook;
  }
  
  public boolean getUseShutdownHook()
  {
    return this.useShutdownHook;
  }
  
  public void setParentClassLoader(ClassLoader parentClassLoader)
  {
    this.parentClassLoader = parentClassLoader;
  }
  
  public ClassLoader getParentClassLoader()
  {
    if (this.parentClassLoader != null) {
      return this.parentClassLoader;
    }
    return ClassLoader.getSystemClassLoader();
  }
  
  public void setServer(Server server)
  {
    this.server = server;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public boolean isUseNaming()
  {
    return this.useNaming;
  }
  
  public void setUseNaming(boolean useNaming)
  {
    this.useNaming = useNaming;
  }
  
  public void setAwait(boolean b)
  {
    this.await = b;
  }
  
  public boolean isAwait()
  {
    return this.await;
  }
  
  protected boolean arguments(String[] args)
  {
    boolean isConfig = false;
    if (args.length < 1)
    {
      usage();
      return false;
    }
    for (int i = 0; i < args.length; i++) {
      if (isConfig)
      {
        this.configFile = args[i];
        isConfig = false;
      }
      else if (args[i].equals("-config"))
      {
        isConfig = true;
      }
      else if (args[i].equals("-nonaming"))
      {
        setUseNaming(false);
      }
      else
      {
        if (args[i].equals("-help"))
        {
          usage();
          return false;
        }
        if (args[i].equals("start"))
        {
          this.starting = true;
          this.stopping = false;
        }
        else if (args[i].equals("configtest"))
        {
          this.starting = true;
          this.stopping = false;
        }
        else if (args[i].equals("stop"))
        {
          this.starting = false;
          this.stopping = true;
        }
        else
        {
          usage();
          return false;
        }
      }
    }
    return true;
  }
  
  protected File configFile()
  {
    File file = new File(this.configFile);
    if (!file.isAbsolute()) {
      file = new File(System.getProperty("catalina.base"), this.configFile);
    }
    return file;
  }
  
  protected Digester createStartDigester()
  {
    long t1 = System.currentTimeMillis();
    
    Digester digester = new Digester();
    digester.setValidating(false);
    digester.setRulesValidation(true);
    HashMap<Class<?>, List<String>> fakeAttributes = new HashMap();
    
    ArrayList<String> attrs = new ArrayList();
    attrs.add("className");
    fakeAttributes.put(Object.class, attrs);
    digester.setFakeAttributes(fakeAttributes);
    digester.setUseContextClassLoader(true);
    
    digester.addObjectCreate("Server", "org.apache.catalina.core.StandardServer", "className");
    
    digester.addSetProperties("Server");
    digester.addSetNext("Server", "setServer", "org.apache.catalina.Server");
    
    digester.addObjectCreate("Server/GlobalNamingResources", "org.apache.catalina.deploy.NamingResources");
    
    digester.addSetProperties("Server/GlobalNamingResources");
    digester.addSetNext("Server/GlobalNamingResources", "setGlobalNamingResources", "org.apache.catalina.deploy.NamingResources");
    
    digester.addRule("Server/Listener", new ServerListenerCreateRule(null, "className"));
    
    digester.addSetProperties("Server/Listener");
    
    digester.addRule("Server/Listener", new ServerListenerSetNextRule("addLifecycleListener", "org.apache.catalina.LifecycleListener"));
    
    digester.addRule("Server/ModuleService", new ModuleServiceCreateRule("com.taobao.tomcat.modular.StandardModuleService", "className"));
    
    digester.addObjectCreate("Server/Service", "org.apache.catalina.core.StandardService", "className");
    
    digester.addSetProperties("Server/Service");
    digester.addSetNext("Server/Service", "addService", "org.apache.catalina.Service");
    
    digester.addObjectCreate("Server/Service/Listener", null, "className");
    
    digester.addSetProperties("Server/Service/Listener");
    digester.addSetNext("Server/Service/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");
    
    digester.addObjectCreate("Server/Service/Executor", "org.apache.catalina.core.StandardThreadExecutor", "className");
    
    digester.addSetProperties("Server/Service/Executor");
    
    digester.addSetNext("Server/Service/Executor", "addExecutor", "org.apache.catalina.Executor");
    
    digester.addRule("Server/Service/Connector", new ConnectorCreateRule());
    
    digester.addRule("Server/Service/Connector", new SetAllPropertiesRule(new String[] { "executor" }));
    
    digester.addSetNext("Server/Service/Connector", "addConnector", "org.apache.catalina.connector.Connector");
    
    digester.addObjectCreate("Server/Service/Connector/Listener", null, "className");
    
    digester.addSetProperties("Server/Service/Connector/Listener");
    digester.addSetNext("Server/Service/Connector/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");
    
    digester.addRuleSet(new NamingRuleSet("Server/GlobalNamingResources/"));
    digester.addRuleSet(new EngineRuleSet("Server/Service/"));
    digester.addRuleSet(new HostRuleSet("Server/Service/Engine/"));
    digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Host/"));
    addClusterRuleSet(digester, "Server/Service/Engine/Host/Cluster/");
    digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/Host/Context/"));
    
    digester.addRule("Server/Service/Engine", new SetParentClassLoaderRule(this.parentClassLoader));
    
    addClusterRuleSet(digester, "Server/Service/Engine/Cluster/");
    
    long t2 = System.currentTimeMillis();
    if (log.isDebugEnabled()) {
      log.debug("Digester for server.xml created " + (t2 - t1));
    }
    return digester;
  }
  
  private void addClusterRuleSet(Digester digester, String prefix)
  {
    Class<?> clazz = null;
    Constructor<?> constructor = null;
    try
    {
      clazz = Class.forName("org.apache.catalina.ha.ClusterRuleSet");
      constructor = clazz.getConstructor(new Class[] { String.class });
      RuleSet ruleSet = (RuleSet)constructor.newInstance(new Object[] { prefix });
      digester.addRuleSet(ruleSet);
    }
    catch (Exception e)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("catalina.noCluster", new Object[] { e.getClass().getName() + ": " + e.getMessage() }), e);
      } else if (log.isInfoEnabled()) {
        log.info(sm.getString("catalina.noCluster", new Object[] { e.getClass().getName() + ": " + e.getMessage() }));
      }
    }
  }
  
  protected Digester createStopDigester()
  {
    Digester digester = new Digester();
    digester.setUseContextClassLoader(true);
    
    digester.addObjectCreate("Server", "org.apache.catalina.core.StandardServer", "className");
    
    digester.addSetProperties("Server");
    digester.addSetNext("Server", "setServer", "org.apache.catalina.Server");
    
    return digester;
  }
  
  public void stopServer()
  {
    stopServer(null);
  }
  
  public void stopServer(String[] arguments)
  {
      if (arguments != null) {
          arguments(arguments);
      }

      Server s = getServer();
      if( s == null ) {
          // Create and execute our Digester
          Digester digester = createStopDigester();
          File file = configFile();
          FileInputStream fis = null;
          try {
              InputSource is =
                  new InputSource(file.toURI().toURL().toString());
              fis = new FileInputStream(file);
              is.setByteStream(fis);
              digester.push(this);
              digester.parse(is);
          } catch (Exception e) {
              log.error("Catalina.stop: ", e);
              System.exit(1);
          } finally {
              if (fis != null) {
                  try {
                      fis.close();
                  } catch (IOException e) {
                      // Ignore
                  }
              }
          }
      } else {
          // Server object already present. Must be running as a service
          try {
              s.stop();
          } catch (LifecycleException e) {
              log.error("Catalina.stop: ", e);
          }
          return;
      }

      // Stop the existing server
      s = getServer();
      if (s.getPort()>0) {
          Socket socket = null;
          OutputStream stream = null;
          try {
              socket = new Socket(s.getAddress(), s.getPort());
              stream = socket.getOutputStream();
              String shutdown = s.getShutdown();
              for (int i = 0; i < shutdown.length(); i++) {
                  stream.write(shutdown.charAt(i));
              }
              stream.flush();
          } catch (ConnectException ce) {
              log.error(sm.getString("catalina.stopServer.connectException",
                                     s.getAddress(),
                                     String.valueOf(s.getPort())));
              log.error("Catalina.stop: ", ce);
              System.exit(1);
          } catch (IOException e) {
              log.error("Catalina.stop: ", e);
              System.exit(1);
          } finally {
              if (stream != null) {
                  try {
                      stream.close();
                  } catch (IOException e) {
                      // Ignore
                  }
              }
              if (socket != null) {
                  try {
                      socket.close();
                  } catch (IOException e) {
                      // Ignore
                  }
              }
          }
      } else {
          log.error(sm.getString("catalina.stopServer"));
          System.exit(1);
      }
  }
  
  public void load()
  {
    long t1 = System.nanoTime();
    
    initDirs();
    
    initNaming();
    
    Digester digester = createStartDigester();
    
    InputSource inputSource = null;
    InputStream inputStream = null;
    File file = null;
    try
    {
      file = configFile();
      inputStream = new FileInputStream(file);
      inputSource = new InputSource(file.toURI().toURL().toString());
    }
    catch (Exception e)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("catalina.configFail", new Object[] { file }), e);
      }
    }
    if (inputStream == null) {
      try
      {
        inputStream = getClass().getClassLoader().getResourceAsStream(getConfigFile());
        
        inputSource = new InputSource(getClass().getClassLoader().getResource(getConfigFile()).toString());
      }
      catch (Exception e)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("catalina.configFail", new Object[] { getConfigFile() }), e);
        }
      }
    }
    if (inputStream == null) {
      try
      {
        inputStream = getClass().getClassLoader().getResourceAsStream("server-embed.xml");
        
        inputSource = new InputSource(getClass().getClassLoader().getResource("server-embed.xml").toString());
      }
      catch (Exception e)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("catalina.configFail", new Object[] { "server-embed.xml" }), e);
        }
      }
    }
    if ((inputStream == null) || (inputSource == null))
    {
      if (file == null)
      {
        log.warn(sm.getString("catalina.configFail", new Object[] { getConfigFile() + "] or [server-embed.xml]" }));
      }
      else
      {
        log.warn(sm.getString("catalina.configFail", new Object[] { file.getAbsolutePath() }));
        if ((file.exists()) && (!file.canRead())) {
          log.warn("Permissions incorrect, read permission is not allowed on the file.");
        }
      }
      return;
    }
    try
    {
      inputSource.setByteStream(inputStream);
      digester.push(this);
      digester.parse(inputSource);
      try
      {
        inputStream.close();
      }
      catch (IOException e) {}
      getServer().setCatalina(this);
    }
    catch (SAXParseException spe)
    {
      log.warn("Catalina.start using " + getConfigFile() + ": " + spe.getMessage()); return;
    }
    catch (Exception e)
    {
      log.warn("Catalina.start using " + getConfigFile() + ": ", e); return;
    }
    finally
    {
      try
      {
        inputStream.close();
      }
      catch (IOException e) {}
    }
    initStreams();
    try
    {
      getServer().init();
    }
    catch (LifecycleException e)
    {
      if (Boolean.getBoolean("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE")) {
        throw new Error(e);
      }
      log.error("Catalina.start", e);
    }
    long t2 = System.nanoTime();
    if (log.isInfoEnabled()) {
      log.info("Initialization processed in " + (t2 - t1) / 1000000L + " ms");
    }
  }
  
  public void load(String[] args)
  {
    try
    {
      if (arguments(args)) {
        load();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
  
  public void start()
  {
    if (getServer() == null) {
      load();
    }
    if (getServer() == null)
    {
      log.fatal("Cannot start server. Server instance is not configured.");
      return;
    }
    long t1 = System.nanoTime();
    try
    {
      getServer().start();
    }
    catch (LifecycleException e)
    {
      log.fatal(sm.getString("catalina.serverStartFail"), e);
      try
      {
        getServer().destroy();
      }
      catch (LifecycleException e1)
      {
        log.debug("destroy() failed for failed Server ", e1);
      }
      return;
    }
    long t2 = System.nanoTime();
    if (log.isInfoEnabled()) {
      log.info("Server startup in " + (t2 - t1) / 1000000L + " ms");
    }
    if (this.useShutdownHook)
    {
      if (this.shutdownHook == null) {
        this.shutdownHook = new CatalinaShutdownHook();
      }
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
      
      LogManager logManager = LogManager.getLogManager();
      if ((logManager instanceof ClassLoaderLogManager)) {
        ((ClassLoaderLogManager)logManager).setUseShutdownHook(false);
      }
    }
    if (this.await)
    {
      await();
      stop();
    }
  }
  
  public void stop()
  {
    try
    {
      if (this.useShutdownHook)
      {
        Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        
        LogManager logManager = LogManager.getLogManager();
        if ((logManager instanceof ClassLoaderLogManager)) {
          ((ClassLoaderLogManager)logManager).setUseShutdownHook(true);
        }
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    try
    {
      Server s = getServer();
      LifecycleState state = s.getState();
      if ((LifecycleState.STOPPING_PREP.compareTo(state) > 0) || (LifecycleState.DESTROYED.compareTo(state) < 0))
      {
        s.stop();
        s.destroy();
      }
    }
    catch (LifecycleException e)
    {
      log.error("Catalina.stop", e);
    }
  }
  
  public void await()
  {
    getServer().await();
  }
  
  protected void usage()
  {
    System.out.println("usage: java org.apache.catalina.startup.Catalina [ -config {pathname} ] [ -nonaming ]  { -help | start | stop }");
  }
  
  protected void initDirs()
  {
    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome == null)
    {
      String j2eeHome = System.getProperty("com.sun.enterprise.home");
      if (j2eeHome != null) {
        catalinaHome = System.getProperty("com.sun.enterprise.home");
      } else if (System.getProperty("catalina.base") != null) {
        catalinaHome = System.getProperty("catalina.base");
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
    System.setOut(new SystemLogHandler(System.out));
    System.setErr(new SystemLogHandler(System.err));
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
  
  protected void setSecurityProtection()
  {
    SecurityConfig securityConfig = SecurityConfig.newInstance();
    securityConfig.setPackageDefinition();
    securityConfig.setPackageAccess();
  }
  
  protected class CatalinaShutdownHook
    extends Thread
  {
    protected CatalinaShutdownHook() {}
    
    public void run()
    {
      try
      {
        if (Catalina.this.getServer() != null) {
          Catalina.this.stop();
        }
      }
      catch (Throwable ex)
      {
        LogManager logManager;
        ExceptionUtils.handleThrowable(ex);
        Catalina.log.error(Catalina.sm.getString("catalina.shutdownHookFail"), ex);
      }
      finally
      {
        LogManager logManager;
        logManager = LogManager.getLogManager();
        if ((logManager instanceof ClassLoaderLogManager)) {
          ((ClassLoaderLogManager)logManager).shutdown();
        }
      }
    }
  }
  
  private static final Log log = LogFactory.getLog(Catalina.class);
}

