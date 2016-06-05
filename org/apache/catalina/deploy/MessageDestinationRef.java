package org.apache.catalina.deploy;

public class MessageDestinationRef
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String link = null;
  
  public MessageDestinationRef() {}
  
  public String getLink()
  {
    return this.link;
  }
  
  public void setLink(String link)
  {
    this.link = link;
  }
  
  private String usage = null;
  
  public String getUsage()
  {
    return this.usage;
  }
  
  public void setUsage(String usage)
  {
    this.usage = usage;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("MessageDestination[");
    sb.append("name=");
    sb.append(getName());
    if (this.link != null)
    {
      sb.append(", link=");
      sb.append(this.link);
    }
    if (getType() != null)
    {
      sb.append(", type=");
      sb.append(getType());
    }
    if (this.usage != null)
    {
      sb.append(", usage=");
      sb.append(this.usage);
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
    result = 31 * result + (this.link == null ? 0 : this.link.hashCode());
    result = 31 * result + (this.usage == null ? 0 : this.usage.hashCode());
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
    MessageDestinationRef other = (MessageDestinationRef)obj;
    if (this.link == null)
    {
      if (other.link != null) {
        return false;
      }
    }
    else if (!this.link.equals(other.link)) {
      return false;
    }
    if (this.usage == null)
    {
      if (other.usage != null) {
        return false;
      }
    }
    else if (!this.usage.equals(other.usage)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\MessageDestinationRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */