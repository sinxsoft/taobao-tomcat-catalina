package org.apache.catalina;

import javax.naming.Context;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.startup.Catalina;

public abstract interface Server
  extends Lifecycle
{
  public abstract String getInfo();
  
  public abstract NamingResources getGlobalNamingResources();
  
  public abstract void setGlobalNamingResources(NamingResources paramNamingResources);
  
  public abstract Context getGlobalNamingContext();
  
  public abstract int getPort();
  
  public abstract void setPort(int paramInt);
  
  public abstract String getAddress();
  
  public abstract void setAddress(String paramString);
  
  public abstract String getShutdown();
  
  public abstract void setShutdown(String paramString);
  
  public abstract ClassLoader getParentClassLoader();
  
  public abstract void setParentClassLoader(ClassLoader paramClassLoader);
  
  public abstract Catalina getCatalina();
  
  public abstract void setCatalina(Catalina paramCatalina);
  
  public abstract void addService(Service paramService);
  
  public abstract void await();
  
  public abstract Service findService(String paramString);
  
  public abstract Service[] findServices();
  
  public abstract void removeService(Service paramService);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Server.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */