package org.apache.catalina.session;

public class TooManyActiveSessionsException
  extends IllegalStateException
{
  private static final long serialVersionUID = 1L;
  private final int maxActiveSessions;
  
  public TooManyActiveSessionsException(String message, int maxActive)
  {
    super(message);
    
    this.maxActiveSessions = maxActive;
  }
  
  public int getMaxActiveSessions()
  {
    return this.maxActiveSessions;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\TooManyActiveSessionsException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */