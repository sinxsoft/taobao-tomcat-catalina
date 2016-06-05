package com.taobao.catalina.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.xml.sax.Attributes;

public class HostCreateRule
  extends ObjectCreateRule
{
  public HostCreateRule(String className, String attributeName)
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
    realClassName = mapping(realClassName);
    if (realClassName == null) {
      throw new NullPointerException("No class name specified for " + namespace + " " + name);
    }
    Class<?> clazz = this.digester.getClassLoader().loadClass(realClassName);
    Object instance = clazz.newInstance();
    
    ArrayList<String> attrs = new ArrayList((Collection)this.digester.getFakeAttributes().get(Object.class));
    attrs.add("hostConfigClass");
    this.digester.getFakeAttributes().put(clazz, attrs);
    
    this.digester.push(instance);
  }
  
  private String mapping(String className)
  {
    Log logger = this.digester.getLogger();
    if (className.equals("com.taobao.tomcat.deploy.TomcatHost"))
    {
      logger.warn("found <Host className=\"com.taobao.tomcat.deploy.TomcatHost\" ...> in server.xml, ignore");
      return "org.apache.catalina.core.StandardHost";
    }
    return className;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\catalina\startup\HostCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */