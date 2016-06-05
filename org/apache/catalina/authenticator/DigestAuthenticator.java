package org.apache.catalina.authenticator;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.apache.tomcat.util.security.MD5Encoder;

public class DigestAuthenticator
  extends AuthenticatorBase
{
  private static final Log log = LogFactory.getLog(DigestAuthenticator.class);
  @Deprecated
  protected static final MD5Encoder md5Encoder = new MD5Encoder();
  protected static final String info = "org.apache.catalina.authenticator.DigestAuthenticator/1.0";
  protected static final String QOP = "auth";
  @Deprecated
  protected static volatile MessageDigest md5Helper;
  protected Map<String, NonceInfo> nonces;
  
  public DigestAuthenticator()
  {
    setCache(false);
    try
    {
      if (md5Helper == null) {
        md5Helper = MessageDigest.getInstance("MD5");
      }
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new IllegalStateException(e);
    }
  }
  
  protected long lastTimestamp = 0L;
  protected final Object lastTimestampLock = new Object();
  protected int nonceCacheSize = 1000;
  protected int nonceCountWindowSize = 100;
  protected String key = null;
  protected long nonceValidity = 300000L;
  protected String opaque;
  protected boolean validateUri = true;
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.DigestAuthenticator/1.0";
  }
  
  public int getNonceCountWindowSize()
  {
    return this.nonceCountWindowSize;
  }
  
  public void setNonceCountWindowSize(int nonceCountWindowSize)
  {
    this.nonceCountWindowSize = nonceCountWindowSize;
  }
  
  public int getNonceCacheSize()
  {
    return this.nonceCacheSize;
  }
  
  public void setNonceCacheSize(int nonceCacheSize)
  {
    this.nonceCacheSize = nonceCacheSize;
  }
  
  public String getKey()
  {
    return this.key;
  }
  
  public void setKey(String key)
  {
    this.key = key;
  }
  
  public long getNonceValidity()
  {
    return this.nonceValidity;
  }
  
  public void setNonceValidity(long nonceValidity)
  {
    this.nonceValidity = nonceValidity;
  }
  
  public String getOpaque()
  {
    return this.opaque;
  }
  
  public void setOpaque(String opaque)
  {
    this.opaque = opaque;
  }
  
  public boolean isValidateUri()
  {
    return this.validateUri;
  }
  
  public void setValidateUri(boolean validateUri)
  {
    this.validateUri = validateUri;
  }
  
  public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    Principal principal = request.getUserPrincipal();
    if (principal != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("Already authenticated '" + principal.getName() + "'");
      }
      String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
      if (ssoId != null) {
        associate(ssoId, request.getSessionInternal(true));
      }
      return true;
    }
    String authorization = request.getHeader("authorization");
    DigestInfo digestInfo = new DigestInfo(getOpaque(), getNonceValidity(), getKey(), this.nonces, isValidateUri());
    if ((authorization != null) && 
      (digestInfo.parse(request, authorization)))
    {
      if (digestInfo.validate(request, config)) {
        principal = digestInfo.authenticate(this.context.getRealm());
      }
      if ((principal != null) && (!digestInfo.isNonceStale()))
      {
        register(request, response, principal, "DIGEST", digestInfo.getUsername(), null);
        
        return true;
      }
    }
    String nonce = generateNonce(request);
    
    setAuthenticateHeader(request, response, config, nonce, (principal != null) && (digestInfo.isNonceStale()));
    
    response.sendError(401);
    return false;
  }
  
  protected String getAuthMethod()
  {
    return "DIGEST";
  }
  
  @Deprecated
  protected String parseUsername(String authorization)
  {
    if (authorization == null) {
      return null;
    }
    if (!authorization.startsWith("Digest ")) {
      return null;
    }
    authorization = authorization.substring(7).trim();
    
    StringTokenizer commaTokenizer = new StringTokenizer(authorization, ",");
    while (commaTokenizer.hasMoreTokens())
    {
      String currentToken = commaTokenizer.nextToken();
      int equalSign = currentToken.indexOf('=');
      if (equalSign < 0) {
        return null;
      }
      String currentTokenName = currentToken.substring(0, equalSign).trim();
      
      String currentTokenValue = currentToken.substring(equalSign + 1).trim();
      if ("username".equals(currentTokenName)) {
        return removeQuotes(currentTokenValue);
      }
    }
    return null;
  }
  
  protected static String removeQuotes(String quotedString, boolean quotesRequired)
  {
    if ((quotedString.length() > 0) && (quotedString.charAt(0) != '"') && (!quotesRequired)) {
      return quotedString;
    }
    if (quotedString.length() > 2) {
      return quotedString.substring(1, quotedString.length() - 1);
    }
    return "";
  }
  
  protected static String removeQuotes(String quotedString)
  {
    return removeQuotes(quotedString, false);
  }
  
  protected String generateNonce(Request request)
  {
    long currentTime = System.currentTimeMillis();
    synchronized (this.lastTimestampLock)
    {
      if (currentTime > this.lastTimestamp) {
        this.lastTimestamp = currentTime;
      } else {
        currentTime = ++this.lastTimestamp;
      }
    }
    String ipTimeKey = request.getRemoteAddr() + ":" + currentTime + ":" + getKey();
    
    byte[] buffer = ConcurrentMessageDigest.digestMD5(new byte[][] { ipTimeKey.getBytes(B2CConverter.ISO_8859_1) });
    
    String nonce = currentTime + ":" + MD5Encoder.encode(buffer);
    
    NonceInfo info = new NonceInfo(currentTime, getNonceCountWindowSize());
    synchronized (this.nonces)
    {
      this.nonces.put(nonce, info);
    }
    return nonce;
  }
  
  protected void setAuthenticateHeader(HttpServletRequest request, HttpServletResponse response, LoginConfig config, String nonce, boolean isNonceStale)
  {
    String realmName = config.getRealmName();
    if (realmName == null) {
      realmName = "Authentication required";
    }
    String authenticateHeader;
    
    if (isNonceStale) {
      authenticateHeader = "Digest realm=\"" + realmName + "\", " + "qop=\"" + "auth" + "\", nonce=\"" + nonce + "\", " + "opaque=\"" + getOpaque() + "\", stale=true";
    } else {
      authenticateHeader = "Digest realm=\"" + realmName + "\", " + "qop=\"" + "auth" + "\", nonce=\"" + nonce + "\", " + "opaque=\"" + getOpaque() + "\"";
    }
    response.setHeader("WWW-Authenticate", authenticateHeader);
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    super.startInternal();
    if (getKey() == null) {
      setKey(this.sessionIdGenerator.generateSessionId());
    }
    if (getOpaque() == null) {
      setOpaque(this.sessionIdGenerator.generateSessionId());
    }
    this.nonces = new LinkedHashMap<String, DigestAuthenticator.NonceInfo>()
    {
      private static final long serialVersionUID = 1L;
      private static final long LOG_SUPPRESS_TIME = 300000L;
      private long lastLog = 0L;
      
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, DigestAuthenticator.NonceInfo> eldest)
      {
        long currentTime = System.currentTimeMillis();
        if (size() > DigestAuthenticator.this.getNonceCacheSize())
        {
          if ((this.lastLog < currentTime) && (currentTime - ((DigestAuthenticator.NonceInfo)eldest.getValue()).getTimestamp() < DigestAuthenticator.this.getNonceValidity()))
          {
            DigestAuthenticator.log.warn(AuthenticatorBase.sm.getString("digestAuthenticator.cacheRemove"));
            
            this.lastLog = (currentTime + 300000L);
          }
          return true;
        }
        return false;
      }
    };
  }
  
  private static class DigestInfo
  {
    private final String opaque;
    private final long nonceValidity;
    private final String key;
    private final Map<String, DigestAuthenticator.NonceInfo> nonces;
    private boolean validateUri = true;
    private String userName = null;
    private String method = null;
    private String uri = null;
    private String response = null;
    private String nonce = null;
    private String nc = null;
    private String cnonce = null;
    private String realmName = null;
    private String qop = null;
    private String opaqueReceived = null;
    private boolean nonceStale = false;
    
    public DigestInfo(String opaque, long nonceValidity, String key, Map<String, DigestAuthenticator.NonceInfo> nonces, boolean validateUri)
    {
      this.opaque = opaque;
      this.nonceValidity = nonceValidity;
      this.key = key;
      this.nonces = nonces;
      this.validateUri = validateUri;
    }
    
    public String getUsername()
    {
      return this.userName;
    }
    
    public boolean parse(Request request, String authorization)
    {
      if (authorization == null) {
        return false;
      }
      Map<String, String> directives;
      try
      {
        directives = HttpParser.parseAuthorizationDigest(new StringReader(authorization));
      }
      catch (IOException e)
      {
        return false;
      }
      if (directives == null) {
        return false;
      }
      this.method = request.getMethod();
      this.userName = ((String)directives.get("username"));
      this.realmName = ((String)directives.get("realm"));
      this.nonce = ((String)directives.get("nonce"));
      this.nc = ((String)directives.get("nc"));
      this.cnonce = ((String)directives.get("cnonce"));
      this.qop = ((String)directives.get("qop"));
      this.uri = ((String)directives.get("uri"));
      this.response = ((String)directives.get("response"));
      this.opaqueReceived = ((String)directives.get("opaque"));
      
      return true;
    }
    
    public boolean validate(Request request, LoginConfig config)
    {
      if ((this.userName == null) || (this.realmName == null) || (this.nonce == null) || (this.uri == null) || (this.response == null)) {
        return false;
      }
      if (this.validateUri)
      {
        String query = request.getQueryString();
        String uriQuery;
      
        if (query == null) {
          uriQuery = request.getRequestURI();
        } else {
          uriQuery = request.getRequestURI() + "?" + query;
        }
        if (!this.uri.equals(uriQuery))
        {
          String host = request.getHeader("host");
          String scheme = request.getScheme();
          if ((host != null) && (!uriQuery.startsWith(scheme)))
          {
            StringBuilder absolute = new StringBuilder();
            absolute.append(scheme);
            absolute.append("://");
            absolute.append(host);
            absolute.append(uriQuery);
            if (!this.uri.equals(absolute.toString())) {
              return false;
            }
          }
          else
          {
            return false;
          }
        }
      }
      String lcRealm = config.getRealmName();
      if (lcRealm == null) {
        lcRealm = "Authentication required";
      }
      if (!lcRealm.equals(this.realmName)) {
        return false;
      }
      if (!this.opaque.equals(this.opaqueReceived)) {
        return false;
      }
      int i = this.nonce.indexOf(":");
      if ((i < 0) || (i + 1 == this.nonce.length())) {
        return false;
      }
      long nonceTime;
      try
      {
        nonceTime = Long.parseLong(this.nonce.substring(0, i));
      }
      catch (NumberFormatException nfe)
      {
        return false;
      }
      String md5clientIpTimeKey = this.nonce.substring(i + 1);
      long currentTime = System.currentTimeMillis();
      if (currentTime - nonceTime > this.nonceValidity)
      {
        this.nonceStale = true;
        synchronized (this.nonces)
        {
          this.nonces.remove(this.nonce);
        }
      }
      String serverIpTimeKey = request.getRemoteAddr() + ":" + nonceTime + ":" + this.key;
      
      byte[] buffer = ConcurrentMessageDigest.digestMD5(new byte[][] { serverIpTimeKey.getBytes(B2CConverter.ISO_8859_1) });
      
      String md5ServerIpTimeKey = MD5Encoder.encode(buffer);
      if (!md5ServerIpTimeKey.equals(md5clientIpTimeKey)) {
        return false;
      }
      if ((this.qop != null) && (!"auth".equals(this.qop))) {
        return false;
      }
      if (this.qop == null)
      {
        if ((this.cnonce != null) || (this.nc != null)) {
          return false;
        }
      }
      else
      {
        if ((this.cnonce == null) || (this.nc == null)) {
          return false;
        }
        if ((this.nc.length() < 6) || (this.nc.length() > 8)) {
          return false;
        }
        long count;
        try
        {
          count = Long.parseLong(this.nc, 16);
        }
        catch (NumberFormatException nfe)
        {
          return false;
        }
        DigestAuthenticator.NonceInfo info;
        synchronized (this.nonces)
        {
          info = (DigestAuthenticator.NonceInfo)this.nonces.get(this.nonce);
        }
        if (info == null) {
          this.nonceStale = true;
        } else if (!info.nonceCountValid(count)) {
          return false;
        }
      }
      return true;
    }
    
    public boolean isNonceStale()
    {
      return this.nonceStale;
    }
    
    public Principal authenticate(Realm realm)
    {
      String a2 = this.method + ":" + this.uri;
      
      byte[] buffer = ConcurrentMessageDigest.digestMD5(new byte[][] { a2.getBytes(B2CConverter.ISO_8859_1) });
      
      String md5a2 = MD5Encoder.encode(buffer);
      
      return realm.authenticate(this.userName, this.response, this.nonce, this.nc, this.cnonce, this.qop, this.realmName, md5a2);
    }
  }
  
  private static class NonceInfo
  {
    private volatile long timestamp;
    private volatile boolean[] seen;
    private volatile int offset;
    private volatile int count = 0;
    
    public NonceInfo(long currentTime, int seenWindowSize)
    {
      this.timestamp = currentTime;
      this.seen = new boolean[seenWindowSize];
      this.offset = (seenWindowSize / 2);
    }
    
    public synchronized boolean nonceCountValid(long nonceCount)
    {
      if ((this.count - this.offset >= nonceCount) || (nonceCount > this.count - this.offset + this.seen.length)) {
        return false;
      }
      int checkIndex = (int)((nonceCount + this.offset) % this.seen.length);
      if (this.seen[checkIndex] != false) {
        return false;
      }
      this.seen[checkIndex] = true;
      this.seen[(this.count % this.seen.length)] = false;
      this.count += 1;
      return true;
    }
    
    public long getTimestamp()
    {
      return this.timestamp;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\DigestAuthenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */