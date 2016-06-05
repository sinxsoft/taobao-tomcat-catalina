package org.apache.catalina;

import java.util.EventObject;

public final class LifecycleEvent
  extends EventObject
{
  private static final long serialVersionUID = 1L;
  
  public LifecycleEvent(Lifecycle lifecycle, String type, Object data)
  {
    super(lifecycle);
    this.type = type;
    this.data = data;
  }
  
  private Object data = null;
  private String type = null;
  
  public Object getData()
  {
    return this.data;
  }
  
  public Lifecycle getLifecycle()
  {
    return (Lifecycle)getSource();
  }
  
  public String getType()
  {
    return this.type;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\LifecycleEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */