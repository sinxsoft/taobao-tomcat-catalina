package org.apache.catalina.session;

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class StandardSessionFacade
  implements HttpSession
{
  public StandardSessionFacade(StandardSession session)
  {
    this.session = session;
  }
  
  public StandardSessionFacade(HttpSession session)
  {
    this.session = session;
  }
  
  private HttpSession session = null;
  
  public long getCreationTime()
  {
    return this.session.getCreationTime();
  }
  
  public String getId()
  {
    return this.session.getId();
  }
  
  public long getLastAccessedTime()
  {
    return this.session.getLastAccessedTime();
  }
  
  public ServletContext getServletContext()
  {
    return this.session.getServletContext();
  }
  
  public void setMaxInactiveInterval(int interval)
  {
    this.session.setMaxInactiveInterval(interval);
  }
  
  public int getMaxInactiveInterval()
  {
    return this.session.getMaxInactiveInterval();
  }
  
  @Deprecated
  public HttpSessionContext getSessionContext()
  {
    return this.session.getSessionContext();
  }
  
  public Object getAttribute(String name)
  {
    return this.session.getAttribute(name);
  }
  
  @Deprecated
  public Object getValue(String name)
  {
    return this.session.getAttribute(name);
  }
  
  public Enumeration<String> getAttributeNames()
  {
    return this.session.getAttributeNames();
  }
  
  @Deprecated
  public String[] getValueNames()
  {
    return this.session.getValueNames();
  }
  
  public void setAttribute(String name, Object value)
  {
    this.session.setAttribute(name, value);
  }
  
  @Deprecated
  public void putValue(String name, Object value)
  {
    this.session.setAttribute(name, value);
  }
  
  public void removeAttribute(String name)
  {
    this.session.removeAttribute(name);
  }
  
  @Deprecated
  public void removeValue(String name)
  {
    this.session.removeAttribute(name);
  }
  
  public void invalidate()
  {
    this.session.invalidate();
  }
  
  public boolean isNew()
  {
    return this.session.isNew();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\StandardSessionFacade.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */