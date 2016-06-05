package org.apache.catalina.connector;

import java.io.Serializable;
import java.security.Principal;

public class CoyotePrincipal
  implements Principal, Serializable
{
  private static final long serialVersionUID = 1L;
  
  public CoyotePrincipal(String name)
  {
    this.name = name;
  }
  
  protected String name = null;
  
  public String getName()
  {
    return this.name;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("CoyotePrincipal[");
    sb.append(this.name);
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CoyotePrincipal.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */