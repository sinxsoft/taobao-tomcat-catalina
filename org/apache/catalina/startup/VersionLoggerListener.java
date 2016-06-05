package org.apache.catalina.startup;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.ServerInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class VersionLoggerListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(VersionLoggerListener.class);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  private boolean logArgs = true;
  private boolean logEnv = false;
  private boolean logProps = false;
  
  public VersionLoggerListener() {}
  
  public boolean getLogArgs()
  {
    return this.logArgs;
  }
  
  public void setLogArgs(boolean logArgs)
  {
    this.logArgs = logArgs;
  }
  
  public boolean getLogEnv()
  {
    return this.logEnv;
  }
  
  public void setLogEnv(boolean logEnv)
  {
    this.logEnv = logEnv;
  }
  
  public boolean getLogProps()
  {
    return this.logProps;
  }
  
  public void setLogProps(boolean logProps)
  {
    this.logProps = logProps;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if ("before_init".equals(event.getType())) {
      log();
    }
  }
  
  private void log()
  {
    log.info(sm.getString("versionLoggerListener.serverInfo.server.version", new Object[] { ServerInfo.getServerInfo() }));
    
    log.info(sm.getString("versionLoggerListener.serverInfo.server.built", new Object[] { ServerInfo.getServerBuilt() }));
    
    log.info(sm.getString("versionLoggerListener.serverInfo.server.number", new Object[] { ServerInfo.getServerNumber() }));
    
    log.info(sm.getString("versionLoggerListener.os.name", new Object[] { System.getProperty("os.name") }));
    
    log.info(sm.getString("versionLoggerListener.os.version", new Object[] { System.getProperty("os.version") }));
    
    log.info(sm.getString("versionLoggerListener.os.arch", new Object[] { System.getProperty("os.arch") }));
    
    log.info(sm.getString("versionLoggerListener.java.home", new Object[] { System.getProperty("java.home") }));
    
    log.info(sm.getString("versionLoggerListener.vm.version", new Object[] { System.getProperty("java.runtime.version") }));
    
    log.info(sm.getString("versionLoggerListener.vm.vendor", new Object[] { System.getProperty("java.vm.vendor") }));
    
    log.info(sm.getString("versionLoggerListener.catalina.base", new Object[] { System.getProperty("catalina.base") }));
    
    log.info(sm.getString("versionLoggerListener.catalina.home", new Object[] { System.getProperty("catalina.home") }));
    if (this.logArgs)
    {
      List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
      for (String arg : args) {
        log.info(sm.getString("versionLoggerListener.arg", new Object[] { arg }));
      }
    }
    if (this.logEnv)
    {
      SortedMap<String, String> sortedMap = new TreeMap(System.getenv());
      for (Map.Entry<String, String> e : sortedMap.entrySet()) {
        log.info(sm.getString("versionLoggerListener.env", new Object[] { e.getKey(), e.getValue() }));
      }
    }
    if (this.logProps)
    {
      SortedMap<String, String> sortedMap = new TreeMap();
      for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
        sortedMap.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
      }
      for (Map.Entry<String, String> e : sortedMap.entrySet()) {
        log.info(sm.getString("versionLoggerListener.prop", new Object[] { e.getKey(), e.getValue() }));
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\VersionLoggerListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */