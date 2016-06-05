package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.http.MimeHeaders;

public class BasicAuthenticator
  extends AuthenticatorBase
{
  private static final Log log = LogFactory.getLog(BasicAuthenticator.class);
  protected static final String info = "org.apache.catalina.authenticator.BasicAuthenticator/1.0";
  
  public BasicAuthenticator() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.BasicAuthenticator/1.0";
  }
  
  public boolean authenticate(org.apache.catalina.connector.Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
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
    String username = null;
    String password = null;
    
    MessageBytes authorization = request.getCoyoteRequest().getMimeHeaders().getValue("authorization");
    if (authorization != null)
    {
      authorization.toBytes();
      ByteChunk authorizationBC = authorization.getByteChunk();
      if (authorizationBC.startsWithIgnoreCase("basic ", 0))
      {
        authorizationBC.setOffset(authorizationBC.getOffset() + 6);
        
        byte[] decoded = Base64.decodeBase64(authorizationBC.getBuffer(), authorizationBC.getOffset(), authorizationBC.getLength());
        
        int colon = -1;
        for (int i = 0; i < decoded.length; i++) {
          if (decoded[i] == 58)
          {
            colon = i;
            break;
          }
        }
        if (colon < 0)
        {
          username = new String(decoded, B2CConverter.ISO_8859_1);
        }
        else
        {
          username = new String(decoded, 0, colon, B2CConverter.ISO_8859_1);
          
          password = new String(decoded, colon + 1, decoded.length - colon - 1, B2CConverter.ISO_8859_1);
        }
        authorizationBC.setOffset(authorizationBC.getOffset() - 6);
      }
      principal = this.context.getRealm().authenticate(username, password);
      if (principal != null)
      {
        register(request, response, principal, "BASIC", username, password);
        
        return true;
      }
    }
    StringBuilder value = new StringBuilder(16);
    value.append("Basic realm=\"");
    if (config.getRealmName() == null) {
      value.append("Authentication required");
    } else {
      value.append(config.getRealmName());
    }
    value.append('"');
    response.setHeader("WWW-Authenticate", value.toString());
    response.sendError(401);
    return false;
  }
  
  protected String getAuthMethod()
  {
    return "BASIC";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\BasicAuthenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */