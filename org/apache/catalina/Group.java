package org.apache.catalina;

import java.security.Principal;
import java.util.Iterator;

public abstract interface Group
  extends Principal
{
  public abstract String getDescription();
  
  public abstract void setDescription(String paramString);
  
  public abstract String getGroupname();
  
  public abstract void setGroupname(String paramString);
  
  public abstract Iterator<Role> getRoles();
  
  public abstract UserDatabase getUserDatabase();
  
  public abstract Iterator<User> getUsers();
  
  public abstract void addRole(Role paramRole);
  
  public abstract boolean isInRole(Role paramRole);
  
  public abstract void removeRole(Role paramRole);
  
  public abstract void removeRoles();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Group.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */