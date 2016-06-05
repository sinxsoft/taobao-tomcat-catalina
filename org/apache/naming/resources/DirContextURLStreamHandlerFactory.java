package org.apache.naming.resources;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirContextURLStreamHandlerFactory
  implements URLStreamHandlerFactory
{
  private static DirContextURLStreamHandlerFactory instance = new DirContextURLStreamHandlerFactory();
  
  public static DirContextURLStreamHandlerFactory getInstance()
  {
    return instance;
  }
  
  public static void addUserFactory(URLStreamHandlerFactory factory)
  {
    instance.userFactories.add(factory);
  }
  
  private List<URLStreamHandlerFactory> userFactories = new CopyOnWriteArrayList();
  
  private DirContextURLStreamHandlerFactory() {}
  
  public URLStreamHandler createURLStreamHandler(String protocol)
  {
    if (protocol.equals("jndi")) {
      return new DirContextURLStreamHandler();
    }
    for (URLStreamHandlerFactory factory : this.userFactories)
    {
      URLStreamHandler handler = factory.createURLStreamHandler(protocol);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\DirContextURLStreamHandlerFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */