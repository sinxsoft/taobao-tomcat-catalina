package org.apache.catalina.startup;

import java.util.Enumeration;

public abstract interface UserDatabase
{
  public abstract UserConfig getUserConfig();
  
  public abstract void setUserConfig(UserConfig paramUserConfig);
  
  public abstract String getHome(String paramString);
  
  public abstract Enumeration<String> getUsers();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\UserDatabase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */