package com.taobao.catalina.startup;

import java.lang.reflect.Method;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.xml.sax.Attributes;

public class LoaderCreateRule
  extends ObjectCreateRule
{
  public LoaderCreateRule(String className, String attributeName)
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
    Class<?> clazz = this.digester.getClassLoader().loadClass(mapping(realClassName));
    Object instance = clazz.newInstance();
    Method setLoaderClassMethod = clazz.getMethod("setLoaderClass", new Class[] { String.class });
    setLoaderClassMethod.invoke(instance, new Object[] { "com.taobao.tomcat.container.context.loader.AliWebappClassLoader" });
    this.digester.push(instance);
  }
  
  private String mapping(String className)
  {
    if (className.equals("com.taobao.tomcat.classloader.TomcatWebAppLoader"))
    {
      Log logger = this.digester.getLogger();
      logger.warn("found <Loader className=\"com.taobao.tomcat.classloader.TomcatWebAppLoader\"/> in context.xml, ignore");
      
      return "org.apache.catalina.loader.WebappLoader";
    }
    return className;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\catalina\startup\LoaderCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */