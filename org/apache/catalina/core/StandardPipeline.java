package org.apache.catalina.core;

import java.util.ArrayList;
import javax.management.ObjectName;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.util.LifecycleBase;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;

public class StandardPipeline
  extends LifecycleBase
  implements Pipeline, Contained
{
  private static final Log log = LogFactory.getLog(StandardPipeline.class);
  
  public StandardPipeline()
  {
    this(null);
  }
  
  public StandardPipeline(Container container)
  {
    setContainer(container);
  }
  
  protected Valve basic = null;
  protected Container container = null;
  protected static final String info = "org.apache.catalina.core.StandardPipeline/1.0";
  protected Valve first = null;
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardPipeline/1.0";
  }
  
  public boolean isAsyncSupported()
  {
    Valve valve = this.first != null ? this.first : this.basic;
    boolean supported = true;
    while ((supported) && (valve != null))
    {
      supported &= valve.isAsyncSupported();
      valve = valve.getNext();
    }
    return supported;
  }
  
  public Container getContainer()
  {
    return this.container;
  }
  
  public void setContainer(Container container)
  {
    this.container = container;
  }
  
  protected void initInternal() {}
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    Valve current = this.first;
    if (current == null) {
      current = this.basic;
    }
    while (current != null)
    {
      if ((current instanceof Lifecycle)) {
        ((Lifecycle)current).start();
      }
      current = current.getNext();
    }
    setState(LifecycleState.STARTING);
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
    
    Valve current = this.first;
    if (current == null) {
      current = this.basic;
    }
    while (current != null)
    {
      if ((current instanceof Lifecycle)) {
        ((Lifecycle)current).stop();
      }
      current = current.getNext();
    }
  }
  
  protected void destroyInternal()
  {
    Valve[] valves = getValves();
    for (Valve valve : valves) {
      removeValve(valve);
    }
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("Pipeline[");
    sb.append(this.container);
    sb.append(']');
    return sb.toString();
  }
  
  public Valve getBasic()
  {
    return this.basic;
  }
  
  public void setBasic(Valve valve)
  {
    Valve oldBasic = this.basic;
    if (oldBasic == valve) {
      return;
    }
    if (oldBasic != null)
    {
      if ((getState().isAvailable()) && ((oldBasic instanceof Lifecycle))) {
        try
        {
          ((Lifecycle)oldBasic).stop();
        }
        catch (LifecycleException e)
        {
          log.error("StandardPipeline.setBasic: stop", e);
        }
      }
      if ((oldBasic instanceof Contained)) {
        try
        {
          ((Contained)oldBasic).setContainer(null);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
      }
    }
    if (valve == null) {
      return;
    }
    if ((valve instanceof Contained)) {
      ((Contained)valve).setContainer(this.container);
    }
    if ((getState().isAvailable()) && ((valve instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)valve).start();
      }
      catch (LifecycleException e)
      {
        log.error("StandardPipeline.setBasic: start", e);
        return;
      }
    }
    Valve current = this.first;
    while (current != null)
    {
      if (current.getNext() == oldBasic)
      {
        current.setNext(valve);
        break;
      }
      current = current.getNext();
    }
    this.basic = valve;
  }
  
  public void addValve(Valve valve)
  {
    if ((valve instanceof Contained)) {
      ((Contained)valve).setContainer(this.container);
    }
    if ((getState().isAvailable()) && 
      ((valve instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)valve).start();
      }
      catch (LifecycleException e)
      {
        log.error("StandardPipeline.addValve: start: ", e);
      }
    }
    if (this.first == null)
    {
      this.first = valve;
      valve.setNext(this.basic);
    }
    else
    {
      Valve current = this.first;
      while (current != null)
      {
        if (current.getNext() == this.basic)
        {
          current.setNext(valve);
          valve.setNext(this.basic);
          break;
        }
        current = current.getNext();
      }
    }
    this.container.fireContainerEvent("addValve", valve);
  }
  
  public Valve[] getValves()
  {
    ArrayList<Valve> valveList = new ArrayList();
    Valve current = this.first;
    if (current == null) {
      current = this.basic;
    }
    while (current != null)
    {
      valveList.add(current);
      current = current.getNext();
    }
    return (Valve[])valveList.toArray(new Valve[0]);
  }
  
  public ObjectName[] getValveObjectNames()
  {
    ArrayList<ObjectName> valveList = new ArrayList();
    Valve current = this.first;
    if (current == null) {
      current = this.basic;
    }
    while (current != null)
    {
      if ((current instanceof ValveBase)) {
        valveList.add(((ValveBase)current).getObjectName());
      }
      current = current.getNext();
    }
    return (ObjectName[])valveList.toArray(new ObjectName[0]);
  }
  
  public void removeValve(Valve valve)
  {
    Valve current;
    if (this.first == valve)
    {
      this.first = this.first.getNext();
      current = null;
    }
    else
    {
      current = this.first;
    }
    while (current != null)
    {
      if (current.getNext() == valve)
      {
        current.setNext(valve.getNext());
        break;
      }
      current = current.getNext();
    }
    if (this.first == this.basic) {
      this.first = null;
    }
    if ((valve instanceof Contained)) {
      ((Contained)valve).setContainer(null);
    }
    if ((valve instanceof Lifecycle))
    {
      if (getState().isAvailable()) {
        try
        {
          ((Lifecycle)valve).stop();
        }
        catch (LifecycleException e)
        {
          log.error("StandardPipeline.removeValve: stop: ", e);
        }
      }
      try
      {
        ((Lifecycle)valve).destroy();
      }
      catch (LifecycleException e)
      {
        log.error("StandardPipeline.removeValve: destroy: ", e);
      }
    }
    this.container.fireContainerEvent("removeValve", valve);
  }
  
  public Valve getFirst()
  {
    if (this.first != null) {
      return this.first;
    }
    return this.basic;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardPipeline.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */