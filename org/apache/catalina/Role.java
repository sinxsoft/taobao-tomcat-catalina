package org.apache.catalina;

import java.security.Principal;

public abstract interface Role
  extends Principal
{
  public abstract String getDescription();
  
  public abstract void setDescription(String paramString);
  
  public abstract String getRolename();
  
  public abstract void setRolename(String paramString);
  
  public abstract UserDatabase getUserDatabase();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Role.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */