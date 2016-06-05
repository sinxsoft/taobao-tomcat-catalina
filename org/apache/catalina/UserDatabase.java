package org.apache.catalina;

import java.util.Iterator;

public abstract interface UserDatabase
{
  public abstract Iterator<Group> getGroups();
  
  public abstract String getId();
  
  public abstract Iterator<Role> getRoles();
  
  public abstract Iterator<User> getUsers();
  
  public abstract void close()
    throws Exception;
  
  public abstract Group createGroup(String paramString1, String paramString2);
  
  public abstract Role createRole(String paramString1, String paramString2);
  
  public abstract User createUser(String paramString1, String paramString2, String paramString3);
  
  public abstract Group findGroup(String paramString);
  
  public abstract Role findRole(String paramString);
  
  public abstract User findUser(String paramString);
  
  public abstract void open()
    throws Exception;
  
  public abstract void removeGroup(Group paramGroup);
  
  public abstract void removeRole(Role paramRole);
  
  public abstract void removeUser(User paramUser);
  
  public abstract void save()
    throws Exception;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\UserDatabase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */