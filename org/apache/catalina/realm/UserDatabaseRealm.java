package org.apache.catalina.realm;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.naming.Context;
import org.apache.catalina.Group;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Role;
import org.apache.catalina.Server;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.Wrapper;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class UserDatabaseRealm
  extends RealmBase
{
  protected UserDatabase database = null;
  protected static final String info = "org.apache.catalina.realm.UserDatabaseRealm/1.0";
  protected static final String name = "UserDatabaseRealm";
  protected String resourceName = "UserDatabase";
  
  public UserDatabaseRealm() {}
  
  public String getInfo()
  {
    return "org.apache.catalina.realm.UserDatabaseRealm/1.0";
  }
  
  public String getResourceName()
  {
    return this.resourceName;
  }
  
  public void setResourceName(String resourceName)
  {
    this.resourceName = resourceName;
  }
  
  public boolean hasRole(Wrapper wrapper, Principal principal, String role)
  {
    if (wrapper != null)
    {
      String realRole = wrapper.findSecurityReference(role);
      if (realRole != null) {
        role = realRole;
      }
    }
    if ((principal instanceof GenericPrincipal))
    {
      GenericPrincipal gp = (GenericPrincipal)principal;
      if ((gp.getUserPrincipal() instanceof User)) {
        principal = gp.getUserPrincipal();
      }
    }
    if (!(principal instanceof User)) {
      return super.hasRole(null, principal, role);
    }
    if ("*".equals(role)) {
      return true;
    }
    if (role == null) {
      return false;
    }
    User user = (User)principal;
    Role dbrole = this.database.findRole(role);
    if (dbrole == null) {
      return false;
    }
    if (user.isInRole(dbrole)) {
      return true;
    }
    Iterator<Group> groups = user.getGroups();
    while (groups.hasNext())
    {
      Group group = (Group)groups.next();
      if (group.isInRole(dbrole)) {
        return true;
      }
    }
    return false;
  }
  
  protected String getName()
  {
    return "UserDatabaseRealm";
  }
  
  protected String getPassword(String username)
  {
    User user = this.database.findUser(username);
    if (user == null) {
      return null;
    }
    return user.getPassword();
  }
  
  protected Principal getPrincipal(String username)
  {
    User user = this.database.findUser(username);
    if (user == null) {
      return null;
    }
    List<String> roles = new ArrayList();
    Iterator<Role> uroles = user.getRoles();
    while (uroles.hasNext())
    {
      Role role = (Role)uroles.next();
      roles.add(role.getName());
    }
    Iterator<Group> groups = user.getGroups();
    while (groups.hasNext())
    {
      Group group = (Group)groups.next();
      uroles = group.getRoles();
      while (uroles.hasNext())
      {
        Role role = (Role)uroles.next();
        roles.add(role.getName());
      }
    }
    return new GenericPrincipal(username, user.getPassword(), roles, user);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    try
    {
      Context context = getServer().getGlobalNamingContext();
      this.database = ((UserDatabase)context.lookup(this.resourceName));
    }
    catch (Throwable e)
    {
      ExceptionUtils.handleThrowable(e);
      this.containerLog.error(sm.getString("userDatabaseRealm.lookup", new Object[] { this.resourceName }), e);
      
      this.database = null;
    }
    if (this.database == null) {
      throw new LifecycleException(sm.getString("userDatabaseRealm.noDatabase", new Object[] { this.resourceName }));
    }
    super.startInternal();
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    super.stopInternal();
    
    this.database = null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\UserDatabaseRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */