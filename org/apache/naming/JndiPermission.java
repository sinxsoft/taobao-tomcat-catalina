package org.apache.naming;

import java.security.BasicPermission;

public final class JndiPermission
  extends BasicPermission
{
  private static final long serialVersionUID = 1L;
  
  public JndiPermission(String name)
  {
    super(name);
  }
  
  public JndiPermission(String name, String actions)
  {
    super(name, actions);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\JndiPermission.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */