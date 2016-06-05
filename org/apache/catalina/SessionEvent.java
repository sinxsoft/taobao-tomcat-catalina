package org.apache.catalina;

import java.util.EventObject;

public final class SessionEvent
  extends EventObject
{
  private static final long serialVersionUID = 1L;
  private Object data = null;
  private Session session = null;
  private String type = null;
  
  public SessionEvent(Session session, String type, Object data)
  {
    super(session);
    this.session = session;
    this.type = type;
    this.data = data;
  }
  
  public Object getData()
  {
    return this.data;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public String getType()
  {
    return this.type;
  }
  
  public String toString()
  {
    return "SessionEvent['" + getSession() + "','" + getType() + "']";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\SessionEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */