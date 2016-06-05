package org.apache.catalina.startup;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public class SetContextPropertiesRule
  extends Rule
{
  public SetContextPropertiesRule() {}
  
  public void begin(String namespace, String nameX, Attributes attributes)
    throws Exception
  {
    for (int i = 0; i < attributes.getLength(); i++)
    {
      String name = attributes.getLocalName(i);
      if ("".equals(name)) {
        name = attributes.getQName(i);
      }
      if ((!"path".equals(name)) && (!"docBase".equals(name)))
      {
        String value = attributes.getValue(i);
        if ((!this.digester.isFakeAttribute(this.digester.peek(), name)) && (!IntrospectionUtils.setProperty(this.digester.peek(), name, value)) && (this.digester.getRulesValidation())) {
          this.digester.getLogger().warn("[SetContextPropertiesRule]{" + this.digester.getMatch() + "} Setting property '" + name + "' to '" + value + "' did not find a matching property.");
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetContextPropertiesRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */