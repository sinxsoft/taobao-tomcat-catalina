package org.apache.catalina.startup;

import java.lang.reflect.Method;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetPublicIdRule
  extends Rule
{
  public SetPublicIdRule(String method)
  {
    this.method = method;
  }
  
  private String method = null;
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    Object top = this.digester.peek();
    Class<?>[] paramClasses = new Class[1];
    paramClasses[0] = "String".getClass();
    String[] paramValues = new String[1];
    paramValues[0] = this.digester.getPublicId();
    
    Method m = null;
    try
    {
      m = top.getClass().getMethod(this.method, paramClasses);
    }
    catch (NoSuchMethodException e)
    {
      this.digester.getLogger().error("Can't find method " + this.method + " in " + top + " CLASS " + top.getClass());
      
      return;
    }
    m.invoke(top, (Object[])paramValues);
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("" + top.getClass().getName() + "." + this.method + "(" + paramValues[0] + ")");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetPublicIdRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */