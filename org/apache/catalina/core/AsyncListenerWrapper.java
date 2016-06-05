package org.apache.catalina.core;

import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

public class AsyncListenerWrapper
{
  private AsyncListener listener = null;
  
  public AsyncListenerWrapper() {}
  
  public void fireOnStartAsync(AsyncEvent event)
    throws IOException
  {
    this.listener.onStartAsync(event);
  }
  
  public void fireOnComplete(AsyncEvent event)
    throws IOException
  {
    this.listener.onComplete(event);
  }
  
  public void fireOnTimeout(AsyncEvent event)
    throws IOException
  {
    this.listener.onTimeout(event);
  }
  
  public void fireOnError(AsyncEvent event)
    throws IOException
  {
    this.listener.onError(event);
  }
  
  public AsyncListener getListener()
  {
    return this.listener;
  }
  
  public void setListener(AsyncListener listener)
  {
    this.listener = listener;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\AsyncListenerWrapper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */