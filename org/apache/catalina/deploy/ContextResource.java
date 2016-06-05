package org.apache.catalina.deploy;

public class ContextResource
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String auth = null;
  
  public ContextResource() {}
  
  public String getAuth()
  {
    return this.auth;
  }
  
  public void setAuth(String auth)
  {
    this.auth = auth;
  }
  
  private String scope = "Shareable";
  
  public String getScope()
  {
    return this.scope;
  }
  
  public void setScope(String scope)
  {
    this.scope = scope;
  }
  
  private boolean singleton = true;
  
  public boolean getSingleton()
  {
    return this.singleton;
  }
  
  public void setSingleton(boolean singleton)
  {
    this.singleton = singleton;
  }
  
  private String closeMethod = null;
  
  public String getCloseMethod()
  {
    return this.closeMethod;
  }
  
  public void setCloseMethod(String closeMethod)
  {
    this.closeMethod = closeMethod;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextResource[");
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
    if (this.auth != null)
    {
      sb.append(", auth=");
      sb.append(this.auth);
    }
    if (this.scope != null)
    {
      sb.append(", scope=");
      sb.append(this.scope);
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.auth == null ? 0 : this.auth.hashCode());
    result = 31 * result + (this.closeMethod == null ? 0 : this.closeMethod.hashCode());
    
    result = 31 * result + (this.scope == null ? 0 : this.scope.hashCode());
    result = 31 * result + (this.singleton ? 1231 : 1237);
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
    ContextResource other = (ContextResource)obj;
    if (this.auth == null)
    {
      if (other.auth != null) {
        return false;
      }
    }
    else if (!this.auth.equals(other.auth)) {
      return false;
    }
    if (this.closeMethod == null)
    {
      if (other.closeMethod != null) {
        return false;
      }
    }
    else if (!this.closeMethod.equals(other.closeMethod)) {
      return false;
    }
    if (this.scope == null)
    {
      if (other.scope != null) {
        return false;
      }
    }
    else if (!this.scope.equals(other.scope)) {
      return false;
    }
    if (this.singleton != other.singleton) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextResource.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */