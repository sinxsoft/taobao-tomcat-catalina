package org.apache.catalina;

public abstract interface Cluster
{
  public abstract String getInfo();
  
  public abstract String getClusterName();
  
  public abstract void setClusterName(String paramString);
  
  public abstract void setContainer(Container paramContainer);
  
  public abstract Container getContainer();
  
  @Deprecated
  public abstract void setProtocol(String paramString);
  
  @Deprecated
  public abstract String getProtocol();
  
  public abstract Manager createManager(String paramString);
  
  public abstract void registerManager(Manager paramManager);
  
  public abstract void removeManager(Manager paramManager);
  
  public abstract void backgroundProcess();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Cluster.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */