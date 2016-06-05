package com.taobao.catalina.startup;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public class HostConfigRule
  extends Rule
{
  private String attributeName;
  private String listenerClass;
  
  public HostConfigRule(String listenerClass, String attributeName)
  {
    this.listenerClass = listenerClass;
    this.attributeName = attributeName;
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    Container c = (Container)this.digester.peek();
    Container p = null;
    Object obj = this.digester.peek(1);
    if ((obj instanceof Container)) {
      p = (Container)obj;
    }
    String className = null;
    if (this.attributeName != null)
    {
      String value = attributes.getValue(this.attributeName);
      if (value != null) {
        className = value;
      }
    }
    if ((p != null) && (className == null))
    {
      String configClass = (String)IntrospectionUtils.getProperty(p, this.attributeName);
      if ((configClass != null) && (configClass.length() > 0)) {
        className = configClass;
      }
    }
    if (className == null) {
      className = this.listenerClass;
    }
    Class<?> clazz = Class.forName(mapping(className));
    LifecycleListener listener = (LifecycleListener)clazz.newInstance();
    
    c.addLifecycleListener(listener);
  }
  
  private String mapping(String className)
  {
    Log logger = this.digester.getLogger();
    if ("com.taobao.tomcat.deploy.TomcatHostConfig".equals(className))
    {
      logger.warn("found <Host ... hostConfigClass=\"com.taobao.tomcat.deploy.TomcatHostConfig\"> in server.xml, ignore");
      
      return "org.apache.catalina.startup.HostConfig";
    }
    if ("com.taobao.tomcat.container.host.AliHostConfig".equals(className))
    {
      this.digester.getLogger().warn("found <Host ... hostConfigClass=\"com.taobao.tomcat.container.host.AliHostConfig\"> in server.xml, ignore");
      
      return "org.apache.catalina.startup.HostConfig";
    }
    return className;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\catalina\startup\HostConfigRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */