package org.apache.catalina.core;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Globals;
import org.apache.catalina.Wrapper;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometFilter;
import org.apache.catalina.comet.CometFilterChain;
import org.apache.catalina.comet.CometProcessor;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.InstanceSupport;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

final class ApplicationFilterChain
  implements FilterChain, CometFilterChain
{
  private static final ThreadLocal<ServletRequest> lastServicedRequest;
  private static final ThreadLocal<ServletResponse> lastServicedResponse;
  public static final int INCREMENT = 10;
  
  static
  {
    if (ApplicationDispatcher.WRAP_SAME_OBJECT)
    {
      lastServicedRequest = new ThreadLocal();
      lastServicedResponse = new ThreadLocal();
    }
    else
    {
      lastServicedRequest = null;
      lastServicedResponse = null;
    }
  }
  
  private ApplicationFilterConfig[] filters = new ApplicationFilterConfig[0];
  private int pos = 0;
  private int n = 0;
  private Servlet servlet = null;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private InstanceSupport support = null;
  private static Class<?>[] classType = { ServletRequest.class, ServletResponse.class, FilterChain.class };
  private static Class<?>[] classTypeUsedInService = { ServletRequest.class, ServletResponse.class };
  private static Class<?>[] cometClassType = { CometEvent.class, CometFilterChain.class };
  private static Class<?>[] classTypeUsedInEvent = { CometEvent.class };
  
  public void doFilter(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    if (Globals.IS_SECURITY_ENABLED)
    {
      final ServletRequest req = request;
      final ServletResponse res = response;
      try
      {
        AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Void run()
            throws ServletException, IOException
          {
            ApplicationFilterChain.this.internalDoFilter(req, res);
            return null;
          }
        });
      }
      catch (PrivilegedActionException pe)
      {
        Exception e = pe.getException();
        if ((e instanceof ServletException)) {
          throw ((ServletException)e);
        }
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        if ((e instanceof RuntimeException)) {
          throw ((RuntimeException)e);
        }
        throw new ServletException(e.getMessage(), e);
      }
    }
    else
    {
      internalDoFilter(request, response);
    }
  }
  
  private void internalDoFilter(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    if (this.pos < this.n)
    {
      ApplicationFilterConfig filterConfig = this.filters[(this.pos++)];
      Filter filter = null;
      try
      {
        filter = filterConfig.getFilter();
        this.support.fireInstanceEvent("beforeFilter", filter, request, response);
        if ((request.isAsyncSupported()) && ("false".equalsIgnoreCase(filterConfig.getFilterDef().getAsyncSupported()))) {
          request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", Boolean.FALSE);
        }
        if (Globals.IS_SECURITY_ENABLED)
        {
          ServletRequest req = request;
          ServletResponse res = response;
          Principal principal = ((HttpServletRequest)req).getUserPrincipal();
          
          Object[] args = { req, res, this };
          SecurityUtil.doAsPrivilege("doFilter", filter, classType, args, principal);
        }
        else
        {
          filter.doFilter(request, response, this);
        }
        this.support.fireInstanceEvent("afterFilter", filter, request, response);
      }
      catch (IOException e)
      {
        if (filter != null) {
          this.support.fireInstanceEvent("afterFilter", filter, request, response, e);
        }
        throw e;
      }
      catch (ServletException e)
      {
        if (filter != null) {
          this.support.fireInstanceEvent("afterFilter", filter, request, response, e);
        }
        throw e;
      }
      catch (RuntimeException e)
      {
        if (filter != null) {
          this.support.fireInstanceEvent("afterFilter", filter, request, response, e);
        }
        throw e;
      }
      catch (Throwable e)
      {
        e = ExceptionUtils.unwrapInvocationTargetException(e);
        ExceptionUtils.handleThrowable(e);
        if (filter != null) {
          this.support.fireInstanceEvent("afterFilter", filter, request, response, e);
        }
        throw new ServletException(sm.getString("filterChain.filter"), e);
      }
      return;
    }
    try
    {
      if (ApplicationDispatcher.WRAP_SAME_OBJECT)
      {
        lastServicedRequest.set(request);
        lastServicedResponse.set(response);
      }
      this.support.fireInstanceEvent("beforeService", this.servlet, request, response);
      if ((request.isAsyncSupported()) && (!this.support.getWrapper().isAsyncSupported())) {
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", Boolean.FALSE);
      }
      if (((request instanceof HttpServletRequest)) && ((response instanceof HttpServletResponse)))
      {
        if (Globals.IS_SECURITY_ENABLED)
        {
          ServletRequest req = request;
          ServletResponse res = response;
          Principal principal = ((HttpServletRequest)req).getUserPrincipal();
          
          Object[] args = { req, res };
          SecurityUtil.doAsPrivilege("service", this.servlet, classTypeUsedInService, args, principal);
        }
        else
        {
          this.servlet.service(request, response);
        }
      }
      else {
        this.servlet.service(request, response);
      }
      this.support.fireInstanceEvent("afterService", this.servlet, request, response);
    }
    catch (IOException e)
    {
      this.support.fireInstanceEvent("afterService", this.servlet, request, response, e);
      
      throw e;
    }
    catch (ServletException e)
    {
      this.support.fireInstanceEvent("afterService", this.servlet, request, response, e);
      
      throw e;
    }
    catch (RuntimeException e)
    {
      this.support.fireInstanceEvent("afterService", this.servlet, request, response, e);
      
      throw e;
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.support.fireInstanceEvent("afterService", this.servlet, request, response, e);
      
      throw new ServletException(sm.getString("filterChain.servlet"), e);
    }
    finally
    {
      if (ApplicationDispatcher.WRAP_SAME_OBJECT)
      {
        lastServicedRequest.set(null);
        lastServicedResponse.set(null);
      }
    }
  }
  
  public void doFilterEvent(CometEvent event)
    throws IOException, ServletException
  {
    if (Globals.IS_SECURITY_ENABLED)
    {
      final CometEvent ev = event;
      try
      {
        AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Void run()
            throws ServletException, IOException
          {
            ApplicationFilterChain.this.internalDoFilterEvent(ev);
            return null;
          }
        });
      }
      catch (PrivilegedActionException pe)
      {
        Exception e = pe.getException();
        if ((e instanceof ServletException)) {
          throw ((ServletException)e);
        }
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        if ((e instanceof RuntimeException)) {
          throw ((RuntimeException)e);
        }
        throw new ServletException(e.getMessage(), e);
      }
    }
    else
    {
      internalDoFilterEvent(event);
    }
  }
  
  public static ServletRequest getLastServicedRequest()
  {
    return (ServletRequest)lastServicedRequest.get();
  }
  
  public static ServletResponse getLastServicedResponse()
  {
    return (ServletResponse)lastServicedResponse.get();
  }
  
  private void internalDoFilterEvent(CometEvent event)
    throws IOException, ServletException
  {
    if (this.pos < this.n)
    {
      ApplicationFilterConfig filterConfig = this.filters[(this.pos++)];
      CometFilter filter = null;
      try
      {
        filter = (CometFilter)filterConfig.getFilter();
        if (Globals.IS_SECURITY_ENABLED)
        {
          CometEvent ev = event;
          Principal principal = ev.getHttpServletRequest().getUserPrincipal();
          
          Object[] args = { ev, this };
          SecurityUtil.doAsPrivilege("doFilterEvent", filter, cometClassType, args, principal);
        }
        else
        {
          filter.doFilterEvent(event, this);
        }
      }
      catch (IOException e)
      {
        throw e;
      }
      catch (ServletException e)
      {
        throw e;
      }
      catch (RuntimeException e)
      {
        throw e;
      }
      catch (Throwable e)
      {
        e = ExceptionUtils.unwrapInvocationTargetException(e);
        ExceptionUtils.handleThrowable(e);
        
        throw new ServletException(sm.getString("filterChain.filter"), e);
      }
      return;
    }
    try
    {
      if (Globals.IS_SECURITY_ENABLED)
      {
        CometEvent ev = event;
        Principal principal = ev.getHttpServletRequest().getUserPrincipal();
        
        Object[] args = { ev };
        SecurityUtil.doAsPrivilege("event", this.servlet, classTypeUsedInEvent, args, principal);
      }
      else
      {
        ((CometProcessor)this.servlet).event(event);
      }
    }
    catch (IOException e)
    {
      throw e;
    }
    catch (ServletException e)
    {
      throw e;
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      
      throw new ServletException(sm.getString("filterChain.servlet"), e);
    }
  }
  
  void addFilter(ApplicationFilterConfig filterConfig)
  {
    for (ApplicationFilterConfig filter : this.filters) {
      if (filter == filterConfig) {
        return;
      }
    }
    if (this.n == this.filters.length)
    {
      ApplicationFilterConfig[] newFilters = new ApplicationFilterConfig[this.n + 10];
      
      System.arraycopy(this.filters, 0, newFilters, 0, this.n);
      this.filters = newFilters;
    }
    this.filters[(this.n++)] = filterConfig;
  }
  
  void release()
  {
    for (int i = 0; i < this.n; i++) {
      this.filters[i] = null;
    }
    this.n = 0;
    this.pos = 0;
    this.servlet = null;
    this.support = null;
  }
  
  void reuse()
  {
    this.pos = 0;
  }
  
  void setServlet(Servlet servlet)
  {
    this.servlet = servlet;
  }
  
  void setSupport(InstanceSupport support)
  {
    this.support = support;
  }
  
  public ApplicationFilterChain() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationFilterChain.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */