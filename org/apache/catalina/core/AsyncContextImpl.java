package org.apache.catalina.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.NamingException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.AsyncDispatcher;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Response;
import org.apache.coyote.ActionCode;
import org.apache.coyote.AsyncContextCallback;
import org.apache.coyote.RequestInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.PrivilegedGetTccl;
import org.apache.tomcat.util.security.PrivilegedSetTccl;

public class AsyncContextImpl
  implements AsyncContext, AsyncContextCallback
{
  private static final Log log = LogFactory.getLog(AsyncContextImpl.class);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private volatile ServletRequest servletRequest = null;
  private volatile ServletResponse servletResponse = null;
  private List<AsyncListenerWrapper> listeners = new ArrayList();
  private boolean hasOriginalRequestAndResponse = true;
  private volatile Runnable dispatch = null;
  private Context context = null;
  private long timeout = -1L;
  private AsyncEvent event = null;
  private org.apache.catalina.connector.Request request;
  private volatile InstanceManager instanceManager;
  
  public AsyncContextImpl(org.apache.catalina.connector.Request request)
  {
    if (log.isDebugEnabled()) {
      logDebug("Constructor");
    }
    this.request = request;
  }
  
  public void complete()
  {
    if (log.isDebugEnabled()) {
      logDebug("complete   ");
    }
    check();
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_COMPLETE, null);
  }
  
  public void fireOnComplete()
  {
    List<AsyncListenerWrapper> listenersCopy = new ArrayList();
    
    listenersCopy.addAll(this.listeners);
    ClassLoader oldCL;
    
    if (Globals.IS_SECURITY_ENABLED)
    {
      PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
      oldCL = (ClassLoader)AccessController.doPrivileged(pa);
    }
    else
    {
      oldCL = Thread.currentThread().getContextClassLoader();
    }
    ClassLoader newCL = this.context.getLoader().getClassLoader();
    try
    {
      if (Globals.IS_SECURITY_ENABLED)
      {
        PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
        AccessController.doPrivileged(pa);
      }
      else
      {
        Thread.currentThread().setContextClassLoader(newCL);
      }
      for (AsyncListenerWrapper listener : listenersCopy) {
        try
        {
          listener.fireOnComplete(this.event);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          log.warn("onComplete() failed for listener of type [" + listener.getClass().getName() + "]", t);
        }
      }
    }
    finally
    {
      PrivilegedAction<Void> pa;
      clearServletRequestResponse();
      if (Globals.IS_SECURITY_ENABLED)
      {
       pa = new PrivilegedSetTccl(oldCL);
        AccessController.doPrivileged(pa);
      }
      else
      {
        Thread.currentThread().setContextClassLoader(oldCL);
      }
    }
    try
    {
      this.request.getResponse().finishResponse();
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      
      log.debug(sm.getString("asyncContextImpl.finishResponseError"), t);
    }
  }
  
  public boolean timeout()
  {
    AtomicBoolean result = new AtomicBoolean();
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_TIMEOUT, result);
    if (result.get())
    {
      ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
      ClassLoader newCL = this.request.getContext().getLoader().getClassLoader();
      try
      {
        Thread.currentThread().setContextClassLoader(newCL);
        List<AsyncListenerWrapper> listenersCopy = new ArrayList();
        
        listenersCopy.addAll(this.listeners);
        for (AsyncListenerWrapper listener : listenersCopy) {
          try
          {
            listener.fireOnTimeout(this.event);
          }
          catch (Throwable t)
          {
            ExceptionUtils.handleThrowable(t);
            log.warn("onTimeout() failed for listener of type [" + listener.getClass().getName() + "]", t);
          }
        }
        this.request.getCoyoteRequest().action(ActionCode.ASYNC_IS_TIMINGOUT, result);
        
        return !result.get() ? true : false;
      }
      finally
      {
        Thread.currentThread().setContextClassLoader(oldCL);
      }
    }
    return true;
  }
  
  public void dispatch()
  {
    check();
    
    ServletRequest servletRequest = getRequest();
    String pathInfo;
    String path;

    if ((servletRequest instanceof HttpServletRequest))
    {
      HttpServletRequest sr = (HttpServletRequest)servletRequest;
      path = sr.getServletPath();
      pathInfo = sr.getPathInfo();
    }
    else
    {
      path = this.request.getServletPath();
      pathInfo = this.request.getPathInfo();
    }
    if (pathInfo != null) {
      path = path + pathInfo;
    }
    dispatch(path);
  }
  
  public void dispatch(String path)
  {
    check();
    dispatch(this.request.getServletContext(), path);
  }
  
  public void dispatch(ServletContext context, String path)
  {
    if (log.isDebugEnabled()) {
      logDebug("dispatch   ");
    }
    check();
    if (this.dispatch != null) {
      throw new IllegalStateException(sm.getString("asyncContextImpl.dispatchingStarted"));
    }
    if (this.request.getAttribute("javax.servlet.async.request_uri") == null)
    {
      this.request.setAttribute("javax.servlet.async.request_uri", this.request.getRequestURI());
      this.request.setAttribute("javax.servlet.async.context_path", this.request.getContextPath());
      this.request.setAttribute("javax.servlet.async.servlet_path", this.request.getServletPath());
      this.request.setAttribute("javax.servlet.async.path_info", this.request.getPathInfo());
      this.request.setAttribute("javax.servlet.async.query_string", this.request.getQueryString());
    }
    RequestDispatcher requestDispatcher = context.getRequestDispatcher(path);
    if (!(requestDispatcher instanceof AsyncDispatcher)) {
      throw new UnsupportedOperationException(sm.getString("asyncContextImpl.noAsyncDispatcher"));
    }
    final AsyncDispatcher applicationDispatcher = (AsyncDispatcher)requestDispatcher;
    
    final ServletRequest servletRequest = getRequest();
    final ServletResponse servletResponse = getResponse();
    Runnable run = new Runnable()
    {
      public void run()
      {
        AsyncContextImpl.this.request.getCoyoteRequest().action(ActionCode.ASYNC_DISPATCHED, null);
        try
        {
          applicationDispatcher.dispatch(servletRequest, servletResponse);
        }
        catch (Exception x)
        {
          throw new RuntimeException(x);
        }
      }
    };
    this.dispatch = run;
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_DISPATCH, null);
    clearServletRequestResponse();
  }
  
  public ServletRequest getRequest()
  {
    check();
    if (this.servletRequest == null) {
      throw new IllegalStateException(sm.getString("asyncContextImpl.request.ise"));
    }
    return this.servletRequest;
  }
  
  public ServletResponse getResponse()
  {
    check();
    if (this.servletResponse == null) {
      throw new IllegalStateException(sm.getString("asyncContextImpl.response.ise"));
    }
    return this.servletResponse;
  }
  
  public void start(Runnable run)
  {
    if (log.isDebugEnabled()) {
      logDebug("start      ");
    }
    check();
    Runnable wrapper = new RunnableWrapper(run, this.context);
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_RUN, wrapper);
  }
  
  public void addListener(AsyncListener listener)
  {
    check();
    AsyncListenerWrapper wrapper = new AsyncListenerWrapper();
    wrapper.setListener(listener);
    this.listeners.add(wrapper);
  }
  
  public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse)
  {
    check();
    AsyncListenerWrapper wrapper = new AsyncListenerWrapper();
    wrapper.setListener(listener);
    this.listeners.add(wrapper);
  }
  
  public <T extends AsyncListener> T createListener(Class<T> clazz)
    throws ServletException
  {
    check();
    T listener = null;
    try
    {
      listener = (T)getInstanceManager().newInstance(clazz.getName(), clazz.getClassLoader());
    }
    catch (InstantiationException e)
    {
      ServletException se = new ServletException(e);
      throw se;
    }
    catch (IllegalAccessException e)
    {
      ServletException se = new ServletException(e);
      throw se;
    }
    catch (InvocationTargetException e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      ServletException se = new ServletException(e);
      throw se;
    }
    catch (NamingException e)
    {
      ServletException se = new ServletException(e);
      throw se;
    }
    catch (ClassNotFoundException e)
    {
      ServletException se = new ServletException(e);
      throw se;
    }
    catch (Exception e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      ServletException se = new ServletException(e);
      throw se;
    }
    return listener;
  }
  
  public void recycle()
  {
    if (log.isDebugEnabled()) {
      logDebug("recycle    ");
    }
    this.context = null;
    this.dispatch = null;
    this.event = null;
    this.hasOriginalRequestAndResponse = true;
    this.instanceManager = null;
    this.listeners.clear();
    this.request = null;
    clearServletRequestResponse();
    this.timeout = -1L;
  }
  
  private void clearServletRequestResponse()
  {
    this.servletRequest = null;
    this.servletResponse = null;
  }
  
  public boolean isStarted()
  {
    AtomicBoolean result = new AtomicBoolean(false);
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_IS_STARTED, result);
    
    return result.get();
  }
  
  public void setStarted(Context context, ServletRequest request, ServletResponse response, boolean originalRequestResponse)
  {
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_START, this);
    
    this.context = context;
    this.servletRequest = request;
    this.servletResponse = response;
    this.hasOriginalRequestAndResponse = originalRequestResponse;
    this.event = new AsyncEvent(this, request, response);
    
    List<AsyncListenerWrapper> listenersCopy = new ArrayList();
    
    listenersCopy.addAll(this.listeners);
    this.listeners.clear();
    for (AsyncListenerWrapper listener : listenersCopy) {
      try
      {
        listener.fireOnStartAsync(this.event);
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        log.warn("onStartAsync() failed for listener of type [" + listener.getClass().getName() + "]", t);
      }
    }
  }
  
  public boolean hasOriginalRequestAndResponse()
  {
    check();
    return this.hasOriginalRequestAndResponse;
  }
  
  protected void doInternalDispatch()
    throws ServletException, IOException
  {
    if (log.isDebugEnabled()) {
      logDebug("intDispatch");
    }
    try
    {
      Runnable runnable = this.dispatch;
      this.dispatch = null;
      runnable.run();
      if (!this.request.isAsync()) {
        fireOnComplete();
      }
    }
    catch (RuntimeException x)
    {
      if ((x.getCause() instanceof ServletException)) {
        throw ((ServletException)x.getCause());
      }
      if ((x.getCause() instanceof IOException)) {
        throw ((IOException)x.getCause());
      }
      throw new ServletException(x);
    }
  }
  
  public long getTimeout()
  {
    check();
    return this.timeout;
  }
  
  public void setTimeout(long timeout)
  {
    check();
    this.timeout = timeout;
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_SETTIMEOUT, Long.valueOf(timeout));
  }
  
  public void setErrorState(Throwable t, boolean fireOnError)
  {
    if (t != null) {
      this.request.setAttribute("javax.servlet.error.exception", t);
    }
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_ERROR, null);
    AsyncEvent errorEvent;
    if (fireOnError)
    {
      errorEvent = new AsyncEvent(this.event.getAsyncContext(), this.event.getSuppliedRequest(), this.event.getSuppliedResponse(), t);
      
      List<AsyncListenerWrapper> listenersCopy = new ArrayList();
      
      listenersCopy.addAll(this.listeners);
      for (AsyncListenerWrapper listener : listenersCopy) {
        try
        {
          listener.fireOnError(errorEvent);
        }
        catch (Throwable t2)
        {
          ExceptionUtils.handleThrowable(t);
          log.warn("onError() failed for listener of type [" + listener.getClass().getName() + "]", t2);
        }
      }
    }
    AtomicBoolean result = new AtomicBoolean();
    this.request.getCoyoteRequest().action(ActionCode.ASYNC_IS_ERROR, result);
    if (result.get())
    {
      if ((this.servletResponse instanceof HttpServletResponse)) {
        ((HttpServletResponse)this.servletResponse).setStatus(500);
      }
      Host host = (Host)this.context.getParent();
      Valve stdHostValve = host.getPipeline().getBasic();
      if ((stdHostValve instanceof StandardHostValve)) {
        ((StandardHostValve)stdHostValve).throwable(this.request, this.request.getResponse(), t);
      }
      this.request.getCoyoteRequest().action(ActionCode.ASYNC_IS_ERROR, result);
      if (result.get()) {
        complete();
      }
    }
  }
  
  private void logDebug(String method)
  {
    StringBuilder uri = new StringBuilder();
    String rHashCode;
    String crHashCode;
    String rpHashCode;
    String stage;
    if (this.request == null)
    {
      rHashCode = "null";
      crHashCode = "null";
      rpHashCode = "null";
      stage = "-";
      uri.append("N/A");
    }
    else
    {
      rHashCode = Integer.toHexString(this.request.hashCode());
      org.apache.coyote.Request coyoteRequest = this.request.getCoyoteRequest();
     
      if (coyoteRequest == null)
      {
        crHashCode = "null";
        rpHashCode = "null";
        stage = "-";
      }
      else
      {
        crHashCode = Integer.toHexString(coyoteRequest.hashCode());
        RequestInfo rp = coyoteRequest.getRequestProcessor();
     
        if (rp == null)
        {
          rpHashCode = "null";
          stage = "-";
        }
        else
        {
          rpHashCode = Integer.toHexString(rp.hashCode());
          stage = Integer.toString(rp.getStage());
        }
      }
      uri.append(this.request.getRequestURI());
      if (this.request.getQueryString() != null)
      {
        uri.append('?');
        uri.append(this.request.getQueryString());
      }
    }
    String threadName = Thread.currentThread().getName();
    int len = threadName.length();
    if (len > 20) {
      threadName = threadName.substring(len - 20, len);
    }
    String msg = String.format("Req: %1$8s  CReq: %2$8s  RP: %3$8s  Stage: %4$s  Thread: %5$20s  State: %6$20s  Method: %7$11s  URI: %8$s", new Object[] { rHashCode, crHashCode, rpHashCode, stage, threadName, "N/A", method, uri });
    if (log.isTraceEnabled()) {
      log.trace(msg, new DebugException());
    } else {
      log.debug(msg);
    }
  }
  
  private InstanceManager getInstanceManager()
  {
    if (this.instanceManager == null) {
      if ((this.context instanceof StandardContext)) {
        this.instanceManager = ((StandardContext)this.context).getInstanceManager();
      } else {
        this.instanceManager = new DefaultInstanceManager(null, new HashMap(), this.context, getClass().getClassLoader());
      }
    }
    return this.instanceManager;
  }
  
  private void check()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("asyncContextImpl.requestEnded"));
    }
  }
  
  private static class RunnableWrapper
    implements Runnable
  {
    private Runnable wrapped = null;
    private Context context = null;
    
    public RunnableWrapper(Runnable wrapped, Context ctxt)
    {
      this.wrapped = wrapped;
      this.context = ctxt;
    }
    
    public void run()
    {
      ClassLoader oldCL;

      if (Globals.IS_SECURITY_ENABLED)
      {
        PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
        oldCL = (ClassLoader)AccessController.doPrivileged(pa);
      }
      else
      {
        oldCL = Thread.currentThread().getContextClassLoader();
      }
      try
      {
        if (Globals.IS_SECURITY_ENABLED)
        {
          PrivilegedAction<Void> pa = new PrivilegedSetTccl(this.context.getLoader().getClassLoader());
          
          AccessController.doPrivileged(pa);
        }
        else
        {
          Thread.currentThread().setContextClassLoader(this.context.getLoader().getClassLoader());
        }
        this.wrapped.run();
      }
      finally
      {
        PrivilegedAction<Void> pa;
        if (Globals.IS_SECURITY_ENABLED)
        {
         pa = new PrivilegedSetTccl(oldCL);
          
          AccessController.doPrivileged(pa);
        }
        else
        {
          Thread.currentThread().setContextClassLoader(oldCL);
        }
      }
    }
  }
  
  private static class DebugException
    extends Exception
  {
    private static final long serialVersionUID = 1L;
    
    private DebugException() {}
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\AsyncContextImpl.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */