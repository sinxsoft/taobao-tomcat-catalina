package org.apache.catalina.core;

import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometProcessor;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.res.StringManager;

final class StandardWrapperValve
  extends ValveBase
{
  private volatile long processingTime;
  private volatile long maxTime;
  
  public StandardWrapperValve()
  {
    super(true);
  }
  
  private volatile long minTime = Long.MAX_VALUE;
  private volatile int requestCount;
  private volatile int errorCount;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public final void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    boolean unavailable = false;
    Throwable throwable = null;
    
    long t1 = System.currentTimeMillis();
    this.requestCount += 1;
    StandardWrapper wrapper = (StandardWrapper)getContainer();
    Servlet servlet = null;
    Context context = (Context)wrapper.getParent();
    if (!context.getState().isAvailable())
    {
      response.sendError(503, sm.getString("standardContext.isUnavailable"));
      
      unavailable = true;
    }
    if ((!unavailable) && (wrapper.isUnavailable()))
    {
      this.container.getLogger().info(sm.getString("standardWrapper.isUnavailable", new Object[] { wrapper.getName() }));
      
      long available = wrapper.getAvailable();
      if ((available > 0L) && (available < Long.MAX_VALUE))
      {
        response.setDateHeader("Retry-After", available);
        response.sendError(503, sm.getString("standardWrapper.isUnavailable", new Object[] { wrapper.getName() }));
      }
      else if (available == Long.MAX_VALUE)
      {
        response.sendError(404, sm.getString("standardWrapper.notFound", new Object[] { wrapper.getName() }));
      }
      unavailable = true;
    }
    try
    {
      if (!unavailable) {
        servlet = wrapper.allocate();
      }
    }
    catch (UnavailableException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.allocateException", new Object[] { wrapper.getName() }), e);
      
      long available = wrapper.getAvailable();
      if ((available > 0L) && (available < Long.MAX_VALUE))
      {
        response.setDateHeader("Retry-After", available);
        response.sendError(503, sm.getString("standardWrapper.isUnavailable", new Object[] { wrapper.getName() }));
      }
      else if (available == Long.MAX_VALUE)
      {
        response.sendError(404, sm.getString("standardWrapper.notFound", new Object[] { wrapper.getName() }));
      }
    }
    catch (ServletException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.allocateException", new Object[] { wrapper.getName() }), StandardWrapper.getRootCause(e));
      
      throwable = e;
      exception(request, response, e);
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.allocateException", new Object[] { wrapper.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
      servlet = null;
    }
    boolean comet = false;
    if (((servlet instanceof CometProcessor)) && (request.getAttribute("org.apache.tomcat.comet.support") == Boolean.TRUE))
    {
      comet = true;
      request.setComet(true);
    }
    MessageBytes requestPathMB = request.getRequestPathMB();
    DispatcherType dispatcherType = DispatcherType.REQUEST;
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      dispatcherType = DispatcherType.ASYNC;
    }
    request.setAttribute("org.apache.catalina.core.DISPATCHER_TYPE", dispatcherType);
    request.setAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH", requestPathMB);
    
    ApplicationFilterFactory factory = ApplicationFilterFactory.getInstance();
    
    ApplicationFilterChain filterChain = factory.createFilterChain(request, wrapper, servlet);
    
    request.setComet(false);
    try
    {
      if ((servlet != null) && (filterChain != null)) {
        if (context.getSwallowOutput())
        {
          try
          {
            SystemLogHandler.startCapture();
            if (request.isAsyncDispatching())
            {
              ((AsyncContextImpl)request.getAsyncContext()).doInternalDispatch();
            }
            else if (comet)
            {
              filterChain.doFilterEvent(request.getEvent());
              request.setComet(true);
            }
            else
            {
              filterChain.doFilter(request.getRequest(), response.getResponse());
            }
          }
          finally
          {
            String log;
            log = SystemLogHandler.stopCapture();
            if ((log != null) && (log.length() > 0)) {
              context.getLogger().info(log);
            }
          }
        }
        else if (request.isAsyncDispatching())
        {
          ((AsyncContextImpl)request.getAsyncContext()).doInternalDispatch();
        }
        else if (comet)
        {
          request.setComet(true);
          filterChain.doFilterEvent(request.getEvent());
        }
        else
        {
          filterChain.doFilter(request.getRequest(), response.getResponse());
        }
      }
    }
    catch (ClientAbortException e)
    {
      throwable = e;
      exception(request, response, e);
    }
    catch (IOException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
    }
    catch (UnavailableException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
      
      wrapper.unavailable(e);
      long available = wrapper.getAvailable();
      if ((available > 0L) && (available < Long.MAX_VALUE))
      {
        response.setDateHeader("Retry-After", available);
        response.sendError(503, sm.getString("standardWrapper.isUnavailable", new Object[] { wrapper.getName() }));
      }
      else if (available == Long.MAX_VALUE)
      {
        response.sendError(404, sm.getString("standardWrapper.notFound", new Object[] { wrapper.getName() }));
      }
    }
    catch (ServletException e)
    {
      Throwable rootCause = StandardWrapper.getRootCause(e);
      if (!(rootCause instanceof ClientAbortException)) {
        this.container.getLogger().error(sm.getString("standardWrapper.serviceExceptionRoot", new Object[] { wrapper.getName(), context.getName(), e.getMessage() }), rootCause);
      }
      throwable = e;
      exception(request, response, e);
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
    }
    if (filterChain != null) {
      if (request.isComet()) {
        filterChain.reuse();
      } else {
        filterChain.release();
      }
    }
    try
    {
      if (servlet != null) {
        wrapper.deallocate(servlet);
      }
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.deallocateException", new Object[] { wrapper.getName() }), e);
      if (throwable == null)
      {
        throwable = e;
        exception(request, response, e);
      }
    }
    try
    {
      if ((servlet != null) && (wrapper.getAvailable() == Long.MAX_VALUE)) {
        wrapper.unload();
      }
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.unloadException", new Object[] { wrapper.getName() }), e);
      if (throwable == null)
      {
        throwable = e;
        exception(request, response, e);
      }
    }
    long t2 = System.currentTimeMillis();
    
    long time = t2 - t1;
    this.processingTime += time;
    if (time > this.maxTime) {
      this.maxTime = time;
    }
    if (time < this.minTime) {
      this.minTime = time;
    }
  }
  
  public void event(Request request, Response response, CometEvent event)
    throws IOException, ServletException
  {
    Throwable throwable = null;
    
    long t1 = System.currentTimeMillis();
    
    StandardWrapper wrapper = (StandardWrapper)getContainer();
    if (wrapper == null) {
      return;
    }
    Servlet servlet = null;
    Context context = (Context)wrapper.getParent();
    
    boolean unavailable = (!context.getState().isAvailable()) || (wrapper.isUnavailable());
    try
    {
      if (!unavailable) {
        servlet = wrapper.allocate();
      }
    }
    catch (UnavailableException e) {}catch (ServletException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.allocateException", new Object[] { wrapper.getName() }), StandardWrapper.getRootCause(e));
      
      throwable = e;
      exception(request, response, e);
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.allocateException", new Object[] { wrapper.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
      servlet = null;
    }
    MessageBytes requestPathMB = request.getRequestPathMB();
    request.setAttribute("org.apache.catalina.core.DISPATCHER_TYPE", DispatcherType.REQUEST);
    
    request.setAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH", requestPathMB);
    
    ApplicationFilterChain filterChain = (ApplicationFilterChain)request.getFilterChain();
    try
    {
      if ((servlet != null) && (filterChain != null)) {
        if (context.getSwallowOutput()) {
          try
          {
            SystemLogHandler.startCapture();
            filterChain.doFilterEvent(request.getEvent());
          }
          finally
          {
            String log;
            log = SystemLogHandler.stopCapture();
            if ((log != null) && (log.length() > 0)) {
              context.getLogger().info(log);
            }
          }
        } else {
          filterChain.doFilterEvent(request.getEvent());
        }
      }
    }
    catch (ClientAbortException e)
    {
      throwable = e;
      exception(request, response, e);
    }
    catch (IOException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
    }
    catch (UnavailableException e)
    {
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
    }
    catch (ServletException e)
    {
      Throwable rootCause = StandardWrapper.getRootCause(e);
      if (!(rootCause instanceof ClientAbortException)) {
        this.container.getLogger().error(sm.getString("standardWrapper.serviceExceptionRoot", new Object[] { wrapper.getName(), context.getName(), e.getMessage() }), rootCause);
      }
      throwable = e;
      exception(request, response, e);
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.serviceException", new Object[] { wrapper.getName(), context.getName() }), e);
      
      throwable = e;
      exception(request, response, e);
    }
    if (filterChain != null) {
      filterChain.reuse();
    }
    try
    {
      if (servlet != null) {
        wrapper.deallocate(servlet);
      }
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.deallocateException", new Object[] { wrapper.getName() }), e);
      if (throwable == null)
      {
        throwable = e;
        exception(request, response, e);
      }
    }
    try
    {
      if ((servlet != null) && (wrapper.getAvailable() == Long.MAX_VALUE)) {
        wrapper.unload();
      }
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.container.getLogger().error(sm.getString("standardWrapper.unloadException", new Object[] { wrapper.getName() }), e);
      if (throwable == null)
      {
        throwable = e;
        exception(request, response, e);
      }
    }
    long t2 = System.currentTimeMillis();
    
    long time = t2 - t1;
    this.processingTime += time;
    if (time > this.maxTime) {
      this.maxTime = time;
    }
    if (time < this.minTime) {
      this.minTime = time;
    }
  }
  
  private void exception(Request request, Response response, Throwable exception)
  {
    request.setAttribute("javax.servlet.error.exception", exception);
    response.setStatus(500);
    response.setError();
  }
  
  public long getProcessingTime()
  {
    return this.processingTime;
  }
  
  @Deprecated
  public void setProcessingTime(long processingTime)
  {
    this.processingTime = processingTime;
  }
  
  public long getMaxTime()
  {
    return this.maxTime;
  }
  
  @Deprecated
  public void setMaxTime(long maxTime)
  {
    this.maxTime = maxTime;
  }
  
  public long getMinTime()
  {
    return this.minTime;
  }
  
  @Deprecated
  public void setMinTime(long minTime)
  {
    this.minTime = minTime;
  }
  
  public int getRequestCount()
  {
    return this.requestCount;
  }
  
  @Deprecated
  public void setRequestCount(int requestCount)
  {
    this.requestCount = requestCount;
  }
  
  public int getErrorCount()
  {
    return this.errorCount;
  }
  
  public void incrementErrorCount()
  {
    this.errorCount += 1;
  }
  
  @Deprecated
  public void setErrorCount(int errorCount)
  {
    this.errorCount = errorCount;
  }
  
  protected void initInternal()
    throws LifecycleException
  {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardWrapperValve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */