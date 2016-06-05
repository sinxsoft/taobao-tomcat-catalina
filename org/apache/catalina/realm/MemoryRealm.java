package org.apache.catalina.realm;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;

public class MemoryRealm
  extends RealmBase
{
  private static final Log log = LogFactory.getLog(MemoryRealm.class);
  private static Digester digester = null;
  protected static final String info = "org.apache.catalina.realm.MemoryRealm/1.0";
  protected static final String name = "MemoryRealm";
  private String pathname = "conf/tomcat-users.xml";
  private Map<String, GenericPrincipal> principals = new HashMap();
  
  public MemoryRealm() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.realm.MemoryRealm/1.0";
  }
  
  public String getPathname()
  {
    return this.pathname;
  }
  
  public void setPathname(String pathname)
  {
    this.pathname = pathname;
  }
  
  public Principal authenticate(String username, String credentials)
  {
    GenericPrincipal principal = (GenericPrincipal)this.principals.get(username);
    boolean validated;

    if (principal == null) {
      validated = false;
    } else {
      validated = compareCredentials(credentials, principal.getPassword());
    }
    if (validated)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("memoryRealm.authenticateSuccess", new Object[] { username }));
      }
      return principal;
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("memoryRealm.authenticateFailure", new Object[] { username }));
    }
    return null;
  }
  
  void addUser(String username, String password, String roles)
  {
    ArrayList<String> list = new ArrayList();
    roles = roles + ",";
    for (;;)
    {
      int comma = roles.indexOf(',');
      if (comma < 0) {
        break;
      }
      String role = roles.substring(0, comma).trim();
      list.add(role);
      roles = roles.substring(comma + 1);
    }
    GenericPrincipal principal = new GenericPrincipal(username, password, list);
    
    this.principals.put(username, principal);
  }
  
  protected synchronized Digester getDigester()
  {
    if (digester == null)
    {
      digester = new Digester();
      digester.setValidating(false);
      try
      {
        digester.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
      }
      catch (Exception e)
      {
        log.warn(sm.getString("memoryRealm.xmlFeatureEncoding"), e);
      }
      digester.addRuleSet(new MemoryRuleSet());
    }
    return digester;
  }
  
  protected String getName()
  {
    return "MemoryRealm";
  }
  
  protected String getPassword(String username)
  {
    GenericPrincipal principal = (GenericPrincipal)this.principals.get(username);
    if (principal != null) {
      return principal.getPassword();
    }
    return null;
  }
  
  protected Principal getPrincipal(String username)
  {
    return (Principal)this.principals.get(username);
  }
  
  @Deprecated
  protected Map<String, GenericPrincipal> getPrincipals()
  {
    return this.principals;
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    File file = new File(this.pathname);
    if (!file.isAbsolute()) {
      file = new File(System.getProperty("catalina.base"), this.pathname);
    }
    if ((!file.exists()) || (!file.canRead())) {
      throw new LifecycleException(sm.getString("memoryRealm.loadExist", new Object[] { file.getAbsolutePath() }));
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("memoryRealm.loadPath", new Object[] { file.getAbsolutePath() }));
    }
    Digester digester = getDigester();
    try
    {
      synchronized (digester)
      {
        digester.push(this);
        digester.parse(file);
      }
    }
    catch (Exception e)
    {
      throw new LifecycleException(sm.getString("memoryRealm.readXml"), e);
    }
    finally
    {
      digester.reset();
    }
    super.startInternal();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\MemoryRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */