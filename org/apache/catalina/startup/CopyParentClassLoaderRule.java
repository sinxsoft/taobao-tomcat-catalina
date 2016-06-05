package org.apache.catalina.startup;

import java.lang.reflect.Method;
import org.apache.catalina.Container;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public class CopyParentClassLoaderRule
  extends Rule
{
  public CopyParentClassLoaderRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("Copying parent class loader");
    }
    Container child = (Container)this.digester.peek(0);
    Object parent = this.digester.peek(1);
    Method method = parent.getClass().getMethod("getParentClassLoader", new Class[0]);
    
    ClassLoader classLoader = (ClassLoader)method.invoke(parent, new Object[0]);
    
    child.setParentClassLoader(classLoader);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\CopyParentClassLoaderRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */