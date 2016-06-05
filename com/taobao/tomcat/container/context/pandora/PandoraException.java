package com.taobao.tomcat.container.context.pandora;

public class PandoraException
  extends RuntimeException
{
  private static final long serialVersionUID = 5460967683841729224L;
  
  public PandoraException() {}
  
  public PandoraException(String msg)
  {
    super(msg);
  }
  
  public PandoraException(String message, Throwable cause)
  {
    super(message, cause);
  }
  
  public PandoraException(Throwable cause)
  {
    super(cause);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\context\pandora\PandoraException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */