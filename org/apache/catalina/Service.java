package org.apache.catalina;

import org.apache.catalina.connector.Connector;

public abstract interface Service
  extends Lifecycle
{
  public abstract Container getContainer();
  
  public abstract void setContainer(Container paramContainer);
  
  public abstract String getInfo();
  
  public abstract String getName();
  
  public abstract void setName(String paramString);
  
  public abstract Server getServer();
  
  public abstract void setServer(Server paramServer);
  
  public abstract ClassLoader getParentClassLoader();
  
  public abstract void setParentClassLoader(ClassLoader paramClassLoader);
  
  public abstract void addConnector(Connector paramConnector);
  
  public abstract Connector[] findConnectors();
  
  public abstract void removeConnector(Connector paramConnector);
  
  public abstract void addExecutor(Executor paramExecutor);
  
  public abstract Executor[] findExecutors();
  
  public abstract Executor getExecutor(String paramString);
  
  public abstract void removeExecutor(Executor paramExecutor);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Service.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */