package org.apache.catalina.deploy;

public class ContextEjb
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String home = null;
  
  public ContextEjb() {}
  
  public String getHome()
  {
    return this.home;
  }
  
  public void setHome(String home)
  {
    this.home = home;
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
  
  private String remote = null;
  
  public String getRemote()
  {
    return this.remote;
  }
  
  public void setRemote(String remote)
  {
    this.remote = remote;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextEjb[");
    sb.append("name=");
    sb.append(getName());
    if (getDescription() != null)
    {
      sb.append(", description=");
      sb.append(getDescription());
    }
    if (getType() != null)
    {
      sb.append(", type=");
      sb.append(getType());
    }
    if (this.home != null)
    {
      sb.append(", home=");
      sb.append(this.home);
    }
    if (this.remote != null)
    {
      sb.append(", remote=");
      sb.append(this.remote);
    }
    if (this.link != null)
    {
      sb.append(", link=");
      sb.append(this.link);
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.home == null ? 0 : this.home.hashCode());
    result = 31 * result + (this.link == null ? 0 : this.link.hashCode());
    result = 31 * result + (this.remote == null ? 0 : this.remote.hashCode());
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ContextEjb other = (ContextEjb)obj;
    if (this.home == null)
    {
      if (other.home != null) {
        return false;
      }
    }
    else if (!this.home.equals(other.home)) {
      return false;
    }
    if (this.link == null)
    {
      if (other.link != null) {
        return false;
      }
    }
    else if (!this.link.equals(other.link)) {
      return false;
    }
    if (this.remote == null)
    {
      if (other.remote != null) {
        return false;
      }
    }
    else if (!this.remote.equals(other.remote)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextEjb.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */