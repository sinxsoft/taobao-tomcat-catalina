package org.apache.catalina;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;

public abstract interface Authenticator
{
  public abstract boolean authenticate(Request paramRequest, HttpServletResponse paramHttpServletResponse)
    throws IOException;
  
  @Deprecated
  public abstract boolean authenticate(Request paramRequest, HttpServletResponse paramHttpServletResponse, LoginConfig paramLoginConfig)
    throws IOException;
  
  public abstract void login(String paramString1, String paramString2, Request paramRequest)
    throws ServletException;
  
  public abstract void logout(Request paramRequest)
    throws ServletException;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Authenticator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */