package org.apache.catalina.deploy;

public class ContextResourceLink
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String global = null;
  private String factory = null;
  
  public ContextResourceLink() {}
  
  public String getGlobal()
  {
    return this.global;
  }
  
  public void setGlobal(String global)
  {
    this.global = global;
  }
  
  public String getFactory()
  {
    return this.factory;
  }
  
  public void setFactory(String factory)
  {
    this.factory = factory;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextResourceLink[");
    sb.append("name=");
    sb.append(getName());
    if (getType() != null)
    {
      sb.append(", type=");
      sb.append(getType());
    }
    if (getGlobal() != null)
    {
      sb.append(", global=");
      sb.append(getGlobal());
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.factory == null ? 0 : this.factory.hashCode());
    result = 31 * result + (this.global == null ? 0 : this.global.hashCode());
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
    ContextResourceLink other = (ContextResourceLink)obj;
    if (this.factory == null)
    {
      if (other.factory != null) {
        return false;
      }
    }
    else if (!this.factory.equals(other.factory)) {
      return false;
    }
    if (this.global == null)
    {
      if (other.global != null) {
        return false;
      }
    }
    else if (!this.global.equals(other.global)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextResourceLink.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */