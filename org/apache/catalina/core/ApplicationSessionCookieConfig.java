package org.apache.catalina.core;

import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.util.SessionConfig;
import org.apache.tomcat.util.res.StringManager;

public class ApplicationSessionCookieConfig
  implements SessionCookieConfig
{
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private boolean httpOnly;
  private boolean secure;
  private int maxAge = -1;
  private String comment;
  private String domain;
  private String name;
  private String path;
  private StandardContext context;
  
  public ApplicationSessionCookieConfig(StandardContext context)
  {
    this.context = context;
  }
  
  public String getComment()
  {
    return this.comment;
  }
  
  public String getDomain()
  {
    return this.domain;
  }
  
  public int getMaxAge()
  {
    return this.maxAge;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getPath()
  {
    return this.path;
  }
  
  public boolean isHttpOnly()
  {
    return this.httpOnly;
  }
  
  public boolean isSecure()
  {
    return this.secure;
  }
  
  public void setComment(String comment)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "comment", this.context.getPath() }));
    }
    this.comment = comment;
  }
  
  public void setDomain(String domain)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "domain name", this.context.getPath() }));
    }
    this.domain = domain;
  }
  
  public void setHttpOnly(boolean httpOnly)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "HttpOnly", this.context.getPath() }));
    }
    this.httpOnly = httpOnly;
  }
  
  public void setMaxAge(int maxAge)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "max age", this.context.getPath() }));
    }
    this.maxAge = maxAge;
  }
  
  public void setName(String name)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "name", this.context.getPath() }));
    }
    this.name = name;
  }
  
  public void setPath(String path)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "path", this.context.getPath() }));
    }
    this.path = path;
  }
  
  public void setSecure(boolean secure)
  {
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationSessionCookieConfig.ise", new Object[] { "secure", this.context.getPath() }));
    }
    this.secure = secure;
  }
  
  public static Cookie createSessionCookie(Context context, String sessionId, boolean secure)
  {
    SessionCookieConfig scc = context.getServletContext().getSessionCookieConfig();
    
    Cookie cookie = new Cookie(SessionConfig.getSessionCookieName(context), sessionId);
    
    cookie.setMaxAge(scc.getMaxAge());
    cookie.setComment(scc.getComment());
    if (context.getSessionCookieDomain() == null)
    {
      if (scc.getDomain() != null) {
        cookie.setDomain(scc.getDomain());
      }
    }
    else {
      cookie.setDomain(context.getSessionCookieDomain());
    }
    if ((scc.isSecure()) || (secure)) {
      cookie.setSecure(true);
    }
    if ((scc.isHttpOnly()) || (context.getUseHttpOnly())) {
      cookie.setHttpOnly(true);
    }
    String contextPath = context.getSessionCookiePath();
    if ((contextPath == null) || (contextPath.length() == 0)) {
      contextPath = scc.getPath();
    }
    if ((contextPath == null) || (contextPath.length() == 0)) {
      contextPath = context.getEncodedPath();
    }
    if (context.getSessionCookiePathUsesTrailingSlash())
    {
      if (!contextPath.endsWith("/")) {
        contextPath = contextPath + "/";
      }
    }
    else if (contextPath.length() == 0) {
      contextPath = "/";
    }
    cookie.setPath(contextPath);
    
    return cookie;
  }
  
  @Deprecated
  public static String getSessionCookieName(Context context)
  {
    return SessionConfig.getSessionCookieName(context);
  }
  
  @Deprecated
  public static String getSessionUriParamName(Context context)
  {
    return SessionConfig.getSessionUriParamName(context);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationSessionCookieConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */