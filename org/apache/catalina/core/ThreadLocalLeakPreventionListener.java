package org.apache.catalina.core;

import java.util.concurrent.Executor;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

public class ThreadLocalLeakPreventionListener
  implements LifecycleListener, ContainerListener
{
  private static final Log log = LogFactory.getLog(ThreadLocalLeakPreventionListener.class);
  private volatile boolean serverStopping = false;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public ThreadLocalLeakPreventionListener() {}
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      Lifecycle lifecycle = event.getLifecycle();
      if (("after_start".equals(event.getType())) && ((lifecycle instanceof Server)))
      {
        Server server = (Server)lifecycle;
        registerListenersForServer(server);
      }
      if (("before_stop".equals(event.getType())) && ((lifecycle instanceof Server))) {
        this.serverStopping = true;
      }
      if (("after_stop".equals(event.getType())) && ((lifecycle instanceof Context))) {
        stopIdleThreads((Context)lifecycle);
      }
    }
    catch (Exception e)
    {
      String msg = sm.getString("threadLocalLeakPreventionListener.lifecycleEvent.error", new Object[] { event });
      
      log.error(msg, e);
    }
  }
  
  public void containerEvent(ContainerEvent event)
  {
    try
    {
      String type = event.getType();
      if ("addChild".equals(type)) {
        processContainerAddChild(event.getContainer(), (Container)event.getData());
      } else if ("removeChild".equals(type)) {
        processContainerRemoveChild(event.getContainer(), (Container)event.getData());
      }
    }
    catch (Exception e)
    {
      String msg = sm.getString("threadLocalLeakPreventionListener.containerEvent.error", new Object[] { event });
      
      log.error(msg, e);
    }
  }
  
  private void registerListenersForServer(Server server)
  {
    for (Service service : server.findServices())
    {
      Engine engine = (Engine)service.getContainer();
      engine.addContainerListener(this);
      registerListenersForEngine(engine);
    }
  }
  
  private void registerListenersForEngine(Engine engine)
  {
    for (Container hostContainer : engine.findChildren())
    {
      Host host = (Host)hostContainer;
      host.addContainerListener(this);
      registerListenersForHost(host);
    }
  }
  
  private void registerListenersForHost(Host host)
  {
    for (Container contextContainer : host.findChildren())
    {
      Context context = (Context)contextContainer;
      registerContextListener(context);
    }
  }
  
  private void registerContextListener(Context context)
  {
    context.addLifecycleListener(this);
  }
  
  protected void processContainerAddChild(Container parent, Container child)
  {
    if (log.isDebugEnabled()) {
      log.debug("Process addChild[parent=" + parent + ",child=" + child + "]");
    }
    if ((child instanceof Context)) {
      registerContextListener((Context)child);
    } else if ((child instanceof Engine)) {
      registerListenersForEngine((Engine)child);
    } else if ((child instanceof Host)) {
      registerListenersForHost((Host)child);
    }
  }
  
  protected void processContainerRemoveChild(Container parent, Container child)
  {
    if (log.isDebugEnabled()) {
      log.debug("Process removeChild[parent=" + parent + ",child=" + child + "]");
    }
    if ((child instanceof Context))
    {
      Context context = (Context)child;
      context.removeLifecycleListener(this);
    }
    else if (((child instanceof Host)) || ((child instanceof Engine)))
    {
      child.removeContainerListener(this);
    }
  }
  
  private void stopIdleThreads(Context context)
  {
    if (this.serverStopping) {
      return;
    }
    if ((!(context instanceof StandardContext)) || (!((StandardContext)context).getRenewThreadsWhenStoppingContext()))
    {
      log.debug("Not renewing threads when the context is stopping. It is not configured to do it.");
      
      return;
    }
    Engine engine = (Engine)context.getParent().getParent();
    Service service = engine.getService();
    Connector[] connectors = service.findConnectors();
    if (connectors != null) {
      for (Connector connector : connectors)
      {
        ProtocolHandler handler = connector.getProtocolHandler();
        Executor executor = null;
        if (handler != null) {
          executor = handler.getExecutor();
        }
        if ((executor instanceof ThreadPoolExecutor))
        {
          ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)executor;
          
          threadPoolExecutor.contextStopping();
        }
        else if ((executor instanceof StandardThreadExecutor))
        {
          StandardThreadExecutor stdThreadExecutor = (StandardThreadExecutor)executor;
          
          stdThreadExecutor.contextStopping();
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ThreadLocalLeakPreventionListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */