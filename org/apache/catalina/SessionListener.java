package org.apache.catalina;

import java.util.EventListener;

public abstract interface SessionListener
  extends EventListener
{
  public abstract void sessionEvent(SessionEvent paramSessionEvent);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\SessionListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */