package org.apache.catalina.startup;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.NamingResources;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;

public class SetNextNamingRule
  extends Rule
{
  public SetNextNamingRule(String methodName, String paramType)
  {
    this.methodName = methodName;
    this.paramType = paramType;
  }
  
  protected String methodName = null;
  protected String paramType = null;
  
  public void end(String namespace, String name)
    throws Exception
  {
    Object child = this.digester.peek(0);
    Object parent = this.digester.peek(1);
    
    NamingResources namingResources = null;
    if ((parent instanceof Context)) {
      namingResources = ((Context)parent).getNamingResources();
    } else {
      namingResources = (NamingResources)parent;
    }
    IntrospectionUtils.callMethod1(namingResources, this.methodName, child, this.paramType, this.digester.getClassLoader());
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("SetNextRule[");
    sb.append("methodName=");
    sb.append(this.methodName);
    sb.append(", paramType=");
    sb.append(this.paramType);
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetNextNamingRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */