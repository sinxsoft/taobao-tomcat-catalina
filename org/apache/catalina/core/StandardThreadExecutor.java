package org.apache.catalina.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.Executor;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

public class StandardThreadExecutor
  extends LifecycleMBeanBase
  implements Executor, ResizableExecutor
{
  protected int threadPriority = 5;
  protected boolean daemon = true;
  protected String namePrefix = "tomcat-exec-";
  protected int maxThreads = 200;
  protected int minSpareThreads = 25;
  protected int maxIdleTime = 60000;
  protected ThreadPoolExecutor executor = null;
  protected String name;
  protected boolean prestartminSpareThreads = false;
  protected int maxQueueSize = Integer.MAX_VALUE;
  protected long threadRenewalDelay = 1000L;
  private TaskQueue taskqueue = null;
  
  public StandardThreadExecutor() {}
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    this.taskqueue = new TaskQueue(this.maxQueueSize);
    TaskThreadFactory tf = new TaskThreadFactory(this.namePrefix, this.daemon, getThreadPriority());
    this.executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), this.maxIdleTime, TimeUnit.MILLISECONDS, this.taskqueue, tf);
    this.executor.setThreadRenewalDelay(this.threadRenewalDelay);
    if (this.prestartminSpareThreads) {
      this.executor.prestartAllCoreThreads();
    }
    this.taskqueue.setParent(this.executor);
    
    setState(LifecycleState.STARTING);
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
    if (this.executor != null) {
      this.executor.shutdownNow();
    }
    this.executor = null;
    this.taskqueue = null;
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    super.destroyInternal();
  }
  
  public void execute(Runnable command, long timeout, TimeUnit unit)
  {
    if (this.executor != null) {
      this.executor.execute(command, timeout, unit);
    } else {
      throw new IllegalStateException("StandardThreadExecutor not started.");
    }
  }
  
  public void execute(Runnable command)
  {
    if (this.executor != null) {
      try
      {
        this.executor.execute(command);
      }
      catch (RejectedExecutionException rx)
      {
        if (!((TaskQueue)this.executor.getQueue()).force(command)) {
          throw new RejectedExecutionException("Work queue full.");
        }
      }
    } else {
      throw new IllegalStateException("StandardThreadPool not started.");
    }
  }
  
  public void contextStopping()
  {
    if (this.executor != null) {
      this.executor.contextStopping();
    }
  }
  
  public int getThreadPriority()
  {
    return this.threadPriority;
  }
  
  public boolean isDaemon()
  {
    return this.daemon;
  }
  
  public String getNamePrefix()
  {
    return this.namePrefix;
  }
  
  public int getMaxIdleTime()
  {
    return this.maxIdleTime;
  }
  
  public int getMaxThreads()
  {
    return this.maxThreads;
  }
  
  public int getMinSpareThreads()
  {
    return this.minSpareThreads;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public boolean isPrestartminSpareThreads()
  {
    return this.prestartminSpareThreads;
  }
  
  public void setThreadPriority(int threadPriority)
  {
    this.threadPriority = threadPriority;
  }
  
  public void setDaemon(boolean daemon)
  {
    this.daemon = daemon;
  }
  
  public void setNamePrefix(String namePrefix)
  {
    this.namePrefix = namePrefix;
  }
  
  public void setMaxIdleTime(int maxIdleTime)
  {
    this.maxIdleTime = maxIdleTime;
    if (this.executor != null) {
      this.executor.setKeepAliveTime(maxIdleTime, TimeUnit.MILLISECONDS);
    }
  }
  
  public void setMaxThreads(int maxThreads)
  {
    this.maxThreads = maxThreads;
    if (this.executor != null) {
      this.executor.setMaximumPoolSize(maxThreads);
    }
  }
  
  public void setMinSpareThreads(int minSpareThreads)
  {
    this.minSpareThreads = minSpareThreads;
    if (this.executor != null) {
      this.executor.setCorePoolSize(minSpareThreads);
    }
  }
  
  public void setPrestartminSpareThreads(boolean prestartminSpareThreads)
  {
    this.prestartminSpareThreads = prestartminSpareThreads;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public void setMaxQueueSize(int size)
  {
    this.maxQueueSize = size;
  }
  
  public int getMaxQueueSize()
  {
    return this.maxQueueSize;
  }
  
  public long getThreadRenewalDelay()
  {
    return this.threadRenewalDelay;
  }
  
  public void setThreadRenewalDelay(long threadRenewalDelay)
  {
    this.threadRenewalDelay = threadRenewalDelay;
    if (this.executor != null) {
      this.executor.setThreadRenewalDelay(threadRenewalDelay);
    }
  }
  
  public int getActiveCount()
  {
    return this.executor != null ? this.executor.getActiveCount() : 0;
  }
  
  public long getCompletedTaskCount()
  {
    return this.executor != null ? this.executor.getCompletedTaskCount() : 0L;
  }
  
  public int getCorePoolSize()
  {
    return this.executor != null ? this.executor.getCorePoolSize() : 0;
  }
  
  public int getLargestPoolSize()
  {
    return this.executor != null ? this.executor.getLargestPoolSize() : 0;
  }
  
  public int getPoolSize()
  {
    return this.executor != null ? this.executor.getPoolSize() : 0;
  }
  
  public int getQueueSize()
  {
    return this.executor != null ? this.executor.getQueue().size() : -1;
  }
  
  public boolean resizePool(int corePoolSize, int maximumPoolSize)
  {
    if (this.executor == null) {
      return false;
    }
    this.executor.setCorePoolSize(corePoolSize);
    this.executor.setMaximumPoolSize(maximumPoolSize);
    return true;
  }
  
  public boolean resizeQueue(int capacity)
  {
    return false;
  }
  
  protected String getDomainInternal()
  {
    return null;
  }
  
  protected String getObjectNameKeyProperties()
  {
    StringBuilder name = new StringBuilder("type=Executor,name=");
    name.append(getName());
    return name.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardThreadExecutor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */