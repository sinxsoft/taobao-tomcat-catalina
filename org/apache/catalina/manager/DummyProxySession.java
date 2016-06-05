package org.apache.catalina.manager;

import java.security.Principal;
import java.util.Iterator;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionListener;

public class DummyProxySession
  implements Session
{
  private String sessionId;
  
  public DummyProxySession(String sessionId)
  {
    this.sessionId = sessionId;
  }
  
  public void access() {}
  
  public void addSessionListener(SessionListener listener) {}
  
  public void endAccess() {}
  
  public void expire() {}
  
  public String getAuthType()
  {
    return null;
  }
  
  public long getCreationTime()
  {
    return 0L;
  }
  
  public long getCreationTimeInternal()
  {
    return 0L;
  }
  
  public String getId()
  {
    return this.sessionId;
  }
  
  public String getIdInternal()
  {
    return this.sessionId;
  }
  
  public String getInfo()
  {
    return null;
  }
  
  public long getLastAccessedTime()
  {
    return 0L;
  }
  
  public long getLastAccessedTimeInternal()
  {
    return 0L;
  }
  
  public Manager getManager()
  {
    return null;
  }
  
  public int getMaxInactiveInterval()
  {
    return 0;
  }
  
  public Object getNote(String name)
  {
    return null;
  }
  
  public Iterator<String> getNoteNames()
  {
    return null;
  }
  
  public Principal getPrincipal()
  {
    return null;
  }
  
  public HttpSession getSession()
  {
    return null;
  }
  
  public long getThisAccessedTime()
  {
    return 0L;
  }
  
  public long getThisAccessedTimeInternal()
  {
    return 0L;
  }
  
  public boolean isValid()
  {
    return false;
  }
  
  public void recycle() {}
  
  public void removeNote(String name) {}
  
  public void removeSessionListener(SessionListener listener) {}
  
  public void setAuthType(String authType) {}
  
  public void setCreationTime(long time) {}
  
  public void setId(String id)
  {
    this.sessionId = id;
  }
  
  public void setId(String id, boolean notify)
  {
    this.sessionId = id;
  }
  
  public void setManager(Manager manager) {}
  
  public void setMaxInactiveInterval(int interval) {}
  
  public void setNew(boolean isNew) {}
  
  public void setNote(String name, Object value) {}
  
  public void setPrincipal(Principal principal) {}
  
  public void setValid(boolean isValid) {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\manager\DummyProxySession.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */