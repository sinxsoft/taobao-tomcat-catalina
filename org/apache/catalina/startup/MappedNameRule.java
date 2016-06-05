package org.apache.catalina.startup;

import org.apache.catalina.deploy.ResourceBase;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;

final class MappedNameRule
  extends Rule
{
  public MappedNameRule() {}
  
  public void body(String namespace, String name, String text)
    throws Exception
  {
    ResourceBase resourceBase = (ResourceBase)this.digester.peek();
    resourceBase.setProperty("mappedName", text.trim());
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\MappedNameRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */