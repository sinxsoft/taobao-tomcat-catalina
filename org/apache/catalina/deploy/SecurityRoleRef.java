package org.apache.catalina.deploy;

public class SecurityRoleRef
{
  private String name = null;
  
  public SecurityRoleRef() {}
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  private String link = null;
  
  public String getLink()
  {
    return this.link;
  }
  
  public void setLink(String link)
  {
    this.link = link;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("SecurityRoleRef[");
    sb.append("name=");
    sb.append(this.name);
    if (this.link != null)
    {
      sb.append(", link=");
      sb.append(this.link);
    }
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\SecurityRoleRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */