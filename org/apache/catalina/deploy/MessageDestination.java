package org.apache.catalina.deploy;

public class MessageDestination
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String displayName = null;
  
  public MessageDestination() {}
  
  public String getDisplayName()
  {
    return this.displayName;
  }
  
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }
  
  private String largeIcon = null;
  
  public String getLargeIcon()
  {
    return this.largeIcon;
  }
  
  public void setLargeIcon(String largeIcon)
  {
    this.largeIcon = largeIcon;
  }
  
  private String smallIcon = null;
  
  public String getSmallIcon()
  {
    return this.smallIcon;
  }
  
  public void setSmallIcon(String smallIcon)
  {
    this.smallIcon = smallIcon;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("MessageDestination[");
    sb.append("name=");
    sb.append(getName());
    if (this.displayName != null)
    {
      sb.append(", displayName=");
      sb.append(this.displayName);
    }
    if (this.largeIcon != null)
    {
      sb.append(", largeIcon=");
      sb.append(this.largeIcon);
    }
    if (this.smallIcon != null)
    {
      sb.append(", smallIcon=");
      sb.append(this.smallIcon);
    }
    if (getDescription() != null)
    {
      sb.append(", description=");
      sb.append(getDescription());
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.displayName == null ? 0 : this.displayName.hashCode());
    
    result = 31 * result + (this.largeIcon == null ? 0 : this.largeIcon.hashCode());
    
    result = 31 * result + (this.smallIcon == null ? 0 : this.smallIcon.hashCode());
    
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
    MessageDestination other = (MessageDestination)obj;
    if (this.displayName == null)
    {
      if (other.displayName != null) {
        return false;
      }
    }
    else if (!this.displayName.equals(other.displayName)) {
      return false;
    }
    if (this.largeIcon == null)
    {
      if (other.largeIcon != null) {
        return false;
      }
    }
    else if (!this.largeIcon.equals(other.largeIcon)) {
      return false;
    }
    if (this.smallIcon == null)
    {
      if (other.smallIcon != null) {
        return false;
      }
    }
    else if (!this.smallIcon.equals(other.smallIcon)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\MessageDestination.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */