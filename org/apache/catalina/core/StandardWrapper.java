package org.apache.catalina.core;

import java.beans.PropertyChangeSupport;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpServlet;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Wrapper;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.InstanceSupport;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.PeriodicEventListener;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.modeler.Util;
import org.apache.tomcat.util.res.StringManager;

public class StandardWrapper
  extends ContainerBase
  implements ServletConfig, Wrapper, NotificationEmitter
{
  private static final Log log = LogFactory.getLog(StandardWrapper.class);
  protected static final String[] DEFAULT_SERVLET_METHODS = { "GET", "HEAD", "POST" };
  
  public StandardWrapper()
  {
    this.swValve = new StandardWrapperValve();
    this.pipeline.setBasic(this.swValve);
    this.broadcaster = new NotificationBroadcasterSupport();
  }
  
  protected long available = 0L;
  protected NotificationBroadcasterSupport broadcaster = null;
  protected AtomicInteger countAllocated = new AtomicInteger(0);
  protected StandardWrapperFacade facade = new StandardWrapperFacade(this);
  protected static final String info = "org.apache.catalina.core.StandardWrapper/1.0";
  protected volatile Servlet instance = null;
  protected volatile boolean instanceInitialized = false;
  protected InstanceSupport instanceSupport = new InstanceSupport(this);
  protected int loadOnStartup = -1;
  protected ArrayList<String> mappings = new ArrayList();
  protected HashMap<String, String> parameters = new HashMap();
  protected HashMap<String, String> references = new HashMap();
  protected String runAs = null;
  protected long sequenceNumber = 0L;
  protected String servletClass = null;
  protected volatile boolean singleThreadModel = false;
  protected volatile boolean unloading = false;
  protected int maxInstances = 20;
  protected int nInstances = 0;
  protected Stack<Servlet> instancePool = null;
  protected long unloadDelay = 2000L;
  protected boolean isJspServlet;
  protected ObjectName jspMonitorON;
  protected boolean swallowOutput = false;
  protected StandardWrapperValve swValve;
  protected long loadTime = 0L;
  protected int classLoadTime = 0;
  protected MultipartConfigElement multipartConfigElement = null;
  protected boolean asyncSupported = false;
  protected boolean enabled = true;
  protected volatile boolean servletSecurityAnnotationScanRequired = false;
  private boolean overridable = false;
  protected static Class<?>[] classType = { ServletConfig.class };
  @Deprecated
  protected static Class<?>[] classTypeUsedInService = { ServletRequest.class, ServletResponse.class };
  private final ReentrantReadWriteLock parametersLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock mappingsLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock referencesLock = new ReentrantReadWriteLock();
  protected MBeanNotificationInfo[] notificationInfo;
  
  public boolean isOverridable()
  {
    return this.overridable;
  }
  
  public void setOverridable(boolean overridable)
  {
    this.overridable = overridable;
  }
  
  public long getAvailable()
  {
    return this.available;
  }
  
  public void setAvailable(long available)
  {
    long oldAvailable = this.available;
    if (available > System.currentTimeMillis()) {
      this.available = available;
    } else {
      this.available = 0L;
    }
    this.support.firePropertyChange("available", Long.valueOf(oldAvailable), Long.valueOf(this.available));
  }
  
  public int getCountAllocated()
  {
    return this.countAllocated.get();
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardWrapper/1.0";
  }
  
  public InstanceSupport getInstanceSupport()
  {
    return this.instanceSupport;
  }
  
  public int getLoadOnStartup()
  {
    if ((this.isJspServlet) && (this.loadOnStartup < 0)) {
      return Integer.MAX_VALUE;
    }
    return this.loadOnStartup;
  }
  
  public void setLoadOnStartup(int value)
  {
    int oldLoadOnStartup = this.loadOnStartup;
    this.loadOnStartup = value;
    this.support.firePropertyChange("loadOnStartup", Integer.valueOf(oldLoadOnStartup), Integer.valueOf(this.loadOnStartup));
  }
  
  public void setLoadOnStartupString(String value)
  {
    try
    {
      setLoadOnStartup(Integer.parseInt(value));
    }
    catch (NumberFormatException e)
    {
      setLoadOnStartup(0);
    }
  }
  
  public String getLoadOnStartupString()
  {
    return Integer.toString(getLoadOnStartup());
  }
  
  public int getMaxInstances()
  {
    return this.maxInstances;
  }
  
  public void setMaxInstances(int maxInstances)
  {
    int oldMaxInstances = this.maxInstances;
    this.maxInstances = maxInstances;
    this.support.firePropertyChange("maxInstances", oldMaxInstances, this.maxInstances);
  }
  
  public void setParent(Container container)
  {
    if ((container != null) && (!(container instanceof Context))) {
      throw new IllegalArgumentException(sm.getString("standardWrapper.notContext"));
    }
    if ((container instanceof StandardContext))
    {
      this.swallowOutput = ((StandardContext)container).getSwallowOutput();
      this.unloadDelay = ((StandardContext)container).getUnloadDelay();
    }
    super.setParent(container);
  }
  
  public String getRunAs()
  {
    return this.runAs;
  }
  
  public void setRunAs(String runAs)
  {
    String oldRunAs = this.runAs;
    this.runAs = runAs;
    this.support.firePropertyChange("runAs", oldRunAs, this.runAs);
  }
  
  public String getServletClass()
  {
    return this.servletClass;
  }
  
  public void setServletClass(String servletClass)
  {
    String oldServletClass = this.servletClass;
    this.servletClass = servletClass;
    this.support.firePropertyChange("servletClass", oldServletClass, this.servletClass);
    if ("org.apache.jasper.servlet.JspServlet".equals(servletClass)) {
      this.isJspServlet = true;
    }
  }
  
  public void setServletName(String name)
  {
    setName(name);
  }
  
  public boolean isSingleThreadModel()
  {
    if ((this.singleThreadModel) || (this.instance != null)) {
      return this.singleThreadModel;
    }
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    ClassLoader webappClassLoader = ((Context)getParent()).getLoader().getClassLoader();
    try
    {
      Thread.currentThread().setContextClassLoader(webappClassLoader);
      Servlet s = allocate();
      deallocate(s);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    finally
    {
      Thread.currentThread().setContextClassLoader(old);
    }
    return this.singleThreadModel;
  }
  
  public boolean isUnavailable()
  {
    if (!isEnabled()) {
      return true;
    }
    if (this.available == 0L) {
      return false;
    }
    if (this.available <= System.currentTimeMillis())
    {
      this.available = 0L;
      return false;
    }
    return true;
  }
  
  public String[] getServletMethods()
    throws ServletException
  {
    this.instance = loadServlet();
    
    Class<? extends Servlet> servletClazz = this.instance.getClass();
    if (!HttpServlet.class.isAssignableFrom(servletClazz)) {
      return DEFAULT_SERVLET_METHODS;
    }
    HashSet<String> allow = new HashSet();
    allow.add("TRACE");
    allow.add("OPTIONS");
    
    Method[] methods = getAllDeclaredMethods(servletClazz);
    for (int i = 0; (methods != null) && (i < methods.length); i++)
    {
      Method m = methods[i];
      if (m.getName().equals("doGet"))
      {
        allow.add("GET");
        allow.add("HEAD");
      }
      else if (m.getName().equals("doPost"))
      {
        allow.add("POST");
      }
      else if (m.getName().equals("doPut"))
      {
        allow.add("PUT");
      }
      else if (m.getName().equals("doDelete"))
      {
        allow.add("DELETE");
      }
    }
    String[] methodNames = new String[allow.size()];
    return (String[])allow.toArray(methodNames);
  }
  
  public Servlet getServlet()
  {
    return this.instance;
  }
  
  public void setServlet(Servlet servlet)
  {
    this.instance = servlet;
  }
  
  public void setServletSecurityAnnotationScanRequired(boolean b)
  {
    this.servletSecurityAnnotationScanRequired = b;
  }
  
  public void backgroundProcess()
  {
    super.backgroundProcess();
    if (!getState().isAvailable()) {
      return;
    }
    if ((getServlet() != null) && ((getServlet() instanceof PeriodicEventListener))) {
      ((PeriodicEventListener)getServlet()).periodicEvent();
    }
  }
  
  public static Throwable getRootCause(ServletException e)
  {
    Throwable rootCause = e;
    Throwable rootCauseCheck = null;
    
    int loops = 0;
    do
    {
      loops++;
      rootCauseCheck = rootCause.getCause();
      if (rootCauseCheck != null) {
        rootCause = rootCauseCheck;
      }
    } while ((rootCauseCheck != null) && (loops < 20));
    return rootCause;
  }
  
  public void addChild(Container child)
  {
    throw new IllegalStateException(sm.getString("standardWrapper.notChild"));
  }
  
  public void addInitParameter(String name, String value)
  {
    try
    {
      this.parametersLock.writeLock().lock();
      this.parameters.put(name, value);
    }
    finally
    {
      this.parametersLock.writeLock().unlock();
    }
    fireContainerEvent("addInitParameter", name);
  }
  
  public void addInstanceListener(InstanceListener listener)
  {
    this.instanceSupport.addInstanceListener(listener);
  }
  
  public void addMapping(String mapping)
  {
    try
    {
      this.mappingsLock.writeLock().lock();
      this.mappings.add(mapping);
    }
    finally
    {
      this.mappingsLock.writeLock().unlock();
    }
    if (this.parent.getState().equals(LifecycleState.STARTED)) {
      fireContainerEvent("addMapping", mapping);
    }
  }
  
  public void addSecurityReference(String name, String link)
  {
    try
    {
      this.referencesLock.writeLock().lock();
      this.references.put(name, link);
    }
    finally
    {
      this.referencesLock.writeLock().unlock();
    }
    fireContainerEvent("addSecurityReference", name);
  }
  
  public Servlet allocate()
    throws ServletException
  {
    if (this.unloading) {
      throw new ServletException(sm.getString("standardWrapper.unloading", new Object[] { getName() }));
    }
    boolean newInstance = false;
    if (!this.singleThreadModel)
    {
      if (this.instance == null) {
        synchronized (this)
        {
          if (this.instance == null) {
            try
            {
              if (log.isDebugEnabled()) {
                log.debug("Allocating non-STM instance");
              }
              this.instance = loadServlet();
              if (!this.singleThreadModel)
              {
                newInstance = true;
                this.countAllocated.incrementAndGet();
              }
            }
            catch (ServletException e)
            {
              throw e;
            }
            catch (Throwable e)
            {
              ExceptionUtils.handleThrowable(e);
              throw new ServletException(sm.getString("standardWrapper.allocate"), e);
            }
          }
        }
      }
      if (!this.instanceInitialized) {
        initServlet(this.instance);
      }
      if (this.singleThreadModel)
      {
        if (newInstance) {
          synchronized (this.instancePool)
          {
            this.instancePool.push(this.instance);
            this.nInstances += 1;
          }
        }
      }
      else
      {
        if (log.isTraceEnabled()) {
          log.trace("  Returning non-STM instance");
        }
        if (!newInstance) {
          this.countAllocated.incrementAndGet();
        }
        return this.instance;
      }
    }
    synchronized (this.instancePool)
    {
      while (this.countAllocated.get() >= this.nInstances) {
        if (this.nInstances < this.maxInstances) {
          try
          {
            this.instancePool.push(loadServlet());
            this.nInstances += 1;
          }
          catch (ServletException e)
          {
            throw e;
          }
          catch (Throwable e)
          {
            ExceptionUtils.handleThrowable(e);
            throw new ServletException(sm.getString("standardWrapper.allocate"), e);
          }
        } else {
          try
          {
            this.instancePool.wait();
          }
          catch (InterruptedException e) {}
        }
      }
      if (log.isTraceEnabled()) {
        log.trace("  Returning allocated STM instance");
      }
      this.countAllocated.incrementAndGet();
      return (Servlet)this.instancePool.pop();
    }
  }
  
  public void deallocate(Servlet servlet)
    throws ServletException
  {
    if (!this.singleThreadModel)
    {
      this.countAllocated.decrementAndGet();
      return;
    }
    synchronized (this.instancePool)
    {
      this.countAllocated.decrementAndGet();
      this.instancePool.push(servlet);
      this.instancePool.notify();
    }
  }
  
  public String findInitParameter(String name)
  {
    try
    {
      this.parametersLock.readLock().lock();
      return (String)this.parameters.get(name);
    }
    finally
    {
      this.parametersLock.readLock().unlock();
    }
  }
  
  public String[] findInitParameters()
  {
    try
    {
      this.parametersLock.readLock().lock();
      String[] results = new String[this.parameters.size()];
      return (String[])this.parameters.keySet().toArray(results);
    }
    finally
    {
      this.parametersLock.readLock().unlock();
    }
  }
  
  public String[] findMappings()
  {
    try
    {
      this.mappingsLock.readLock().lock();
      return (String[])this.mappings.toArray(new String[this.mappings.size()]);
    }
    finally
    {
      this.mappingsLock.readLock().unlock();
    }
  }
  
  public String findSecurityReference(String name)
  {
    try
    {
      this.referencesLock.readLock().lock();
      return (String)this.references.get(name);
    }
    finally
    {
      this.referencesLock.readLock().unlock();
    }
  }
  
  public String[] findSecurityReferences()
  {
    try
    {
      this.referencesLock.readLock().lock();
      String[] results = new String[this.references.size()];
      return (String[])this.references.keySet().toArray(results);
    }
    finally
    {
      this.referencesLock.readLock().unlock();
    }
  }
  
  @Deprecated
  public Wrapper findMappingObject()
  {
    return (Wrapper)getMappingObject();
  }
  
  public synchronized void load()
    throws ServletException
  {
    this.instance = loadServlet();
    if (!this.instanceInitialized) {
      initServlet(this.instance);
    }
    if (this.isJspServlet)
    {
      StringBuilder oname = new StringBuilder(MBeanUtils.getDomain(getParent()));
      
      oname.append(":type=JspMonitor,name=");
      oname.append(getName());
      
      oname.append(getWebModuleKeyProperties());
      try
      {
        this.jspMonitorON = new ObjectName(oname.toString());
        Registry.getRegistry(null, null).registerComponent(this.instance, this.jspMonitorON, null);
      }
      catch (Exception ex)
      {
        log.info("Error registering JSP monitoring with jmx " + this.instance);
      }
    }
  }
  
  public synchronized Servlet loadServlet()
    throws ServletException
  {
    if (this.unloading) {
      throw new ServletException(sm.getString("standardWrapper.unloading", new Object[] { getName() }));
    }
    if ((!this.singleThreadModel) && (this.instance != null)) {
      return this.instance;
    }
    PrintStream out = System.out;
    if (this.swallowOutput) {
      SystemLogHandler.startCapture();
    }
    Servlet servlet;
    try
    {
      long t1 = System.currentTimeMillis();
      if (this.servletClass == null)
      {
        unavailable(null);
        throw new ServletException(sm.getString("standardWrapper.notClass", new Object[] { getName() }));
      }
      InstanceManager instanceManager = ((StandardContext)getParent()).getInstanceManager();
      try
      {
        servlet = (Servlet)instanceManager.newInstance(this.servletClass);
      }
      catch (ClassCastException e)
      {
        unavailable(null);
        
        throw new ServletException(sm.getString("standardWrapper.notServlet", new Object[] { this.servletClass }), e);
      }
      catch (Throwable e)
      {
        e = ExceptionUtils.unwrapInvocationTargetException(e);
        ExceptionUtils.handleThrowable(e);
        unavailable(null);
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("standardWrapper.instantiate", new Object[] { this.servletClass }), e);
        }
        throw new ServletException(sm.getString("standardWrapper.instantiate", new Object[] { this.servletClass }), e);
      }
      if (this.multipartConfigElement == null)
      {
        MultipartConfig annotation = (MultipartConfig)servlet.getClass().getAnnotation(MultipartConfig.class);
        if (annotation != null) {
          this.multipartConfigElement = new MultipartConfigElement(annotation);
        }
      }
      processServletSecurityAnnotation(servlet.getClass());
      if (((servlet instanceof ContainerServlet)) && ((isContainerProvidedServlet(this.servletClass)) || (((Context)getParent()).getPrivileged()))) {
        ((ContainerServlet)servlet).setWrapper(this);
      }
      this.classLoadTime = ((int)(System.currentTimeMillis() - t1));
      if ((servlet instanceof SingleThreadModel))
      {
        if (this.instancePool == null) {
          this.instancePool = new Stack();
        }
        this.singleThreadModel = true;
      }
      initServlet(servlet);
      
      fireContainerEvent("load", this);
      
      this.loadTime = (System.currentTimeMillis() - t1);
    }
    finally
    {
      String log;
      if (this.swallowOutput)
      {
        log = SystemLogHandler.stopCapture();
        if ((log != null) && (log.length() > 0)) {
          if (getServletContext() != null) {
            getServletContext().log(log);
          } else {
            out.println(log);
          }
        }
      }
    }
    return servlet;
  }
  
  public void servletSecurityAnnotationScan()
    throws ServletException
  {
    if (getServlet() == null)
    {
      Class<?> clazz = null;
      try
      {
        clazz = getParent().getLoader().getClassLoader().loadClass(getServletClass());
        
        processServletSecurityAnnotation(clazz);
      }
      catch (ClassNotFoundException e) {}
    }
    else if (this.servletSecurityAnnotationScanRequired)
    {
      processServletSecurityAnnotation(getServlet().getClass());
    }
  }
  
  private void processServletSecurityAnnotation(Class<?> clazz)
  {
    this.servletSecurityAnnotationScanRequired = false;
    
    Context ctxt = (Context)getParent();
    if (ctxt.getIgnoreAnnotations()) {
      return;
    }
    ServletSecurity secAnnotation = (ServletSecurity)clazz.getAnnotation(ServletSecurity.class);
    if (secAnnotation != null) {
      ctxt.addServletSecurity(new ApplicationServletRegistration(this, ctxt), new ServletSecurityElement(secAnnotation));
    }
  }
  
  private synchronized void initServlet(Servlet servlet)
    throws ServletException
  {
    if ((this.instanceInitialized) && (!this.singleThreadModel)) {
      return;
    }
    try
    {
      this.instanceSupport.fireInstanceEvent("beforeInit", servlet);
      if (Globals.IS_SECURITY_ENABLED)
      {
        boolean success = false;
        try
        {
          Object[] args = { this.facade };
          SecurityUtil.doAsPrivilege("init", servlet, classType, args);
          
          success = true;
        }
        finally
        {
          if (!success) {
            SecurityUtil.remove(servlet);
          }
        }
      }
      else
      {
        servlet.init(this.facade);
      }
      this.instanceInitialized = true;
      
      this.instanceSupport.fireInstanceEvent("afterInit", servlet);
    }
    catch (UnavailableException f)
    {
      this.instanceSupport.fireInstanceEvent("afterInit", servlet, f);
      
      unavailable(f);
      throw f;
    }
    catch (ServletException f)
    {
      this.instanceSupport.fireInstanceEvent("afterInit", servlet, f);
      
      throw f;
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
      getServletContext().log("StandardWrapper.Throwable", f);
      this.instanceSupport.fireInstanceEvent("afterInit", servlet, f);
      
      throw new ServletException(sm.getString("standardWrapper.initException", new Object[] { getName() }), f);
    }
  }
  
  public void removeInitParameter(String name)
  {
    try
    {
      this.parametersLock.writeLock().lock();
      this.parameters.remove(name);
    }
    finally
    {
      this.parametersLock.writeLock().unlock();
    }
    fireContainerEvent("removeInitParameter", name);
  }
  
  public void removeInstanceListener(InstanceListener listener)
  {
    this.instanceSupport.removeInstanceListener(listener);
  }
  
  public void removeMapping(String mapping)
  {
    try
    {
      this.mappingsLock.writeLock().lock();
      this.mappings.remove(mapping);
    }
    finally
    {
      this.mappingsLock.writeLock().unlock();
    }
    if (this.parent.getState().equals(LifecycleState.STARTED)) {
      fireContainerEvent("removeMapping", mapping);
    }
  }
  
  public void removeSecurityReference(String name)
  {
    try
    {
      this.referencesLock.writeLock().lock();
      this.references.remove(name);
    }
    finally
    {
      this.referencesLock.writeLock().unlock();
    }
    fireContainerEvent("removeSecurityReference", name);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (getParent() != null)
    {
      sb.append(getParent().toString());
      sb.append(".");
    }
    sb.append("StandardWrapper[");
    sb.append(getName());
    sb.append("]");
    return sb.toString();
  }
  
  public void unavailable(UnavailableException unavailable)
  {
    getServletContext().log(sm.getString("standardWrapper.unavailable", new Object[] { getName() }));
    if (unavailable == null)
    {
      setAvailable(Long.MAX_VALUE);
    }
    else if (unavailable.isPermanent())
    {
      setAvailable(Long.MAX_VALUE);
    }
    else
    {
      int unavailableSeconds = unavailable.getUnavailableSeconds();
      if (unavailableSeconds <= 0) {
        unavailableSeconds = 60;
      }
      setAvailable(System.currentTimeMillis() + unavailableSeconds * 1000L);
    }
  }
  
  public synchronized void unload()
    throws ServletException
  {
    if ((!this.singleThreadModel) && (this.instance == null)) {
      return;
    }
    this.unloading = true;
    if (this.countAllocated.get() > 0)
    {
      int nRetries = 0;
      long delay = this.unloadDelay / 20L;
      while ((nRetries < 21) && (this.countAllocated.get() > 0))
      {
        if (nRetries % 10 == 0) {
          log.info(sm.getString("standardWrapper.waiting", new Object[] { this.countAllocated.toString(), getName() }));
        }
        try
        {
          Thread.sleep(delay);
        }
        catch (InterruptedException e) {}
        nRetries++;
      }
    }
    if (this.instanceInitialized)
    {
      PrintStream out = System.out;
      if (this.swallowOutput) {
        SystemLogHandler.startCapture();
      }
      try
      {
        this.instanceSupport.fireInstanceEvent("beforeDestroy", this.instance);
        if (Globals.IS_SECURITY_ENABLED) {
          try
          {
            SecurityUtil.doAsPrivilege("destroy", this.instance);
          }
          finally
          {
            SecurityUtil.remove(this.instance);
          }
        } else {
          this.instance.destroy();
        }
        this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance);
        if (!((Context)getParent()).getIgnoreAnnotations()) {
          ((StandardContext)getParent()).getInstanceManager().destroyInstance(this.instance);
        }
      }
      catch (Throwable t)
      {
        String log;
        t = ExceptionUtils.unwrapInvocationTargetException(t);
        ExceptionUtils.handleThrowable(t);
        this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance, t);
        
        this.instance = null;
        this.instancePool = null;
        this.nInstances = 0;
        fireContainerEvent("unload", this);
        this.unloading = false;
        throw new ServletException(sm.getString("standardWrapper.destroyException", new Object[] { getName() }), t);
      }
      finally
      {
        if (this.swallowOutput)
        {
          String log = SystemLogHandler.stopCapture();
          if ((log != null) && (log.length() > 0)) {
            if (getServletContext() != null) {
              getServletContext().log(log);
            } else {
              out.println(log);
            }
          }
        }
      }
    }
    this.instance = null;
    if ((this.isJspServlet) && (this.jspMonitorON != null)) {
      Registry.getRegistry(null, null).unregisterComponent(this.jspMonitorON);
    }
    if ((this.singleThreadModel) && (this.instancePool != null))
    {
      try
      {
        while (!this.instancePool.isEmpty())
        {
          Servlet s = (Servlet)this.instancePool.pop();
          if (Globals.IS_SECURITY_ENABLED) {
            try
            {
              SecurityUtil.doAsPrivilege("destroy", s);
            }
            finally
            {
              SecurityUtil.remove(s);
            }
          } else {
            s.destroy();
          }
          if (!((Context)getParent()).getIgnoreAnnotations()) {
            ((StandardContext)getParent()).getInstanceManager().destroyInstance(s);
          }
        }
      }
      catch (Throwable t)
      {
        t = ExceptionUtils.unwrapInvocationTargetException(t);
        ExceptionUtils.handleThrowable(t);
        this.instancePool = null;
        this.nInstances = 0;
        this.unloading = false;
        fireContainerEvent("unload", this);
        throw new ServletException(sm.getString("standardWrapper.destroyException", new Object[] { getName() }), t);
      }
      this.instancePool = null;
      this.nInstances = 0;
    }
    this.singleThreadModel = false;
    
    this.unloading = false;
    fireContainerEvent("unload", this);
  }
  
  public String getInitParameter(String name)
  {
    return findInitParameter(name);
  }
  
  public Enumeration<String> getInitParameterNames()
  {
    try
    {
      this.parametersLock.readLock().lock();
      return Collections.enumeration(this.parameters.keySet());
    }
    finally
    {
      this.parametersLock.readLock().unlock();
    }
  }
  
  public ServletContext getServletContext()
  {
    if (this.parent == null) {
      return null;
    }
    if (!(this.parent instanceof Context)) {
      return null;
    }
    return ((Context)this.parent).getServletContext();
  }
  
  public String getServletName()
  {
    return getName();
  }
  
  public long getProcessingTime()
  {
    return this.swValve.getProcessingTime();
  }
  
  @Deprecated
  public void setProcessingTime(long processingTime)
  {
    this.swValve.setProcessingTime(processingTime);
  }
  
  public long getMaxTime()
  {
    return this.swValve.getMaxTime();
  }
  
  @Deprecated
  public void setMaxTime(long maxTime)
  {
    this.swValve.setMaxTime(maxTime);
  }
  
  public long getMinTime()
  {
    return this.swValve.getMinTime();
  }
  
  @Deprecated
  public void setMinTime(long minTime)
  {
    this.swValve.setMinTime(minTime);
  }
  
  public int getRequestCount()
  {
    return this.swValve.getRequestCount();
  }
  
  @Deprecated
  public void setRequestCount(int requestCount)
  {
    this.swValve.setRequestCount(requestCount);
  }
  
  public int getErrorCount()
  {
    return this.swValve.getErrorCount();
  }
  
  @Deprecated
  public void setErrorCount(int errorCount)
  {
    this.swValve.setErrorCount(errorCount);
  }
  
  public void incrementErrorCount()
  {
    this.swValve.incrementErrorCount();
  }
  
  public long getLoadTime()
  {
    return this.loadTime;
  }
  
  @Deprecated
  public void setLoadTime(long loadTime)
  {
    this.loadTime = loadTime;
  }
  
  public int getClassLoadTime()
  {
    return this.classLoadTime;
  }
  
  public MultipartConfigElement getMultipartConfigElement()
  {
    return this.multipartConfigElement;
  }
  
  public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement)
  {
    this.multipartConfigElement = multipartConfigElement;
  }
  
  public boolean isAsyncSupported()
  {
    return this.asyncSupported;
  }
  
  public void setAsyncSupported(boolean asyncSupported)
  {
    this.asyncSupported = asyncSupported;
  }
  
  public boolean isEnabled()
  {
    return this.enabled;
  }
  
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }
  
  protected boolean isContainerProvidedServlet(String classname)
  {
    if (classname.startsWith("org.apache.catalina.")) {
      return true;
    }
    try
    {
      Class<?> clazz = getClass().getClassLoader().loadClass(classname);
      
      return ContainerServlet.class.isAssignableFrom(clazz);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    return false;
  }
  
  protected Method[] getAllDeclaredMethods(Class<?> c)
  {
    if (c.equals(HttpServlet.class)) {
      return null;
    }
    Method[] parentMethods = getAllDeclaredMethods(c.getSuperclass());
    
    Method[] thisMethods = c.getDeclaredMethods();
    if (thisMethods == null) {
      return parentMethods;
    }
    if ((parentMethods != null) && (parentMethods.length > 0))
    {
      Method[] allMethods = new Method[parentMethods.length + thisMethods.length];
      
      System.arraycopy(parentMethods, 0, allMethods, 0, parentMethods.length);
      
      System.arraycopy(thisMethods, 0, allMethods, parentMethods.length, thisMethods.length);
      
      thisMethods = allMethods;
    }
    return thisMethods;
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    if (getObjectName() != null)
    {
      Notification notification = new Notification("j2ee.state.starting", getObjectName(), this.sequenceNumber++);
      
      this.broadcaster.sendNotification(notification);
    }
    super.startInternal();
    
    setAvailable(0L);
    if (getObjectName() != null)
    {
      Notification notification = new Notification("j2ee.state.running", getObjectName(), this.sequenceNumber++);
      
      this.broadcaster.sendNotification(notification);
    }
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    setAvailable(Long.MAX_VALUE);
    if (getObjectName() != null)
    {
      Notification notification = new Notification("j2ee.state.stopping", getObjectName(), this.sequenceNumber++);
      
      this.broadcaster.sendNotification(notification);
    }
    try
    {
      unload();
    }
    catch (ServletException e)
    {
      getServletContext().log(sm.getString("standardWrapper.unloadException", new Object[] { getName() }), e);
    }
    super.stopInternal();
    if (getObjectName() != null)
    {
      Notification notification = new Notification("j2ee.state.stopped", getObjectName(), this.sequenceNumber++);
      
      this.broadcaster.sendNotification(notification);
    }
    Notification notification = new Notification("j2ee.object.deleted", getObjectName(), this.sequenceNumber++);
    
    this.broadcaster.sendNotification(notification);
  }
  
  protected String getObjectNameKeyProperties()
  {
    StringBuilder keyProperties = new StringBuilder("j2eeType=Servlet,name=");
    
    String name = getName();
    if (Util.objectNameValueNeedsQuote(name)) {
      name = ObjectName.quote(name);
    }
    keyProperties.append(name);
    
    keyProperties.append(getWebModuleKeyProperties());
    
    return keyProperties.toString();
  }
  
  private String getWebModuleKeyProperties()
  {
    StringBuilder keyProperties = new StringBuilder(",WebModule=//");
    String hostName = getParent().getParent().getName();
    if (hostName == null) {
      keyProperties.append("DEFAULT");
    } else {
      keyProperties.append(hostName);
    }
    String contextName = ((Context)getParent()).getName();
    if (!contextName.startsWith("/")) {
      keyProperties.append('/');
    }
    keyProperties.append(contextName);
    
    StandardContext ctx = null;
    if ((this.parent instanceof StandardContext)) {
      ctx = (StandardContext)getParent();
    }
    keyProperties.append(",J2EEApplication=");
    if (ctx == null) {
      keyProperties.append("none");
    } else {
      keyProperties.append(ctx.getJ2EEApplication());
    }
    keyProperties.append(",J2EEServer=");
    if (ctx == null) {
      keyProperties.append("none");
    } else {
      keyProperties.append(ctx.getJ2EEServer());
    }
    return keyProperties.toString();
  }
  
  public boolean isStateManageable()
  {
    return false;
  }
  
  public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object object)
    throws ListenerNotFoundException
  {
    this.broadcaster.removeNotificationListener(listener, filter, object);
  }
  
  public MBeanNotificationInfo[] getNotificationInfo()
  {
    if (this.notificationInfo == null) {
      this.notificationInfo = new MBeanNotificationInfo[] { new MBeanNotificationInfo(new String[] { "j2ee.object.created" }, Notification.class.getName(), "servlet is created"), new MBeanNotificationInfo(new String[] { "j2ee.state.starting" }, Notification.class.getName(), "servlet is starting"), new MBeanNotificationInfo(new String[] { "j2ee.state.running" }, Notification.class.getName(), "servlet is running"), new MBeanNotificationInfo(new String[] { "j2ee.state.stopped" }, Notification.class.getName(), "servlet start to stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.stopped" }, Notification.class.getName(), "servlet is stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.deleted" }, Notification.class.getName(), "servlet is deleted") };
    }
    return this.notificationInfo;
  }
  
  public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object object)
    throws IllegalArgumentException
  {
    this.broadcaster.addNotificationListener(listener, filter, object);
  }
  
  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    this.broadcaster.removeNotificationListener(listener);
  }
  
  @Deprecated
  public boolean isEventProvider()
  {
    return false;
  }
  
  @Deprecated
  public boolean isStatisticsProvider()
  {
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardWrapper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */