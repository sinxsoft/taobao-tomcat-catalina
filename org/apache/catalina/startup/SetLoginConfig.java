package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetLoginConfig
  extends Rule
{
  protected boolean isLoginConfigSet = false;
  
  public SetLoginConfig() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    if (this.isLoginConfigSet) {
      throw new IllegalArgumentException("<login-config> element is limited to 1 occurrence");
    }
    this.isLoginConfigSet = true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetLoginConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */