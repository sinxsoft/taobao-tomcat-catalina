package org.apache.catalina.startup;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.catalina.security.SecurityClassLoad;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public final class Bootstrap
{
  private static final Log log = LogFactory.getLog(Bootstrap.class);
  private static Bootstrap daemon = null;
  private Object catalinaDaemon = null;
  protected ClassLoader commonLoader = null;
  protected ClassLoader catalinaLoader = null;
  protected ClassLoader sharedLoader = null;
  
  public Bootstrap() {}
  
  private void initClassLoaders()
  {
    try
    {
      this.commonLoader = createClassLoader("common", getClass().getClassLoader());
      if (this.commonLoader == null) {
        this.commonLoader = getClass().getClassLoader();
      }
      this.catalinaLoader = createClassLoader("server", this.commonLoader);
      this.sharedLoader = createClassLoader("shared", this.commonLoader);
    }
    catch (Throwable t)
    {
      handleThrowable(t);
      log.error("Class loader creation threw exception", t);
      System.exit(1);
    }
  }
  
  private ClassLoader createClassLoader(String name, ClassLoader parent)
    throws Exception
  {
    String value = CatalinaProperties.getProperty(name + ".loader");
    if ((value == null) || (value.equals(""))) {
      return parent;
    }
    value = replace(value);
    
    List<ClassLoaderFactory.Repository> repositories = new ArrayList();
    
    StringTokenizer tokenizer = new StringTokenizer(value, ",");
    while (tokenizer.hasMoreElements())
    {
      String repository = tokenizer.nextToken().trim();
      if (repository.length() != 0) {
        try
        {
          URL url = new URL(repository);
          repositories.add(new ClassLoaderFactory.Repository(repository, ClassLoaderFactory.RepositoryType.URL));
        }
        catch (MalformedURLException e)
        {
          if (repository.endsWith("*.jar"))
          {
            repository = repository.substring(0, repository.length() - "*.jar".length());
            
            repositories.add(new ClassLoaderFactory.Repository(repository, ClassLoaderFactory.RepositoryType.GLOB));
          }
          else if (repository.endsWith(".jar"))
          {
            repositories.add(new ClassLoaderFactory.Repository(repository, ClassLoaderFactory.RepositoryType.JAR));
          }
          else
          {
            repositories.add(new ClassLoaderFactory.Repository(repository, ClassLoaderFactory.RepositoryType.DIR));
          }
        }
      }
    }
    ClassLoader classLoader = ClassLoaderFactory.createClassLoader(repositories, parent);
    
    MBeanServer mBeanServer = null;
    if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
      mBeanServer = (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
    } else {
      mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    ObjectName objectName = new ObjectName("Catalina:type=ServerClassLoader,name=" + name);
    
    mBeanServer.registerMBean(classLoader, objectName);
    
    return classLoader;
  }
  
  protected String replace(String str)
  {
    String result = str;
    int pos_start = str.indexOf("${");
    if (pos_start >= 0)
    {
      StringBuilder builder = new StringBuilder();
      int pos_end = -1;
      while (pos_start >= 0)
      {
        builder.append(str, pos_end + 1, pos_start);
        pos_end = str.indexOf('}', pos_start + 2);
        if (pos_end < 0)
        {
          pos_end = pos_start - 1;
          break;
        }
        String propName = str.substring(pos_start + 2, pos_end);
        String replacement;

        if (propName.length() == 0)
        {
          replacement = null;
        }
        else
        {

          if ("catalina.home".equals(propName))
          {
            replacement = getCatalinaHome();
          }
          else
          {

            if ("catalina.base".equals(propName)) {
              replacement = getCatalinaBase();
            } else {
              replacement = System.getProperty(propName);
            }
          }
        }
        if (replacement != null) {
          builder.append(replacement);
        } else {
          builder.append(str, pos_start, pos_end + 1);
        }
        pos_start = str.indexOf("${", pos_end + 1);
      }
      builder.append(str, pos_end + 1, str.length());
      result = builder.toString();
    }
    return result;
  }
  
  public void init()
    throws Exception
  {
    setCatalinaHome();
    setCatalinaBase();
    
    initClassLoaders();
    
    Thread.currentThread().setContextClassLoader(this.catalinaLoader);
    
    SecurityClassLoad.securityClassLoad(this.catalinaLoader);
    if (log.isDebugEnabled()) {
      log.debug("Loading startup class");
    }
    Class<?> startupClass = this.catalinaLoader.loadClass("org.apache.catalina.startup.Catalina");
    
    Object startupInstance = startupClass.newInstance();
    if (log.isDebugEnabled()) {
      log.debug("Setting startup class properties");
    }
    String methodName = "setParentClassLoader";
    Class<?>[] paramTypes = new Class[1];
    paramTypes[0] = Class.forName("java.lang.ClassLoader");
    Object[] paramValues = new Object[1];
    paramValues[0] = this.sharedLoader;
    Method method = startupInstance.getClass().getMethod(methodName, paramTypes);
    
    method.invoke(startupInstance, paramValues);
    
    this.catalinaDaemon = startupInstance;
  }
  
  private void load(String[] arguments)
    throws Exception
  {
    String methodName = "load";
    Object[] param;
    Class<?>[] paramTypes;

    if ((arguments == null) || (arguments.length == 0))
    {
      paramTypes = null;
      param = null;
    }
    else
    {
      paramTypes = new Class[1];
      paramTypes[0] = arguments.getClass();
      param = new Object[1];
      param[0] = arguments;
    }
    Method method = this.catalinaDaemon.getClass().getMethod(methodName, paramTypes);
    if (log.isDebugEnabled()) {
      log.debug("Calling startup class " + method);
    }
    method.invoke(this.catalinaDaemon, param);
  }
  
  private Object getServer()
    throws Exception
  {
    String methodName = "getServer";
    Method method = this.catalinaDaemon.getClass().getMethod(methodName, new Class[0]);
    
    return method.invoke(this.catalinaDaemon, new Object[0]);
  }
  
  public void init(String[] arguments)
    throws Exception
  {
    init();
    load(arguments);
  }
  
  public void start()
    throws Exception
  {
    if (this.catalinaDaemon == null) {
      init();
    }
    Method method = this.catalinaDaemon.getClass().getMethod("start", (Class[])null);
    method.invoke(this.catalinaDaemon, (Object[])null);
  }
  
  public void stop()
    throws Exception
  {
    Method method = this.catalinaDaemon.getClass().getMethod("stop", (Class[])null);
    method.invoke(this.catalinaDaemon, (Object[])null);
  }
  
  public void stopServer()
    throws Exception
  {
    Method method = this.catalinaDaemon.getClass().getMethod("stopServer", (Class[])null);
    
    method.invoke(this.catalinaDaemon, (Object[])null);
  }
  
  public void stopServer(String[] arguments)
    throws Exception
  {
    Object[] param;
    Class<?>[] paramTypes;
    if ((arguments == null) || (arguments.length == 0))
    {
      paramTypes = null;
      param = null;
    }
    else
    {
      paramTypes = new Class[1];
      paramTypes[0] = arguments.getClass();
      param = new Object[1];
      param[0] = arguments;
    }
    Method method = this.catalinaDaemon.getClass().getMethod("stopServer", paramTypes);
    
    method.invoke(this.catalinaDaemon, param);
  }
  
  public void setAwait(boolean await)
    throws Exception
  {
    Class<?>[] paramTypes = new Class[1];
    paramTypes[0] = Boolean.TYPE;
    Object[] paramValues = new Object[1];
    paramValues[0] = Boolean.valueOf(await);
    Method method = this.catalinaDaemon.getClass().getMethod("setAwait", paramTypes);
    
    method.invoke(this.catalinaDaemon, paramValues);
  }
  
  public boolean getAwait()
    throws Exception
  {
    Class<?>[] paramTypes = new Class[0];
    Object[] paramValues = new Object[0];
    Method method = this.catalinaDaemon.getClass().getMethod("getAwait", paramTypes);
    
    Boolean b = (Boolean)method.invoke(this.catalinaDaemon, paramValues);
    return b.booleanValue();
  }
  
  public void destroy() {}
  
  public static void main(String[] args)
  {
    if (daemon == null)
    {
      Bootstrap bootstrap = new Bootstrap();
      try
      {
        bootstrap.init();
      }
      catch (Throwable t)
      {
        handleThrowable(t);
        t.printStackTrace();
        return;
      }
      daemon = bootstrap;
    }
    else
    {
      Thread.currentThread().setContextClassLoader(daemon.catalinaLoader);
    }
    try
    {
      String command = "start";
      if (args.length > 0) {
        command = args[(args.length - 1)];
      }
      if (command.equals("startd"))
      {
        args[(args.length - 1)] = "start";
        daemon.load(args);
        daemon.start();
      }
      else if (command.equals("stopd"))
      {
        args[(args.length - 1)] = "stop";
        daemon.stop();
      }
      else if (command.equals("start"))
      {
        daemon.setAwait(true);
        daemon.load(args);
        daemon.start();
      }
      else if (command.equals("stop"))
      {
        daemon.stopServer(args);
      }
      else if (command.equals("configtest"))
      {
        daemon.load(args);
        if (null == daemon.getServer()) {
          System.exit(1);
        }
        System.exit(0);
      }
      else
      {
        log.warn("Bootstrap: command \"" + command + "\" does not exist.");
      }
    }
    catch (Throwable t)
    {
      if (((t instanceof InvocationTargetException)) && (t.getCause() != null)) {
        t = t.getCause();
      }
      handleThrowable(t);
      t.printStackTrace();
      System.exit(1);
    }
  }
  
  public void setCatalinaHome(String s)
  {
    System.setProperty("catalina.home", s);
  }
  
  public void setCatalinaBase(String s)
  {
    System.setProperty("catalina.base", s);
  }
  
  public void setCatalinaLogs()
  {
    if (System.getProperty("catalina.logs") != null) {
      return;
    }
    String catalinaBase = getCatalinaBase();
    if (catalinaBase == null) {
      throw new RuntimeException("catalina.base not set");
    }
    String catalinaLogs = new File(catalinaBase, "logs").getPath();
    System.setProperty("catalina.logs", catalinaLogs);
  }
  
  private void setCatalinaBase()
  {
    if (System.getProperty("catalina.base") != null) {
      return;
    }
    if (System.getProperty("catalina.home") != null) {
      System.setProperty("catalina.base", System.getProperty("catalina.home"));
    } else {
      System.setProperty("catalina.base", System.getProperty("user.dir"));
    }
  }
  
  private void setCatalinaHome()
  {
    if (System.getProperty("catalina.home") != null) {
      return;
    }
    File bootstrapJar = new File(System.getProperty("user.dir"), "bootstrap.jar");
    if (bootstrapJar.exists()) {
      try
      {
        System.setProperty("catalina.home", new File(System.getProperty("user.dir"), "..").getCanonicalPath());
      }
      catch (Exception e)
      {
        System.setProperty("catalina.home", System.getProperty("user.dir"));
      }
    } else {
      System.setProperty("catalina.home", System.getProperty("user.dir"));
    }
  }
  
  public static String getCatalinaHome()
  {
    return System.getProperty("catalina.home", System.getProperty("user.dir"));
  }
  
  public static String getCatalinaBase()
  {
    return System.getProperty("catalina.base", getCatalinaHome());
  }
  
  private static void handleThrowable(Throwable t)
  {
    if ((t instanceof ThreadDeath)) {
      throw ((ThreadDeath)t);
    }
    if ((t instanceof VirtualMachineError)) {
      throw ((VirtualMachineError)t);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\Bootstrap.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */