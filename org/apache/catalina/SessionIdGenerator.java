package org.apache.catalina;

public abstract interface SessionIdGenerator
{
  public abstract String getJvmRoute();
  
  public abstract void setJvmRoute(String paramString);
  
  public abstract int getSessionIdLength();
  
  public abstract void setSessionIdLength(int paramInt);
  
  public abstract String generateSessionId();
  
  public abstract String generateSessionId(String paramString);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\SessionIdGenerator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */