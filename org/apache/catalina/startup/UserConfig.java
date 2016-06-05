package org.apache.catalina.startup;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public final class UserConfig
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(UserConfig.class);
  private String configClass;
  private String contextClass;
  private String directoryName;
  private String homeBase;
  private Host host;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  private String userClass;
  protected Pattern allow;
  protected Pattern deny;
  
  public UserConfig()
  {
    this.configClass = "org.apache.catalina.startup.ContextConfig";
    
    this.contextClass = "org.apache.catalina.core.StandardContext";
    
    this.directoryName = "public_html";
    
    this.homeBase = null;
    
    this.host = null;
    
    this.userClass = "org.apache.catalina.startup.PasswdUserDatabase";
    
    this.allow = null;
    
    this.deny = null;
  }
  
  public String getConfigClass()
  {
    return this.configClass;
  }
  
  public void setConfigClass(String configClass)
  {
    this.configClass = configClass;
  }
  
  public String getContextClass()
  {
    return this.contextClass;
  }
  
  public void setContextClass(String contextClass)
  {
    this.contextClass = contextClass;
  }
  
  public String getDirectoryName()
  {
    return this.directoryName;
  }
  
  public void setDirectoryName(String directoryName)
  {
    this.directoryName = directoryName;
  }
  
  public String getHomeBase()
  {
    return this.homeBase;
  }
  
  public void setHomeBase(String homeBase)
  {
    this.homeBase = homeBase;
  }
  
  public String getUserClass()
  {
    return this.userClass;
  }
  
  public void setUserClass(String userClass)
  {
    this.userClass = userClass;
  }
  
  public String getAllow()
  {
    if (this.allow == null) {
      return null;
    }
    return this.allow.toString();
  }
  
  public void setAllow(String allow)
  {
    if ((allow == null) || (allow.length() == 0)) {
      this.allow = null;
    } else {
      this.allow = Pattern.compile(allow);
    }
  }
  
  public String getDeny()
  {
    if (this.deny == null) {
      return null;
    }
    return this.deny.toString();
  }
  
  public void setDeny(String deny)
  {
    if ((deny == null) || (deny.length() == 0)) {
      this.deny = null;
    } else {
      this.deny = Pattern.compile(deny);
    }
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      this.host = ((Host)event.getLifecycle());
    }
    catch (ClassCastException e)
    {
      log.error(sm.getString("hostConfig.cce", new Object[] { event.getLifecycle() }), e);
      return;
    }
    if (event.getType().equals("start")) {
      start();
    } else if (event.getType().equals("stop")) {
      stop();
    }
  }
  
  private void deploy()
  {
    if (this.host.getLogger().isDebugEnabled()) {
      this.host.getLogger().debug(sm.getString("userConfig.deploying"));
    }
    UserDatabase database = null;
    try
    {
      Class<?> clazz = Class.forName(this.userClass);
      database = (UserDatabase)clazz.newInstance();
      database.setUserConfig(this);
    }
    catch (Exception e)
    {
      this.host.getLogger().error(sm.getString("userConfig.database"), e);
      return;
    }
    ExecutorService executor = this.host.getStartStopExecutor();
    List<Future<?>> results = new ArrayList();
    
    Enumeration<String> users = database.getUsers();
    while (users.hasMoreElements())
    {
      String user = (String)users.nextElement();
      if (isDeployAllowed(user))
      {
        String home = database.getHome(user);
        results.add(executor.submit(new DeployUserDirectory(this, user, home)));
      }
    }
    for (Future<?> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        this.host.getLogger().error(sm.getString("userConfig.deploy.threaded.error"), e);
      }
    }
  }
  
  private void deploy(String user, String home)
  {
    String contextPath = "/~" + user;
    if (this.host.findChild(contextPath) != null) {
      return;
    }
    File app = new File(home, this.directoryName);
    if ((!app.exists()) || (!app.isDirectory())) {
      return;
    }
    this.host.getLogger().info(sm.getString("userConfig.deploy", new Object[] { user }));
    try
    {
      Class<?> clazz = Class.forName(this.contextClass);
      Context context = (Context)clazz.newInstance();
      
      context.setPath(contextPath);
      context.setDocBase(app.toString());
      clazz = Class.forName(this.configClass);
      LifecycleListener listener = (LifecycleListener)clazz.newInstance();
      
      context.addLifecycleListener(listener);
      this.host.addChild(context);
    }
    catch (Exception e)
    {
      this.host.getLogger().error(sm.getString("userConfig.error", new Object[] { user }), e);
    }
  }
  
  private void start()
  {
    if (this.host.getLogger().isDebugEnabled()) {
      this.host.getLogger().debug(sm.getString("userConfig.start"));
    }
    deploy();
  }
  
  private void stop()
  {
    if (this.host.getLogger().isDebugEnabled()) {
      this.host.getLogger().debug(sm.getString("userConfig.stop"));
    }
  }
  
  private boolean isDeployAllowed(String user)
  {
    if ((this.deny != null) && (this.deny.matcher(user).matches())) {
      return false;
    }
    if (this.allow != null)
    {
      if (this.allow.matcher(user).matches()) {
        return true;
      }
      return false;
    }
    return true;
  }
  
  private static class DeployUserDirectory
    implements Runnable
  {
    private UserConfig config;
    private String user;
    private String home;
    
    public DeployUserDirectory(UserConfig config, String user, String home)
    {
      this.config = config;
      this.user = user;
      this.home = home;
    }
    
    public void run()
    {
      this.config.deploy(this.user, this.home);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\UserConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */