package org.apache.catalina.loader;

import java.net.URL;
import java.net.URLClassLoader;

@Deprecated
public class StandardClassLoader
  extends URLClassLoader
  implements StandardClassLoaderMBean
{
  public StandardClassLoader(URL[] repositories)
  {
    super(repositories);
  }
  
  public StandardClassLoader(URL[] repositories, ClassLoader parent)
  {
    super(repositories, parent);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\StandardClassLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */