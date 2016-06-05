package org.apache.catalina.connector;

import java.io.IOException;

public final class ClientAbortException
  extends IOException
{
  private static final long serialVersionUID = 1L;
  
  public ClientAbortException() {}
  
  public ClientAbortException(String message)
  {
    super(message);
    this.message = getMessage();
  }
  
  public ClientAbortException(Throwable throwable)
  {
    super(throwable);
    this.message = getMessage();
    this.throwable = throwable;
  }
  
  public ClientAbortException(String message, Throwable throwable)
  {
    super(message, throwable);
    this.message = getMessage();
    this.throwable = throwable;
  }
  
  @Deprecated
  protected String message = null;
  @Deprecated
  protected Throwable throwable = null;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\ClientAbortException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */