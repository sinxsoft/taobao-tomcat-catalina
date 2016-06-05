package org.apache.catalina.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Server;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextHandler;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceEnvRef;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.ContextService;
import org.apache.catalina.deploy.ContextTransaction;
import org.apache.catalina.deploy.NamingResources;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.ContextAccessController;
import org.apache.naming.ContextBindings;
import org.apache.naming.EjbRef;
import org.apache.naming.HandlerRef;
import org.apache.naming.NamingContext;
import org.apache.naming.ResourceEnvRef;
import org.apache.naming.ResourceLinkRef;
import org.apache.naming.ResourceRef;
import org.apache.naming.ServiceRef;
import org.apache.naming.TransactionRef;
import org.apache.naming.factory.ResourceLinkFactory;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

public class NamingContextListener
  implements LifecycleListener, ContainerListener, PropertyChangeListener
{
  private static final Log log = LogFactory.getLog(NamingContextListener.class);
  protected Log logger = log;
  protected String name = "/";
  protected Object container = null;
  protected boolean initialized = false;
  protected NamingResources namingResources = null;
  protected NamingContext namingContext = null;
  protected javax.naming.Context compCtx = null;
  protected javax.naming.Context envCtx = null;
  protected HashMap<String, ObjectName> objectNames = new HashMap();
  private boolean exceptionOnFailedWrite = true;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public NamingContextListener() {}
  
  public boolean getExceptionOnFailedWrite()
  {
    return this.exceptionOnFailedWrite;
  }
  
  public void setExceptionOnFailedWrite(boolean exceptionOnFailedWrite)
  {
    this.exceptionOnFailedWrite = exceptionOnFailedWrite;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  @Deprecated
  public javax.naming.Context getCompContext()
  {
    return this.compCtx;
  }
  
  public javax.naming.Context getEnvContext()
  {
    return this.envCtx;
  }
  
  @Deprecated
  public NamingContext getNamingContext()
  {
    return this.namingContext;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    this.container = event.getLifecycle();
    if ((this.container instanceof org.apache.catalina.Context))
    {
      this.namingResources = ((org.apache.catalina.Context)this.container).getNamingResources();
      this.logger = log;
    }
    else if ((this.container instanceof Server))
    {
      this.namingResources = ((Server)this.container).getGlobalNamingResources();
    }
    else
    {
      return;
    }
    if ("configure_start".equals(event.getType()))
    {
      if (this.initialized) {
        return;
      }
      try
      {
        Hashtable<String, Object> contextEnv = new Hashtable();
        try
        {
          this.namingContext = new NamingContext(contextEnv, getName());
        }
        catch (NamingException e) {}
        ContextAccessController.setSecurityToken(getName(), this.container);
        ContextAccessController.setSecurityToken(this.container, this.container);
        ContextBindings.bindContext(this.container, this.namingContext, this.container);
        if (log.isDebugEnabled()) {
          log.debug("Bound " + this.container);
        }
        this.namingContext.setExceptionOnFailedWrite(getExceptionOnFailedWrite());
        
        ContextAccessController.setWritable(getName(), this.container);
        try
        {
          createNamingContext();
        }
        catch (NamingException e)
        {
          this.logger.error(sm.getString("naming.namingContextCreationFailed", new Object[] { e }));
        }
        this.namingResources.addPropertyChangeListener(this);
        if ((this.container instanceof org.apache.catalina.Context))
        {
          ContextAccessController.setReadOnly(getName());
          try
          {
            ContextBindings.bindClassLoader(this.container, this.container, ((Container)this.container).getLoader().getClassLoader());
          }
          catch (NamingException e)
          {
            this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
          }
        }
        if ((this.container instanceof Server))
        {
          ResourceLinkFactory.setGlobalContext(this.namingContext);
          try
          {
            ContextBindings.bindClassLoader(this.container, this.container, getClass().getClassLoader());
          }
          catch (NamingException e)
          {
            this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
          }
          if ((this.container instanceof StandardServer)) {
            ((StandardServer)this.container).setGlobalNamingContext(this.namingContext);
          }
        }
      }
      finally
      {
        this.initialized = true;
      }
    }
    else if ("configure_stop".equals(event.getType()))
    {
      if (!this.initialized) {
        return;
      }
      try
      {
        ContextAccessController.setWritable(getName(), this.container);
        ContextBindings.unbindContext(this.container, this.container);
        if ((this.container instanceof org.apache.catalina.Context)) {
          ContextBindings.unbindClassLoader(this.container, this.container, ((Container)this.container).getLoader().getClassLoader());
        }
        if ((this.container instanceof Server))
        {
          this.namingResources.removePropertyChangeListener(this);
          ContextBindings.unbindClassLoader(this.container, this.container, getClass().getClassLoader());
        }
        ContextAccessController.unsetSecurityToken(getName(), this.container);
        ContextAccessController.unsetSecurityToken(this.container, this.container);
        if (!this.objectNames.isEmpty())
        {
          Collection<ObjectName> names = this.objectNames.values();
          Registry registry = Registry.getRegistry(null, null);
          for (ObjectName objectName : names) {
            registry.unregisterComponent(objectName);
          }
        }
      }
      finally
      {
        Registry registry;
        this.objectNames.clear();
        
        this.namingContext = null;
        this.envCtx = null;
        this.compCtx = null;
        this.initialized = false;
      }
    }
  }
  
  public void containerEvent(ContainerEvent event)
  {
    if (!this.initialized) {
      return;
    }
    ContextAccessController.setWritable(getName(), this.container);
    
    String type = event.getType();
    if (type.equals("addEjb"))
    {
      String ejbName = (String)event.getData();
      if (ejbName != null)
      {
        ContextEjb ejb = this.namingResources.findEjb(ejbName);
        addEjb(ejb);
      }
    }
    else if (type.equals("addEnvironment"))
    {
      String environmentName = (String)event.getData();
      if (environmentName != null)
      {
        ContextEnvironment env = this.namingResources.findEnvironment(environmentName);
        
        addEnvironment(env);
      }
    }
    else if (type.equals("addLocalEjb"))
    {
      String localEjbName = (String)event.getData();
      if (localEjbName != null)
      {
        ContextLocalEjb localEjb = this.namingResources.findLocalEjb(localEjbName);
        
        addLocalEjb(localEjb);
      }
    }
    else if (type.equals("addResource"))
    {
      String resourceName = (String)event.getData();
      if (resourceName != null)
      {
        ContextResource resource = this.namingResources.findResource(resourceName);
        
        addResource(resource);
      }
    }
    else if (type.equals("addResourceLink"))
    {
      String resourceLinkName = (String)event.getData();
      if (resourceLinkName != null)
      {
        ContextResourceLink resourceLink = this.namingResources.findResourceLink(resourceLinkName);
        
        addResourceLink(resourceLink);
      }
    }
    else if (type.equals("addResourceEnvRef"))
    {
      String resourceEnvRefName = (String)event.getData();
      if (resourceEnvRefName != null)
      {
        ContextResourceEnvRef resourceEnvRef = this.namingResources.findResourceEnvRef(resourceEnvRefName);
        
        addResourceEnvRef(resourceEnvRef);
      }
    }
    else if (type.equals("addService"))
    {
      String serviceName = (String)event.getData();
      if (serviceName != null)
      {
        ContextService service = this.namingResources.findService(serviceName);
        
        addService(service);
      }
    }
    else if (type.equals("removeEjb"))
    {
      String ejbName = (String)event.getData();
      if (ejbName != null) {
        removeEjb(ejbName);
      }
    }
    else if (type.equals("removeEnvironment"))
    {
      String environmentName = (String)event.getData();
      if (environmentName != null) {
        removeEnvironment(environmentName);
      }
    }
    else if (type.equals("removeLocalEjb"))
    {
      String localEjbName = (String)event.getData();
      if (localEjbName != null) {
        removeLocalEjb(localEjbName);
      }
    }
    else if (type.equals("removeResource"))
    {
      String resourceName = (String)event.getData();
      if (resourceName != null) {
        removeResource(resourceName);
      }
    }
    else if (type.equals("removeResourceLink"))
    {
      String resourceLinkName = (String)event.getData();
      if (resourceLinkName != null) {
        removeResourceLink(resourceLinkName);
      }
    }
    else if (type.equals("removeResourceEnvRef"))
    {
      String resourceEnvRefName = (String)event.getData();
      if (resourceEnvRefName != null) {
        removeResourceEnvRef(resourceEnvRefName);
      }
    }
    else if (type.equals("removeService"))
    {
      String serviceName = (String)event.getData();
      if (serviceName != null) {
        removeService(serviceName);
      }
    }
    ContextAccessController.setReadOnly(getName());
  }
  
  public void propertyChange(PropertyChangeEvent event)
  {
    if (!this.initialized) {
      return;
    }
    Object source = event.getSource();
    if (source == this.namingResources)
    {
      ContextAccessController.setWritable(getName(), this.container);
      
      processGlobalResourcesChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
      
      ContextAccessController.setReadOnly(getName());
    }
  }
  
  private void processGlobalResourcesChange(String name, Object oldValue, Object newValue)
  {
    if (name.equals("ejb"))
    {
      if (oldValue != null)
      {
        ContextEjb ejb = (ContextEjb)oldValue;
        if (ejb.getName() != null) {
          removeEjb(ejb.getName());
        }
      }
      if (newValue != null)
      {
        ContextEjb ejb = (ContextEjb)newValue;
        if (ejb.getName() != null) {
          addEjb(ejb);
        }
      }
    }
    else if (name.equals("environment"))
    {
      if (oldValue != null)
      {
        ContextEnvironment env = (ContextEnvironment)oldValue;
        if (env.getName() != null) {
          removeEnvironment(env.getName());
        }
      }
      if (newValue != null)
      {
        ContextEnvironment env = (ContextEnvironment)newValue;
        if (env.getName() != null) {
          addEnvironment(env);
        }
      }
    }
    else if (name.equals("localEjb"))
    {
      if (oldValue != null)
      {
        ContextLocalEjb ejb = (ContextLocalEjb)oldValue;
        if (ejb.getName() != null) {
          removeLocalEjb(ejb.getName());
        }
      }
      if (newValue != null)
      {
        ContextLocalEjb ejb = (ContextLocalEjb)newValue;
        if (ejb.getName() != null) {
          addLocalEjb(ejb);
        }
      }
    }
    else if (name.equals("resource"))
    {
      if (oldValue != null)
      {
        ContextResource resource = (ContextResource)oldValue;
        if (resource.getName() != null) {
          removeResource(resource.getName());
        }
      }
      if (newValue != null)
      {
        ContextResource resource = (ContextResource)newValue;
        if (resource.getName() != null) {
          addResource(resource);
        }
      }
    }
    else if (name.equals("resourceEnvRef"))
    {
      if (oldValue != null)
      {
        ContextResourceEnvRef resourceEnvRef = (ContextResourceEnvRef)oldValue;
        if (resourceEnvRef.getName() != null) {
          removeResourceEnvRef(resourceEnvRef.getName());
        }
      }
      if (newValue != null)
      {
        ContextResourceEnvRef resourceEnvRef = (ContextResourceEnvRef)newValue;
        if (resourceEnvRef.getName() != null) {
          addResourceEnvRef(resourceEnvRef);
        }
      }
    }
    else if (name.equals("resourceLink"))
    {
      if (oldValue != null)
      {
        ContextResourceLink rl = (ContextResourceLink)oldValue;
        if (rl.getName() != null) {
          removeResourceLink(rl.getName());
        }
      }
      if (newValue != null)
      {
        ContextResourceLink rl = (ContextResourceLink)newValue;
        if (rl.getName() != null) {
          addResourceLink(rl);
        }
      }
    }
    else if (name.equals("service"))
    {
      if (oldValue != null)
      {
        ContextService service = (ContextService)oldValue;
        if (service.getName() != null) {
          removeService(service.getName());
        }
      }
      if (newValue != null)
      {
        ContextService service = (ContextService)newValue;
        if (service.getName() != null) {
          addService(service);
        }
      }
    }
  }
  
  private void createNamingContext()
    throws NamingException
  {
    if ((this.container instanceof Server))
    {
      this.compCtx = this.namingContext;
      this.envCtx = this.namingContext;
    }
    else
    {
      this.compCtx = this.namingContext.createSubcontext("comp");
      this.envCtx = this.compCtx.createSubcontext("env");
    }
    if (log.isDebugEnabled()) {
      log.debug("Creating JNDI naming context");
    }
    if (this.namingResources == null)
    {
      this.namingResources = new NamingResources();
      this.namingResources.setContainer(this.container);
    }
    int i;
    ContextResourceLink[] resourceLinks = this.namingResources.findResourceLinks();
    for (i = 0; i < resourceLinks.length; i++) {
      addResourceLink(resourceLinks[i]);
    }
    ContextResource[] resources = this.namingResources.findResources();
    for (i = 0; i < resources.length; i++) {
      addResource(resources[i]);
    }
    ContextResourceEnvRef[] resourceEnvRefs = this.namingResources.findResourceEnvRefs();
    for (i = 0; i < resourceEnvRefs.length; i++) {
      addResourceEnvRef(resourceEnvRefs[i]);
    }
    ContextEnvironment[] contextEnvironments = this.namingResources.findEnvironments();
    for (i = 0; i < contextEnvironments.length; i++) {
      addEnvironment(contextEnvironments[i]);
    }
    ContextEjb[] ejbs = this.namingResources.findEjbs();
    for (i = 0; i < ejbs.length; i++) {
      addEjb(ejbs[i]);
    }
    ContextService[] services = this.namingResources.findServices();
    for (i = 0; i < services.length; i++) {
      addService(services[i]);
    }
    if ((this.container instanceof org.apache.catalina.Context)) {
      try
      {
        Reference ref = new TransactionRef();
        this.compCtx.bind("UserTransaction", ref);
        ContextTransaction transaction = this.namingResources.getTransaction();
        if (transaction != null)
        {
          Iterator<String> params = transaction.listProperties();
          while (params.hasNext())
          {
            String paramName = (String)params.next();
            String paramValue = (String)transaction.getProperty(paramName);
            StringRefAddr refAddr = new StringRefAddr(paramName, paramValue);
            ref.add(refAddr);
          }
        }
      }
      catch (NameAlreadyBoundException e) {}catch (NamingException e)
      {
        this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
      }
    }
    if ((this.container instanceof org.apache.catalina.Context)) {
      try
      {
        this.compCtx.bind("Resources", ((Container)this.container).getResources());
      }
      catch (NamingException e)
      {
        this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
      }
    }
  }
  
  protected ObjectName createObjectName(ContextResource resource)
    throws MalformedObjectNameException
  {
    String domain = null;
    if ((this.container instanceof StandardServer)) {
      domain = ((StandardServer)this.container).getDomain();
    } else if ((this.container instanceof ContainerBase)) {
      domain = ((ContainerBase)this.container).getDomain();
    }
    if (domain == null) {
      domain = "Catalina";
    }
    ObjectName name = null;
    String quotedResourceName = ObjectName.quote(resource.getName());
    if ((this.container instanceof Server))
    {
      name = new ObjectName(domain + ":type=DataSource" + ",class=" + resource.getType() + ",name=" + quotedResourceName);
    }
    else if ((this.container instanceof org.apache.catalina.Context))
    {
      String contextName = ((org.apache.catalina.Context)this.container).getName();
      if (!contextName.startsWith("/")) {
        contextName = "/" + contextName;
      }
      Host host = (Host)((org.apache.catalina.Context)this.container).getParent();
      name = new ObjectName(domain + ":type=DataSource" + ",context=" + contextName + ",host=" + host.getName() + ",class=" + resource.getType() + ",name=" + quotedResourceName);
    }
    return name;
  }
  
  public void addEjb(ContextEjb ejb)
  {
    Reference ref = new EjbRef(ejb.getType(), ejb.getHome(), ejb.getRemote(), ejb.getLink());
    
    Iterator<String> params = ejb.listProperties();
    while (params.hasNext())
    {
      String paramName = (String)params.next();
      String paramValue = (String)ejb.getProperty(paramName);
      StringRefAddr refAddr = new StringRefAddr(paramName, paramValue);
      ref.add(refAddr);
    }
    try
    {
      createSubcontexts(this.envCtx, ejb.getName());
      this.envCtx.bind(ejb.getName(), ref);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
    }
  }
  
  public void addEnvironment(ContextEnvironment env)
  {
    Object value = null;
    
    String type = env.getType();
    try
    {
      if (type.equals("java.lang.String"))
      {
        value = env.getValue();
      }
      else if (type.equals("java.lang.Byte"))
      {
        if (env.getValue() == null) {
          value = Byte.valueOf((byte)0);
        } else {
          value = Byte.decode(env.getValue());
        }
      }
      else if (type.equals("java.lang.Short"))
      {
        if (env.getValue() == null) {
          value = Short.valueOf((short)0);
        } else {
          value = Short.decode(env.getValue());
        }
      }
      else if (type.equals("java.lang.Integer"))
      {
        if (env.getValue() == null) {
          value = Integer.valueOf(0);
        } else {
          value = Integer.decode(env.getValue());
        }
      }
      else if (type.equals("java.lang.Long"))
      {
        if (env.getValue() == null) {
          value = Long.valueOf(0L);
        } else {
          value = Long.decode(env.getValue());
        }
      }
      else if (type.equals("java.lang.Boolean"))
      {
        value = Boolean.valueOf(env.getValue());
      }
      else if (type.equals("java.lang.Double"))
      {
        if (env.getValue() == null) {
          value = Double.valueOf(0.0D);
        } else {
          value = Double.valueOf(env.getValue());
        }
      }
      else if (type.equals("java.lang.Float"))
      {
        if (env.getValue() == null) {
          value = Float.valueOf(0.0F);
        } else {
          value = Float.valueOf(env.getValue());
        }
      }
      else if (type.equals("java.lang.Character"))
      {
        if (env.getValue() == null) {
          value = Character.valueOf('\000');
        } else if (env.getValue().length() == 1) {
          value = Character.valueOf(env.getValue().charAt(0));
        } else {
          throw new IllegalArgumentException();
        }
      }
      else
      {
        value = constructEnvEntry(env.getType(), env.getValue());
        if (value == null) {
          this.logger.error(sm.getString("naming.invalidEnvEntryType", new Object[] { env.getName() }));
        }
      }
    }
    catch (NumberFormatException e)
    {
      this.logger.error(sm.getString("naming.invalidEnvEntryValue", new Object[] { env.getName() }));
    }
    catch (IllegalArgumentException e)
    {
      this.logger.error(sm.getString("naming.invalidEnvEntryValue", new Object[] { env.getName() }));
    }
    if (value != null) {
      try
      {
        if (this.logger.isDebugEnabled()) {
          this.logger.debug("  Adding environment entry " + env.getName());
        }
        createSubcontexts(this.envCtx, env.getName());
        this.envCtx.bind(env.getName(), value);
      }
      catch (NamingException e)
      {
        this.logger.error(sm.getString("naming.invalidEnvEntryValue", new Object[] { e }));
      }
    }
  }
  
  private Object constructEnvEntry(String type, String value)
  {
    try
    {
      Class<?> clazz = Class.forName(type);
      Constructor<?> c = null;
      try
      {
        c = clazz.getConstructor(new Class[] { String.class });
        return c.newInstance(new Object[] { value });
      }
      catch (NoSuchMethodException e)
      {
        if (value.length() != 1) {
          return null;
        }
        try
        {
          c = clazz.getConstructor(new Class[] { Character.TYPE });
          return c.newInstance(new Object[] { Character.valueOf(value.charAt(0)) });
        }
        catch (NoSuchMethodException ex) {}
      }
      return null;
    }
    catch (Exception e) {return null;}
  }
  
  public void addLocalEjb(ContextLocalEjb localEjb) {}
  
  public void addService(ContextService service)
  {
    if (service.getWsdlfile() != null)
    {
      URL wsdlURL = null;
      try
      {
        wsdlURL = new URL(service.getWsdlfile());
      }
      catch (MalformedURLException e) {}
      if (wsdlURL == null) {
        try
        {
          wsdlURL = ((org.apache.catalina.Context)this.container).getServletContext().getResource(service.getWsdlfile());
        }
        catch (MalformedURLException e) {}
      }
      if (wsdlURL == null) {
        try
        {
          wsdlURL = ((org.apache.catalina.Context)this.container).getServletContext().getResource("/" + service.getWsdlfile());
          
          this.logger.debug("  Changing service ref wsdl file for /" + service.getWsdlfile());
        }
        catch (MalformedURLException e)
        {
          this.logger.error(sm.getString("naming.wsdlFailed", new Object[] { e }));
        }
      }
      if (wsdlURL == null) {
        service.setWsdlfile(null);
      } else {
        service.setWsdlfile(wsdlURL.toString());
      }
    }
    if (service.getJaxrpcmappingfile() != null)
    {
      URL jaxrpcURL = null;
      try
      {
        jaxrpcURL = new URL(service.getJaxrpcmappingfile());
      }
      catch (MalformedURLException e) {}
      if (jaxrpcURL == null) {
        try
        {
          jaxrpcURL = ((org.apache.catalina.Context)this.container).getServletContext().getResource(service.getJaxrpcmappingfile());
        }
        catch (MalformedURLException e) {}
      }
      if (jaxrpcURL == null) {
        try
        {
          jaxrpcURL = ((org.apache.catalina.Context)this.container).getServletContext().getResource("/" + service.getJaxrpcmappingfile());
          
          this.logger.debug("  Changing service ref jaxrpc file for /" + service.getJaxrpcmappingfile());
        }
        catch (MalformedURLException e)
        {
          this.logger.error(sm.getString("naming.wsdlFailed", new Object[] { e }));
        }
      }
      if (jaxrpcURL == null) {
        service.setJaxrpcmappingfile(null);
      } else {
        service.setJaxrpcmappingfile(jaxrpcURL.toString());
      }
    }
    Reference ref = new ServiceRef(service.getName(), service.getType(), service.getServiceqname(), service.getWsdlfile(), service.getJaxrpcmappingfile());
    
    Iterator<String> portcomponent = service.getServiceendpoints();
    while (portcomponent.hasNext())
    {
      String serviceendpoint = (String)portcomponent.next();
      StringRefAddr refAddr = new StringRefAddr("serviceendpointinterface", serviceendpoint);
      ref.add(refAddr);
      String portlink = service.getPortlink(serviceendpoint);
      refAddr = new StringRefAddr("portcomponentlink", portlink);
      ref.add(refAddr);
    }
    Iterator<String> handlers = service.getHandlers();
    while (handlers.hasNext())
    {
      String handlername = (String)handlers.next();
      ContextHandler handler = service.getHandler(handlername);
      HandlerRef handlerRef = new HandlerRef(handlername, handler.getHandlerclass());
      Iterator<String> localParts = handler.getLocalparts();
      while (localParts.hasNext())
      {
        String localPart = (String)localParts.next();
        String namespaceURI = handler.getNamespaceuri(localPart);
        handlerRef.add(new StringRefAddr("handlerlocalpart", localPart));
        handlerRef.add(new StringRefAddr("handlernamespace", namespaceURI));
      }
      Iterator<String> params = handler.listProperties();
      while (params.hasNext())
      {
        String paramName = (String)params.next();
        String paramValue = (String)handler.getProperty(paramName);
        handlerRef.add(new StringRefAddr("handlerparamname", paramName));
        handlerRef.add(new StringRefAddr("handlerparamvalue", paramValue));
      }
      for (int i = 0; i < handler.getSoapRolesSize(); i++) {
        handlerRef.add(new StringRefAddr("handlersoaprole", handler.getSoapRole(i)));
      }
      for (int i = 0; i < handler.getPortNamesSize(); i++) {
        handlerRef.add(new StringRefAddr("handlerportname", handler.getPortName(i)));
      }
      ((ServiceRef)ref).addHandler(handlerRef);
    }
    try
    {
      if (this.logger.isDebugEnabled()) {
        this.logger.debug("  Adding service ref " + service.getName() + "  " + ref);
      }
      createSubcontexts(this.envCtx, service.getName());
      this.envCtx.bind(service.getName(), ref);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
    }
  }
  
  public void addResource(ContextResource resource)
  {
    Reference ref = new ResourceRef(resource.getType(), resource.getDescription(), resource.getScope(), resource.getAuth(), resource.getSingleton());
    
    Iterator<String> params = resource.listProperties();
    while (params.hasNext())
    {
      String paramName = (String)params.next();
      String paramValue = (String)resource.getProperty(paramName);
      StringRefAddr refAddr = new StringRefAddr(paramName, paramValue);
      ref.add(refAddr);
    }
    try
    {
      if (this.logger.isDebugEnabled()) {
        this.logger.debug("  Adding resource ref " + resource.getName() + "  " + ref);
      }
      createSubcontexts(this.envCtx, resource.getName());
      this.envCtx.bind(resource.getName(), ref);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
    }
    if (("javax.sql.DataSource".equals(ref.getClassName())) && (resource.getSingleton())) {
      try
      {
        ObjectName on = createObjectName(resource);
        Object actualResource = this.envCtx.lookup(resource.getName());
        Registry.getRegistry(null, null).registerComponent(actualResource, on, null);
        this.objectNames.put(resource.getName(), on);
      }
      catch (Exception e)
      {
        this.logger.warn(sm.getString("naming.jmxRegistrationFailed", new Object[] { e }));
      }
    }
  }
  
  public void addResourceEnvRef(ContextResourceEnvRef resourceEnvRef)
  {
    Reference ref = new ResourceEnvRef(resourceEnvRef.getType());
    
    Iterator<String> params = resourceEnvRef.listProperties();
    while (params.hasNext())
    {
      String paramName = (String)params.next();
      String paramValue = (String)resourceEnvRef.getProperty(paramName);
      StringRefAddr refAddr = new StringRefAddr(paramName, paramValue);
      ref.add(refAddr);
    }
    try
    {
      if (this.logger.isDebugEnabled()) {
        log.debug("  Adding resource env ref " + resourceEnvRef.getName());
      }
      createSubcontexts(this.envCtx, resourceEnvRef.getName());
      this.envCtx.bind(resourceEnvRef.getName(), ref);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
    }
  }
  
  public void addResourceLink(ContextResourceLink resourceLink)
  {
    Reference ref = new ResourceLinkRef(resourceLink.getType(), resourceLink.getGlobal(), resourceLink.getFactory(), null);
    
    Iterator<String> i = resourceLink.listProperties();
    while (i.hasNext())
    {
      String key = ((String)i.next()).toString();
      Object val = resourceLink.getProperty(key);
      if (val != null)
      {
        StringRefAddr refAddr = new StringRefAddr(key, val.toString());
        ref.add(refAddr);
      }
    }
    javax.naming.Context ctx = "UserTransaction".equals(resourceLink.getName()) ? this.compCtx : this.envCtx;
    try
    {
      if (this.logger.isDebugEnabled()) {
        log.debug("  Adding resource link " + resourceLink.getName());
      }
      createSubcontexts(this.envCtx, resourceLink.getName());
      ctx.bind(resourceLink.getName(), ref);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.bindFailed", new Object[] { e }));
    }
  }
  
  public void removeEjb(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  public void removeEnvironment(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  public void removeLocalEjb(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  public void removeService(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  public void removeResource(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
    ObjectName on = (ObjectName)this.objectNames.get(name);
    if (on != null) {
      Registry.getRegistry(null, null).unregisterComponent(on);
    }
  }
  
  public void removeResourceEnvRef(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  public void removeResourceLink(String name)
  {
    try
    {
      this.envCtx.unbind(name);
    }
    catch (NamingException e)
    {
      this.logger.error(sm.getString("naming.unbindFailed", new Object[] { e }));
    }
  }
  
  private void createSubcontexts(javax.naming.Context ctx, String name)
    throws NamingException
  {
    javax.naming.Context currentContext = ctx;
    StringTokenizer tokenizer = new StringTokenizer(name, "/");
    while (tokenizer.hasMoreTokens())
    {
      String token = tokenizer.nextToken();
      if ((!token.equals("")) && (tokenizer.hasMoreTokens())) {
        try
        {
          currentContext = currentContext.createSubcontext(token);
        }
        catch (NamingException e)
        {
          currentContext = (javax.naming.Context)currentContext.lookup(token);
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\NamingContextListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */