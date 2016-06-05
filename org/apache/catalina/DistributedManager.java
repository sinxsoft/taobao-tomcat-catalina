package org.apache.catalina;

import java.util.Set;

public abstract interface DistributedManager
{
  public abstract int getActiveSessionsFull();
  
  public abstract Set<String> getSessionIdsFull();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\DistributedManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */