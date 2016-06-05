package org.apache.catalina.authenticator;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.coyote.ActionCode;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.Parameters;
import org.apache.tomcat.util.res.StringManager;

public class FormAuthenticator
  extends AuthenticatorBase
{
  private static final Log log = LogFactory.getLog(FormAuthenticator.class);
  protected static final String info = "org.apache.catalina.authenticator.FormAuthenticator/1.0";
  protected String characterEncoding = null;
  protected String landingPage = null;
  
  public FormAuthenticator() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.FormAuthenticator/1.0";
  }
  
  public String getCharacterEncoding()
  {
    return this.characterEncoding;
  }
  
  public void setCharacterEncoding(String encoding)
  {
    this.characterEncoding = encoding;
  }
  
  public String getLandingPage()
  {
    return this.landingPage;
  }
  
  public void setLandingPage(String landingPage)
  {
    this.landingPage = landingPage;
  }
  
  public boolean authenticate(org.apache.catalina.connector.Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    Session session = null;
    
    Principal principal = request.getUserPrincipal();
    String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
    if (principal != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("Already authenticated '" + principal.getName() + "'");
      }
      if (ssoId != null) {
        associate(ssoId, request.getSessionInternal(true));
      }
      return true;
    }
    if (ssoId != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("SSO Id " + ssoId + " set; attempting " + "reauthentication");
      }
      if (reauthenticateFromSSO(ssoId, request)) {
        return true;
      }
    }
    if (!this.cache)
    {
      session = request.getSessionInternal(true);
      if (log.isDebugEnabled()) {
        log.debug("Checking for reauthenticate in session " + session);
      }
      String username = (String)session.getNote("org.apache.catalina.session.USERNAME");
      
      String password = (String)session.getNote("org.apache.catalina.session.PASSWORD");
      if ((username != null) && (password != null))
      {
        if (log.isDebugEnabled()) {
          log.debug("Reauthenticating username '" + username + "'");
        }
        principal = this.context.getRealm().authenticate(username, password);
        if (principal != null)
        {
          session.setNote("org.apache.catalina.authenticator.PRINCIPAL", principal);
          if (!matchRequest(request))
          {
            register(request, response, principal, "FORM", username, password);
            
            return true;
          }
        }
        if (log.isDebugEnabled()) {
          log.debug("Reauthentication failed, proceed normally");
        }
      }
    }
    if (matchRequest(request))
    {
      session = request.getSessionInternal(true);
      if (log.isDebugEnabled()) {
        log.debug("Restore request from session '" + session.getIdInternal() + "'");
      }
      principal = (Principal)session.getNote("org.apache.catalina.authenticator.PRINCIPAL");
      
      register(request, response, principal, "FORM", (String)session.getNote("org.apache.catalina.session.USERNAME"), (String)session.getNote("org.apache.catalina.session.PASSWORD"));
      if (this.cache)
      {
        session.removeNote("org.apache.catalina.session.USERNAME");
        session.removeNote("org.apache.catalina.session.PASSWORD");
      }
      if (restoreRequest(request, session))
      {
        if (log.isDebugEnabled()) {
          log.debug("Proceed to restored request");
        }
        return true;
      }
      if (log.isDebugEnabled()) {
        log.debug("Restore of original request failed");
      }
      response.sendError(400);
      return false;
    }
    MessageBytes uriMB = MessageBytes.newInstance();
    CharChunk uriCC = uriMB.getCharChunk();
    uriCC.setLimit(-1);
    String contextPath = request.getContextPath();
    String requestURI = request.getDecodedRequestURI();
    
    boolean loginAction = (requestURI.startsWith(contextPath)) && (requestURI.endsWith("/j_security_check"));
    if (!loginAction)
    {
      session = request.getSessionInternal(true);
      if (log.isDebugEnabled()) {
        log.debug("Save request in session '" + session.getIdInternal() + "'");
      }
      try
      {
        saveRequest(request, session);
      }
      catch (IOException ioe)
      {
        log.debug("Request body too big to save during authentication");
        response.sendError(403, sm.getString("authenticator.requestBodyTooBig"));
        
        return false;
      }
      forwardToLoginPage(request, response, config);
      return false;
    }
    request.getResponse().sendAcknowledgement();
    Realm realm = this.context.getRealm();
    if (this.characterEncoding != null) {
      request.setCharacterEncoding(this.characterEncoding);
    }
    String username = request.getParameter("j_username");
    String password = request.getParameter("j_password");
    if (log.isDebugEnabled()) {
      log.debug("Authenticating username '" + username + "'");
    }
    principal = realm.authenticate(username, password);
    if (principal == null)
    {
      forwardToErrorPage(request, response, config);
      return false;
    }
    if (log.isDebugEnabled()) {
      log.debug("Authentication of '" + username + "' was successful");
    }
    if (session == null) {
      session = request.getSessionInternal(false);
    }
    if (session == null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("User took so long to log on the session expired");
      }
      if (this.landingPage == null)
      {
        response.sendError(408, sm.getString("authenticator.sessionExpired"));
      }
      else
      {
        String uri = request.getContextPath() + this.landingPage;
        SavedRequest saved = new SavedRequest();
        saved.setMethod("GET");
        saved.setRequestURI(uri);
        saved.setDecodedRequestURI(uri);
        request.getSessionInternal(true).setNote("org.apache.catalina.authenticator.REQUEST", saved);
        
        response.sendRedirect(response.encodeRedirectURL(uri));
      }
      return false;
    }
    session.setNote("org.apache.catalina.authenticator.PRINCIPAL", principal);
    
    session.setNote("org.apache.catalina.session.USERNAME", username);
    session.setNote("org.apache.catalina.session.PASSWORD", password);
    
    requestURI = savedRequestURL(session);
    if (log.isDebugEnabled()) {
      log.debug("Redirecting to original '" + requestURI + "'");
    }
    if (requestURI == null)
    {
      if (this.landingPage == null)
      {
        response.sendError(400, sm.getString("authenticator.formlogin"));
      }
      else
      {
        String uri = request.getContextPath() + this.landingPage;
        SavedRequest saved = new SavedRequest();
        saved.setMethod("GET");
        saved.setRequestURI(uri);
        saved.setDecodedRequestURI(uri);
        session.setNote("org.apache.catalina.authenticator.REQUEST", saved);
        response.sendRedirect(response.encodeRedirectURL(uri));
      }
    }
    else {
      response.sendRedirect(response.encodeRedirectURL(requestURI));
    }
    return false;
  }
  
  protected String getAuthMethod()
  {
    return "FORM";
  }
  
  protected void forwardToLoginPage(org.apache.catalina.connector.Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("formAuthenticator.forwardLogin", new Object[] { request.getRequestURI(), request.getMethod(), config.getLoginPage(), this.context.getName() }));
    }
    String loginPage = config.getLoginPage();
    if ((loginPage == null) || (loginPage.length() == 0))
    {
      String msg = sm.getString("formAuthenticator.noLoginPage", new Object[] { this.context.getName() });
      
      log.warn(msg);
      response.sendError(500, msg);
      
      return;
    }
    if (getChangeSessionIdOnAuthentication())
    {
      Session session = request.getSessionInternal(false);
      if (session != null)
      {
        Manager manager = request.getContext().getManager();
        manager.changeSessionId(session);
        request.changeSessionId(session.getId());
      }
    }
    String oldMethod = request.getMethod();
    request.getCoyoteRequest().method().setString("GET");
    
    RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(loginPage);
    try
    {
      if (this.context.fireRequestInitEvent(request))
      {
        disp.forward(request.getRequest(), response);
        this.context.fireRequestDestroyEvent(request);
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      String msg = sm.getString("formAuthenticator.forwardLoginFail");
      log.warn(msg, t);
      request.setAttribute("javax.servlet.error.exception", t);
      response.sendError(500, msg);
    }
    finally
    {
      request.getCoyoteRequest().method().setString(oldMethod);
    }
  }
  
  protected void forwardToErrorPage(org.apache.catalina.connector.Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    String errorPage = config.getErrorPage();
    if ((errorPage == null) || (errorPage.length() == 0))
    {
      String msg = sm.getString("formAuthenticator.noErrorPage", new Object[] { this.context.getName() });
      
      log.warn(msg);
      response.sendError(500, msg);
      
      return;
    }
    RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(config.getErrorPage());
    try
    {
      if (this.context.fireRequestInitEvent(request))
      {
        disp.forward(request.getRequest(), response);
        this.context.fireRequestDestroyEvent(request);
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      String msg = sm.getString("formAuthenticator.forwardErrorFail");
      log.warn(msg, t);
      request.setAttribute("javax.servlet.error.exception", t);
      response.sendError(500, msg);
    }
  }
  
  protected boolean matchRequest(org.apache.catalina.connector.Request request)
  {
    Session session = request.getSessionInternal(false);
    if (session == null) {
      return false;
    }
    SavedRequest sreq = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
    if (sreq == null) {
      return false;
    }
    if (session.getNote("org.apache.catalina.authenticator.PRINCIPAL") == null) {
      return false;
    }
    String decodedRequestURI = request.getDecodedRequestURI();
    if (decodedRequestURI == null) {
      return false;
    }
    return decodedRequestURI.equals(sreq.getDecodedRequestURI());
  }
  
  protected boolean restoreRequest(org.apache.catalina.connector.Request request, Session session)
    throws IOException
  {
    SavedRequest saved = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
    
    session.removeNote("org.apache.catalina.authenticator.REQUEST");
    session.removeNote("org.apache.catalina.authenticator.PRINCIPAL");
    if (saved == null) {
      return false;
    }
    byte[] buffer = new byte['က'];
    InputStream is = request.createInputStream();
    while (is.read(buffer) >= 0) {}
    request.clearCookies();
    Iterator<Cookie> cookies = saved.getCookies();
    while (cookies.hasNext()) {
      request.addCookie((Cookie)cookies.next());
    }
    String method = saved.getMethod();
    MimeHeaders rmh = request.getCoyoteRequest().getMimeHeaders();
    rmh.recycle();
    boolean cachable = ("GET".equalsIgnoreCase(method)) || ("HEAD".equalsIgnoreCase(method));
    
    Iterator<String> names = saved.getHeaderNames();
    while (names.hasNext())
    {
      String name = (String)names.next();
      if ((!"If-Modified-Since".equalsIgnoreCase(name)) && ((!cachable) || (!"If-None-Match".equalsIgnoreCase(name))))
      {
        Iterator<String> values = saved.getHeaderValues(name);
        while (values.hasNext()) {
          rmh.addValue(name).setString((String)values.next());
        }
      }
    }
    request.clearLocales();
    Iterator<Locale> locales = saved.getLocales();
    while (locales.hasNext()) {
      request.addLocale((Locale)locales.next());
    }
    request.getCoyoteRequest().getParameters().recycle();
    request.getCoyoteRequest().getParameters().setQueryStringEncoding(request.getConnector().getURIEncoding());
    
    ByteChunk body = saved.getBody();
    if (body != null)
    {
      request.getCoyoteRequest().action(ActionCode.REQ_SET_BODY_REPLAY, body);
      
      MessageBytes contentType = MessageBytes.newInstance();
      
      String savedContentType = saved.getContentType();
      if ((savedContentType == null) && ("POST".equalsIgnoreCase(method))) {
        savedContentType = "application/x-www-form-urlencoded";
      }
      contentType.setString(savedContentType);
      request.getCoyoteRequest().setContentType(contentType);
    }
    request.getCoyoteRequest().method().setString(method);
    
    request.getCoyoteRequest().queryString().setString(saved.getQueryString());
    
    request.getCoyoteRequest().requestURI().setString(saved.getRequestURI());
    
    return true;
  }
  
  protected void saveRequest(org.apache.catalina.connector.Request request, Session session)
    throws IOException
  {
    SavedRequest saved = new SavedRequest();
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (int i = 0; i < cookies.length; i++) {
        saved.addCookie(cookies[i]);
      }
    }
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements())
    {
      String name = (String)names.nextElement();
      Enumeration<String> values = request.getHeaders(name);
      while (values.hasMoreElements())
      {
        String value = (String)values.nextElement();
        saved.addHeader(name, value);
      }
    }
    Enumeration<Locale> locales = request.getLocales();
    while (locales.hasMoreElements())
    {
      Locale locale = (Locale)locales.nextElement();
      saved.addLocale(locale);
    }
    request.getResponse().sendAcknowledgement();
    
    ByteChunk body = new ByteChunk();
    body.setLimit(request.getConnector().getMaxSavePostSize());
    
    byte[] buffer = new byte['က'];
    
    InputStream is = request.getInputStream();
    int bytesRead;
    while ((bytesRead = is.read(buffer)) >= 0) {
      body.append(buffer, 0, bytesRead);
    }
    if (body.getLength() > 0)
    {
      saved.setContentType(request.getContentType());
      saved.setBody(body);
    }
    saved.setMethod(request.getMethod());
    saved.setQueryString(request.getQueryString());
    saved.setRequestURI(request.getRequestURI());
    saved.setDecodedRequestURI(request.getDecodedRequestURI());
    
    session.setNote("org.apache.catalina.authenticator.REQUEST", saved);
  }
  
  protected String savedRequestURL(Session session)
  {
    SavedRequest saved = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
    if (saved == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(saved.getRequestURI());
    if (saved.getQueryString() != null)
    {
      sb.append('?');
      sb.append(saved.getQueryString());
    }
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\FormAuthenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */