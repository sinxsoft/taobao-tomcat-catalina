package org.apache.catalina.deploy;

import java.util.List;

public abstract interface Injectable
{
  public abstract String getName();
  
  public abstract void addInjectionTarget(String paramString1, String paramString2);
  
  public abstract List<InjectionTarget> getInjectionTargets();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\Injectable.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */