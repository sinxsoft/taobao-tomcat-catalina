package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.apache.tomcat.util.res.StringManager;
import org.xml.sax.Attributes;

final class NameRule
  extends Rule
{
  protected boolean isNameSet = false;
  
  public NameRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    if (this.isNameSet) {
      throw new IllegalArgumentException(WebRuleSet.sm.getString("webRuleSet.nameCount"));
    }
    this.isNameSet = true;
  }
  
  public void body(String namespace, String name, String text)
    throws Exception
  {
    super.body(namespace, name, text);
    ((WebXml)this.digester.peek()).setName(text);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\NameRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */