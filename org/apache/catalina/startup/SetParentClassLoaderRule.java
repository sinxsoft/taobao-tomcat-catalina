package org.apache.catalina.startup;

import org.apache.catalina.Container;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetParentClassLoaderRule
  extends Rule
{
  public SetParentClassLoaderRule(ClassLoader parentClassLoader)
  {
    this.parentClassLoader = parentClassLoader;
  }
  
  ClassLoader parentClassLoader = null;
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("Setting parent class loader");
    }
    Container top = (Container)this.digester.peek();
    top.setParentClassLoader(this.parentClassLoader);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetParentClassLoaderRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */