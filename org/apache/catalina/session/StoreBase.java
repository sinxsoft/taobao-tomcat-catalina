package org.apache.catalina.session;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Store;
import org.apache.catalina.util.LifecycleBase;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

public abstract class StoreBase
  extends LifecycleBase
  implements Store
{
  protected static final String info = "StoreBase/1.0";
  protected static String storeName = "StoreBase";
  protected PropertyChangeSupport support = new PropertyChangeSupport(this);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.session");
  protected Manager manager;
  
  public StoreBase() {}
  
  public String getInfo()
  {
    return "StoreBase/1.0";
  }
  
  public String getStoreName()
  {
    return storeName;
  }
  
  public void setManager(Manager manager)
  {
    Manager oldManager = this.manager;
    this.manager = manager;
    this.support.firePropertyChange("manager", oldManager, this.manager);
  }
  
  public Manager getManager()
  {
    return this.manager;
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  public void processExpires()
  {
    String[] keys = null;
    if (!getState().isAvailable()) {
      return;
    }
    try
    {
      keys = keys();
    }
    catch (IOException e)
    {
      this.manager.getContainer().getLogger().error("Error getting keys", e);
      return;
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(getStoreName() + ": processExpires check number of " + keys.length + " sessions");
    }
    long timeNow = System.currentTimeMillis();
    for (int i = 0; i < keys.length; i++) {
      try
      {
        StandardSession session = (StandardSession)load(keys[i]);
        if (session != null)
        {
          int timeIdle = (int)((timeNow - session.getThisAccessedTime()) / 1000L);
          if (timeIdle >= session.getMaxInactiveInterval())
          {
            if (this.manager.getContainer().getLogger().isDebugEnabled()) {
              this.manager.getContainer().getLogger().debug(getStoreName() + ": processExpires expire store session " + keys[i]);
            }
            boolean isLoaded = false;
            if ((this.manager instanceof PersistentManagerBase)) {
              isLoaded = ((PersistentManagerBase)this.manager).isLoaded(keys[i]);
            } else {
              try
              {
                if (this.manager.findSession(keys[i]) != null) {
                  isLoaded = true;
                }
              }
              catch (IOException ioe) {}
            }
            if (isLoaded) {
              session.recycle();
            } else {
              session.expire();
            }
            remove(keys[i]);
          }
        }
      }
      catch (Exception e)
      {
        this.manager.getContainer().getLogger().error("Session: " + keys[i] + "; ", e);
        try
        {
          remove(keys[i]);
        }
        catch (IOException e2)
        {
          this.manager.getContainer().getLogger().error("Error removing key", e2);
        }
      }
    }
  }
  
  protected void initInternal() {}
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STARTING);
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
  }
  
  protected void destroyInternal() {}
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append('[');
    if (this.manager == null) {
      sb.append("Manager is null");
    } else {
      sb.append(this.manager);
    }
    sb.append(']');
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\StoreBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */