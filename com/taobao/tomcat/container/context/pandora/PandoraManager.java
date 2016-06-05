package com.taobao.tomcat.container.context.pandora;

import com.taobao.tomcat.container.context.loader.AliWebappClassLoader;
import com.taobao.tomcat.util.Constants;
import com.taobao.tomcat.util.IOUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class PandoraManager
  extends LifecycleMBeanBase
{
  private static final Log log = LogFactory.getLog(PandoraManager.class);
  private String name = "Pandora";
  private String base = "deploy";
  private String target = "taobao-hsf.sar";
  private boolean init = true;
  private String containerClassName = "com.taobao.pandora.delegator.PandoraDelegator";
  private Class<?> containerClass = null;
  private String containerVersion = null;
  private SharedClassRepository sharedRepository;
  private StandardContext context;
  
  public PandoraManager() {}
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
    initContainer();
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (!this.init)
    {
      setState(LifecycleState.STARTING);
      return;
    }
    WebappLoader loader = (WebappLoader)this.context.getLoader();
    AliWebappClassLoader classLoader = (AliWebappClassLoader)loader.getClassLoader();
    this.sharedRepository = new SharedClassRepository();
    classLoader.setCommonRepository(this.sharedRepository);
    try
    {
      log.info("Starting pandora container.");
      if (getMainVersion(this.containerVersion) < 3)
      {
        exportV2(classLoader);
      }
      else
      {
        Map<String, Object> paramContext = new HashMap();
        paramContext.put("AppClassLoader", classLoader);
        exportV3(paramContext);
      }
    }
    catch (Exception e)
    {
      throw new PandoraException("Failed to start Pandora container.", e);
    }
    log.info("Pandora container started.");
    
    addRepository(loader, this.context.getRealPath(""));
    classLoader.collectClassInfos();
    setState(LifecycleState.STARTING);
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    if (this.containerClass != null)
    {
      File containerDir = getPandoraLocation();
      log.info("Stopping pandora container: " + containerDir);
      try
      {
        this.containerClass.getMethod("stop", new Class[0]).invoke(null, new Object[0]);
      }
      catch (Exception e)
      {
        throw new LifecycleException("Failed to stop pandora container: " + containerDir, e);
      }
      finally
      {
        this.containerClass = null;
      }
      log.info("Pandora container stopped.");
    }
    setState(LifecycleState.STOPPING);
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    log.info("Destroying pandora container...");
    super.destroyInternal();
    this.containerClass = null;
    this.sharedRepository = null;
    this.context = null;
    log.info("Pandora container destroyed.");
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  protected String getDomainInternal()
  {
    return getName();
  }
  
  protected String getObjectNameKeyProperties()
  {
    return "type=PandoraManager(" + this.context.getName() + ")";
  }
  
  public String getContainerClassName()
  {
    return this.containerClassName;
  }
  
  public void setContainerClassName(String containerClassName)
  {
    if ((containerClassName != null) && (!"".equals(containerClassName))) {
      this.containerClassName = containerClassName;
    }
  }
  
  public String getBase()
  {
    return this.base;
  }
  
  public void setBase(String base)
  {
    this.base = base;
  }
  
  public String getTarget()
  {
    return this.target;
  }
  
  public void setTarget(String target)
  {
    this.target = target;
  }
  
  public boolean isInit()
  {
    return this.init;
  }
  
  public void setInit(boolean init)
  {
    this.init = init;
  }
  
  public ClassRepository getClassRepository()
  {
    return this.sharedRepository;
  }
  
  public StandardContext getContext()
  {
    return this.context;
  }
  
  public void setContext(StandardContext context)
  {
    this.context = context;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("PandoraManager[");
    sb.append(getName());
    sb.append("]");
    return sb.toString();
  }
  
  private void addRepository(Loader loader, String webRoot)
  {
    if (webRoot == null) {
      return;
    }
    File webRootDir = new File(webRoot);
    if ((!webRootDir.exists()) || (!webRootDir.isDirectory())) {
      return;
    }
    if ((loader instanceof WebappLoader)) {
      loader.addRepository(webRootDir.toURI().toString());
    }
  }
  
  private void initContainer()
    throws LifecycleException
  {
    this.init = ((!Boolean.getBoolean("pandora.skip")) && (this.init));
    if (!this.init)
    {
      log.info("Won't bootstrap pandora since it's disabled explicitly");
      return;
    }
    File containerDir = getPandoraLocation();
    if (containerDir == null)
    {
      this.init = false;
      log.info("Couldn't find pandora.sar directory, therefore will not init pandora container");
      return;
    }
    log.info("Initializing pandora container: " + containerDir);
    try
    {
      URLClassLoader containerLoader = getContainerLoader(containerDir);
      
      this.containerVersion = getContainerVersion(containerLoader);
      if (getMainVersion(this.containerVersion) < 3) {
        initPandoraV2(containerLoader, containerDir);
      } else {
        initPandoraV3(containerLoader, containerDir);
      }
    }
    catch (Exception e)
    {
      throw new LifecycleException("Failed to init Pandora container: " + containerDir, e);
    }
    log.info("Pandora container initialized.");
  }
  
  private URLClassLoader getContainerLoader(File containerDir)
    throws MalformedURLException
  {
    ArrayList<URL> list = new ArrayList();
    for (File jarFile : IOUtils.listFiles(new File(containerDir, "lib"), ".jar")) {
      list.add(jarFile.toURI().toURL());
    }
    URL[] urls = (URL[])list.toArray(new URL[list.size()]);
    return new URLClassLoader(urls);
  }
  
  private void initPandoraV2(URLClassLoader containerLoader, File containerDir)
    throws Exception
  {
    this.containerClassName = "com.taobao.hsf.container.HSFContainer";
    this.containerClass = containerLoader.loadClass(this.containerClassName);
    if (this.containerClass == null) {
      throw new ClassNotFoundException(this.containerClassName);
    }
    String[] dirs = { containerDir.getCanonicalPath() };
    this.containerClass.getMethod("start", new Class[] { String[].class }).invoke(null, new Object[] { dirs });
  }
  
  private void initPandoraV3(URLClassLoader containerLoader, File containerDir)
    throws Exception
  {
    this.containerClass = containerLoader.loadClass(this.containerClassName);
    if (this.containerClass == null) {
      throw new ClassNotFoundException(this.containerClassName);
    }
    Method method = this.containerClass.getMethod("init", new Class[] { String[].class });
    String[] dirs = { containerDir.getCanonicalPath() };
    method.invoke(null, new Object[] { dirs });
  }
  
  private String getContainerVersion(URLClassLoader containerLoader)
    throws IOException
  {
    return "2.x";
  }
  
  private int getMainVersion(String ver)
  {
    return Integer.parseInt(ver.substring(0, ver.indexOf('.')));
  }
  
  private void exportV2(ClassLoader appClassLoader)
    throws Exception
  {
    this.containerClass.getMethod("setThirdContainerClassLoader", new Class[] { ClassLoader.class }).invoke(null, new Object[] { appClassLoader });
    
    Method method = this.containerClass.getMethod("getExportedClasses", (Class[])null);
    
    final Map<String, Class<?>> exported = (Map)method.invoke(null, (Object[])null);
    
    Map<String, Class<?>> exportedBundleClasses = null;
    try
    {
      method = this.containerClass.getMethod("getExportedBundleClasses", (Class[])null);
      
      Map<String, Class<?>> tmp = (Map)method.invoke(null, (Object[])null);
      exportedBundleClasses = tmp;
    }
    catch (NoSuchMethodException e)
    {
      log.info(e.getMessage() + " is not found. It's OK for hsf 1.x");
    }
    final Map<String, Class<?>> deferred = exportedBundleClasses == null ? Collections.emptyMap() : exportedBundleClasses;
    
    Map<String, Class<?>> accessor = new Map()
    {
      public int size()
      {
        return exported.size() + deferred.size();
      }
      
      public boolean isEmpty()
      {
        return (exported.isEmpty()) && (deferred.isEmpty());
      }
      
      public boolean containsKey(Object key)
      {
        return (exported.containsKey(key)) || (deferred.containsKey(key));
      }
      
      public boolean containsValue(Object value)
      {
        return (exported.containsValue(value)) || (deferred.containsValue(value));
      }
      
      public Class<?> get(Object key)
      {
        Class<?> value = (Class)exported.get(key);
        if (value == null) {
          value = (Class)deferred.get(key);
        }
        return value;
      }
      
      public Set<String> keySet()
      {
        Set<String> keySet = new HashSet(exported.keySet());
        keySet.addAll(deferred.keySet());
        return keySet;
      }
      
      public Collection<Class<?>> values()
      {
        Collection<Class<?>> values = new ArrayList(exported.values());
        values.addAll(deferred.values());
        return values;
      }
      
      public Set<Map.Entry<String, Class<?>>> entrySet()
      {
        Set<Map.Entry<String, Class<?>>> entrySet = new HashSet(exported.entrySet());
        entrySet.addAll(deferred.entrySet());
        return entrySet;
      }
      
      public Class<?> put(String key, Class<?> value)
      {
        return null;
      }
      
      public Class<?> remove(Object key)
      {
        return null;
      }
      
      //public void putAll(Map<? extends String, ? extends Class<?>> m) {}
      
      public void clear() {}

	@Override
	public Object put(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map m) {
		// TODO Auto-generated method stub
		
	}
    };
    this.sharedRepository.setModuleRepository(accessor);
  }
  
  private void exportV3(Map<String, Object> paramContext)
    throws Exception
  {
    Method method = this.containerClass.getMethod("start", new Class[] { Map.class });
    method.invoke(null, new Object[] { paramContext });
    
    Map<String, Class<?>> classRepository = (Map)paramContext.get("ClassRepository");
    this.sharedRepository.setModuleRepository(classRepository);
  }
  
  private File getPandoraLocation()
  {
    File pandora = getPandoraLocation(System.getProperty("pandora.location"));
    if (pandora != null)
    {
      log.info("found pandora location from system property: " + pandora);
      return pandora;
    }
    File baseDir = getCanonicalPath(getBase());
    String target = getTarget();
    pandora = getPandoraLocation(baseDir, target);
    if (pandora != null) {
      return pandora;
    }
    pandora = getPandoraLocation(baseDir);
    if (pandora != null) {
      return pandora;
    }
    return null;
  }
  
  private File getPandoraLocation(File baseDir, String pandoraFile)
  {
    if ((pandoraFile != null) && (!pandoraFile.trim().isEmpty()))
    {
      File subDir = new File(baseDir, pandoraFile);
      if ((subDir.exists()) && (subDir.isDirectory())) {
        return subDir;
      }
    }
    return null;
  }
  
  private File getPandoraLocation(File baseDir)
  {
    for (String sub : Constants.PANDORA_TARGET_OPTIONS)
    {
      File subDir = new File(baseDir, sub);
      if ((subDir.exists()) && (subDir.isDirectory())) {
        return subDir;
      }
    }
    return null;
  }
  
  private File getPandoraLocation(String pandoraFile)
  {
    if ((pandoraFile == null) || (pandoraFile.trim().isEmpty())) {
      return null;
    }
    File file = new File(pandoraFile);
    if (!file.isAbsolute()) {
      file = new File(System.getProperty("catalina.base"), pandoraFile);
    }
    if (file.isDirectory()) {
      return file;
    }
    return null;
  }
  
  private File getCanonicalPath(String path)
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
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\context\pandora\PandoraManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */