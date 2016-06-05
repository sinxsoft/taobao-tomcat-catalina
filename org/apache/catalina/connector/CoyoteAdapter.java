package org.apache.catalina.connector;

import com.taobao.tomcat.util.http.CookieWarn;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletContext;
import javax.servlet.SessionTrackingMode;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometEvent.EventSubType;
import org.apache.catalina.comet.CometEvent.EventType;
import org.apache.catalina.core.AsyncContextImpl;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.SessionConfig;
import org.apache.catalina.util.URLEncoder;
import org.apache.coyote.ActionCode;
import org.apache.coyote.Adapter;
import org.apache.coyote.RequestInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.http.Cookies;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.Parameters;
import org.apache.tomcat.util.http.ServerCookie;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.res.StringManager;

public class CoyoteAdapter
  implements Adapter
{
  private static final Log log = LogFactory.getLog(CoyoteAdapter.class);
  private static final String POWERED_BY = "Servlet/3.0 JSP/2.2 (" + ServerInfo.getServerInfo() + " Java/" + System.getProperty("java.vm.vendor") + "/" + System.getProperty("java.runtime.version") + ")";
  private static final EnumSet<SessionTrackingMode> SSL_ONLY = EnumSet.of(SessionTrackingMode.SSL);
  public static final int ADAPTER_NOTES = 1;
  protected static final boolean ALLOW_BACKSLASH = Boolean.valueOf(System.getProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", "false")).booleanValue();
  
  public CoyoteAdapter(Connector connector)
  {
    this.connector = connector;
  }
  
  private Connector connector = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  protected static URLEncoder urlEncoder = new URLEncoder();
  
  static
  {
    urlEncoder.addSafeCharacter('-');
    urlEncoder.addSafeCharacter('_');
    urlEncoder.addSafeCharacter('.');
    urlEncoder.addSafeCharacter('*');
    urlEncoder.addSafeCharacter('/');
  }
  
  public boolean event(org.apache.coyote.Request req, org.apache.coyote.Response res, SocketStatus status)
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    if (request.getWrapper() == null) {
      return false;
    }
    boolean error = false;
    boolean read = false;
    try
    {
      if (status == SocketStatus.OPEN_READ)
      {
        if (response.isClosed())
        {        	
          request.getEvent().setEventType(CometEvent.EventType.END);
          request.getEvent().setEventSubType(null);
        }
        else
        {
          try
          {
            if (request.read()) {
              read = true;
            }
          }
          catch (IOException e)
          {
            error = true;
          }
          if (read)
          {
            request.getEvent().setEventType(CometEvent.EventType.READ);
            request.getEvent().setEventSubType(null);
          }
          else if (error)
          {
            request.getEvent().setEventType(CometEvent.EventType.ERROR);
            request.getEvent().setEventSubType(CometEvent.EventSubType.CLIENT_DISCONNECT);
          }
          else
          {
            request.getEvent().setEventType(CometEvent.EventType.END);
            request.getEvent().setEventSubType(null);
          }
        }
      }
      else if (status == SocketStatus.DISCONNECT)
      {
        request.getEvent().setEventType(CometEvent.EventType.ERROR);
        request.getEvent().setEventSubType(CometEvent.EventSubType.CLIENT_DISCONNECT);
        error = true;
      }
      else if (status == SocketStatus.ERROR)
      {
        request.getEvent().setEventType(CometEvent.EventType.ERROR);
        request.getEvent().setEventSubType(CometEvent.EventSubType.IOEXCEPTION);
        error = true;
      }
      else if (status == SocketStatus.STOP)
      {
        request.getEvent().setEventType(CometEvent.EventType.END);
        request.getEvent().setEventSubType(CometEvent.EventSubType.SERVER_SHUTDOWN);
      }
      else if (status == SocketStatus.TIMEOUT)
      {
        if (response.isClosed())
        {
          request.getEvent().setEventType(CometEvent.EventType.END);
          request.getEvent().setEventSubType(null);
        }
        else
        {
          request.getEvent().setEventType(CometEvent.EventType.ERROR);
          request.getEvent().setEventSubType(CometEvent.EventSubType.TIMEOUT);
        }
      }
      req.getRequestProcessor().setWorkerThreadName(Thread.currentThread().getName());
      
      this.connector.getService().getContainer().getPipeline().getFirst().event(request, response, request.getEvent());
      if ((!error) && (!response.isClosed()) && (request.getAttribute("javax.servlet.error.exception") != null))
      {
        request.getEvent().setEventType(CometEvent.EventType.ERROR);
        request.getEvent().setEventSubType(null);
        error = true;
        this.connector.getService().getContainer().getPipeline().getFirst().event(request, response, request.getEvent());
      }
      if ((response.isClosed()) || (!request.isComet()))
      {
        if ((status == SocketStatus.OPEN_READ) && (request.getEvent().getEventType() != CometEvent.EventType.END))
        {
          request.getEvent().setEventType(CometEvent.EventType.END);
          request.getEvent().setEventSubType(null);
          error = true;
          this.connector.getService().getContainer().getPipeline().getFirst().event(request, response, request.getEvent());
        }
        res.action(ActionCode.COMET_END, null);
      }
      else if ((!error) && (read) && (request.getAvailable()))
      {
        request.getEvent().setEventType(CometEvent.EventType.ERROR);
        request.getEvent().setEventSubType(CometEvent.EventSubType.IOEXCEPTION);
        error = true;
        this.connector.getService().getContainer().getPipeline().getFirst().event(request, response, request.getEvent());
      }
      return !error ;
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      if (!(t instanceof IOException)) {
        log.error(sm.getString("coyoteAdapter.service"), t);
      }
      error = true;
      return false;
    }
    finally
    {
      req.getRequestProcessor().setWorkerThreadName(null);
      if ((error) || (response.isClosed()) || (!request.isComet()))
      {
        ((Context)request.getMappingData().context).logAccess(request, response, System.currentTimeMillis() - req.getStartTime(), false);
        
        request.recycle();
        request.setFilterChain(null);
        response.recycle();
      }
    }
  }
  
  public boolean asyncDispatch(org.apache.coyote.Request req, org.apache.coyote.Response res, SocketStatus status)
    throws Exception
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    if (request == null) {
      throw new IllegalStateException("Dispatch may only happen on an existing request.");
    }
    boolean comet = false;
    boolean success = true;
    AsyncContextImpl asyncConImpl = (AsyncContextImpl)request.getAsyncContext();
    req.getRequestProcessor().setWorkerThreadName(Thread.currentThread().getName());
    try
    {
      if ((!request.isAsync()) && (!comet))
      {
        Context ctxt = (Context)request.getMappingData().context;
        if (ctxt != null) {
          ctxt.fireRequestDestroyEvent(request);
        }
        response.setSuspended(false);
      }
      if ((status == SocketStatus.TIMEOUT) && 
        (!asyncConImpl.timeout())) {
        asyncConImpl.setErrorState(null, false);
      }
      if ((!request.isAsyncDispatching()) && (request.isAsync()) && (response.isErrorReportRequired())) {
        this.connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
      }
      if (request.isAsyncDispatching())
      {
        this.connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
        Throwable t = (Throwable)request.getAttribute("javax.servlet.error.exception");
        if (t != null) {
          asyncConImpl.setErrorState(t, true);
        }
      }
      if (request.isComet()) {
        if ((!response.isClosed()) && (!response.isError()))
        {
          if ((request.getAvailable()) || ((request.getContentLength() > 0) && (!request.isParametersParsed())))
          {
            if (event(req, res, SocketStatus.OPEN_READ))
            {
              comet = true;
              res.action(ActionCode.COMET_BEGIN, null);
            }
          }
          else
          {
            comet = true;
            res.action(ActionCode.COMET_BEGIN, null);
          }
        }
        else {
          request.setFilterChain(null);
        }
      }
      if ((!request.isAsync()) && (!comet))
      {
        request.finishRequest();
        response.finishResponse();
        req.action(ActionCode.POST_REQUEST, null);
        ((Context)request.getMappingData().context).logAccess(request, response, System.currentTimeMillis() - req.getStartTime(), false);
      }
      AtomicBoolean error = new AtomicBoolean(false);
      res.action(ActionCode.IS_ERROR, error);
      if (error.get()) {
        success = false;
      }
    }
    catch (IOException e)
    {
      success = false;
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      success = false;
      log.error(sm.getString("coyoteAdapter.service"), t);
    }
    finally
    {
      req.getRequestProcessor().setWorkerThreadName(null);
      if ((!success) || ((!comet) && (!request.isAsync())))
      {
        request.recycle();
        response.recycle();
      }
      else
      {
        request.clearEncoders();
        response.clearEncoders();
      }
    }
    return success;
  }
  
  public void service(org.apache.coyote.Request req, org.apache.coyote.Response res)
    throws Exception
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    if (request == null)
    {
      request = this.connector.createRequest();
      request.setCoyoteRequest(req);
      response = this.connector.createResponse();
      response.setCoyoteResponse(res);
      
      request.setResponse(response);
      response.setRequest(request);
      
      req.setNote(1, request);
      res.setNote(1, response);
      
      req.getParameters().setQueryStringEncoding(this.connector.getURIEncoding());
    }
    if (this.connector.getXpoweredBy()) {
      response.addHeader("X-Powered-By", POWERED_BY);
    }
    boolean comet = false;
    boolean async = false;
    try
    {
      req.getRequestProcessor().setWorkerThreadName(Thread.currentThread().getName());
      boolean postParseSuccess = postParseRequest(req, request, res, response);
      if (postParseSuccess)
      {
        request.setAsyncSupported(this.connector.getService().getContainer().getPipeline().isAsyncSupported());
        
        this.connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
        if (request.isComet()) {
          if ((!response.isClosed()) && (!response.isError()))
          {
            if ((request.getAvailable()) || ((request.getContentLength() > 0) && (!request.isParametersParsed())))
            {
              if (event(req, res, SocketStatus.OPEN_READ))
              {
                comet = true;
                res.action(ActionCode.COMET_BEGIN, null);
              }
            }
            else
            {
              comet = true;
              res.action(ActionCode.COMET_BEGIN, null);
            }
          }
          else {
            request.setFilterChain(null);
          }
        }
      }
      AsyncContextImpl asyncConImpl = (AsyncContextImpl)request.getAsyncContext();
      if (asyncConImpl != null)
      {
        async = true;
      }
      else if (!comet)
      {
        request.finishRequest();
        response.finishResponse();
        if ((postParseSuccess) && (request.getMappingData().context != null)) {
          ((Context)request.getMappingData().context).logAccess(request, response, System.currentTimeMillis() - req.getStartTime(), false);
        }
        req.action(ActionCode.POST_REQUEST, null);
      }
    }
    catch (IOException e) {}finally
    {

      req.getRequestProcessor().setWorkerThreadName(null);
      AtomicBoolean error = new AtomicBoolean(false);
      res.action(ActionCode.IS_ERROR, error);
      if (((!comet) && (!async)) || (error.get()))
      {
        request.recycle();
        response.recycle();
      }
      else
      {
        request.clearEncoders();
        response.clearEncoders();
      }
    }
  }
  
  public void errorDispatch(org.apache.coyote.Request req, org.apache.coyote.Response res)
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    if ((request != null) && (request.getMappingData().context != null)) {
      ((Context)request.getMappingData().context).logAccess(request, response, System.currentTimeMillis() - req.getStartTime(), false);
    } else {
      log(req, res, System.currentTimeMillis() - req.getStartTime());
    }
    if (request != null) {
      request.recycle();
    }
    if (response != null) {
      response.recycle();
    }
    req.recycle();
    res.recycle();
  }
  
  public void log(org.apache.coyote.Request req, org.apache.coyote.Response res, long time)
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    if (request == null)
    {
      request = this.connector.createRequest();
      request.setCoyoteRequest(req);
      response = this.connector.createResponse();
      response.setCoyoteResponse(res);
      
      request.setResponse(response);
      response.setRequest(request);
      
      req.setNote(1, request);
      res.setNote(1, response);
      
      req.getParameters().setQueryStringEncoding(this.connector.getURIEncoding());
    }
    try
    {
      boolean logged = false;
      if (request.mappingData != null) {
        if (request.mappingData.context != null)
        {
          logged = true;
          ((Context)request.mappingData.context).logAccess(request, response, time, true);
        }
        else if (request.mappingData.host != null)
        {
          logged = true;
          ((Host)request.mappingData.host).logAccess(request, response, time, true);
        }
      }
      if (!logged) {
        this.connector.getService().getContainer().logAccess(request, response, time, true);
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log.warn(sm.getString("coyoteAdapter.accesslogFail"), t);
    }
    finally
    {
      request.recycle();
      response.recycle();
    }
  }
  
  public void checkRecycled(org.apache.coyote.Request req, org.apache.coyote.Response res)
  {
    Request request = (Request)req.getNote(1);
    Response response = (Response)res.getNote(1);
    String messageKey = null;
    if ((request != null) && (request.getHost() != null)) {
      messageKey = "coyoteAdapter.checkRecycled.request";
    } else if ((response != null) && (response.getContentWritten() != 0L)) {
      messageKey = "coyoteAdapter.checkRecycled.response";
    }
    if (messageKey != null)
    {
      log(req, res, 0L);
      if (this.connector.getState().isAvailable())
      {
        if (log.isInfoEnabled()) {
          log.info(sm.getString(messageKey), new RecycleRequiredException());
        }
      }
      else if (log.isDebugEnabled()) {
        log.debug(sm.getString(messageKey), new RecycleRequiredException());
      }
    }
  }
  
  public String getDomain()
  {
    return this.connector.getDomain();
  }
  
  protected boolean postParseRequest(org.apache.coyote.Request req, Request request, org.apache.coyote.Response res, Response response)
    throws Exception
  {
    if (!req.scheme().isNull())
    {
      request.setSecure(req.scheme().equals("https"));
    }
    else
    {
      req.scheme().setString(this.connector.getScheme());
      request.setSecure(this.connector.getSecure());
    }
    String proxyName = this.connector.getProxyName();
    int proxyPort = this.connector.getProxyPort();
    if (proxyPort != 0) {
      req.setServerPort(proxyPort);
    }
    if (proxyName != null) {
      req.serverName().setString(proxyName);
    }
    MessageBytes decodedURI = req.decodedURI();
    decodedURI.duplicate(req.requestURI());
    
    parsePathParameters(req, request);
    try
    {
      req.getURLDecoder().convert(decodedURI, false);
    }
    catch (IOException ioe)
    {
      res.setStatus(400);
      res.setMessage("Invalid URI: " + ioe.getMessage());
      this.connector.getService().getContainer().logAccess(request, response, 0L, true);
      
      return false;
    }
    if (!normalize(req.decodedURI()))
    {
      res.setStatus(400);
      res.setMessage("Invalid URI");
      this.connector.getService().getContainer().logAccess(request, response, 0L, true);
      
      return false;
    }
    convertURI(decodedURI, request);
    if (!checkNormalize(req.decodedURI()))
    {
      res.setStatus(400);
      res.setMessage("Invalid URI character encoding");
      this.connector.getService().getContainer().logAccess(request, response, 0L, true);
      
      return false;
    }
    String principal = req.getRemoteUser().toString();
    if (principal != null) {
      request.setUserPrincipal(new CoyotePrincipal(principal));
    }
    String authtype = req.getAuthType().toString();
    if (authtype != null) {
      request.setAuthType(authtype);
    }
    MessageBytes serverName;
    if (this.connector.getUseIPVHosts())
    {
      serverName = req.localName();
      if (serverName.isNull()) {
        res.action(ActionCode.REQ_LOCAL_NAME_ATTRIBUTE, null);
      }
    }
    else
    {
      serverName = req.serverName();
    }
    if (request.isAsyncStarted()) {
      request.getMappingData().recycle();
    }
    String version = null;
    Context versionContext = null;
    boolean mapRequired = true;
    while (mapRequired)
    {
      this.connector.getMapper().map(serverName, decodedURI, version, request.getMappingData());
      
      request.setContext((Context)request.getMappingData().context);
      request.setWrapper((Wrapper)request.getMappingData().wrapper);
      if (request.getContext() == null)
      {
        res.setStatus(404);
        res.setMessage("Not found");
        
        Host host = request.getHost();
        if (host != null) {
          host.logAccess(request, response, 0L, true);
        }
        return false;
      }
      if (request.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.URL))
      {
        String sessionID = request.getPathParameter(SessionConfig.getSessionUriParamName(request.getContext()));
        if (sessionID != null)
        {
          request.setRequestedSessionId(sessionID);
          request.setRequestedSessionURL(true);
        }
      }
      parseSessionCookiesId(req, request);
      parseSessionSslId(request);
      
      String sessionID = request.getRequestedSessionId();
      
      mapRequired = false;
      if ((version == null) || (request.getContext() != versionContext))
      {
        version = null;
        versionContext = null;
        
        Object[] contexts = request.getMappingData().contexts;
        if ((contexts != null) && (sessionID != null)) {
          for (int i = contexts.length; i > 0; i--)
          {
            Context ctxt = (Context)contexts[(i - 1)];
            if (ctxt.getManager().findSession(sessionID) != null)
            {
              if (ctxt.equals(request.getMappingData().context)) {
                break;
              }
              version = ctxt.getWebappVersion();
              versionContext = ctxt;
              
              request.getMappingData().recycle();
              mapRequired = true; break;
            }
          }
        }
      }
      if ((!mapRequired) && (request.getContext().getPaused()))
      {
        try
        {
          Thread.sleep(1000L);
        }
        catch (InterruptedException e) {}
        request.getMappingData().recycle();
        mapRequired = true;
      }
    }
    MessageBytes redirectPathMB = request.getMappingData().redirectPath;
    if (!redirectPathMB.isNull())
    {
      String redirectPath = urlEncoder.encode(redirectPathMB.toString());
      String query = request.getQueryString();
      if (request.isRequestedSessionIdFromURL()) {
        redirectPath = redirectPath + ";" + SessionConfig.getSessionUriParamName(request.getContext()) + "=" + request.getRequestedSessionId();
      }
      if (query != null) {
        redirectPath = redirectPath + "?" + query;
      }
      response.sendRedirect(redirectPath);
      request.getContext().logAccess(request, response, 0L, true);
      return false;
    }
    if ((!this.connector.getAllowTrace()) && (req.method().equalsIgnoreCase("TRACE")))
    {
      Wrapper wrapper = request.getWrapper();
      String header = null;
      if (wrapper != null)
      {
        String[] methods = wrapper.getServletMethods();
        if (methods != null) {
          for (int i = 0; i < methods.length; i++) {
            if (!"TRACE".equals(methods[i])) {
              if (header == null) {
                header = methods[i];
              } else {
                header = header + ", " + methods[i];
              }
            }
          }
        }
      }
      res.setStatus(405);
      res.addHeader("Allow", header);
      res.setMessage("TRACE method is not allowed");
      request.getContext().logAccess(request, response, 0L, true);
      return false;
    }
    return true;
  }
  
  protected void parsePathParameters(org.apache.coyote.Request req, Request request)
  {
    req.decodedURI().toBytes();
    
    ByteChunk uriBC = req.decodedURI().getByteChunk();
    int semicolon = uriBC.indexOf(';', 0);
    
    String enc = this.connector.getURIEncoding();
    if (enc == null) {
      enc = "ISO-8859-1";
    }
    Charset charset = null;
    try
    {
      charset = B2CConverter.getCharset(enc);
    }
    catch (UnsupportedEncodingException e1)
    {
      log.warn(sm.getString("coyoteAdapter.parsePathParam", new Object[] { enc }));
    }
    if (log.isDebugEnabled())
    {
      log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "uriBC", uriBC.toString() }));
      
      log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "semicolon", String.valueOf(semicolon) }));
      
      log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "enc", enc }));
    }
    while (semicolon > -1)
    {
      int start = uriBC.getStart();
      int end = uriBC.getEnd();
      
      int pathParamStart = semicolon + 1;
      int pathParamEnd = ByteChunk.findBytes(uriBC.getBuffer(), start + pathParamStart, end, new byte[] { 59, 47 });
      
      String pv = null;
      if (pathParamEnd >= 0)
      {
        if (charset != null) {
          pv = new String(uriBC.getBuffer(), start + pathParamStart, pathParamEnd - pathParamStart, charset);
        }
        byte[] buf = uriBC.getBuffer();
        for (int i = 0; i < end - start - pathParamEnd; i++) {
          buf[(start + semicolon + i)] = buf[(start + i + pathParamEnd)];
        }
        uriBC.setBytes(buf, start, end - start - pathParamEnd + semicolon);
      }
      else
      {
        if (charset != null) {
          pv = new String(uriBC.getBuffer(), start + pathParamStart, end - start - pathParamStart, charset);
        }
        uriBC.setEnd(start + semicolon);
      }
      if (log.isDebugEnabled())
      {
        log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "pathParamStart", String.valueOf(pathParamStart) }));
        
        log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "pathParamEnd", String.valueOf(pathParamEnd) }));
        
        log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "pv", pv }));
      }
      if (pv != null)
      {
        int equals = pv.indexOf('=');
        if (equals > -1)
        {
          String name = pv.substring(0, equals);
          String value = pv.substring(equals + 1);
          request.addPathParameter(name, value);
          if (log.isDebugEnabled())
          {
            log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "equals", String.valueOf(equals) }));
            
            log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "name", name }));
            
            log.debug(sm.getString("coyoteAdapter.debug", new Object[] { "value", value }));
          }
        }
      }
      semicolon = uriBC.indexOf(';', semicolon);
    }
  }
  
  protected void parseSessionSslId(Request request)
  {
    if ((request.getRequestedSessionId() == null) && (SSL_ONLY.equals(request.getServletContext().getEffectiveSessionTrackingModes())) && (request.connector.secure))
    {
      request.setRequestedSessionId(request.getAttribute("javax.servlet.request.ssl_session_id").toString());
      
      request.setRequestedSessionSSL(true);
    }
  }
  
  protected void parseSessionCookiesId(org.apache.coyote.Request req, Request request)
  {
    Context context = (Context)request.getMappingData().context;
    if ((context != null) && (!context.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE))) {
      return;
    }
    Cookies serverCookies = req.getCookies();
    int count = serverCookies.getCookieCount();
    
    CookieWarn cookieWarn = serverCookies.getCookieWarn();
    if ((log.isDebugEnabled()) && (cookieWarn != null)) {
      logCookieWarn(req, request, cookieWarn);
    }
    if (count <= 0) {
      return;
    }
    String sessionCookieName = SessionConfig.getSessionCookieName(context);
    for (int i = 0; i < count; i++)
    {
      ServerCookie scookie = serverCookies.getCookie(i);
      if (scookie.getName().equals(sessionCookieName)) {
        if (!request.isRequestedSessionIdFromCookie())
        {
          convertMB(scookie.getValue());
          request.setRequestedSessionId(scookie.getValue().toString());
          
          request.setRequestedSessionCookie(true);
          request.setRequestedSessionURL(false);
          if (log.isDebugEnabled()) {
            log.debug(" Requested cookie session id is " + request.getRequestedSessionId());
          }
        }
        else if (!request.isRequestedSessionIdValid())
        {
          convertMB(scookie.getValue());
          request.setRequestedSessionId(scookie.getValue().toString());
        }
      }
    }
  }
  
  private void logCookieWarn(org.apache.coyote.Request req, Request request, CookieWarn cookieWarn)
  {
    StringBuilder buf = new StringBuilder();
    buf.append("Bad request. CookieWarn[").append(System.identityHashCode(cookieWarn)).append("]: ").append(cookieWarn.getMessage()).append("\n\tLocalName: ").append(request.getLocalName()).append(", LocalAddr: ").append(request.getLocalAddr()).append(", LocalPort: ").append(request.getLocalPort()).append("\n\tRemoteHost: ").append(request.getRemoteHost()).append(", RemoteAddr: ").append(request.getRemoteAddr()).append(", RemotePort: ").append(request.getRemotePort()).append("\n\tRequestURI: ").append(request.getRequestURI()).append("\n\tCOOKIE: ").append(cookieWarn.getCookieString()).append("\nHTTP HEADERs:");
    
    MimeHeaders headers = req.getMimeHeaders();
    for (int i = 0; i < headers.size(); i++) {
      buf.append("\n\t").append(headers.getName(i)).append(": ").append(headers.getValue(i));
    }
    log.debug(buf);
  }
  
  protected void convertURI(MessageBytes uri, Request request)
    throws Exception
  {
    ByteChunk bc = uri.getByteChunk();
    int length = bc.getLength();
    CharChunk cc = uri.getCharChunk();
    cc.allocate(length, -1);
    
    String enc = this.connector.getURIEncoding();
    if (enc != null)
    {
      B2CConverter conv = request.getURIConverter();
      try
      {
        if (conv == null)
        {
          conv = new B2CConverter(enc, true);
          request.setURIConverter(conv);
        }
        else
        {
          conv.recycle();
        }
      }
      catch (IOException e)
      {
        log.error("Invalid URI encoding; using HTTP default");
        this.connector.setURIEncoding(null);
      }
      if (conv != null) {
        try
        {
          conv.convert(bc, cc, true);
          uri.setChars(cc.getBuffer(), cc.getStart(), cc.getLength());
          return;
        }
        catch (IOException ioe)
        {
          request.getResponse().sendError(400);
        }
      }
    }
    byte[] bbuf = bc.getBuffer();
    char[] cbuf = cc.getBuffer();
    int start = bc.getStart();
    for (int i = 0; i < length; i++) {
      cbuf[i] = ((char)(bbuf[(i + start)] & 0xFF));
    }
    uri.setChars(cbuf, 0, length);
  }
  
  protected void convertMB(MessageBytes mb)
  {
    if (mb.getType() != 2) {
      return;
    }
    ByteChunk bc = mb.getByteChunk();
    CharChunk cc = mb.getCharChunk();
    int length = bc.getLength();
    cc.allocate(length, -1);
    
    byte[] bbuf = bc.getBuffer();
    char[] cbuf = cc.getBuffer();
    int start = bc.getStart();
    for (int i = 0; i < length; i++) {
      cbuf[i] = ((char)(bbuf[(i + start)] & 0xFF));
    }
    mb.setChars(cbuf, 0, length);
  }
  
  public static boolean normalize(MessageBytes uriMB)
  {
    ByteChunk uriBC = uriMB.getByteChunk();
    byte[] b = uriBC.getBytes();
    int start = uriBC.getStart();
    int end = uriBC.getEnd();
    if (start == end) {
      return false;
    }
    if ((end - start == 1) && (b[start] == 42)) {
      return true;
    }
    int pos = 0;
    int index = 0;
    for (pos = start; pos < end; pos++)
    {
      if (b[pos] == 92) {
        if (ALLOW_BACKSLASH) {
          b[pos] = 47;
        } else {
          return false;
        }
      }
      if (b[pos] == 0) {
        return false;
      }
    }
    if (b[start] != 47) {
      return false;
    }
    for (pos = start; pos < end - 1; pos++) {
      if (b[pos] == 47) {
        while ((pos + 1 < end) && (b[(pos + 1)] == 47))
        {
          copyBytes(b, pos, pos + 1, end - pos - 1);
          end--;
        }
      }
    }
    if ((end - start >= 2) && (b[(end - 1)] == 46) && (
      (b[(end - 2)] == 47) || ((b[(end - 2)] == 46) && (b[(end - 3)] == 47))))
    {
      b[end] = 47;
      end++;
    }
    uriBC.setEnd(end);
    
    index = 0;
    for (;;)
    {
      index = uriBC.indexOf("/./", 0, 3, index);
      if (index < 0) {
        break;
      }
      copyBytes(b, start + index, start + index + 2, end - start - index - 2);
      
      end -= 2;
      uriBC.setEnd(end);
    }
    index = 0;
    for (;;)
    {
      index = uriBC.indexOf("/../", 0, 4, index);
      if (index < 0) {
        break;
      }
      if (index == 0) {
        return false;
      }
      int index2 = -1;
      for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos--) {
        if (b[pos] == 47) {
          index2 = pos;
        }
      }
      copyBytes(b, start + index2, start + index + 3, end - start - index - 3);
      
      end = end + index2 - index - 3;
      uriBC.setEnd(end);
      index = index2;
    }
    return true;
  }
  
  public static boolean checkNormalize(MessageBytes uriMB)
  {
    CharChunk uriCC = uriMB.getCharChunk();
    char[] c = uriCC.getChars();
    int start = uriCC.getStart();
    int end = uriCC.getEnd();
    
    int pos = 0;
    for (pos = start; pos < end; pos++)
    {
      if (c[pos] == '\\') {
        return false;
      }
      if (c[pos] == 0) {
        return false;
      }
    }
    for (pos = start; pos < end - 1; pos++) {
      if ((c[pos] == '/') && 
        (c[(pos + 1)] == '/')) {
        return false;
      }
    }
    if ((end - start >= 2) && (c[(end - 1)] == '.') && (
      (c[(end - 2)] == '/') || ((c[(end - 2)] == '.') && (c[(end - 3)] == '/')))) {
      return false;
    }
    if (uriCC.indexOf("/./", 0, 3, 0) >= 0) {
      return false;
    }
    if (uriCC.indexOf("/../", 0, 4, 0) >= 0) {
      return false;
    }
    return true;
  }
  
  protected static void copyBytes(byte[] b, int dest, int src, int len)
  {
    for (int pos = 0; pos < len; pos++) {
      b[(pos + dest)] = b[(pos + src)];
    }
  }
  
  private static class RecycleRequiredException
    extends Exception
  {
    private static final long serialVersionUID = 1L;
    
    private RecycleRequiredException() {}
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CoyoteAdapter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */