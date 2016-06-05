package org.apache.catalina.mbeans;

import java.util.ArrayList;
import java.util.Iterator;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.tomcat.util.modeler.BaseModelMBean;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

public class MemoryUserDatabaseMBean
  extends BaseModelMBean
{
  protected Registry registry = MBeanUtils.createRegistry();
  protected ManagedBean managed = this.registry.findManagedBean("MemoryUserDatabase");
  protected ManagedBean managedGroup = this.registry.findManagedBean("Group");
  protected ManagedBean managedRole = this.registry.findManagedBean("Role");
  protected ManagedBean managedUser = this.registry.findManagedBean("User");
  
  public MemoryUserDatabaseMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public String[] getGroups()
  {
    UserDatabase database = (UserDatabase)this.resource;
    ArrayList<String> results = new ArrayList();
    Iterator<Group> groups = database.getGroups();
    while (groups.hasNext())
    {
      Group group = (Group)groups.next();
      results.add(findGroup(group.getGroupname()));
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String[] getRoles()
  {
    UserDatabase database = (UserDatabase)this.resource;
    ArrayList<String> results = new ArrayList();
    Iterator<Role> roles = database.getRoles();
    while (roles.hasNext())
    {
      Role role = (Role)roles.next();
      results.add(findRole(role.getRolename()));
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String[] getUsers()
  {
    UserDatabase database = (UserDatabase)this.resource;
    ArrayList<String> results = new ArrayList();
    Iterator<User> users = database.getUsers();
    while (users.hasNext())
    {
      User user = (User)users.next();
      results.add(findUser(user.getUsername()));
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String createGroup(String groupname, String description)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Group group = database.createGroup(groupname, description);
    try
    {
      MBeanUtils.createMBean(group);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception creating group [" + groupname + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
    return findGroup(groupname);
  }
  
  public String createRole(String rolename, String description)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Role role = database.createRole(rolename, description);
    try
    {
      MBeanUtils.createMBean(role);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception creating role [" + rolename + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
    return findRole(rolename);
  }
  
  public String createUser(String username, String password, String fullName)
  {
    UserDatabase database = (UserDatabase)this.resource;
    User user = database.createUser(username, password, fullName);
    try
    {
      MBeanUtils.createMBean(user);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception creating user [" + username + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
    return findUser(username);
  }
  
  public String findGroup(String groupname)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Group group = database.findGroup(groupname);
    if (group == null) {
      return null;
    }
    try
    {
      ObjectName oname = MBeanUtils.createObjectName(this.managedGroup.getDomain(), group);
      
      return oname.toString();
    }
    catch (MalformedObjectNameException e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for group [" + groupname + "]");
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  public String findRole(String rolename)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Role role = database.findRole(rolename);
    if (role == null) {
      return null;
    }
    try
    {
      ObjectName oname = MBeanUtils.createObjectName(this.managedRole.getDomain(), role);
      
      return oname.toString();
    }
    catch (MalformedObjectNameException e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for role [" + rolename + "]");
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  public String findUser(String username)
  {
    UserDatabase database = (UserDatabase)this.resource;
    User user = database.findUser(username);
    if (user == null) {
      return null;
    }
    try
    {
      ObjectName oname = MBeanUtils.createObjectName(this.managedUser.getDomain(), user);
      
      return oname.toString();
    }
    catch (MalformedObjectNameException e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for user [" + username + "]");
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  public void removeGroup(String groupname)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Group group = database.findGroup(groupname);
    if (group == null) {
      return;
    }
    try
    {
      MBeanUtils.destroyMBean(group);
      database.removeGroup(group);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception destroying group [" + groupname + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  public void removeRole(String rolename)
  {
    UserDatabase database = (UserDatabase)this.resource;
    Role role = database.findRole(rolename);
    if (role == null) {
      return;
    }
    try
    {
      MBeanUtils.destroyMBean(role);
      database.removeRole(role);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception destroying role [" + rolename + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
  }
  
  public void removeUser(String username)
  {
    UserDatabase database = (UserDatabase)this.resource;
    User user = database.findUser(username);
    if (user == null) {
      return;
    }
    try
    {
      MBeanUtils.destroyMBean(user);
      database.removeUser(user);
    }
    catch (Exception e)
    {
      IllegalArgumentException iae = new IllegalArgumentException("Exception destroying user [" + username + "] MBean");
      
      iae.initCause(e);
      throw iae;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\MemoryUserDatabaseMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */