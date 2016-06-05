package org.apache.catalina;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public abstract interface Valve
{
  public abstract String getInfo();
  
  public abstract Valve getNext();
  
  public abstract void setNext(Valve paramValve);
  
  public abstract void backgroundProcess();
  
  public abstract void invoke(Request paramRequest, Response paramResponse)
    throws IOException, ServletException;
  
  public abstract void event(Request paramRequest, Response paramResponse, CometEvent paramCometEvent)
    throws IOException, ServletException;
  
  public abstract boolean isAsyncSupported();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Valve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */