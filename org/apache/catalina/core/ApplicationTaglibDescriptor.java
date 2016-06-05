package org.apache.catalina.core;

import javax.servlet.descriptor.TaglibDescriptor;

public class ApplicationTaglibDescriptor
  implements TaglibDescriptor
{
  private String location;
  private String uri;
  
  public ApplicationTaglibDescriptor(String location, String uri)
  {
    this.location = location;
    this.uri = uri;
  }
  
  public String getTaglibLocation()
  {
    return this.location;
  }
  
  public String getTaglibURI()
  {
    return this.uri;
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.location == null ? 0 : this.location.hashCode());
    
    result = 31 * result + (this.uri == null ? 0 : this.uri.hashCode());
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ApplicationTaglibDescriptor)) {
      return false;
    }
    ApplicationTaglibDescriptor other = (ApplicationTaglibDescriptor)obj;
    if (this.location == null)
    {
      if (other.location != null) {
        return false;
      }
    }
    else if (!this.location.equals(other.location)) {
      return false;
    }
    if (this.uri == null)
    {
      if (other.uri != null) {
        return false;
      }
    }
    else if (!this.uri.equals(other.uri)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationTaglibDescriptor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */