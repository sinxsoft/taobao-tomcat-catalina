package com.taobao.tomcat.util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassLoaderUtils
{
  public static String WEB_INF_LIB = "WEB-INF/lib";
  private static Method method;
  
  static
  {
    try
    {
      method = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
      method.setAccessible(true);
    }
    catch (Exception ex) {}
  }
  
  public static URL locateURL(Class<?> clazz)
  {
    if (null == clazz) {
      return null;
    }
    ProtectionDomain pd = clazz.getProtectionDomain();
    if (null == pd) {
      return null;
    }
    CodeSource cs = pd.getCodeSource();
    if (null == cs) {
      return null;
    }
    return cs.getLocation();
  }
  
  public static Class<?> findLoadedClass(ClassLoader classLoader, String className)
  {
    if ((classLoader == null) || (className == null) || (method == null)) {
      return null;
    }
    try
    {
      Object obj = method.invoke(classLoader, new Object[] { className });
      if ((obj != null) && ((obj instanceof Class))) {
        return (Class)obj;
      }
    }
    catch (Exception ex) {}
    return null;
  }
  
  public static Map<String, URL> locateURLs(URLClassLoader classLoader)
  {
    Map<String, URL> urls = new ConcurrentHashMap();
    if (null != classLoader)
    {
      URL[] _urls = classLoader.getURLs();
      for (URL _url : _urls)
      {
        String path = _url.getFile();
        if (path.contains(WEB_INF_LIB)) {
          urls.put(path, _url);
        }
      }
    }
    return urls;
  }
  
  public static URL[] createClassPath(File file)
    throws Exception
  {
    URL[] urls = null;
    if ((file.exists()) && (file.isDirectory()))
    {
      String[] jars = file.list(new FilenameFilter()
      {
        public boolean accept(File dir, String name)
        {
          if (name.endsWith(".jar")) {
            return true;
          }
          return false;
        }
      });
      if (jars.length != 0)
      {
        urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
          urls[i] = new File(file.getAbsolutePath(), jars[i]).toURI().toURL();
        }
      }
    }
    else if ((file.exists()) && (!file.isDirectory()))
    {
      if (file.getName().endsWith(".jar")) {
        urls = new URL[] { file.toURI().toURL() };
      }
    }
    return urls;
  }
  
  public ClassLoaderUtils() {}
}

