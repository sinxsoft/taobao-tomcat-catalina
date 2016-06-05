package org.apache.catalina.startup;

import java.util.Map;
import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.CallMethodRule;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;

final class LifecycleCallbackRule
  extends CallMethodRule
{
  private final boolean postConstruct;
  
  public LifecycleCallbackRule(String methodName, int paramCount, boolean postConstruct)
  {
    super(methodName, paramCount);
    this.postConstruct = postConstruct;
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    Object[] params = (Object[])this.digester.peekParams();
    if ((params != null) && (params.length == 2))
    {
      WebXml webXml = (WebXml)this.digester.peek();
      if (this.postConstruct)
      {
        if (webXml.getPostConstructMethods().containsKey(params[0])) {
          throw new IllegalArgumentException(WebRuleSet.sm.getString("webRuleSet.postconstruct.duplicate", new Object[] { params[0] }));
        }
      }
      else if (webXml.getPreDestroyMethods().containsKey(params[0])) {
        throw new IllegalArgumentException(WebRuleSet.sm.getString("webRuleSet.predestroy.duplicate", new Object[] { params[0] }));
      }
    }
    super.end(namespace, name);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\LifecycleCallbackRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */