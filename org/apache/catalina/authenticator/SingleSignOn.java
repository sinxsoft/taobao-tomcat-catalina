package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

public class SingleSignOn
  extends ValveBase
  implements SessionListener
{
  public SingleSignOn()
  {
    super(true);
  }
  
  protected Map<String, SingleSignOnEntry> cache = new HashMap();
  protected static final String info = "org.apache.catalina.authenticator.SingleSignOn";
  private boolean requireReauthentication = false;
  protected Map<Session, String> reverse = new HashMap();
  @Deprecated
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.authenticator");
  private String cookieDomain;
  
  public String getCookieDomain()
  {
    return this.cookieDomain;
  }
  
  public void setCookieDomain(String cookieDomain)
  {
    if ((cookieDomain != null) && (cookieDomain.trim().length() == 0)) {
      this.cookieDomain = null;
    } else {
      this.cookieDomain = cookieDomain;
    }
  }
  
  public boolean getRequireReauthentication()
  {
    return this.requireReauthentication;
  }
  
  public void setRequireReauthentication(boolean required)
  {
    this.requireReauthentication = required;
  }
  
  public void sessionEvent(SessionEvent event)
  {
    if (!getState().isAvailable()) {
      return;
    }
    if ((!"destroySession".equals(event.getType())) && (!"passivateSession".equals(event.getType()))) {
      return;
    }
    Session session = event.getSession();
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Process session destroyed on " + session);
    }
    String ssoId = null;
    synchronized (this.reverse)
    {
      ssoId = (String)this.reverse.get(session);
    }
    if (ssoId == null) {
      return;
    }
    if (((session.getMaxInactiveInterval() > 0) && (System.currentTimeMillis() - session.getThisAccessedTimeInternal() >= session.getMaxInactiveInterval() * 1000)) || ("passivateSession".equals(event.getType())) || (!session.getManager().getContainer().getState().isAvailable())) {
      removeSession(ssoId, session);
    } else {
      deregister(ssoId);
    }
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.SingleSignOn";
  }
  
  public void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    request.removeNote("org.apache.catalina.request.SSOID");
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Process request for '" + request.getRequestURI() + "'");
    }
    if (request.getUserPrincipal() != null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug(" Principal '" + request.getUserPrincipal().getName() + "' has already been authenticated");
      }
      getNext().invoke(request, response);
      return;
    }
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug(" Checking for SSO cookie");
    }
    Cookie cookie = null;
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      cookies = new Cookie[0];
    }
    for (int i = 0; i < cookies.length; i++) {
      if (Constants.SINGLE_SIGN_ON_COOKIE.equals(cookies[i].getName()))
      {
        cookie = cookies[i];
        break;
      }
    }
    if (cookie == null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug(" SSO cookie is not present");
      }
      getNext().invoke(request, response);
      return;
    }
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug(" Checking for cached principal for " + cookie.getValue());
    }
    SingleSignOnEntry entry = lookup(cookie.getValue());
    if (entry != null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug(" Found cached principal '" + (entry.getPrincipal() != null ? entry.getPrincipal().getName() : "") + "' with auth type '" + entry.getAuthType() + "'");
      }
      request.setNote("org.apache.catalina.request.SSOID", cookie.getValue());
      if (!getRequireReauthentication())
      {
        request.setAuthType(entry.getAuthType());
        request.setUserPrincipal(entry.getPrincipal());
      }
    }
    else
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug(" No cached principal found, erasing SSO cookie");
      }
      cookie.setValue("REMOVE");
      
      cookie.setMaxAge(0);
      
      cookie.setPath("/");
      String domain = getCookieDomain();
      if (domain != null) {
        cookie.setDomain(domain);
      }
      cookie.setSecure(request.isSecure());
      if ((request.getServletContext().getSessionCookieConfig().isHttpOnly()) || (request.getContext().getUseHttpOnly())) {
        cookie.setHttpOnly(true);
      }
      response.addCookie(cookie);
    }
    getNext().invoke(request, response);
  }
  
  protected void associate(String ssoId, Session session)
  {
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Associate sso id " + ssoId + " with session " + session);
    }
    SingleSignOnEntry sso = lookup(ssoId);
    if (sso != null) {
      sso.addSession(this, session);
    }
    synchronized (this.reverse)
    {
      this.reverse.put(session, ssoId);
    }
  }
  
  protected void deregister(String ssoId, Session session)
  {
    synchronized (this.reverse)
    {
      this.reverse.remove(session);
    }
    SingleSignOnEntry sso = lookup(ssoId);
    if (sso == null) {
      return;
    }
    sso.removeSession(session);
    
    Session[] sessions = sso.findSessions();
    if ((sessions == null) || (sessions.length == 0)) {
      synchronized (this.cache)
      {
        this.cache.remove(ssoId);
      }
    }
  }
  
  protected void deregister(String ssoId)
  {
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Deregistering sso id '" + ssoId + "'");
    }
    SingleSignOnEntry sso = null;
    synchronized (this.cache)
    {
      sso = (SingleSignOnEntry)this.cache.remove(ssoId);
    }
    if (sso == null) {
      return;
    }
    Session[] sessions = sso.findSessions();
    for (int i = 0; i < sessions.length; i++)
    {
      if (this.containerLog.isTraceEnabled()) {
        this.containerLog.trace(" Invalidating session " + sessions[i]);
      }
      synchronized (this.reverse)
      {
        this.reverse.remove(sessions[i]);
      }
      sessions[i].expire();
    }
  }
  
  protected boolean reauthenticate(String ssoId, Realm realm, Request request)
  {
    if ((ssoId == null) || (realm == null)) {
      return false;
    }
    boolean reauthenticated = false;
    
    SingleSignOnEntry entry = lookup(ssoId);
    if ((entry != null) && (entry.getCanReauthenticate()))
    {
      String username = entry.getUsername();
      if (username != null)
      {
        Principal reauthPrincipal = realm.authenticate(username, entry.getPassword());
        if (reauthPrincipal != null)
        {
          reauthenticated = true;
          
          request.setAuthType(entry.getAuthType());
          request.setUserPrincipal(reauthPrincipal);
        }
      }
    }
    return reauthenticated;
  }
  
  protected void register(String ssoId, Principal principal, String authType, String username, String password)
  {
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Registering sso id '" + ssoId + "' for user '" + (principal != null ? principal.getName() : "") + "' with auth type '" + authType + "'");
    }
    synchronized (this.cache)
    {
      this.cache.put(ssoId, new SingleSignOnEntry(principal, authType, username, password));
    }
  }
  
  protected void update(String ssoId, Principal principal, String authType, String username, String password)
  {
    SingleSignOnEntry sso = lookup(ssoId);
    if ((sso != null) && (!sso.getCanReauthenticate()))
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("Update sso id " + ssoId + " to auth type " + authType);
      }
      synchronized (sso)
      {
        sso.updateCredentials(principal, authType, username, password);
      }
    }
  }
  
  protected SingleSignOnEntry lookup(String ssoId)
  {
	  synchronized (cache) {
          return cache.get(ssoId);
      }
  }
  
  protected void removeSession(String ssoId, Session session)
  {
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("Removing session " + session.toString() + " from sso id " + ssoId);
    }
    SingleSignOnEntry entry = lookup(ssoId);
    if (entry == null) {
      return;
    }
    entry.removeSession(session);
    synchronized (this.reverse)
    {
      this.reverse.remove(session);
    }
    if (entry.findSessions().length == 0) {
      deregister(ssoId);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\SingleSignOn.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */