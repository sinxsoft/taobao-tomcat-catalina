package org.apache.catalina.realm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class JAASRealm
  extends RealmBase
{
  private static final Log log = LogFactory.getLog(JAASRealm.class);
  protected String appName = null;
  protected static final String info = "org.apache.catalina.realm.JAASRealm/1.0";
  protected static final String name = "JAASRealm";
  protected List<String> roleClasses = new ArrayList();
  protected List<String> userClasses = new ArrayList();
  protected boolean useContextClassLoader = true;
  protected String configFile;
  protected Configuration jaasConfiguration;
  protected volatile boolean jaasConfigurationLoaded = false;
  
  public JAASRealm() {}
  
  public String getConfigFile()
  {
    return this.configFile;
  }
  
  public void setConfigFile(String configFile)
  {
    this.configFile = configFile;
  }
  
  public void setAppName(String name)
  {
    this.appName = name;
  }
  
  public String getAppName()
  {
    return this.appName;
  }
  
  public void setUseContextClassLoader(boolean useContext)
  {
    this.useContextClassLoader = useContext;
    log.info("Setting useContextClassLoader = " + useContext);
  }
  
  public boolean isUseContextClassLoader()
  {
    return this.useContextClassLoader;
  }
  
  public void setContainer(Container container)
  {
    super.setContainer(container);
    if (this.appName == null)
    {
      String name = container.getName();
      if (!name.startsWith("/")) {
        name = "/" + name;
      }
      name = makeLegalForJAAS(name);
      
      this.appName = name;
      
      log.info("Set JAAS app name " + this.appName);
    }
  }
  
  protected String roleClassNames = null;
  
  public String getRoleClassNames()
  {
    return this.roleClassNames;
  }
  
  public void setRoleClassNames(String roleClassNames)
  {
    this.roleClassNames = roleClassNames;
  }
  
  protected void parseClassNames(String classNamesString, List<String> classNamesList)
  {
    classNamesList.clear();
    if (classNamesString == null) {
      return;
    }
    ClassLoader loader = getClass().getClassLoader();
    if (isUseContextClassLoader()) {
      loader = Thread.currentThread().getContextClassLoader();
    }
    String[] classNames = classNamesString.split("[ ]*,[ ]*");
    for (int i = 0; i < classNames.length; i++) {
      if (classNames[i].length() != 0) {
        try
        {
          Class<?> principalClass = Class.forName(classNames[i], false, loader);
          if (Principal.class.isAssignableFrom(principalClass)) {
            classNamesList.add(classNames[i]);
          } else {
            log.error("Class " + classNames[i] + " is not implementing " + "java.security.Principal! Class not added.");
          }
        }
        catch (ClassNotFoundException e)
        {
          log.error("Class " + classNames[i] + " not found! Class not added.");
        }
      }
    }
  }
  
  protected String userClassNames = null;
  
  public String getUserClassNames()
  {
    return this.userClassNames;
  }
  
  public void setUserClassNames(String userClassNames)
  {
    this.userClassNames = userClassNames;
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.realm.JAASRealm/1.0";
  }
  
  public Principal authenticate(String username, String credentials)
  {
    return authenticate(username, new JAASCallbackHandler(this, username, credentials));
  }
  
  public Principal authenticate(String username, String clientDigest, String nonce, String nc, String cnonce, String qop, String realmName, String md5a2)
  {
    return authenticate(username, new JAASCallbackHandler(this, username, clientDigest, nonce, nc, cnonce, qop, realmName, md5a2, "DIGEST"));
  }
  
  protected Principal authenticate(String username, CallbackHandler callbackHandler)
  {
    try
    {
      LoginContext loginContext = null;
      if (this.appName == null) {
        this.appName = "Tomcat";
      }
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("jaasRealm.beginLogin", new Object[] { username, this.appName }));
      }
      ClassLoader ocl = null;
      if (!isUseContextClassLoader())
      {
        ocl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      }
      try
      {
        Configuration config = getConfig();
        loginContext = new LoginContext(this.appName, null, callbackHandler, config);
      }
      catch (Throwable e)
      {
        ExceptionUtils.handleThrowable(e);
        log.error(sm.getString("jaasRealm.unexpectedError"), e);
        return null;
      }
      finally
      {
        if (!isUseContextClassLoader()) {
          Thread.currentThread().setContextClassLoader(ocl);
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Login context created " + username);
      }
      Subject subject = null;
      try
      {
        loginContext.login();
        subject = loginContext.getSubject();
        if (subject == null)
        {
          if (log.isDebugEnabled()) {
            log.debug(sm.getString("jaasRealm.failedLogin", new Object[] { username }));
          }
          return null;
        }
      }
      catch (AccountExpiredException e)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("jaasRealm.accountExpired", new Object[] { username }));
        }
        return null;
      }
      catch (CredentialExpiredException e)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("jaasRealm.credentialExpired", new Object[] { username }));
        }
        return null;
      }
      catch (FailedLoginException e)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("jaasRealm.failedLogin", new Object[] { username }));
        }
        return null;
      }
      catch (LoginException e)
      {
        log.warn(sm.getString("jaasRealm.loginException", new Object[] { username }), e);
        return null;
      }
      catch (Throwable e)
      {
        ExceptionUtils.handleThrowable(e);
        log.error(sm.getString("jaasRealm.unexpectedError"), e);
        return null;
      }
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("jaasRealm.loginContextCreated", new Object[] { username }));
      }
      Principal principal = createPrincipal(username, subject, loginContext);
      if (principal == null)
      {
        log.debug(sm.getString("jaasRealm.authenticateFailure", new Object[] { username }));
        return null;
      }
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("jaasRealm.authenticateSuccess", new Object[] { username }));
      }
      return principal;
    }
    catch (Throwable t)
    {
      log.error("error ", t);
    }
    return null;
  }
  
  protected String getName()
  {
    return "JAASRealm";
  }
  
  protected String getPassword(String username)
  {
    return null;
  }
  
  protected Principal getPrincipal(String username)
  {
    return authenticate(username, new JAASCallbackHandler(this, username, null, null, null, null, null, null, null, "CLIENT_CERT"));
  }
  
  protected Principal createPrincipal(String username, Subject subject, LoginContext loginContext)
  {
    List<String> roles = new ArrayList();
    Principal userPrincipal = null;
    
    Iterator<Principal> principals = subject.getPrincipals().iterator();
    while (principals.hasNext())
    {
      Principal principal = (Principal)principals.next();
      
      String principalClass = principal.getClass().getName();
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("jaasRealm.checkPrincipal", new Object[] { principal, principalClass }));
      }
      if ((userPrincipal == null) && (this.userClasses.contains(principalClass)))
      {
        userPrincipal = principal;
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("jaasRealm.userPrincipalSuccess", new Object[] { principal.getName() }));
        }
      }
      if (this.roleClasses.contains(principalClass))
      {
        roles.add(principal.getName());
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("jaasRealm.rolePrincipalAdd", new Object[] { principal.getName() }));
        }
      }
    }
    if (userPrincipal == null)
    {
      if (log.isDebugEnabled())
      {
        log.debug(sm.getString("jaasRealm.userPrincipalFailure"));
        log.debug(sm.getString("jaasRealm.rolePrincipalFailure"));
      }
    }
    else if ((roles.size() == 0) && 
      (log.isDebugEnabled())) {
      log.debug(sm.getString("jaasRealm.rolePrincipalFailure"));
    }
    return new GenericPrincipal(username, null, roles, userPrincipal, loginContext);
  }
  
  protected String makeLegalForJAAS(String src)
  {
    String result = src;
    if (result == null) {
      result = "other";
    }
    if (result.startsWith("/")) {
      result = result.substring(1);
    }
    return result;
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    parseClassNames(this.userClassNames, this.userClasses);
    parseClassNames(this.roleClassNames, this.roleClasses);
    
    super.startInternal();
  }
  
  /* Error */
  protected Configuration getConfig()
  {
	  try {
          if (jaasConfigurationLoaded) {
              return jaasConfiguration;
          }
          synchronized (this) {
              if (configFile == null) {
                  jaasConfigurationLoaded = true;
                  return null;
              }
              URL resource = Thread.currentThread().getContextClassLoader().
                      getResource(configFile);
              URI uri = resource.toURI();
              @SuppressWarnings("unchecked")
              Class<Configuration> sunConfigFile = (Class<Configuration>)
                      Class.forName("com.sun.security.auth.login.ConfigFile");
              Constructor<Configuration> constructor =
                      sunConfigFile.getConstructor(URI.class);
              Configuration config = constructor.newInstance(uri);
              this.jaasConfiguration = config;
              this.jaasConfigurationLoaded = true;
              return this.jaasConfiguration;
          }
      } catch (URISyntaxException ex) {
          throw new RuntimeException(ex);
      } catch (NoSuchMethodException ex) {
          throw new RuntimeException(ex);
      } catch (SecurityException ex) {
          throw new RuntimeException(ex);
      } catch (InstantiationException ex) {
          throw new RuntimeException(ex);
      } catch (IllegalAccessException ex) {
          throw new RuntimeException(ex);
      } catch (IllegalArgumentException ex) {
          throw new RuntimeException(ex);
      } catch (InvocationTargetException ex) {
          throw new RuntimeException(ex.getCause());
      } catch (ClassNotFoundException ex) {
          throw new RuntimeException(ex);
      }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\JAASRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */