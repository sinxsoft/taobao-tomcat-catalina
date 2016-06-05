package org.apache.catalina.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.catalina.AccessLog;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.NullRealm;
import org.apache.catalina.util.ServerInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class StandardEngine
  extends ContainerBase
  implements Engine
{
  private static final Log log = LogFactory.getLog(StandardEngine.class);
  
  public StandardEngine()
  {
    this.pipeline.setBasic(new StandardEngineValve());
    try
    {
      setJvmRoute(System.getProperty("jvmRoute"));
    }
    catch (Exception ex)
    {
      log.warn(sm.getString("standardEngine.jvmRouteFail"));
    }
    this.backgroundProcessorDelay = 10;
  }
  
  private String defaultHost = null;
  private static final String info = "org.apache.catalina.core.StandardEngine/1.0";
  private Service service = null;
  private String baseDir = null;
  private String jvmRouteId;
  private final AtomicReference<AccessLog> defaultAccessLog = new AtomicReference();
  
  public Realm getRealm()
  {
    Realm configured = super.getRealm();
    if (configured == null)
    {
      configured = new NullRealm();
      setRealm(configured);
    }
    return configured;
  }
  
  public String getDefaultHost()
  {
    return this.defaultHost;
  }
  
  public void setDefaultHost(String host)
  {
    String oldDefaultHost = this.defaultHost;
    if (host == null) {
      this.defaultHost = null;
    } else {
      this.defaultHost = host.toLowerCase(Locale.ENGLISH);
    }
    this.support.firePropertyChange("defaultHost", oldDefaultHost, this.defaultHost);
  }
  
  public void setJvmRoute(String routeId)
  {
    this.jvmRouteId = routeId;
  }
  
  public String getJvmRoute()
  {
    return this.jvmRouteId;
  }
  
  public Service getService()
  {
    return this.service;
  }
  
  public void setService(Service service)
  {
    this.service = service;
  }
  
  public String getBaseDir()
  {
    if (this.baseDir == null) {
      this.baseDir = System.getProperty("catalina.base");
    }
    if (this.baseDir == null) {
      this.baseDir = System.getProperty("catalina.home");
    }
    return this.baseDir;
  }
  
  public void setBaseDir(String baseDir)
  {
    this.baseDir = baseDir;
  }
  
  public void addChild(Container child)
  {
    if (!(child instanceof Host)) {
      throw new IllegalArgumentException(sm.getString("standardEngine.notHost"));
    }
    super.addChild(child);
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardEngine/1.0";
  }
  
  public void setParent(Container container)
  {
    throw new IllegalArgumentException(sm.getString("standardEngine.notParent"));
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    getRealm();
    super.initInternal();
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    if (log.isInfoEnabled()) {
      log.info("Starting Servlet Engine: " + ServerInfo.getServerInfo());
    }
    super.startInternal();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("StandardEngine[");
    sb.append(getName());
    sb.append("]");
    return sb.toString();
  }
  
  public void logAccess(Request request, Response response, long time, boolean useDefault)
  {
    boolean logged = false;
    if (getAccessLog() != null)
    {
      this.accessLog.log(request, response, time);
      logged = true;
    }
    if ((!logged) && (useDefault))
    {
      AccessLog newDefaultAccessLog = (AccessLog)this.defaultAccessLog.get();
      if (newDefaultAccessLog == null)
      {
        Host host = (Host)findChild(getDefaultHost());
        Context context = null;
        if ((host != null) && (host.getState().isAvailable()))
        {
          newDefaultAccessLog = host.getAccessLog();
          if (newDefaultAccessLog != null)
          {
            if (this.defaultAccessLog.compareAndSet(null, newDefaultAccessLog))
            {
              AccessLogListener l = new AccessLogListener(this, host, null);
              
              l.install();
            }
          }
          else
          {
            context = (Context)host.findChild("");
            if ((context != null) && (context.getState().isAvailable()))
            {
              newDefaultAccessLog = context.getAccessLog();
              if ((newDefaultAccessLog != null) && 
                (this.defaultAccessLog.compareAndSet(null, newDefaultAccessLog)))
              {
                AccessLogListener l = new AccessLogListener(this, null, context);
                
                l.install();
              }
            }
          }
        }
        if (newDefaultAccessLog == null)
        {
          newDefaultAccessLog = new NoopAccessLog();
          if (this.defaultAccessLog.compareAndSet(null, newDefaultAccessLog))
          {
            AccessLogListener l = new AccessLogListener(this, host, context);
            
            l.install();
          }
        }
      }
      newDefaultAccessLog.log(request, response, time);
    }
  }
  
  public ClassLoader getParentClassLoader()
  {
    if (this.parentClassLoader != null) {
      return this.parentClassLoader;
    }
    if (this.service != null) {
      return this.service.getParentClassLoader();
    }
    return ClassLoader.getSystemClassLoader();
  }
  
  protected String getObjectNameKeyProperties()
  {
    return "type=Engine";
  }
  
  protected static final class NoopAccessLog
    implements AccessLog
  {
    protected NoopAccessLog() {}
    
    public void log(Request request, Response response, long time) {}
    
    public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {}
    
    public boolean getRequestAttributesEnabled()
    {
      return false;
    }
  }
  
  protected static final class AccessLogListener
    implements PropertyChangeListener, LifecycleListener, ContainerListener
  {
    private StandardEngine engine;
    private Host host;
    private Context context;
    private volatile boolean disabled = false;
    
    public AccessLogListener(StandardEngine engine, Host host, Context context)
    {
      this.engine = engine;
      this.host = host;
      this.context = context;
    }
    
    public void install()
    {
      this.engine.addPropertyChangeListener(this);
      if (this.host != null)
      {
        this.host.addContainerListener(this);
        this.host.addLifecycleListener(this);
      }
      if (this.context != null) {
        this.context.addLifecycleListener(this);
      }
    }
    
    private void uninstall()
    {
      this.disabled = true;
      if (this.context != null) {
        this.context.removeLifecycleListener(this);
      }
      if (this.host != null)
      {
        this.host.removeLifecycleListener(this);
        this.host.removeContainerListener(this);
      }
      this.engine.removePropertyChangeListener(this);
    }
    
    public void lifecycleEvent(LifecycleEvent event)
    {
      if (this.disabled) {
        return;
      }
      String type = event.getType();
      if (("after_start".equals(type)) || ("before_stop".equals(type)) || ("before_destroy".equals(type)))
      {
        this.engine.defaultAccessLog.set(null);
        uninstall();
      }
    }
    
    public void propertyChange(PropertyChangeEvent evt)
    {
      if (this.disabled) {
        return;
      }
      if ("defaultHost".equals(evt.getPropertyName()))
      {
        this.engine.defaultAccessLog.set(null);
        uninstall();
      }
    }
    
    public void containerEvent(ContainerEvent event)
    {
      if (this.disabled) {
        return;
      }
      if ("addChild".equals(event.getType()))
      {
        Context context = (Context)event.getData();
        if ("".equals(context.getPath()))
        {
          this.engine.defaultAccessLog.set(null);
          uninstall();
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardEngine.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */