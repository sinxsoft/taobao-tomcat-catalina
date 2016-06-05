package org.apache.catalina;

public abstract interface Lifecycle
{
  public static final String BEFORE_INIT_EVENT = "before_init";
  public static final String AFTER_INIT_EVENT = "after_init";
  public static final String START_EVENT = "start";
  public static final String BEFORE_START_EVENT = "before_start";
  public static final String AFTER_START_EVENT = "after_start";
  public static final String STOP_EVENT = "stop";
  public static final String BEFORE_STOP_EVENT = "before_stop";
  public static final String AFTER_STOP_EVENT = "after_stop";
  public static final String AFTER_DESTROY_EVENT = "after_destroy";
  public static final String BEFORE_DESTROY_EVENT = "before_destroy";
  public static final String PERIODIC_EVENT = "periodic";
  public static final String CONFIGURE_START_EVENT = "configure_start";
  public static final String CONFIGURE_STOP_EVENT = "configure_stop";
  
  public abstract void addLifecycleListener(LifecycleListener paramLifecycleListener);
  
  public abstract LifecycleListener[] findLifecycleListeners();
  
  public abstract void removeLifecycleListener(LifecycleListener paramLifecycleListener);
  
  public abstract void init()
    throws LifecycleException;
  
  public abstract void start()
    throws LifecycleException;
  
  public abstract void stop()
    throws LifecycleException;
  
  public abstract void destroy()
    throws LifecycleException;
  
  public abstract LifecycleState getState();
  
  public abstract String getStateName();
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Lifecycle.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */