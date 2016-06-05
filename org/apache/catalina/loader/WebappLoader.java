package org.apache.catalina.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.jar.JarFile;
import javax.management.ObjectName;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

public class WebappLoader
  extends LifecycleMBeanBase
  implements Loader, PropertyChangeListener
{
  public WebappLoader()
  {
    this(null);
  }
  
  public WebappLoader(ClassLoader parent)
  {
    this.parentClassLoader = parent;
  }
  
  private static boolean first = true;
  private WebappClassLoader classLoader = null;
  private Container container = null;
  private boolean delegate = false;
  private static final String info = "org.apache.catalina.loader.WebappLoader/1.0";
  private String loaderClass = "org.apache.catalina.loader.WebappClassLoader";
  private ClassLoader parentClassLoader = null;
  private boolean reloadable = false;
  private String[] repositories = new String[0];
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.loader");
  protected PropertyChangeSupport support = new PropertyChangeSupport(this);
  private String classpath = null;
  private ArrayList<String> loaderRepositories = null;
  private boolean searchExternalFirst = false;
  
  public ClassLoader getClassLoader()
  {
    return this.classLoader;
  }
  
  public Container getContainer()
  {
    return this.container;
  }
  
  public void setContainer(Container container)
  {
    if ((this.container != null) && ((this.container instanceof Context))) {
      ((Context)this.container).removePropertyChangeListener(this);
    }
    Container oldContainer = this.container;
    this.container = container;
    this.support.firePropertyChange("container", oldContainer, this.container);
    if ((this.container != null) && ((this.container instanceof Context)))
    {
      setReloadable(((Context)this.container).getReloadable());
      ((Context)this.container).addPropertyChangeListener(this);
    }
  }
  
  public boolean getDelegate()
  {
    return this.delegate;
  }
  
  public void setDelegate(boolean delegate)
  {
    boolean oldDelegate = this.delegate;
    this.delegate = delegate;
    this.support.firePropertyChange("delegate", Boolean.valueOf(oldDelegate), Boolean.valueOf(this.delegate));
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.loader.WebappLoader/1.0";
  }
  
  public String getLoaderClass()
  {
    return this.loaderClass;
  }
  
  public void setLoaderClass(String loaderClass)
  {
    this.loaderClass = loaderClass;
  }
  
  public boolean getReloadable()
  {
    return this.reloadable;
  }
  
  public void setReloadable(boolean reloadable)
  {
    boolean oldReloadable = this.reloadable;
    this.reloadable = reloadable;
    this.support.firePropertyChange("reloadable", Boolean.valueOf(oldReloadable), Boolean.valueOf(this.reloadable));
  }
  
  public boolean getSearchExternalFirst()
  {
    return this.searchExternalFirst;
  }
  
  public void setSearchExternalFirst(boolean searchExternalFirst)
  {
    this.searchExternalFirst = searchExternalFirst;
    if (this.classLoader != null) {
      this.classLoader.setSearchExternalFirst(searchExternalFirst);
    }
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }
  
  public void addRepository(String repository)
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("webappLoader.addRepository", new Object[] { repository }));
    }
    for (int i = 0; i < this.repositories.length; i++) {
      if (repository.equals(this.repositories[i])) {
        return;
      }
    }
    String[] results = new String[this.repositories.length + 1];
    for (int i = 0; i < this.repositories.length; i++) {
      results[i] = this.repositories[i];
    }
    results[this.repositories.length] = repository;
    this.repositories = results;
    if ((getState().isAvailable()) && (this.classLoader != null))
    {
      this.classLoader.addRepository(repository);
      if (this.loaderRepositories != null) {
        this.loaderRepositories.add(repository);
      }
      setClassPath();
    }
  }
  
  public void backgroundProcess()
  {
    if ((this.reloadable) && (modified())) {
      try
      {
        Thread.currentThread().setContextClassLoader(WebappLoader.class.getClassLoader());
        if ((this.container instanceof StandardContext)) {
          ((StandardContext)this.container).reload();
        }
      }
      finally
      {
        if (this.container.getLoader() != null) {
          Thread.currentThread().setContextClassLoader(this.container.getLoader().getClassLoader());
        }
      }
    } else {
      closeJARs(false);
    }
  }
  
  public String[] findRepositories()
  {
    return (String[])this.repositories.clone();
  }
  
  public String[] getRepositories()
  {
    return (String[])this.repositories.clone();
  }
  
  public String getRepositoriesString()
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.repositories.length; i++) {
      sb.append(this.repositories[i]).append(":");
    }
    return sb.toString();
  }
  
  public String[] getLoaderRepositories()
  {
    if (this.loaderRepositories == null) {
      return null;
    }
    String[] res = new String[this.loaderRepositories.size()];
    this.loaderRepositories.toArray(res);
    return res;
  }
  
  public String getLoaderRepositoriesString()
  {
    String[] repositories = getLoaderRepositories();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < repositories.length; i++) {
      sb.append(repositories[i]).append(":");
    }
    return sb.toString();
  }
  
  public String getClasspath()
  {
    return this.classpath;
  }
  
  public boolean modified()
  {
    return this.classLoader != null ? this.classLoader.modified() : false;
  }
  
  public void closeJARs(boolean force)
  {
    if (this.classLoader != null) {
      this.classLoader.closeJARs(force);
    }
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("WebappLoader[");
    if (this.container != null) {
      sb.append(this.container.getName());
    }
    sb.append("]");
    return sb.toString();
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("webappLoader.starting"));
    }
    if (this.container.getResources() == null)
    {
      log.info("No resources for " + this.container);
      setState(LifecycleState.STARTING);
      return;
    }
    URLStreamHandlerFactory streamHandlerFactory = DirContextURLStreamHandlerFactory.getInstance();
    if (first)
    {
      first = false;
      try
      {
        URL.setURLStreamHandlerFactory(streamHandlerFactory);
      }
      catch (Exception e)
      {
        log.error("Error registering jndi stream handler", e);
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        
        log.info("Dual registration of jndi stream handler: " + t.getMessage());
      }
    }
    try
    {
      this.classLoader = createClassLoader();
      this.classLoader.setResources(this.container.getResources());
      this.classLoader.setDelegate(this.delegate);
      this.classLoader.setSearchExternalFirst(this.searchExternalFirst);
      if ((this.container instanceof StandardContext))
      {
        this.classLoader.setAntiJARLocking(((StandardContext)this.container).getAntiJARLocking());
        
        this.classLoader.setClearReferencesStatic(((StandardContext)this.container).getClearReferencesStatic());
        
        this.classLoader.setClearReferencesStopThreads(((StandardContext)this.container).getClearReferencesStopThreads());
        
        this.classLoader.setClearReferencesStopTimerThreads(((StandardContext)this.container).getClearReferencesStopTimerThreads());
        
        this.classLoader.setClearReferencesHttpClientKeepAliveThread(((StandardContext)this.container).getClearReferencesHttpClientKeepAliveThread());
      }
      for (int i = 0; i < this.repositories.length; i++) {
        this.classLoader.addRepository(this.repositories[i]);
      }
      setRepositories();
      setClassPath();
      
      setPermissions();
      
      this.classLoader.start();
      
      DirContextURLStreamHandler.bind(this.classLoader, this.container.getResources());
      
      StandardContext ctx = (StandardContext)this.container;
      String contextName = ctx.getName();
      if (!contextName.startsWith("/")) {
        contextName = "/" + contextName;
      }
      ObjectName cloname = new ObjectName(MBeanUtils.getDomain(ctx) + ":type=WebappClassLoader,context=" + contextName + ",host=" + ctx.getParent().getName());
      
      Registry.getRegistry(null, null).registerComponent(this.classLoader, cloname, null);
    }
    catch (Throwable t)
    {
      t = ExceptionUtils.unwrapInvocationTargetException(t);
      ExceptionUtils.handleThrowable(t);
      log.error("LifecycleException ", t);
      throw new LifecycleException("start: ", t);
    }
    setState(LifecycleState.STARTING);
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("webappLoader.stopping"));
    }
    setState(LifecycleState.STOPPING);
    if ((this.container instanceof Context))
    {
      ServletContext servletContext = ((Context)this.container).getServletContext();
      
      servletContext.removeAttribute("org.apache.catalina.jsp_classpath");
    }
    if (this.classLoader != null)
    {
      this.classLoader.stop();
      DirContextURLStreamHandler.unbind(this.classLoader);
    }
    try
    {
      StandardContext ctx = (StandardContext)this.container;
      String contextName = ctx.getName();
      if (!contextName.startsWith("/")) {
        contextName = "/" + contextName;
      }
      ObjectName cloname = new ObjectName(MBeanUtils.getDomain(ctx) + ":type=WebappClassLoader,context=" + contextName + ",host=" + ctx.getParent().getName());
      
      Registry.getRegistry(null, null).unregisterComponent(cloname);
    }
    catch (Exception e)
    {
      log.error("LifecycleException ", e);
    }
    this.classLoader = null;
  }
  
  public void propertyChange(PropertyChangeEvent event)
  {
    if (!(event.getSource() instanceof Context)) {
      return;
    }
    if (event.getPropertyName().equals("reloadable")) {
      try
      {
        setReloadable(((Boolean)event.getNewValue()).booleanValue());
      }
      catch (NumberFormatException e)
      {
        log.error(sm.getString("webappLoader.reloadable", new Object[] { event.getNewValue().toString() }));
      }
    }
  }
  
  private WebappClassLoader createClassLoader()
    throws Exception
  {
    Class<?> clazz = Class.forName(this.loaderClass);
    WebappClassLoader classLoader = null;
    if (this.parentClassLoader == null) {
      this.parentClassLoader = this.container.getParentClassLoader();
    }
    Class<?>[] argTypes = { ClassLoader.class };
    Object[] args = { this.parentClassLoader };
    Constructor<?> constr = clazz.getConstructor(argTypes);
    classLoader = (WebappClassLoader)constr.newInstance(args);
    
    return classLoader;
  }
  
  private void setPermissions()
  {
    if (!Globals.IS_SECURITY_ENABLED) {
      return;
    }
    if (!(this.container instanceof Context)) {
      return;
    }
    ServletContext servletContext = ((Context)this.container).getServletContext();
    
    File workDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
    if (workDir != null) {
      try
      {
        String workDirPath = workDir.getCanonicalPath();
        this.classLoader.addPermission(new FilePermission(workDirPath, "read,write"));
        
        this.classLoader.addPermission(new FilePermission(workDirPath + File.separator + "-", "read,write,delete"));
      }
      catch (IOException e) {}
    }
    try
    {
      URL rootURL = servletContext.getResource("/");
      this.classLoader.addPermission(rootURL);
      
      String contextRoot = servletContext.getRealPath("/");
      if (contextRoot != null) {
        try
        {
          contextRoot = new File(contextRoot).getCanonicalPath();
          this.classLoader.addPermission(contextRoot);
        }
        catch (IOException e) {}
      }
      URL classesURL = servletContext.getResource("/WEB-INF/classes/");
      this.classLoader.addPermission(classesURL);
      URL libURL = servletContext.getResource("/WEB-INF/lib/");
      this.classLoader.addPermission(libURL);
      if (contextRoot != null)
      {
        if (libURL != null)
        {
          File rootDir = new File(contextRoot);
          File libDir = new File(rootDir, "WEB-INF/lib/");
          try
          {
            String path = libDir.getCanonicalPath();
            this.classLoader.addPermission(path);
          }
          catch (IOException e) {}
        }
      }
      else if (workDir != null)
      {
        if (libURL != null)
        {
          File libDir = new File(workDir, "WEB-INF/lib/");
          try
          {
            String path = libDir.getCanonicalPath();
            this.classLoader.addPermission(path);
          }
          catch (IOException e) {}
        }
        if (classesURL != null)
        {
          File classesDir = new File(workDir, "WEB-INF/classes/");
          try
          {
            String path = classesDir.getCanonicalPath();
            this.classLoader.addPermission(path);
          }
          catch (IOException e) {}
        }
      }
    }
    catch (MalformedURLException e) {}
  }
  
  private void setRepositories()
    throws IOException
  {
    if (!(this.container instanceof Context)) {
      return;
    }
    ServletContext servletContext = ((Context)this.container).getServletContext();
    if (servletContext == null) {
      return;
    }
    this.loaderRepositories = new ArrayList();
    
    File workDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
    if (workDir == null) {
      log.info("No work dir for " + servletContext);
    }
    if ((log.isDebugEnabled()) && (workDir != null)) {
      log.debug(sm.getString("webappLoader.deploy", new Object[] { workDir.getAbsolutePath() }));
    }
    this.classLoader.setWorkDir(workDir);
    
    DirContext resources = this.container.getResources();
    
    String classesPath = "/WEB-INF/classes";
    DirContext classes = null;
    try
    {
      Object object = resources.lookup(classesPath);
      if ((object instanceof DirContext)) {
        classes = (DirContext)object;
      }
    }
    catch (NamingException e) {}
    if (classes != null)
    {
      File classRepository = null;
      
      String absoluteClassesPath = servletContext.getRealPath(classesPath);
      if (absoluteClassesPath != null)
      {
        classRepository = new File(absoluteClassesPath);
      }
      else
      {
        classRepository = new File(workDir, classesPath);
        if ((!classRepository.mkdirs()) && (!classRepository.isDirectory())) {
          throw new IOException(sm.getString("webappLoader.mkdirFailure"));
        }
        if (!copyDir(classes, classRepository)) {
          throw new IOException(sm.getString("webappLoader.copyFailure"));
        }
      }
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("webappLoader.classDeploy", new Object[] { classesPath, classRepository.getAbsolutePath() }));
      }
      this.classLoader.addRepository(classesPath + "/", classRepository);
      this.loaderRepositories.add(classesPath + "/");
    }
    String libPath = "/WEB-INF/lib";
    
    this.classLoader.setJarPath(libPath);
    
    DirContext libDir = null;
    try
    {
      Object object = resources.lookup(libPath);
      if ((object instanceof DirContext)) {
        libDir = (DirContext)object;
      }
    }
    catch (NamingException e) {}
    if (libDir != null)
    {
      boolean copyJars = false;
      String absoluteLibPath = servletContext.getRealPath(libPath);
      
      File destDir = null;
      if (absoluteLibPath != null)
      {
        destDir = new File(absoluteLibPath);
      }
      else
      {
        copyJars = true;
        destDir = new File(workDir, libPath);
        if ((!destDir.mkdirs()) && (!destDir.isDirectory())) {
          throw new IOException(sm.getString("webappLoader.mkdirFailure"));
        }
      }
      NamingEnumeration<NameClassPair> enumeration = null;
      try
      {
        enumeration = libDir.list("");
      }
      catch (NamingException e)
      {
        IOException ioe = new IOException(sm.getString("webappLoader.namingFailure", new Object[] { libPath }));
        
        ioe.initCause(e);
        throw ioe;
      }
      while (enumeration.hasMoreElements())
      {
        NameClassPair ncPair = (NameClassPair)enumeration.nextElement();
        String filename = libPath + "/" + ncPair.getName();
        if (filename.endsWith(".jar"))
        {
          File destFile = new File(destDir, ncPair.getName());
          if (log.isDebugEnabled()) {
            log.debug(sm.getString("webappLoader.jarDeploy", new Object[] { filename, destFile.getAbsolutePath() }));
          }
          Object obj = null;
          try
          {
            obj = libDir.lookup(ncPair.getName());
          }
          catch (NamingException e)
          {
            IOException ioe = new IOException(sm.getString("webappLoader.namingFailure", new Object[] { filename }));
            
            ioe.initCause(e);
            throw ioe;
          }
          if ((obj instanceof Resource))
          {
            Resource jarResource = (Resource)obj;
            if ((copyJars) && 
              (!copy(jarResource.streamContent(), new FileOutputStream(destFile)))) {
              throw new IOException(sm.getString("webappLoader.copyFailure"));
            }
            try
            {
              JarFile jarFile = new JarFile(destFile);
              this.classLoader.addJar(filename, jarFile, destFile);
            }
            catch (Exception ex) {}
            this.loaderRepositories.add(filename);
          }
        }
      }
    }
  }
  
  private void setClassPath()
  {
    if (!(this.container instanceof Context)) {
      return;
    }
    ServletContext servletContext = ((Context)this.container).getServletContext();
    if (servletContext == null) {
      return;
    }
    if ((this.container instanceof StandardContext))
    {
      String baseClasspath = ((StandardContext)this.container).getCompilerClasspath();
      if (baseClasspath != null)
      {
        servletContext.setAttribute("org.apache.catalina.jsp_classpath", baseClasspath);
        
        return;
      }
    }
    StringBuilder classpath = new StringBuilder();
    
    ClassLoader loader = getClassLoader();
    if ((this.delegate) && (loader != null)) {
      loader = loader.getParent();
    }
    while ((loader != null) && 
      (buildClassPath(servletContext, classpath, loader))) {
      loader = loader.getParent();
    }
    if (this.delegate)
    {
      loader = getClassLoader();
      if (loader != null) {
        buildClassPath(servletContext, classpath, loader);
      }
    }
    this.classpath = classpath.toString();
    
    servletContext.setAttribute("org.apache.catalina.jsp_classpath", classpath.toString());
  }
  
  private boolean buildClassPath(ServletContext servletContext, StringBuilder classpath, ClassLoader loader)
  {
    if ((loader instanceof URLClassLoader))
    {
      URL[] repositories = ((URLClassLoader)loader).getURLs();
      for (int i = 0; i < repositories.length; i++)
      {
        String repository = repositories[i].toString();
        if (repository.startsWith("file://"))
        {
          repository = utf8Decode(repository.substring(7));
        }
        else if (repository.startsWith("file:"))
        {
          repository = utf8Decode(repository.substring(5));
        }
        else
        {
          if (!repository.startsWith("jndi:")) {
            continue;
          }
          repository = servletContext.getRealPath(repository.substring(5));
        }
        if (repository != null)
        {
          if (classpath.length() > 0) {
            classpath.append(File.pathSeparator);
          }
          classpath.append(repository);
        }
      }
    }
    else
    {
      String cp = getClasspath(loader);
      if (cp == null)
      {
        log.info("Unknown loader " + loader + " " + loader.getClass());
      }
      else
      {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(cp);
      }
      return false;
    }
    return true;
  }
  
  private String utf8Decode(String input)
  {
    String result = null;
    try
    {
      result = URLDecoder.decode(input, "UTF-8");
    }
    catch (UnsupportedEncodingException uee) {}
    return result;
  }
  
  private String getClasspath(ClassLoader loader)
  {
    try
    {
      Method m = loader.getClass().getMethod("getClasspath", new Class[0]);
      if (log.isTraceEnabled()) {
        log.trace("getClasspath " + m);
      }
      if (m == null) {
        return null;
      }
      Object o = m.invoke(loader, new Object[0]);
      if (log.isDebugEnabled()) {
        log.debug("gotClasspath " + o);
      }
      if ((o instanceof String)) {
        return (String)o;
      }
      return null;
    }
    catch (Exception ex)
    {
      Throwable t = ExceptionUtils.unwrapInvocationTargetException(ex);
      ExceptionUtils.handleThrowable(t);
      if (log.isDebugEnabled()) {
        log.debug("getClasspath ", ex);
      }
    }
    return null;
  }
  
  private boolean copyDir(DirContext srcDir, File destDir)
  {
    try
    {
      NamingEnumeration<NameClassPair> enumeration = srcDir.list("");
      while (enumeration.hasMoreElements())
      {
        NameClassPair ncPair = (NameClassPair)enumeration.nextElement();
        String name = ncPair.getName();
        Object object = srcDir.lookup(name);
        File currentFile = new File(destDir, name);
        if ((object instanceof Resource))
        {
          InputStream is = ((Resource)object).streamContent();
          OutputStream os = new FileOutputStream(currentFile);
          if (!copy(is, os)) {
            return false;
          }
        }
        else if ((object instanceof InputStream))
        {
          OutputStream os = new FileOutputStream(currentFile);
          if (!copy((InputStream)object, os)) {
            return false;
          }
        }
        else if ((object instanceof DirContext))
        {
          if ((!currentFile.isDirectory()) && (!currentFile.mkdir())) {
            return false;
          }
          if (!copyDir((DirContext)object, currentFile)) {
            return false;
          }
        }
      }
    }
    catch (NamingException e)
    {
      return false;
    }
    catch (IOException e)
    {
      return false;
    }
    return true;
  }
  
  private boolean copy(InputStream is, OutputStream os)
  {
    try
    {
      byte[] buf = new byte['က'];
      for (;;)
      {
        int len = is.read(buf);
        if (len < 0) {
          break;
        }
        os.write(buf, 0, len);
      }
      is.close();
      os.close();
    }
    catch (IOException e)
    {
      return false;
    }
    return true;
  }
  
  private static final Log log = LogFactory.getLog(WebappLoader.class);
  
  protected String getDomainInternal()
  {
    return MBeanUtils.getDomain(this.container);
  }
  
  protected String getObjectNameKeyProperties()
  {
    StringBuilder name = new StringBuilder("type=Loader");
    if ((this.container instanceof Context))
    {
      name.append(",context=");
      Context context = (Context)this.container;
      
      String contextName = context.getName();
      if (!contextName.startsWith("/")) {
        name.append("/");
      }
      name.append(contextName);
      
      name.append(",host=");
      name.append(context.getParent().getName());
    }
    else
    {
      name.append(",container=");
      name.append(this.container.getName());
    }
    return name.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\WebappLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */