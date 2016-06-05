package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetDistributableRule
  extends Rule
{
  public SetDistributableRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    WebXml webXml = (WebXml)this.digester.peek();
    webXml.setDistributable(true);
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug(webXml.getClass().getName() + ".setDistributable(true)");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetDistributableRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */