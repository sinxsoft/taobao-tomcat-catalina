package org.apache.catalina.startup;

import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetOverrideRule
  extends Rule
{
  public SetOverrideRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    ContextEnvironment envEntry = (ContextEnvironment)this.digester.peek();
    envEntry.setOverride(false);
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug(envEntry.getClass().getName() + ".setOverride(false)");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetOverrideRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */