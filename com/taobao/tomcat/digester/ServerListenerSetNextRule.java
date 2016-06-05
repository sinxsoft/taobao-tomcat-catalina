package com.taobao.tomcat.digester;

import org.apache.catalina.LifecycleListener;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.SetNextRule;

public class ServerListenerSetNextRule
  extends SetNextRule
{
  public ServerListenerSetNextRule(String methodName, String paramType)
  {
    super(methodName, paramType);
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    Object child = this.digester.peek(0);
    if ((child instanceof LifecycleListener))
    {
      Object parent = this.digester.peek(1);
      
      IntrospectionUtils.callMethod1(parent, this.methodName, child, this.paramType, this.digester.getClassLoader());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\digester\ServerListenerSetNextRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */