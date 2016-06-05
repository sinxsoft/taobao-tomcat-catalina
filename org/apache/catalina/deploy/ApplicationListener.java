package org.apache.catalina.deploy;

public class ApplicationListener
{
  private final String className;
  private final boolean pluggabilityBlocked;
  
  public ApplicationListener(String className, boolean pluggabilityBlocked)
  {
    this.className = className;
    this.pluggabilityBlocked = pluggabilityBlocked;
  }
  
  public String getClassName()
  {
    return this.className;
  }
  
  public boolean isPluggabilityBlocked()
  {
    return this.pluggabilityBlocked;
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.className == null ? 0 : this.className.hashCode());
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ApplicationListener)) {
      return false;
    }
    ApplicationListener other = (ApplicationListener)obj;
    if (this.className == null)
    {
      if (other.className != null) {
        return false;
      }
    }
    else if (!this.className.equals(other.className)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ApplicationListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */