package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.apache.tomcat.util.res.StringManager;
import org.xml.sax.Attributes;

final class AbsoluteOrderingRule
  extends Rule
{
  protected boolean isAbsoluteOrderingSet = false;
  private final boolean fragment;
  
  public AbsoluteOrderingRule(boolean fragment)
  {
    this.fragment = fragment;
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    if (this.fragment) {
      this.digester.getLogger().warn(WebRuleSet.sm.getString("webRuleSet.absoluteOrdering"));
    }
    if (this.isAbsoluteOrderingSet) {
      throw new IllegalArgumentException(WebRuleSet.sm.getString("webRuleSet.absoluteOrderingCount"));
    }
    this.isAbsoluteOrderingSet = true;
    WebXml webXml = (WebXml)this.digester.peek();
    webXml.createAbsoluteOrdering();
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug(webXml.getClass().getName() + ".setAbsoluteOrdering()");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\AbsoluteOrderingRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */