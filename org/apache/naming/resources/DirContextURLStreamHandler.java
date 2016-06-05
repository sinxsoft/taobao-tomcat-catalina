package org.apache.naming.resources;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;
import javax.naming.directory.DirContext;

public class DirContextURLStreamHandler
  extends URLStreamHandler
{
  public DirContextURLStreamHandler() {}
  
  public DirContextURLStreamHandler(DirContext context)
  {
    this.context = context;
  }
  
  private static Hashtable<ClassLoader, DirContext> clBindings = new Hashtable();
  private static Hashtable<Thread, DirContext> threadBindings = new Hashtable();
  protected DirContext context = null;
  
  protected URLConnection openConnection(URL u)
    throws IOException
  {
    DirContext currentContext = this.context;
    if (currentContext == null) {
      currentContext = get();
    }
    return new DirContextURLConnection(currentContext, u);
  }
  
  protected String toExternalForm(URL u)
  {
    int len = u.getProtocol().length() + 1;
    if (u.getPath() != null) {
      len += u.getPath().length();
    }
    if (u.getQuery() != null) {
      len += 1 + u.getQuery().length();
    }
    if (u.getRef() != null) {
      len += 1 + u.getRef().length();
    }
    StringBuilder result = new StringBuilder(len);
    result.append(u.getProtocol());
    result.append(":");
    if (u.getPath() != null) {
      result.append(u.getPath());
    }
    if (u.getQuery() != null)
    {
      result.append('?');
      result.append(u.getQuery());
    }
    if (u.getRef() != null)
    {
      result.append("#");
      result.append(u.getRef());
    }
    return result.toString();
  }
  
  public static void setProtocolHandler()
  {
    String value = System.getProperty("java.protocol.handler.pkgs");
    if (value == null)
    {
      value = "org.apache.naming.resources";
      System.setProperty("java.protocol.handler.pkgs", value);
    }
    else if (value.indexOf("org.apache.naming.resources") == -1)
    {
      value = value + "|org.apache.naming.resources";
      System.setProperty("java.protocol.handler.pkgs", value);
    }
  }
  
  public static boolean isBound()
  {
    return (clBindings.containsKey(Thread.currentThread().getContextClassLoader())) || (threadBindings.containsKey(Thread.currentThread()));
  }
  
  public static void bind(DirContext dirContext)
  {
    ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
    if (currentCL != null) {
      clBindings.put(currentCL, dirContext);
    }
  }
  
  public static void unbind()
  {
    ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
    if (currentCL != null) {
      clBindings.remove(currentCL);
    }
  }
  
  public static void bindThread(DirContext dirContext)
  {
    threadBindings.put(Thread.currentThread(), dirContext);
  }
  
  public static void unbindThread()
  {
    threadBindings.remove(Thread.currentThread());
  }
  
  public static DirContext get()
  {
    DirContext result = null;
    
    Thread currentThread = Thread.currentThread();
    ClassLoader currentCL = currentThread.getContextClassLoader();
    
    result = (DirContext)clBindings.get(currentCL);
    if (result != null) {
      return result;
    }
    result = (DirContext)threadBindings.get(currentThread);
    
    currentCL = currentCL.getParent();
    while (currentCL != null)
    {
      result = (DirContext)clBindings.get(currentCL);
      if (result != null) {
        return result;
      }
      currentCL = currentCL.getParent();
    }
    if (result == null) {
      throw new IllegalStateException("Illegal class loader binding");
    }
    return result;
  }
  
  public static void bind(ClassLoader cl, DirContext dirContext)
  {
    clBindings.put(cl, dirContext);
  }
  
  public static void unbind(ClassLoader cl)
  {
    clBindings.remove(cl);
  }
  
  public static DirContext get(ClassLoader cl)
  {
    return (DirContext)clBindings.get(cl);
  }
  
  public static DirContext get(Thread thread)
  {
    return (DirContext)threadBindings.get(thread);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\DirContextURLStreamHandler.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */