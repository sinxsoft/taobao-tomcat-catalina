package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import javax.management.ObjectName;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class StandardService
  extends LifecycleMBeanBase
  implements Service
{
  private static final Log log = LogFactory.getLog(StandardService.class);
  private static final String info = "org.apache.catalina.core.StandardService/1.0";
  private String name = null;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private Server server = null;
  protected PropertyChangeSupport support = new PropertyChangeSupport(this);
  protected Connector[] connectors = new Connector[0];
  protected ArrayList<Executor> executors = new ArrayList();
  protected Container container = null;
  private ClassLoader parentClassLoader = null;
  
  public StandardService() {}
  
  public Container getContainer()
  {
    return this.container;
  }
  
  public void setContainer(Container container)
  {
    Container oldContainer = this.container;
    if ((oldContainer != null) && ((oldContainer instanceof Engine))) {
      ((Engine)oldContainer).setService(null);
    }
    this.container = container;
    if ((this.container != null) && ((this.container instanceof Engine))) {
      ((Engine)this.container).setService(this);
    }
    if ((getState().isAvailable()) && (this.container != null)) {
      try
      {
        this.container.start();
      }
      catch (LifecycleException e) {}
    }
    if ((getState().isAvailable()) && (oldContainer != null)) {
      try
      {
        oldContainer.stop();
      }
      catch (LifecycleException e) {}
    }
    this.support.firePropertyChange("container", oldContainer, this.container);
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardService/1.0";
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public void setServer(Server server)
  {
    this.server = server;
  }
  
  public void addConnector(Connector connector)
  {
    synchronized (this.connectors)
    {
      connector.setService(this);
      Connector[] results = new Connector[this.connectors.length + 1];
      System.arraycopy(this.connectors, 0, results, 0, this.connectors.length);
      results[this.connectors.length] = connector;
      this.connectors = results;
      if (getState().isAvailable()) {
        try
        {
          connector.start();
        }
        catch (LifecycleException e)
        {
          log.error(sm.getString("standardService.connector.startFailed", new Object[] { connector }), e);
        }
      }
      this.support.firePropertyChange("connector", null, connector);
    }
  }
  
  public ObjectName[] getConnectorNames()
  {
    ObjectName[] results = new ObjectName[this.connectors.length];
    for (int i = 0; i < results.length; i++) {
      results[i] = this.connectors[i].getObjectName();
    }
    return results;
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }
  
  public Connector[] findConnectors()
  {
    return this.connectors;
  }
  
  public void removeConnector(Connector connector)
  {
    synchronized (this.connectors)
    {
      int j = -1;
      for (int i = 0; i < this.connectors.length; i++) {
        if (connector == this.connectors[i])
        {
          j = i;
          break;
        }
      }
      if (j < 0) {
        return;
      }
      if (this.connectors[j].getState().isAvailable()) {
        try
        {
          this.connectors[j].stop();
        }
        catch (LifecycleException e)
        {
          log.error(sm.getString("standardService.connector.stopFailed", new Object[] { this.connectors[j] }), e);
        }
      }
      connector.setService(null);
      int k = 0;
      Connector[] results = new Connector[this.connectors.length - 1];
      for (int i = 0; i < this.connectors.length; i++) {
        if (i != j) {
          results[(k++)] = this.connectors[i];
        }
      }
      this.connectors = results;
      
      this.support.firePropertyChange("connector", connector, null);
    }
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("StandardService[");
    sb.append(getName());
    sb.append("]");
    return sb.toString();
  }
  
  public void addExecutor(Executor ex)
  {
    synchronized (this.executors)
    {
      if (!this.executors.contains(ex))
      {
        this.executors.add(ex);
        if (getState().isAvailable()) {
          try
          {
            ex.start();
          }
          catch (LifecycleException x)
          {
            log.error("Executor.start", x);
          }
        }
      }
    }
  }
  
  public Executor[] findExecutors()
  {
    synchronized (this.executors)
    {
      Executor[] arr = new Executor[this.executors.size()];
      this.executors.toArray(arr);
      return arr;
    }
  }
  
  public Executor getExecutor(String executorName)
  {
    synchronized (this.executors)
    {
      for (Executor executor : this.executors) {
        if (executorName.equals(executor.getName())) {
          return executor;
        }
      }
    }
    return null;
  }
  
  public void removeExecutor(Executor ex)
  {
    synchronized (this.executors)
    {
      if ((this.executors.remove(ex)) && (getState().isAvailable())) {
        try
        {
          ex.stop();
        }
        catch (LifecycleException e)
        {
          log.error("Executor.stop", e);
        }
      }
    }
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (log.isInfoEnabled()) {
      log.info(sm.getString("standardService.start.name", new Object[] { this.name }));
    }
    setState(LifecycleState.STARTING);
    if (this.container != null) {
      synchronized (this.container)
      {
        this.container.start();
      }
    }
    synchronized (this.executors)
    {
      for (Executor executor : this.executors) {
        executor.start();
      }
    }
    synchronized (this.connectors)
    {
      for (Connector connector : this.connectors) {
        try
        {
          if (connector.getState() != LifecycleState.FAILED) {
            connector.start();
          }
        }
        catch (Exception e)
        {
          log.error(sm.getString("standardService.connector.startFailed", new Object[] { connector }), e);
        }
      }
    }
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    synchronized (this.connectors)
    {
      for (Connector connector : this.connectors) {
        try
        {
          connector.pause();
        }
        catch (Exception e)
        {
          log.error(sm.getString("standardService.connector.pauseFailed", new Object[] { connector }), e);
        }
      }
    }
    if (log.isInfoEnabled()) {
      log.info(sm.getString("standardService.stop.name", new Object[] { this.name }));
    }
    setState(LifecycleState.STOPPING);
    if (this.container != null) {
      synchronized (this.container)
      {
        this.container.stop();
      }
    }
    synchronized (this.connectors)
    {
      for (Connector connector : this.connectors) {
        if (LifecycleState.STARTED.equals(connector.getState())) {
          try
          {
            connector.stop();
          }
          catch (Exception e)
          {
            log.error(sm.getString("standardService.connector.stopFailed", new Object[] { connector }), e);
          }
        }
      }
    }
    synchronized (this.executors)
    {
      for (Executor executor : this.executors) {
        executor.stop();
      }
    }
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
    if (this.container != null) {
      this.container.init();
    }
    for (Executor executor : findExecutors())
    {
      if ((executor instanceof LifecycleMBeanBase)) {
        ((LifecycleMBeanBase)executor).setDomain(getDomain());
      }
      executor.init();
    }
    synchronized (this.connectors)
    {
      for (Connector connector : this.connectors) {
        try
        {
          connector.init();
        }
        catch (Exception e)
        {
          String message = sm.getString("standardService.connector.initFailed", new Object[] { connector });
          
          log.error(message, e);
          if (Boolean.getBoolean("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE")) {
            throw new LifecycleException(message);
          }
        }
      }
    }
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    synchronized (this.connectors)
    {
      for (Connector connector : this.connectors) {
        try
        {
          connector.destroy();
        }
        catch (Exception e)
        {
          log.error(sm.getString("standardService.connector.destroyfailed", new Object[] { connector }), e);
        }
      }
    }
    for (Executor executor : findExecutors()) {
      executor.destroy();
    }
    if (this.container != null) {
      this.container.destroy();
    }
    super.destroyInternal();
  }
  
  public ClassLoader getParentClassLoader()
  {
    if (this.parentClassLoader != null) {
      return this.parentClassLoader;
    }
    if (this.server != null) {
      return this.server.getParentClassLoader();
    }
    return ClassLoader.getSystemClassLoader();
  }
  
  public void setParentClassLoader(ClassLoader parent)
  {
    ClassLoader oldParentClassLoader = this.parentClassLoader;
    this.parentClassLoader = parent;
    this.support.firePropertyChange("parentClassLoader", oldParentClassLoader, this.parentClassLoader);
  }
  
  protected String getDomainInternal()
  {
    return MBeanUtils.getDomain(this);
  }
  
  public final String getObjectNameKeyProperties()
  {
    return "type=Service";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardService.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */