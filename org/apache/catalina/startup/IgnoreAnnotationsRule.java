package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class IgnoreAnnotationsRule
  extends Rule
{
  public IgnoreAnnotationsRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    WebXml webxml = (WebXml)this.digester.peek(this.digester.getCount() - 1);
    String value = attributes.getValue("metadata-complete");
    if ("true".equals(value)) {
      webxml.setMetadataComplete(true);
    } else if ("false".equals(value)) {
      webxml.setMetadataComplete(false);
    }
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug(webxml.getClass().getName() + ".setMetadataComplete( " + webxml.isMetadataComplete() + ")");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\IgnoreAnnotationsRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */