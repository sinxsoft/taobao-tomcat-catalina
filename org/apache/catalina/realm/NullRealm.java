package org.apache.catalina.realm;

import java.security.Principal;

public class NullRealm
  extends RealmBase
{
  private static final String NAME = "NullRealm";
  
  public NullRealm() {}
  
  protected String getName()
  {
    return "NullRealm";
  }
  
  protected String getPassword(String username)
  {
    return null;
  }
  
  protected Principal getPrincipal(String username)
  {
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\NullRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */