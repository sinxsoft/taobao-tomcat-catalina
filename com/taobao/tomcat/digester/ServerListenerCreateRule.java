package com.taobao.tomcat.digester;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.xml.sax.Attributes;

public class ServerListenerCreateRule
  extends ObjectCreateRule
{
  private boolean objectCreate = true;
  
  public ServerListenerCreateRule(String className, String attributeName)
  {
    super(className, attributeName);
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    String realClassName = this.className;
    if (this.attributeName != null)
    {
      String value = attributes.getValue(this.attributeName);
      if (value != null) {
        realClassName = value;
      }
    }
    if (realClassName == null) {
      throw new NullPointerException("No class name specified for " + namespace + " " + name);
    }
    if (shouldIgnore(realClassName))
    {
      this.objectCreate = false;
    }
    else
    {
      Class<?> clazz = this.digester.getClassLoader().loadClass(realClassName);
      Object instance = clazz.newInstance();
      this.digester.push(instance);
      this.objectCreate = true;
    }
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    if (this.objectCreate) {
      this.digester.pop();
    }
  }
  
  private boolean shouldIgnore(String className)
  {
    Log logger = this.digester.getLogger();
    if (className.equals("com.alibaba.taobao.tomcat.monitor.MonitorServiceListener"))
    {
      logger.warn("found <Listener className=\"com.alibaba.taobao.tomcat.monitor.MonitorServiceListener\"/> in server.xml, ignore");
      
      return true;
    }
    if (className.equals("com.taobao.tomcat.monitor.MonitorServiceListener"))
    {
      logger.warn("found <Listener className=\"com.taobao.tomcat.monitor.MonitorServiceListener\"/> in server.xml, ignore");
      
      return true;
    }
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\digester\ServerListenerCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */