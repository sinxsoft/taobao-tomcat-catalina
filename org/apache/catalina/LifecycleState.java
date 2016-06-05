package org.apache.catalina;

public enum LifecycleState
{
  NEW(false, null),  INITIALIZING(false, "before_init"),  INITIALIZED(false, "after_init"),  STARTING_PREP(false, "before_start"),  STARTING(true, "start"),  STARTED(true, "after_start"),  STOPPING_PREP(true, "before_stop"),  STOPPING(false, "stop"),  STOPPED(false, "after_stop"),  DESTROYING(false, "before_destroy"),  DESTROYED(false, "after_destroy"),  FAILED(false, null),  MUST_STOP(true, null),  MUST_DESTROY(false, null);
  
  private final boolean available;
  private final String lifecycleEvent;
  
  private LifecycleState(boolean available, String lifecycleEvent)
  {
    this.available = available;
    this.lifecycleEvent = lifecycleEvent;
  }
  
  public boolean isAvailable()
  {
    return this.available;
  }
  
  public String getLifecycleEvent()
  {
    return this.lifecycleEvent;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\LifecycleState.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */