package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class VersionRule
  extends Rule
{
  public VersionRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    WebXml webxml = (WebXml)this.digester.peek(this.digester.getCount() - 1);
    webxml.setVersion(attributes.getValue("version"));
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug(webxml.getClass().getName() + ".setVersion( " + webxml.getVersion() + ")");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\VersionRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */