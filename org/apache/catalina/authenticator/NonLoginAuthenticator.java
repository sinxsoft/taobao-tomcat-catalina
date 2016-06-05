package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.juli.logging.Log;

public final class NonLoginAuthenticator
  extends AuthenticatorBase
{
  private static final String info = "org.apache.catalina.authenticator.NonLoginAuthenticator/1.0";
  
  public NonLoginAuthenticator() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.authenticator.NonLoginAuthenticator/1.0";
  }
  
  public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config)
    throws IOException
  {
    Principal principal = request.getPrincipal();
    if (principal != null)
    {
      if (this.containerLog.isDebugEnabled()) {
        this.containerLog.debug("Already authenticated as '" + principal.getName() + "'");
      }
      if (this.cache)
      {
        Session session = request.getSessionInternal(true);
        
        session.setPrincipal(principal);
        
        String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
        if (ssoId != null)
        {
          if (this.containerLog.isDebugEnabled()) {
            this.containerLog.debug("User authenticated by existing SSO");
          }
          associate(ssoId, session);
        }
      }
      return true;
    }
    if (this.containerLog.isDebugEnabled()) {
      this.containerLog.debug("User authenticated without any roles");
    }
    return true;
  }
  
  protected String getAuthMethod()
  {
    return "NONE";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\NonLoginAuthenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */