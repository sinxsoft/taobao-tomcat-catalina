package org.apache.catalina.mbeans;

import java.util.Iterator;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import org.apache.catalina.Group;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.modeler.Registry;

public class GlobalResourcesLifecycleListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(GlobalResourcesLifecycleListener.class);
  protected Lifecycle component = null;
  protected static Registry registry = MBeanUtils.createRegistry();
  
  public GlobalResourcesLifecycleListener() {}
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if ("start".equals(event.getType()))
    {
      this.component = event.getLifecycle();
      createMBeans();
    }
    else if ("stop".equals(event.getType()))
    {
      destroyMBeans();
      this.component = null;
    }
  }
  
  protected void createMBeans()
  {
    Context context = null;
    try
    {
      context = (Context)new InitialContext().lookup("java:/");
    }
    catch (NamingException e)
    {
      log.error("No global naming context defined for server");
      return;
    }
    try
    {
      createMBeans("", context);
    }
    catch (NamingException e)
    {
      log.error("Exception processing Global JNDI Resources", e);
    }
  }
  
  protected void createMBeans(String prefix, Context context)
    throws NamingException
  {
    if (log.isDebugEnabled()) {
      log.debug("Creating MBeans for Global JNDI Resources in Context '" + prefix + "'");
    }
    try
    {
      NamingEnumeration<Binding> bindings = context.listBindings("");
      while (bindings.hasMore())
      {
        Binding binding = (Binding)bindings.next();
        String name = prefix + binding.getName();
        Object value = context.lookup(binding.getName());
        if (log.isDebugEnabled()) {
          log.debug("Checking resource " + name);
        }
        if ((value instanceof Context)) {
          createMBeans(name + "/", (Context)value);
        } else if ((value instanceof UserDatabase)) {
          try
          {
            createMBeans(name, (UserDatabase)value);
          }
          catch (Exception e)
          {
            log.error("Exception creating UserDatabase MBeans for " + name, e);
          }
        }
      }
    }
    catch (RuntimeException ex)
    {
      log.error("RuntimeException " + ex);
    }
    catch (OperationNotSupportedException ex)
    {
      log.error("Operation not supported " + ex);
    }
  }
  
  protected void createMBeans(String name, UserDatabase database)
    throws Exception
  {
    if (log.isDebugEnabled())
    {
      log.debug("Creating UserDatabase MBeans for resource " + name);
      log.debug("Database=" + database);
    }
    if (MBeanUtils.createMBean(database) == null) {
      throw new IllegalArgumentException("Cannot create UserDatabase MBean for resource " + name);
    }
    Iterator<Role> roles = database.getRoles();
    while (roles.hasNext())
    {
      Role role = (Role)roles.next();
      if (log.isDebugEnabled()) {
        log.debug("  Creating Role MBean for role " + role);
      }
      if (MBeanUtils.createMBean(role) == null) {
        throw new IllegalArgumentException("Cannot create Role MBean for role " + role);
      }
    }
    Iterator<Group> groups = database.getGroups();
    while (groups.hasNext())
    {
      Group group = (Group)groups.next();
      if (log.isDebugEnabled()) {
        log.debug("  Creating Group MBean for group " + group);
      }
      if (MBeanUtils.createMBean(group) == null) {
        throw new IllegalArgumentException("Cannot create Group MBean for group " + group);
      }
    }
    Iterator<User> users = database.getUsers();
    while (users.hasNext())
    {
      User user = (User)users.next();
      if (log.isDebugEnabled()) {
        log.debug("  Creating User MBean for user " + user);
      }
      if (MBeanUtils.createMBean(user) == null) {
        throw new IllegalArgumentException("Cannot create User MBean for user " + user);
      }
    }
  }
  
  protected void destroyMBeans()
  {
    if (log.isDebugEnabled()) {
      log.debug("Destroying MBeans for Global JNDI Resources");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\GlobalResourcesLifecycleListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */