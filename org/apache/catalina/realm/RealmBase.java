package org.apache.catalina.realm;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Realm;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.catalina.util.SessionConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.MD5Encoder;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

public abstract class RealmBase
  extends LifecycleMBeanBase
  implements Realm
{
  private static final Log log = LogFactory.getLog(RealmBase.class);
  protected Container container;
  protected Log containerLog;
  protected String digest;
  protected String digestEncoding;
  protected static final String info = "org.apache.catalina.realm.RealmBase/1.0";
  protected volatile MessageDigest md;
  @Deprecated
  protected static final MD5Encoder md5Encoder = new MD5Encoder();
  protected static volatile MessageDigest md5Helper;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.realm");
  protected PropertyChangeSupport support;
  protected boolean validate;
  protected String x509UsernameRetrieverClassName;
  protected X509UsernameRetriever x509UsernameRetriever;
  protected AllRolesMode allRolesMode;
  protected boolean stripRealmForGss;
  protected String realmPath;
  
  public Container getContainer()
  {
    return this.container;
  }
  
  public void setContainer(Container container)
  {
    Container oldContainer = this.container;
    this.container = container;
    this.support.firePropertyChange("container", oldContainer, this.container);
  }
  
  public String getAllRolesMode()
  {
    return this.allRolesMode.toString();
  }
  
  public void setAllRolesMode(String allRolesMode)
  {
    this.allRolesMode = AllRolesMode.toMode(allRolesMode);
  }
  
  public String getDigest()
  {
    return this.digest;
  }
  
  public void setDigest(String digest)
  {
    this.digest = digest;
  }
  
  public String getDigestEncoding()
  {
    return this.digestEncoding;
  }
  
  public void setDigestEncoding(String charset)
  {
    this.digestEncoding = charset;
  }
  
  protected Charset getDigestCharset()
    throws UnsupportedEncodingException
  {
    if (this.digestEncoding == null) {
      return Charset.defaultCharset();
    }
    return B2CConverter.getCharset(getDigestEncoding());
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.realm.RealmBase/1.0";
  }
  
  public boolean getValidate()
  {
    return this.validate;
  }
  
  public void setValidate(boolean validate)
  {
    this.validate = validate;
  }
  
  public String getX509UsernameRetrieverClassName()
  {
    return this.x509UsernameRetrieverClassName;
  }
  
  public void setX509UsernameRetrieverClassName(String className)
  {
    this.x509UsernameRetrieverClassName = className;
  }
  
  public boolean isStripRealmForGss()
  {
    return this.stripRealmForGss;
  }
  
  public void setStripRealmForGss(boolean stripRealmForGss)
  {
    this.stripRealmForGss = stripRealmForGss;
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }
  
  public Principal authenticate(String username, String credentials)
  {
    String serverCredentials = getPassword(username);
    
    boolean validated = compareCredentials(credentials, serverCredentials);
    if (!validated)
    {
      if (this.containerLog.isTraceEnabled()) {
        this.containerLog.trace(sm.getString("realmBase.authenticateFailure", new Object[] { username }));
      }
      return null;
    }
    if (this.containerLog.isTraceEnabled()) {
      this.containerLog.trace(sm.getString("realmBase.authenticateSuccess", new Object[] { username }));
    }
    return getPrincipal(username);
  }
  
  public Principal authenticate(String username, String clientDigest, String nonce, String nc, String cnonce, String qop, String realm, String md5a2)
  {
    String md5a1 = getDigest(username, realm);
    if (md5a1 == null) {
      return null;
    }
    md5a1 = md5a1.toLowerCase(Locale.ENGLISH);
    String serverDigestValue;

    if (qop == null) {
      serverDigestValue = md5a1 + ":" + nonce + ":" + md5a2;
    } else {
      serverDigestValue = md5a1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + md5a2;
    }
    byte[] valueBytes = null;
    try
    {
      valueBytes = serverDigestValue.getBytes(getDigestCharset());
    }
    catch (UnsupportedEncodingException uee)
    {
      log.error("Illegal digestEncoding: " + getDigestEncoding(), uee);
      throw new IllegalArgumentException(uee.getMessage());
    }
    String serverDigest = null;
    synchronized (md5Helper)
    {
      serverDigest = MD5Encoder.encode(md5Helper.digest(valueBytes));
    }
    if (log.isDebugEnabled()) {
      log.debug("Digest : " + clientDigest + " Username:" + username + " ClientSigest:" + clientDigest + " nonce:" + nonce + " nc:" + nc + " cnonce:" + cnonce + " qop:" + qop + " realm:" + realm + "md5a2:" + md5a2 + " Server digest:" + serverDigest);
    }
    if (serverDigest.equals(clientDigest)) {
      return getPrincipal(username);
    }
    return null;
  }
  
  public Principal authenticate(X509Certificate[] certs)
  {
    if ((certs == null) || (certs.length < 1)) {
      return null;
    }
    if (log.isDebugEnabled()) {
      log.debug("Authenticating client certificate chain");
    }
    if (this.validate) {
      for (int i = 0; i < certs.length; i++)
      {
        if (log.isDebugEnabled()) {
          log.debug(" Checking validity for '" + certs[i].getSubjectDN().getName() + "'");
        }
        try
        {
          certs[i].checkValidity();
        }
        catch (Exception e)
        {
          if (log.isDebugEnabled()) {
            log.debug("  Validity exception", e);
          }
          return null;
        }
      }
    }
    return getPrincipal(certs[0]);
  }
  
  public Principal authenticate(GSSContext gssContext, boolean storeCred)
  {
    if (gssContext.isEstablished())
    {
      GSSName gssName = null;
      try
      {
        gssName = gssContext.getSrcName();
      }
      catch (GSSException e)
      {
        log.warn(sm.getString("realmBase.gssNameFail"), e);
      }
      if (gssName != null)
      {
        String name = gssName.toString();
        if (isStripRealmForGss())
        {
          int i = name.indexOf('@');
          if (i > 0) {
            name = name.substring(0, i);
          }
        }
        GSSCredential gssCredential = null;
        if ((storeCred) && (gssContext.getCredDelegState())) {
          try
          {
            gssCredential = gssContext.getDelegCred();
          }
          catch (GSSException e)
          {
            if (log.isDebugEnabled()) {
              log.debug(sm.getString("realmBase.delegatedCredentialFail", new Object[] { name }), e);
            }
          }
        }
        return getPrincipal(name, gssCredential);
      }
    }
    return null;
  }
  
  protected boolean compareCredentials(String userCredentials, String serverCredentials)
  {
    if (serverCredentials == null) {
      return false;
    }
    if (hasMessageDigest())
    {
      if ((serverCredentials.startsWith("{MD5}")) || (serverCredentials.startsWith("{SHA}")))
      {
        String serverDigest = serverCredentials.substring(5);
        String userDigest;
        synchronized (this)
        {
          this.md.reset();
          this.md.update(userCredentials.getBytes(B2CConverter.ISO_8859_1));
          userDigest = Base64.encodeBase64String(this.md.digest());
        }
        return userDigest.equals(serverDigest);
      }
      if (serverCredentials.startsWith("{SSHA}"))
      {
        String serverDigestPlusSalt = serverCredentials.substring(6);
        
        byte[] serverDigestPlusSaltBytes = Base64.decodeBase64(serverDigestPlusSalt);
        
        int saltPos = 20;
        byte[] serverDigestBytes = new byte[20];
        System.arraycopy(serverDigestPlusSaltBytes, 0, serverDigestBytes, 0, 20);
        byte[] userDigestBytes;
        synchronized (this)
        {
          this.md.reset();
          
          this.md.update(userCredentials.getBytes(B2CConverter.ISO_8859_1));
          
          this.md.update(serverDigestPlusSaltBytes, 20, serverDigestPlusSaltBytes.length - 20);
          
          userDigestBytes = this.md.digest();
        }
        return Arrays.equals(userDigestBytes, serverDigestBytes);
      }
      String userDigest = digest(userCredentials);
      return serverCredentials.equalsIgnoreCase(userDigest);
    }
    return serverCredentials.equals(userCredentials);
  }
  
  public SecurityConstraint[] findSecurityConstraints(Request request, Context context)
  {
    ArrayList<SecurityConstraint> results = null;
    
    SecurityConstraint[] constraints = context.findConstraints();
    if ((constraints == null) || (constraints.length == 0))
    {
      if (log.isDebugEnabled()) {
        log.debug("  No applicable constraints defined");
      }
      return null;
    }
    String uri = request.getRequestPathMB().toString();
    if (uri == null) {
      uri = "/";
    }
    String method = request.getMethod();
    
    boolean found = false;
    for (int i = 0; i < constraints.length; i++)
    {
      SecurityCollection[] collection = constraints[i].findCollections();
      if (collection != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
        }
        for (int j = 0; j < collection.length; j++)
        {
          String[] patterns = collection[j].findPatterns();
          if (patterns != null) {
            for (int k = 0; k < patterns.length; k++) {
              if (uri.equals(patterns[k]))
              {
                found = true;
                if (collection[j].findMethod(method))
                {
                  if (results == null) {
                    results = new ArrayList();
                  }
                  results.add(constraints[i]);
                }
              }
            }
          }
        }
      }
    }
    if (found) {
      return resultsToArray(results);
    }
    int longest = -1;
    int i;
    for (i = 0; i < constraints.length; i++)
    {
      SecurityCollection[] collection = constraints[i].findCollections();
      if (collection != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
        }
        for (int j = 0; j < collection.length; j++)
        {
          String[] patterns = collection[j].findPatterns();
          if (patterns != null)
          {
            boolean matched = false;
            int length = -1;
            for (int k = 0; k < patterns.length; k++)
            {
              String pattern = patterns[k];
              if ((pattern.startsWith("/")) && (pattern.endsWith("/*")) && (pattern.length() >= longest)) {
                if (pattern.length() == 2)
                {
                  matched = true;
                  length = pattern.length();
                }
                else if ((pattern.regionMatches(0, uri, 0, pattern.length() - 1)) || ((pattern.length() - 2 == uri.length()) && (pattern.regionMatches(0, uri, 0, pattern.length() - 2))))
                {
                  matched = true;
                  length = pattern.length();
                }
              }
            }
            if (matched)
            {
              if (length > longest)
              {
                found = false;
                if (results != null) {
                  results.clear();
                }
                longest = length;
              }
              if (collection[j].findMethod(method))
              {
                found = true;
                if (results == null) {
                  results = new ArrayList();
                }
                results.add(constraints[i]);
              }
            }
          }
        }
      }
    }
    if (found) {
      return resultsToArray(results);
    }
    for (i = 0; i < constraints.length; i++)
    {
      SecurityCollection[] collection = constraints[i].findCollections();
      if (collection != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
        }
        boolean matched = false;
        int pos = -1;
        for (int j = 0; j < collection.length; j++)
        {
          String[] patterns = collection[j].findPatterns();
          if (patterns != null) {
            for (int k = 0; (k < patterns.length) && (!matched); k++)
            {
              String pattern = patterns[k];
              if (pattern.startsWith("*."))
              {
                int slash = uri.lastIndexOf("/");
                int dot = uri.lastIndexOf(".");
                if ((slash >= 0) && (dot > slash) && (dot != uri.length() - 1) && (uri.length() - dot == pattern.length() - 1)) {
                  if (pattern.regionMatches(1, uri, dot, uri.length() - dot))
                  {
                    matched = true;
                    pos = j;
                  }
                }
              }
            }
          }
        }
        if (matched)
        {
          found = true;
          if (collection[pos].findMethod(method))
          {
            if (results == null) {
              results = new ArrayList();
            }
            results.add(constraints[i]);
          }
        }
      }
    }
    if (found) {
      return resultsToArray(results);
    }
    for (i = 0; i < constraints.length; i++)
    {
      SecurityCollection[] collection = constraints[i].findCollections();
      if (collection != null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> " + constraints[i].included(uri, method));
        }
        for (int j = 0; j < collection.length; j++)
        {
          String[] patterns = collection[j].findPatterns();
          if (patterns != null)
          {
            boolean matched = false;
            for (int k = 0; (k < patterns.length) && (!matched); k++)
            {
              String pattern = patterns[k];
              if (pattern.equals("/")) {
                matched = true;
              }
            }
            if (matched)
            {
              if (results == null) {
                results = new ArrayList();
              }
              results.add(constraints[i]);
            }
          }
        }
      }
    }
    if (results == null) {
      if (log.isDebugEnabled()) {
        log.debug("  No applicable constraint located");
      }
    }
    return resultsToArray(results);
  }
  
  private SecurityConstraint[] resultsToArray(ArrayList<SecurityConstraint> results)
  {
    if ((results == null) || (results.size() == 0)) {
      return null;
    }
    SecurityConstraint[] array = new SecurityConstraint[results.size()];
    results.toArray(array);
    return array;
  }
  
  public boolean hasResourcePermission(Request request, Response response, SecurityConstraint[] constraints, Context context)
    throws IOException
  {
    if ((constraints == null) || (constraints.length == 0)) {
      return true;
    }
    Principal principal = request.getPrincipal();
    boolean status = false;
    boolean denyfromall = false;
    for (int i = 0; i < constraints.length; i++)
    {
      SecurityConstraint constraint = constraints[i];
      String[] roles;

      if (constraint.getAllRoles()) {
        roles = request.getContext().findSecurityRoles();
      } else {
        roles = constraint.findAuthRoles();
      }
      if (roles == null) {
        roles = new String[0];
      }
      if (log.isDebugEnabled()) {
        log.debug("  Checking roles " + principal);
      }
      if ((roles.length == 0) && (!constraint.getAllRoles()))
      {
        if (constraint.getAuthConstraint())
        {
          if (log.isDebugEnabled()) {
            log.debug("No roles");
          }
          status = false;
          denyfromall = true;
          break;
        }
        if (log.isDebugEnabled()) {
          log.debug("Passing all access");
        }
        status = true;
      }
      else if (principal == null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  No user authenticated, cannot grant access");
        }
      }
      else
      {
        for (int j = 0; j < roles.length; j++) {
          if (hasRole(null, principal, roles[j]))
          {
            status = true;
            if (log.isDebugEnabled()) {
              log.debug("Role found:  " + roles[j]);
            }
          }
          else if (log.isDebugEnabled())
          {
            log.debug("No role found:  " + roles[j]);
          }
        }
      }
    }
    if ((!denyfromall) && (this.allRolesMode != AllRolesMode.STRICT_MODE) && (!status) && (principal != null))
    {
      if (log.isDebugEnabled()) {
        log.debug("Checking for all roles mode: " + this.allRolesMode);
      }
      for (int i = 0; i < constraints.length; i++)
      {
        SecurityConstraint constraint = constraints[i];
        if (constraint.getAllRoles())
        {
          if (this.allRolesMode == AllRolesMode.AUTH_ONLY_MODE)
          {
            if (log.isDebugEnabled()) {
              log.debug("Granting access for role-name=*, auth-only");
            }
            status = true;
            break;
          }
          String[] roles = request.getContext().findSecurityRoles();
          if ((roles.length == 0) && (this.allRolesMode == AllRolesMode.STRICT_AUTH_ONLY_MODE))
          {
            if (log.isDebugEnabled()) {
              log.debug("Granting access for role-name=*, strict auth-only");
            }
            status = true;
            break;
          }
        }
      }
    }
    if (!status) {
      response.sendError(403, sm.getString("realmBase.forbidden"));
    }
    return status;
  }
  
  public boolean hasRole(Wrapper wrapper, Principal principal, String role)
  {
    if (wrapper != null)
    {
      String realRole = wrapper.findSecurityReference(role);
      if (realRole != null) {
        role = realRole;
      }
    }
    if ((principal == null) || (role == null) || (!(principal instanceof GenericPrincipal))) {
      return false;
    }
    GenericPrincipal gp = (GenericPrincipal)principal;
    boolean result = gp.hasRole(role);
    if (log.isDebugEnabled())
    {
      String name = principal.getName();
      if (result) {
        log.debug(sm.getString("realmBase.hasRoleSuccess", new Object[] { name, role }));
      } else {
        log.debug(sm.getString("realmBase.hasRoleFailure", new Object[] { name, role }));
      }
    }
    return result;
  }
  
  public boolean hasUserDataPermission(Request request, Response response, SecurityConstraint[] constraints)
    throws IOException
  {
    if ((constraints == null) || (constraints.length == 0))
    {
      if (log.isDebugEnabled()) {
        log.debug("  No applicable security constraint defined");
      }
      return true;
    }
    for (int i = 0; i < constraints.length; i++)
    {
      SecurityConstraint constraint = constraints[i];
      String userConstraint = constraint.getUserConstraint();
      if (userConstraint == null)
      {
        if (log.isDebugEnabled()) {
          log.debug("  No applicable user data constraint defined");
        }
        return true;
      }
      if (userConstraint.equals("NONE"))
      {
        if (log.isDebugEnabled()) {
          log.debug("  User data constraint has no restrictions");
        }
        return true;
      }
    }
    if (request.getRequest().isSecure())
    {
      if (log.isDebugEnabled()) {
        log.debug("  User data constraint already satisfied");
      }
      return true;
    }
    int redirectPort = request.getConnector().getRedirectPort();
    if (redirectPort <= 0)
    {
      if (log.isDebugEnabled()) {
        log.debug("  SSL redirect is disabled");
      }
      response.sendError(403, request.getRequestURI());
      
      return false;
    }
    StringBuilder file = new StringBuilder();
    String protocol = "https";
    String host = request.getServerName();
    
    file.append(protocol).append("://").append(host);
    if (redirectPort != 443) {
      file.append(":").append(redirectPort);
    }
    file.append(request.getRequestURI());
    String requestedSessionId = request.getRequestedSessionId();
    if ((requestedSessionId != null) && (request.isRequestedSessionIdFromURL()))
    {
      file.append(";");
      file.append(SessionConfig.getSessionUriParamName(request.getContext()));
      
      file.append("=");
      file.append(requestedSessionId);
    }
    String queryString = request.getQueryString();
    if (queryString != null)
    {
      file.append('?');
      file.append(queryString);
    }
    if (log.isDebugEnabled()) {
      log.debug("  Redirecting to " + file.toString());
    }
    response.sendRedirect(file.toString());
    return false;
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
    if (this.container != null) {
      this.containerLog = this.container.getLogger();
    }
    this.x509UsernameRetriever = createUsernameRetriever(this.x509UsernameRetrieverClassName);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (this.digest != null) {
      try
      {
        this.md = MessageDigest.getInstance(this.digest);
      }
      catch (NoSuchAlgorithmException e)
      {
        throw new LifecycleException(sm.getString("realmBase.algorithm", new Object[] { this.digest }), e);
      }
    }
    setState(LifecycleState.STARTING);
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
    
    this.md = null;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("Realm[");
    sb.append(getName());
    sb.append(']');
    return sb.toString();
  }
  
  protected String digest(String credentials)
  {
    if (!hasMessageDigest()) {
      return credentials;
    }
    synchronized (this)
    {
      try
      {
        this.md.reset();
        
        byte[] bytes = null;
        try
        {
          bytes = credentials.getBytes(getDigestCharset());
        }
        catch (UnsupportedEncodingException uee)
        {
          log.error("Illegal digestEncoding: " + getDigestEncoding(), uee);
          throw new IllegalArgumentException(uee.getMessage());
        }
        this.md.update(bytes);
        
        return HexUtils.toHexString(this.md.digest());
      }
      catch (Exception e)
      {
        log.error(sm.getString("realmBase.digest"), e);
        return credentials;
      }
    }
  }
  
  protected boolean hasMessageDigest()
  {
    return this.md != null;
  }
  
  protected String getDigest(String username, String realmName)
  {
    if (md5Helper == null) {
      try
      {
        md5Helper = MessageDigest.getInstance("MD5");
      }
      catch (NoSuchAlgorithmException e)
      {
        log.error("Couldn't get MD5 digest: ", e);
        throw new IllegalStateException(e.getMessage());
      }
    }
    if (hasMessageDigest()) {
      return getPassword(username);
    }
    String digestValue = username + ":" + realmName + ":" + getPassword(username);
    
    byte[] valueBytes = null;
    try
    {
      valueBytes = digestValue.getBytes(getDigestCharset());
    }
    catch (UnsupportedEncodingException uee)
    {
      log.error("Illegal digestEncoding: " + getDigestEncoding(), uee);
      throw new IllegalArgumentException(uee.getMessage());
    }
    byte[] digest = null;
    synchronized (md5Helper)
    {
      digest = md5Helper.digest(valueBytes);
    }
    return MD5Encoder.encode(digest);
  }
  
  protected Principal getPrincipal(X509Certificate usercert)
  {
    String username = this.x509UsernameRetriever.getUsername(usercert);
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("realmBase.gotX509Username", new Object[] { username }));
    }
    return getPrincipal(username);
  }
  
  protected Principal getPrincipal(String username, GSSCredential gssCredential)
  {
    Principal p = getPrincipal(username);
    if ((p instanceof GenericPrincipal)) {
      ((GenericPrincipal)p).setGssCredential(gssCredential);
    }
    return p;
  }
  
  protected Server getServer()
  {
    Container c = this.container;
    if ((c instanceof Context)) {
      c = c.getParent();
    }
    if ((c instanceof Host)) {
      c = c.getParent();
    }
    if ((c instanceof Engine))
    {
      Service s = ((Engine)c).getService();
      if (s != null) {
        return s.getServer();
      }
    }
    return null;
  }
  
  public static final String Digest(String credentials, String algorithm, String encoding)
  {
    try
    {
      MessageDigest md = (MessageDigest)MessageDigest.getInstance(algorithm).clone();
      if (encoding == null) {
        md.update(credentials.getBytes());
      } else {
        md.update(credentials.getBytes(encoding));
      }
      return HexUtils.toHexString(md.digest());
    }
    catch (Exception ex)
    {
      log.error(ex);
    }
    return credentials;
  }
  
  public static void main(String[] args)
  {
    String encoding = null;
    int firstCredentialArg = 2;
    if ((args.length > 4) && (args[2].equalsIgnoreCase("-e")))
    {
      encoding = args[3];
      firstCredentialArg = 4;
    }
    if ((args.length > firstCredentialArg) && (args[0].equalsIgnoreCase("-a"))) {
      for (int i = firstCredentialArg; i < args.length; i++)
      {
        System.out.print(args[i] + ":");
        System.out.println(Digest(args[i], args[1], encoding));
      }
    } else {
      System.out.println("Usage: RealmBase -a <algorithm> [-e <encoding>] <credentials>");
    }
  }
  
  public String getObjectNameKeyProperties()
  {
    StringBuilder keyProperties = new StringBuilder("type=Realm");
    keyProperties.append(getRealmSuffix());
    keyProperties.append(MBeanUtils.getContainerKeyProperties(this.container));
    
    return keyProperties.toString();
  }
  
  public String getDomainInternal()
  {
    return MBeanUtils.getDomain(this.container);
  }
  
  public RealmBase()
  {
    this.container = null;
    
    this.containerLog = null;
    
    this.digest = null;
    
    this.digestEncoding = null;
    
    this.md = null;
    
    this.support = new PropertyChangeSupport(this);
    
    this.validate = true;
    
    this.allRolesMode = AllRolesMode.STRICT_MODE;
    
    this.stripRealmForGss = true;
    
    this.realmPath = "/realm0";
  }
  
  public String getRealmPath()
  {
    return this.realmPath;
  }
  
  public void setRealmPath(String theRealmPath)
  {
    this.realmPath = theRealmPath;
  }
  
  protected String getRealmSuffix()
  {
    return ",realmPath=" + getRealmPath();
  }
  
  protected static class AllRolesMode
  {
    private String name;
    public static final AllRolesMode STRICT_MODE = new AllRolesMode("strict");
    public static final AllRolesMode AUTH_ONLY_MODE = new AllRolesMode("authOnly");
    public static final AllRolesMode STRICT_AUTH_ONLY_MODE = new AllRolesMode("strictAuthOnly");
    
    static AllRolesMode toMode(String name)
    {
      AllRolesMode mode;
      if (name.equalsIgnoreCase(STRICT_MODE.name))
      {
        mode = STRICT_MODE;
      }
      else
      {

        if (name.equalsIgnoreCase(AUTH_ONLY_MODE.name))
        {
          mode = AUTH_ONLY_MODE;
        }
        else
        {
          
          if (name.equalsIgnoreCase(STRICT_AUTH_ONLY_MODE.name)) {
            mode = STRICT_AUTH_ONLY_MODE;
          } else {
            throw new IllegalStateException("Unknown mode, must be one of: strict, authOnly, strictAuthOnly");
          }
        }
      }

      return mode;
    }
    
    private AllRolesMode(String name)
    {
      this.name = name;
    }
    
    public boolean equals(Object o)
    {
      boolean equals = false;
      if ((o instanceof AllRolesMode))
      {
        AllRolesMode mode = (AllRolesMode)o;
        equals = this.name.equals(mode.name);
      }
      return equals;
    }
    
    public int hashCode()
    {
      return this.name.hashCode();
    }
    
    public String toString()
    {
      return this.name;
    }
  }
  
  private static X509UsernameRetriever createUsernameRetriever(String className)
    throws LifecycleException
  {
    if ((null == className) || ("".equals(className.trim()))) {
      return new X509SubjectDnRetriever();
    }
    try
    {
      Class<? extends X509UsernameRetriever> clazz = (Class<? extends X509UsernameRetriever>) Class.forName(className);
      return (X509UsernameRetriever)clazz.newInstance();
    }
    catch (ClassNotFoundException e)
    {
      throw new LifecycleException(sm.getString("realmBase.createUsernameRetriever.ClassNotFoundException", new Object[] { className }), e);
    }
    catch (InstantiationException e)
    {
      throw new LifecycleException(sm.getString("realmBase.createUsernameRetriever.InstantiationException", new Object[] { className }), e);
    }
    catch (IllegalAccessException e)
    {
      throw new LifecycleException(sm.getString("realmBase.createUsernameRetriever.IllegalAccessException", new Object[] { className }), e);
    }
    catch (ClassCastException e)
    {
      throw new LifecycleException(sm.getString("realmBase.createUsernameRetriever.ClassCastException", new Object[] { className }), e);
    }
  }
  
  public void backgroundProcess() {}
  
  protected abstract String getName();
  
  protected abstract String getPassword(String paramString);
  
  protected abstract Principal getPrincipal(String paramString);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\RealmBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */