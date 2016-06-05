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

public class GroupMBean
  extends BaseModelMBean
{
  protected Registry registry = MBeanUtils.createRegistry();
  protected ManagedBean managed = this.registry.findManagedBean("Group");
  
  public GroupMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public String[] getRoles()
  {
    Group group = (Group)this.resource;
    ArrayList<String> results = new ArrayList();
    Iterator<Role> roles = group.getRoles();
    while (roles.hasNext())
    {
      Role role = null;
      try
      {
        role = (Role)roles.next();
        ObjectName oname = MBeanUtils.createObjectName(this.managed.getDomain(), role);
        
        results.add(oname.toString());
      }
      catch (MalformedObjectNameException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for role " + role);
        
        iae.initCause(e);
        throw iae;
      }
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String[] getUsers()
  {
    Group group = (Group)this.resource;
    ArrayList<String> results = new ArrayList();
    Iterator<User> users = group.getUsers();
    while (users.hasNext())
    {
      User user = null;
      try
      {
        user = (User)users.next();
        ObjectName oname = MBeanUtils.createObjectName(this.managed.getDomain(), user);
        
        results.add(oname.toString());
      }
      catch (MalformedObjectNameException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for user " + user);
        
        iae.initCause(e);
        throw iae;
      }
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public void addRole(String rolename)
  {
    Group group = (Group)this.resource;
    if (group == null) {
      return;
    }
    Role role = group.getUserDatabase().findRole(rolename);
    if (role == null) {
      throw new IllegalArgumentException("Invalid role name '" + rolename + "'");
    }
    group.addRole(role);
  }
  
  public void removeRole(String rolename)
  {
    Group group = (Group)this.resource;
    if (group == null) {
      return;
    }
    Role role = group.getUserDatabase().findRole(rolename);
    if (role == null) {
      throw new IllegalArgumentException("Invalid role name '" + rolename + "'");
    }
    group.removeRole(role);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\GroupMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */