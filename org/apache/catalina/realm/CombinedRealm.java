package org.apache.catalina.realm;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.management.ObjectName;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

public class CombinedRealm
  extends RealmBase
{
  private static final Log log = LogFactory.getLog(CombinedRealm.class);
  protected List<Realm> realms = new LinkedList();
  protected static final String name = "CombinedRealm";
  
  public CombinedRealm() {}
  
  public void addRealm(Realm theRealm)
  {
    this.realms.add(theRealm);
    if (log.isDebugEnabled()) {
      sm.getString("combinedRealm.addRealm", new Object[] { theRealm.getInfo(), Integer.toString(this.realms.size()) });
    }
  }
  
  public ObjectName[] getRealms()
  {
    ObjectName[] result = new ObjectName[this.realms.size()];
    for (Realm realm : this.realms) {
      if ((realm instanceof RealmBase)) {
        result[this.realms.indexOf(realm)] = ((RealmBase)realm).getObjectName();
      }
    }
    return result;
  }
  
  public Principal authenticate(String username, String clientDigest, String nonce, String nc, String cnonce, String qop, String realmName, String md5a2)
  {
    Principal authenticatedUser = null;
    for (Realm realm : this.realms)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("combinedRealm.authStart", new Object[] { username, realm.getInfo() }));
      }
      authenticatedUser = realm.authenticate(username, clientDigest, nonce, nc, cnonce, qop, realmName, md5a2);
      if (authenticatedUser == null)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("combinedRealm.authFail", new Object[] { username, realm.getInfo() }));
        }
      }
      else
      {
        if (!log.isDebugEnabled()) {
          break;
        }
        log.debug(sm.getString("combinedRealm.authSuccess", new Object[] { username, realm.getInfo() })); break;
      }
    }
    return authenticatedUser;
  }
  
  public Principal authenticate(String username, String credentials)
  {
    Principal authenticatedUser = null;
    for (Realm realm : this.realms)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("combinedRealm.authStart", new Object[] { username, realm.getInfo() }));
      }
      authenticatedUser = realm.authenticate(username, credentials);
      if (authenticatedUser == null)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("combinedRealm.authFail", new Object[] { username, realm.getInfo() }));
        }
      }
      else
      {
        if (!log.isDebugEnabled()) {
          break;
        }
        log.debug(sm.getString("combinedRealm.authSuccess", new Object[] { username, realm.getInfo() })); break;
      }
    }
    return authenticatedUser;
  }
  
  public void setContainer(Container container)
  {
    for (Realm realm : this.realms)
    {
      if ((realm instanceof RealmBase)) {
        ((RealmBase)realm).setRealmPath(getRealmPath() + "/realm" + this.realms.indexOf(realm));
      }
      realm.setContainer(container);
    }
    super.setContainer(container);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    Iterator<Realm> iter = this.realms.iterator();
    while (iter.hasNext())
    {
      Realm realm = (Realm)iter.next();
      if ((realm instanceof Lifecycle)) {
        try
        {
          ((Lifecycle)realm).start();
        }
        catch (LifecycleException e)
        {
          iter.remove();
          log.error(sm.getString("combinedRealm.realmStartFail", new Object[] { realm.getInfo() }), e);
        }
      }
    }
    super.startInternal();
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    super.stopInternal();
    for (Realm realm : this.realms) {
      if ((realm instanceof Lifecycle)) {
        ((Lifecycle)realm).stop();
      }
    }
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    for (Realm realm : this.realms) {
      if ((realm instanceof Lifecycle)) {
        ((Lifecycle)realm).destroy();
      }
    }
    super.destroyInternal();
  }
  
  public Principal authenticate(X509Certificate[] certs)
  {
    Principal authenticatedUser = null;
    String username = null;
    if ((certs != null) && (certs.length > 0)) {
      username = certs[0].getSubjectDN().getName();
    }
    for (Realm realm : this.realms)
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("combinedRealm.authStart", new Object[] { username, realm.getInfo() }));
      }
      authenticatedUser = realm.authenticate(certs);
      if (authenticatedUser == null)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("combinedRealm.authFail", new Object[] { username, realm.getInfo() }));
        }
      }
      else
      {
        if (!log.isDebugEnabled()) {
          break;
        }
        log.debug(sm.getString("combinedRealm.authSuccess", new Object[] { username, realm.getInfo() })); break;
      }
    }
    return authenticatedUser;
  }
  
  public Principal authenticate(GSSContext gssContext, boolean storeCreds)
  {
    if (gssContext.isEstablished())
    {
      Principal authenticatedUser = null;
      String username = null;
      
      GSSName name = null;
      try
      {
        name = gssContext.getSrcName();
      }
      catch (GSSException e)
      {
        log.warn(sm.getString("realmBase.gssNameFail"), e);
        return null;
      }
      username = name.toString();
      for (Realm realm : this.realms)
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("combinedRealm.authStart", new Object[] { username, realm.getInfo() }));
        }
        authenticatedUser = realm.authenticate(gssContext, storeCreds);
        if (authenticatedUser == null)
        {
          if (log.isDebugEnabled()) {
            log.debug(sm.getString("combinedRealm.authFail", new Object[] { username, realm.getInfo() }));
          }
        }
        else
        {
          if (!log.isDebugEnabled()) {
            break;
          }
          log.debug(sm.getString("combinedRealm.authSuccess", new Object[] { username, realm.getInfo() })); break;
        }
      }
      return authenticatedUser;
    }
    return null;
  }
  
  protected String getName()
  {
    return "CombinedRealm";
  }
  
  protected String getPassword(String username)
  {
    UnsupportedOperationException uoe = new UnsupportedOperationException(sm.getString("combinedRealm.getPassword"));
    
    log.error(sm.getString("combinedRealm.unexpectedMethod"), uoe);
    throw uoe;
  }
  
  protected Principal getPrincipal(String username)
  {
    UnsupportedOperationException uoe = new UnsupportedOperationException(sm.getString("combinedRealm.getPrincipal"));
    
    log.error(sm.getString("combinedRealm.unexpectedMethod"), uoe);
    throw uoe;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\CombinedRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */