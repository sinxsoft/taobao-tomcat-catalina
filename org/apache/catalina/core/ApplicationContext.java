package org.apache.catalina.core;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.util.ResourceSet;
import org.apache.catalina.util.ServerInfo;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.Resource;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.RequestUtil;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.apache.tomcat.util.res.StringManager;

public class ApplicationContext
  implements ServletContext
{
  protected static final boolean STRICT_SERVLET_COMPLIANCE = Globals.STRICT_SERVLET_COMPLIANCE;
  protected static final boolean GET_RESOURCE_REQUIRE_SLASH;
  
  static
  {
    String requireSlash = System.getProperty("org.apache.catalina.core.ApplicationContext.GET_RESOURCE_REQUIRE_SLASH");
    if (requireSlash == null) {
      GET_RESOURCE_REQUIRE_SLASH = STRICT_SERVLET_COMPLIANCE;
    } else {
      GET_RESOURCE_REQUIRE_SLASH = Boolean.valueOf(requireSlash).booleanValue();
    }
  }
  
  public ApplicationContext(StandardContext context)
  {
    this.context = context;
    this.sessionCookieConfig = new ApplicationSessionCookieConfig(context);
    
    populateSessionTrackingModes();
  }
  
  protected Map<String, Object> attributes = new ConcurrentHashMap();
  private Map<String, String> readOnlyAttributes = new ConcurrentHashMap();
  private StandardContext context = null;
  private static final List<String> emptyString = Collections.emptyList();
  private static final List<Servlet> emptyServlet = Collections.emptyList();
  private ServletContext facade = new ApplicationContextFacade(this);
  private final ConcurrentHashMap<String, String> parameters = new ConcurrentHashMap();
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private ThreadLocal<DispatchData> dispatchData = new ThreadLocal();
  private SessionCookieConfig sessionCookieConfig;
  private Set<SessionTrackingMode> sessionTrackingModes = null;
  private Set<SessionTrackingMode> defaultSessionTrackingModes = null;
  private Set<SessionTrackingMode> supportedSessionTrackingModes = null;
  private boolean newServletContextListenerAllowed = true;
  
  @Deprecated
  public DirContext getResources()
  {
    return this.context.getResources();
  }
  
  public Object getAttribute(String name)
  {
    return this.attributes.get(name);
  }
  
  public Enumeration<String> getAttributeNames()
  {
    Set<String> names = new HashSet();
    names.addAll(this.attributes.keySet());
    return Collections.enumeration(names);
  }
  
  public ServletContext getContext(String uri)
  {
    if ((uri == null) || (!uri.startsWith("/"))) {
      return null;
    }
    Context child = null;
    try
    {
      Container host = this.context.getParent();
      child = (Context)host.findChild(uri);
      if ((child != null) && (!child.getState().isAvailable())) {
        child = null;
      }
      if (child == null)
      {
        int i = uri.indexOf("##");
        if (i > -1) {
          uri = uri.substring(0, i);
        }
        MessageBytes hostMB = MessageBytes.newInstance();
        hostMB.setString(host.getName());
        
        MessageBytes pathMB = MessageBytes.newInstance();
        pathMB.setString(uri);
        
        MappingData mappingData = new MappingData();
        ((Engine)host.getParent()).getService().findConnectors()[0].getMapper().map(hostMB, pathMB, null, mappingData);
        if (((Context)mappingData.context).getPath().equals(uri)) {
          child = (Context)mappingData.context;
        }
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      return null;
    }
    if (child == null) {
      return null;
    }
    if (this.context.getCrossContext()) {
      return child.getServletContext();
    }
    if (child == this.context) {
      return this.context.getServletContext();
    }
    return null;
  }
  
  public String getContextPath()
  {
    return this.context.getPath();
  }
  
  public String getInitParameter(String name)
  {
    if (("org.apache.jasper.XML_VALIDATE_TLD".equals(name)) && (this.context.getTldValidation())) {
      return "true";
    }
    if (("org.apache.jasper.XML_VALIDATE".equals(name)) && (this.context.getXmlValidation())) {
      return "true";
    }
    if (("org.apache.jasper.XML_BLOCK_EXTERNAL".equals(name)) && 
      (!this.context.getXmlBlockExternal())) {
      return "false";
    }
    return (String)this.parameters.get(name);
  }
  
  public Enumeration<String> getInitParameterNames()
  {
    Set<String> names = new HashSet();
    names.addAll(this.parameters.keySet());
    if (this.context.getTldValidation()) {
      names.add("org.apache.jasper.XML_VALIDATE_TLD");
    }
    if (this.context.getXmlValidation()) {
      names.add("org.apache.jasper.XML_VALIDATE");
    }
    if (!this.context.getXmlBlockExternal()) {
      names.add("org.apache.jasper.XML_BLOCK_EXTERNAL");
    }
    return Collections.enumeration(names);
  }
  
  public int getMajorVersion()
  {
    return 3;
  }
  
  public int getMinorVersion()
  {
    return 0;
  }
  
  public String getMimeType(String file)
  {
    if (file == null) {
      return null;
    }
    int period = file.lastIndexOf(".");
    if (period < 0) {
      return null;
    }
    String extension = file.substring(period + 1);
    if (extension.length() < 1) {
      return null;
    }
    return this.context.findMimeMapping(extension);
  }
  
  public RequestDispatcher getNamedDispatcher(String name)
  {
    if (name == null) {
      return null;
    }
    Wrapper wrapper = (Wrapper)this.context.findChild(name);
    if (wrapper == null) {
      return null;
    }
    return new ApplicationDispatcher(wrapper, null, null, null, null, name);
  }
  
  public String getRealPath(String path)
  {
    return this.context.getRealPath(path);
  }
  
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null) {
      return null;
    }
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException(sm.getString("applicationContext.requestDispatcher.iae", new Object[] { path }));
    }
    String queryString = null;
    String normalizedPath = path;
    int pos = normalizedPath.indexOf('?');
    if (pos >= 0)
    {
      queryString = normalizedPath.substring(pos + 1);
      normalizedPath = normalizedPath.substring(0, pos);
    }
    normalizedPath = RequestUtil.normalize(normalizedPath);
    if (normalizedPath == null) {
      return null;
    }
    pos = normalizedPath.length();
    
    DispatchData dd = (DispatchData)this.dispatchData.get();
    if (dd == null)
    {
      dd = new DispatchData();
      this.dispatchData.set(dd);
    }
    MessageBytes uriMB = dd.uriMB;
    uriMB.recycle();
    
    MappingData mappingData = dd.mappingData;
    
    CharChunk uriCC = uriMB.getCharChunk();
    try
    {
      uriCC.append(this.context.getPath(), 0, this.context.getPath().length());
      
      int semicolon = normalizedPath.indexOf(';');
      if ((pos >= 0) && (semicolon > pos)) {
        semicolon = -1;
      }
      uriCC.append(normalizedPath, 0, semicolon > 0 ? semicolon : pos);
      this.context.getMapper().map(uriMB, mappingData);
      if (mappingData.wrapper == null) {
        return null;
      }
      if (semicolon > 0) {
        uriCC.append(normalizedPath, semicolon, pos - semicolon);
      }
    }
    catch (Exception e)
    {
      log(sm.getString("applicationContext.mapping.error"), e);
      return null;
    }
    Wrapper wrapper = (Wrapper)mappingData.wrapper;
    String wrapperPath = mappingData.wrapperPath.toString();
    String pathInfo = mappingData.pathInfo.toString();
    
    mappingData.recycle();
    
    return new ApplicationDispatcher(wrapper, uriCC.toString(), wrapperPath, pathInfo, queryString, null);
  }
  
  public URL getResource(String path)
    throws MalformedURLException
  {
    if ((path == null) || ((!path.startsWith("/")) && (GET_RESOURCE_REQUIRE_SLASH))) {
      throw new MalformedURLException(sm.getString("applicationContext.requestDispatcher.iae", new Object[] { path }));
    }
    String normPath = RequestUtil.normalize(path);
    if (normPath == null) {
      return null;
    }
    DirContext resources = this.context.getResources();
    if (resources != null)
    {
      String fullPath = this.context.getPath() + normPath;
      String hostName = this.context.getParent().getName();
      try
      {
        resources.lookup(normPath);
        URI uri = new URI("jndi", null, "", -1, getJNDIUri(hostName, fullPath), null, null);
        
        return new URL(null, uri.toString(), new DirContextURLStreamHandler(resources));
      }
      catch (NamingException e) {}catch (Exception e)
      {
        log(sm.getString("applicationContext.lookup.error", new Object[] { path, getContextPath() }), e);
      }
    }
    return null;
  }
  
  public InputStream getResourceAsStream(String path)
  {
    if (path == null) {
      return null;
    }
    if ((!path.startsWith("/")) && (GET_RESOURCE_REQUIRE_SLASH)) {
      return null;
    }
    String normalizedPath = RequestUtil.normalize(path);
    if (normalizedPath == null) {
      return null;
    }
    DirContext resources = this.context.getResources();
    if (resources != null) {
      try
      {
        Object resource = resources.lookup(normalizedPath);
        if ((resource instanceof Resource)) {
          return ((Resource)resource).streamContent();
        }
      }
      catch (NamingException e) {}catch (Exception e)
      {
        log(sm.getString("applicationContext.lookup.error", new Object[] { path, getContextPath() }), e);
      }
    }
    return null;
  }
  
  public Set<String> getResourcePaths(String path)
  {
    if (path == null) {
      return null;
    }
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException(sm.getString("applicationContext.resourcePaths.iae", new Object[] { path }));
    }
    String normalizedPath;
    
    if (File.separatorChar == '\\') {
      normalizedPath = RequestUtil.normalize(path, true);
    } else {
      normalizedPath = RequestUtil.normalize(path, false);
    }
    if (normalizedPath == null) {
      return null;
    }
    DirContext resources = this.context.getResources();
    if (resources != null) {
      return getResourcePathsInternal(resources, normalizedPath);
    }
    return null;
  }
  
  private Set<String> getResourcePathsInternal(DirContext resources, String path)
  {
    ResourceSet<String> set = new ResourceSet();
    try
    {
      listCollectionPaths(set, resources, path);
    }
    catch (NamingException e)
    {
      return null;
    }
    set.setLocked(true);
    return set;
  }
  
  public String getServerInfo()
  {
    return ServerInfo.getServerInfo();
  }
  
  @Deprecated
  public Servlet getServlet(String name)
  {
    return null;
  }
  
  public String getServletContextName()
  {
    return this.context.getDisplayName();
  }
  
  @Deprecated
  public Enumeration<String> getServletNames()
  {
    return Collections.enumeration(emptyString);
  }
  
  @Deprecated
  public Enumeration<Servlet> getServlets()
  {
    return Collections.enumeration(emptyServlet);
  }
  
  public void log(String message)
  {
    this.context.getLogger().info(message);
  }
  
  @Deprecated
  public void log(Exception exception, String message)
  {
    this.context.getLogger().error(message, exception);
  }
  
  public void log(String message, Throwable throwable)
  {
    this.context.getLogger().error(message, throwable);
  }
  
  public void removeAttribute(String name)
  {
    Object value = null;
    if (this.readOnlyAttributes.containsKey(name)) {
      return;
    }
    value = this.attributes.remove(name);
    if (value == null) {
      return;
    }
    Object[] listeners = this.context.getApplicationEventListeners();
    if ((listeners == null) || (listeners.length == 0)) {
      return;
    }
    ServletContextAttributeEvent event = new ServletContextAttributeEvent(this.context.getServletContext(), name, value);
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof ServletContextAttributeListener))
      {
        ServletContextAttributeListener listener = (ServletContextAttributeListener)listeners[i];
        try
        {
          this.context.fireContainerEvent("beforeContextAttributeRemoved", listener);
          
          listener.attributeRemoved(event);
          this.context.fireContainerEvent("afterContextAttributeRemoved", listener);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          this.context.fireContainerEvent("afterContextAttributeRemoved", listener);
          
          log(sm.getString("applicationContext.attributeEvent"), t);
        }
      }
    }
  }
  
  public void setAttribute(String name, Object value)
  {
    if (name == null) {
      throw new IllegalArgumentException(sm.getString("applicationContext.setAttribute.namenull"));
    }
    if (value == null)
    {
      removeAttribute(name);
      return;
    }
    Object oldValue = null;
    boolean replaced = false;
    if (this.readOnlyAttributes.containsKey(name)) {
      return;
    }
    oldValue = this.attributes.get(name);
    if (oldValue != null) {
      replaced = true;
    }
    this.attributes.put(name, value);
    
    Object[] listeners = this.context.getApplicationEventListeners();
    if ((listeners == null) || (listeners.length == 0)) {
      return;
    }
    ServletContextAttributeEvent event = null;
    if (replaced) {
      event = new ServletContextAttributeEvent(this.context.getServletContext(), name, oldValue);
    } else {
      event = new ServletContextAttributeEvent(this.context.getServletContext(), name, value);
    }
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof ServletContextAttributeListener))
      {
        ServletContextAttributeListener listener = (ServletContextAttributeListener)listeners[i];
        try
        {
          if (replaced)
          {
            this.context.fireContainerEvent("beforeContextAttributeReplaced", listener);
            
            listener.attributeReplaced(event);
            this.context.fireContainerEvent("afterContextAttributeReplaced", listener);
          }
          else
          {
            this.context.fireContainerEvent("beforeContextAttributeAdded", listener);
            
            listener.attributeAdded(event);
            this.context.fireContainerEvent("afterContextAttributeAdded", listener);
          }
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          if (replaced) {
            this.context.fireContainerEvent("afterContextAttributeReplaced", listener);
          } else {
            this.context.fireContainerEvent("afterContextAttributeAdded", listener);
          }
          log(sm.getString("applicationContext.attributeEvent"), t);
        }
      }
    }
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, String filterClass)
    throws IllegalStateException
  {
    return addFilter(filterName, filterClass, null);
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
    throws IllegalStateException
  {
    return addFilter(filterName, null, filter);
  }
  
  public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass)
    throws IllegalStateException
  {
    return addFilter(filterName, filterClass.getName(), null);
  }
  
  private FilterRegistration.Dynamic addFilter(String filterName, String filterClass, Filter filter)
    throws IllegalStateException
  {
    if ((filterName == null) || (filterName.equals(""))) {
      throw new IllegalArgumentException(sm.getString("applicationContext.invalidFilterName", new Object[] { filterName }));
    }
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.addFilter.ise", new Object[] { getContextPath() }));
    }
    FilterDef filterDef = this.context.findFilterDef(filterName);
    if (filterDef == null)
    {
      filterDef = new FilterDef();
      filterDef.setFilterName(filterName);
      this.context.addFilterDef(filterDef);
    }
    else if ((filterDef.getFilterName() != null) && (filterDef.getFilterClass() != null))
    {
      return null;
    }
    if (filter == null)
    {
      filterDef.setFilterClass(filterClass);
    }
    else
    {
      filterDef.setFilterClass(filter.getClass().getName());
      filterDef.setFilter(filter);
    }
    return new ApplicationFilterRegistration(filterDef, this.context);
  }
  
  public <T extends Filter> T createFilter(Class<T> c)
    throws ServletException
  {
    try
    {
      return (T)this.context.getInstanceManager().newInstance(c.getName());
    }
    catch (IllegalAccessException e)
    {
      throw new ServletException(e);
    }
    catch (InvocationTargetException e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      throw new ServletException(e);
    }
    catch (NamingException e)
    {
      throw new ServletException(e);
    }
    catch (InstantiationException e)
    {
      throw new ServletException(e);
    }
    catch (ClassNotFoundException e)
    {
      throw new ServletException(e);
    }
  }
  
  public FilterRegistration getFilterRegistration(String filterName)
  {
    FilterDef filterDef = this.context.findFilterDef(filterName);
    if (filterDef == null) {
      return null;
    }
    return new ApplicationFilterRegistration(filterDef, this.context);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, String servletClass)
    throws IllegalStateException
  {
    return addServlet(servletName, servletClass, null);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet)
    throws IllegalStateException
  {
    return addServlet(servletName, null, servlet);
  }
  
  public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass)
    throws IllegalStateException
  {
    return addServlet(servletName, servletClass.getName(), null);
  }
  
  private ServletRegistration.Dynamic addServlet(String servletName, String servletClass, Servlet servlet)
    throws IllegalStateException
  {
    if ((servletName == null) || (servletName.equals(""))) {
      throw new IllegalArgumentException(sm.getString("applicationContext.invalidServletName", new Object[] { servletName }));
    }
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.addServlet.ise", new Object[] { getContextPath() }));
    }
    Wrapper wrapper = (Wrapper)this.context.findChild(servletName);
    if (wrapper == null)
    {
      wrapper = this.context.createWrapper();
      wrapper.setName(servletName);
      this.context.addChild(wrapper);
    }
    else if ((wrapper.getName() != null) && (wrapper.getServletClass() != null))
    {
      if (wrapper.isOverridable()) {
        wrapper.setOverridable(false);
      } else {
        return null;
      }
    }
    if (servlet == null)
    {
      wrapper.setServletClass(servletClass);
    }
    else
    {
      wrapper.setServletClass(servlet.getClass().getName());
      wrapper.setServlet(servlet);
    }
    return this.context.dynamicServletAdded(wrapper);
  }
  
  public <T extends Servlet> T createServlet(Class<T> c)
    throws ServletException
  {
    try
    {
      T servlet = (T)this.context.getInstanceManager().newInstance(c.getName());
      this.context.dynamicServletCreated(servlet);
      return servlet;
    }
    catch (IllegalAccessException e)
    {
      throw new ServletException(e);
    }
    catch (InvocationTargetException e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      throw new ServletException(e);
    }
    catch (NamingException e)
    {
      throw new ServletException(e);
    }
    catch (InstantiationException e)
    {
      throw new ServletException(e);
    }
    catch (ClassNotFoundException e)
    {
      throw new ServletException(e);
    }
  }
  
  public ServletRegistration getServletRegistration(String servletName)
  {
    Wrapper wrapper = (Wrapper)this.context.findChild(servletName);
    if (wrapper == null) {
      return null;
    }
    return (ServletRegistration) new ApplicationServletRegistration(wrapper, this.context);
  }
  
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
  {
    return this.defaultSessionTrackingModes;
  }
  
  private void populateSessionTrackingModes()
  {
    this.defaultSessionTrackingModes = EnumSet.of(SessionTrackingMode.URL);
    this.supportedSessionTrackingModes = EnumSet.of(SessionTrackingMode.URL);
    if (this.context.getCookies())
    {
      this.defaultSessionTrackingModes.add(SessionTrackingMode.COOKIE);
      this.supportedSessionTrackingModes.add(SessionTrackingMode.COOKIE);
    }
    Service s = ((Engine)this.context.getParent().getParent()).getService();
    Connector[] connectors = s.findConnectors();
    for (Connector connector : connectors) {
      if (Boolean.TRUE.equals(connector.getAttribute("SSLEnabled")))
      {
        this.supportedSessionTrackingModes.add(SessionTrackingMode.SSL);
        break;
      }
    }
  }
  
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
  {
    if (this.sessionTrackingModes != null) {
      return this.sessionTrackingModes;
    }
    return this.defaultSessionTrackingModes;
  }
  
  public SessionCookieConfig getSessionCookieConfig()
  {
    return this.sessionCookieConfig;
  }
  
  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.setSessionTracking.ise", new Object[] { getContextPath() }));
    }
    for (SessionTrackingMode sessionTrackingMode : sessionTrackingModes) {
      if (!this.supportedSessionTrackingModes.contains(sessionTrackingMode)) {
        throw new IllegalArgumentException(sm.getString("applicationContext.setSessionTracking.iae.invalid", new Object[] { sessionTrackingMode.toString(), getContextPath() }));
      }
    }
    if ((sessionTrackingModes.contains(SessionTrackingMode.SSL)) && 
      (sessionTrackingModes.size() > 1)) {
      throw new IllegalArgumentException(sm.getString("applicationContext.setSessionTracking.iae.ssl", new Object[] { getContextPath() }));
    }
    this.sessionTrackingModes = sessionTrackingModes;
  }
  
  public boolean setInitParameter(String name, String value)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.setInitParam.ise", new Object[] { getContextPath() }));
    }
    return this.parameters.putIfAbsent(name, value) == null;
  }
  
  public void addListener(Class<? extends EventListener> listenerClass)
  {
    EventListener listener;
    try
    {
      listener = createListener(listenerClass);
    }
    catch (ServletException e)
    {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.init", new Object[] { listenerClass.getName() }), e);
    }
    addListener(listener);
  }
  
  public void addListener(String className)
  {
    try
    {
      Object obj = this.context.getInstanceManager().newInstance(className);
      if (!(obj instanceof EventListener)) {
        throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.wrongType", new Object[] { className }));
      }
      EventListener listener = (EventListener)obj;
      addListener(listener);
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.cnfe", new Object[] { className }), e);
    }
    catch (InvocationTargetException e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.cnfe", new Object[] { className }), e);
    }
    catch (NamingException e)
    {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.cnfe", new Object[] { className }), e);
    }
    catch (InstantiationException e)
    {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.cnfe", new Object[] { className }), e);
    }
    catch (ClassNotFoundException e)
    {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.cnfe", new Object[] { className }), e);
    }
  }
  
  public <T extends EventListener> void addListener(T t)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.addListener.ise", new Object[] { getContextPath() }));
    }
    boolean match = false;
    if (((t instanceof ServletContextAttributeListener)) || ((t instanceof ServletRequestListener)) || ((t instanceof ServletRequestAttributeListener)) || ((t instanceof HttpSessionAttributeListener)))
    {
      this.context.addApplicationEventListener(t);
      match = true;
    }
    if (((t instanceof HttpSessionListener)) || (((t instanceof ServletContextListener)) && (this.newServletContextListenerAllowed)))
    {
      this.context.addApplicationLifecycleListener(t);
      match = true;
    }
    if (match) {
      return;
    }
    if ((t instanceof ServletContextListener)) {
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.sclNotAllowed", new Object[] { t.getClass().getName() }));
    }
    throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.wrongType", new Object[] { t.getClass().getName() }));
  }
  
  public <T extends EventListener> T createListener(Class<T> c)
    throws ServletException
  {
    try
    {
      T listener = (T)this.context.getInstanceManager().newInstance(c);
      if (((listener instanceof ServletContextListener)) || ((listener instanceof ServletContextAttributeListener)) || ((listener instanceof ServletRequestListener)) || ((listener instanceof ServletRequestAttributeListener)) || ((listener instanceof HttpSessionListener)) || ((listener instanceof HttpSessionAttributeListener))) {
        return listener;
      }
      throw new IllegalArgumentException(sm.getString("applicationContext.addListener.iae.wrongType", new Object[] { listener.getClass().getName() }));
    }
    catch (IllegalAccessException e)
    {
      throw new ServletException(e);
    }
    catch (InvocationTargetException e)
    {
      ExceptionUtils.handleThrowable(e.getCause());
      throw new ServletException(e);
    }
    catch (NamingException e)
    {
      throw new ServletException(e);
    }
    catch (InstantiationException e)
    {
      throw new ServletException(e);
    }
  }
  
  public void declareRoles(String... roleNames)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationContext.addRole.ise", new Object[] { getContextPath() }));
    }
    if (roleNames == null) {
      throw new IllegalArgumentException(sm.getString("applicationContext.roles.iae", new Object[] { getContextPath() }));
    }
    for (String role : roleNames)
    {
      if ((role == null) || ("".equals(role))) {
        throw new IllegalArgumentException(sm.getString("applicationContext.role.iae", new Object[] { getContextPath() }));
      }
      this.context.addSecurityRole(role);
    }
  }
  
  public ClassLoader getClassLoader()
  {
    ClassLoader result = this.context.getLoader().getClassLoader();
    if (Globals.IS_SECURITY_ENABLED)
    {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      ClassLoader parent = result;
      while ((parent != null) && 
        (parent != tccl)) {
        parent = parent.getParent();
      }
      if (parent == null) {
        System.getSecurityManager().checkPermission(new RuntimePermission("getClassLoader"));
      }
    }
    return result;
  }
  
  public int getEffectiveMajorVersion()
  {
    return this.context.getEffectiveMajorVersion();
  }
  
  public int getEffectiveMinorVersion()
  {
    return this.context.getEffectiveMinorVersion();
  }
  
  public Map<String, ? extends FilterRegistration> getFilterRegistrations()
  {
    Map<String, ApplicationFilterRegistration> result = new HashMap();
    
    FilterDef[] filterDefs = this.context.findFilterDefs();
    for (FilterDef filterDef : filterDefs) {
      result.put(filterDef.getFilterName(), new ApplicationFilterRegistration(filterDef, this.context));
    }
    return result;
  }
  
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    JspConfigDescriptor jspConfigDescriptor = this.context.getJspConfigDescriptor();
    if ((jspConfigDescriptor.getJspPropertyGroups().isEmpty()) && (jspConfigDescriptor.getTaglibs().isEmpty())) {
      return null;
    }
    return jspConfigDescriptor;
  }
  
  public Map<String, ? extends ServletRegistration> getServletRegistrations()
  {
    Map<String, ApplicationServletRegistration> result = new HashMap<String, ApplicationServletRegistration>();
    
    Container[] wrappers = this.context.findChildren();
    for (Container wrapper : wrappers) {
      result.put(((Wrapper)wrapper).getName(), new ApplicationServletRegistration((Wrapper)wrapper, this.context));
    }
    return (Map<String, ? extends ServletRegistration>) result;
  }
  
  protected StandardContext getContext()
  {
    return this.context;
  }
  
  @Deprecated
  protected Map<String, String> getReadonlyAttributes()
  {
    return this.readOnlyAttributes;
  }
  
  protected void clearAttributes()
  {
    ArrayList<String> list = new ArrayList();
    Iterator<String> iter = this.attributes.keySet().iterator();
    while (iter.hasNext()) {
      list.add(iter.next());
    }
    Iterator<String> keys = list.iterator();
    while (keys.hasNext())
    {
      String key = (String)keys.next();
      removeAttribute(key);
    }
  }
  
  protected ServletContext getFacade()
  {
    return this.facade;
  }
  
  void setAttributeReadOnly(String name)
  {
    if (this.attributes.containsKey(name)) {
      this.readOnlyAttributes.put(name, name);
    }
  }
  
  protected void setNewServletContextListenerAllowed(boolean allowed)
  {
    this.newServletContextListenerAllowed = allowed;
  }
  
  private static void listCollectionPaths(Set<String> set, DirContext resources, String path)
    throws NamingException
  {
    Enumeration<Binding> childPaths = resources.listBindings(path);
    while (childPaths.hasMoreElements())
    {
      Binding binding = (Binding)childPaths.nextElement();
      String name = binding.getName();
      StringBuilder childPath = new StringBuilder(path);
      if ((!"/".equals(path)) && (!path.endsWith("/"))) {
        childPath.append("/");
      }
      childPath.append(name);
      Object object = binding.getObject();
      if ((object instanceof DirContext)) {
        childPath.append("/");
      }
      set.add(childPath.toString());
    }
  }
  
  private static String getJNDIUri(String hostName, String path)
  {
    String result;
    
    if (path.startsWith("/")) {
      result = "/" + hostName + path;
    } else {
      result = "/" + hostName + "/" + path;
    }
    return result;
  }
  
  private static final class DispatchData
  {
    public MessageBytes uriMB;
    public MappingData mappingData;
    
    public DispatchData()
    {
      this.uriMB = MessageBytes.newInstance();
      CharChunk uriCC = this.uriMB.getCharChunk();
      uriCC.setLimit(-1);
      this.mappingData = new MappingData();
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */