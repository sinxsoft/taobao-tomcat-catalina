package org.apache.catalina;

import java.util.concurrent.TimeUnit;

public abstract interface Executor
  extends java.util.concurrent.Executor, Lifecycle
{
  public abstract String getName();
  
  public abstract void execute(Runnable paramRunnable, long paramLong, TimeUnit paramTimeUnit);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Executor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */