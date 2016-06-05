package org.apache.catalina.deploy;

import java.util.EnumSet;
import javax.servlet.SessionTrackingMode;

public class SessionConfig
{
  private Integer sessionTimeout;
  private String cookieName;
  private String cookieDomain;
  private String cookiePath;
  private String cookieComment;
  private Boolean cookieHttpOnly;
  private Boolean cookieSecure;
  private Integer cookieMaxAge;
  private EnumSet<SessionTrackingMode> sessionTrackingModes = EnumSet.noneOf(SessionTrackingMode.class);
  
  public SessionConfig() {}
  
  public Integer getSessionTimeout()
  {
    return this.sessionTimeout;
  }
  
  public void setSessionTimeout(String sessionTimeout)
  {
    this.sessionTimeout = Integer.valueOf(sessionTimeout);
  }
  
  public String getCookieName()
  {
    return this.cookieName;
  }
  
  public void setCookieName(String cookieName)
  {
    this.cookieName = cookieName;
  }
  
  public String getCookieDomain()
  {
    return this.cookieDomain;
  }
  
  public void setCookieDomain(String cookieDomain)
  {
    this.cookieDomain = cookieDomain;
  }
  
  public String getCookiePath()
  {
    return this.cookiePath;
  }
  
  public void setCookiePath(String cookiePath)
  {
    this.cookiePath = cookiePath;
  }
  
  public String getCookieComment()
  {
    return this.cookieComment;
  }
  
  public void setCookieComment(String cookieComment)
  {
    this.cookieComment = cookieComment;
  }
  
  public Boolean getCookieHttpOnly()
  {
    return this.cookieHttpOnly;
  }
  
  public void setCookieHttpOnly(String cookieHttpOnly)
  {
    this.cookieHttpOnly = Boolean.valueOf(cookieHttpOnly);
  }
  
  public Boolean getCookieSecure()
  {
    return this.cookieSecure;
  }
  
  public void setCookieSecure(String cookieSecure)
  {
    this.cookieSecure = Boolean.valueOf(cookieSecure);
  }
  
  public Integer getCookieMaxAge()
  {
    return this.cookieMaxAge;
  }
  
  public void setCookieMaxAge(String cookieMaxAge)
  {
    this.cookieMaxAge = Integer.valueOf(cookieMaxAge);
  }
  
  public EnumSet<SessionTrackingMode> getSessionTrackingModes()
  {
    return this.sessionTrackingModes;
  }
  
  public void addSessionTrackingMode(String sessionTrackingMode)
  {
    this.sessionTrackingModes.add(SessionTrackingMode.valueOf(sessionTrackingMode));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\SessionConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */