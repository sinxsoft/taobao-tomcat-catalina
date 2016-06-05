package org.apache.catalina.core;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.PrivilegedSetTccl;

final class StandardHostValve
  extends ValveBase
{
  private static final Log log = LogFactory.getLog(StandardHostValve.class);
  protected static final boolean STRICT_SERVLET_COMPLIANCE = Globals.STRICT_SERVLET_COMPLIANCE;
  protected static final boolean ACCESS_SESSION;
  private static final String info = "org.apache.catalina.core.StandardHostValve/1.0";
  
  static
  {
    String accessSession = System.getProperty("org.apache.catalina.core.StandardHostValve.ACCESS_SESSION");
    if (accessSession == null) {
      ACCESS_SESSION = STRICT_SERVLET_COMPLIANCE;
    } else {
      ACCESS_SESSION = Boolean.valueOf(accessSession).booleanValue();
    }
  }
  
  public StandardHostValve()
  {
    super(true);
  }
  
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardHostValve/1.0";
  }
  
  public final void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    Context context = request.getContext();
    if (context == null)
    {
      response.sendError(500, sm.getString("standardHost.noContext"));
      
      return;
    }
    if (context.getLoader() != null) {
      if (Globals.IS_SECURITY_ENABLED)
      {
        PrivilegedAction<Void> pa = new PrivilegedSetTccl(context.getLoader().getClassLoader());
        
        AccessController.doPrivileged(pa);
      }
      else
      {
        Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
      }
    }
    if (request.isAsyncSupported()) {
      request.setAsyncSupported(context.getPipeline().isAsyncSupported());
    }
    boolean asyncAtStart = request.isAsync();
    boolean asyncDispatching = request.isAsyncDispatching();
    if ((asyncAtStart) || (context.fireRequestInitEvent(request)))
    {
      try
      {
        if ((!asyncAtStart) || (asyncDispatching)) {
          context.getPipeline().getFirst().invoke(request, response);
        } else if (!response.isErrorReportRequired()) {
          throw new IllegalStateException(sm.getString("standardHost.asyncStateError"));
        }
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if (response.isErrorReportRequired())
        {
          this.container.getLogger().error("Exception Processing " + request.getRequestURI(), t);
        }
        else
        {
          request.setAttribute("javax.servlet.error.exception", t);
          throwable(request, response, t);
        }
      }
      response.setSuspended(false);
      
      Throwable t = (Throwable)request.getAttribute("javax.servlet.error.exception");
      if (!context.getState().isAvailable()) {
        return;
      }
      if (response.isErrorReportRequired()) {
        if (t != null) {
          throwable(request, response, t);
        } else {
          status(request, response);
        }
      }
      if ((!request.isAsync()) && (!response.isErrorReportRequired())) {
        context.fireRequestDestroyEvent(request);
      }
    }
    if (ACCESS_SESSION) {
      request.getSession(false);
    }
    if (Globals.IS_SECURITY_ENABLED)
    {
      PrivilegedAction<Void> pa = new PrivilegedSetTccl(StandardHostValve.class.getClassLoader());
      
      AccessController.doPrivileged(pa);
    }
    else
    {
      Thread.currentThread().setContextClassLoader(StandardHostValve.class.getClassLoader());
    }
  }
  
  public final void event(Request request, Response response, CometEvent event)
    throws IOException, ServletException
  {
    Context context = request.getContext();
    if (context.getLoader() != null) {
      Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
    }
    context.getPipeline().getFirst().event(request, response, event);
    
    response.setSuspended(false);
    
    Throwable t = (Throwable)request.getAttribute("javax.servlet.error.exception");
    if (t != null) {
      throwable(request, response, t);
    } else {
      status(request, response);
    }
    if (ACCESS_SESSION) {
      request.getSession(false);
    }
    Thread.currentThread().setContextClassLoader(StandardHostValve.class.getClassLoader());
  }
  
  private void status(Request request, Response response)
  {
    int statusCode = response.getStatus();
    
    Context context = request.getContext();
    if (context == null) {
      return;
    }
    if (!response.isError()) {
      return;
    }
    ErrorPage errorPage = context.findErrorPage(statusCode);
    if (errorPage == null) {
      errorPage = context.findErrorPage(0);
    }
    if ((errorPage != null) && (response.setErrorReported()))
    {
      response.setAppCommitted(false);
      request.setAttribute("javax.servlet.error.status_code", Integer.valueOf(statusCode));
      
      String message = response.getMessage();
      if (message == null) {
        message = "";
      }
      request.setAttribute("javax.servlet.error.message", message);
      request.setAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH", errorPage.getLocation());
      
      request.setAttribute("org.apache.catalina.core.DISPATCHER_TYPE", DispatcherType.ERROR);
      
      Wrapper wrapper = request.getWrapper();
      if (wrapper != null) {
        request.setAttribute("javax.servlet.error.servlet_name", wrapper.getName());
      }
      request.setAttribute("javax.servlet.error.request_uri", request.getRequestURI());
      if (custom(request, response, errorPage)) {
        try
        {
          response.finishResponse();
        }
        catch (ClientAbortException e) {}catch (IOException e)
        {
          this.container.getLogger().warn("Exception Processing " + errorPage, e);
        }
      }
    }
  }
  
  protected void throwable(Request request, Response response, Throwable throwable)
  {
    Context context = request.getContext();
    if (context == null) {
      return;
    }
    Throwable realError = throwable;
    if ((realError instanceof ServletException))
    {
      realError = ((ServletException)realError).getRootCause();
      if (realError == null) {
        realError = throwable;
      }
    }
    if ((realError instanceof ClientAbortException))
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("standardHost.clientAbort", new Object[] { realError.getCause().getMessage() }));
      }
      return;
    }
    ErrorPage errorPage = findErrorPage(context, throwable);
    if ((errorPage == null) && (realError != throwable)) {
      errorPage = findErrorPage(context, realError);
    }
    if (errorPage != null)
    {
      if (response.setErrorReported())
      {
        response.setAppCommitted(false);
        request.setAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH", errorPage.getLocation());
        
        request.setAttribute("org.apache.catalina.core.DISPATCHER_TYPE", DispatcherType.ERROR);
        
        request.setAttribute("javax.servlet.error.status_code", new Integer(500));
        
        request.setAttribute("javax.servlet.error.message", throwable.getMessage());
        
        request.setAttribute("javax.servlet.error.exception", realError);
        
        Wrapper wrapper = request.getWrapper();
        if (wrapper != null) {
          request.setAttribute("javax.servlet.error.servlet_name", wrapper.getName());
        }
        request.setAttribute("javax.servlet.error.request_uri", request.getRequestURI());
        
        request.setAttribute("javax.servlet.error.exception_type", realError.getClass());
        if (custom(request, response, errorPage)) {
          try
          {
            response.finishResponse();
          }
          catch (IOException e)
          {
            this.container.getLogger().warn("Exception Processing " + errorPage, e);
          }
        }
      }
    }
    else
    {
      response.setStatus(500);
      
      response.setError();
      
      status(request, response);
    }
  }
  
  private boolean custom(Request request, Response response, ErrorPage errorPage)
  {
    if (this.container.getLogger().isDebugEnabled()) {
      this.container.getLogger().debug("Processing " + errorPage);
    }
    try
    {
      ServletContext servletContext = request.getContext().getServletContext();
      
      RequestDispatcher rd = servletContext.getRequestDispatcher(errorPage.getLocation());
      if (response.isCommitted())
      {
        rd.include(request.getRequest(), response.getResponse());
      }
      else
      {
        response.resetBuffer(true);
        response.setContentLength(-1);
        
        rd.forward(request.getRequest(), response.getResponse());
        
        response.setSuspended(false);
      }
      return true;
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      
      this.container.getLogger().error("Exception Processing " + errorPage, t);
    }
    return false;
  }
  
  private static ErrorPage findErrorPage(Context context, Throwable exception)
  {
    if (exception == null) {
      return null;
    }
    Class<?> clazz = exception.getClass();
    String name = clazz.getName();
    while (!Object.class.equals(clazz))
    {
      ErrorPage errorPage = context.findErrorPage(name);
      if (errorPage != null) {
        return errorPage;
      }
      clazz = clazz.getSuperclass();
      if (clazz == null) {
        break;
      }
      name = clazz.getName();
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardHostValve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */