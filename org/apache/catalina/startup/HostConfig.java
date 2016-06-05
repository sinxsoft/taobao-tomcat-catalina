package org.apache.catalina.startup;

import com.taobao.tomcat.container.host.AliHostConfigHelper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.ObjectName;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DistributedManager;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.util.IOTools;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

public class HostConfig
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(HostConfig.class);
  protected File appBase;
  protected File configBase;
  @Deprecated
  protected String configClass;
  protected String contextClass;
  protected Host host;
  protected ObjectName oname;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  protected boolean deployXML;
  protected boolean copyXML;
  protected boolean unpackWARs;
  protected Map<String, DeployedApplication> deployed;
  protected ArrayList<String> serviced;
  protected Digester digester;
  private final Object digesterLock;
  protected Set<String> invalidWars;
  
  public HostConfig()
  {
    this.appBase = null;
    
    this.configBase = null;
    
    this.configClass = "org.apache.catalina.startup.ContextConfig";
    
    this.contextClass = "org.apache.catalina.core.StandardContext";
    
    this.host = null;
    
    this.oname = null;
    
    this.deployXML = false;
    
    this.copyXML = false;
    
    this.unpackWARs = false;
    
    this.deployed = new ConcurrentHashMap();
    
    this.serviced = new ArrayList();
    
    this.digester = createDigester(this.contextClass);
    this.digesterLock = new Object();
    
    this.invalidWars = new HashSet();
  }
  
  @Deprecated
  public String getConfigClass()
  {
    return this.configClass;
  }
  
  @Deprecated
  public void setConfigClass(String configClass)
  {
    this.configClass = configClass;
  }
  
  public String getContextClass()
  {
    return this.contextClass;
  }
  
  public void setContextClass(String contextClass)
  {
    String oldContextClass = this.contextClass;
    this.contextClass = contextClass;
    if (!oldContextClass.equals(contextClass)) {
      synchronized (this.digesterLock)
      {
        this.digester = createDigester(getContextClass());
      }
    }
  }
  
  public boolean isDeployXML()
  {
    return this.deployXML;
  }
  
  public void setDeployXML(boolean deployXML)
  {
    this.deployXML = deployXML;
  }
  
  public boolean isCopyXML()
  {
    return this.copyXML;
  }
  
  public void setCopyXML(boolean copyXML)
  {
    this.copyXML = copyXML;
  }
  
  public boolean isUnpackWARs()
  {
    return this.unpackWARs;
  }
  
  public void setUnpackWARs(boolean unpackWARs)
  {
    this.unpackWARs = unpackWARs;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      this.host = ((Host)event.getLifecycle());
      if ((this.host instanceof StandardHost))
      {
        setCopyXML(((StandardHost)this.host).isCopyXML());
        setDeployXML(((StandardHost)this.host).isDeployXML());
        setUnpackWARs(((StandardHost)this.host).isUnpackWARs());
        setContextClass(((StandardHost)this.host).getContextClass());
      }
    }
    catch (ClassCastException e)
    {
      log.error(sm.getString("hostConfig.cce", new Object[] { event.getLifecycle() }), e);
      return;
    }
    if (event.getType().equals("periodic")) {
      check();
    } else if (event.getType().equals("start")) {
      start();
    } else if (event.getType().equals("stop")) {
      stop();
    }
  }
  
  public synchronized void addServiced(String name)
  {
    this.serviced.add(name);
  }
  
  public synchronized boolean isServiced(String name)
  {
    return this.serviced.contains(name);
  }
  
  public synchronized void removeServiced(String name)
  {
    this.serviced.remove(name);
  }
  
  public long getDeploymentTime(String name)
  {
    DeployedApplication app = (DeployedApplication)this.deployed.get(name);
    if (app == null) {
      return 0L;
    }
    return app.timestamp;
  }
  
  public boolean isDeployed(String name)
  {
    DeployedApplication app = (DeployedApplication)this.deployed.get(name);
    if (app == null) {
      return false;
    }
    return true;
  }
  
  protected static Digester createDigester(String contextClassName)
  {
    Digester digester = new Digester();
    digester.setValidating(false);
    
    digester.addObjectCreate("Context", contextClassName, "className");
    
    digester.addSetProperties("Context");
    return digester;
  }
  
  protected File returnCanonicalPath(String path)
  {
    File file = new File(path);
    File base = new File(System.getProperty("catalina.base"));
    if (!file.isAbsolute()) {
      file = new File(base, path);
    }
    try
    {
      return file.getCanonicalFile();
    }
    catch (IOException e) {}
    return file;
  }
  
  protected File appBase()
  {
    if (this.appBase != null) {
      return this.appBase;
    }
    this.appBase = returnCanonicalPath(this.host.getAppBase());
    return this.appBase;
  }
  
  protected File configBase()
  {
    if (this.configBase != null) {
      return this.configBase;
    }
    if (this.host.getXmlBase() != null)
    {
      this.configBase = returnCanonicalPath(this.host.getXmlBase());
    }
    else
    {
      StringBuilder xmlDir = new StringBuilder("conf");
      Container parent = this.host.getParent();
      if ((parent instanceof Engine))
      {
        xmlDir.append('/');
        xmlDir.append(parent.getName());
      }
      xmlDir.append('/');
      xmlDir.append(this.host.getName());
      this.configBase = returnCanonicalPath(xmlDir.toString());
    }
    return this.configBase;
  }
  
  public String getConfigBaseName()
  {
    return configBase().getAbsolutePath();
  }
  
  protected void deployApps()
  {
    File appBase = appBase();
    File configBase = configBase();
    String[] filteredAppPaths = filterAppPaths(appBase.list());
    
    deployDescriptors(configBase, configBase.list());
    
    deployWARs(appBase, filteredAppPaths);
    
    deployDirectories(appBase, filteredAppPaths);
  }
  
  protected String[] filterAppPaths(String[] unfilteredAppPaths)
  {
    Pattern filter = this.host.getDeployIgnorePattern();
    if (filter == null) {
      return unfilteredAppPaths;
    }
    List<String> filteredList = new ArrayList();
    Matcher matcher = null;
    for (String appPath : unfilteredAppPaths)
    {
      if (matcher == null) {
        matcher = filter.matcher(appPath);
      } else {
        matcher.reset(appPath);
      }
      if (matcher.matches())
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("hostConfig.ignorePath", new Object[] { appPath }));
        }
      }
      else {
        filteredList.add(appPath);
      }
    }
    return (String[])filteredList.toArray(new String[filteredList.size()]);
  }
  
  protected void deployApps(String name)
  {
    File appBase = appBase();
    File configBase = configBase();
    ContextName cn = new ContextName(name, false);
    String baseName = cn.getBaseName();
    if (deploymentExists(cn.getName())) {
      return;
    }
    File xml = new File(configBase, baseName + ".xml");
    if (xml.exists())
    {
      deployDescriptor(cn, xml);
      return;
    }
    File war = new File(appBase, baseName + ".war");
    if (war.exists())
    {
      deployWAR(cn, war);
      return;
    }
    File dir = new File(appBase, baseName);
    if (dir.exists()) {
      deployDirectory(cn, dir);
    }
  }
  
  protected void deployDescriptors(File configBase, String[] files)
  {
    if (files == null) {
      return;
    }
    ExecutorService es = this.host.getStartStopExecutor();
    List<Future<?>> results = new ArrayList();
    for (int i = 0; i < files.length; i++)
    {
      File contextXml = new File(configBase, files[i]);
      if (files[i].toLowerCase(Locale.ENGLISH).endsWith(".xml"))
      {
        ContextName cn = new ContextName(files[i], true);
        if ((!isServiced(cn.getName())) && (!deploymentExists(cn.getName()))) {
          results.add(es.submit(new DeployDescriptor(this, cn, contextXml)));
        }
      }
    }
    for (Future<?> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        log.error(sm.getString("hostConfig.deployDescriptor.threaded.error"), e);
      }
    }
  }
  
  protected void deployDescriptor(ContextName cn, File contextXml)
  {
    DeployedApplication deployedApp = new DeployedApplication(cn.getName(), true);
    
    long startTime = 0L;
    if (log.isInfoEnabled())
    {
      startTime = System.currentTimeMillis();
      log.info(sm.getString("hostConfig.deployDescriptor", new Object[] { contextXml.getAbsolutePath() }));
    }
    Context context = null;
    boolean isExternalWar = false;
    boolean isExternal = false;
    File expandedDocBase = null;
    FileInputStream fis = null;
    try
    {
      fis = new FileInputStream(contextXml);
      synchronized (this.digesterLock)
      {
        try
        {
          context = (Context)this.digester.parse(fis);
        }
        catch (Exception e)
        {
          log.error(sm.getString("hostConfig.deployDescriptor.error", new Object[] { contextXml.getAbsolutePath() }), e);
          
          context = new FailedContext();
        }
        finally
        {
          this.digester.reset();
        }
      }
      Class<?> clazz = Class.forName(this.host.getConfigClass());
      LifecycleListener listener = (LifecycleListener)clazz.newInstance();
      
      context.addLifecycleListener(listener);
      
      context.setConfigFile(contextXml.toURI().toURL());
      context.setName(cn.getName());
      context.setPath(cn.getPath());
      context.setWebappVersion(cn.getVersion());
      if (context.getDocBase() != null)
      {
        File docBase = new File(context.getDocBase());
        if (!docBase.isAbsolute()) {
          docBase = new File(appBase(), context.getDocBase());
        }
        if (!docBase.getCanonicalPath().startsWith(appBase().getAbsolutePath() + File.separator))
        {
          isExternal = true;
          deployedApp.redeployResources.put(contextXml.getAbsolutePath(), Long.valueOf(contextXml.lastModified()));
          
          deployedApp.redeployResources.put(docBase.getAbsolutePath(), Long.valueOf(docBase.lastModified()));
          if (docBase.getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(".war")) {
            isExternalWar = true;
          }
        }
        else
        {
          log.warn(sm.getString("hostConfig.deployDescriptor.localDocBaseSpecified", new Object[] { docBase }));
          
          context.setDocBase(null);
        }
      }
      this.host.addChild(context);
    }
    catch (Throwable t)
    {
      File warDocBase;
      ExceptionUtils.handleThrowable(t);
      log.error(sm.getString("hostConfig.deployDescriptor.error", new Object[] { contextXml.getAbsolutePath() }), t);
    }
    finally
    {
      File warDocBase;
      if (fis != null) {
        try
        {
          fis.close();
        }
        catch (IOException e) {}
      }
      expandedDocBase = new File(appBase(), cn.getBaseName());
      if (context.getDocBase() != null)
      {
        expandedDocBase = new File(context.getDocBase());
        if (!expandedDocBase.isAbsolute()) {
          expandedDocBase = new File(appBase(), context.getDocBase());
        }
      }
      if ((isExternalWar) && (this.unpackWARs))
      {
        deployedApp.redeployResources.put(expandedDocBase.getAbsolutePath(), Long.valueOf(expandedDocBase.lastModified()));
        
        addWatchedResources(deployedApp, expandedDocBase.getAbsolutePath(), context);
      }
      else
      {
        if (!isExternal)
        {
          warDocBase = new File(expandedDocBase.getAbsolutePath() + ".war");
          if (warDocBase.exists()) {
            deployedApp.redeployResources.put(warDocBase.getAbsolutePath(), Long.valueOf(warDocBase.lastModified()));
          } else {
            deployedApp.redeployResources.put(warDocBase.getAbsolutePath(), Long.valueOf(0L));
          }
        }
        if (expandedDocBase.exists())
        {
          deployedApp.redeployResources.put(expandedDocBase.getAbsolutePath(), Long.valueOf(expandedDocBase.lastModified()));
          
          addWatchedResources(deployedApp, expandedDocBase.getAbsolutePath(), context);
        }
        else
        {
          if ((!isExternal) && (!this.unpackWARs)) {
            deployedApp.reloadResources.put(expandedDocBase.getAbsolutePath(), Long.valueOf(0L));
          }
          addWatchedResources(deployedApp, null, context);
        }
        if (!isExternal) {
          deployedApp.redeployResources.put(contextXml.getAbsolutePath(), Long.valueOf(contextXml.lastModified()));
        }
      }
      addGlobalRedeployResources(deployedApp);
    }
    if (this.host.findChild(context.getName()) != null) {
      this.deployed.put(context.getName(), deployedApp);
    }
    if (log.isInfoEnabled()) {
      log.info(sm.getString("hostConfig.deployDescriptor.finished", new Object[] { contextXml.getAbsolutePath(), Long.valueOf(System.currentTimeMillis() - startTime) }));
    }
  }
  
  protected void deployWARs(File appBase, String[] files)
  {
    if (files == null) {
      return;
    }
    ExecutorService es = this.host.getStartStopExecutor();
    List<Future<?>> results = new ArrayList();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].equalsIgnoreCase("META-INF")) {
        if (!files[i].equalsIgnoreCase("WEB-INF"))
        {
          File war = new File(appBase, files[i]);
          if ((files[i].toLowerCase(Locale.ENGLISH).endsWith(".war")) && (war.isFile()) && (!this.invalidWars.contains(files[i])))
          {
            ContextName cn = AliHostConfigHelper.parseJbossWebXmlForWar(war);
            if (!isServiced(cn.getName())) {
              if (deploymentExists(cn.getName()))
              {
                DeployedApplication app = (DeployedApplication)this.deployed.get(cn.getName());
                if ((!this.unpackWARs) && (app != null))
                {
                  File dir = new File(appBase, cn.getBaseName());
                  if (dir.exists())
                  {
                    if (!app.loggedDirWarning)
                    {
                      log.warn(sm.getString("hostConfig.deployWar.hiddenDir", new Object[] { dir.getAbsoluteFile(), war.getAbsoluteFile() }));
                      
                      app.loggedDirWarning = true;
                    }
                  }
                  else {
                    app.loggedDirWarning = false;
                  }
                }
              }
              else if (!validateContextPath(appBase, cn.getBaseName()))
              {
                log.error(sm.getString("hostConfig.illegalWarName", new Object[] { files[i] }));
                
                this.invalidWars.add(files[i]);
              }
              else
              {
                results.add(es.submit(new DeployWar(this, cn, war)));
              }
            }
          }
        }
      }
    }
    for (Future<?> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        log.error(sm.getString("hostConfig.deployWar.threaded.error"), e);
      }
    }
  }
  
  private boolean validateContextPath(File appBase, String contextPath)
  {
    String canonicalDocBase = null;
    StringBuilder docBase;
    try
    {
      String canonicalAppBase = appBase.getCanonicalPath();
      docBase = new StringBuilder(canonicalAppBase);
      if (canonicalAppBase.endsWith(File.separator)) {
        docBase.append(contextPath.substring(1).replace('/', File.separatorChar));
      } else {
        docBase.append(contextPath.replace('/', File.separatorChar));
      }
      canonicalDocBase = new File(docBase.toString()).getCanonicalPath();
      if (canonicalDocBase.endsWith(File.separator)) {
        docBase.append(File.separator);
      }
    }
    catch (IOException ioe)
    {
      return false;
    }
    return canonicalDocBase.equals(docBase.toString());
  }
  
  protected void deployWAR(ContextName cn, File war)
  {
    JarFile jar = null;
    InputStream istream = null;
    FileOutputStream fos = null;
    BufferedOutputStream ostream = null;
    
    File xml = new File(appBase(), cn.getBaseName() + "/META-INF/context.xml");
    
    boolean xmlInWar = false;
    JarEntry entry = null;
    try
    {
      jar = new JarFile(war);
      entry = jar.getJarEntry("META-INF/context.xml");
      if (entry != null) {
        xmlInWar = true;
      }
    }
    catch (IOException e) {}finally
    {
      entry = null;
      if (jar != null)
      {
        try
        {
          jar.close();
        }
        catch (IOException ioe) {}
        jar = null;
      }
    }
    Context context = null;
    try
    {
      if ((this.deployXML) && (xml.exists()) && (!this.copyXML))
      {
        synchronized (this.digesterLock)
        {
          try
          {
            context = (Context)this.digester.parse(xml);
          }
          catch (Exception e)
          {
            log.error(sm.getString("hostConfig.deployDescriptor.error", new Object[] { war.getAbsolutePath() }), e);
          }
          finally
          {
            if (context == null) {
              context = new FailedContext();
            }
            this.digester.reset();
          }
        }
        context.setConfigFile(xml.toURI().toURL());
      }
      else if ((this.deployXML) && (xmlInWar))
      {
        synchronized (this.digesterLock)
        {
          try
          {
            jar = new JarFile(war);
            entry = jar.getJarEntry("META-INF/context.xml");
            
            istream = jar.getInputStream(entry);
            context = (Context)this.digester.parse(istream);
          }
          catch (Exception e)
          {
            log.error(sm.getString("hostConfig.deployDescriptor.error", new Object[] { war.getAbsolutePath() }), e);
          }
          finally
          {
            if (context == null) {
              context = new FailedContext();
            }
            context.setConfigFile(new URL("jar:" + war.toURI().toString() + "!/" + "META-INF/context.xml"));
            if (istream != null)
            {
              try
              {
                istream.close();
              }
              catch (IOException e) {}
              istream = null;
            }
            entry = null;
            if (jar != null)
            {
              try
              {
                jar.close();
              }
              catch (IOException e) {}
              jar = null;
            }
            this.digester.reset();
          }
        }
      }
      else if ((!this.deployXML) && (xmlInWar))
      {
        log.error(sm.getString("hostConfig.deployDescriptor.blocked", new Object[] { cn.getPath(), "META-INF/context.xml", new File(configBase(), cn.getBaseName() + ".xml") }));
      }
      else
      {
        context = (Context)Class.forName(this.contextClass).newInstance();
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.error(sm.getString("hostConfig.deployWar.error", new Object[] { war.getAbsolutePath() }), t);
    }
    finally
    {
      if (context == null) {
        context = new FailedContext();
      }
    }
    boolean copyThisXml = false;
    if (this.deployXML)
    {
      if ((this.host instanceof StandardHost)) {
        copyThisXml = ((StandardHost)this.host).isCopyXML();
      }
      if ((!copyThisXml) && ((context instanceof StandardContext))) {
        copyThisXml = ((StandardContext)context).getCopyXML();
      }
      if ((xmlInWar) && (copyThisXml))
      {
        xml = new File(configBase(), cn.getBaseName() + ".xml");
        entry = null;
        try
        {
          jar = new JarFile(war);
          entry = jar.getJarEntry("META-INF/context.xml");
          
          istream = jar.getInputStream(entry);
          
          fos = new FileOutputStream(xml);
          ostream = new BufferedOutputStream(fos, 1024);
          byte[] buffer = new byte['Ѐ'];
          for (;;)
          {
            int n = istream.read(buffer);
            if (n < 0) {
              break;
            }
            ostream.write(buffer, 0, n);
          }
          ostream.flush();
        }
        catch (IOException e) {}finally
        {
          if (ostream != null)
          {
            try
            {
              ostream.close();
            }
            catch (IOException ioe) {}
            ostream = null;
          }
          if (fos != null)
          {
            try
            {
              fos.close();
            }
            catch (IOException ioe) {}
            fos = null;
          }
          if (istream != null)
          {
            try
            {
              istream.close();
            }
            catch (IOException ioe) {}
            istream = null;
          }
          if (jar != null)
          {
            try
            {
              jar.close();
            }
            catch (IOException ioe) {}
            jar = null;
          }
        }
      }
    }
    DeployedApplication deployedApp = new DeployedApplication(cn.getName(), (xml.exists()) && (this.deployXML) && (copyThisXml));
    
    long startTime = 0L;
    if (log.isInfoEnabled())
    {
      startTime = System.currentTimeMillis();
      log.info(sm.getString("hostConfig.deployWar", new Object[] { war.getAbsolutePath() }));
    }
    try
    {
      deployedApp.redeployResources.put(war.getAbsolutePath(), Long.valueOf(war.lastModified()));
      if ((this.deployXML) && (xml.exists()) && (copyThisXml)) {
        deployedApp.redeployResources.put(xml.getAbsolutePath(), Long.valueOf(xml.lastModified()));
      } else {
        deployedApp.redeployResources.put(new File(configBase(), cn.getBaseName() + ".xml").getAbsolutePath(), Long.valueOf(0L));
      }
      Object clazz = Class.forName(this.host.getConfigClass());
      LifecycleListener listener = (LifecycleListener)((Class)clazz).newInstance();
      
      context.addLifecycleListener(listener);
      
      context.setName(cn.getName());
      context.setPath(cn.getPath());
      context.setWebappVersion(cn.getVersion());
      
      context.setDocBase(war.getAbsolutePath());
      
      this.host.addChild(context);
    }
    catch (Throwable t)
    {
      File docBase;
      ExceptionUtils.handleThrowable(t);
      log.error(sm.getString("hostConfig.deployWar.error", new Object[] { war.getAbsolutePath() }), t);
    }
    finally
    {
      File docBase;
      if ((this.unpackWARs) && (context != null) && (context.getDocBase() != null))
      {
        docBase = new File(appBase(), cn.getBaseName());
        deployedApp.redeployResources.put(docBase.getAbsolutePath(), Long.valueOf(docBase.lastModified()));
        
        addWatchedResources(deployedApp, docBase.getAbsolutePath(), context);
        if ((this.deployXML) && (!copyThisXml) && ((xmlInWar) || (xml.exists()))) {
          deployedApp.redeployResources.put(xml.getAbsolutePath(), Long.valueOf(xml.lastModified()));
        }
      }
      else
      {
        addWatchedResources(deployedApp, null, context);
      }
      addGlobalRedeployResources(deployedApp);
    }
    this.deployed.put(cn.getName(), deployedApp);
    if (log.isInfoEnabled()) {
      log.info(sm.getString("hostConfig.deployWar.finished", new Object[] { war.getAbsolutePath(), Long.valueOf(System.currentTimeMillis() - startTime) }));
    }
  }
  
  protected void deployDirectories(File appBase, String[] files)
  {
    if (files == null) {
      return;
    }
    ExecutorService es = this.host.getStartStopExecutor();
    List<Future<?>> results = new ArrayList();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].equalsIgnoreCase("META-INF")) {
        if (!files[i].equalsIgnoreCase("WEB-INF")) {
          if (!AliHostConfigHelper.isPandoraSarFile(files[i]))
          {
            File dir = new File(appBase, files[i]);
            if (dir.isDirectory())
            {
              ContextName cn = AliHostConfigHelper.parseJbossWebXml(dir);
              if ((!isServiced(cn.getName())) && (!deploymentExists(cn.getName()))) {
                results.add(es.submit(new DeployDirectory(this, cn, dir)));
              }
            }
          }
        }
      }
    }
    for (Future<?> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        log.error(sm.getString("hostConfig.deployDir.threaded.error"), e);
      }
    }
  }
  
  protected void deployDirectory(ContextName cn, File dir)
  {
    long startTime = 0L;
    if (log.isInfoEnabled())
    {
      startTime = System.currentTimeMillis();
      log.info(sm.getString("hostConfig.deployDir", new Object[] { dir.getAbsolutePath() }));
    }
    Context context = null;
    File xml = new File(dir, "META-INF/context.xml");
    File xmlCopy = new File(configBase(), cn.getBaseName() + ".xml");
    
    boolean copyThisXml = this.copyXML;
    DeployedApplication deployedApp;
    try
    {
      if ((this.deployXML) && (xml.exists()))
      {
        synchronized (this.digesterLock)
        {
          try
          {
            context = (Context)this.digester.parse(xml);
          }
          catch (Exception e)
          {
            log.error(sm.getString("hostConfig.deployDescriptor.error", new Object[] { xml }), e);
            
            context = new FailedContext();
          }
          finally
          {
            if (context == null) {
              context = new FailedContext();
            }
            this.digester.reset();
          }
        }
        if ((!copyThisXml) && ((context instanceof StandardContext))) {
          copyThisXml = ((StandardContext)context).getCopyXML();
        }
        if (copyThisXml)
        {
          InputStream is = null;
          OutputStream os = null;
          try
          {
            is = new FileInputStream(xml);
            os = new FileOutputStream(xmlCopy);
            IOTools.flow(is, os);
            try
            {
              if (is != null) {
                is.close();
              }
            }
            catch (IOException e) {}
            try
            {
              if (os != null) {
                os.close();
              }
            }
            catch (IOException e) {}
            context.setConfigFile(xmlCopy.toURI().toURL());
          }
          finally
          {
            try
            {
              if (is != null) {
                is.close();
              }
            }
            catch (IOException e) {}
            try
            {
              if (os != null) {
                os.close();
              }
            }
            catch (IOException e) {}
          }
        }
        else
        {
          context.setConfigFile(xml.toURI().toURL());
        }
      }
      else if ((!this.deployXML) && (xml.exists()))
      {
        log.error(sm.getString("hostConfig.deployDescriptor.blocked", new Object[] { cn.getPath(), xml, xmlCopy }));
        
        context = new FailedContext();
      }
      else
      {
        context = (Context)Class.forName(this.contextClass).newInstance();
      }
      Class<?> clazz = Class.forName(this.host.getConfigClass());
      LifecycleListener listener = (LifecycleListener)clazz.newInstance();
      
      context.addLifecycleListener(listener);
      
      context.setName(cn.getName());
      context.setPath(cn.getPath());
      context.setWebappVersion(cn.getVersion());
      
      context.setDocBase(dir.getAbsolutePath());
      
      this.host.addChild(context);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.error(sm.getString("hostConfig.deployDir.error", new Object[] { dir.getAbsolutePath() }), t);
    }
    finally
    {
      deployedApp = new DeployedApplication(cn.getName(), (xml.exists()) && (this.deployXML) && (copyThisXml));
      
      deployedApp.redeployResources.put(dir.getAbsolutePath() + ".war", Long.valueOf(0L));
      
      deployedApp.redeployResources.put(dir.getAbsolutePath(), Long.valueOf(dir.lastModified()));
      if ((this.deployXML) && (xml.exists()))
      {
        if (copyThisXml)
        {
          deployedApp.redeployResources.put(xmlCopy.getAbsolutePath(), Long.valueOf(xmlCopy.lastModified()));
        }
        else
        {
          deployedApp.redeployResources.put(xml.getAbsolutePath(), Long.valueOf(xml.lastModified()));
          
          deployedApp.redeployResources.put(xmlCopy.getAbsolutePath(), Long.valueOf(0L));
        }
      }
      else
      {
        deployedApp.redeployResources.put(xmlCopy.getAbsolutePath(), Long.valueOf(0L));
        if (!xml.exists()) {
          deployedApp.redeployResources.put(xml.getAbsolutePath(), Long.valueOf(0L));
        }
      }
      addWatchedResources(deployedApp, dir.getAbsolutePath(), context);
      
      addGlobalRedeployResources(deployedApp);
    }
    this.deployed.put(cn.getName(), deployedApp);
    if (log.isInfoEnabled()) {
      log.info(sm.getString("hostConfig.deployDir.finished", new Object[] { dir.getAbsolutePath(), Long.valueOf(System.currentTimeMillis() - startTime) }));
    }
  }
  
  protected boolean deploymentExists(String contextName)
  {
    return (this.deployed.containsKey(contextName)) || (this.host.findChild(contextName) != null);
  }
  
  protected void addWatchedResources(DeployedApplication app, String docBase, Context context)
  {
    File docBaseFile = null;
    if (docBase != null)
    {
      docBaseFile = new File(docBase);
      if (!docBaseFile.isAbsolute()) {
        docBaseFile = new File(appBase(), docBase);
      }
    }
    String[] watchedResources = context.findWatchedResources();
    for (int i = 0; i < watchedResources.length; i++)
    {
      File resource = new File(watchedResources[i]);
      if (!resource.isAbsolute()) {
        if (docBase != null)
        {
          resource = new File(docBaseFile, watchedResources[i]);
        }
        else
        {
          if (!log.isDebugEnabled()) {
            continue;
          }
          log.debug("Ignoring non-existent WatchedResource '" + resource.getAbsolutePath() + "'"); continue;
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Watching WatchedResource '" + resource.getAbsolutePath() + "'");
      }
      app.reloadResources.put(resource.getAbsolutePath(), Long.valueOf(resource.lastModified()));
    }
  }
  
  protected void addGlobalRedeployResources(DeployedApplication app)
  {
    File hostContextXml = new File(getConfigBaseName(), "context.xml.default");
    if (hostContextXml.isFile()) {
      app.redeployResources.put(hostContextXml.getAbsolutePath(), Long.valueOf(hostContextXml.lastModified()));
    }
    File globalContextXml = returnCanonicalPath("conf/context.xml");
    if (globalContextXml.isFile()) {
      app.redeployResources.put(globalContextXml.getAbsolutePath(), Long.valueOf(globalContextXml.lastModified()));
    }
  }
  
  protected synchronized void checkResources(DeployedApplication app)
  {
    String[] resources = (String[])app.redeployResources.keySet().toArray(new String[0]);
    for (int i = 0; i < resources.length; i++)
    {
      File resource = new File(resources[i]);
      if (log.isDebugEnabled()) {
        log.debug("Checking context[" + app.name + "] redeploy resource " + resource);
      }
      long lastModified = ((Long)app.redeployResources.get(resources[i])).longValue();
      if ((resource.exists()) || (lastModified == 0L))
      {
        if (resource.lastModified() > lastModified) {
          if (resource.isDirectory())
          {
            app.redeployResources.put(resources[i], Long.valueOf(resource.lastModified()));
          }
          else
          {
            if ((app.hasDescriptor) && (resource.getName().toLowerCase(Locale.ENGLISH).endsWith(".war")))
            {
              Context context = (Context)this.host.findChild(app.name);
              String docBase = context.getDocBase();
              if (!docBase.toLowerCase(Locale.ENGLISH).endsWith(".war"))
              {
                File docBaseFile = new File(docBase);
                if (!docBaseFile.isAbsolute()) {
                  docBaseFile = new File(appBase(), docBase);
                }
                ExpandWar.delete(docBaseFile);
                
                context.setDocBase(resource.getAbsolutePath());
              }
              reload(app);
              
              app.redeployResources.put(resources[i], Long.valueOf(resource.lastModified()));
              
              app.timestamp = System.currentTimeMillis();
              if (this.unpackWARs) {
                addWatchedResources(app, context.getDocBase(), context);
              } else {
                addWatchedResources(app, null, context);
              }
              return;
            }
            undeploy(app);
            deleteRedeployResources(app, resources, i, false);
          }
        }
      }
      else
      {
        try
        {
          Thread.sleep(500L);
        }
        catch (InterruptedException e1) {}
        if (!resource.exists()) {
          if (lastModified != 0L)
          {
            undeploy(app);
            deleteRedeployResources(app, resources, i, true);
            return;
          }
        }
      }
    }
    resources = (String[])app.reloadResources.keySet().toArray(new String[0]);
    for (int i = 0; i < resources.length; i++)
    {
      File resource = new File(resources[i]);
      if (log.isDebugEnabled()) {
        log.debug("Checking context[" + app.name + "] reload resource " + resource);
      }
      long lastModified = ((Long)app.reloadResources.get(resources[i])).longValue();
      if (((!resource.exists()) && (lastModified != 0L)) || (resource.lastModified() != lastModified))
      {
        reload(app);
        
        app.reloadResources.put(resources[i], Long.valueOf(resource.lastModified()));
        
        app.timestamp = System.currentTimeMillis();
        return;
      }
    }
  }
  
  private void reload(DeployedApplication app)
  {
    if (log.isInfoEnabled()) {
      log.info(sm.getString("hostConfig.reload", new Object[] { app.name }));
    }
    Context context = (Context)this.host.findChild(app.name);
    if (context.getState().isAvailable()) {
      context.reload();
    } else {
      try
      {
        context.start();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("hostConfig.context.restart", new Object[] { app.name }), e);
      }
    }
  }
  
  private void undeploy(DeployedApplication app)
  {
    if (log.isInfoEnabled()) {
      log.info(sm.getString("hostConfig.undeploy", new Object[] { app.name }));
    }
    Container context = this.host.findChild(app.name);
    try
    {
      this.host.removeChild(context);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.warn(sm.getString("hostConfig.context.remove", new Object[] { app.name }), t);
    }
    this.deployed.remove(app.name);
  }
  
  private void deleteRedeployResources(DeployedApplication app, String[] resources, int i, boolean deleteReloadResources)
  {
    for (int j = i + 1; j < resources.length; j++) {
      try
      {
        File current = new File(resources[j]);
        current = current.getCanonicalFile();
        if (!"context.xml.default".equals(current.getName())) {
          if (isDeletableResource(current))
          {
            if (log.isDebugEnabled()) {
              log.debug("Delete " + current);
            }
            ExpandWar.delete(current);
          }
        }
      }
      catch (IOException e)
      {
        log.warn(sm.getString("hostConfig.canonicalizing", new Object[] { app.name }), e);
      }
    }
    if (deleteReloadResources)
    {
      String[] resources2 = (String[])app.reloadResources.keySet().toArray(new String[0]);
      for (int j = 0; j < resources2.length; j++) {
        try
        {
          File current = new File(resources2[j]);
          current = current.getCanonicalFile();
          if (!"context.xml.default".equals(current.getName())) {
            if (isDeletableResource(current))
            {
              if (log.isDebugEnabled()) {
                log.debug("Delete " + current);
              }
              ExpandWar.delete(current);
            }
          }
        }
        catch (IOException e)
        {
          log.warn(sm.getString("hostConfig.canonicalizing", new Object[] { app.name }), e);
        }
      }
    }
  }
  
  private boolean isDeletableResource(File resource)
  {
    if ((resource.getAbsolutePath().startsWith(appBase().getAbsolutePath() + File.separator)) || ((resource.getAbsolutePath().startsWith(configBase().getAbsolutePath())) && (resource.getAbsolutePath().endsWith(".xml")))) {
      return true;
    }
    return false;
  }
  
  public void start()
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("hostConfig.start"));
    }
    try
    {
      ObjectName hostON = this.host.getObjectName();
      this.oname = new ObjectName(hostON.getDomain() + ":type=Deployer,host=" + this.host.getName());
      
      Registry.getRegistry(null, null).registerComponent(this, this.oname, getClass().getName());
    }
    catch (Exception e)
    {
      log.error(sm.getString("hostConfig.jmx.register", new Object[] { this.oname }), e);
    }
    if (this.host.getCreateDirs())
    {
      File[] dirs = { appBase(), configBase() };
      for (int i = 0; i < dirs.length; i++) {
        if ((!dirs[i].mkdirs()) && (!dirs[i].isDirectory())) {
          log.error(sm.getString("hostConfig.createDirs", new Object[] { dirs[i] }));
        }
      }
    }
    if (!appBase().isDirectory())
    {
      log.error(sm.getString("hostConfig.appBase", new Object[] { this.host.getName(), appBase().getPath() }));
      
      this.host.setDeployOnStartup(false);
      this.host.setAutoDeploy(false);
    }
    if (this.host.getDeployOnStartup()) {
      deployApps();
    }
  }
  
  public void stop()
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("hostConfig.stop"));
    }
    if (this.oname != null) {
      try
      {
        Registry.getRegistry(null, null).unregisterComponent(this.oname);
      }
      catch (Exception e)
      {
        log.error(sm.getString("hostConfig.jmx.unregister", new Object[] { this.oname }), e);
      }
    }
    this.oname = null;
  }
  
  protected void check()
  {
    if (this.host.getAutoDeploy())
    {
      DeployedApplication[] apps = (DeployedApplication[])this.deployed.values().toArray(new DeployedApplication[0]);
      for (int i = 0; i < apps.length; i++) {
        if (!isServiced(apps[i].name)) {
          checkResources(apps[i]);
        }
      }
      if (this.host.getUndeployOldVersions()) {
        checkUndeploy();
      }
      deployApps();
    }
  }
  
  public void check(String name)
  {
    DeployedApplication app = (DeployedApplication)this.deployed.get(name);
    if (app != null) {
      checkResources(app);
    }
    deployApps(name);
  }
  
  public synchronized void checkUndeploy()
  {
    SortedSet<String> sortedAppNames = new TreeSet();
    sortedAppNames.addAll(this.deployed.keySet());
    if (sortedAppNames.size() < 2) {
      return;
    }
    Iterator<String> iter = sortedAppNames.iterator();
    
    ContextName previous = new ContextName((String)iter.next(), false);
    do
    {
      ContextName current = new ContextName((String)iter.next(), false);
      if (current.getPath().equals(previous.getPath()))
      {
        Context previousContext = (Context)this.host.findChild(previous.getName());
        
        Context currentContext = (Context)this.host.findChild(current.getName());
        if ((previousContext != null) && (currentContext != null) && (currentContext.getState().isAvailable()) && (!isServiced(previous.getName())))
        {
          Manager manager = previousContext.getManager();
          if (manager != null)
          {
            int sessionCount;
            if ((manager instanceof DistributedManager)) {
              sessionCount = ((DistributedManager)manager).getActiveSessionsFull();
            } else {
              sessionCount = manager.getActiveSessions();
            }
            if (sessionCount == 0)
            {
              if (log.isInfoEnabled()) {
                log.info(sm.getString("hostConfig.undeployVersion", new Object[] { previous.getName() }));
              }
              DeployedApplication app = (DeployedApplication)this.deployed.get(previous.getName());
              
              String[] resources = (String[])app.redeployResources.keySet().toArray(new String[0]);
              
              undeploy(app);
              deleteRedeployResources(app, resources, -1, true);
            }
          }
        }
      }
      previous = current;
    } while (iter.hasNext());
  }
  
  public void manageApp(Context context)
  {
    String contextName = context.getName();
    if (this.deployed.containsKey(contextName)) {
      return;
    }
    DeployedApplication deployedApp = new DeployedApplication(contextName, false);
    
    boolean isWar = false;
    if (context.getDocBase() != null)
    {
      File docBase = new File(context.getDocBase());
      if (!docBase.isAbsolute()) {
        docBase = new File(appBase(), context.getDocBase());
      }
      deployedApp.redeployResources.put(docBase.getAbsolutePath(), Long.valueOf(docBase.lastModified()));
      if (docBase.getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(".war")) {
        isWar = true;
      }
    }
    this.host.addChild(context);
    if ((isWar) && (this.unpackWARs))
    {
      File docBase = new File(appBase(), context.getBaseName());
      deployedApp.redeployResources.put(docBase.getAbsolutePath(), Long.valueOf(docBase.lastModified()));
      
      addWatchedResources(deployedApp, docBase.getAbsolutePath(), context);
    }
    else
    {
      addWatchedResources(deployedApp, null, context);
    }
    this.deployed.put(contextName, deployedApp);
  }
  
  public void unmanageApp(String contextName)
  {
    if (isServiced(contextName))
    {
      this.deployed.remove(contextName);
      this.host.removeChild(this.host.findChild(contextName));
    }
  }
  
  protected static class DeployedApplication
  {
    public String name;
    public final boolean hasDescriptor;
    
    public DeployedApplication(String name, boolean hasDescriptor)
    {
      this.name = name;
      this.hasDescriptor = hasDescriptor;
    }
    
    public LinkedHashMap<String, Long> redeployResources = new LinkedHashMap();
    public HashMap<String, Long> reloadResources = new HashMap();
    public long timestamp = System.currentTimeMillis();
    public boolean loggedDirWarning = false;
  }
  
  private static class DeployDescriptor
    implements Runnable
  {
    private HostConfig config;
    private ContextName cn;
    private File descriptor;
    
    public DeployDescriptor(HostConfig config, ContextName cn, File descriptor)
    {
      this.config = config;
      this.cn = cn;
      this.descriptor = descriptor;
    }
    
    public void run()
    {
      this.config.deployDescriptor(this.cn, this.descriptor);
    }
  }
  
  private static class DeployWar
    implements Runnable
  {
    private HostConfig config;
    private ContextName cn;
    private File war;
    
    public DeployWar(HostConfig config, ContextName cn, File war)
    {
      this.config = config;
      this.cn = cn;
      this.war = war;
    }
    
    public void run()
    {
      this.config.deployWAR(this.cn, this.war);
    }
  }
  
  private static class DeployDirectory
    implements Runnable
  {
    private HostConfig config;
    private ContextName cn;
    private File dir;
    
    public DeployDirectory(HostConfig config, ContextName cn, File dir)
    {
      this.config = config;
      this.cn = cn;
      this.dir = dir;
    }
    
    public void run()
    {
      this.config.deployDirectory(this.cn, this.dir);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\HostConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */