package com.taobao.catalina.startup;

import java.lang.reflect.Method;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.xml.sax.Attributes;

public class PandoraManagerCreateRule
  extends ObjectCreateRule
{
  public PandoraManagerCreateRule(String prefix, String className, String attributeName)
  {
    super(className, attributeName);
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    StandardContext context = (StandardContext)this.digester.peek();
    Class<?> clazz = this.digester.getClassLoader().loadClass(this.className);
    Object instance = clazz.newInstance();
    Method setName = clazz.getMethod("setName", new Class[] { String.class });
    
    setName.invoke(instance, new Object[] { "Pandora[" + context.getName() + "]" });
    Method setContext = clazz.getMethod("setContext", new Class[] { StandardContext.class });
    setContext.invoke(instance, new Object[] { context });
    
    String initValue = attributes.getValue("init");
    boolean init = (initValue == null) || (Boolean.parseBoolean(initValue));
    Method setInit = clazz.getMethod("setInit", new Class[] { Boolean.TYPE });
    setInit.invoke(instance, new Object[] { Boolean.valueOf(init) });
    
    this.digester.push(instance);
  }
  
  public void end(String namespace, String names)
    throws Exception
  {
    this.digester.pop();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\catalina\startup\PandoraManagerCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */