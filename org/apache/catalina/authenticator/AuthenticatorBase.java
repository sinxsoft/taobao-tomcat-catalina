package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.ActionCode;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.apache.tomcat.util.res.StringManager;

public abstract class AuthenticatorBase
  extends ValveBase
  implements Authenticator
{
  private static final Log log = LogFactory.getLog(AuthenticatorBase.class);
  protected static final String AUTH_HEADER_NAME = "WWW-Authenticate";
  protected static final String REALM_NAME = "Authentication required";
  
  public AuthenticatorBase()
  {
    super(true);
  }
  
  protected boolean alwaysUseSession = false;
  protected boolean cache = true;
  protected boolean changeSessionIdOnAuthentication = true;
  protected Context context = null;
  protected static final String info = "org.apache.catalina.authenticator.AuthenticatorBase/1.0";
  protected boolean disableProxyCaching = true;
  protected boolean securePagesWithPragma = false;
  protected String secureRandomClass = null;
  protected String secureRandomAlgorithm = "SHA1PRNG";
  protected String secureRandomProvider = null;
  protected SessionIdGeneratorBase sessionIdGenerator = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.authenticator");
  protected SingleSignOn sso = null;
  private static final String DATE_ONE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).format(new Date(1L));
  
  public boolean getAlwaysUseSession()
  {
    return this.alwaysUseSession;
  }
  
  public void setAlwaysUseSession(boolean alwaysUseSession)
  {
    this.alwaysUseSession = alwaysUseSession;
  }
  
  public boolean getCache()
  {
    return this.cache;
  }
  
  public void setCache(boolean cache)
  {
    this.cache = cache;
  }
  
  public Container getContainer()
  {
    return this.context;
  }
  
  public void setContainer(Container container)
  {
    if ((container != null) && (!(container instanceof Context))) {
      throw new IllegalArgumentException(sm.getString("authenticator.notContext"));
    }
    super.setContainer(container);
    this.context = ((Context)container);
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.AuthenticatorBase/1.0";
  }
  
  public boolean getDisableProxyCaching()
  {
    return this.disableProxyCaching;
  }
  
  public void setDisableProxyCaching(boolean nocache)
  {
    this.disableProxyCaching = nocache;
  }
  
  public boolean getSecurePagesWithPragma()
  {
    return this.securePagesWithPragma;
  }
  
  public void setSecurePagesWithPragma(boolean securePagesWithPragma)
  {
    this.securePagesWithPragma = securePagesWithPragma;
  }
  
  public boolean getChangeSessionIdOnAuthentication()
  {
    return this.changeSessionIdOnAuthentication;
  }
  
  public void setChangeSessionIdOnAuthentication(boolean changeSessionIdOnAuthentication)
  {
    this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
  }
  
  public String getSecureRandomClass()
  {
    return this.secureRandomClass;
  }
  
  public void setSecureRandomClass(String secureRandomClass)
  {
    this.secureRandomClass = secureRandomClass;
  }
  
  public String getSecureRandomAlgorithm()
  {
    return this.secureRandomAlgorithm;
  }
  
  public void setSecureRandomAlgorithm(String secureRandomAlgorithm)
  {
    this.secureRandomAlgorithm = secureRandomAlgorithm;
  }
  
  public String getSecureRandomProvider()
  {
    return this.secureRandomProvider;
  }
  
  public void setSecureRandomProvider(String secureRandomProvider)
  {
    this.secureRandomProvider = secureRandomProvider;
  }
  
  public void invoke(org.apache.catalina.connector.Request request, Response response)
    throws IOException, ServletException
  {
    if (log.isDebugEnabled()) {
      log.debug("Security checking request " + request.getMethod() + " " + request.getRequestURI());
    }
    LoginConfig config = this.context.getLoginConfig();
    if (this.cache)
    {
      Principal principal = request.getUserPrincipal();
      if (principal == null)
      {
        Session session = request.getSessionInternal(false);
        if (session != null)
        {
          principal = session.getPrincipal();
          if (principal != null)
          {
            if (log.isDebugEnabled()) {
              log.debug("We have cached auth type " + session.getAuthType() + " for principal " + session.getPrincipal());
            }
            request.setAuthType(session.getAuthType());
            request.setUserPrincipal(principal);
          }
        }
      }
    }
    String contextPath = this.context.getPath();
    String requestURI = request.getDecodedRequestURI();
    if ((requestURI.startsWith(contextPath)) && (requestURI.endsWith("/j_security_check"))) {
      if (!authenticate(request, response, config))
      {
        if (log.isDebugEnabled()) {
          log.debug(" Failed authenticate() test ??" + requestURI);
        }
        return;
      }
    }
    Session session = request.getSessionInternal(false);
    if (session != null)
    {
      SavedRequest savedRequest = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
      if (savedRequest != null)
      {
        String decodedRequestURI = request.getDecodedRequestURI();
        if ((decodedRequestURI != null) && (decodedRequestURI.equals(savedRequest.getDecodedRequestURI()))) {
          if (!authenticate(request, response))
          {
            if (log.isDebugEnabled()) {
              log.debug(" Failed authenticate() test");
            }
            return;
          }
        }
      }
    }
    Wrapper wrapper = (Wrapper)request.getMappingData().wrapper;
    if (wrapper != null) {
      wrapper.servletSecurityAnnotationScan();
    }
    Realm realm = this.context.getRealm();
    
    SecurityConstraint[] constraints = realm.findSecurityConstraints(request, this.context);
    if ((constraints == null) && (!this.context.getPreemptiveAuthentication()))
    {
      if (log.isDebugEnabled()) {
        log.debug(" Not subject to any constraint");
      }
      getNext().invoke(request, response);
      return;
    }
    if ((constraints != null) && (this.disableProxyCaching) && (!"POST".equalsIgnoreCase(request.getMethod())))
    {
      if (this.securePagesWithPragma)
      {
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
      }
      else
      {
        response.setHeader("Cache-Control", "private");
      }
      response.setHeader("Expires", DATE_ONE);
    }
    if (constraints != null)
    {
      if (log.isDebugEnabled()) {
        log.debug(" Calling hasUserDataPermission()");
      }
      if (!realm.hasUserDataPermission(request, response, constraints))
      {
        if (log.isDebugEnabled()) {
          log.debug(" Failed hasUserDataPermission() test");
        }
        return;
      }
    }
    boolean authRequired;
    
    if (constraints == null)
    {
      authRequired = false;
    }
    else
    {
      authRequired = true;
      for (int i = 0; (i < constraints.length) && (authRequired); i++) {
        if (!constraints[i].getAuthConstraint())
        {
          authRequired = false;
        }
        else if (!constraints[i].getAllRoles())
        {
          String[] roles = constraints[i].findAuthRoles();
          if ((roles == null) || (roles.length == 0)) {
            authRequired = false;
          }
        }
      }
    }
    if ((!authRequired) && (this.context.getPreemptiveAuthentication())) {
      authRequired = request.getCoyoteRequest().getMimeHeaders().getValue("authorization") != null;
    }
    if ((!authRequired) && (this.context.getPreemptiveAuthentication()) && ("CLIENT_CERT".equals(getAuthMethod())))
    {
      X509Certificate[] certs = getRequestCertificates(request);
      authRequired = (certs != null) && (certs.length > 0);
    }
    if (authRequired)
    {
      if (log.isDebugEnabled()) {
        log.debug(" Calling authenticate()");
      }
      if (!authenticate(request, response, config))
      {
        if (log.isDebugEnabled()) {
          log.debug(" Failed authenticate() test");
        }
        return;
      }
    }
    if (constraints != null)
    {
      if (log.isDebugEnabled()) {
        log.debug(" Calling accessControl()");
      }
      if (!realm.hasResourcePermission(request, response, constraints, this.context))
      {
        if (log.isDebugEnabled()) {
          log.debug(" Failed accessControl() test");
        }
        return;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(" Successfully passed all security constraints");
    }
    getNext().invoke(request, response);
  }
  
  protected X509Certificate[] getRequestCertificates(org.apache.catalina.connector.Request request)
    throws IllegalStateException
  {
    X509Certificate[] certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
    if ((certs == null) || (certs.length < 1)) {
      try
      {
        request.getCoyoteRequest().action(ActionCode.REQ_SSL_CERTIFICATE, null);
        certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
      }
      catch (IllegalStateException ise) {}
    }
    return certs;
  }
  
  protected void associate(String ssoId, Session session)
  {
    if (this.sso == null) {
      return;
    }
    this.sso.associate(ssoId, session);
  }
  
  public boolean authenticate(org.apache.catalina.connector.Request request, HttpServletResponse response)
    throws IOException
  {
    if ((this.context == null) || (this.context.getLoginConfig() == null)) {
      return true;
    }
    return authenticate(request, response, this.context.getLoginConfig());
  }
  
  public abstract boolean authenticate(org.apache.catalina.connector.Request paramRequest, HttpServletResponse paramHttpServletResponse, LoginConfig paramLoginConfig)
    throws IOException;
  
  protected boolean reauthenticateFromSSO(String ssoId, org.apache.catalina.connector.Request request)
  {
    if ((this.sso == null) || (ssoId == null)) {
      return false;
    }
    boolean reauthenticated = false;
    
    Container parent = getContainer();
    if (parent != null)
    {
      Realm realm = parent.getRealm();
      if (realm != null) {
        reauthenticated = this.sso.reauthenticate(ssoId, realm, request);
      }
    }
    if (reauthenticated)
    {
      associate(ssoId, request.getSessionInternal(true));
      if (log.isDebugEnabled()) {
        log.debug(" Reauthenticated cached principal '" + request.getUserPrincipal().getName() + "' with auth type '" + request.getAuthType() + "'");
      }
    }
    return reauthenticated;
  }
  
  public void register(org.apache.catalina.connector.Request request, HttpServletResponse response, Principal principal, String authType, String username, String password)
  {
    if (log.isDebugEnabled())
    {
      String name = principal == null ? "none" : principal.getName();
      log.debug("Authenticated '" + name + "' with type '" + authType + "'");
    }
    request.setAuthType(authType);
    request.setUserPrincipal(principal);
    
    Session session = request.getSessionInternal(false);
    if (session != null)
    {
      if (this.changeSessionIdOnAuthentication)
      {
        Manager manager = request.getContext().getManager();
        manager.changeSessionId(session);
        request.changeSessionId(session.getId());
      }
    }
    else if (this.alwaysUseSession) {
      session = request.getSessionInternal(true);
    }
    if ((this.cache) && 
      (session != null))
    {
      session.setAuthType(authType);
      session.setPrincipal(principal);
      if (username != null) {
        session.setNote("org.apache.catalina.session.USERNAME", username);
      } else {
        session.removeNote("org.apache.catalina.session.USERNAME");
      }
      if (password != null) {
        session.setNote("org.apache.catalina.session.PASSWORD", password);
      } else {
        session.removeNote("org.apache.catalina.session.PASSWORD");
      }
    }
    if (this.sso == null) {
      return;
    }
    String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
    if (ssoId == null)
    {
      ssoId = this.sessionIdGenerator.generateSessionId();
      Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, ssoId);
      cookie.setMaxAge(-1);
      cookie.setPath("/");
      
      cookie.setSecure(request.isSecure());
      
      String ssoDomain = this.sso.getCookieDomain();
      if (ssoDomain != null) {
        cookie.setDomain(ssoDomain);
      }
      if ((request.getServletContext().getSessionCookieConfig().isHttpOnly()) || (request.getContext().getUseHttpOnly())) {
        cookie.setHttpOnly(true);
      }
      response.addCookie(cookie);
      
      this.sso.register(ssoId, principal, authType, username, password);
      request.setNote("org.apache.catalina.request.SSOID", ssoId);
    }
    else
    {
      if (principal == null)
      {
        this.sso.deregister(ssoId);
        request.removeNote("org.apache.catalina.request.SSOID");
        return;
      }
      this.sso.update(ssoId, principal, authType, username, password);
    }
    if (session == null) {
      session = request.getSessionInternal(true);
    }
    this.sso.associate(ssoId, session);
  }
  
  public void login(String username, String password, org.apache.catalina.connector.Request request)
    throws ServletException
  {
    Principal principal = doLogin(request, username, password);
    register(request, request.getResponse(), principal, getAuthMethod(), username, password);
  }
  
  protected abstract String getAuthMethod();
  
  protected Principal doLogin(org.apache.catalina.connector.Request request, String username, String password)
    throws ServletException
  {
    Principal p = this.context.getRealm().authenticate(username, password);
    if (p == null) {
      throw new ServletException(sm.getString("authenticator.loginFail"));
    }
    return p;
  }
  
  public void logout(org.apache.catalina.connector.Request request)
    throws ServletException
  {
    register(request, request.getResponse(), null, null, null, null);
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    Container parent = this.context.getParent();
    while ((this.sso == null) && (parent != null))
    {
      Valve[] valves = parent.getPipeline().getValves();
      for (int i = 0; i < valves.length; i++) {
        if ((valves[i] instanceof SingleSignOn))
        {
          this.sso = ((SingleSignOn)valves[i]);
          break;
        }
      }
      if (this.sso == null) {
        parent = parent.getParent();
      }
    }
    if (log.isDebugEnabled()) {
      if (this.sso != null) {
        log.debug("Found SingleSignOn Valve at " + this.sso);
      } else {
        log.debug("No SingleSignOn Valve is present");
      }
    }
    this.sessionIdGenerator = new StandardSessionIdGenerator();
    this.sessionIdGenerator.setSecureRandomAlgorithm(getSecureRandomAlgorithm());
    this.sessionIdGenerator.setSecureRandomClass(getSecureRandomClass());
    this.sessionIdGenerator.setSecureRandomProvider(getSecureRandomProvider());
    
    super.startInternal();
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    super.stopInternal();
    
    this.sso = null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\AuthenticatorBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */