package org.apache.catalina.realm;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;

public class JAASMemoryLoginModule
  extends MemoryRealm
  implements LoginModule
{
  private static final Log log = LogFactory.getLog(JAASMemoryLoginModule.class);
  protected CallbackHandler callbackHandler = null;
  protected boolean committed = false;
  protected Map<String, ?> options = null;
  protected String pathname = "conf/tomcat-users.xml";
  protected Principal principal = null;
  protected Map<String, ?> sharedState = null;
  protected Subject subject = null;
  
  public JAASMemoryLoginModule()
  {
    log.debug("MEMORY LOGIN MODULE");
  }
  
  public boolean abort()
    throws LoginException
  {
    if (this.principal == null) {
      return false;
    }
    if (this.committed)
    {
      logout();
    }
    else
    {
      this.committed = false;
      this.principal = null;
    }
    log.debug("Abort");
    return true;
  }
  
  public boolean commit()
    throws LoginException
  {
    log.debug("commit " + this.principal);
    if (this.principal == null) {
      return false;
    }
    if (!this.subject.getPrincipals().contains(this.principal))
    {
      this.subject.getPrincipals().add(this.principal);
      if ((this.principal instanceof GenericPrincipal))
      {
        String[] roles = ((GenericPrincipal)this.principal).getRoles();
        for (int i = 0; i < roles.length; i++) {
          this.subject.getPrincipals().add(new GenericPrincipal(roles[i], null, null));
        }
      }
    }
    this.committed = true;
    return true;
  }
  
  public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options)
  {
    log.debug("Init");
    
    this.subject = subject;
    this.callbackHandler = callbackHandler;
    this.sharedState = sharedState;
    this.options = options;
    if (options.get("pathname") != null) {
      this.pathname = ((String)options.get("pathname"));
    }
    load();
  }
  
  public boolean login()
    throws LoginException
  {
    if (this.callbackHandler == null) {
      throw new LoginException("No CallbackHandler specified");
    }
    Callback[] callbacks = new Callback[9];
    callbacks[0] = new NameCallback("Username: ");
    callbacks[1] = new PasswordCallback("Password: ", false);
    callbacks[2] = new TextInputCallback("nonce");
    callbacks[3] = new TextInputCallback("nc");
    callbacks[4] = new TextInputCallback("cnonce");
    callbacks[5] = new TextInputCallback("qop");
    callbacks[6] = new TextInputCallback("realmName");
    callbacks[7] = new TextInputCallback("md5a2");
    callbacks[8] = new TextInputCallback("authMethod");
    
    String username = null;
    String password = null;
    String nonce = null;
    String nc = null;
    String cnonce = null;
    String qop = null;
    String realmName = null;
    String md5a2 = null;
    String authMethod = null;
    try
    {
      this.callbackHandler.handle(callbacks);
      username = ((NameCallback)callbacks[0]).getName();
      password = new String(((PasswordCallback)callbacks[1]).getPassword());
      
      nonce = ((TextInputCallback)callbacks[2]).getText();
      nc = ((TextInputCallback)callbacks[3]).getText();
      cnonce = ((TextInputCallback)callbacks[4]).getText();
      qop = ((TextInputCallback)callbacks[5]).getText();
      realmName = ((TextInputCallback)callbacks[6]).getText();
      md5a2 = ((TextInputCallback)callbacks[7]).getText();
      authMethod = ((TextInputCallback)callbacks[8]).getText();
    }
    catch (IOException e)
    {
      throw new LoginException(e.toString());
    }
    catch (UnsupportedCallbackException e)
    {
      throw new LoginException(e.toString());
    }
    if (authMethod == null) {
      this.principal = super.authenticate(username, password);
    } else if (authMethod.equals("DIGEST")) {
      this.principal = super.authenticate(username, password, nonce, nc, cnonce, qop, realmName, md5a2);
    } else if (authMethod.equals("CLIENT_CERT")) {
      this.principal = super.getPrincipal(username);
    } else {
      throw new LoginException("Unknown authentication method");
    }
    log.debug("login " + username + " " + this.principal);
    if (this.principal != null) {
      return true;
    }
    throw new FailedLoginException("Username or password is incorrect");
  }
  
  public boolean logout()
    throws LoginException
  {
    this.subject.getPrincipals().remove(this.principal);
    this.committed = false;
    this.principal = null;
    return true;
  }
  
  protected void load()
  {
    File file = new File(this.pathname);
    if (!file.isAbsolute()) {
      file = new File(System.getProperty("catalina.base"), this.pathname);
    }
    if ((!file.exists()) || (!file.canRead()))
    {
      log.warn("Cannot load configuration file " + file.getAbsolutePath());
      return;
    }
    Digester digester = new Digester();
    digester.setValidating(false);
    digester.addRuleSet(new MemoryRuleSet());
    try
    {
      digester.push(this);
      digester.parse(file);
    }
    catch (Exception e)
    {
      log.warn("Error processing configuration file " + file.getAbsolutePath(), e);
    }
    finally
    {
      digester.reset();
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\JAASMemoryLoginModule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */