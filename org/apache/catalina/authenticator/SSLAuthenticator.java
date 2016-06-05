package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

public class SSLAuthenticator
  extends AuthenticatorBase
{
  protected static final String info = "org.apache.catalina.authenticator.SSLAuthenticator/1.0";
  
  public SSLAuthenticator() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.SSLAuthenticator/1.0";
  }
  
  public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    Principal principal = request.getUserPrincipal();
    if (principal != null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("Already authenticated '" + principal.getName() + "'");
      }
      String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
      if (ssoId != null) {
        associate(ssoId, request.getSessionInternal(true));
      }
      return true;
    }
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug(" Looking up certificates");
    }
    X509Certificate[] certs = getRequestCertificates(request);
    if ((certs == null) || (certs.length < 1))
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("  No certificates included with this request");
      }
      response.sendError(401, sm.getString("authenticator.certificates"));
      
      return false;
    }
    principal = this.context.getRealm().authenticate(certs);
    if (principal == null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("  Realm.authenticate() returned false");
      }
      response.sendError(401, sm.getString("authenticator.unauthorized"));
      
      return false;
    }
    register(request, response, principal, "CLIENT_CERT", null, null);
    
    return true;
  }
  
  protected String getAuthMethod()
  {
    return "CLIENT_CERT";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\SSLAuthenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */