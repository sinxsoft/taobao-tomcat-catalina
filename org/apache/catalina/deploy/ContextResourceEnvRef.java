package org.apache.catalina.deploy;

public class ContextResourceEnvRef
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private boolean override = true;
  
  public ContextResourceEnvRef() {}
  
  public boolean getOverride()
  {
    return this.override;
  }
  
  public void setOverride(boolean override)
  {
    this.override = override;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextResourceEnvRef[");
    sb.append("name=");
    sb.append(getName());
    if (getType() != null)
    {
      sb.append(", type=");
      sb.append(getType());
    }
    sb.append(", override=");
    sb.append(this.override);
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.override ? 1231 : 1237);
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
    ContextResourceEnvRef other = (ContextResourceEnvRef)obj;
    if (this.override != other.override) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextResourceEnvRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */