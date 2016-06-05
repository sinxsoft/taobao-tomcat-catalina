package org.apache.catalina;

public abstract interface Engine
  extends Container
{
  public abstract String getDefaultHost();
  
  public abstract void setDefaultHost(String paramString);
  
  public abstract String getJvmRoute();
  
  public abstract void setJvmRoute(String paramString);
  
  public abstract Service getService();
  
  public abstract void setService(Service paramService);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Engine.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */