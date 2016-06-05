package org.apache.catalina.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityUtil;
import org.apache.coyote.http11.upgrade.UpgradeInbound;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.tomcat.util.res.StringManager;

public class RequestFacade
  implements HttpServletRequest
{
  private final class GetAttributePrivilegedAction
    implements PrivilegedAction<Enumeration<String>>
  {
    private GetAttributePrivilegedAction() {}
    
    public Enumeration<String> run()
    {
      return RequestFacade.this.request.getAttributeNames();
    }
  }
  
  private final class GetParameterMapPrivilegedAction
    implements PrivilegedAction<Map<String, String[]>>
  {
    private GetParameterMapPrivilegedAction() {}
    
    public Map<String, String[]> run()
    {
      return RequestFacade.this.request.getParameterMap();
    }
  }
  
  private final class GetRequestDispatcherPrivilegedAction
    implements PrivilegedAction<RequestDispatcher>
  {
    private final String path;
    
    public GetRequestDispatcherPrivilegedAction(String path)
    {
      this.path = path;
    }
    
    public RequestDispatcher run()
    {
      return RequestFacade.this.request.getRequestDispatcher(this.path);
    }
  }
  
  private final class GetParameterPrivilegedAction
    implements PrivilegedAction<String>
  {
    public String name;
    
    public GetParameterPrivilegedAction(String name)
    {
      this.name = name;
    }
    
    public String run()
    {
      return RequestFacade.this.request.getParameter(this.name);
    }
  }
  
  private final class GetParameterNamesPrivilegedAction
    implements PrivilegedAction<Enumeration<String>>
  {
    private GetParameterNamesPrivilegedAction() {}
    
    public Enumeration<String> run()
    {
      return RequestFacade.this.request.getParameterNames();
    }
  }
  
  private final class GetParameterValuePrivilegedAction
    implements PrivilegedAction<String[]>
  {
    public String name;
    
    public GetParameterValuePrivilegedAction(String name)
    {
      this.name = name;
    }
    
    public String[] run()
    {
      return RequestFacade.this.request.getParameterValues(this.name);
    }
  }
  
  private final class GetCookiesPrivilegedAction
    implements PrivilegedAction<Cookie[]>
  {
    private GetCookiesPrivilegedAction() {}
    
    public Cookie[] run()
    {
      return RequestFacade.this.request.getCookies();
    }
  }
  
  private final class GetCharacterEncodingPrivilegedAction
    implements PrivilegedAction<String>
  {
    private GetCharacterEncodingPrivilegedAction() {}
    
    public String run()
    {
      return RequestFacade.this.request.getCharacterEncoding();
    }
  }
  
  private final class GetHeadersPrivilegedAction
    implements PrivilegedAction<Enumeration<String>>
  {
    private final String name;
    
    public GetHeadersPrivilegedAction(String name)
    {
      this.name = name;
    }
    
    public Enumeration<String> run()
    {
      return RequestFacade.this.request.getHeaders(this.name);
    }
  }
  
  private final class GetHeaderNamesPrivilegedAction
    implements PrivilegedAction<Enumeration<String>>
  {
    private GetHeaderNamesPrivilegedAction() {}
    
    public Enumeration<String> run()
    {
      return RequestFacade.this.request.getHeaderNames();
    }
  }
  
  private final class GetLocalePrivilegedAction
    implements PrivilegedAction<Locale>
  {
    private GetLocalePrivilegedAction() {}
    
    public Locale run()
    {
      return RequestFacade.this.request.getLocale();
    }
  }
  
  private final class GetLocalesPrivilegedAction
    implements PrivilegedAction<Enumeration<Locale>>
  {
    private GetLocalesPrivilegedAction() {}
    
    public Enumeration<Locale> run()
    {
      return RequestFacade.this.request.getLocales();
    }
  }
  
  private final class GetSessionPrivilegedAction
    implements PrivilegedAction<HttpSession>
  {
    private final boolean create;
    
    public GetSessionPrivilegedAction(boolean create)
    {
      this.create = create;
    }
    
    public HttpSession run()
    {
      return RequestFacade.this.request.getSession(this.create);
    }
  }
  
  public RequestFacade(Request request)
  {
    this.request = request;
  }
  
  protected Request request = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  
  public void clear()
  {
    this.request = null;
  }
  
  protected Object clone()
    throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }
  
  public Object getAttribute(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getAttribute(name);
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Enumeration)AccessController.doPrivileged(new GetAttributePrivilegedAction());
    }
    return this.request.getAttributeNames();
  }
  
  public String getCharacterEncoding()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (String)AccessController.doPrivileged(new GetCharacterEncodingPrivilegedAction());
    }
    return this.request.getCharacterEncoding();
  }
  
  public void setCharacterEncoding(String env)
    throws UnsupportedEncodingException
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    this.request.setCharacterEncoding(env);
  }
  
  public int getContentLength()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getContentLength();
  }
  
  public String getContentType()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getContentType();
  }
  
  public ServletInputStream getInputStream()
    throws IOException
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getInputStream();
  }
  
  public String getParameter(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (String)AccessController.doPrivileged(new GetParameterPrivilegedAction(name));
    }
    return this.request.getParameter(name);
  }
  
  public Enumeration<String> getParameterNames()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Enumeration)AccessController.doPrivileged(new GetParameterNamesPrivilegedAction());
    }
    return this.request.getParameterNames();
  }
  
  public String[] getParameterValues(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    String[] ret = null;
    if (SecurityUtil.isPackageProtectionEnabled())
    {
      ret = (String[])AccessController.doPrivileged(new GetParameterValuePrivilegedAction(name));
      if (ret != null) {
        ret = (String[])ret.clone();
      }
    }
    else
    {
      ret = this.request.getParameterValues(name);
    }
    return ret;
  }
  
  public Map<String, String[]> getParameterMap()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Map)AccessController.doPrivileged(new GetParameterMapPrivilegedAction());
    }
    return this.request.getParameterMap();
  }
  
  public String getProtocol()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getProtocol();
  }
  
  public String getScheme()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getScheme();
  }
  
  public String getServerName()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getServerName();
  }
  
  public int getServerPort()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getServerPort();
  }
  
  public BufferedReader getReader()
    throws IOException
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getReader();
  }
  
  public String getRemoteAddr()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRemoteAddr();
  }
  
  public String getRemoteHost()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRemoteHost();
  }
  
  public void setAttribute(String name, Object o)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    this.request.setAttribute(name, o);
  }
  
  public void removeAttribute(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    this.request.removeAttribute(name);
  }
  
  public Locale getLocale()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Locale)AccessController.doPrivileged(new GetLocalePrivilegedAction());
    }
    return this.request.getLocale();
  }
  
  public Enumeration<Locale> getLocales()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Enumeration)AccessController.doPrivileged(new GetLocalesPrivilegedAction());
    }
    return this.request.getLocales();
  }
  
  public boolean isSecure()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isSecure();
  }
  
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (RequestDispatcher)AccessController.doPrivileged(new GetRequestDispatcherPrivilegedAction(path));
    }
    return this.request.getRequestDispatcher(path);
  }
  
  public String getRealPath(String path)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRealPath(path);
  }
  
  public String getAuthType()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getAuthType();
  }
  
  public Cookie[] getCookies()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    Cookie[] ret = null;
    if (SecurityUtil.isPackageProtectionEnabled())
    {
      ret = (Cookie[])AccessController.doPrivileged(new GetCookiesPrivilegedAction());
      if (ret != null) {
        ret = (Cookie[])ret.clone();
      }
    }
    else
    {
      ret = this.request.getCookies();
    }
    return ret;
  }
  
  public long getDateHeader(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getDateHeader(name);
  }
  
  public String getHeader(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getHeader(name);
  }
  
  public Enumeration<String> getHeaders(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Enumeration)AccessController.doPrivileged(new GetHeadersPrivilegedAction(name));
    }
    return this.request.getHeaders(name);
  }
  
  public Enumeration<String> getHeaderNames()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (Globals.IS_SECURITY_ENABLED) {
      return (Enumeration)AccessController.doPrivileged(new GetHeaderNamesPrivilegedAction());
    }
    return this.request.getHeaderNames();
  }
  
  public int getIntHeader(String name)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getIntHeader(name);
  }
  
  public String getMethod()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getMethod();
  }
  
  public String getPathInfo()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getPathInfo();
  }
  
  public String getPathTranslated()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getPathTranslated();
  }
  
  public String getContextPath()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getContextPath();
  }
  
  public String getQueryString()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getQueryString();
  }
  
  public String getRemoteUser()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRemoteUser();
  }
  
  public boolean isUserInRole(String role)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isUserInRole(role);
  }
  
  public Principal getUserPrincipal()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getUserPrincipal();
  }
  
  public String getRequestedSessionId()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRequestedSessionId();
  }
  
  public String getRequestURI()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRequestURI();
  }
  
  public StringBuffer getRequestURL()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRequestURL();
  }
  
  public String getServletPath()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getServletPath();
  }
  
  public HttpSession getSession(boolean create)
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    if (SecurityUtil.isPackageProtectionEnabled()) {
      return (HttpSession)AccessController.doPrivileged(new GetSessionPrivilegedAction(create));
    }
    return this.request.getSession(create);
  }
  
  public HttpSession getSession()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return getSession(true);
  }
  
  public boolean isRequestedSessionIdValid()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isRequestedSessionIdValid();
  }
  
  public boolean isRequestedSessionIdFromCookie()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isRequestedSessionIdFromCookie();
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isRequestedSessionIdFromURL();
  }
  
  public boolean isRequestedSessionIdFromUrl()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.isRequestedSessionIdFromURL();
  }
  
  public String getLocalAddr()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getLocalAddr();
  }
  
  public String getLocalName()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getLocalName();
  }
  
  public int getLocalPort()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getLocalPort();
  }
  
  public int getRemotePort()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getRemotePort();
  }
  
  public ServletContext getServletContext()
  {
    if (this.request == null) {
      throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
    }
    return this.request.getServletContext();
  }
  
  public AsyncContext startAsync()
    throws IllegalStateException
  {
    return this.request.startAsync();
  }
  
  public AsyncContext startAsync(ServletRequest request, ServletResponse response)
    throws IllegalStateException
  {
    return this.request.startAsync(request, response);
  }
  
  public boolean isAsyncStarted()
  {
    return this.request.isAsyncStarted();
  }
  
  public boolean isAsyncSupported()
  {
    return this.request.isAsyncSupported();
  }
  
  public AsyncContext getAsyncContext()
  {
    return this.request.getAsyncContext();
  }
  
  public DispatcherType getDispatcherType()
  {
    return this.request.getDispatcherType();
  }
  
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return this.request.authenticate(response);
  }
  
  public void login(String username, String password)
    throws ServletException
  {
    this.request.login(username, password);
  }
  
  public void logout()
    throws ServletException
  {
    this.request.logout();
  }
  
  public Collection<Part> getParts()
    throws IllegalStateException, IOException, ServletException
  {
    return this.request.getParts();
  }
  
  public Part getPart(String name)
    throws IllegalStateException, IOException, ServletException
  {
    return this.request.getPart(name);
  }
  
  public boolean getAllowTrace()
  {
    return this.request.getConnector().getAllowTrace();
  }
  
  public void doUpgrade(UpgradeInbound inbound)
    throws IOException
  {
    this.request.doUpgrade(inbound);
  }
  
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass)
    throws ServletException
  {
    return this.request.upgrade(httpUpgradeHandlerClass);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\RequestFacade.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */