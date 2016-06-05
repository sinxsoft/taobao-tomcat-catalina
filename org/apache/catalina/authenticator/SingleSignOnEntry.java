package org.apache.catalina.authenticator;

import java.security.Principal;
import org.apache.catalina.Session;

public class SingleSignOnEntry
{
  protected String authType = null;
  protected String password = null;
  protected Principal principal = null;
  protected Session[] sessions = new Session[0];
  protected String username = null;
  protected boolean canReauthenticate = false;
  
  public SingleSignOnEntry(Principal principal, String authType, String username, String password)
  {
    updateCredentials(principal, authType, username, password);
  }
  
  public synchronized void addSession(SingleSignOn sso, Session session)
  {
    for (int i = 0; i < this.sessions.length; i++) {
      if (session == this.sessions[i]) {
        return;
      }
    }
    Session[] results = new Session[this.sessions.length + 1];
    System.arraycopy(this.sessions, 0, results, 0, this.sessions.length);
    results[this.sessions.length] = session;
    this.sessions = results;
    session.addSessionListener(sso);
  }
  
  public synchronized void removeSession(Session session)
  {
    Session[] nsessions = new Session[this.sessions.length - 1];
    int i = 0;
    for (int j = 0; i < this.sessions.length; i++) {
      if (session != this.sessions[i]) {
        nsessions[(j++)] = this.sessions[i];
      }
    }
    this.sessions = nsessions;
  }
  
  public synchronized Session[] findSessions()
  {
    return this.sessions;
  }
  
  public String getAuthType()
  {
    return this.authType;
  }
  
  public boolean getCanReauthenticate()
  {
    return this.canReauthenticate;
  }
  
  public String getPassword()
  {
    return this.password;
  }
  
  public Principal getPrincipal()
  {
    return this.principal;
  }
  
  public String getUsername()
  {
    return this.username;
  }
  
  public void updateCredentials(Principal principal, String authType, String username, String password)
  {
    this.principal = principal;
    this.authType = authType;
    this.username = username;
    this.password = password;
    this.canReauthenticate = (("BASIC".equals(authType)) || ("FORM".equals(authType)));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\authenticator\SingleSignOnEntry.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */