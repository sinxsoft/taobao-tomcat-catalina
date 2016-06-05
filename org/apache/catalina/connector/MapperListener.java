package org.apache.catalina.connector;

import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.http.mapper.WrapperMappingInfo;
import org.apache.tomcat.util.res.StringManager;

public class MapperListener
  extends LifecycleMBeanBase
  implements ContainerListener, LifecycleListener
{
  private static final Log log = LogFactory.getLog(MapperListener.class);
  private Mapper mapper = null;
  private Connector connector = null;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  private final String domain = null;
  
  public MapperListener(Mapper mapper, Connector connector)
  {
    this.mapper = mapper;
    this.connector = connector;
  }
  
  @Deprecated
  public String getConnectorName()
  {
    return this.connector.toString();
  }
  
  public void startInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STARTING);
    
    findDefaultHost();
    
    Engine engine = (Engine)this.connector.getService().getContainer();
    addListeners(engine);
    
    Container[] conHosts = engine.findChildren();
    for (Container conHost : conHosts)
    {
      Host host = (Host)conHost;
      if (!LifecycleState.NEW.equals(host.getState())) {
        registerHost(host);
      }
    }
  }
  
  public void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
    
    Engine engine = (Engine)this.connector.getService().getContainer();
    removeListeners(engine);
  }
  
  protected String getDomainInternal()
  {
    return this.connector.getDomainInternal();
  }
  
  protected String getObjectNameKeyProperties()
  {
    return this.connector.createObjectNameKeyProperties("Mapper");
  }
  
  public void containerEvent(ContainerEvent event)
  {
    if ("addChild".equals(event.getType()))
    {
      Container child = (Container)event.getData();
      addListeners(child);
      if (child.getState().isAvailable()) {
        if ((child instanceof Host)) {
          registerHost((Host)child);
        } else if ((child instanceof org.apache.catalina.Context)) {
          registerContext((org.apache.catalina.Context)child);
        } else if ((child instanceof Wrapper)) {
          if (child.getParent().getState().isAvailable()) {
            registerWrapper((Wrapper)child);
          }
        }
      }
    }
    else if ("removeChild".equals(event.getType()))
    {
      Container child = (Container)event.getData();
      removeListeners(child);
    }
    else if ("addAlias".equals(event.getType()))
    {
      this.mapper.addHostAlias(((Host)event.getSource()).getName(), event.getData().toString());
    }
    else if ("removeAlias".equals(event.getType()))
    {
      this.mapper.removeHostAlias(event.getData().toString());
    }
    else if ("addMapping".equals(event.getType()))
    {
      Wrapper wrapper = (Wrapper)event.getSource();
      org.apache.catalina.Context context = (org.apache.catalina.Context)wrapper.getParent();
      String contextPath = context.getPath();
      if ("/".equals(contextPath)) {
        contextPath = "";
      }
      String version = context.getWebappVersion();
      String hostName = context.getParent().getName();
      String wrapperName = wrapper.getName();
      String mapping = (String)event.getData();
      boolean jspWildCard = ("jsp".equals(wrapperName)) && (mapping.endsWith("/*"));
      
      this.mapper.addWrapper(hostName, contextPath, version, mapping, wrapper, jspWildCard, context.isResourceOnlyServlet(wrapperName));
    }
    else if ("removeMapping".equals(event.getType()))
    {
      Wrapper wrapper = (Wrapper)event.getSource();
      
      org.apache.catalina.Context context = (org.apache.catalina.Context)wrapper.getParent();
      String contextPath = context.getPath();
      if ("/".equals(contextPath)) {
        contextPath = "";
      }
      String version = context.getWebappVersion();
      String hostName = context.getParent().getName();
      
      String mapping = (String)event.getData();
      
      this.mapper.removeWrapper(hostName, contextPath, version, mapping);
    }
    else if ("addWelcomeFile".equals(event.getType()))
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)event.getSource();
      
      String hostName = context.getParent().getName();
      
      String contextPath = context.getPath();
      if ("/".equals(contextPath)) {
        contextPath = "";
      }
      String welcomeFile = (String)event.getData();
      
      this.mapper.addWelcomeFile(hostName, contextPath, context.getWebappVersion(), welcomeFile);
    }
    else if ("removeWelcomeFile".equals(event.getType()))
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)event.getSource();
      
      String hostName = context.getParent().getName();
      
      String contextPath = context.getPath();
      if ("/".equals(contextPath)) {
        contextPath = "";
      }
      String welcomeFile = (String)event.getData();
      
      this.mapper.removeWelcomeFile(hostName, contextPath, context.getWebappVersion(), welcomeFile);
    }
    else if ("clearWelcomeFiles".equals(event.getType()))
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)event.getSource();
      
      String hostName = context.getParent().getName();
      
      String contextPath = context.getPath();
      if ("/".equals(contextPath)) {
        contextPath = "";
      }
      this.mapper.clearWelcomeFiles(hostName, contextPath, context.getWebappVersion());
    }
  }
  
  private void findDefaultHost()
  {
    Engine engine = (Engine)this.connector.getService().getContainer();
    String defaultHost = engine.getDefaultHost();
    
    boolean found = false;
    if ((defaultHost != null) && (defaultHost.length() > 0))
    {
      Container[] containers = engine.findChildren();
      for (Container container : containers)
      {
        Host host = (Host)container;
        if (defaultHost.equalsIgnoreCase(host.getName()))
        {
          found = true;
          break;
        }
        String[] aliases = host.findAliases();
        for (String alias : aliases) {
          if (defaultHost.equalsIgnoreCase(alias))
          {
            found = true;
            break;
          }
        }
      }
    }
    if (found) {
      this.mapper.setDefaultHostName(defaultHost);
    } else {
      log.warn(sm.getString("mapperListener.unknownDefaultHost", new Object[] { defaultHost, this.connector }));
    }
  }
  
  private void registerHost(Host host)
  {
    String[] aliases = host.findAliases();
    this.mapper.addHost(host.getName(), aliases, host);
    for (Container container : host.findChildren()) {
      if (container.getState().isAvailable()) {
        registerContext((org.apache.catalina.Context)container);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.registerHost", new Object[] { host.getName(), this.domain, this.connector }));
    }
  }
  
  private void unregisterHost(Host host)
  {
    String hostname = host.getName();
    
    this.mapper.removeHost(hostname);
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.unregisterHost", new Object[] { hostname, this.domain, this.connector }));
    }
  }
  
  private void unregisterWrapper(Wrapper wrapper)
  {
    org.apache.catalina.Context context = (org.apache.catalina.Context)wrapper.getParent();
    String contextPath = context.getPath();
    String wrapperName = wrapper.getName();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }
    String version = context.getWebappVersion();
    String hostName = context.getParent().getName();
    
    String[] mappings = wrapper.findMappings();
    for (String mapping : mappings) {
      this.mapper.removeWrapper(hostName, contextPath, version, mapping);
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.unregisterWrapper", new Object[] { wrapperName, contextPath, this.connector }));
    }
  }
  
  private void registerContext(org.apache.catalina.Context context)
  {
    String contextPath = context.getPath();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }
    Container host = context.getParent();
    
    javax.naming.Context resources = context.getResources();
    String[] welcomeFiles = context.findWelcomeFiles();
    List<WrapperMappingInfo> wrappers = new ArrayList();
    for (Container container : context.findChildren())
    {
      prepareWrapperMappingInfo(context, (Wrapper)container, wrappers);
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("mapperListener.registerWrapper", new Object[] { container.getName(), contextPath, this.connector }));
      }
    }
    this.mapper.addContextVersion(host.getName(), host, contextPath, context.getWebappVersion(), context, welcomeFiles, resources, wrappers);
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.registerContext", new Object[] { contextPath, this.connector }));
    }
  }
  
  private void unregisterContext(org.apache.catalina.Context context)
  {
    String contextPath = context.getPath();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }
    String hostName = context.getParent().getName();
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.unregisterContext", new Object[] { contextPath, this.connector }));
    }
    if (context.getPaused())
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("mapperListener.pauseContext", new Object[] { contextPath, this.connector }));
      }
      this.mapper.pauseContextVersion(context, hostName, contextPath, context.getWebappVersion());
    }
    else
    {
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("mapperListener.unregisterContext", new Object[] { contextPath, this.connector }));
      }
      this.mapper.removeContextVersion(hostName, contextPath, context.getWebappVersion());
    }
  }
  
  private void registerWrapper(Wrapper wrapper)
  {
    org.apache.catalina.Context context = (org.apache.catalina.Context)wrapper.getParent();
    String contextPath = context.getPath();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }
    String version = context.getWebappVersion();
    String hostName = context.getParent().getName();
    
    List<WrapperMappingInfo> wrappers = new ArrayList();
    prepareWrapperMappingInfo(context, wrapper, wrappers);
    this.mapper.addWrappers(hostName, contextPath, version, wrappers);
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("mapperListener.registerWrapper", new Object[] { wrapper.getName(), contextPath, this.connector }));
    }
  }
  
  private void prepareWrapperMappingInfo(org.apache.catalina.Context context, Wrapper wrapper, List<WrapperMappingInfo> wrappers)
  {
    String wrapperName = wrapper.getName();
    boolean resourceOnly = context.isResourceOnlyServlet(wrapperName);
    String[] mappings = wrapper.findMappings();
    for (String mapping : mappings)
    {
      boolean jspWildCard = (wrapperName.equals("jsp")) && (mapping.endsWith("/*"));
      
      wrappers.add(new WrapperMappingInfo(mapping, wrapper, jspWildCard, resourceOnly));
    }
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if (event.getType().equals("after_start"))
    {
      Object obj = event.getSource();
      if ((obj instanceof Wrapper))
      {
        Wrapper w = (Wrapper)obj;
        if (w.getParent().getState().isAvailable()) {
          registerWrapper(w);
        }
      }
      else if ((obj instanceof org.apache.catalina.Context))
      {
        org.apache.catalina.Context c = (org.apache.catalina.Context)obj;
        if (c.getParent().getState().isAvailable()) {
          registerContext(c);
        }
      }
      else if ((obj instanceof Host))
      {
        registerHost((Host)obj);
      }
    }
    else if (event.getType().equals("before_stop"))
    {
      Object obj = event.getSource();
      if ((obj instanceof Wrapper)) {
        unregisterWrapper((Wrapper)obj);
      } else if ((obj instanceof org.apache.catalina.Context)) {
        unregisterContext((org.apache.catalina.Context)obj);
      } else if ((obj instanceof Host)) {
        unregisterHost((Host)obj);
      }
    }
  }
  
  private void addListeners(Container container)
  {
    container.addContainerListener(this);
    container.addLifecycleListener(this);
    for (Container child : container.findChildren()) {
      addListeners(child);
    }
  }
  
  private void removeListeners(Container container)
  {
    container.removeContainerListener(this);
    container.removeLifecycleListener(this);
    for (Container child : container.findChildren()) {
      removeListeners(child);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\MapperListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */