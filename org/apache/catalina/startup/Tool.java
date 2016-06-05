package org.apache.catalina.startup;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;

public final class Tool
{
  private static final Log log = LogFactory.getLog(Tool.class);
  private static boolean ant = false;
  private static String catalinaHome = System.getProperty("catalina.home");
  private static boolean common = false;
  private static boolean server = false;
  private static boolean shared = false;
  
  public Tool() {}
  
  public static void main(String[] args)
  {
    if (catalinaHome == null)
    {
      log.error("Must set 'catalina.home' system property");
      System.exit(1);
    }
    int index = 0;
    for (;;)
    {
      if (index == args.length)
      {
        usage();
        System.exit(1);
      }
      if ("-ant".equals(args[index]))
      {
        ant = true;
      }
      else if ("-common".equals(args[index]))
      {
        common = true;
      }
      else if ("-server".equals(args[index]))
      {
        server = true;
      }
      else
      {
        if (!"-shared".equals(args[index])) {
          break;
        }
        shared = true;
      }
      index++;
    }
    if (index > args.length)
    {
      usage();
      System.exit(1);
    }
    if (ant) {
      System.setProperty("ant.home", catalinaHome);
    }
    ClassLoader classLoader = null;
    try
    {
      ArrayList<File> packed = new ArrayList();
      ArrayList<File> unpacked = new ArrayList();
      unpacked.add(new File(catalinaHome, "classes"));
      packed.add(new File(catalinaHome, "lib"));
      if (common)
      {
        unpacked.add(new File(catalinaHome, "common" + File.separator + "classes"));
        
        packed.add(new File(catalinaHome, "common" + File.separator + "lib"));
      }
      if (server)
      {
        unpacked.add(new File(catalinaHome, "server" + File.separator + "classes"));
        
        packed.add(new File(catalinaHome, "server" + File.separator + "lib"));
      }
      if (shared)
      {
        unpacked.add(new File(catalinaHome, "shared" + File.separator + "classes"));
        
        packed.add(new File(catalinaHome, "shared" + File.separator + "lib"));
      }
      classLoader = ClassLoaderFactory.createClassLoader((File[])unpacked.toArray(new File[0]), (File[])packed.toArray(new File[0]), null);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.error("Class loader creation threw exception", t);
      System.exit(1);
    }
    Thread.currentThread().setContextClassLoader(classLoader);
    
    Class<?> clazz = null;
    String className = args[(index++)];
    try
    {
      if (log.isDebugEnabled()) {
        log.debug("Loading application class " + className);
      }
      clazz = classLoader.loadClass(className);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.error("Exception creating instance of " + className, t);
      System.exit(1);
    }
    Method method = null;
    String[] params = new String[args.length - index];
    System.arraycopy(args, index, params, 0, params.length);
    try
    {
      if (log.isDebugEnabled()) {
        log.debug("Identifying main() method");
      }
      String methodName = "main";
      Class<?>[] paramTypes = new Class[1];
      paramTypes[0] = params.getClass();
      method = clazz.getMethod(methodName, paramTypes);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.error("Exception locating main() method", t);
      System.exit(1);
    }
    try
    {
      if (log.isDebugEnabled()) {
        log.debug("Calling main() method");
      }
      Object[] paramValues = new Object[1];
      paramValues[0] = params;
      method.invoke(null, paramValues);
    }
    catch (Throwable t)
    {
      t = ExceptionUtils.unwrapInvocationTargetException(t);
      ExceptionUtils.handleThrowable(t);
      log.error("Exception calling main() method", t);
      System.exit(1);
    }
  }
  
  private static void usage()
  {
    log.info("Usage:  java org.apache.catalina.startup.Tool [<options>] <class> [<arguments>]");
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\Tool.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */