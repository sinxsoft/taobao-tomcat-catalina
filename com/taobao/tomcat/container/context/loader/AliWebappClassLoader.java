package com.taobao.tomcat.container.context.loader;

import com.taobao.tomcat.container.context.pandora.ClassRepository;
import com.taobao.tomcat.container.context.pandora.SharedClassRepository;
import com.taobao.tomcat.util.ClassLoaderUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.naming.resources.ResourceCache;

public class AliWebappClassLoader
  extends WebappClassLoader
{
  private static final Log log = LogFactory.getLog(WebappClassLoader.class);
  private static final String osgiClassLoaderName = "org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader";
  private ClassRepository commonRepository = null;
  private Set<ClassLoader> pandoraClassLoaders = new HashSet();
  private Set<String> pandoraJars = new HashSet();
  private Set<String> webappJars = new HashSet();
  private Set<String> usedJars = new HashSet();
  
  public AliWebappClassLoader(ClassLoader parent)
  {
    super(parent);
  }
  
  public ClassRepository getCommonRepository()
  {
    return this.commonRepository;
  }
  
  public void setCommonRepository(ClassRepository commonRepository)
  {
    this.commonRepository = commonRepository;
  }
  
  public Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    Class<?> clazz = null;
    if (this.commonRepository != null) {
      clazz = this.commonRepository.resolveClass(name);
    }
    if (clazz == null) {
      clazz = super.loadClass(name, resolve);
    }
    if (clazz != null) {
      loadClass(clazz);
    }
    return clazz;
  }
  
  public URL getResource(String name)
  {
    URL url = getExportedResource(name);
    if (url != null) {
      return url;
    }
    url = super.getResource(name);
    if ((url != null) && (Boolean.getBoolean("com.tomcat.catalina.loader.jbossGetResource")))
    {
      String path = url.toString();
      if (path.endsWith("WEB-INF/classes/")) {
        try
        {
          return new URL(path.substring(0, path.length() - "WEB-INF/classes/".length()));
        }
        catch (MalformedURLException e) {}
      }
    }
    return url;
  }
  
  public InputStream getResourceAsStream(String name)
  {
    InputStream in = getExportedResourceAsStream(name);
    if (in != null) {
      return in;
    }
    return super.getResourceAsStream(name);
  }
  
  public void stop()
    throws LifecycleException
  {
    super.stop();
    this.pandoraClassLoaders.clear();
    this.pandoraJars.clear();
    this.webappJars.clear();
    this.usedJars.clear();
    this.pandoraClassLoaders = null;
    this.pandoraJars = null;
    this.webappJars = null;
    this.usedJars = null;
    this.commonRepository = null;
  }
  
  public InputStream getExportedResourceAsStream(String name)
  {
    Class<?> clazz = getClassByResourceName(name);
    if (clazz != null)
    {
      ClassLoader cl = clazz.getClassLoader();
      if (cl == null) {
        return null;
      }
      if ("org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader".startsWith(cl.getClass().getName())) {
        return null;
      }
      return cl.getResourceAsStream(name);
    }
    return null;
  }
  
  public URL getExportedResource(String name)
  {
    Class<?> clazz = getClassByResourceName(name);
    if (clazz != null)
    {
      ClassLoader cl = clazz.getClassLoader();
      if (cl == null) {
        return null;
      }
      if ("org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader".startsWith(cl.getClass().getName())) {
        return null;
      }
      return cl.getResource(name);
    }
    return null;
  }
  
  private Class<?> getClassByResourceName(String name)
  {
    if ((name == null) || (!name.endsWith(".class")) || (this.commonRepository == null)) {
      return null;
    }
    String className = name;
    if (className.startsWith("/")) {
      className = className.substring(1);
    }
    className = className.substring(0, className.length() - 6).replace('/', '.');
    
    return this.commonRepository.resolveClass(className);
  }
  
  public String[] getWebappLoadedJars()
  {
    return (String[])this.usedJars.toArray(new String[this.usedJars.size()]);
  }
  
  public String[] getWebappUnloadedJars()
  {
    Set<String> allJars = new HashSet(this.webappJars);
    allJars.removeAll(this.usedJars);
    return (String[])allJars.toArray(new String[allJars.size()]);
  }
  
  public String[] getPandoraJars()
  {
    return (String[])this.pandoraJars.toArray(new String[this.pandoraJars.size()]);
  }
  
  public URL getLoadLocation(String path)
  {
    ResourceEntry entry = (ResourceEntry)this.resourceEntries.get(path);
    return entry != null ? entry.source : null;
  }
  
  public byte[] getContent(String file)
  {
    String jarEntry = "";
    if (file.indexOf("!") > 0)
    {
      String[] parts = file.split("!");
      file = parts[0];
      jarEntry = parts[1];
      if (jarEntry.startsWith("/")) {
        jarEntry = jarEntry.substring(1);
      }
    }
    DirContext resourcesContext = getResources();
    try
    {
      Object node = resourcesContext.lookup(file);
      if ((node instanceof Resource))
      {
        Resource resource = (Resource)node;
        ResourceAttributes resourceAttributes = (ResourceAttributes)resourcesContext.getAttributes(file);
        String canonicalPath = resourceAttributes.getCanonicalPath();
        return isJar(canonicalPath) ? readFromJarFile(canonicalPath, jarEntry) : readRegularFile(resource, resourceAttributes);
      }
    }
    catch (NamingException e)
    {
      log.warn("illegal path, '" + file + "' doesn't exist in this webapp", e);
    }
    catch (IOException e)
    {
      log.warn("cannot open resource '" + file + "' from this webapp", e);
    }
    return null;
  }
  
  public Map<String, Object[]> listResources(String path)
  {
    if ((path == null) || (path.trim().length() == 0)) {
      path = "";
    }
    DirContext resourcesContext = getResources();
    Object node = null;
    try
    {
      node = resourcesContext.lookup(path);
    }
    catch (NamingException e)
    {
      log.warn("illegal path, '" + path + "' doesn't exist in this webapp", e);
    }
    if ((node instanceof DirContext)) {
      return listResources((DirContext)node);
    }
    if ((node instanceof Resource))
    {
      String canonicalPath = getCanonicalPath(resourcesContext, path);
      if (isJar(canonicalPath)) {
        return listJarEntries(canonicalPath);
      }
    }
    return null;
  }
  
  public String[] getJarLocations(String jarFileNamePattern)
  {
    if (jarFileNamePattern == null) {
      return new String[0];
    }
    List<String> resultList = searchQualifiedJarFile(null, this.webappJars, jarFileNamePattern);
    resultList = searchQualifiedJarFile(resultList, this.pandoraJars, jarFileNamePattern);
    return (String[])resultList.toArray(new String[resultList.size()]);
  }
  
  public String[] getClassLocations(String className)
  {
    if (className == null) {
      return new String[0];
    }
    Set<String> resultSet = new HashSet();
    Class<?> clazz = ClassLoaderUtils.findLoadedClass(this, className);
    if (clazz != null)
    {
      URL url = ClassLoaderUtils.locateURL(clazz);
      if (url != null) {
        resultSet.add(url.getFile());
      }
    }
    for (ClassLoader classLoader : this.pandoraClassLoaders)
    {
      clazz = ClassLoaderUtils.findLoadedClass(classLoader, className);
      if (clazz != null)
      {
        URL url = ClassLoaderUtils.locateURL(clazz);
        if (url != null) {
          resultSet.add(url.getFile());
        }
      }
    }
    return (String[])resultSet.toArray(new String[resultSet.size()]);
  }
  
  public void collectClassInfos()
  {
    collectClassInfoFromPandoraModules();
    collectClassInfoFromWebapp();
  }
  
  private boolean isJar(String path)
  {
    return (path != null) && (path.endsWith(".jar"));
  }
  
  private List<String> searchQualifiedJarFile(List<String> result, Collection<String> jarFileList, String pattern)
  {
    if ((result == null) || (result.isEmpty())) {
      result = new ArrayList();
    }
    for (String jarFile : jarFileList)
    {
      String jarFileName = jarFile.substring(jarFile.lastIndexOf('/') + 1);
      if (jarFileName.toLowerCase().contains(pattern.toLowerCase())) {
        result.add(jarFile);
      }
    }
    return result;
  }
  
  private byte[] readFromJarFile(String jarFile, String jarEntry)
    throws IOException
  {
    JarFile jar = null;
    try
    {
      jar = new JarFile(jarFile);
      JarEntry entry = jar.getJarEntry(jarEntry);
      return read(jar.getInputStream(entry), (int)entry.getSize());
    }
    finally
    {
      closeJar(jar);
    }
  }
  
  private byte[] readRegularFile(Resource resource, ResourceAttributes resourceAttributes)
    throws IOException
  {
    return read(resource.streamContent(), (int)resourceAttributes.getContentLength());
  }
  
  private byte[] read(InputStream is, int size)
    throws IOException
  {
    byte[] bytes = new byte[size];
    int pos = 0;
    for (;;)
    {
      int n = is.read(bytes, pos, size - pos);
      if (n <= 0) {
        break;
      }
      pos += n;
    }
    return bytes;
  }
  
  private String getCanonicalPath(DirContext resourcesContext, String path)
  {
    try
    {
      ResourceAttributes attributes = (ResourceAttributes)resourcesContext.getAttributes(path);
      return attributes.getCanonicalPath();
    }
    catch (NamingException e)
    {
      log.warn("illegal path, '" + path + "' doesn't exist in this webapp", e);
    }
    return null;
  }
  
  private Map<String, Object[]> listResources(DirContext dirContext)
  {
    Map<String, Object[]> entries = new HashMap();
    try
    {
      NamingEnumeration<Binding> enumeration = dirContext.listBindings("");
      while (enumeration.hasMoreElements())
      {
        Binding binding = (Binding)enumeration.nextElement();
        String entryName = binding.getName();
        ResourceAttributes attributes = (ResourceAttributes)dirContext.getAttributes(entryName);
        String currentLocation = translateBackSlash(dirContext.getNameInNamespace());
        String baseLocation = translateBackSlash(getResources().getNameInNamespace());
        String relativeLocation = resolveRelativePath(baseLocation, currentLocation);
        relativeLocation = relativeLocation + "/" + entryName;
        String resourceLocation = transferLocationUnderWebInfo(relativeLocation);
        boolean isDir = attributes.isCollection();
        long lastModified = attributes.getLastModified();
        boolean isLoaded = isLoaded(baseLocation, resourceLocation, isDir);
        entries.put(binding.getName(), createEntryStatus(lastModified, isLoaded, !isDir));
      }
    }
    catch (NamingException e)
    {
      log.warn("listBindings on " + dirContext + " failed", e);
    }
    return entries;
  }
  
  private boolean isLoaded(String baseLocation, String relativePath, boolean isDir)
  {
    if (isJar(relativePath)) {
      try
      {
        File file = new File(baseLocation + "/" + relativePath);
        return this.usedJars.contains(file.toURL().getFile());
      }
      catch (MalformedURLException e) {}
    }
    return (!isDir) && ((this.resourceEntries.get(relativePath) != null) || (isCached(relativePath)));
  }
  
  private String translateBackSlash(String path)
  {
    return path.contains("\\") ? path.replaceAll("\\\\", "/") : path;
  }
  
  private boolean isCached(String relativePath)
  {
    DirContext context = getResources();
    if (!(context instanceof ProxyDirContext)) {
      return false;
    }
    ProxyDirContext cachedContext = (ProxyDirContext)context;
    ResourceCache cache = cachedContext.getCache();
    if (!relativePath.startsWith("/")) {
      relativePath = "/" + relativePath;
    }
    return cache.lookup(relativePath) != null;
  }
  
  private String transferLocationUnderWebInfo(String location)
  {
    String webInf = "WEB-INF/classes/";
    if (location.startsWith(webInf))
    {
      location = location.substring(webInf.length());
      if (location.endsWith(".class"))
      {
        location = location.substring(0, location.length() - 6);
        location = location.replace('/', '.');
      }
    }
    return location;
  }
  
  private Map<String, Object[]> listJarEntries(String jarFilePath)
  {
    Map<String, Object[]> allJarEntries = new HashMap();
    JarFile jarFile = null;
    try
    {
      jarFile = new JarFile(jarFilePath);
      Enumeration<JarEntry> jarEntries = jarFile.entries();
      while (jarEntries.hasMoreElements())
      {
        JarEntry jarEntry = (JarEntry)jarEntries.nextElement();
        Object[] status = createEntryStatus(jarEntry.getTime(), this.resourceEntries.containsKey(jarEntry.getName()), true);
        
        allJarEntries.put(jarEntry.getName(), status);
      }
    }
    catch (IOException e)
    {
      log.warn("error occurs when opening jar file: " + jarFilePath, e);
    }
    finally
    {
      closeJar(jarFile);
    }
    return allJarEntries;
  }
  
  private Object[] createEntryStatus(long lastModified, boolean isLoaded, boolean isFile)
  {
    Object[] status = new Object[3];
    status[0] = Long.valueOf(lastModified);
    status[1] = Boolean.valueOf(isLoaded);
    status[2] = Boolean.valueOf(isFile);
    return status;
  }
  
  private String resolveRelativePath(String baseLocation, String currentLocation)
  {
    String relativePath = currentLocation;
    if (currentLocation.startsWith(baseLocation))
    {
      relativePath = currentLocation.substring(baseLocation.length());
      if (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
      }
    }
    return relativePath;
  }
  
  private void closeJar(JarFile jarFile)
  {
    if (jarFile != null) {
      try
      {
        jarFile.close();
      }
      catch (IOException e) {}
    }
  }
  
  private void collectClassInfoFromWebapp()
  {
    URL[] urls = getURLs();
    for (URL url : urls) {
      if (url.getFile().endsWith("jar")) {
        this.webappJars.add(url.getFile());
      }
    }
  }
  
  private void collectClassInfoFromPandoraModules()
  {
    if ((this.commonRepository instanceof SharedClassRepository))
    {
      SharedClassRepository r = (SharedClassRepository)this.commonRepository;
      for (Class<?> c : r.getClasses()) {
        addPandoraClassLoader(c.getClassLoader());
      }
    }
  }
  
  private void addPandoraClassLoader(ClassLoader classLoader)
  {
    if (classLoader != null)
    {
      this.pandoraClassLoaders.add(classLoader);
      if ((classLoader instanceof URLClassLoader))
      {
        URL[] urls = ((URLClassLoader)classLoader).getURLs();
        if (urls != null) {
          for (URL url : urls) {
            if (url.getFile().endsWith("jar")) {
              this.pandoraJars.add(url.getFile());
            }
          }
        }
      }
    }
  }
  
  private boolean loadClass(Class<?> clazz)
  {
    if (clazz != null) {
      try
      {
        URL url = ClassLoaderUtils.locateURL(clazz);
        if ((url != null) && (url.getFile().endsWith("jar"))) {
          this.usedJars.add(url.getFile());
        }
      }
      catch (Exception ex)
      {
        log.error("loadClass " + clazz + " got exception.", ex);
      }
    }
    return false;
  }
  
  public String toString()
  {
    return getClass().getName();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\context\loader\AliWebappClassLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */