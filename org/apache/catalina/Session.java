package org.apache.catalina;

import java.security.Principal;
import java.util.Iterator;
import javax.servlet.http.HttpSession;

public abstract interface Session
{
  public static final String SESSION_CREATED_EVENT = "createSession";
  public static final String SESSION_DESTROYED_EVENT = "destroySession";
  public static final String SESSION_ACTIVATED_EVENT = "activateSession";
  public static final String SESSION_PASSIVATED_EVENT = "passivateSession";
  
  public abstract String getAuthType();
  
  public abstract void setAuthType(String paramString);
  
  public abstract long getCreationTime();
  
  public abstract long getCreationTimeInternal();
  
  public abstract void setCreationTime(long paramLong);
  
  public abstract String getId();
  
  public abstract String getIdInternal();
  
  public abstract void setId(String paramString);
  
  public abstract void setId(String paramString, boolean paramBoolean);
  
  public abstract String getInfo();
  
  public abstract long getThisAccessedTime();
  
  public abstract long getThisAccessedTimeInternal();
  
  public abstract long getLastAccessedTime();
  
  public abstract long getLastAccessedTimeInternal();
  
  public abstract Manager getManager();
  
  public abstract void setManager(Manager paramManager);
  
  public abstract int getMaxInactiveInterval();
  
  public abstract void setMaxInactiveInterval(int paramInt);
  
  public abstract void setNew(boolean paramBoolean);
  
  public abstract Principal getPrincipal();
  
  public abstract void setPrincipal(Principal paramPrincipal);
  
  public abstract HttpSession getSession();
  
  public abstract void setValid(boolean paramBoolean);
  
  public abstract boolean isValid();
  
  public abstract void access();
  
  public abstract void addSessionListener(SessionListener paramSessionListener);
  
  public abstract void endAccess();
  
  public abstract void expire();
  
  public abstract Object getNote(String paramString);
  
  public abstract Iterator<String> getNoteNames();
  
  public abstract void recycle();
  
  public abstract void removeNote(String paramString);
  
  public abstract void removeSessionListener(SessionListener paramSessionListener);
  
  public abstract void setNote(String paramString, Object paramObject);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Session.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */