package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;

public abstract interface Manager
{
  public abstract Container getContainer();
  
  public abstract void setContainer(Container paramContainer);
  
  public abstract boolean getDistributable();
  
  public abstract void setDistributable(boolean paramBoolean);
  
  public abstract String getInfo();
  
  public abstract int getMaxInactiveInterval();
  
  public abstract void setMaxInactiveInterval(int paramInt);
  
  public abstract int getSessionIdLength();
  
  public abstract void setSessionIdLength(int paramInt);
  
  public abstract long getSessionCounter();
  
  public abstract void setSessionCounter(long paramLong);
  
  public abstract int getMaxActive();
  
  public abstract void setMaxActive(int paramInt);
  
  public abstract int getActiveSessions();
  
  public abstract long getExpiredSessions();
  
  public abstract void setExpiredSessions(long paramLong);
  
  public abstract int getRejectedSessions();
  
  public abstract int getSessionMaxAliveTime();
  
  public abstract void setSessionMaxAliveTime(int paramInt);
  
  public abstract int getSessionAverageAliveTime();
  
  public abstract int getSessionCreateRate();
  
  public abstract int getSessionExpireRate();
  
  public abstract void add(Session paramSession);
  
  public abstract void addPropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract void changeSessionId(Session paramSession);
  
  public abstract Session createEmptySession();
  
  public abstract Session createSession(String paramString);
  
  public abstract Session findSession(String paramString)
    throws IOException;
  
  public abstract Session[] findSessions();
  
  public abstract void load()
    throws ClassNotFoundException, IOException;
  
  public abstract void remove(Session paramSession);
  
  public abstract void remove(Session paramSession, boolean paramBoolean);
  
  public abstract void removePropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract void unload()
    throws IOException;
  
  public abstract void backgroundProcess();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Manager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */