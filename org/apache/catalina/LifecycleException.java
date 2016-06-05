package org.apache.catalina;

public final class LifecycleException
  extends Exception
{
  private static final long serialVersionUID = 1L;
  
  public LifecycleException() {}
  
  public LifecycleException(String message)
  {
    super(message);
  }
  
  public LifecycleException(Throwable throwable)
  {
    super(throwable);
  }
  
  public LifecycleException(String message, Throwable throwable)
  {
    super(message, throwable);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\LifecycleException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */