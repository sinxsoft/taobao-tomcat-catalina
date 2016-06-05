package org.apache.catalina.deploy;

import java.io.Serializable;

public class ApplicationParameter
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private String description = null;
  
  public ApplicationParameter() {}
  
  public String getDescription()
  {
    return this.description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  private String name = null;
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  private boolean override = true;
  
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
    StringBuilder sb = new StringBuilder("ApplicationParameter[");
    sb.append("name=");
    sb.append(this.name);
    if (this.description != null)
    {
      sb.append(", description=");
      sb.append(this.description);
    }
    sb.append(", value=");
    sb.append(this.value);
    sb.append(", override=");
    sb.append(this.override);
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ApplicationParameter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */