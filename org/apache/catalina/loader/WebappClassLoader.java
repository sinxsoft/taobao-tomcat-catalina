package org.apache.catalina.loader;

import java.beans.Introspector;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.JndiPermission;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.res.StringManager;

public class WebappClassLoader
  extends URLClassLoader
  implements Lifecycle
{
  private static final Log log = LogFactory.getLog(WebappClassLoader.class);
  private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
  private static final List<String> JVM_THREAD_GROUP_NAMES = new ArrayList();
  private static final String JVM_THREAD_GROUP_SYSTEM = "system";
  private static final String SERVICES_PREFIX = "META-INF/services/";
  
  static
  {
    JVM_THREAD_GROUP_NAMES.add("system");
    JVM_THREAD_GROUP_NAMES.add("RMI Runtime");
  }
  
  protected class PrivilegedFindResourceByName
    implements PrivilegedAction<ResourceEntry>
  {
    protected String name;
    protected String path;
    
    PrivilegedFindResourceByName(String name, String path)
    {
      this.name = name;
      this.path = path;
    }
    
    public ResourceEntry run()
    {
      return WebappClassLoader.this.findResourceInternal(this.name, this.path);
    }
  }
  
  protected static final class PrivilegedGetClassLoader
    implements PrivilegedAction<ClassLoader>
  {
    public Class<?> clazz;
    
    public PrivilegedGetClassLoader(Class<?> clazz)
    {
      this.clazz = clazz;
    }
    
    public ClassLoader run()
    {
      return this.clazz.getClassLoader();
    }
  }
  
  protected static final String[] triggers = { "javax.servlet.Servlet", "javax.el.Expression" };
  protected static final String[] packageTriggers = new String[0];
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.loader");
  boolean antiJARLocking = false;
  
  public WebappClassLoader()
  {
    super(new URL[0]);
    
    ClassLoader p = getParent();
    if (p == null) {
      p = getSystemClassLoader();
    }
    this.parent = p;
    
    ClassLoader j = String.class.getClassLoader();
    if (j == null)
    {
      j = getSystemClassLoader();
      while (j.getParent() != null) {
        j = j.getParent();
      }
    }
    this.j2seClassLoader = j;
    
    this.securityManager = System.getSecurityManager();
    if (this.securityManager != null) {
      refreshPolicy();
    }
  }
  
  public WebappClassLoader(ClassLoader parent)
  {
    super(new URL[0], parent);
    
    ClassLoader p = getParent();
    if (p == null) {
      p = getSystemClassLoader();
    }
    this.parent = p;
    
    ClassLoader j = String.class.getClassLoader();
    if (j == null)
    {
      j = getSystemClassLoader();
      while (j.getParent() != null) {
        j = j.getParent();
      }
    }
    this.j2seClassLoader = j;
    
    this.securityManager = System.getSecurityManager();
    if (this.securityManager != null) {
      refreshPolicy();
    }
  }
  
  protected DirContext resources = null;
  protected HashMap<String, ResourceEntry> resourceEntries = new HashMap();
  protected HashMap<String, String> notFoundResources = new LinkedHashMap<String, String>()
  {
    private static final long serialVersionUID = 1L;
    
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
    {
      return size() > 1000;
    }
  };
  protected boolean delegate = false;
  protected long lastJarAccessed = 0L;
  protected String[] repositories = new String[0];
  protected URL[] repositoryURLs = null;
  protected File[] files = new File[0];
  protected JarFile[] jarFiles = new JarFile[0];
  protected File[] jarRealFiles = new File[0];
  protected String jarPath = null;
  protected String[] jarNames = new String[0];
  protected long[] lastModifiedDates = new long[0];
  protected String[] paths = new String[0];
  protected ArrayList<Permission> permissionList = new ArrayList();
  protected File loaderDir = null;
  protected String canonicalLoaderDir = null;
  protected HashMap<String, PermissionCollection> loaderPC = new HashMap();
  protected SecurityManager securityManager = null;
  protected ClassLoader parent = null;
  protected ClassLoader system = null;
  protected ClassLoader j2seClassLoader;
  protected boolean started = false;
  protected boolean hasExternalRepositories = false;
  protected boolean searchExternalFirst = false;
  protected boolean needConvert = false;
  protected Permission allPermission = new AllPermission();
  private boolean clearReferencesStatic = false;
  private boolean clearReferencesStopThreads = false;
  private boolean clearReferencesStopTimerThreads = false;
  private boolean clearReferencesLogFactoryRelease = true;
  private boolean clearReferencesHttpClientKeepAliveThread = true;
  private String contextName = "unknown";
  
  public DirContext getResources()
  {
    return this.resources;
  }
  
  public void setResources(DirContext resources)
  {
    this.resources = resources;
    if ((resources instanceof ProxyDirContext)) {
      this.contextName = ((ProxyDirContext)resources).getContextName();
    }
  }
  
  public String getContextName()
  {
    return this.contextName;
  }
  
  public boolean getDelegate()
  {
    return this.delegate;
  }
  
  public void setDelegate(boolean delegate)
  {
    this.delegate = delegate;
  }
  
  public boolean getAntiJARLocking()
  {
    return this.antiJARLocking;
  }
  
  public void setAntiJARLocking(boolean antiJARLocking)
  {
    this.antiJARLocking = antiJARLocking;
  }
  
  public boolean getSearchExternalFirst()
  {
    return this.searchExternalFirst;
  }
  
  public void setSearchExternalFirst(boolean searchExternalFirst)
  {
    this.searchExternalFirst = searchExternalFirst;
  }
  
  public void addPermission(String filepath)
  {
    if (filepath == null) {
      return;
    }
    String path = filepath;
    if (this.securityManager != null)
    {
      Permission permission = null;
      if ((path.startsWith("jndi:")) || (path.startsWith("jar:jndi:")))
      {
        if (!path.endsWith("/")) {
          path = path + "/";
        }
        permission = new JndiPermission(path + "*");
        addPermission(permission);
      }
      else
      {
        if (!path.endsWith(File.separator))
        {
          permission = new FilePermission(path, "read");
          addPermission(permission);
          path = path + File.separator;
        }
        permission = new FilePermission(path + "-", "read");
        addPermission(permission);
      }
    }
  }
  
  public void addPermission(URL url)
  {
    if (url != null) {
      addPermission(url.toString());
    }
  }
  
  public void addPermission(Permission permission)
  {
    if ((this.securityManager != null) && (permission != null)) {
      this.permissionList.add(permission);
    }
  }
  
  public String getJarPath()
  {
    return this.jarPath;
  }
  
  public void setJarPath(String jarPath)
  {
    this.jarPath = jarPath;
  }
  
  public void setWorkDir(File workDir)
  {
    this.loaderDir = new File(workDir, "loader");
    if (this.loaderDir == null) {
      this.canonicalLoaderDir = null;
    } else {
      try
      {
        this.canonicalLoaderDir = this.loaderDir.getCanonicalPath();
        if (!this.canonicalLoaderDir.endsWith(File.separator)) {
          this.canonicalLoaderDir += File.separator;
        }
      }
      catch (IOException ioe)
      {
        this.canonicalLoaderDir = null;
      }
    }
  }
  
  @Deprecated
  protected void setParentClassLoader(ClassLoader pcl)
  {
    this.parent = pcl;
  }
  
  public boolean getClearReferencesStatic()
  {
    return this.clearReferencesStatic;
  }
  
  public void setClearReferencesStatic(boolean clearReferencesStatic)
  {
    this.clearReferencesStatic = clearReferencesStatic;
  }
  
  public boolean getClearReferencesStopThreads()
  {
    return this.clearReferencesStopThreads;
  }
  
  public void setClearReferencesStopThreads(boolean clearReferencesStopThreads)
  {
    this.clearReferencesStopThreads = clearReferencesStopThreads;
  }
  
  public boolean getClearReferencesStopTimerThreads()
  {
    return this.clearReferencesStopTimerThreads;
  }
  
  public void setClearReferencesStopTimerThreads(boolean clearReferencesStopTimerThreads)
  {
    this.clearReferencesStopTimerThreads = clearReferencesStopTimerThreads;
  }
  
  public boolean getClearReferencesLogFactoryRelease()
  {
    return this.clearReferencesLogFactoryRelease;
  }
  
  public void setClearReferencesLogFactoryRelease(boolean clearReferencesLogFactoryRelease)
  {
    this.clearReferencesLogFactoryRelease = clearReferencesLogFactoryRelease;
  }
  
  public boolean getClearReferencesHttpClientKeepAliveThread()
  {
    return this.clearReferencesHttpClientKeepAliveThread;
  }
  
  public void setClearReferencesHttpClientKeepAliveThread(boolean clearReferencesHttpClientKeepAliveThread)
  {
    this.clearReferencesHttpClientKeepAliveThread = clearReferencesHttpClientKeepAliveThread;
  }
  
  public void addRepository(String repository)
  {
    if ((repository.startsWith("/WEB-INF/lib")) || (repository.startsWith("/WEB-INF/classes"))) {
      return;
    }
    try
    {
      URL url = new URL(repository);
      super.addURL(url);
      this.hasExternalRepositories = true;
      this.repositoryURLs = null;
    }
    catch (MalformedURLException e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Invalid repository: " + repository);
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  synchronized void addRepository(String repository, File file)
  {
    if (repository == null) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("addRepository(" + repository + ")");
    }
    String[] result = new String[this.repositories.length + 1];
    for (int i = 0; i < this.repositories.length; i++) {
      result[i] = this.repositories[i];
    }
    result[this.repositories.length] = repository;
    this.repositories = result;
    
    File[] result2 = new File[this.files.length + 1];
    int i;
    for (i = 0; i < this.files.length; i++) {
      result2[i] = this.files[i];
    }
    result2[this.files.length] = file;
    this.files = result2;
  }
  
  synchronized void addJar(String jar, JarFile jarFile, File file)
    throws IOException
  {
    if (jar == null) {
      return;
    }
    if (jarFile == null) {
      return;
    }
    if (file == null) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("addJar(" + jar + ")");
    }
    if ((this.jarPath != null) && (jar.startsWith(this.jarPath)))
    {
      String jarName = jar.substring(this.jarPath.length());
      while (jarName.startsWith("/")) {
        jarName = jarName.substring(1);
      }
      String[] result = new String[this.jarNames.length + 1];
      for (int i = 0; i < this.jarNames.length; i++) {
        result[i] = this.jarNames[i];
      }
      result[this.jarNames.length] = jarName;
      this.jarNames = result;
    }
    try
    {
      long lastModified = ((ResourceAttributes)this.resources.getAttributes(jar)).getLastModified();
      
      String[] result = new String[this.paths.length + 1];
      int i;
      for (i = 0; i < this.paths.length; i++) {
        result[i] = this.paths[i];
      }
      result[this.paths.length] = jar;
      this.paths = result;
      
      long[] result3 = new long[this.lastModifiedDates.length + 1];
      for (i = 0; i < this.lastModifiedDates.length; i++) {
        result3[i] = this.lastModifiedDates[i];
      }
      result3[this.lastModifiedDates.length] = lastModified;
      this.lastModifiedDates = result3;
    }
    catch (NamingException e) {}
    if (!validateJarFile(file)) {
      return;
    }
    JarFile[] result2 = new JarFile[this.jarFiles.length + 1];
    for (int i = 0; i < this.jarFiles.length; i++) {
      result2[i] = this.jarFiles[i];
    }
    result2[this.jarFiles.length] = jarFile;
    this.jarFiles = result2;
    
    File[] result4 = new File[this.jarRealFiles.length + 1];
    int i;
    for (i = 0; i < this.jarRealFiles.length; i++) {
      result4[i] = this.jarRealFiles[i];
    }
    result4[this.jarRealFiles.length] = file;
    this.jarRealFiles = result4;
  }
  
  public String[] findRepositories()
  {
    return (String[])this.repositories.clone();
  }
  
  public boolean modified()
  {
    if (log.isDebugEnabled()) {
      log.debug("modified()");
    }
    int length = this.paths.length;
    
    int length2 = this.lastModifiedDates.length;
    if (length > length2) {
      length = length2;
    }
    for (int i = 0; i < length; i++) {
      try
      {
        long lastModified = ((ResourceAttributes)this.resources.getAttributes(this.paths[i])).getLastModified();
        if (lastModified != this.lastModifiedDates[i])
        {
          if (log.isDebugEnabled()) {
            log.debug("  Resource '" + this.paths[i] + "' was modified; Date is now: " + new Date(lastModified) + " Was: " + new Date(this.lastModifiedDates[i]));
          }
          return true;
        }
      }
      catch (NamingException e)
      {
        log.error("    Resource '" + this.paths[i] + "' is missing");
        return true;
      }
    }
    length = this.jarNames.length;
    if (getJarPath() != null) {
      try
      {
        NamingEnumeration<Binding> enumeration = this.resources.listBindings(getJarPath());
        
        int i = 0;
        while ((enumeration.hasMoreElements()) && (i < length))
        {
          NameClassPair ncPair = (NameClassPair)enumeration.nextElement();
          String name = ncPair.getName();
          if (name.endsWith(".jar"))
          {
            if (!name.equals(this.jarNames[i]))
            {
              log.info("    Additional JARs have been added : '" + name + "'");
              
              return true;
            }
            i++;
          }
        }
        if (enumeration.hasMoreElements()) {
          while (enumeration.hasMoreElements())
          {
            NameClassPair ncPair = (NameClassPair)enumeration.nextElement();
            String name = ncPair.getName();
            if (name.endsWith(".jar"))
            {
              log.info("    Additional JARs have been added");
              return true;
            }
          }
        }
        if (i < this.jarNames.length)
        {
          log.info("    Additional JARs have been added");
          return true;
        }
      }
      catch (NamingException e)
      {
        if (log.isDebugEnabled()) {
          log.debug("    Failed tracking modifications of '" + getJarPath() + "'");
        }
      }
      catch (ClassCastException e)
      {
        log.error("    Failed tracking modifications of '" + getJarPath() + "' : " + e.getMessage());
      }
    }
    return false;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("WebappClassLoader\r\n");
    sb.append("  context: ");
    sb.append(this.contextName);
    sb.append("\r\n");
    sb.append("  delegate: ");
    sb.append(this.delegate);
    sb.append("\r\n");
    sb.append("  repositories:\r\n");
    if (this.repositories != null) {
      for (int i = 0; i < this.repositories.length; i++)
      {
        sb.append("    ");
        sb.append(this.repositories[i]);
        sb.append("\r\n");
      }
    }
    if (this.parent != null)
    {
      sb.append("----------> Parent Classloader:\r\n");
      sb.append(this.parent.toString());
      sb.append("\r\n");
    }
    return sb.toString();
  }
  
  protected void addURL(URL url)
  {
    super.addURL(url);
    this.hasExternalRepositories = true;
    this.repositoryURLs = null;
  }
  
  protected final Class<?> doDefineClass(String name, byte[] b, int off, int len, ProtectionDomain protectionDomain)
  {
    return super.defineClass(name, b, off, len, protectionDomain);
  }
  
  public Class<?> findClass(String name)
    throws ClassNotFoundException
  {
    if (log.isDebugEnabled()) {
      log.debug("    findClass(" + name + ")");
    }
    if (!this.started) {
      throw new ClassNotFoundException(name);
    }
    if (this.securityManager != null)
    {
      int i = name.lastIndexOf('.');
      if (i >= 0) {
        try
        {
          if (log.isTraceEnabled()) {
            log.trace("      securityManager.checkPackageDefinition");
          }
          this.securityManager.checkPackageDefinition(name.substring(0, i));
        }
        catch (Exception se)
        {
          if (log.isTraceEnabled()) {
            log.trace("      -->Exception-->ClassNotFoundException", se);
          }
          throw new ClassNotFoundException(name, se);
        }
      }
    }
    Class<?> clazz = null;
    try
    {
      if (log.isTraceEnabled()) {
        log.trace("      findClassInternal(" + name + ")");
      }
      if ((this.hasExternalRepositories) && (this.searchExternalFirst)) {
        try
        {
          clazz = super.findClass(name);
        }
        catch (ClassNotFoundException cnfe) {}catch (AccessControlException ace)
        {
          log.warn("WebappClassLoader.findClassInternal(" + name + ") security exception: " + ace.getMessage(), ace);
          
          throw new ClassNotFoundException(name, ace);
        }
        catch (RuntimeException e)
        {
          if (log.isTraceEnabled()) {
            log.trace("      -->RuntimeException Rethrown", e);
          }
          throw e;
        }
      }
      if (clazz == null) {
        try
        {
          clazz = findClassInternal(name);
        }
        catch (ClassNotFoundException cnfe)
        {
          if ((!this.hasExternalRepositories) || (this.searchExternalFirst)) {
            throw cnfe;
          }
        }
        catch (AccessControlException ace)
        {
          log.warn("WebappClassLoader.findClassInternal(" + name + ") security exception: " + ace.getMessage(), ace);
          
          throw new ClassNotFoundException(name, ace);
        }
        catch (RuntimeException e)
        {
          if (log.isTraceEnabled()) {
            log.trace("      -->RuntimeException Rethrown", e);
          }
          throw e;
        }
      }
      if ((clazz == null) && (this.hasExternalRepositories) && (!this.searchExternalFirst)) {
        try
        {
          clazz = super.findClass(name);
        }
        catch (AccessControlException ace)
        {
          log.warn("WebappClassLoader.findClassInternal(" + name + ") security exception: " + ace.getMessage(), ace);
          
          throw new ClassNotFoundException(name, ace);
        }
        catch (RuntimeException e)
        {
          if (log.isTraceEnabled()) {
            log.trace("      -->RuntimeException Rethrown", e);
          }
          throw e;
        }
      }
      if (clazz == null)
      {
        if (log.isDebugEnabled()) {
          log.debug("    --> Returning ClassNotFoundException");
        }
        throw new ClassNotFoundException(name);
      }
    }
    catch (ClassNotFoundException e)
    {
      if (log.isTraceEnabled()) {
        log.trace("    --> Passing on ClassNotFoundException");
      }
      throw e;
    }
    if (log.isTraceEnabled()) {
      log.debug("      Returning class " + clazz);
    }
    if (log.isTraceEnabled())
    {
      ClassLoader cl;
      
      if (Globals.IS_SECURITY_ENABLED) {
        cl = (ClassLoader)AccessController.doPrivileged(new PrivilegedGetClassLoader(clazz));
      } else {
        cl = clazz.getClassLoader();
      }
      log.debug("      Loaded by " + cl.toString());
    }
    return clazz;
  }
  
  public URL findResource(String name)
  {
    if (log.isDebugEnabled()) {
      log.debug("    findResource(" + name + ")");
    }
    URL url = null;
    if ((this.hasExternalRepositories) && (this.searchExternalFirst)) {
      url = super.findResource(name);
    }
    if (url == null)
    {
      ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(name);
      if (entry == null) {
        if (this.securityManager != null)
        {
          PrivilegedAction<ResourceEntry> dp = new PrivilegedFindResourceByName(name, name);
          
          entry = (ResourceEntry)AccessController.doPrivileged(dp);
        }
        else
        {
          entry = findResourceInternal(name, name);
        }
      }
      if (entry != null) {
        url = entry.source;
      }
    }
    if ((url == null) && (this.hasExternalRepositories) && (!this.searchExternalFirst)) {
      url = super.findResource(name);
    }
    if (log.isDebugEnabled()) {
      if (url != null) {
        log.debug("    --> Returning '" + url.toString() + "'");
      } else {
        log.debug("    --> Resource not found, returning null");
      }
    }
    return url;
  }
  
  public Enumeration<URL> findResources(String name)
    throws IOException
  {
    if (log.isDebugEnabled()) {
      log.debug("    findResources(" + name + ")");
    }
    LinkedHashSet<URL> result = new LinkedHashSet();
    
    int jarFilesLength = this.jarFiles.length;
    int repositoriesLength = this.repositories.length;
    if ((this.hasExternalRepositories) && (this.searchExternalFirst))
    {
      Enumeration<URL> otherResourcePaths = super.findResources(name);
      while (otherResourcePaths.hasMoreElements()) {
        result.add(otherResourcePaths.nextElement());
      }
    }
    for (int i = 0; i < repositoriesLength; i++) {
      try
      {
        String fullPath = this.repositories[i] + name;
        this.resources.lookup(fullPath);
        try
        {
          result.add(getURI(new File(this.files[i], name)));
        }
        catch (MalformedURLException e) {}
      }
      catch (NamingException e) {}
    }
    synchronized (this.jarFiles)
    {
      if (openJARs()) {
    	  int i;
        for (i = 0; i < jarFilesLength; i++)
        {
          JarEntry jarEntry = this.jarFiles[i].getJarEntry(name);
          if (jarEntry != null) {
            try
            {
              String jarFakeUrl = getURI(this.jarRealFiles[i]).toString();
              jarFakeUrl = "jar:" + jarFakeUrl + "!/" + name;
              result.add(new URL(jarFakeUrl));
            }
            catch (MalformedURLException e) {}
          }
        }
      }
    }
    if ((this.hasExternalRepositories) && (!this.searchExternalFirst))
    {
      Enumeration<URL> otherResourcePaths = super.findResources(name);
      while (otherResourcePaths.hasMoreElements()) {
        result.add(otherResourcePaths.nextElement());
      }
    }
    return Collections.enumeration(result);
  }
  
  public URL getResource(String name)
  {
    if (log.isDebugEnabled()) {
      log.debug("getResource(" + name + ")");
    }
    URL url = null;
    if (this.delegate)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Delegating to parent classloader " + this.parent);
      }
      url = this.parent.getResource(name);
      if (url != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  --> Returning '" + url.toString() + "'");
        }
        return url;
      }
    }
    url = findResource(name);
    if (url != null)
    {
      if (this.antiJARLocking)
      {
        ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(name);
        try
        {
          String repository = entry.codeBase.toString();
          if ((repository.endsWith(".jar")) && (!name.endsWith(".class")))
          {
            File resourceFile = new File(this.loaderDir, name);
            url = getURI(resourceFile);
          }
        }
        catch (Exception e) {}
      }
      if (log.isDebugEnabled()) {
        log.debug("  --> Returning '" + url.toString() + "'");
      }
      return url;
    }
    if (!this.delegate)
    {
      url = this.parent.getResource(name);
      if (url != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  --> Returning '" + url.toString() + "'");
        }
        return url;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("  --> Resource not found, returning null");
    }
    return null;
  }
  
  public InputStream getResourceAsStream(String name)
  {
    if (log.isDebugEnabled()) {
      log.debug("getResourceAsStream(" + name + ")");
    }
    InputStream stream = null;
    
    stream = findLoadedResource(name);
    if (stream != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("  --> Returning stream from cache");
      }
      return stream;
    }
    if (this.delegate)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Delegating to parent classloader " + this.parent);
      }
      stream = this.parent.getResourceAsStream(name);
      if (stream != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  --> Returning stream from parent");
        }
        return stream;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("  Searching local repositories");
    }
    URL url = findResource(name);
    if (url != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("  --> Returning stream from local");
      }
      stream = findLoadedResource(name);
      try
      {
        if ((this.hasExternalRepositories) && (stream == null)) {
          stream = url.openStream();
        }
      }
      catch (IOException e) {}
      if (stream != null) {
        return stream;
      }
    }
    if (!this.delegate)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Delegating to parent classloader unconditionally " + this.parent);
      }
      stream = this.parent.getResourceAsStream(name);
      if (stream != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  --> Returning stream from parent");
        }
        return stream;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("  --> Resource not found, returning null");
    }
    return null;
  }
  
  public Class<?> loadClass(String name)
    throws ClassNotFoundException
  {
    return loadClass(name, false);
  }
  
  public synchronized Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    if (log.isDebugEnabled()) {
      log.debug("loadClass(" + name + ", " + resolve + ")");
    }
    Class<?> clazz = null;
    if (!this.started) {
      try
      {
        throw new IllegalStateException();
      }
      catch (IllegalStateException e)
      {
        log.info(sm.getString("webappClassLoader.stopped", new Object[] { name }));
      }
    }
    clazz = findLoadedClass0(name);
    if (clazz != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Returning class from cache");
      }
      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
    clazz = findLoadedClass(name);
    if (clazz != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Returning class from cache");
      }
      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
    try
    {
      clazz = this.j2seClassLoader.loadClass(name);
      if (clazz != null)
      {
        if (resolve) {
          resolveClass(clazz);
        }
        return clazz;
      }
    }
    catch (ClassNotFoundException e) {}
    if (this.securityManager != null)
    {
      int i = name.lastIndexOf('.');
      if (i >= 0) {
        try
        {
          this.securityManager.checkPackageAccess(name.substring(0, i));
        }
        catch (SecurityException se)
        {
          String error = "Security Violation, attempt to use Restricted Class: " + name;
          
          log.info(error, se);
          throw new ClassNotFoundException(error, se);
        }
      }
    }
    boolean delegateLoad = (this.delegate) || (filter(name));
    if (delegateLoad)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Delegating to parent classloader1 " + this.parent);
      }
      try
      {
        clazz = Class.forName(name, false, this.parent);
        if (clazz != null)
        {
          if (log.isDebugEnabled()) {
            log.debug("  Loading class from parent");
          }
          if (resolve) {
            resolveClass(clazz);
          }
          return clazz;
        }
      }
      catch (ClassNotFoundException e) {}
    }
    if (log.isDebugEnabled()) {
      log.debug("  Searching local repositories");
    }
    try
    {
      clazz = findClass(name);
      if (clazz != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  Loading class from local repository");
        }
        if (resolve) {
          resolveClass(clazz);
        }
        return clazz;
      }
    }
    catch (ClassNotFoundException e) {}
    if (!delegateLoad)
    {
      if (log.isDebugEnabled()) {
        log.debug("  Delegating to parent classloader at end: " + this.parent);
      }
      try
      {
        clazz = Class.forName(name, false, this.parent);
        if (clazz != null)
        {
          if (log.isDebugEnabled()) {
            log.debug("  Loading class from parent");
          }
          if (resolve) {
            resolveClass(clazz);
          }
          return clazz;
        }
      }
      catch (ClassNotFoundException e) {}
    }
    throw new ClassNotFoundException(name);
  }
  
  protected PermissionCollection getPermissions(CodeSource codeSource)
  {
    String codeUrl = codeSource.getLocation().toString();
    PermissionCollection pc;
    if ((pc = (PermissionCollection)this.loaderPC.get(codeUrl)) == null)
    {
      pc = super.getPermissions(codeSource);
      if (pc != null)
      {
        Iterator<Permission> perms = this.permissionList.iterator();
        while (perms.hasNext())
        {
          Permission p = (Permission)perms.next();
          pc.add(p);
        }
        this.loaderPC.put(codeUrl, pc);
      }
    }
    return pc;
  }
  
  public URL[] getURLs()
  {
	  int i;
    if (this.repositoryURLs != null) {
      return (URL[])this.repositoryURLs.clone();
    }
    URL[] external = super.getURLs();
    
    int filesLength = this.files.length;
    int jarFilesLength = this.jarRealFiles.length;
    int externalsLength = external.length;
    int off = 0;
    try
    {
      URL[] urls = new URL[filesLength + jarFilesLength + externalsLength];
      if (this.searchExternalFirst)
      {
        for ( i = 0; i < externalsLength; i++) {
          urls[i] = external[i];
        }
        off = externalsLength;
      }
      for ( i = 0; i < filesLength; i++) {
        urls[(off + i)] = getURI(this.files[i]);
      }
      off += filesLength;

      for (i = 0; i < jarFilesLength; i++) {
        urls[(off + i)] = getURI(this.jarRealFiles[i]);
      }
      off += jarFilesLength;
      if (!this.searchExternalFirst) {
    
        for (i = 0; i < externalsLength; i++) {
          urls[(off + i)] = external[i];
        }
      }
      this.repositoryURLs = urls;
    }
    catch (MalformedURLException e)
    {
      this.repositoryURLs = new URL[0];
    }
    return (URL[])this.repositoryURLs.clone();
  }
  
  public LifecycleListener[] findLifecycleListeners()
  {
    return new LifecycleListener[0];
  }
  
  public LifecycleState getState()
  {
    return LifecycleState.NEW;
  }
  
  public String getStateName()
  {
    return getState().toString();
  }
  
  public void start()
    throws LifecycleException
  {
    this.started = true;
    String encoding = null;
    try
    {
      encoding = System.getProperty("file.encoding");
    }
    catch (SecurityException e)
    {
      return;
    }
    if (encoding.indexOf("EBCDIC") != -1) {
      this.needConvert = true;
    }
  }
  
  public boolean isStarted()
  {
    return this.started;
  }
  
  public void stop()
    throws LifecycleException
  {
    clearReferences();
    
    this.started = false;
    
    int length = this.files.length;
    for (int i = 0; i < length; i++) {
      this.files[i] = null;
    }
    length = this.jarFiles.length;
    for (int i = 0; i < length; i++)
    {
      try
      {
        if (this.jarFiles[i] != null) {
          this.jarFiles[i].close();
        }
      }
      catch (IOException e) {}
      this.jarFiles[i] = null;
    }
    this.notFoundResources.clear();
    this.resourceEntries.clear();
    this.resources = null;
    this.repositories = null;
    this.repositoryURLs = null;
    this.files = null;
    this.jarFiles = null;
    this.jarRealFiles = null;
    this.jarPath = null;
    this.jarNames = null;
    this.lastModifiedDates = null;
    this.paths = null;
    this.hasExternalRepositories = false;
    this.parent = null;
    
    this.permissionList.clear();
    this.loaderPC.clear();
    if (this.loaderDir != null) {
      deleteDir(this.loaderDir);
    }
  }
  
  public void closeJARs(boolean force)
  {
    if (this.jarFiles.length > 0) {
      synchronized (this.jarFiles)
      {
        if ((force) || (System.currentTimeMillis() > this.lastJarAccessed + 90000L)) {
          for (int i = 0; i < this.jarFiles.length; i++) {
            try
            {
              if (this.jarFiles[i] != null)
              {
                this.jarFiles[i].close();
                this.jarFiles[i] = null;
              }
            }
            catch (IOException e)
            {
              if (log.isDebugEnabled()) {
                log.debug("Failed to close JAR", e);
              }
            }
          }
        }
      }
    }
  }
  
  protected ClassLoader getJavaseClassLoader()
  {
    return this.j2seClassLoader;
  }
  
  protected void setJavaseClassLoader(ClassLoader classLoader)
  {
    if (classLoader == null) {
      throw new IllegalArgumentException(sm.getString("webappClassLoader.javaseClassLoaderNull"));
    }
    this.j2seClassLoader = classLoader;
  }
  
  protected void clearReferences()
  {
    clearReferencesJdbc();
    
    clearReferencesThreads();
    
    checkThreadLocalsForLeaks();
    
    clearReferencesRmiTargets();
    if (this.clearReferencesStatic) {
      clearReferencesStaticFinal();
    }
    IntrospectionUtils.clear();
    if (this.clearReferencesLogFactoryRelease) {
      LogFactory.release(this);
    }
    clearReferencesResourceBundles();
    
    Introspector.flushCaches();
  }
  
  private final void clearReferencesJdbc()
  {
    InputStream is = getResourceAsStream("org/apache/catalina/loader/JdbcLeakPrevention.class");
    
    byte[] classBytes = new byte['ࠀ'];
    int offset = 0;
    try
    {
      int read = is.read(classBytes, offset, classBytes.length - offset);
      while (read > -1)
      {
        offset += read;
        if (offset == classBytes.length)
        {
          byte[] tmp = new byte[classBytes.length * 2];
          System.arraycopy(classBytes, 0, tmp, 0, classBytes.length);
          classBytes = tmp;
        }
        read = is.read(classBytes, offset, classBytes.length - offset);
      }
      Class<?> lpClass = defineClass("org.apache.catalina.loader.JdbcLeakPrevention", classBytes, 0, offset, getClass().getProtectionDomain());
      
      Object obj = lpClass.newInstance();
      
      List<String> driverNames = (List)obj.getClass().getMethod("clearJdbcDriverRegistrations", new Class[0]).invoke(obj, new Object[0]);
      for (String name : driverNames) {
        log.error(sm.getString("webappClassLoader.clearJdbc", new Object[] { this.contextName, name }));
      }
      Throwable t;
      return;
    }
    catch (Exception e)
    {
      Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
      ExceptionUtils.handleThrowable(t);
      log.warn(sm.getString("webappClassLoader.jdbcRemoveFailed", new Object[] { this.contextName }), t);
    }
    finally
    {
      if (is != null) {
        try
        {
          is.close();
        }
        catch (IOException ioe)
        {
          log.warn(sm.getString("webappClassLoader.jdbcRemoveStreamError", new Object[] { this.contextName }), ioe);
        }
      }
    }
  }
  
  private final void clearReferencesStaticFinal()
  {
    Collection<ResourceEntry> values = ((HashMap)this.resourceEntries.clone()).values();
    
    Iterator<ResourceEntry> loadedClasses = values.iterator();
    while (loadedClasses.hasNext())
    {
      ResourceEntry entry = (ResourceEntry)loadedClasses.next();
      if (entry.loadedClass != null)
      {
        Class<?> clazz = entry.loadedClass;
        try
        {
          Field[] fields = clazz.getDeclaredFields();
          for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers()))
            {
              fields[i].get(null);
              break;
            }
          }
        }
        catch (Throwable t) {}
      }
    }
    loadedClasses = values.iterator();
    while (loadedClasses.hasNext())
    {
      ResourceEntry entry = (ResourceEntry)loadedClasses.next();
      if (entry.loadedClass != null)
      {
        Class<?> clazz = entry.loadedClass;
        try
        {
          Field[] fields = clazz.getDeclaredFields();
          for (int i = 0; i < fields.length; i++)
          {
            Field field = fields[i];
            int mods = field.getModifiers();
            if ((!field.getType().isPrimitive()) && (field.getName().indexOf("$") == -1)) {
              if (Modifier.isStatic(mods)) {
                try
                {
                  field.setAccessible(true);
                  if (Modifier.isFinal(mods))
                  {
                    if ((!field.getType().getName().startsWith("java.")) && (!field.getType().getName().startsWith("javax."))) {
                      nullInstance(field.get(null));
                    }
                  }
                  else
                  {
                    field.set(null, null);
                    if (log.isDebugEnabled()) {
                      log.debug("Set field " + field.getName() + " to null in class " + clazz.getName());
                    }
                  }
                }
                catch (Throwable t)
                {
                  ExceptionUtils.handleThrowable(t);
                  if (log.isDebugEnabled()) {
                    log.debug("Could not set field " + field.getName() + " to null in class " + clazz.getName(), t);
                  }
                }
              }
            }
          }
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          if (log.isDebugEnabled()) {
            log.debug("Could not clean fields for class " + clazz.getName(), t);
          }
        }
      }
    }
  }
  
  private void nullInstance(Object instance)
  {
    if (instance == null) {
      return;
    }
    Field[] fields = instance.getClass().getDeclaredFields();
    for (int i = 0; i < fields.length; i++)
    {
      Field field = fields[i];
      int mods = field.getModifiers();
      if ((!field.getType().isPrimitive()) && (field.getName().indexOf("$") == -1)) {
        try
        {
          field.setAccessible(true);
          if ((!Modifier.isStatic(mods)) || (!Modifier.isFinal(mods)))
          {
            Object value = field.get(instance);
            if (null != value)
            {
              Class<? extends Object> valueClass = value.getClass();
              if (!loadedByThisOrChild(valueClass))
              {
                if (log.isDebugEnabled()) {
                  log.debug("Not setting field " + field.getName() + " to null in object of class " + instance.getClass().getName() + " because the referenced object was of type " + valueClass.getName() + " which was not loaded by this WebappClassLoader.");
                }
              }
              else
              {
                field.set(instance, null);
                if (log.isDebugEnabled()) {
                  log.debug("Set field " + field.getName() + " to null in class " + instance.getClass().getName());
                }
              }
            }
          }
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          if (log.isDebugEnabled()) {
            log.debug("Could not set field " + field.getName() + " to null in object instance of class " + instance.getClass().getName(), t);
          }
        }
      }
    }
  }
  
  private void clearReferencesThreads()
  {
    Thread[] threads = getThreads();
    List<Thread> executorThreadsToStop = new ArrayList();
    for (Thread thread : threads) {
      if (thread != null)
      {
        ClassLoader ccl = thread.getContextClassLoader();
        if (ccl == this) {
          if (thread != Thread.currentThread())
          {
            ThreadGroup tg = thread.getThreadGroup();
            if ((tg != null) && (JVM_THREAD_GROUP_NAMES.contains(tg.getName())))
            {
              if ((this.clearReferencesHttpClientKeepAliveThread) && (thread.getName().equals("Keep-Alive-Timer")))
              {
                thread.setContextClassLoader(this.parent);
                log.debug(sm.getString("webappClassLoader.checkThreadsHttpClient"));
              }
            }
            else if (thread.isAlive()) {
              if ((thread.getClass().getName().startsWith("java.util.Timer")) && (this.clearReferencesStopTimerThreads))
              {
                clearReferencesStopTimerThread(thread);
              }
              else
              {
                if (isRequestThread(thread)) {
                  log.error(sm.getString("webappClassLoader.warnRequestThread", new Object[] { this.contextName, thread.getName() }));
                } else {
                  log.error(sm.getString("webappClassLoader.warnThread", new Object[] { this.contextName, thread.getName() }));
                }
                if (this.clearReferencesStopThreads)
                {
                  boolean usingExecutor = false;
                  try
                  {
                    Object target = null;
                    for (String fieldName : new String[] { "target", "runnable", "action" }) {
                      try
                      {
                        Field targetField = thread.getClass().getDeclaredField(fieldName);
                        
                        targetField.setAccessible(true);
                        target = targetField.get(thread);
                      }
                      catch (NoSuchFieldException nfe) {}
                    }
                    if ((target != null) && (target.getClass().getCanonicalName() != null) && (target.getClass().getCanonicalName().equals("java.util.concurrent.ThreadPoolExecutor.Worker")))
                    {
                      Field executorField = target.getClass().getDeclaredField("this$0");
                      
                      executorField.setAccessible(true);
                      Object executor = executorField.get(target);
                      if ((executor instanceof ThreadPoolExecutor))
                      {
                        ((ThreadPoolExecutor)executor).shutdownNow();
                        usingExecutor = true;
                      }
                    }
                  }
                  catch (SecurityException e)
                  {
                    log.warn(sm.getString("webappClassLoader.stopThreadFail", new Object[] { thread.getName(), this.contextName }), e);
                  }
                  catch (NoSuchFieldException e)
                  {
                    log.warn(sm.getString("webappClassLoader.stopThreadFail", new Object[] { thread.getName(), this.contextName }), e);
                  }
                  catch (IllegalArgumentException e)
                  {
                    log.warn(sm.getString("webappClassLoader.stopThreadFail", new Object[] { thread.getName(), this.contextName }), e);
                  }
                  catch (IllegalAccessException e)
                  {
                    log.warn(sm.getString("webappClassLoader.stopThreadFail", new Object[] { thread.getName(), this.contextName }), e);
                  }
                  if (usingExecutor) {
                    executorThreadsToStop.add(thread);
                  } else {
                    thread.stop();
                  }
                }
              }
            }
          }
        }
      }
    }
    int count = 0;
    for (Thread t : executorThreadsToStop)
    {
      while ((t.isAlive()) && (count < 100))
      {
        try
        {
          Thread.sleep(20L);
        }
        catch (InterruptedException e)
        {
          break;
        }
        count++;
      }
      if (t.isAlive()) {
        t.stop();
      }
    }
  }
  
  private boolean isRequestThread(Thread thread)
  {
    StackTraceElement[] elements = thread.getStackTrace();
    if ((elements == null) || (elements.length == 0)) {
      return false;
    }
    for (int i = 0; i < elements.length; i++)
    {
      StackTraceElement element = elements[(elements.length - (i + 1))];
      if ("org.apache.catalina.connector.CoyoteAdapter".equals(element.getClassName())) {
        return true;
      }
    }
    return false;
  }
  
  private void clearReferencesStopTimerThread(Thread thread)
  {
    try
    {
      try
      {
        Field newTasksMayBeScheduledField = thread.getClass().getDeclaredField("newTasksMayBeScheduled");
        
        newTasksMayBeScheduledField.setAccessible(true);
        Field queueField = thread.getClass().getDeclaredField("queue");
        queueField.setAccessible(true);
        
        Object queue = queueField.get(thread);
        
        Method clearMethod = queue.getClass().getDeclaredMethod("clear", new Class[0]);
        clearMethod.setAccessible(true);
        synchronized (queue)
        {
          newTasksMayBeScheduledField.setBoolean(thread, false);
          clearMethod.invoke(queue, new Object[0]);
          queue.notify();
        }
      }
      catch (NoSuchFieldException nfe)
      {
        Method cancelMethod = thread.getClass().getDeclaredMethod("cancel", new Class[0]);
        synchronized (thread)
        {
          cancelMethod.setAccessible(true);
          cancelMethod.invoke(thread, new Object[0]);
        }
      }
      log.error(sm.getString("webappClassLoader.warnTimerThread", new Object[] { this.contextName, thread.getName() }));
    }
    catch (Exception e)
    {
      Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
      ExceptionUtils.handleThrowable(t);
      log.warn(sm.getString("webappClassLoader.stopTimerThreadFail", new Object[] { thread.getName(), this.contextName }), t);
    }
  }
  
  private void checkThreadLocalsForLeaks()
  {
    Thread[] threads = getThreads();
    try
    {
      Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
      
      threadLocalsField.setAccessible(true);
      Field inheritableThreadLocalsField = Thread.class.getDeclaredField("inheritableThreadLocals");
      
      inheritableThreadLocalsField.setAccessible(true);
      
      Class<?> tlmClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
      Field tableField = tlmClass.getDeclaredField("table");
      tableField.setAccessible(true);
      Method expungeStaleEntriesMethod = tlmClass.getDeclaredMethod("expungeStaleEntries", new Class[0]);
      expungeStaleEntriesMethod.setAccessible(true);
      for (int i = 0; i < threads.length; i++) {
        if (threads[i] != null)
        {
          Object threadLocalMap = threadLocalsField.get(threads[i]);
          if (null != threadLocalMap)
          {
            expungeStaleEntriesMethod.invoke(threadLocalMap, new Object[0]);
            checkThreadLocalMapForLeaks(threadLocalMap, tableField);
          }
          threadLocalMap = inheritableThreadLocalsField.get(threads[i]);
          if (null != threadLocalMap)
          {
            expungeStaleEntriesMethod.invoke(threadLocalMap, new Object[0]);
            checkThreadLocalMapForLeaks(threadLocalMap, tableField);
          }
        }
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.warn(sm.getString("webappClassLoader.checkThreadLocalsForLeaksFail", new Object[] { getContextName() }), t);
    }
  }
  
  private void checkThreadLocalMapForLeaks(Object map, Field internalTableField)
    throws IllegalAccessException, NoSuchFieldException
  {
    if (map != null)
    {
      Object[] table = (Object[])internalTableField.get(map);
      if (table != null) {
        for (int j = 0; j < table.length; j++)
        {
          Object obj = table[j];
          if (obj != null)
          {
            boolean potentialLeak = false;
            
            Object key = ((Reference)obj).get();
            if ((equals(key)) || (loadedByThisOrChild(key))) {
              potentialLeak = true;
            }
            Field valueField = obj.getClass().getDeclaredField("value");
            
            valueField.setAccessible(true);
            Object value = valueField.get(obj);
            if ((equals(value)) || (loadedByThisOrChild(value))) {
              potentialLeak = true;
            }
            if (potentialLeak)
            {
              Object[] args = new Object[5];
              args[0] = this.contextName;
              if (key != null)
              {
                args[1] = getPrettyClassName(key.getClass());
                try
                {
                  args[2] = key.toString();
                }
                catch (Exception e)
                {
                  log.error(sm.getString("webappClassLoader.checkThreadLocalsForLeaks.badKey", new Object[] { args[1] }), e);
                  
                  args[2] = sm.getString("webappClassLoader.checkThreadLocalsForLeaks.unknown");
                }
              }
              if (value != null)
              {
                args[3] = getPrettyClassName(value.getClass());
                try
                {
                  args[4] = value.toString();
                }
                catch (Exception e)
                {
                  log.error(sm.getString("webappClassLoader.checkThreadLocalsForLeaks.badValue", new Object[] { args[3] }), e);
                  
                  args[4] = sm.getString("webappClassLoader.checkThreadLocalsForLeaks.unknown");
                }
              }
              if (value == null)
              {
                if (log.isDebugEnabled()) {
                  log.debug(sm.getString("webappClassLoader.checkThreadLocalsForLeaksDebug", args));
                }
              }
              else {
                log.error(sm.getString("webappClassLoader.checkThreadLocalsForLeaks", args));
              }
            }
          }
        }
      }
    }
  }
  
  private String getPrettyClassName(Class<?> clazz)
  {
    String name = clazz.getCanonicalName();
    if (name == null) {
      name = clazz.getName();
    }
    return name;
  }
  
  private boolean loadedByThisOrChild(Object o)
  {
    if (o == null) {
      return false;
    }
    Class<?> clazz;

    if ((o instanceof Class)) {
      clazz = (Class)o;
    } else {
      clazz = o.getClass();
    }
    ClassLoader cl = clazz.getClassLoader();
    while (cl != null)
    {
      if (cl == this) {
        return true;
      }
      cl = cl.getParent();
    }
    if ((o instanceof Collection))
    {
      Iterator<?> iter = ((Collection)o).iterator();
      try
      {
        while (iter.hasNext())
        {
          Object entry = iter.next();
          if (loadedByThisOrChild(entry)) {
            return true;
          }
        }
      }
      catch (ConcurrentModificationException e)
      {
        log.warn(sm.getString("webappClassLoader", new Object[] { clazz.getName(), getContextName() }), e);
      }
    }
    return false;
  }
  
  private Thread[] getThreads()
  {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    try
    {
      while (tg.getParent() != null) {
        tg = tg.getParent();
      }
    }
    catch (SecurityException se)
    {
      String msg = sm.getString("webappClassLoader.getThreadGroupError", new Object[] { tg.getName() });
      if (log.isDebugEnabled()) {
        log.debug(msg, se);
      } else {
        log.warn(msg);
      }
    }
    int threadCountGuess = tg.activeCount() + 50;
    Thread[] threads = new Thread[threadCountGuess];
    int threadCountActual = tg.enumerate(threads);
    while (threadCountActual == threadCountGuess)
    {
      threadCountGuess *= 2;
      threads = new Thread[threadCountGuess];
      
      threadCountActual = tg.enumerate(threads);
    }
    return threads;
  }
  
  private void clearReferencesRmiTargets()
  {
    try
    {
      Class<?> objectTargetClass = Class.forName("sun.rmi.transport.Target");
      
      Field cclField = objectTargetClass.getDeclaredField("ccl");
      cclField.setAccessible(true);
      
      Class<?> objectTableClass = Class.forName("sun.rmi.transport.ObjectTable");
      
      Field objTableField = objectTableClass.getDeclaredField("objTable");
      objTableField.setAccessible(true);
      Object objTable = objTableField.get(null);
      if (objTable == null) {
        return;
      }
      if ((objTable instanceof Map))
      {
        Iterator<?> iter = ((Map)objTable).values().iterator();
        while (iter.hasNext())
        {
          Object obj = iter.next();
          Object cclObject = cclField.get(obj);
          if (this == cclObject) {
            iter.remove();
          }
        }
      }
      Field implTableField = objectTableClass.getDeclaredField("implTable");
      implTableField.setAccessible(true);
      Object implTable = implTableField.get(null);
      if (implTable == null) {
        return;
      }
      if ((implTable instanceof Map))
      {
        Iterator<?> iter = ((Map)implTable).values().iterator();
        while (iter.hasNext())
        {
          Object obj = iter.next();
          Object cclObject = cclField.get(obj);
          if (this == cclObject) {
            iter.remove();
          }
        }
      }
    }
    catch (ClassNotFoundException e)
    {
      log.info(sm.getString("webappClassLoader.clearRmiInfo", new Object[] { this.contextName }), e);
    }
    catch (SecurityException e)
    {
      log.warn(sm.getString("webappClassLoader.clearRmiFail", new Object[] { this.contextName }), e);
    }
    catch (NoSuchFieldException e)
    {
      log.warn(sm.getString("webappClassLoader.clearRmiFail", new Object[] { this.contextName }), e);
    }
    catch (IllegalArgumentException e)
    {
      log.warn(sm.getString("webappClassLoader.clearRmiFail", new Object[] { this.contextName }), e);
    }
    catch (IllegalAccessException e)
    {
      log.warn(sm.getString("webappClassLoader.clearRmiFail", new Object[] { this.contextName }), e);
    }
  }
  
  private void clearReferencesResourceBundles()
  {
    try
    {
      Field cacheListField = ResourceBundle.class.getDeclaredField("cacheList");
      
      cacheListField.setAccessible(true);
      
      Map<?, ?> cacheList = (Map)cacheListField.get(null);
      
      Set<?> keys = cacheList.keySet();
      
      Field loaderRefField = null;
      
      Iterator<?> keysIter = keys.iterator();
      
      int countRemoved = 0;
      while (keysIter.hasNext())
      {
        Object key = keysIter.next();
        if (loaderRefField == null)
        {
          loaderRefField = key.getClass().getDeclaredField("loaderRef");
          
          loaderRefField.setAccessible(true);
        }
        WeakReference<?> loaderRef = (WeakReference)loaderRefField.get(key);
        
        ClassLoader loader = (ClassLoader)loaderRef.get();
        while ((loader != null) && (loader != this)) {
          loader = loader.getParent();
        }
        if (loader != null)
        {
          keysIter.remove();
          countRemoved++;
        }
      }
      if ((countRemoved > 0) && (log.isDebugEnabled())) {
        log.debug(sm.getString("webappClassLoader.clearReferencesResourceBundlesCount", new Object[] { Integer.valueOf(countRemoved), this.contextName }));
      }
    }
    catch (SecurityException e)
    {
      log.error(sm.getString("webappClassLoader.clearReferencesResourceBundlesFail", new Object[] { this.contextName }), e);
    }
    catch (NoSuchFieldException e)
    {
      if (Globals.IS_ORACLE_JVM) {
        log.error(sm.getString("webappClassLoader.clearReferencesResourceBundlesFail", new Object[] { getContextName() }), e);
      } else {
        log.debug(sm.getString("webappClassLoader.clearReferencesResourceBundlesFail", new Object[] { getContextName() }), e);
      }
    }
    catch (IllegalArgumentException e)
    {
      log.error(sm.getString("webappClassLoader.clearReferencesResourceBundlesFail", new Object[] { this.contextName }), e);
    }
    catch (IllegalAccessException e)
    {
      log.error(sm.getString("webappClassLoader.clearReferencesResourceBundlesFail", new Object[] { this.contextName }), e);
    }
  }
  
  protected boolean openJARs()
  {
    if ((this.started) && (this.jarFiles.length > 0))
    {
      this.lastJarAccessed = System.currentTimeMillis();
      if (this.jarFiles[0] == null) {
        for (int i = 0; i < this.jarFiles.length; i++) {
          try
          {
            this.jarFiles[i] = new JarFile(this.jarRealFiles[i]);
          }
          catch (IOException e)
          {
            if (log.isDebugEnabled()) {
              log.debug("Failed to open JAR", e);
            }
            return false;
          }
        }
      }
    }
    return true;
  }
  
  protected Class<?> findClassInternal(String name)
    throws ClassNotFoundException
  {
    if (!validate(name)) {
      throw new ClassNotFoundException(name);
    }
    String tempPath = name.replace('.', '/');
    String classPath = tempPath + ".class";
    
    ResourceEntry entry = null;
    if (this.securityManager != null)
    {
      PrivilegedAction<ResourceEntry> dp = new PrivilegedFindResourceByName(name, classPath);
      
      entry = (ResourceEntry)AccessController.doPrivileged(dp);
    }
    else
    {
      entry = findResourceInternal(name, classPath);
    }
    if (entry == null) {
      throw new ClassNotFoundException(name);
    }
    Class<?> clazz = entry.loadedClass;
    if (clazz != null) {
      return clazz;
    }
    synchronized (this)
    {
      clazz = entry.loadedClass;
      if (clazz != null) {
        return clazz;
      }
      if (entry.binaryContent == null) {
        throw new ClassNotFoundException(name);
      }
      String packageName = null;
      int pos = name.lastIndexOf('.');
      if (pos != -1) {
        packageName = name.substring(0, pos);
      }
      Package pkg = null;
      if (packageName != null)
      {
        pkg = getPackage(packageName);
        if (pkg == null)
        {
          try
          {
            if (entry.manifest == null) {
              definePackage(packageName, null, null, null, null, null, null, null);
            } else {
              definePackage(packageName, entry.manifest, entry.codeBase);
            }
          }
          catch (IllegalArgumentException e) {}
          pkg = getPackage(packageName);
        }
      }
      if (this.securityManager != null) {
        if (pkg != null)
        {
          boolean sealCheck = true;
          if (pkg.isSealed()) {
            sealCheck = pkg.isSealed(entry.codeBase);
          } else {
            sealCheck = (entry.manifest == null) || (!isPackageSealed(packageName, entry.manifest));
          }
          if (!sealCheck) {
            throw new SecurityException("Sealing violation loading " + name + " : Package " + packageName + " is sealed.");
          }
        }
      }
      try
      {
        clazz = defineClass(name, entry.binaryContent, 0, entry.binaryContent.length, new CodeSource(entry.codeBase, entry.certificates));
      }
      catch (UnsupportedClassVersionError ucve)
      {
        throw new UnsupportedClassVersionError(ucve.getLocalizedMessage() + " " + sm.getString("webappClassLoader.wrongVersion", new Object[] { name }));
      }
      entry.loadedClass = clazz;
      entry.binaryContent = null;
      entry.source = null;
      entry.codeBase = null;
      entry.manifest = null;
      entry.certificates = null;
    }
    return clazz;
  }
  
  protected ResourceEntry findResourceInternal(File file, String path)
  {
    ResourceEntry entry = new ResourceEntry();
    try
    {
      entry.source = getURI(new File(file, path));
      entry.codeBase = entry.source;
    }
    catch (MalformedURLException e)
    {
      return null;
    }
    return entry;
  }
  
  protected ResourceEntry findResourceInternal(String name, String path)
  {
    if (!this.started)
    {
      log.info(sm.getString("webappClassLoader.stopped", new Object[] { name }));
      return null;
    }
    if ((name == null) || (path == null)) {
      return null;
    }
    ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(name);
    if (entry != null) {
      return entry;
    }
    int contentLength = -1;
    InputStream binaryStream = null;
    boolean isClassResource = path.endsWith(".class");
    boolean isCacheable = isClassResource;
    if (!isCacheable) {
      isCacheable = path.startsWith("META-INF/services/");
    }
    int jarFilesLength = this.jarFiles.length;
    int repositoriesLength = this.repositories.length;
    
    Resource resource = null;
    
    boolean fileNeedConvert = false;
    ResourceAttributes attributes = null;
    for (int i = 0; (entry == null) && (i < repositoriesLength); i++) {
      try
      {
        String fullPath = this.repositories[i] + path;
        
        Object lookupResult = this.resources.lookup(fullPath);
        if ((lookupResult instanceof Resource)) {
          resource = (Resource)lookupResult;
        }
        attributes = (ResourceAttributes)this.resources.getAttributes(fullPath);
        
        contentLength = (int)attributes.getContentLength();
        String canonicalPath = attributes.getCanonicalPath();
        if (canonicalPath != null) {
          entry = findResourceInternal(new File(canonicalPath), "");
        } else {
          entry = findResourceInternal(this.files[i], path);
        }
        entry.lastModified = attributes.getLastModified();
        if (resource != null)
        {
          try
          {
            binaryStream = resource.streamContent();
          }
          catch (IOException e)
          {
            return null;
          }
          if ((this.needConvert) && 
            (path.endsWith(".properties"))) {
            fileNeedConvert = true;
          }
          synchronized (this.allPermission)
          {
            long[] result2 = new long[this.lastModifiedDates.length + 1];
            for (int j = 0; j < this.lastModifiedDates.length; j++) {
              result2[j] = this.lastModifiedDates[j];
            }
            result2[this.lastModifiedDates.length] = entry.lastModified;
            this.lastModifiedDates = result2;
            
            String[] result = new String[this.paths.length + 1];
            for (int j = 0; j < this.paths.length; j++) {
              result[j] = this.paths[j];
            }
            result[this.paths.length] = fullPath;
            this.paths = result;
          }
        }
      }
      catch (NamingException e) {}
    }
    if ((entry == null) && (this.notFoundResources.containsKey(name))) {
      return null;
    }
    JarEntry jarEntry = null;
    synchronized (this.jarFiles)
    {
      try
      {
        if (!openJARs())
        {
          attributes = null;
          if (binaryStream != null) {
            try
            {
              binaryStream.close();
            }
            catch (IOException e) {}
          }
          return null;
        }
        JarEntry jarEntry2;
        int i;
        for (i = 0; (entry == null) && (i < jarFilesLength); i++)
        {
          jarEntry = this.jarFiles[i].getJarEntry(path);
          if (jarEntry != null)
          {
            entry = new ResourceEntry();
            try
            {
              entry.codeBase = getURI(this.jarRealFiles[i]);
              String jarFakeUrl = entry.codeBase.toString();
              jarFakeUrl = "jar:" + jarFakeUrl + "!/" + path;
              entry.source = new URL(jarFakeUrl);
              entry.lastModified = this.jarRealFiles[i].lastModified();
            }
            catch (MalformedURLException e)
            {
            
              if (binaryStream != null) {
                try
                {
                  binaryStream.close();
                }
                catch (IOException ex) {}
              }
              return null;
            }
            contentLength = (int)jarEntry.getSize();
            try
            {
              entry.manifest = this.jarFiles[i].getManifest();
              binaryStream = this.jarFiles[i].getInputStream(jarEntry);
            }
            catch (IOException e)
            {
              e = null;
              if (binaryStream != null) {
                try
                {
                  binaryStream.close();
                }
                catch (IOException ex) {}
              }
              return null;
            }
            if ((this.antiJARLocking) && (!path.endsWith(".class")))
            {
              byte[] buf = new byte['Ѐ'];
              File resourceFile = new File(this.loaderDir, jarEntry.getName());
              if (!resourceFile.exists())
              {
                Enumeration<JarEntry> entries = this.jarFiles[i].entries();
                for (;;)
                {
                  if (entries.hasMoreElements())
                  {
                    jarEntry2 = (JarEntry)entries.nextElement();
                    if ((!jarEntry2.isDirectory()) && (!jarEntry2.getName().endsWith(".class")))
                    {
                      resourceFile = new File(this.loaderDir, jarEntry2.getName());
                      try
                      {
                        if (!resourceFile.getCanonicalPath().startsWith(this.canonicalLoaderDir)) {
                          throw new IllegalArgumentException(sm.getString("webappClassLoader.illegalJarPath", new Object[] { jarEntry2.getName() }));
                        }
                      }
                      catch (IOException ioe)
                      {
                        throw new IllegalArgumentException(sm.getString("webappClassLoader.validationErrorJarPath", new Object[] { jarEntry2.getName() }), ioe);
                      }
                      File parentFile = resourceFile.getParentFile();
                      if ((!parentFile.mkdirs()) && (!parentFile.exists())) {}
                      FileOutputStream os = null;
                      InputStream is = null;
                      try
                      {
                        is = this.jarFiles[i].getInputStream(jarEntry2);
                        
                        os = new FileOutputStream(resourceFile);
                        for (;;)
                        {
                          int n = is.read(buf);
                          if (n <= 0) {
                            break;
                          }
                          os.write(buf, 0, n);
                        }
                        resourceFile.setLastModified(jarEntry2.getTime());
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
                      catch (IOException e) {}finally
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
                  }
                }
              }
            }
          }
        }
        if (entry == null)
        {
          synchronized (this.notFoundResources)
          {
            this.notFoundResources.put(name, name);
          }
        
          if (binaryStream != null) {
            try
            {
              binaryStream.close();
            }
            catch (IOException e) {}
          }
          //return (ResourceEntry)???;
          return null;
        }
        if ((binaryStream != null) && ((isCacheable) || (fileNeedConvert)))
        {
          byte[] binaryContent = new byte[contentLength];
          
          int pos = 0;
          try
          {
            for (;;)
            {
              int n = binaryStream.read(binaryContent, pos, binaryContent.length - pos);
              if (n <= 0) {
                break;
              }
              pos += n;
            }
          }
          catch (IOException e)
          {
            log.error(sm.getString("webappClassLoader.readError", new Object[] { name }), e);
          
            if (binaryStream != null) {
              try
              {
                binaryStream.close();
              }
              catch (IOException ex) {}
            }
            return null;
          }
          if (fileNeedConvert)
          {
            String str = new String(binaryContent, 0, pos);
            try
            {
              binaryContent = str.getBytes(CHARSET_UTF8);
            }
            catch (Exception e)
            {
              e = null;
              if (binaryStream != null) {
                try
                {
                  binaryStream.close();
                }
                catch (IOException ex) {}
              }
              return null;
            }
          }
          entry.binaryContent = binaryContent;
          if (jarEntry != null) {
            entry.certificates = jarEntry.getCertificates();
          }
        }
        if (binaryStream != null) {
          try
          {
            binaryStream.close();
          }
          catch (IOException e) {}
        }
      }
      finally
      {
        if (binaryStream != null) {
          try
          {
            binaryStream.close();
          }
          catch (IOException e) {}
        }
      }
    }
    synchronized (this.resourceEntries)
    {
      ResourceEntry entry2 = (ResourceEntry)this.resourceEntries.get(name);
      if (entry2 == null) {
        this.resourceEntries.put(name, entry);
      } else {
        entry = entry2;
      }
    }
    return entry;
  }
  
  protected boolean isPackageSealed(String name, Manifest man)
  {
    String path = name.replace('.', '/') + '/';
    Attributes attr = man.getAttributes(path);
    String sealed = null;
    if (attr != null) {
      sealed = attr.getValue(Attributes.Name.SEALED);
    }
    if ((sealed == null) && 
      ((attr = man.getMainAttributes()) != null)) {
      sealed = attr.getValue(Attributes.Name.SEALED);
    }
    return "true".equalsIgnoreCase(sealed);
  }
  
  protected InputStream findLoadedResource(String name)
  {
    ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(name);
    if (entry != null)
    {
      if (entry.binaryContent != null) {
        return new ByteArrayInputStream(entry.binaryContent);
      }
      try
      {
        return entry.source.openStream();
      }
      catch (IOException ioe) {}
    }
    return null;
  }
  
  protected Class<?> findLoadedClass0(String name)
  {
    ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(name);
    if (entry != null) {
      return entry.loadedClass;
    }
    return null;
  }
  
  protected void refreshPolicy()
  {
    try
    {
      Policy policy = Policy.getPolicy();
      policy.refresh();
    }
    catch (AccessControlException e) {}
  }
  
  protected boolean filter(String name)
  {
    if (name == null) {
      return false;
    }
    String packageName = null;
    int pos = name.lastIndexOf('.');
    if (pos != -1) {
      packageName = name.substring(0, pos);
    } else {
      return false;
    }
    for (int i = 0; i < packageTriggers.length; i++) {
      if (packageName.startsWith(packageTriggers[i])) {
        return true;
      }
    }
    return false;
  }
  
  protected boolean validate(String name)
  {
    if (name == null) {
      return false;
    }
    if (name.startsWith("java.")) {
      return false;
    }
    if (name.startsWith("javax.servlet.jsp.jstl")) {
      return true;
    }
    if (name.startsWith("javax.servlet.")) {
      return false;
    }
    if (name.startsWith("javax.el")) {
      return false;
    }
    return true;
  }
  
  protected boolean validateJarFile(File file)
    throws IOException
  {
    if (triggers == null) {
      return true;
    }
    JarFile jarFile = null;
    try
    {
      jarFile = new JarFile(file);
      for (int i = 0; i < triggers.length; i++)
      {
        Class<?> clazz = null;
        try
        {
          if (this.parent != null) {
            clazz = this.parent.loadClass(triggers[i]);
          } else {
            clazz = Class.forName(triggers[i]);
          }
        }
        catch (Exception e)
        {
          clazz = null;
        }
        if (clazz != null)
        {
          String name = triggers[i].replace('.', '/') + ".class";
          if (log.isDebugEnabled()) {
            log.debug(" Checking for " + name);
          }
          JarEntry jarEntry = jarFile.getJarEntry(name);
          if (jarEntry != null)
          {
            log.info("validateJarFile(" + file + ") - jar not loaded. See Servlet Spec 3.0, " + "section 10.7.2. Offending class: " + name);
            
            return false;
          }
        }
      }
      return true;
    }
    finally
    {
      if (jarFile != null) {
        try
        {
          jarFile.close();
        }
        catch (IOException ioe) {}
      }
    }
  }
  
  @Deprecated
  protected URL getURL(File file, boolean encoded)
    throws MalformedURLException
  {
    File realFile = file;
    try
    {
      realFile = realFile.getCanonicalFile();
    }
    catch (IOException e) {}
    if (encoded) {
      return getURI(realFile);
    }
    return realFile.toURI().toURL();
  }
  
  protected URL getURI(File file)
    throws MalformedURLException
  {
    File realFile = file;
    try
    {
      realFile = realFile.getCanonicalFile();
    }
    catch (IOException e) {}
    return realFile.toURI().toURL();
  }
  
  protected static void deleteDir(File dir)
  {
    String[] files = dir.list();
    if (files == null) {
      files = new String[0];
    }
    for (int i = 0; i < files.length; i++)
    {
      File file = new File(dir, files[i]);
      if (file.isDirectory()) {
        deleteDir(file);
      } else {
        file.delete();
      }
    }
    dir.delete();
  }
  
  public void addLifecycleListener(LifecycleListener listener) {}
  
  public void removeLifecycleListener(LifecycleListener listener) {}
  
  public void init() {}
  
  public void destroy() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\WebappClassLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */