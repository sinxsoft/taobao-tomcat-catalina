package org.apache.catalina.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.RequestUtil;

class ApplicationHttpRequest
  extends HttpServletRequestWrapper
{
  protected static final String[] specials = { "javax.servlet.include.request_uri", "javax.servlet.include.context_path", "javax.servlet.include.servlet_path", "javax.servlet.include.path_info", "javax.servlet.include.query_string", "javax.servlet.forward.request_uri", "javax.servlet.forward.context_path", "javax.servlet.forward.servlet_path", "javax.servlet.forward.path_info", "javax.servlet.forward.query_string" };
  
  public ApplicationHttpRequest(HttpServletRequest request, Context context, boolean crossContext)
  {
    super(request);
    this.context = context;
    this.crossContext = crossContext;
    setRequest(request);
  }
  
  protected Context context = null;
  protected String contextPath = null;
  protected boolean crossContext = false;
  protected DispatcherType dispatcherType = null;
  protected static final String info = "org.apache.catalina.core.ApplicationHttpRequest/1.0";
  protected Map<String, String[]> parameters = null;
  private boolean parsedParams = false;
  protected String pathInfo = null;
  private String queryParamString = null;
  protected String queryString = null;
  protected Object requestDispatcherPath = null;
  protected String requestURI = null;
  protected String servletPath = null;
  protected Session session = null;
  protected Object[] specialAttributes = new Object[specials.length];
  
  public ServletContext getServletContext()
  {
    if (this.context == null) {
      return null;
    }
    return this.context.getServletContext();
  }
  
  public Object getAttribute(String name)
  {
    if (name.equals("org.apache.catalina.core.DISPATCHER_TYPE")) {
      return this.dispatcherType;
    }
    if (name.equals("org.apache.catalina.core.DISPATCHER_REQUEST_PATH"))
    {
      if (this.requestDispatcherPath != null) {
        return this.requestDispatcherPath.toString();
      }
      return null;
    }
    int pos = getSpecial(name);
    if (pos == -1) {
      return getRequest().getAttribute(name);
    }
    if ((this.specialAttributes[pos] == null) && (this.specialAttributes[5] == null) && (pos >= 5)) {
      return getRequest().getAttribute(name);
    }
    return this.specialAttributes[pos];
  }
  
  public Enumeration<String> getAttributeNames()
  {
    return new AttributeNamesEnumerator();
  }
  
  public void removeAttribute(String name)
  {
    if (!removeSpecial(name)) {
      getRequest().removeAttribute(name);
    }
  }
  
  public void setAttribute(String name, Object value)
  {
    if (name.equals("org.apache.catalina.core.DISPATCHER_TYPE"))
    {
      this.dispatcherType = ((DispatcherType)value);
      return;
    }
    if (name.equals("org.apache.catalina.core.DISPATCHER_REQUEST_PATH"))
    {
      this.requestDispatcherPath = value;
      return;
    }
    if (!setSpecial(name, value)) {
      getRequest().setAttribute(name, value);
    }
  }
  
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (this.context == null) {
      return null;
    }
    if (path == null) {
      return null;
    }
    if (path.startsWith("/")) {
      return this.context.getServletContext().getRequestDispatcher(path);
    }
    String servletPath = (String)getAttribute("javax.servlet.include.servlet_path");
    if (servletPath == null) {
      servletPath = getServletPath();
    }
    String pathInfo = getPathInfo();
    String requestPath = null;
    if (pathInfo == null) {
      requestPath = servletPath;
    } else {
      requestPath = servletPath + pathInfo;
    }
    int pos = requestPath.lastIndexOf('/');
    String relative = null;
    if (pos >= 0) {
      relative = requestPath.substring(0, pos + 1) + path;
    } else {
      relative = requestPath + path;
    }
    return this.context.getServletContext().getRequestDispatcher(relative);
  }
  
  public DispatcherType getDispatcherType()
  {
    return this.dispatcherType;
  }
  
  public String getContextPath()
  {
    return this.contextPath;
  }
  
  public String getParameter(String name)
  {
    parseParameters();
    
    Object value = this.parameters.get(name);
    if (value == null) {
      return null;
    }
    if ((value instanceof String[])) {
      return ((String[])(String[])value)[0];
    }
    if ((value instanceof String)) {
      return (String)value;
    }
    return value.toString();
  }
  
  public Map<String, String[]> getParameterMap()
  {
    parseParameters();
    return this.parameters;
  }
  
  public Enumeration<String> getParameterNames()
  {
    parseParameters();
    return Collections.enumeration(this.parameters.keySet());
  }
  
  public String[] getParameterValues(String name)
  {
    parseParameters();
    Object value = this.parameters.get(name);
    if (value == null) {
      return null;
    }
    if ((value instanceof String[])) {
      return (String[])value;
    }
    if ((value instanceof String))
    {
      String[] values = new String[1];
      values[0] = ((String)value);
      return values;
    }
    String[] values = new String[1];
    values[0] = value.toString();
    return values;
  }
  
  public String getPathInfo()
  {
    return this.pathInfo;
  }
  
  public String getPathTranslated()
  {
    if ((getPathInfo() == null) || (getServletContext() == null)) {
      return null;
    }
    return getServletContext().getRealPath(getPathInfo());
  }
  
  public String getQueryString()
  {
    return this.queryString;
  }
  
  public String getRequestURI()
  {
    return this.requestURI;
  }
  
  public StringBuffer getRequestURL()
  {
    StringBuffer url = new StringBuffer();
    String scheme = getScheme();
    int port = getServerPort();
    if (port < 0) {
      port = 80;
    }
    url.append(scheme);
    url.append("://");
    url.append(getServerName());
    if (((scheme.equals("http")) && (port != 80)) || ((scheme.equals("https")) && (port != 443)))
    {
      url.append(':');
      url.append(port);
    }
    url.append(getRequestURI());
    
    return url;
  }
  
  public String getServletPath()
  {
    return this.servletPath;
  }
  
  public HttpSession getSession()
  {
    return getSession(true);
  }
  
  public HttpSession getSession(boolean create)
  {
    if (this.crossContext)
    {
      if (this.context == null) {
        return null;
      }
      if ((this.session != null) && (this.session.isValid())) {
        return this.session.getSession();
      }
      HttpSession other = super.getSession(false);
      if ((create) && (other == null)) {
        other = super.getSession(true);
      }
      if (other != null)
      {
        Session localSession = null;
        try
        {
          localSession = this.context.getManager().findSession(other.getId());
          if ((localSession != null) && (!localSession.isValid())) {
            localSession = null;
          }
        }
        catch (IOException e) {}
        if ((localSession == null) && (create)) {
          localSession = this.context.getManager().createSession(other.getId());
        }
        if (localSession != null)
        {
          localSession.access();
          this.session = localSession;
          return this.session.getSession();
        }
      }
      return null;
    }
    return super.getSession(create);
  }
  
  public boolean isRequestedSessionIdValid()
  {
    if (this.crossContext)
    {
      String requestedSessionId = getRequestedSessionId();
      if (requestedSessionId == null) {
        return false;
      }
      if (this.context == null) {
        return false;
      }
      Manager manager = this.context.getManager();
      if (manager == null) {
        return false;
      }
      Session session = null;
      try
      {
        session = manager.findSession(requestedSessionId);
      }
      catch (IOException e) {}
      if ((session != null) && (session.isValid())) {
        return true;
      }
      return false;
    }
    return super.isRequestedSessionIdValid();
  }
  
  public void recycle()
  {
    if (this.session != null) {
      this.session.endAccess();
    }
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.ApplicationHttpRequest/1.0";
  }
  
  Map<String, String[]> copyMap(Map<String, String[]> orig)
  {
    if (orig == null) {
      return new HashMap();
    }
    HashMap<String, String[]> dest = new HashMap();
    for (Map.Entry<String, String[]> entry : orig.entrySet()) {
      dest.put(entry.getKey(), entry.getValue());
    }
    return dest;
  }
  
  void setContextPath(String contextPath)
  {
    this.contextPath = contextPath;
  }
  
  void setPathInfo(String pathInfo)
  {
    this.pathInfo = pathInfo;
  }
  
  void setQueryString(String queryString)
  {
    this.queryString = queryString;
  }
  
  void setRequest(HttpServletRequest request)
  {
    super.setRequest(request);
    
    this.dispatcherType = ((DispatcherType)request.getAttribute("org.apache.catalina.core.DISPATCHER_TYPE"));
    this.requestDispatcherPath = request.getAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH");
    
    this.contextPath = request.getContextPath();
    this.pathInfo = request.getPathInfo();
    this.queryString = request.getQueryString();
    this.requestURI = request.getRequestURI();
    this.servletPath = request.getServletPath();
  }
  
  void setRequestURI(String requestURI)
  {
    this.requestURI = requestURI;
  }
  
  void setServletPath(String servletPath)
  {
    this.servletPath = servletPath;
  }
  
  void parseParameters()
  {
    if (this.parsedParams) {
      return;
    }
    this.parameters = new HashMap();
    this.parameters = copyMap(getRequest().getParameterMap());
    mergeParameters();
    this.parsedParams = true;
  }
  
  void setQueryParams(String queryString)
  {
    this.queryParamString = queryString;
  }
  
  protected boolean isSpecial(String name)
  {
    for (int i = 0; i < specials.length; i++) {
      if (specials[i].equals(name)) {
        return true;
      }
    }
    return false;
  }
  
  protected int getSpecial(String name)
  {
    for (int i = 0; i < specials.length; i++) {
      if (specials[i].equals(name)) {
        return i;
      }
    }
    return -1;
  }
  
  protected boolean setSpecial(String name, Object value)
  {
    for (int i = 0; i < specials.length; i++) {
      if (specials[i].equals(name))
      {
        this.specialAttributes[i] = value;
        return true;
      }
    }
    return false;
  }
  
  protected boolean removeSpecial(String name)
  {
    for (int i = 0; i < specials.length; i++) {
      if (specials[i].equals(name))
      {
        this.specialAttributes[i] = null;
        return true;
      }
    }
    return false;
  }
  
  protected String[] mergeValues(Object values1, Object values2)
  {
    ArrayList<Object> results = new ArrayList();
    if (values1 != null) {
      if ((values1 instanceof String))
      {
        results.add(values1);
      }
      else if ((values1 instanceof String[]))
      {
        String[] values = (String[])values1;
        for (int i = 0; i < values.length; i++) {
          results.add(values[i]);
        }
      }
      else
      {
        results.add(values1.toString());
      }
    }
    if (values2 != null) {
      if ((values2 instanceof String))
      {
        results.add(values2);
      }
      else if ((values2 instanceof String[]))
      {
        String[] values = (String[])values2;
        for (int i = 0; i < values.length; i++) {
          results.add(values[i]);
        }
      }
      else
      {
        results.add(values2.toString());
      }
    }
    String[] values = new String[results.size()];
    return (String[])results.toArray(values);
  }
  
  private void mergeParameters()
  {
    if ((this.queryParamString == null) || (this.queryParamString.length() < 1)) {
      return;
    }
    HashMap<String, String[]> queryParameters = new HashMap();
    String encoding = getCharacterEncoding();
    if (encoding == null) {
      encoding = "ISO-8859-1";
    }
    RequestUtil.parseParameters(queryParameters, this.queryParamString, encoding);
    
    Iterator<String> keys = this.parameters.keySet().iterator();
    while (keys.hasNext())
    {
      String key = (String)keys.next();
      Object value = queryParameters.get(key);
      if (value == null) {
        queryParameters.put(key, this.parameters.get(key));
      } else {
        queryParameters.put(key, mergeValues(value, this.parameters.get(key)));
      }
    }
    this.parameters = queryParameters;
  }
  
  protected class AttributeNamesEnumerator
    implements Enumeration<String>
  {
    protected int pos = -1;
    protected int last = -1;
    protected Enumeration<String> parentEnumeration = null;
    protected String next = null;
    
    public AttributeNamesEnumerator()
    {
      this.parentEnumeration = ApplicationHttpRequest.this.getRequest().getAttributeNames();
      for (int i = ApplicationHttpRequest.this.specialAttributes.length - 1; i >= 0; i--) {
        if (ApplicationHttpRequest.this.getAttribute(ApplicationHttpRequest.specials[i]) != null)
        {
          this.last = i;
          break;
        }
      }
    }
    
    public boolean hasMoreElements()
    {
      return (this.pos != this.last) || (this.next != null) || ((this.next = findNext()) != null);
    }
    
    public String nextElement()
    {
      if (this.pos != this.last) {
        for (int i = this.pos + 1; i <= this.last; i++) {
          if (ApplicationHttpRequest.this.getAttribute(ApplicationHttpRequest.specials[i]) != null)
          {
            this.pos = i;
            return ApplicationHttpRequest.specials[i];
          }
        }
      }
      String result = this.next;
      if (this.next != null) {
        this.next = findNext();
      } else {
        throw new NoSuchElementException();
      }
      return result;
    }
    
    protected String findNext()
    {
      String result = null;
      while ((result == null) && (this.parentEnumeration.hasMoreElements()))
      {
        String current = (String)this.parentEnumeration.nextElement();
        if (!ApplicationHttpRequest.this.isSpecial(current)) {
          result = current;
        }
      }
      return result;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationHttpRequest.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */