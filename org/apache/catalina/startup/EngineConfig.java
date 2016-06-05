package org.apache.catalina.startup;

import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class EngineConfig
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(EngineConfig.class);
  protected Engine engine = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  
  public EngineConfig() {}
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      this.engine = ((Engine)event.getLifecycle());
    }
    catch (ClassCastException e)
    {
      log.error(sm.getString("engineConfig.cce", new Object[] { event.getLifecycle() }), e);
      return;
    }
    if (event.getType().equals("start")) {
      start();
    } else if (event.getType().equals("stop")) {
      stop();
    }
  }
  
  protected void start()
  {
    if (this.engine.getLogger().isDebugEnabled()) {
      this.engine.getLogger().debug(sm.getString("engineConfig.start"));
    }
  }
  
  protected void stop()
  {
    if (this.engine.getLogger().isDebugEnabled()) {
      this.engine.getLogger().debug(sm.getString("engineConfig.stop"));
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\EngineConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */