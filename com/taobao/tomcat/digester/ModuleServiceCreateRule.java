package com.taobao.tomcat.digester;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.xml.sax.Attributes;

public class ModuleServiceCreateRule
  extends ObjectCreateRule
{
  public ModuleServiceCreateRule(String className, String attributeName)
  {
    super(className, attributeName);
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    Log logger = this.digester.getLogger();
    logger.warn("found <ModuleService .../> from server.xml, ignore");
  }
  
  public void end(String namespace, String name)
    throws Exception
  {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\digester\ModuleServiceCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */