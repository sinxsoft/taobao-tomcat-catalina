package org.apache.catalina.mbeans;

import java.util.Hashtable;
import java.util.Set;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Group;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Role;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.ajp.AjpAprProtocol;
import org.apache.coyote.ajp.AjpProtocol;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

public class MBeanUtils
{
  private static String[][] exceptions = { { "org.apache.catalina.users.MemoryGroup", "Group" }, { "org.apache.catalina.users.MemoryRole", "Role" }, { "org.apache.catalina.users.MemoryUser", "User" } };
  private static Registry registry = createRegistry();
  private static MBeanServer mserver = createServer();
  
  public MBeanUtils() {}
  
  static String createManagedName(Object component)
  {
    String className = component.getClass().getName();
    for (int i = 0; i < exceptions.length; i++) {
      if (className.equals(exceptions[i][0])) {
        return exceptions[i][1];
      }
    }
    int period = className.lastIndexOf('.');
    if (period >= 0) {
      className = className.substring(period + 1);
    }
    return className;
  }
  
  public static DynamicMBean createMBean(ContextEnvironment environment)
    throws Exception
  {
    String mname = createManagedName(environment);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(environment);
    ObjectName oname = createObjectName(domain, environment);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  public static DynamicMBean createMBean(ContextResource resource)
    throws Exception
  {
    String mname = createManagedName(resource);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(resource);
    ObjectName oname = createObjectName(domain, resource);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  public static DynamicMBean createMBean(ContextResourceLink resourceLink)
    throws Exception
  {
    String mname = createManagedName(resourceLink);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(resourceLink);
    ObjectName oname = createObjectName(domain, resourceLink);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  static DynamicMBean createMBean(Group group)
    throws Exception
  {
    String mname = createManagedName(group);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(group);
    ObjectName oname = createObjectName(domain, group);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  @Deprecated
  static DynamicMBean createMBean(Loader loader)
    throws Exception
  {
    String mname = createManagedName(loader);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(loader);
    ObjectName oname = createObjectName(domain, loader);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  @Deprecated
  static DynamicMBean createMBean(MBeanFactory factory)
    throws Exception
  {
    String mname = createManagedName(factory);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(factory);
    ObjectName oname = createObjectName(domain, factory);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  @Deprecated
  static DynamicMBean createMBean(NamingResources resource)
    throws Exception
  {
    String mname = createManagedName(resource);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(resource);
    ObjectName oname = createObjectName(domain, resource);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  static DynamicMBean createMBean(Role role)
    throws Exception
  {
    String mname = createManagedName(role);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(role);
    ObjectName oname = createObjectName(domain, role);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  static DynamicMBean createMBean(User user)
    throws Exception
  {
    String mname = createManagedName(user);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(user);
    ObjectName oname = createObjectName(domain, user);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  static DynamicMBean createMBean(UserDatabase userDatabase)
    throws Exception
  {
    String mname = createManagedName(userDatabase);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null)
    {
      Exception e = new Exception("ManagedBean is not found with " + mname);
      throw new MBeanException(e);
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    DynamicMBean mbean = managed.createMBean(userDatabase);
    ObjectName oname = createObjectName(domain, userDatabase);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    mserver.registerMBean(mbean, oname);
    return mbean;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Connector connector)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    try
    {
      Object addressObj = IntrospectionUtils.getProperty(connector, "address");
      Integer port = (Integer)IntrospectionUtils.getProperty(connector, "port");
      
      StringBuilder sb = new StringBuilder(domain);
      sb.append(":type=Connector");
      sb.append(",port=");
      sb.append(port);
      if (addressObj != null)
      {
        String address = addressObj.toString();
        if (address.length() > 0)
        {
          sb.append(",address=");
          sb.append(ObjectName.quote(address));
        }
      }
      return new ObjectName(sb.toString());
    }
    catch (Exception e)
    {
      MalformedObjectNameException mone = new MalformedObjectNameException("Cannot create object name for " + connector);
      
      mone.initCause(e);
      throw mone;
    }
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Context context)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Host host = (Host)context.getParent();
    ContextName cn = new ContextName(context.getName(), false);
    name = new ObjectName(domain + ":j2eeType=WebModule,name=//" + host.getName() + cn.getDisplayName() + ",J2EEApplication=none,J2EEServer=none");
    
    return name;
  }
  
  public static ObjectName createObjectName(String domain, ContextEnvironment environment)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Object container = environment.getNamingResources().getContainer();
    if ((container instanceof Server))
    {
      name = new ObjectName(domain + ":type=Environment" + ",resourcetype=Global,name=" + environment.getName());
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=Environment" + ",resourcetype=Context,context=" + cn.getDisplayName() + ",host=" + host.getName() + ",name=" + environment.getName());
    }
    return name;
  }
  
  public static ObjectName createObjectName(String domain, ContextResource resource)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    String quotedResourceName = ObjectName.quote(resource.getName());
    Object container = resource.getNamingResources().getContainer();
    if ((container instanceof Server))
    {
      name = new ObjectName(domain + ":type=Resource" + ",resourcetype=Global,class=" + resource.getType() + ",name=" + quotedResourceName);
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=Resource" + ",resourcetype=Context,context=" + cn.getDisplayName() + ",host=" + host.getName() + ",class=" + resource.getType() + ",name=" + quotedResourceName);
    }
    return name;
  }
  
  public static ObjectName createObjectName(String domain, ContextResourceLink resourceLink)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    String quotedResourceLinkName = ObjectName.quote(resourceLink.getName());
    
    Object container = resourceLink.getNamingResources().getContainer();
    if ((container instanceof Server))
    {
      name = new ObjectName(domain + ":type=ResourceLink" + ",resourcetype=Global" + ",name=" + quotedResourceLinkName);
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=ResourceLink" + ",resourcetype=Context,context=" + cn.getDisplayName() + ",host=" + host.getName() + ",name=" + quotedResourceLinkName);
    }
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Engine engine)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Engine");
    return name;
  }
  
  static ObjectName createObjectName(String domain, Group group)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Group,groupname=" + ObjectName.quote(group.getGroupname()) + ",database=" + group.getUserDatabase().getId());
    
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Host host)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Host,host=" + host.getName());
    
    return name;
  }
  
  static ObjectName createObjectName(String domain, Loader loader)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Container container = loader.getContainer();
    if ((container instanceof Engine))
    {
      name = new ObjectName(domain + ":type=Loader");
    }
    else if ((container instanceof Host))
    {
      name = new ObjectName(domain + ":type=Loader,host=" + container.getName());
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=Loader,context=" + cn.getDisplayName() + ",host=" + host.getName());
    }
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Manager manager)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Container container = manager.getContainer();
    if ((container instanceof Engine))
    {
      name = new ObjectName(domain + ":type=Manager");
    }
    else if ((container instanceof Host))
    {
      name = new ObjectName(domain + ":type=Manager,host=" + container.getName());
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=Manager,context=" + cn.getDisplayName() + ",host=" + host.getName());
    }
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, NamingResources resources)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Object container = resources.getContainer();
    if ((container instanceof Server))
    {
      name = new ObjectName(domain + ":type=NamingResources" + ",resourcetype=Global");
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=NamingResources" + ",resourcetype=Context,context=" + cn.getDisplayName() + ",host=" + host.getName());
    }
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, MBeanFactory factory)
    throws MalformedObjectNameException
  {
    ObjectName name = new ObjectName(domain + ":type=MBeanFactory");
    
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Realm realm)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    Container container = realm.getContainer();
    if ((container instanceof Engine))
    {
      name = new ObjectName(domain + ":type=Realm");
    }
    else if ((container instanceof Host))
    {
      name = new ObjectName(domain + ":type=Realm,host=" + container.getName());
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      name = new ObjectName(domain + ":type=Realm,context=" + cn.getDisplayName() + ",host=" + host.getName());
    }
    return name;
  }
  
  static ObjectName createObjectName(String domain, Role role)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Role,rolename=" + role.getRolename() + ",database=" + role.getUserDatabase().getId());
    
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Server server)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Server");
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Service service)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=Service,serviceName=" + service.getName());
    
    return name;
  }
  
  static ObjectName createObjectName(String domain, User user)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=User,username=" + ObjectName.quote(user.getUsername()) + ",database=" + user.getUserDatabase().getId());
    
    return name;
  }
  
  static ObjectName createObjectName(String domain, UserDatabase userDatabase)
    throws MalformedObjectNameException
  {
    ObjectName name = null;
    name = new ObjectName(domain + ":type=UserDatabase,database=" + userDatabase.getId());
    
    return name;
  }
  
  @Deprecated
  static ObjectName createObjectName(String domain, Valve valve)
    throws MalformedObjectNameException
  {
    if ((valve instanceof ValveBase))
    {
      ObjectName name = ((ValveBase)valve).getObjectName();
      if (name != null) {
        return name;
      }
    }
    ObjectName name = null;
    Container container = null;
    String className = valve.getClass().getName();
    int period = className.lastIndexOf('.');
    if (period >= 0) {
      className = className.substring(period + 1);
    }
    if ((valve instanceof Contained)) {
      container = ((Contained)valve).getContainer();
    }
    if (container == null) {
      throw new MalformedObjectNameException("Cannot create mbean for non-contained valve " + valve);
    }
    if ((container instanceof Engine))
    {
      String local = "";
      int seq = getSeq(local);
      String ext = "";
      if (seq > 0) {
        ext = ",seq=" + seq;
      }
      name = new ObjectName(domain + ":type=Valve,name=" + className + ext + local);
    }
    else if ((container instanceof Host))
    {
      String local = ",host=" + container.getName();
      int seq = getSeq(local);
      String ext = "";
      if (seq > 0) {
        ext = ",seq=" + seq;
      }
      name = new ObjectName(domain + ":type=Valve,name=" + className + ext + local);
    }
    else if ((container instanceof Context))
    {
      Context context = (Context)container;
      ContextName cn = new ContextName(context.getName(), false);
      Container host = context.getParent();
      String local = ",context=" + cn.getDisplayName() + ",host=" + host.getName();
      
      int seq = getSeq(local);
      String ext = "";
      if (seq > 0) {
        ext = ",seq=" + seq;
      }
      name = new ObjectName(domain + ":type=Valve,name=" + className + ext + local);
    }
    return name;
  }
  
  @Deprecated
  static Hashtable<String, int[]> seq = new Hashtable();
  
  @Deprecated
  static int getSeq(String key)
  {
    int[] i = (int[])seq.get(key);
    if (i == null)
    {
      i = new int[1];
      i[0] = 0;
      seq.put(key, i);
    }
    else
    {
      i[0] += 1;
    }
    return i[0];
  }
  
  public static synchronized Registry createRegistry()
  {
    if (registry == null)
    {
      registry = Registry.getRegistry(null, null);
      ClassLoader cl = MBeanUtils.class.getClassLoader();
      
      registry.loadDescriptors("org.apache.catalina.mbeans", cl);
      registry.loadDescriptors("org.apache.catalina.authenticator", cl);
      registry.loadDescriptors("org.apache.catalina.core", cl);
      registry.loadDescriptors("org.apache.catalina", cl);
      registry.loadDescriptors("org.apache.catalina.deploy", cl);
      registry.loadDescriptors("org.apache.catalina.loader", cl);
      registry.loadDescriptors("org.apache.catalina.realm", cl);
      registry.loadDescriptors("org.apache.catalina.session", cl);
      registry.loadDescriptors("org.apache.catalina.startup", cl);
      registry.loadDescriptors("org.apache.catalina.users", cl);
      registry.loadDescriptors("org.apache.catalina.ha", cl);
      registry.loadDescriptors("org.apache.catalina.connector", cl);
      registry.loadDescriptors("org.apache.catalina.valves", cl);
    }
    return registry;
  }
  
  public static synchronized MBeanServer createServer()
  {
    if (mserver == null) {
      mserver = Registry.getRegistry(null, null).getMBeanServer();
    }
    return mserver;
  }
  
  @Deprecated
  static void destroyMBean(Connector connector, Service service)
    throws Exception
  {
    String domain = service.getContainer().getName();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, connector);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    String worker = null;
    ProtocolHandler handler = connector.getProtocolHandler();
    if ((handler instanceof Http11Protocol)) {
      worker = ((Http11Protocol)handler).getName();
    } else if ((handler instanceof Http11NioProtocol)) {
      worker = ((Http11NioProtocol)handler).getName();
    } else if ((handler instanceof Http11AprProtocol)) {
      worker = ((Http11AprProtocol)handler).getName();
    } else if ((handler instanceof AjpProtocol)) {
      worker = ((AjpProtocol)handler).getName();
    } else if ((handler instanceof AjpAprProtocol)) {
      worker = ((AjpAprProtocol)handler).getName();
    }
    ObjectName query = new ObjectName(domain + ":type=RequestProcessor,worker=" + worker + ",*");
    
    Set<ObjectName> results = mserver.queryNames(query, null);
    for (ObjectName result : results) {
      mserver.unregisterMBean(result);
    }
  }
  
  @Deprecated
  static void destroyMBean(Context context)
    throws Exception
  {
    String domain = context.getParent().getParent().getName();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, context);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  public static void destroyMBean(ContextEnvironment environment)
    throws Exception
  {
    String mname = createManagedName(environment);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, environment);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  public static void destroyMBean(ContextResource resource)
    throws Exception
  {
    if ("org.apache.catalina.UserDatabase".equals(resource.getType())) {
      destroyMBeanUserDatabase(resource.getName());
    }
    String mname = createManagedName(resource);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, resource);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  public static void destroyMBean(ContextResourceLink resourceLink)
    throws Exception
  {
    String mname = createManagedName(resourceLink);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, resourceLink);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Engine engine)
    throws Exception
  {
    String domain = engine.getName();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, engine);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  static void destroyMBean(Group group)
    throws Exception
  {
    String mname = createManagedName(group);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, group);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Host host)
    throws Exception
  {
    String domain = host.getParent().getName();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, host);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Loader loader)
    throws Exception
  {
    String mname = createManagedName(loader);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, loader);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Manager manager)
    throws Exception
  {
    String mname = createManagedName(manager);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, manager);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(NamingResources resources)
    throws Exception
  {
    String mname = createManagedName(resources);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, resources);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Realm realm)
    throws Exception
  {
    String mname = createManagedName(realm);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, realm);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  static void destroyMBean(Role role)
    throws Exception
  {
    String mname = createManagedName(role);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, role);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Server server)
    throws Exception
  {
    String mname = createManagedName(server);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, server);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    oname = new ObjectName("Catalina:type=StringCache");
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
    oname = new ObjectName("Catalina:type=MBeanFactory");
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(Service service)
    throws Exception
  {
    String mname = createManagedName(service);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, service);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  static void destroyMBean(User user)
    throws Exception
  {
    String mname = createManagedName(user);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, user);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  static void destroyMBean(UserDatabase userDatabase)
    throws Exception
  {
    String mname = createManagedName(userDatabase);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, userDatabase);
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  static void destroyMBeanUserDatabase(String userDatabase)
    throws Exception
  {
    ObjectName query = null;
    Set<ObjectName> results = null;
    
    query = new ObjectName("Users:type=Group,database=" + userDatabase + ",*");
    
    results = mserver.queryNames(query, null);
    for (ObjectName result : results) {
      mserver.unregisterMBean(result);
    }
    query = new ObjectName("Users:type=Role,database=" + userDatabase + ",*");
    
    results = mserver.queryNames(query, null);
    for (ObjectName result : results) {
      mserver.unregisterMBean(result);
    }
    query = new ObjectName("Users:type=User,database=" + userDatabase + ",*");
    
    results = mserver.queryNames(query, null);
    for (ObjectName result : results) {
      mserver.unregisterMBean(result);
    }
    ObjectName db = new ObjectName("Users:type=UserDatabase,database=" + userDatabase);
    if (mserver.isRegistered(db)) {
      mserver.unregisterMBean(db);
    }
  }
  
  @Deprecated
  static void destroyMBean(Valve valve, Container container)
    throws Exception
  {
    ((Contained)valve).setContainer(container);
    String mname = createManagedName(valve);
    ManagedBean managed = registry.findManagedBean(mname);
    if (managed == null) {
      return;
    }
    String domain = managed.getDomain();
    if (domain == null) {
      domain = mserver.getDefaultDomain();
    }
    ObjectName oname = createObjectName(domain, valve);
    try
    {
      ((Contained)valve).setContainer(null);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    if (mserver.isRegistered(oname)) {
      mserver.unregisterMBean(oname);
    }
  }
  
  @Deprecated
  public static String getDomain(Service service)
  {
    if (service == null) {
      return null;
    }
    String domain = null;
    
    Container engine = service.getContainer();
    if (engine != null) {
      domain = engine.getName();
    }
    if (domain == null) {
      domain = service.getName();
    }
    return domain;
  }
  
  @Deprecated
  public static String getDomain(Container container)
  {
    String domain = null;
    
    Container c = container;
    while ((!(c instanceof Engine)) && (c != null)) {
      c = c.getParent();
    }
    if (c != null) {
      domain = c.getName();
    }
    return domain;
  }
  
  @Deprecated
  public static String getContainerKeyProperties(Container container)
  {
    Container c = container;
    StringBuilder keyProperties = new StringBuilder();
    int containerCount = 0;
    while (!(c instanceof Engine))
    {
      if ((c instanceof Wrapper))
      {
        keyProperties.append(",servlet=");
        keyProperties.append(c.getName());
      }
      else if ((c instanceof Context))
      {
        keyProperties.append(",context=");
        ContextName cn = new ContextName(c.getName(), false);
        keyProperties.append(cn.getDisplayName());
      }
      else if ((c instanceof Host))
      {
        keyProperties.append(",host=");
        keyProperties.append(c.getName());
      }
      else
      {
        if (c == null)
        {
          keyProperties.append(",container");
          keyProperties.append(containerCount++);
          keyProperties.append("=null");
          break;
        }
        keyProperties.append(",container");
        keyProperties.append(containerCount++);
        keyProperties.append('=');
        keyProperties.append(c.getName());
      }
      c = c.getParent();
    }
    return keyProperties.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\MBeanUtils.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */