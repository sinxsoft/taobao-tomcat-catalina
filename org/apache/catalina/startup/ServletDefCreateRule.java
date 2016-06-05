package org.apache.catalina.startup;

import org.apache.catalina.deploy.ServletDef;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class ServletDefCreateRule
  extends Rule
{
  public ServletDefCreateRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    ServletDef servletDef = new ServletDef();
    this.digester.push(servletDef);
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("new " + servletDef.getClass().getName());
    }
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    ServletDef servletDef = (ServletDef)this.digester.pop();
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("pop " + servletDef.getClass().getName());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\ServletDefCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */