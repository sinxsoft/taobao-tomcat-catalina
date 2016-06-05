package org.apache.catalina.comet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract interface CometEvent
{
  public abstract HttpServletRequest getHttpServletRequest();
  
  public abstract HttpServletResponse getHttpServletResponse();
  
  public abstract EventType getEventType();
  
  public abstract EventSubType getEventSubType();
  
  public abstract void close()
    throws IOException;
  
  public abstract void setTimeout(int paramInt)
    throws IOException, ServletException, UnsupportedOperationException;
  
  public enum EventType
  {
    BEGIN,  READ,  END,  ERROR;
    
    private EventType() {}
  }
  
  public enum EventSubType
  {
    TIMEOUT,  CLIENT_DISCONNECT,  IOEXCEPTION,  WEBAPP_RELOAD,  SERVER_SHUTDOWN,  SESSION_END;
    
    private EventSubType() {}
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\comet\CometEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */