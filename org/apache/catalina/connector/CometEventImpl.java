package org.apache.catalina.connector;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometEvent.EventSubType;
import org.apache.catalina.comet.CometEvent.EventType;
import org.apache.tomcat.util.res.StringManager;

public class CometEventImpl
  implements CometEvent
{
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  
  public CometEventImpl(Request request, Response response)
  {
    this.request = request;
    this.response = response;
  }
  
  protected Request request = null;
  protected Response response = null;
  protected CometEvent.EventType eventType = CometEvent.EventType.BEGIN;
  protected CometEvent.EventSubType eventSubType = null;
  
  public void clear()
  {
    this.request = null;
    this.response = null;
  }
  
  public void setEventType(CometEvent.EventType eventType)
  {
    this.eventType = eventType;
  }
  
  public void setEventSubType(CometEvent.EventSubType eventSubType)
  {
    this.eventSubType = eventSubType;
  }
  
  public void close()
    throws IOException
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("cometEvent.nullRequest"));
    }
    this.request.finishRequest();
    this.response.finishResponse();
    if (this.request.isComet()) {
      this.request.cometClose();
    }
  }
  
  public CometEvent.EventSubType getEventSubType()
  {
    return this.eventSubType;
  }
  
  public CometEvent.EventType getEventType()
  {
    return this.eventType;
  }
  
  public HttpServletRequest getHttpServletRequest()
  {
    return this.request.getRequest();
  }
  
  public HttpServletResponse getHttpServletResponse()
  {
    return this.response.getResponse();
  }
  
  public void setTimeout(int timeout)
    throws IOException, ServletException, UnsupportedOperationException
  {
    if (this.request.getAttribute("org.apache.tomcat.comet.timeout.support") == Boolean.TRUE)
    {
      this.request.setAttribute("org.apache.tomcat.comet.timeout", Integer.valueOf(timeout));
      if (this.request.isComet()) {
        this.request.setCometTimeout(timeout);
      }
    }
    else
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder();
    buf.append(super.toString());
    buf.append("[EventType:");
    buf.append(this.eventType);
    buf.append(", EventSubType:");
    buf.append(this.eventSubType);
    buf.append("]");
    return buf.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CometEventImpl.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */