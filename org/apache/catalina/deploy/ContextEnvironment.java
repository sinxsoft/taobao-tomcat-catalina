package org.apache.catalina.deploy;

public class ContextEnvironment
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private boolean override = true;
  
  public ContextEnvironment() {}
  
  public boolean getOverride()
  {
    return this.override;
  }
  
  public void setOverride(boolean override)
  {
    this.override = override;
  }
  
  private String value = null;
  
  public String getValue()
  {
    return this.value;
  }
  
  public void setValue(String value)
  {
    this.value = value;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextEnvironment[");
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
    if (this.value != null)
    {
      sb.append(", value=");
      sb.append(this.value);
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
    result = 31 * result + (this.value == null ? 0 : this.value.hashCode());
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
    ContextEnvironment other = (ContextEnvironment)obj;
    if (this.override != other.override) {
      return false;
    }
    if (this.value == null)
    {
      if (other.value != null) {
        return false;
      }
    }
    else if (!this.value.equals(other.value)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextEnvironment.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */