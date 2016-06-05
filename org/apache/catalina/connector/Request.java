package org.apache.catalina.connector;

import com.taobao.coyote.RequestFailureType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletResponse;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationPart;
import org.apache.catalina.core.ApplicationSessionCookieConfig;
import org.apache.catalina.core.AsyncContextImpl;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.StringParser;
import org.apache.coyote.ActionCode;
import org.apache.coyote.http11.upgrade.UpgradeInbound;
import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.http.Cookies;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.Parameters;
import org.apache.tomcat.util.http.ServerCookie;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.apache.tomcat.util.http.fileupload.FileUploadBase.InvalidContentTypeException;
import org.apache.tomcat.util.http.fileupload.FileUploadBase.SizeException;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.apache.tomcat.util.res.StringManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

public class Request
  implements HttpServletRequest
{
  private static final Log log = LogFactory.getLog(Request.class);
  protected org.apache.coyote.Request coyoteRequest;
  
  public Request()
  {
    this.formats[0].setTimeZone(GMT_ZONE);
    this.formats[1].setTimeZone(GMT_ZONE);
    this.formats[2].setTimeZone(GMT_ZONE);
  }
  
  public void setCoyoteRequest(org.apache.coyote.Request coyoteRequest)
  {
    this.coyoteRequest = coyoteRequest;
    this.inputBuffer.setRequest(coyoteRequest);
  }
  
  public org.apache.coyote.Request getCoyoteRequest()
  {
    return this.coyoteRequest;
  }
  
  protected static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  protected Cookie[] cookies = null;
  protected SimpleDateFormat[] formats = { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US), new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US), new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };
  protected static Locale defaultLocale = Locale.getDefault();
  protected HashMap<String, Object> attributes = new HashMap();
  protected boolean sslAttributesParsed = false;
  protected ArrayList<Locale> locales = new ArrayList();
  private transient HashMap<String, Object> notes = new HashMap();
  protected String authType = null;
  protected CometEventImpl event = null;
  protected boolean comet = false;
  protected DispatcherType internalDispatcherType = null;
  protected InputBuffer inputBuffer = new InputBuffer();
  protected CoyoteInputStream inputStream = new CoyoteInputStream(this.inputBuffer);
  protected CoyoteReader reader = new CoyoteReader(this.inputBuffer);
  protected boolean usingInputStream = false;
  protected boolean usingReader = false;
  protected Principal userPrincipal = null;
  @Deprecated
  protected boolean sessionParsed = false;
  protected boolean parametersParsed = false;
  protected boolean cookiesParsed = false;
  protected boolean secure = false;
  protected transient Subject subject = null;
  protected static int CACHED_POST_LEN = 8192;
  protected byte[] postData = null;
  protected ParameterMap<String, String[]> parameterMap = new ParameterMap();
  protected Collection<Part> parts = null;
  protected Exception partsParseException = null;
  protected Session session = null;
  protected Object requestDispatcherPath = null;
  protected boolean requestedSessionCookie = false;
  protected String requestedSessionId = null;
  protected boolean requestedSessionURL = false;
  protected boolean requestedSessionSSL = false;
  protected boolean localesParsed = false;
  private final StringParser parser = new StringParser();
  protected int localPort = -1;
  protected String remoteAddr = null;
  protected String remoteHost = null;
  protected int remotePort = -1;
  protected String localAddr = null;
  protected String localName = null;
  protected volatile AsyncContextImpl asyncContext = null;
  protected Boolean asyncSupported = null;
  protected Map<String, String> pathParameters = new HashMap();
  protected Connector connector;
  
  protected void addPathParameter(String name, String value)
  {
    this.pathParameters.put(name, value);
  }
  
  protected String getPathParameter(String name)
  {
    return (String)this.pathParameters.get(name);
  }
  
  public void setAsyncSupported(boolean asyncSupported)
  {
    this.asyncSupported = Boolean.valueOf(asyncSupported);
  }
  
  public void recycle()
  {
    this.context = null;
    this.wrapper = null;
    
    this.internalDispatcherType = null;
    this.requestDispatcherPath = null;
    
    this.comet = false;
    if (this.event != null)
    {
      this.event.clear();
      this.event = null;
    }
    this.authType = null;
    this.inputBuffer.recycle();
    this.usingInputStream = false;
    this.usingReader = false;
    this.userPrincipal = null;
    this.subject = null;
    this.sessionParsed = false;
    this.parametersParsed = false;
    if (this.parts != null)
    {
      for (Part part : this.parts) {
        try
        {
          part.delete();
        }
        catch (IOException ignored) {}
      }
      this.parts = null;
    }
    this.partsParseException = null;
    this.cookiesParsed = false;
    this.locales.clear();
    this.localesParsed = false;
    this.secure = false;
    this.remoteAddr = null;
    this.remoteHost = null;
    this.remotePort = -1;
    this.localPort = -1;
    this.localAddr = null;
    this.localName = null;
    
    this.attributes.clear();
    this.sslAttributesParsed = false;
    this.notes.clear();
    this.cookies = null;
    if (this.session != null) {
      try
      {
        this.session.endAccess();
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        log.warn(sm.getString("coyoteRequest.sessionEndAccessFail"), t);
      }
    }
    this.session = null;
    this.requestedSessionCookie = false;
    this.requestedSessionId = null;
    this.requestedSessionURL = false;
    if ((Globals.IS_SECURITY_ENABLED) || (Connector.RECYCLE_FACADES))
    {
      this.parameterMap = new ParameterMap();
    }
    else
    {
      this.parameterMap.setLocked(false);
      this.parameterMap.clear();
    }
    this.mappingData.recycle();
    if ((Globals.IS_SECURITY_ENABLED) || (Connector.RECYCLE_FACADES))
    {
      if (this.facade != null)
      {
        this.facade.clear();
        this.facade = null;
      }
      if (this.inputStream != null)
      {
        this.inputStream.clear();
        this.inputStream = null;
      }
      if (this.reader != null)
      {
        this.reader.clear();
        this.reader = null;
      }
    }
    this.asyncSupported = null;
    if (this.asyncContext != null) {
      this.asyncContext.recycle();
    }
    this.asyncContext = null;
    
    this.pathParameters.clear();
  }
  
  @Deprecated
  protected boolean isProcessing()
  {
    return this.coyoteRequest.isProcessing();
  }
  
  public void clearEncoders()
  {
    this.inputBuffer.clearEncoders();
  }
  
  public boolean read()
    throws IOException
  {
    return this.inputBuffer.realReadBytes(null, 0, 0) > 0;
  }
  
  public Connector getConnector()
  {
    return this.connector;
  }
  
  public void setConnector(Connector connector)
  {
    this.connector = connector;
  }
  
  protected Context context = null;
  
  public Context getContext()
  {
    return this.context;
  }
  
  public void setContext(Context context)
  {
    this.context = context;
  }
  
  protected FilterChain filterChain = null;
  protected static final String info = "org.apache.coyote.catalina.CoyoteRequest/1.0";
  
  public FilterChain getFilterChain()
  {
    return this.filterChain;
  }
  
  public void setFilterChain(FilterChain filterChain)
  {
    this.filterChain = filterChain;
  }
  
  public Host getHost()
  {
    return (Host)this.mappingData.host;
  }
  
  @Deprecated
  public void setHost(Host host)
  {
    this.mappingData.host = host;
  }
  
  public String getInfo()
  {
    return "org.apache.coyote.catalina.CoyoteRequest/1.0";
  }
  
  protected MappingData mappingData = new MappingData();
  
  public MappingData getMappingData()
  {
    return this.mappingData;
  }
  
  protected RequestFacade facade = null;
  
  public HttpServletRequest getRequest()
  {
    if (this.facade == null) {
      this.facade = new RequestFacade(this);
    }
    return this.facade;
  }
  
  protected Response response = null;
  
  public Response getResponse()
  {
    return this.response;
  }
  
  public void setResponse(Response response)
  {
    this.response = response;
  }
  
  public InputStream getStream()
  {
    if (this.inputStream == null) {
      this.inputStream = new CoyoteInputStream(this.inputBuffer);
    }
    return this.inputStream;
  }
  
  protected B2CConverter URIConverter = null;
  
  protected B2CConverter getURIConverter()
  {
    return this.URIConverter;
  }
  
  protected void setURIConverter(B2CConverter URIConverter)
  {
    this.URIConverter = URIConverter;
  }
  
  protected Wrapper wrapper = null;
  
  public Wrapper getWrapper()
  {
    return this.wrapper;
  }
  
  public void setWrapper(Wrapper wrapper)
  {
    this.wrapper = wrapper;
  }
  
  public ServletInputStream createInputStream()
    throws IOException
  {
    if (this.inputStream == null) {
      this.inputStream = new CoyoteInputStream(this.inputBuffer);
    }
    return this.inputStream;
  }
  
  public void finishRequest()
    throws IOException
  {
    Context context = getContext();
    if ((context != null) && (this.response.getStatus() == 413) && (!context.getSwallowAbortedUploads())) {
      this.coyoteRequest.action(ActionCode.DISABLE_SWALLOW_INPUT, null);
    }
  }
  
  public Object getNote(String name)
  {
    return this.notes.get(name);
  }
  
  @Deprecated
  public Iterator<String> getNoteNames()
  {
    return this.notes.keySet().iterator();
  }
  
  public void removeNote(String name)
  {
    this.notes.remove(name);
  }
  
  public void setLocalPort(int port)
  {
    this.localPort = port;
  }
  
  public void setNote(String name, Object value)
  {
    this.notes.put(name, value);
  }
  
  public void setRemoteAddr(String remoteAddr)
  {
    this.remoteAddr = remoteAddr;
  }
  
  public void setRemoteHost(String remoteHost)
  {
    this.remoteHost = remoteHost;
  }
  
  public void setSecure(boolean secure)
  {
    this.secure = secure;
  }
  
  @Deprecated
  public void setServerName(String name)
  {
    this.coyoteRequest.serverName().setString(name);
  }
  
  public void setServerPort(int port)
  {
    this.coyoteRequest.setServerPort(port);
  }
  
  public Object getAttribute(String name)
  {
    SpecialAttributeAdapter adapter = (SpecialAttributeAdapter)specialAttributes.get(name);
    if (adapter != null) {
      return adapter.get(this, name);
    }
    Object attr = this.attributes.get(name);
    if (attr != null) {
      return attr;
    }
    attr = this.coyoteRequest.getAttribute(name);
    if (attr != null) {
      return attr;
    }
    if (isSSLAttribute(name))
    {
      this.coyoteRequest.action(ActionCode.REQ_SSL_ATTRIBUTE, this.coyoteRequest);
      
      attr = this.coyoteRequest.getAttribute("javax.servlet.request.X509Certificate");
      if (attr != null) {
        this.attributes.put("javax.servlet.request.X509Certificate", attr);
      }
      attr = this.coyoteRequest.getAttribute("javax.servlet.request.cipher_suite");
      if (attr != null) {
        this.attributes.put("javax.servlet.request.cipher_suite", attr);
      }
      attr = this.coyoteRequest.getAttribute("javax.servlet.request.key_size");
      if (attr != null) {
        this.attributes.put("javax.servlet.request.key_size", attr);
      }
      attr = this.coyoteRequest.getAttribute("javax.servlet.request.ssl_session_id");
      if (attr != null)
      {
        this.attributes.put("javax.servlet.request.ssl_session_id", attr);
        this.attributes.put("javax.servlet.request.ssl_session", attr);
      }
      attr = this.coyoteRequest.getAttribute("javax.servlet.request.ssl_session_mgr");
      if (attr != null) {
        this.attributes.put("javax.servlet.request.ssl_session_mgr", attr);
      }
      attr = this.attributes.get(name);
      this.sslAttributesParsed = true;
    }
    return attr;
  }
  
  static boolean isSSLAttribute(String name)
  {
    return ("javax.servlet.request.X509Certificate".equals(name)) || ("javax.servlet.request.cipher_suite".equals(name)) || ("javax.servlet.request.key_size".equals(name)) || ("javax.servlet.request.ssl_session_id".equals(name)) || ("javax.servlet.request.ssl_session".equals(name)) || ("javax.servlet.request.ssl_session_mgr".equals(name));
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if ((isSecure()) && (!this.sslAttributesParsed)) {
      getAttribute("javax.servlet.request.X509Certificate");
    }
    Set<String> names = new HashSet();
    names.addAll(this.attributes.keySet());
    return Collections.enumeration(names);
  }
  
  public String getCharacterEncoding()
  {
    return this.coyoteRequest.getCharacterEncoding();
  }
  
  public int getContentLength()
  {
    return this.coyoteRequest.getContentLength();
  }
  
  public String getContentType()
  {
    return this.coyoteRequest.getContentType();
  }
  
  public ServletInputStream getInputStream()
    throws IOException
  {
    if (this.usingReader) {
      throw new IllegalStateException(sm.getString("coyoteRequest.getInputStream.ise"));
    }
    this.usingInputStream = true;
    if (this.inputStream == null) {
      this.inputStream = new CoyoteInputStream(this.inputBuffer);
    }
    return this.inputStream;
  }
  
  public Locale getLocale()
  {
    if (!this.localesParsed) {
      parseLocales();
    }
    if (this.locales.size() > 0) {
      return (Locale)this.locales.get(0);
    }
    return defaultLocale;
  }
  
  public Enumeration<Locale> getLocales()
  {
    if (!this.localesParsed) {
      parseLocales();
    }
    if (this.locales.size() > 0) {
      return Collections.enumeration(this.locales);
    }
    ArrayList<Locale> results = new ArrayList();
    results.add(defaultLocale);
    return Collections.enumeration(results);
  }
  
  public String getParameter(String name)
  {
    if (!this.parametersParsed) {
      parseParameters();
    }
    return this.coyoteRequest.getParameters().getParameter(name);
  }
  
  public Map<String, String[]> getParameterMap()
  {
    if (this.parameterMap.isLocked()) {
      return this.parameterMap;
    }
    Enumeration<String> enumeration = getParameterNames();
    while (enumeration.hasMoreElements())
    {
      String name = (String)enumeration.nextElement();
      String[] values = getParameterValues(name);
      this.parameterMap.put(name, values);
    }
    this.parameterMap.setLocked(true);
    
    return this.parameterMap;
  }
  
  public Enumeration<String> getParameterNames()
  {
    if (!this.parametersParsed) {
      parseParameters();
    }
    return this.coyoteRequest.getParameters().getParameterNames();
  }
  
  public String[] getParameterValues(String name)
  {
    if (!this.parametersParsed) {
      parseParameters();
    }
    return this.coyoteRequest.getParameters().getParameterValues(name);
  }
  
  public String getProtocol()
  {
    return this.coyoteRequest.protocol().toString();
  }
  
  public BufferedReader getReader()
    throws IOException
  {
    if (this.usingInputStream) {
      throw new IllegalStateException(sm.getString("coyoteRequest.getReader.ise"));
    }
    this.usingReader = true;
    this.inputBuffer.checkConverter();
    if (this.reader == null) {
      this.reader = new CoyoteReader(this.inputBuffer);
    }
    return this.reader;
  }
  
  @Deprecated
  public String getRealPath(String path)
  {
    if (this.context == null) {
      return null;
    }
    ServletContext servletContext = this.context.getServletContext();
    if (servletContext == null) {
      return null;
    }
    try
    {
      return servletContext.getRealPath(path);
    }
    catch (IllegalArgumentException e) {}
    return null;
  }
  
  public String getRemoteAddr()
  {
    if (this.remoteAddr == null)
    {
      this.coyoteRequest.action(ActionCode.REQ_HOST_ADDR_ATTRIBUTE, this.coyoteRequest);
      
      this.remoteAddr = this.coyoteRequest.remoteAddr().toString();
    }
    return this.remoteAddr;
  }
  
  public String getRemoteHost()
  {
    if (this.remoteHost == null) {
      if (!this.connector.getEnableLookups())
      {
        this.remoteHost = getRemoteAddr();
      }
      else
      {
        this.coyoteRequest.action(ActionCode.REQ_HOST_ATTRIBUTE, this.coyoteRequest);
        
        this.remoteHost = this.coyoteRequest.remoteHost().toString();
      }
    }
    return this.remoteHost;
  }
  
  public int getRemotePort()
  {
    if (this.remotePort == -1)
    {
      this.coyoteRequest.action(ActionCode.REQ_REMOTEPORT_ATTRIBUTE, this.coyoteRequest);
      
      this.remotePort = this.coyoteRequest.getRemotePort();
    }
    return this.remotePort;
  }
  
  public String getLocalName()
  {
    if (this.localName == null)
    {
      this.coyoteRequest.action(ActionCode.REQ_LOCAL_NAME_ATTRIBUTE, this.coyoteRequest);
      
      this.localName = this.coyoteRequest.localName().toString();
    }
    return this.localName;
  }
  
  public String getLocalAddr()
  {
    if (this.localAddr == null)
    {
      this.coyoteRequest.action(ActionCode.REQ_LOCAL_ADDR_ATTRIBUTE, this.coyoteRequest);
      
      this.localAddr = this.coyoteRequest.localAddr().toString();
    }
    return this.localAddr;
  }
  
  public int getLocalPort()
  {
    if (this.localPort == -1)
    {
      this.coyoteRequest.action(ActionCode.REQ_LOCALPORT_ATTRIBUTE, this.coyoteRequest);
      
      this.localPort = this.coyoteRequest.getLocalPort();
    }
    return this.localPort;
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
  
  public String getScheme()
  {
    return this.coyoteRequest.scheme().toString();
  }
  
  public String getServerName()
  {
    return this.coyoteRequest.serverName().toString();
  }
  
  public int getServerPort()
  {
    return this.coyoteRequest.getServerPort();
  }
  
  public boolean isSecure()
  {
    return this.secure;
  }
  
  public void removeAttribute(String name)
  {
    if (name.startsWith("org.apache.tomcat.")) {
      this.coyoteRequest.getAttributes().remove(name);
    }
    boolean found = this.attributes.containsKey(name);
    if (found)
    {
      Object value = this.attributes.get(name);
      this.attributes.remove(name);
      
      notifyAttributeRemoved(name, value);
    }
    else {}
  }
  
  public void setAttribute(String name, Object value)
  {
    if (name == null) {
      throw new IllegalArgumentException(sm.getString("coyoteRequest.setAttribute.namenull"));
    }
    if (value == null)
    {
      removeAttribute(name);
      return;
    }
    SpecialAttributeAdapter adapter = (SpecialAttributeAdapter)specialAttributes.get(name);
    if (adapter != null)
    {
      adapter.set(this, name, value);
      return;
    }
    if ((Globals.IS_SECURITY_ENABLED) && (name.equals("org.apache.tomcat.sendfile.filename")))
    {
      String canonicalPath;
      try
      {
        canonicalPath = new File(value.toString()).getCanonicalPath();
      }
      catch (IOException e)
      {
        throw new SecurityException(sm.getString("coyoteRequest.sendfileNotCanonical", new Object[] { value }), e);
      }
      System.getSecurityManager().checkRead(canonicalPath);
      
      value = canonicalPath;
    }
    Object oldValue = this.attributes.put(name, value);
    if (name.startsWith("org.apache.tomcat.")) {
      this.coyoteRequest.setAttribute(name, value);
    }
    notifyAttributeAssigned(name, value, oldValue);
  }
  
  private void notifyAttributeAssigned(String name, Object value, Object oldValue)
  {
    Object[] listeners = this.context.getApplicationEventListeners();
    if ((listeners == null) || (listeners.length == 0)) {
      return;
    }
    boolean replaced = oldValue != null;
    ServletRequestAttributeEvent event = null;
    if (replaced) {
      event = new ServletRequestAttributeEvent(this.context.getServletContext(), getRequest(), name, oldValue);
    } else {
      event = new ServletRequestAttributeEvent(this.context.getServletContext(), getRequest(), name, value);
    }
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof ServletRequestAttributeListener))
      {
        ServletRequestAttributeListener listener = (ServletRequestAttributeListener)listeners[i];
        try
        {
          if (replaced) {
            listener.attributeReplaced(event);
          } else {
            listener.attributeAdded(event);
          }
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          this.context.getLogger().error(sm.getString("coyoteRequest.attributeEvent"), t);
          
          this.attributes.put("javax.servlet.error.exception", t);
        }
      }
    }
  }
  
  private void notifyAttributeRemoved(String name, Object value)
  {
    Object[] listeners = this.context.getApplicationEventListeners();
    if ((listeners == null) || (listeners.length == 0)) {
      return;
    }
    ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(this.context.getServletContext(), getRequest(), name, value);
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof ServletRequestAttributeListener))
      {
        ServletRequestAttributeListener listener = (ServletRequestAttributeListener)listeners[i];
        try
        {
          listener.attributeRemoved(event);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          this.context.getLogger().error(sm.getString("coyoteRequest.attributeEvent"), t);
          
          this.attributes.put("javax.servlet.error.exception", t);
        }
      }
    }
  }
  
  public void setCharacterEncoding(String enc)
    throws UnsupportedEncodingException
  {
    if (this.usingReader) {
      return;
    }
    byte[] buffer = new byte[1];
    buffer[0] = 97;
    
    B2CConverter.getCharset(enc);
    
    this.coyoteRequest.setCharacterEncoding(enc);
  }
  
  public ServletContext getServletContext()
  {
    return this.context.getServletContext();
  }
  
  public AsyncContext startAsync()
  {
    return startAsync(getRequest(), this.response.getResponse());
  }
  
  public AsyncContext startAsync(ServletRequest request, ServletResponse response)
  {
    if (!isAsyncSupported()) {
      throw new IllegalStateException(sm.getString("request.asyncNotSupported"));
    }
    if (this.asyncContext == null) {
      this.asyncContext = new AsyncContextImpl(this);
    }
    this.asyncContext.setStarted(getContext(), request, response, (request == getRequest()) && (response == getResponse().getResponse()));
    
    this.asyncContext.setTimeout(getConnector().getAsyncTimeout());
    
    return this.asyncContext;
  }
  
  public boolean isAsyncStarted()
  {
    if (this.asyncContext == null) {
      return false;
    }
    return this.asyncContext.isStarted();
  }
  
  public boolean isAsyncDispatching()
  {
    if (this.asyncContext == null) {
      return false;
    }
    AtomicBoolean result = new AtomicBoolean(false);
    this.coyoteRequest.action(ActionCode.ASYNC_IS_DISPATCHING, result);
    return result.get();
  }
  
  public boolean isAsyncCompleting()
  {
    if (this.asyncContext == null) {
      return false;
    }
    AtomicBoolean result = new AtomicBoolean(false);
    this.coyoteRequest.action(ActionCode.ASYNC_IS_COMPLETING, result);
    return result.get();
  }
  
  public boolean isAsync()
  {
    if (this.asyncContext == null) {
      return false;
    }
    AtomicBoolean result = new AtomicBoolean(false);
    this.coyoteRequest.action(ActionCode.ASYNC_IS_ASYNC, result);
    return result.get();
  }
  
  public boolean isAsyncSupported()
  {
    if (this.asyncSupported == null) {
      return true;
    }
    return this.asyncSupported.booleanValue();
  }
  
  public AsyncContext getAsyncContext()
  {
    return this.asyncContext;
  }
  
  public DispatcherType getDispatcherType()
  {
    if (this.internalDispatcherType == null) {
      return DispatcherType.REQUEST;
    }
    return this.internalDispatcherType;
  }
  
  public void addCookie(Cookie cookie)
  {
    if (!this.cookiesParsed) {
      parseCookies();
    }
    int size = 0;
    if (this.cookies != null) {
      size = this.cookies.length;
    }
    Cookie[] newCookies = new Cookie[size + 1];
    for (int i = 0; i < size; i++) {
      newCookies[i] = this.cookies[i];
    }
    newCookies[size] = cookie;
    
    this.cookies = newCookies;
  }
  
  public void addLocale(Locale locale)
  {
    this.locales.add(locale);
  }
  
  @Deprecated
  public void addParameter(String name, String[] values)
  {
    this.coyoteRequest.getParameters().addParameterValues(name, values);
  }
  
  public void clearCookies()
  {
    this.cookiesParsed = true;
    this.cookies = null;
  }
  
  @Deprecated
  public void clearHeaders() {}
  
  public void clearLocales()
  {
    this.locales.clear();
  }
  
  @Deprecated
  public void clearParameters() {}
  
  public void setAuthType(String type)
  {
    this.authType = type;
  }
  
  @Deprecated
  public void setContextPath(String path)
  {
    if (path == null) {
      this.mappingData.contextPath.setString("");
    } else {
      this.mappingData.contextPath.setString(path);
    }
  }
  
  public void setPathInfo(String path)
  {
    this.mappingData.pathInfo.setString(path);
  }
  
  public void setRequestedSessionCookie(boolean flag)
  {
    this.requestedSessionCookie = flag;
  }
  
  public void setRequestedSessionId(String id)
  {
    this.requestedSessionId = id;
  }
  
  public void setRequestedSessionURL(boolean flag)
  {
    this.requestedSessionURL = flag;
  }
  
  public void setRequestedSessionSSL(boolean flag)
  {
    this.requestedSessionSSL = flag;
  }
  
  public String getDecodedRequestURI()
  {
    return this.coyoteRequest.decodedURI().toString();
  }
  
  public MessageBytes getDecodedRequestURIMB()
  {
    return this.coyoteRequest.decodedURI();
  }
  
  @Deprecated
  public void setServletPath(String path)
  {
    if (path != null) {
      this.mappingData.wrapperPath.setString(path);
    }
  }
  
  public void setUserPrincipal(Principal principal)
  {
    if (Globals.IS_SECURITY_ENABLED)
    {
      HttpSession session = getSession(false);
      if ((this.subject != null) && (!this.subject.getPrincipals().contains(principal)))
      {
        this.subject.getPrincipals().add(principal);
      }
      else if ((session != null) && (session.getAttribute("javax.security.auth.subject") == null))
      {
        this.subject = new Subject();
        this.subject.getPrincipals().add(principal);
      }
      if (session != null) {
        session.setAttribute("javax.security.auth.subject", this.subject);
      }
    }
    this.userPrincipal = principal;
  }
  
  public String getAuthType()
  {
    return this.authType;
  }
  
  public String getContextPath()
  {
    String canonicalContextPath = getServletContext().getContextPath();
    String uri = getRequestURI();
    char[] uriChars = uri.toCharArray();
    int lastSlash = this.mappingData.contextSlashCount;
    if (lastSlash == 0) {
      return "";
    }
    int pos = 0;
    while (lastSlash > 0)
    {
      pos = nextSlash(uriChars, pos + 1);
      if (pos == -1) {
        break;
      }
      lastSlash--;
    }
    String candidate;
    if (pos == -1) {
      candidate = uri;
    } else {
      candidate = uri.substring(0, pos);
    }
    candidate = org.apache.catalina.util.RequestUtil.URLDecode(candidate, this.connector.getURIEncoding());
    candidate = org.apache.tomcat.util.http.RequestUtil.normalize(candidate);
    boolean match = canonicalContextPath.equals(candidate);
    while ((!match) && (pos != -1))
    {
      pos = nextSlash(uriChars, pos + 1);
      if (pos == -1) {
        candidate = uri;
      } else {
        candidate = uri.substring(0, pos);
      }
      candidate = org.apache.catalina.util.RequestUtil.URLDecode(candidate, this.connector.getURIEncoding());
      candidate = org.apache.tomcat.util.http.RequestUtil.normalize(candidate);
      match = canonicalContextPath.equals(candidate);
    }
    if (match)
    {
      if (pos == -1) {
        return uri;
      }
      return uri.substring(0, pos);
    }
    throw new IllegalStateException(sm.getString("coyoteRequest.getContextPath.ise", new Object[] { canonicalContextPath, uri }));
  }
  
  private int nextSlash(char[] uri, int startPos)
  {
    int len = uri.length;
    int pos = startPos;
    while (pos < len)
    {
      if (uri[pos] == '/') {
        return pos;
      }
      if ((UDecoder.ALLOW_ENCODED_SLASH) && (uri[pos] == '%') && (pos + 2 < len) && (uri[(pos + 1)] == '2') && ((uri[(pos + 2)] == 'f') || (uri[(pos + 2)] == 'F'))) {
        return pos;
      }
      pos++;
    }
    return -1;
  }
  
  @Deprecated
  public MessageBytes getContextPathMB()
  {
    return this.mappingData.contextPath;
  }
  
  public Cookie[] getCookies()
  {
    if (!this.cookiesParsed) {
      parseCookies();
    }
    return this.cookies;
  }
  
  @Deprecated
  public void setCookies(Cookie[] cookies)
  {
    this.cookies = cookies;
  }
  
  public long getDateHeader(String name)
  {
    String value = getHeader(name);
    if (value == null) {
      return -1L;
    }
    long result = FastHttpDateFormat.parseDate(value, this.formats);
    if (result != -1L) {
      return result;
    }
    throw new IllegalArgumentException(value);
  }
  
  public String getHeader(String name)
  {
    return this.coyoteRequest.getHeader(name);
  }
  
  public Enumeration<String> getHeaders(String name)
  {
    return this.coyoteRequest.getMimeHeaders().values(name);
  }
  
  public Enumeration<String> getHeaderNames()
  {
    return this.coyoteRequest.getMimeHeaders().names();
  }
  
  public int getIntHeader(String name)
  {
    String value = getHeader(name);
    if (value == null) {
      return -1;
    }
    return Integer.parseInt(value);
  }
  
  public String getMethod()
  {
    return this.coyoteRequest.method().toString();
  }
  
  public String getPathInfo()
  {
    return this.mappingData.pathInfo.toString();
  }
  
  @Deprecated
  public MessageBytes getPathInfoMB()
  {
    return this.mappingData.pathInfo;
  }
  
  public String getPathTranslated()
  {
    if (this.context == null) {
      return null;
    }
    if (getPathInfo() == null) {
      return null;
    }
    return this.context.getServletContext().getRealPath(getPathInfo());
  }
  
  public String getQueryString()
  {
    return this.coyoteRequest.queryString().toString();
  }
  
  public String getRemoteUser()
  {
    if (this.userPrincipal == null) {
      return null;
    }
    return this.userPrincipal.getName();
  }
  
  public MessageBytes getRequestPathMB()
  {
    return this.mappingData.requestPath;
  }
  
  public String getRequestedSessionId()
  {
    return this.requestedSessionId;
  }
  
  public String getRequestURI()
  {
    return this.coyoteRequest.requestURI().toString();
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
    return this.mappingData.wrapperPath.toString();
  }
  
  @Deprecated
  public MessageBytes getServletPathMB()
  {
    return this.mappingData.wrapperPath;
  }
  
  public HttpSession getSession()
  {
    Session session = doGetSession(true);
    if (session == null) {
      return null;
    }
    return session.getSession();
  }
  
  public HttpSession getSession(boolean create)
  {
    Session session = doGetSession(create);
    if (session == null) {
      return null;
    }
    return session.getSession();
  }
  
  public boolean isRequestedSessionIdFromCookie()
  {
    if (this.requestedSessionId == null) {
      return false;
    }
    return this.requestedSessionCookie;
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    if (this.requestedSessionId == null) {
      return false;
    }
    return this.requestedSessionURL;
  }
  
  @Deprecated
  public boolean isRequestedSessionIdFromUrl()
  {
    return isRequestedSessionIdFromURL();
  }
  
  public boolean isRequestedSessionIdValid()
  {
    if (this.requestedSessionId == null) {
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
      session = manager.findSession(this.requestedSessionId);
    }
    catch (IOException e) {}
    if ((session == null) || (!session.isValid()))
    {
      if (getMappingData().contexts == null) {
        return false;
      }
      for (int i = getMappingData().contexts.length; i > 0; i--)
      {
        Context ctxt = (Context)getMappingData().contexts[(i - 1)];
        try
        {
          if (ctxt.getManager().findSession(this.requestedSessionId) != null) {
            return true;
          }
        }
        catch (IOException e) {}
      }
      return false;
    }
    return true;
  }
  
  public boolean isUserInRole(String role)
  {
    if (this.userPrincipal == null) {
      return false;
    }
    if (this.context == null) {
      return false;
    }
    Realm realm = this.context.getRealm();
    if (realm == null) {
      return false;
    }
    return realm.hasRole(this.wrapper, this.userPrincipal, role);
  }
  
  public Principal getPrincipal()
  {
    return this.userPrincipal;
  }
  
  public Principal getUserPrincipal()
  {
    if ((this.userPrincipal instanceof GenericPrincipal))
    {
      GSSCredential gssCredential = ((GenericPrincipal)this.userPrincipal).getGssCredential();
      if (gssCredential != null)
      {
        int left = -1;
        try
        {
          left = gssCredential.getRemainingLifetime();
        }
        catch (GSSException e)
        {
          log.warn(sm.getString("coyoteRequest.gssLifetimeFail", new Object[] { this.userPrincipal.getName() }), e);
        }
        if (left == 0)
        {
          try
          {
            logout();
          }
          catch (ServletException e) {}
          return null;
        }
      }
      return ((GenericPrincipal)this.userPrincipal).getUserPrincipal();
    }
    return this.userPrincipal;
  }
  
  public Session getSessionInternal()
  {
    return doGetSession(true);
  }
  
  public void changeSessionId(String newSessionId)
  {
    if ((this.requestedSessionId != null) && (this.requestedSessionId.length() > 0)) {
      this.requestedSessionId = newSessionId;
    }
    if ((this.context != null) && (!this.context.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE))) {
      return;
    }
    if (this.response != null)
    {
      Cookie newCookie = ApplicationSessionCookieConfig.createSessionCookie(this.context, newSessionId, this.secure);
      
      this.response.addSessionCookieInternal(newCookie);
    }
  }
  
  public Session getSessionInternal(boolean create)
  {
    return doGetSession(create);
  }
  
  public CometEventImpl getEvent()
  {
    if (this.event == null) {
      this.event = new CometEventImpl(this, this.response);
    }
    return this.event;
  }
  
  public boolean isComet()
  {
    return this.comet;
  }
  
  public void setComet(boolean comet)
  {
    this.comet = comet;
  }
  
  public boolean isParametersParsed()
  {
    return this.parametersParsed;
  }
  
  public boolean getAvailable()
  {
    return this.inputBuffer.available() > 0;
  }
  
  protected void checkSwallowInput()
  {
    Context context = getContext();
    if ((context != null) && (!context.getSwallowAbortedUploads())) {
      this.coyoteRequest.action(ActionCode.DISABLE_SWALLOW_INPUT, null);
    }
  }
  
  public void cometClose()
  {
    this.coyoteRequest.action(ActionCode.COMET_CLOSE, getEvent());
    setComet(false);
  }
  
  public void setCometTimeout(long timeout)
  {
    this.coyoteRequest.action(ActionCode.COMET_SETTIMEOUT, Long.valueOf(timeout));
  }
  
  @Deprecated
  public boolean isRequestedSessionIdFromSSL()
  {
    return this.requestedSessionSSL;
  }
  
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    if (response.isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteRequest.authenticate.ise"));
    }
    return this.context.getAuthenticator().authenticate(this, response);
  }
  
  public void login(String username, String password)
    throws ServletException
  {
    if ((getAuthType() != null) || (getRemoteUser() != null) || (getUserPrincipal() != null)) {
      throw new ServletException(sm.getString("coyoteRequest.alreadyAuthenticated"));
    }
    if (this.context.getAuthenticator() == null) {
      throw new ServletException("no authenticator");
    }
    this.context.getAuthenticator().login(username, password, this);
  }
  
  public void logout()
    throws ServletException
  {
    this.context.getAuthenticator().logout(this);
  }
  
  public Collection<Part> getParts()
    throws IOException, IllegalStateException, ServletException
  {
    parseParts();
    if (this.partsParseException != null)
    {
      if ((this.partsParseException instanceof IOException)) {
        throw ((IOException)this.partsParseException);
      }
      if ((this.partsParseException instanceof IllegalStateException)) {
        throw ((IllegalStateException)this.partsParseException);
      }
      if ((this.partsParseException instanceof ServletException)) {
        throw ((ServletException)this.partsParseException);
      }
    }
    return this.parts;
  }
  
  private void parseParts()
  {
    if ((this.parts != null) || (this.partsParseException != null)) {
      return;
    }
    MultipartConfigElement mce = getWrapper().getMultipartConfigElement();
    if (mce == null) {
      if (getContext().getAllowCasualMultipartParsing())
      {
        mce = new MultipartConfigElement(null, this.connector.getMaxPostSize(), this.connector.getMaxPostSize(), this.connector.getMaxPostSize());
      }
      else
      {
        this.parts = Collections.emptyList();
        return;
      }
    }
    Parameters parameters = this.coyoteRequest.getParameters();
    parameters.setLimit(getConnector().getMaxParameterCount());
    
    boolean success = false;
    try
    {
      String locationStr = mce.getLocation();

      File location;
      if ((locationStr == null) || (locationStr.length() == 0))
      {
        location = (File)this.context.getServletContext().getAttribute("javax.servlet.context.tempdir");
      }
      else
      {
        location = new File(locationStr);
        if (!location.isAbsolute()) {
          location = new File((File)this.context.getServletContext().getAttribute("javax.servlet.context.tempdir"), locationStr).getAbsoluteFile();
        }
      }
      Throwable cause;
      if (!location.isDirectory())
      {
        this.partsParseException = new IOException(sm.getString("coyoteRequest.uploadLocationInvalid", new Object[] { location }));
      }
      else
      {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        try
        {
          factory.setRepository(location.getCanonicalFile());
        }
        catch (IOException ioe)
        {
          this.partsParseException = ioe;
          if ((this.partsParseException != null) || (!success))
          {
            parameters.setParseFailed(true);
            
            cause = this.partsParseException.getCause();
            if ((cause instanceof FileUploadBase.SizeException)) {
              getCoyoteRequest().recordRequestFailure(RequestFailureType.FILE_UPLOAD_MAX_SIZE_EXCEEDED_FAILURE);
            } else if ((cause instanceof FileUploadBase.InvalidContentTypeException)) {
              getCoyoteRequest().recordRequestFailure(RequestFailureType.INVALID_CONTENT_TYPE_FAILURE);
            } else if ((cause instanceof FileUploadException)) {
              getCoyoteRequest().recordRequestFailure(RequestFailureType.FILE_UPLOAD_EXCEPTION);
            } else if ((this.partsParseException instanceof IllegalStateException)) {
              getCoyoteRequest().recordRequestFailure(RequestFailureType.MAX_POST_SIZE_EXCEEDED_FAILURE);
            }
          }
          return;
        }
        factory.setSizeThreshold(mce.getFileSizeThreshold());
        
        ServletFileUpload upload = new ServletFileUpload();
        upload.setFileItemFactory(factory);
        upload.setFileSizeMax(mce.getMaxFileSize());
        upload.setSizeMax(mce.getMaxRequestSize());
        
        this.parts = new ArrayList();
        try
        {
          List<FileItem> items = upload.parseRequest(new ServletRequestContext(this));
          
          int maxPostSize = getConnector().getMaxPostSize();
          int postSize = 0;
          String enc = getCharacterEncoding();
          Charset charset = null;
          if (enc != null) {
            try
            {
              charset = B2CConverter.getCharset(enc);
            }
            catch (UnsupportedEncodingException e) {}
          }
          for (FileItem item : items)
          {
            ApplicationPart part = new ApplicationPart(item, location);
            this.parts.add(part);
            if (part.getSubmittedFileName() == null)
            {
              String name = part.getName();
              String value = null;
              try
              {
                String encoding = parameters.getEncoding();
                if (encoding == null) {
                  if (enc == null) {
                    encoding = "ISO-8859-1";
                  } else {
                    encoding = enc;
                  }
                }
                value = part.getString(encoding);
              }
              catch (UnsupportedEncodingException uee)
              {
                try
                {
                  value = part.getString("ISO-8859-1");
                }
                catch (UnsupportedEncodingException e) {}
              }
              if (maxPostSize > 0)
              {
                if (charset == null) {
                  postSize += name.getBytes().length;
                } else {
                  postSize += name.getBytes(charset).length;
                }
                if (value != null)
                {
                  postSize++;
                  
                  postSize = (int)(postSize + part.getSize());
                }
                postSize++;
                if (postSize > maxPostSize) {
                  throw new IllegalStateException(sm.getString("coyoteRequest.maxPostSizeExceeded"));
                }
              }
              parameters.addParameter(name, value);
            }
          }
          success = true;
        }
        catch (FileUploadBase.InvalidContentTypeException e)
        {
          this.partsParseException = new ServletException(e);
        }
        catch (FileUploadBase.SizeException e)
        {
          checkSwallowInput();
          this.partsParseException = new IllegalStateException(e);
        }
        catch (FileUploadException e)
        {
          this.partsParseException = new IOException(e);
        }
        catch (IllegalStateException e)
        {
          checkSwallowInput();
          this.partsParseException = e;
        }
      }
    }
    finally
    {
      Throwable cause;
      if ((this.partsParseException != null) || (!success))
      {
        parameters.setParseFailed(true);
        
        cause = this.partsParseException.getCause();
        if ((cause instanceof FileUploadBase.SizeException)) {
          getCoyoteRequest().recordRequestFailure(RequestFailureType.FILE_UPLOAD_MAX_SIZE_EXCEEDED_FAILURE);
        } else if ((cause instanceof FileUploadBase.InvalidContentTypeException)) {
          getCoyoteRequest().recordRequestFailure(RequestFailureType.INVALID_CONTENT_TYPE_FAILURE);
        } else if ((cause instanceof FileUploadException)) {
          getCoyoteRequest().recordRequestFailure(RequestFailureType.FILE_UPLOAD_EXCEPTION);
        } else if ((this.partsParseException instanceof IllegalStateException)) {
          getCoyoteRequest().recordRequestFailure(RequestFailureType.MAX_POST_SIZE_EXCEEDED_FAILURE);
        }
      }
    }
  }
  
  public Part getPart(String name)
    throws IOException, IllegalStateException, ServletException
  {
    Collection<Part> c = getParts();
    Iterator<Part> iterator = c.iterator();
    while (iterator.hasNext())
    {
      Part part = (Part)iterator.next();
      if (name.equals(part.getName())) {
        return part;
      }
    }
    return null;
  }
  
  @Deprecated
  public void doUpgrade(UpgradeInbound inbound)
    throws IOException
  {
    this.coyoteRequest.action(ActionCode.UPGRADE_TOMCAT, inbound);
    
    this.response.setStatus(101);
    this.response.flushBuffer();
  }
  
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass)
    throws ServletException
  {
    T handler;
    try
    {
      handler = (T)this.context.getInstanceManager().newInstance(httpUpgradeHandlerClass);
    }
    catch (InstantiationException e)
    {
      throw new ServletException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new ServletException(e);
    }
    catch (InvocationTargetException e)
    {
      throw new ServletException(e);
    }
    catch (NamingException e)
    {
      throw new ServletException(e);
    }
    this.coyoteRequest.action(ActionCode.UPGRADE, handler);
    
    this.response.setStatus(101);
    
    return handler;
  }
  
  protected Session doGetSession(boolean create)
  {
    if (this.context == null) {
      return null;
    }
    if ((this.session != null) && (!this.session.isValid())) {
      this.session = null;
    }
    if (this.session != null) {
      return this.session;
    }
    Manager manager = null;
    if (this.context != null) {
      manager = this.context.getManager();
    }
    if (manager == null) {
      return null;
    }
    if (this.requestedSessionId != null)
    {
      try
      {
        this.session = manager.findSession(this.requestedSessionId);
      }
      catch (IOException e)
      {
        this.session = null;
      }
      if ((this.session != null) && (!this.session.isValid())) {
        this.session = null;
      }
      if (this.session != null)
      {
        this.session.access();
        return this.session;
      }
    }
    if (!create) {
      return null;
    }
    if ((this.context != null) && (this.response != null) && (this.context.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE)) && (this.response.getResponse().isCommitted())) {
      throw new IllegalStateException(sm.getString("coyoteRequest.sessionCreateCommitted"));
    }
    if ((("/".equals(this.context.getSessionCookiePath())) && (isRequestedSessionIdFromCookie())) || (this.requestedSessionSSL)) {
      this.session = manager.createSession(getRequestedSessionId());
    } else {
      this.session = manager.createSession(null);
    }
    if ((this.session != null) && (getContext() != null) && (getContext().getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE)))
    {
      Cookie cookie = ApplicationSessionCookieConfig.createSessionCookie(this.context, this.session.getIdInternal(), isSecure());
      
      this.response.addSessionCookieInternal(cookie);
    }
    if (this.session == null) {
      return null;
    }
    this.session.access();
    return this.session;
  }
  
  protected String unescape(String s)
  {
    if (s == null) {
      return null;
    }
    if (s.indexOf('\\') == -1) {
      return s;
    }
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c != '\\')
      {
        buf.append(c);
      }
      else
      {
        i++;
        if (i >= s.length()) {
          throw new IllegalArgumentException();
        }
        c = s.charAt(i);
        buf.append(c);
      }
    }
    return buf.toString();
  }
  
  protected void parseCookies()
  {
    this.cookiesParsed = true;
    
    Cookies serverCookies = this.coyoteRequest.getCookies();
    int count = serverCookies.getCookieCount();
    if (count <= 0) {
      return;
    }
    this.cookies = new Cookie[count];
    
    int idx = 0;
    for (int i = 0; i < count; i++)
    {
      ServerCookie scookie = serverCookies.getCookie(i);
      try
      {
        Cookie cookie = new Cookie(scookie.getName().toString(), null);
        int version = scookie.getVersion();
        cookie.setVersion(version);
        cookie.setValue(unescape(scookie.getValue().toString()));
        cookie.setPath(unescape(scookie.getPath().toString()));
        String domain = scookie.getDomain().toString();
        if (domain != null) {
          cookie.setDomain(unescape(domain));
        }
        String comment = scookie.getComment().toString();
        cookie.setComment(version == 1 ? unescape(comment) : null);
        this.cookies[(idx++)] = cookie;
      }
      catch (IllegalArgumentException e) {}
    }
    if (idx < count)
    {
      Cookie[] ncookies = new Cookie[idx];
      System.arraycopy(this.cookies, 0, ncookies, 0, idx);
      this.cookies = ncookies;
    }
  }
  
  protected void parseParameters()
  {
    this.parametersParsed = true;
    
    Parameters parameters = this.coyoteRequest.getParameters();
    boolean success = false;
    try
    {
      parameters.setLimit(getConnector().getMaxParameterCount());
      
      String enc = getCharacterEncoding();
      
      boolean useBodyEncodingForURI = this.connector.getUseBodyEncodingForURI();
      if (enc != null)
      {
        parameters.setEncoding(enc);
        if (useBodyEncodingForURI) {
          parameters.setQueryStringEncoding(enc);
        }
      }
      else
      {
        parameters.setEncoding("ISO-8859-1");
        if (useBodyEncodingForURI) {
          parameters.setQueryStringEncoding("ISO-8859-1");
        }
      }
      parameters.handleQueryParameters();
      if ((this.usingInputStream) || (this.usingReader))
      {
        success = true;
      }
      else if (!getConnector().isParseBodyMethod(getMethod()))
      {
        success = true;
      }
      else
      {
        String contentType = getContentType();
        if (contentType == null) {
          contentType = "";
        }
        int semicolon = contentType.indexOf(';');
        if (semicolon >= 0) {
          contentType = contentType.substring(0, semicolon).trim();
        } else {
          contentType = contentType.trim();
        }
        if ("multipart/form-data".equals(contentType))
        {
          parseParts();
          success = true;
        }
        else if (!"application/x-www-form-urlencoded".equals(contentType))
        {
          success = true;
        }
        else
        {
          int len = getContentLength();
          if (len > 0)
          {
            int maxPostSize = this.connector.getMaxPostSize();
            if ((maxPostSize > 0) && (len > maxPostSize))
            {
              if (this.context.getLogger().isDebugEnabled()) {
                this.context.getLogger().debug(sm.getString("coyoteRequest.postTooLarge"));
              }
              checkSwallowInput();
              
              getCoyoteRequest().recordRequestFailure(RequestFailureType.REQUEST_POST_TOO_LARGE_FAILURE); return;
            }
            byte[] formData = null;
            if (len < CACHED_POST_LEN)
            {
              if (this.postData == null) {
                this.postData = new byte[CACHED_POST_LEN];
              }
              formData = this.postData;
            }
            else
            {
              formData = new byte[len];
            }
            try
            {
              if (readPostBody(formData, len) != len) {
                return;
              }
            }
            catch (IOException e)
            {
              if (this.context.getLogger().isDebugEnabled()) {
                this.context.getLogger().debug(sm.getString("coyoteRequest.parseParameters"), e);
              }
              return;
            }
            parameters.processParameters(formData, 0, len);
          }
          else if ("chunked".equalsIgnoreCase(this.coyoteRequest.getHeader("transfer-encoding")))
          {
            byte[] formData = null;
            try
            {
              formData = readChunkedPostBody();
            }
            catch (IOException e)
            {
              if (this.context.getLogger().isDebugEnabled()) {
                this.context.getLogger().debug(sm.getString("coyoteRequest.parseParameters"), e);
              }
              return;
            }
            if (formData != null) {
              parameters.processParameters(formData, 0, formData.length);
            }
          }
          success = true;
        }
      }
    }
    finally
    {
      if (parameters.isParseFailed()) {
        getCoyoteRequest().recordRequestFailure(RequestFailureType.PARAMETERS_PARSE_FAILURE);
      }
      if (!success) {
        parameters.setParseFailed(true);
      }
    }
  }
  
  protected int readPostBody(byte[] body, int len)
    throws IOException
  {
    int offset = 0;
    do
    {
      int inputLen = getStream().read(body, offset, len - offset);
      if (inputLen <= 0) {
        return offset;
      }
      offset += inputLen;
    } while (len - offset > 0);
    return len;
  }
  
  protected byte[] readChunkedPostBody()
    throws IOException
  {
    ByteChunk body = new ByteChunk();
    
    byte[] buffer = new byte[CACHED_POST_LEN];
    
    int len = 0;
    while (len > -1)
    {
      len = getStream().read(buffer, 0, CACHED_POST_LEN);
      if ((this.connector.getMaxPostSize() > 0) && (body.getLength() + len > this.connector.getMaxPostSize()))
      {
        checkSwallowInput();
        
        getCoyoteRequest().recordRequestFailure(RequestFailureType.REQUEST_CHUNKED_POST_TOO_LARGE_FAILURE);
        
        throw new IOException(sm.getString("coyoteRequest.chunkedPostTooLarge"));
      }
      if (len > 0) {
        body.append(buffer, 0, len);
      }
    }
    if (body.getLength() == 0) {
      return null;
    }
    if (body.getLength() < body.getBuffer().length)
    {
      int length = body.getLength();
      byte[] result = new byte[length];
      System.arraycopy(body.getBuffer(), 0, result, 0, length);
      return result;
    }
    return body.getBuffer();
  }
  
  protected void parseLocales()
  {
    this.localesParsed = true;
    
    TreeMap<Double, ArrayList<Locale>> locales = new TreeMap();
    
    Enumeration<String> values = getHeaders("accept-language");
    while (values.hasMoreElements())
    {
      String value = (String)values.nextElement();
      parseLocalesHeader(value, locales);
    }
    for (ArrayList<Locale> list : locales.values()) {
      for (Locale locale : list) {
        addLocale(locale);
      }
    }
  }
  
  protected void parseLocalesHeader(String value, TreeMap<Double, ArrayList<Locale>> locales)
  {
    int white = value.indexOf(' ');
    if (white < 0) {
      white = value.indexOf('\t');
    }
    if (white >= 0)
    {
      StringBuilder sb = new StringBuilder();
      int len = value.length();
      for (int i = 0; i < len; i++)
      {
        char ch = value.charAt(i);
        if ((ch != ' ') && (ch != '\t')) {
          sb.append(ch);
        }
      }
      this.parser.setString(sb.toString());
    }
    else
    {
      this.parser.setString(value);
    }
    int length = this.parser.getLength();
    for (;;)
    {
      int start = this.parser.getIndex();
      if (start >= length) {
        break;
      }
      int end = this.parser.findChar(',');
      String entry = this.parser.extract(start, end).trim();
      this.parser.advance();
      
      double quality = 1.0D;
      int semi = entry.indexOf(";q=");
      if (semi >= 0)
      {
        try
        {
          String strQuality = entry.substring(semi + 3);
          if (strQuality.length() <= 5) {
            quality = Double.parseDouble(strQuality);
          } else {
            quality = 0.0D;
          }
        }
        catch (NumberFormatException e)
        {
          quality = 0.0D;
        }
        entry = entry.substring(0, semi);
      }
      if ((quality >= 5.0E-5D) && 
      
        (!"*".equals(entry)))
      {
        String language = null;
        String country = null;
        String variant = null;
        int dash = entry.indexOf('-');
        if (dash < 0)
        {
          language = entry;
          country = "";
          variant = "";
        }
        else
        {
          language = entry.substring(0, dash);
          country = entry.substring(dash + 1);
          int vDash = country.indexOf('-');
          if (vDash > 0)
          {
            String cTemp = country.substring(0, vDash);
            variant = country.substring(vDash + 1);
            country = cTemp;
          }
          else
          {
            variant = "";
          }
        }
        if ((isAlpha(language)) && (isAlpha(country)) && (isAlpha(variant)))
        {
          Locale locale = new Locale(language, country, variant);
          Double key = new Double(-quality);
          ArrayList<Locale> values = (ArrayList)locales.get(key);
          if (values == null)
          {
            values = new ArrayList();
            locales.put(key, values);
          }
          values.add(locale);
        }
      }
    }
  }
  
  protected static final boolean isAlpha(String value)
  {
    for (int i = 0; i < value.length(); i++)
    {
      char c = value.charAt(i);
      if (((c < 'a') || (c > 'z')) && ((c < 'A') || (c > 'Z'))) {
        return false;
      }
    }
    return true;
  }
  
  private static final Map<String, SpecialAttributeAdapter> specialAttributes = new HashMap();
  
  static
  {
    specialAttributes.put("org.apache.catalina.core.DISPATCHER_TYPE", new SpecialAttributeAdapter()
    {
      public Object get(Request request, String name)
      {
        return request.internalDispatcherType == null ? DispatcherType.REQUEST : request.internalDispatcherType;
      }
      
      public void set(Request request, String name, Object value)
      {
        request.internalDispatcherType = ((DispatcherType)value);
      }
    });
    specialAttributes.put("org.apache.catalina.core.DISPATCHER_REQUEST_PATH", new SpecialAttributeAdapter()
    {
      public Object get(Request request, String name)
      {
        return request.requestDispatcherPath == null ? request.getRequestPathMB().toString() : request.requestDispatcherPath.toString();
      }
      
      public void set(Request request, String name, Object value)
      {
        request.requestDispatcherPath = value;
      }
    });
    specialAttributes.put("org.apache.catalina.ASYNC_SUPPORTED", new SpecialAttributeAdapter()
    {
      public Object get(Request request, String name)
      {
        return request.asyncSupported;
      }
      
      public void set(Request request, String name, Object value)
      {
        Boolean oldValue = request.asyncSupported;
        request.asyncSupported = ((Boolean)value);
        request.notifyAttributeAssigned(name, value, oldValue);
      }
    });
    specialAttributes.put("org.apache.catalina.realm.GSS_CREDENTIAL", new SpecialAttributeAdapter()
    {
      public Object get(Request request, String name)
      {
        if ((request.userPrincipal instanceof GenericPrincipal)) {
          return ((GenericPrincipal)request.userPrincipal).getGssCredential();
        }
        return null;
      }
      
      public void set(Request request, String name, Object value) {}
    });
    specialAttributes.put("org.apache.catalina.parameter_parse_failed", new SpecialAttributeAdapter()
    {
      public Object get(Request request, String name)
      {
        if (request.getCoyoteRequest().getParameters().isParseFailed()) {
          return Boolean.TRUE;
        }
        return null;
      }
      
      public void set(Request request, String name, Object value) {}
    });
  }
  
  private static abstract interface SpecialAttributeAdapter
  {
    public abstract Object get(Request paramRequest, String paramString);
    
    public abstract void set(Request paramRequest, String paramString, Object paramObject);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\Request.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */