package org.apache.catalina.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.SessionConfig;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.buf.UEncoder;
import org.apache.tomcat.util.buf.UEncoder.SafeCharsSet;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.ServerCookie;
import org.apache.tomcat.util.http.parser.MediaTypeCache;
import org.apache.tomcat.util.net.URL;
import org.apache.tomcat.util.res.StringManager;

public class Response
  implements HttpServletResponse
{
  private static final MediaTypeCache MEDIA_TYPE_CACHE = new MediaTypeCache(100);
  
  static
  {
    URL.isSchemeChar('c');
  }
  
  private static final boolean ENFORCE_ENCODING_IN_GET_WRITER = Boolean.valueOf(System.getProperty("org.apache.catalina.connector.Response.ENFORCE_ENCODING_IN_GET_WRITER", "true")).booleanValue();
  protected static final String info = "org.apache.coyote.catalina.CoyoteResponse/1.0";
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  protected SimpleDateFormat format = null;
  @Deprecated
  protected Connector connector;
  protected org.apache.coyote.Response coyoteResponse;
  protected OutputBuffer outputBuffer;
  protected CoyoteOutputStream outputStream;
  protected CoyoteWriter writer;
  
  @Deprecated
  public Connector getConnector()
  {
    return this.connector;
  }
  
  public void setConnector(Connector connector)
  {
    this.connector = connector;
    if ("AJP/1.3".equals(connector.getProtocol())) {
      this.outputBuffer = new OutputBuffer(8184);
    } else {
      this.outputBuffer = new OutputBuffer();
    }
    this.outputStream = new CoyoteOutputStream(this.outputBuffer);
    this.writer = new CoyoteWriter(this.outputBuffer);
  }
  
  public void setCoyoteResponse(org.apache.coyote.Response coyoteResponse)
  {
    this.coyoteResponse = coyoteResponse;
    this.outputBuffer.setResponse(coyoteResponse);
  }
  
  public org.apache.coyote.Response getCoyoteResponse()
  {
    return this.coyoteResponse;
  }
  
  public Context getContext()
  {
    return this.request.getContext();
  }
  
  @Deprecated
  public void setContext(Context context)
  {
    this.request.setContext(context);
  }
  
  protected boolean appCommitted = false;
  protected boolean included = false;
  private boolean isCharacterEncodingSet = false;
  private final AtomicInteger errorState = new AtomicInteger(0);
  protected boolean usingOutputStream = false;
  protected boolean usingWriter = false;
  protected final UEncoder urlEncoder = new UEncoder(UEncoder.SafeCharsSet.WITH_SLASH);
  protected CharChunk redirectURLCC = new CharChunk();
  
  public void recycle()
  {
    this.outputBuffer.recycle();
    this.usingOutputStream = false;
    this.usingWriter = false;
    this.appCommitted = false;
    this.included = false;
    this.errorState.set(0);
    this.isCharacterEncodingSet = false;
    if ((Globals.IS_SECURITY_ENABLED) || (Connector.RECYCLE_FACADES))
    {
      if (this.facade != null)
      {
        this.facade.clear();
        this.facade = null;
      }
      if (this.outputStream != null)
      {
        this.outputStream.clear();
        this.outputStream = null;
      }
      if (this.writer != null)
      {
        this.writer.clear();
        this.writer = null;
      }
    }
    else
    {
      this.writer.recycle();
    }
  }
  
  public void clearEncoders()
  {
    this.outputBuffer.clearEncoders();
  }
  
  public long getContentWritten()
  {
    return this.outputBuffer.getContentWritten();
  }
  
  public long getBytesWritten(boolean flush)
  {
    if (flush) {
      try
      {
        this.outputBuffer.flush();
      }
      catch (IOException ioe) {}
    }
    return this.coyoteResponse.getBytesWritten(flush);
  }
  
  public void setAppCommitted(boolean appCommitted)
  {
    this.appCommitted = appCommitted;
  }
  
  public boolean isAppCommitted()
  {
    return (this.appCommitted) || (isCommitted()) || (isSuspended()) || ((getContentLength() > 0) && (getContentWritten() >= getContentLength()));
  }
  
  @Deprecated
  public boolean getIncluded()
  {
    return this.included;
  }
  
  @Deprecated
  public void setIncluded(boolean included)
  {
    this.included = included;
  }
  
  public String getInfo()
  {
    return "org.apache.coyote.catalina.CoyoteResponse/1.0";
  }
  
  protected Request request = null;
  
  public Request getRequest()
  {
    return this.request;
  }
  
  public void setRequest(Request request)
  {
    this.request = request;
  }
  
  protected ResponseFacade facade = null;
  
  public HttpServletResponse getResponse()
  {
    if (this.facade == null) {
      this.facade = new ResponseFacade(this);
    }
    return this.facade;
  }
  
  @Deprecated
  public OutputStream getStream()
  {
    if (this.outputStream == null) {
      this.outputStream = new CoyoteOutputStream(this.outputBuffer);
    }
    return this.outputStream;
  }
  
  public void setSuspended(boolean suspended)
  {
    this.outputBuffer.setSuspended(suspended);
  }
  
  public boolean isSuspended()
  {
    return this.outputBuffer.isSuspended();
  }
  
  public boolean isClosed()
  {
    return this.outputBuffer.isClosed();
  }
  
  public boolean setError()
  {
    boolean result = this.errorState.compareAndSet(0, 1);
    if (result)
    {
      Wrapper wrapper = getRequest().getWrapper();
      if (wrapper != null) {
        wrapper.incrementErrorCount();
      }
    }
    return result;
  }
  
  public boolean isError()
  {
    return this.errorState.get() > 0;
  }
  
  public boolean isErrorReportRequired()
  {
    return this.errorState.get() == 1;
  }
  
  public boolean setErrorReported()
  {
    return this.errorState.compareAndSet(1, 2);
  }
  
  @Deprecated
  public ServletOutputStream createOutputStream()
    throws IOException
  {
    if (this.outputStream == null) {
      this.outputStream = new CoyoteOutputStream(this.outputBuffer);
    }
    return this.outputStream;
  }
  
  public void finishResponse()
    throws IOException
  {
    this.outputBuffer.close();
  }
  
  public int getContentLength()
  {
    return this.coyoteResponse.getContentLength();
  }
  
  public String getContentType()
  {
    return this.coyoteResponse.getContentType();
  }
  
  public PrintWriter getReporter()
    throws IOException
  {
    if (this.outputBuffer.isNew())
    {
      this.outputBuffer.checkConverter();
      if (this.writer == null) {
        this.writer = new CoyoteWriter(this.outputBuffer);
      }
      return this.writer;
    }
    return null;
  }
  
  public void flushBuffer()
    throws IOException
  {
    this.outputBuffer.flush();
  }
  
  public int getBufferSize()
  {
    return this.outputBuffer.getBufferSize();
  }
  
  public String getCharacterEncoding()
  {
    return this.coyoteResponse.getCharacterEncoding();
  }
  
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    if (this.usingWriter) {
      throw new IllegalStateException(sm.getString("coyoteResponse.getOutputStream.ise"));
    }
    this.usingOutputStream = true;
    if (this.outputStream == null) {
      this.outputStream = new CoyoteOutputStream(this.outputBuffer);
    }
    return this.outputStream;
  }
  
  public Locale getLocale()
  {
    return this.coyoteResponse.getLocale();
  }
  
  public PrintWriter getWriter()
    throws IOException
  {
    if (this.usingOutputStream) {
      throw new IllegalStateException(sm.getString("coyoteResponse.getWriter.ise"));
    }
    if (ENFORCE_ENCODING_IN_GET_WRITER) {
      setCharacterEncoding(getCharacterEncoding());
    }
    this.usingWriter = true;
    this.outputBuffer.checkConverter();
    if (this.writer == null) {
      this.writer = new CoyoteWriter(this.outputBuffer);
    }
    return this.writer;
  }
  
  public boolean isCommitted()
  {
    return this.coyoteResponse.isCommitted();
  }
  
  public void reset()
  {
    if (this.included) {
      return;
    }
    this.coyoteResponse.reset();
    this.outputBuffer.reset();
    this.usingOutputStream = false;
    this.usingWriter = false;
    this.isCharacterEncodingSet = false;
  }
  
  public void resetBuffer()
  {
    resetBuffer(false);
  }
  
  public void resetBuffer(boolean resetWriterStreamFlags)
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.resetBuffer.ise"));
    }
    this.outputBuffer.reset(resetWriterStreamFlags);
    if (resetWriterStreamFlags)
    {
      this.usingOutputStream = false;
      this.usingWriter = false;
      this.isCharacterEncodingSet = false;
    }
  }
  
  public void setBufferSize(int size)
  {
    if ((isCommitted()) || (!this.outputBuffer.isNew())) {
      throw new IllegalStateException(sm.getString("coyoteResponse.setBufferSize.ise"));
    }
    this.outputBuffer.setBufferSize(size);
  }
  
  public void setContentLength(int length)
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    this.coyoteResponse.setContentLength(length);
  }
  
  public void setContentType(String type)
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    if (type == null)
    {
      this.coyoteResponse.setContentType(null);
      return;
    }
    String[] m = MEDIA_TYPE_CACHE.parse(type);
    if (m == null)
    {
      this.coyoteResponse.setContentTypeNoCharset(type);
      return;
    }
    this.coyoteResponse.setContentTypeNoCharset(m[0]);
    if (m[1] != null) {
      if (!this.usingWriter)
      {
        this.coyoteResponse.setCharacterEncoding(m[1]);
        this.isCharacterEncodingSet = true;
      }
    }
  }
  
  public void setCharacterEncoding(String charset)
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    if (this.usingWriter) {
      return;
    }
    this.coyoteResponse.setCharacterEncoding(charset);
    this.isCharacterEncodingSet = true;
  }
  
  public void setLocale(Locale locale)
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    this.coyoteResponse.setLocale(locale);
    if (this.usingWriter) {
      return;
    }
    if (this.isCharacterEncodingSet) {
      return;
    }
    String charset = getContext().getCharset(locale);
    if (charset != null) {
      this.coyoteResponse.setCharacterEncoding(charset);
    }
  }
  
  public String getHeader(String name)
  {
    return this.coyoteResponse.getMimeHeaders().getHeader(name);
  }
  
  public Collection<String> getHeaderNames()
  {
    MimeHeaders headers = this.coyoteResponse.getMimeHeaders();
    int n = headers.size();
    List<String> result = new ArrayList(n);
    for (int i = 0; i < n; i++) {
      result.add(headers.getName(i).toString());
    }
    return result;
  }
  
  public Collection<String> getHeaders(String name)
  {
    Enumeration<String> enumeration = this.coyoteResponse.getMimeHeaders().values(name);
    
    Vector<String> result = new Vector();
    while (enumeration.hasMoreElements()) {
      result.addElement(enumeration.nextElement());
    }
    return result;
  }
  
  public String getMessage()
  {
    return this.coyoteResponse.getMessage();
  }
  
  public int getStatus()
  {
    return this.coyoteResponse.getStatus();
  }
  
  @Deprecated
  public void reset(int status, String message)
  {
    reset();
    setStatus(status, message);
  }
  
  public void addCookie(Cookie cookie)
  {
    if ((this.included) || (isCommitted())) {
      return;
    }
    StringBuffer sb = generateCookieString(cookie);
    
    addHeader("Set-Cookie", sb.toString());
  }
  
  public void addSessionCookieInternal(Cookie cookie)
  {
    if (isCommitted()) {
      return;
    }
    String name = cookie.getName();
    String headername = "Set-Cookie";
    String startsWith = name + "=";
    StringBuffer sb = generateCookieString(cookie);
    boolean set = false;
    MimeHeaders headers = this.coyoteResponse.getMimeHeaders();
    int n = headers.size();
    for (int i = 0; i < n; i++) {
      if ((headers.getName(i).toString().equals("Set-Cookie")) && 
        (headers.getValue(i).toString().startsWith(startsWith)))
      {
        headers.getValue(i).setString(sb.toString());
        set = true;
      }
    }
    if (!set) {
      addHeader("Set-Cookie", sb.toString());
    }
  }
  
  public StringBuffer generateCookieString(final Cookie cookie)
  {
    final StringBuffer sb = new StringBuffer();
    if (SecurityUtil.isPackageProtectionEnabled()) {
      AccessController.doPrivileged(new PrivilegedAction()
      {
        public Void run()
        {
          ServerCookie.appendCookieValue(sb, cookie.getVersion(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getComment(), cookie.getMaxAge(), cookie.getSecure(), cookie.isHttpOnly());
          
          return null;
        }
      });
    } else {
      ServerCookie.appendCookieValue(sb, cookie.getVersion(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getComment(), cookie.getMaxAge(), cookie.getSecure(), cookie.isHttpOnly());
    }
    return sb;
  }
  
  public void addDateHeader(String name, long value)
  {
    if ((name == null) || (name.length() == 0)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    if (this.format == null)
    {
      this.format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      
      this.format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    addHeader(name, FastHttpDateFormat.formatDate(value, this.format));
  }
  
  public void addHeader(String name, String value)
  {
    if ((name == null) || (name.length() == 0) || (value == null)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    char cc = name.charAt(0);
    if (((cc == 'C') || (cc == 'c')) && 
      (checkSpecialHeader(name, value))) {
      return;
    }
    this.coyoteResponse.addHeader(name, value);
  }
  
  private boolean checkSpecialHeader(String name, String value)
  {
    if (name.equalsIgnoreCase("Content-Type"))
    {
      setContentType(value);
      return true;
    }
    return false;
  }
  
  public void addIntHeader(String name, int value)
  {
    if ((name == null) || (name.length() == 0)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    addHeader(name, "" + value);
  }
  
  public boolean containsHeader(String name)
  {
    char cc = name.charAt(0);
    if ((cc == 'C') || (cc == 'c'))
    {
      if (name.equalsIgnoreCase("Content-Type")) {
        return this.coyoteResponse.getContentType() != null;
      }
      if (name.equalsIgnoreCase("Content-Length")) {
        return this.coyoteResponse.getContentLengthLong() != -1L;
      }
    }
    return this.coyoteResponse.containsHeader(name);
  }
  
  public String encodeRedirectURL(String url)
  {
    if (isEncodeable(toAbsolute(url))) {
      return toEncoded(url, this.request.getSessionInternal().getIdInternal());
    }
    return url;
  }
  
  @Deprecated
  public String encodeRedirectUrl(String url)
  {
    return encodeRedirectURL(url);
  }
  
  public String encodeURL(String url)
  {
    String absolute;
    try
    {
      absolute = toAbsolute(url);
    }
    catch (IllegalArgumentException iae)
    {
      return url;
    }
    if (isEncodeable(absolute))
    {
      if (url.equalsIgnoreCase("")) {
        url = absolute;
      } else if ((url.equals(absolute)) && (!hasPath(url))) {
        url = url + '/';
      }
      return toEncoded(url, this.request.getSessionInternal().getIdInternal());
    }
    return url;
  }
  
  @Deprecated
  public String encodeUrl(String url)
  {
    return encodeURL(url);
  }
  
  public void sendAcknowledgement()
    throws IOException
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    this.coyoteResponse.acknowledge();
  }
  
  public void sendError(int status)
    throws IOException
  {
    sendError(status, null);
  }
  
  public void sendError(int status, String message)
    throws IOException
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.sendError.ise"));
    }
    if (this.included) {
      return;
    }
    setError();
    
    this.coyoteResponse.setStatus(status);
    this.coyoteResponse.setMessage(message);
    
    resetBuffer();
    
    setSuspended(true);
  }
  
  public void sendRedirect(String location)
    throws IOException
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.sendRedirect.ise"));
    }
    if (this.included) {
      return;
    }
    resetBuffer(true);
    try
    {
      String absolute = toAbsolute(location);
      setStatus(302);
      setHeader("Location", absolute);
      if (getContext().getSendRedirectBody())
      {
        PrintWriter writer = getWriter();
        writer.print(sm.getString("coyoteResponse.sendRedirect.note", new Object[] { RequestUtil.filter(absolute) }));
        
        flushBuffer();
      }
    }
    catch (IllegalArgumentException e)
    {
      setStatus(404);
    }
    setSuspended(true);
  }
  
  public void setDateHeader(String name, long value)
  {
    if ((name == null) || (name.length() == 0)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    if (this.format == null)
    {
      this.format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      
      this.format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    setHeader(name, FastHttpDateFormat.formatDate(value, this.format));
  }
  
  public void setHeader(String name, String value)
  {
    if ((name == null) || (name.length() == 0) || (value == null)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    char cc = name.charAt(0);
    if (((cc == 'C') || (cc == 'c')) && 
      (checkSpecialHeader(name, value))) {
      return;
    }
    this.coyoteResponse.setHeader(name, value);
  }
  
  public void setIntHeader(String name, int value)
  {
    if ((name == null) || (name.length() == 0)) {
      return;
    }
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    setHeader(name, "" + value);
  }
  
  public void setStatus(int status)
  {
    setStatus(status, null);
  }
  
  @Deprecated
  public void setStatus(int status, String message)
  {
    if (isCommitted()) {
      return;
    }
    if (this.included) {
      return;
    }
    this.coyoteResponse.setStatus(status);
    this.coyoteResponse.setMessage(message);
  }
  
  protected boolean isEncodeable(final String location)
  {
    if (location == null) {
      return false;
    }
    if (location.startsWith("#")) {
      return false;
    }
    final Request hreq = this.request;
    final Session session = hreq.getSessionInternal(false);
    if (session == null) {
      return false;
    }
    if (hreq.isRequestedSessionIdFromCookie()) {
      return false;
    }
    if (!hreq.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.URL)) {
      return false;
    }
    if (SecurityUtil.isPackageProtectionEnabled()) {
      ((Boolean)AccessController.doPrivileged(new PrivilegedAction()
      {
        public Boolean run()
        {
          return Boolean.valueOf(Response.this.doIsEncodeable(hreq, session, location));
        }
      })).booleanValue();
    }
    return doIsEncodeable(hreq, session, location);
  }
  
  private boolean doIsEncodeable(Request hreq, Session session, String location)
  {
    URL url = null;
    try
    {
      url = new URL(location);
    }
    catch (MalformedURLException e)
    {
      return false;
    }
    if (!hreq.getScheme().equalsIgnoreCase(url.getProtocol())) {
      return false;
    }
    if (!hreq.getServerName().equalsIgnoreCase(url.getHost())) {
      return false;
    }
    int serverPort = hreq.getServerPort();
    if (serverPort == -1) {
      if ("https".equals(hreq.getScheme())) {
        serverPort = 443;
      } else {
        serverPort = 80;
      }
    }
    int urlPort = url.getPort();
    if (urlPort == -1) {
      if ("https".equals(url.getProtocol())) {
        urlPort = 443;
      } else {
        urlPort = 80;
      }
    }
    if (serverPort != urlPort) {
      return false;
    }
    String contextPath = getContext().getPath();
    if (contextPath != null)
    {
      String file = url.getFile();
      if ((file == null) || (!file.startsWith(contextPath))) {
        return false;
      }
      String tok = ";" + SessionConfig.getSessionUriParamName(this.request.getContext()) + "=" + session.getIdInternal();
      if (file.indexOf(tok, contextPath.length()) >= 0) {
        return false;
      }
    }
    return true;
  }
  
  protected String toAbsolute(String location)
  {
    if (location == null) {
      return location;
    }
    boolean leadingSlash = location.startsWith("/");
    if (location.startsWith("//"))
    {
      this.redirectURLCC.recycle();
      
      String scheme = this.request.getScheme();
      try
      {
        this.redirectURLCC.append(scheme, 0, scheme.length());
        this.redirectURLCC.append(':');
        this.redirectURLCC.append(location, 0, location.length());
        return this.redirectURLCC.toString();
      }
      catch (IOException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException(location);
        
        iae.initCause(e);
        throw iae;
      }
    }
    if ((leadingSlash) || (!hasScheme(location)))
    {
      this.redirectURLCC.recycle();
      
      String scheme = this.request.getScheme();
      String name = this.request.getServerName();
      int port = this.request.getServerPort();
      try
      {
        this.redirectURLCC.append(scheme, 0, scheme.length());
        this.redirectURLCC.append("://", 0, 3);
        this.redirectURLCC.append(name, 0, name.length());
        if (((scheme.equals("http")) && (port != 80)) || ((scheme.equals("https")) && (port != 443)))
        {
          this.redirectURLCC.append(':');
          String portS = port + "";
          this.redirectURLCC.append(portS, 0, portS.length());
        }
        if (!leadingSlash)
        {
          String relativePath = this.request.getDecodedRequestURI();
          int pos = relativePath.lastIndexOf('/');
          CharChunk encodedURI = null;
          final String frelativePath = relativePath;
          final int fend = pos;
          if (SecurityUtil.isPackageProtectionEnabled()) {
            try
            {
              encodedURI = (CharChunk)AccessController.doPrivileged(new PrivilegedExceptionAction()
              {
                public CharChunk run()
                  throws IOException
                {
                  return Response.this.urlEncoder.encodeURL(frelativePath, 0, fend);
                }
              });
            }
            catch (PrivilegedActionException pae)
            {
              IllegalArgumentException iae = new IllegalArgumentException(location);
              
              iae.initCause(pae.getException());
              throw iae;
            }
          } else {
            encodedURI = this.urlEncoder.encodeURL(relativePath, 0, pos);
          }
          this.redirectURLCC.append(encodedURI);
          encodedURI.recycle();
          this.redirectURLCC.append('/');
        }
        this.redirectURLCC.append(location, 0, location.length());
        
        normalize(this.redirectURLCC);
      }
      catch (IOException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException(location);
        
        iae.initCause(e);
        throw iae;
      }
      return this.redirectURLCC.toString();
    }
    return location;
  }
  
  private void normalize(CharChunk cc)
  {
    int truncate = cc.indexOf('?');
    if (truncate == -1) {
      truncate = cc.indexOf('#');
    }
    char[] truncateCC = null;
    if (truncate > -1)
    {
      truncateCC = Arrays.copyOfRange(cc.getBuffer(), cc.getStart() + truncate, cc.getEnd());
      
      cc.setEnd(cc.getStart() + truncate);
    }
    if ((cc.endsWith("/.")) || (cc.endsWith("/.."))) {
      try
      {
        cc.append('/');
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException(cc.toString(), e);
      }
    }
    char[] c = cc.getChars();
    int start = cc.getStart();
    int end = cc.getEnd();
    int index = 0;
    int startIndex = 0;
    for (int i = 0; i < 3; i++) {
      startIndex = cc.indexOf('/', startIndex + 1);
    }
    index = startIndex;
    for (;;)
    {
      index = cc.indexOf("/./", 0, 3, index);
      if (index < 0) {
        break;
      }
      copyChars(c, start + index, start + index + 2, end - start - index - 2);
      
      end -= 2;
      cc.setEnd(end);
    }
    index = startIndex;
    for (;;)
    {
      index = cc.indexOf("/../", 0, 4, index);
      if (index < 0) {
        break;
      }
      if (index == startIndex) {
        throw new IllegalArgumentException();
      }
      int index2 = -1;
      for (int pos = start + index - 1; (pos >= 0) && (index2 < 0); pos--) {
        if (c[pos] == '/') {
          index2 = pos;
        }
      }
      copyChars(c, start + index2, start + index + 3, end - start - index - 3);
      
      end = end + index2 - index - 3;
      cc.setEnd(end);
      index = index2;
    }
    if (truncateCC != null) {
      try
      {
        cc.append(truncateCC, 0, truncateCC.length);
      }
      catch (IOException ioe)
      {
        throw new IllegalArgumentException(ioe);
      }
    }
  }
  
  private void copyChars(char[] c, int dest, int src, int len)
  {
    for (int pos = 0; pos < len; pos++) {
      c[(pos + dest)] = c[(pos + src)];
    }
  }
  
  private boolean hasPath(String uri)
  {
    int pos = uri.indexOf("://");
    if (pos < 0) {
      return false;
    }
    pos = uri.indexOf('/', pos + 3);
    if (pos < 0) {
      return false;
    }
    return true;
  }
  
  private boolean hasScheme(String uri)
  {
    int len = uri.length();
    for (int i = 0; i < len; i++)
    {
      char c = uri.charAt(i);
      if (c == ':') {
        return i > 0;
      }
      if (!URL.isSchemeChar(c)) {
        return false;
      }
    }
    return false;
  }
  
  protected String toEncoded(String url, String sessionId)
  {
    if ((url == null) || (sessionId == null)) {
      return url;
    }
    String path = url;
    String query = "";
    String anchor = "";
    int question = url.indexOf('?');
    if (question >= 0)
    {
      path = url.substring(0, question);
      query = url.substring(question);
    }
    int pound = path.indexOf('#');
    if (pound >= 0)
    {
      anchor = path.substring(pound);
      path = path.substring(0, pound);
    }
    StringBuilder sb = new StringBuilder(path);
    if (sb.length() > 0)
    {
      sb.append(";");
      sb.append(SessionConfig.getSessionUriParamName(this.request.getContext()));
      
      sb.append("=");
      sb.append(sessionId);
    }
    sb.append(anchor);
    sb.append(query);
    return sb.toString();
  }
  
  public Response() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\Response.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */