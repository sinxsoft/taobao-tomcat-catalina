package org.apache.naming.resources;

import javax.naming.Name;
import javax.naming.NameNotFoundException;

public class ImmutableNameNotFoundException
  extends NameNotFoundException
{
  private static final long serialVersionUID = 1L;
  
  public ImmutableNameNotFoundException() {}
  
  public void appendRemainingComponent(String name) {}
  
  public void appendRemainingName(Name name) {}
  
  public void setRemainingName(Name name) {}
  
  public void setResolvedName(Name name) {}
  
  public void setRootCause(Throwable e) {}
  
  public synchronized Throwable fillInStackTrace()
  {
    return this;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\ImmutableNameNotFoundException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */