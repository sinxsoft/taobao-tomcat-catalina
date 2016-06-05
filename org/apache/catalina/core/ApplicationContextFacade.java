package org.apache.catalina.core;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityUtil;
import org.apache.tomcat.util.ExceptionUtils;

public class ApplicationContextFacade
  implements ServletContext
{
  private final Map<String, Class<?>[]> classCache;
  private final Map<String, Method> objectCache;
  
  public ApplicationContextFacade(ApplicationContext context)
  {
    this.context = context;
    
    this.classCache = new HashMap();
    this.objectCache = new ConcurrentHashMap();
    initClassCache();
  }
  
  private void initClassCache()
  {
    Class<?>[] clazz = { String.class };
    this.classCache.put("getContext", clazz);
    this.classCache.put("getMimeType", clazz);
    this.classCache.put("getResourcePaths", clazz);
    this.classCache.put("getResource", clazz);
    this.classCache.put("getResourceAsStream", clazz);
    this.classCache.put("getRequestDispatcher", clazz);
    this.classCache.put("getNamedDispatcher", clazz);
    this.classCache.put("getServlet", clazz);
    this.classCache.put("setInitParameter", new Class[] { String.class, String.class });
    this.classCache.put("createServlet", new Class[] { Class.class });
    this.classCache.put("addServlet", new Class[] { String.class, String.class });
    this.classCache.put("createFilter", new Class[] { Class.class });
    this.classCache.put("addFilter", new Class[] { String.class, String.class });
    this.classCache.put("createListener", new Class[] { Class.class });
    this.classCache.put("addListener", clazz);
    this.classCache.put("getFilterRegistration", clazz);
    this.classCache.put("getServletRegistration", clazz);
    this.classCache.put("getInitParameter", clazz);
    this.classCache.put("setAttribute", new Class[] { String.class, Object.class });
    this.classCache.put("removeAttribute", clazz);
    this.classCache.put("getRealPath", clazz);
    this.classCache.put("getAttribute", clazz);
    this.classCache.put("log", clazz);
    this.classCache.put("setSessionTrackingModes", new Class[] { Set.class });
  }
  
  private ApplicationContext context = null;
  
  public ServletContext getContext(String uripath)
  {
    ServletContext theContext = null;
    if (SecurityUtil.isPackageProtectionEnabled()) {
      theContext = (ServletContext)doPrivileged("getContext", new Object[] { uripath });
    } else {
      theContext = this.context.getContext(uripath);
    }
    if ((theContext != null) && ((theContext instanceof ApplicationContext))) {
      theContext = ((ApplicationContext)theContext).getFacade();
    }
    return theContext;
  }
  
  public int getMajorVersion()
  {
    return this.context.getMajorVersion();
  }
  
  public int getMinorVersion()
  {
    return this.context.getMinorVersion();
  }
  
  public String getMimeType(String file)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getMimeType", new Object[] { file });
    }
    return this.context.getMimeType(file);
  }
  
  public Set<String> getResourcePaths(String path)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Set)doPrivileged("getResourcePaths", new Object[] { path });
    }
    return this.context.getResourcePaths(path);
  }
  
  public URL getResource(String path)
    throws MalformedURLException
  {
    if (Globals.IS_SECURITY_ENABLED) {
      try
      {
        return (URL)invokeMethod(this.context, "getResource", new Object[] { path });
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if ((t instanceof MalformedURLException)) {
          throw ((MalformedURLException)t);
        }
        return null;
      }
    }
    return this.context.getResource(path);
  }
  
  public InputStream getResourceAsStream(String path)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (InputStream)doPrivileged("getResourceAsStream", new Object[] { path });
    }
    return this.context.getResourceAsStream(path);
  }
  
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (RequestDispatcher)doPrivileged("getRequestDispatcher", new Object[] { path });
    }
    return this.context.getRequestDispatcher(path);
  }
  
  public RequestDispatcher getNamedDispatcher(String name)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (RequestDispatcher)doPrivileged("getNamedDispatcher", new Object[] { name });
    }
    return this.context.getNamedDispatcher(name);
  }
  
  @Deprecated
  public Servlet getServlet(String name)
    throws ServletException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        return (Servlet)invokeMethod(this.context, "getServlet", new Object[] { name });
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if ((t instanceof ServletException)) {
          throw ((ServletException)t);
        }
        return null;
      }
    }
    return this.context.getServlet(name);
  }
  
  @Deprecated
  public Enumeration<Servlet> getServlets()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Enumeration)doPrivileged("getServlets", null);
    }
    return this.context.getServlets();
  }
  
  @Deprecated
  public Enumeration<String> getServletNames()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Enumeration)doPrivileged("getServletNames", null);
    }
    return this.context.getServletNames();
  }
  
  public void log(String msg)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("log", new Object[] { msg });
    } else {
      this.context.log(msg);
    }
  }
  
  @Deprecated
  public void log(Exception exception, String msg)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("log", new Class[] { Exception.class, String.class }, new Object[] { exception, msg });
    } else {
      this.context.log(exception, msg);
    }
  }
  
  public void log(String message, Throwable throwable)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("log", new Class[] { String.class, Throwable.class }, new Object[] { message, throwable });
    } else {
      this.context.log(message, throwable);
    }
  }
  
  public String getRealPath(String path)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getRealPath", new Object[] { path });
    }
    return this.context.getRealPath(path);
  }
  
  public String getServerInfo()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getServerInfo", null);
    }
    return this.context.getServerInfo();
  }
  
  public String getInitParameter(String name)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getInitParameter", new Object[] { name });
    }
    return this.context.getInitParameter(name);
  }
  
  public Enumeration<String> getInitParameterNames()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Enumeration)doPrivileged("getInitParameterNames", null);
    }
    return this.context.getInitParameterNames();
  }
  
  public Object getAttribute(String name)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return doPrivileged("getAttribute", new Object[] { name });
    }
    return this.context.getAttribute(name);
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Enumeration)doPrivileged("getAttributeNames", null);
    }
    return this.context.getAttributeNames();
  }
  
  public void setAttribute(String name, Object object)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("setAttribute", new Object[] { name, object });
    } else {
      this.context.setAttribute(name, object);
    }
  }
  
  public void removeAttribute(String name)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("removeAttribute", new Object[] { name });
    } else {
      this.context.removeAttribute(name);
    }
  }
  
  public String getServletContextName()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getServletContextName", null);
    }
    return this.context.getServletContextName();
  }
  
  public String getContextPath()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (String)doPrivileged("getContextPath", null);
    }
    return this.context.getContextPath();
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, String className)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (FilterRegistration.Dynamic)doPrivileged("addFilter", new Object[] { filterName, className });
    }
    return this.context.addFilter(filterName, className);
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (FilterRegistration.Dynamic)doPrivileged("addFilter", new Class[] { String.class, Filter.class }, new Object[] { filterName, filter });
    }
    return this.context.addFilter(filterName, filter);
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (FilterRegistration.Dynamic)doPrivileged("addFilter", new Class[] { String.class, Class.class }, new Object[] { filterName, filterClass });
    }
    return this.context.addFilter(filterName, filterClass);
  }
  
  public <T extends Filter> T createFilter(Class<T> c)
    throws ServletException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        return (T)invokeMethod(this.context, "createFilter", new Object[] { c });
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if ((t instanceof ServletException)) {
          throw ((ServletException)t);
        }
        return null;
      }
    }
    return this.context.createFilter(c);
  }
  
  public FilterRegistration getFilterRegistration(String filterName)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (FilterRegistration)doPrivileged("getFilterRegistration", new Object[] { filterName });
    }
    return this.context.getFilterRegistration(filterName);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, String className)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (ServletRegistration.Dynamic)doPrivileged("addServlet", new Object[] { servletName, className });
    }
    return this.context.addServlet(servletName, className);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (ServletRegistration.Dynamic)doPrivileged("addServlet", new Class[] { String.class, Servlet.class }, new Object[] { servletName, servlet });
    }
    return this.context.addServlet(servletName, servlet);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (ServletRegistration.Dynamic)doPrivileged("addServlet", new Class[] { String.class, Class.class }, new Object[] { servletName, servletClass });
    }
    return this.context.addServlet(servletName, servletClass);
  }
  
  public <T extends Servlet> T createServlet(Class<T> c)
    throws ServletException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        return (T)invokeMethod(this.context, "createServlet", new Object[] { c });
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if ((t instanceof ServletException)) {
          throw ((ServletException)t);
        }
        return null;
      }
    }
    return this.context.createServlet(c);
  }
  
  public ServletRegistration getServletRegistration(String servletName)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (ServletRegistration)doPrivileged("getServletRegistration", new Object[] { servletName });
    }
    return this.context.getServletRegistration(servletName);
  }
  
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Set)doPrivileged("getDefaultSessionTrackingModes", null);
    }
    return this.context.getDefaultSessionTrackingModes();
  }
  
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Set)doPrivileged("getEffectiveSessionTrackingModes", null);
    }
    return this.context.getEffectiveSessionTrackingModes();
  }
  
  public SessionCookieConfig getSessionCookieConfig()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (SessionCookieConfig)doPrivileged("getSessionCookieConfig", null);
    }
    return this.context.getSessionCookieConfig();
  }
  
  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("setSessionTrackingModes", new Object[] { sessionTrackingModes });
    } else {
      this.context.setSessionTrackingModes(sessionTrackingModes);
    }
  }
  
  public boolean setInitParameter(String name, String value)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return ((Boolean)doPrivileged("setInitParameter", new Object[] { name, value })).booleanValue();
    }
    return this.context.setInitParameter(name, value);
  }
  
  public void addListener(Class<? extends EventListener> listenerClass)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("addListener", new Class[] { Class.class }, new Object[] { listenerClass });
    } else {
      this.context.addListener(listenerClass);
    }
  }
  
  public void addListener(String className)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("addListener", new Object[] { className });
    } else {
      this.context.addListener(className);
    }
  }
  
  public <T extends EventListener> void addListener(T t)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("addListener", new Class[] { EventListener.class }, new Object[] { t });
    } else {
      this.context.addListener(t);
    }
  }
  
  public <T extends EventListener> T createListener(Class<T> c)
    throws ServletException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        return (T)invokeMethod(this.context, "createListener", new Object[] { c });
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        if ((t instanceof ServletException)) {
          throw ((ServletException)t);
        }
        return null;
      }
    }
    return this.context.createListener(c);
  }
  
  public void declareRoles(String... roleNames)
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      doPrivileged("declareRoles", new Object[] { roleNames });
    } else {
      this.context.declareRoles(roleNames);
    }
  }
  
  public ClassLoader getClassLoader()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (ClassLoader)doPrivileged("getClassLoader", null);
    }
    return this.context.getClassLoader();
  }
  
  public int getEffectiveMajorVersion()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return ((Integer)doPrivileged("getEffectiveMajorVersion", null)).intValue();
    }
    return this.context.getEffectiveMajorVersion();
  }
  
  public int getEffectiveMinorVersion()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return ((Integer)doPrivileged("getEffectiveMinorVersion", null)).intValue();
    }
    return this.context.getEffectiveMinorVersion();
  }
  
  public Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Map)doPrivileged("getFilterRegistrations", null);
    }
    return this.context.getFilterRegistrations();
  }
  
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (JspConfigDescriptor)doPrivileged("getJspConfigDescriptor", null);
    }
    return this.context.getJspConfigDescriptor();
  }
  
  public Map<String, ? extends ServletRegistration> getServletRegistrations()
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (Map)doPrivileged("getServletRegistrations", null);
    }
    return this.context.getServletRegistrations();
  }
  
  private Object doPrivileged(String methodName, Object[] params)
  {
    try
    {
      return invokeMethod(this.context, methodName, params);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      throw new RuntimeException(t.getMessage(), t);
    }
  }
  
  private Object invokeMethod(ApplicationContext appContext, String methodName, Object[] params)
    throws Throwable
  {
    try
    {
      Method method = (Method)this.objectCache.get(methodName);
      if (method == null)
      {
        method = appContext.getClass().getMethod(methodName, (Class[])this.classCache.get(methodName));
        
        this.objectCache.put(methodName, method);
      }
      return executeMethod(method, appContext, params);
    }
    catch (Exception ex)
    {
      Object localObject1;
      handleException(ex);
      return null;
    }
    finally
    {
      params = null;
    }
  }
  
  private Object doPrivileged(String methodName, Class<?>[] clazz, Object[] params)
  {
    try
    {
      Method method = this.context.getClass().getMethod(methodName, clazz);
      return executeMethod(method, this.context, params);
    }
    catch (Exception ex)
    {
      try
      {
        handleException(ex);
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        throw new RuntimeException(t.getMessage());
      }
      return null;
    }
    finally
    {
      params = null;
    }
  }
  
  private Object executeMethod(final Method method, final ApplicationContext context, final Object[] params)
    throws PrivilegedActionException, IllegalAccessException, InvocationTargetException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
        public Object run()
          throws IllegalAccessException, InvocationTargetException
        {
          return method.invoke(context, params);
        }
      });
    }
    return method.invoke(context, params);
  }
  
  private void handleException(Exception ex)
    throws Throwable
  {
    if ((ex instanceof PrivilegedActionException)) {
      ex = ((PrivilegedActionException)ex).getException();
    }
    Throwable realException;
    if ((ex instanceof InvocationTargetException))
    {
      realException = ex.getCause();
      if (realException == null) {
        realException = ex;
      }
    }
    else
    {
      realException = ex;
    }
    throw realException;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationContextFacade.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */