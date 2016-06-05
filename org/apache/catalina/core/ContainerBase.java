package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import org.apache.catalina.AccessLog;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public abstract class ContainerBase
  extends LifecycleMBeanBase
  implements Container
{
  private static final Log log = LogFactory.getLog(ContainerBase.class);
  protected HashMap<String, Container> children;
  protected int backgroundProcessorDelay;
  protected List<ContainerListener> listeners;
  protected Loader loader;
  protected Log logger;
  protected String logName;
  protected Manager manager;
  protected Cluster cluster;
  protected String name;
  protected Container parent;
  protected ClassLoader parentClassLoader;
  protected Pipeline pipeline;
  private volatile Realm realm;
  private final ReadWriteLock realmLock;
  protected DirContext resources;
  
  protected class PrivilegedAddChild
    implements PrivilegedAction<Void>
  {
    private Container child;
    
    PrivilegedAddChild(Container child)
    {
      this.child = child;
    }
    
    public Void run()
    {
      ContainerBase.this.addChildInternal(this.child);
      return null;
    }
  }
  
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  protected boolean startChildren;
  protected PropertyChangeSupport support;
  private Thread thread;
  private volatile boolean threadDone;
  protected volatile AccessLog accessLog;
  private volatile boolean accessLogScanComplete;
  private int startStopThreads;
  protected ThreadPoolExecutor startStopExecutor;
  
  public ContainerBase()
  {
    this.children = new HashMap();
    
    this.backgroundProcessorDelay = -1;
    
    this.listeners = new CopyOnWriteArrayList();
    
    this.loader = null;
    
    this.logger = null;
    
    this.logName = null;
    
    this.manager = null;
    
    this.cluster = null;
    
    this.name = null;
    
    this.parent = null;
    
    this.parentClassLoader = null;
    
    this.pipeline = new StandardPipeline(this);
    
    this.realm = null;
    
    this.realmLock = new ReentrantReadWriteLock();
    
    this.resources = null;
    
    this.startChildren = true;
    
    this.support = new PropertyChangeSupport(this);
    
    this.thread = null;
    
    this.threadDone = false;
    
    this.accessLog = null;
    this.accessLogScanComplete = false;
    
    this.startStopThreads = 1;
  }
  
  public int getStartStopThreads()
  {
    return this.startStopThreads;
  }
  
  private int getStartStopThreadsInternal()
  {
    int result = getStartStopThreads();
    if (result > 0) {
      return result;
    }
    result = Runtime.getRuntime().availableProcessors() + result;
    if (result < 1) {
      result = 1;
    }
    return result;
  }
  
  public void setStartStopThreads(int startStopThreads)
  {
    this.startStopThreads = startStopThreads;
    
    ThreadPoolExecutor executor = this.startStopExecutor;
    if (executor != null)
    {
      int newThreads = getStartStopThreadsInternal();
      executor.setMaximumPoolSize(newThreads);
      executor.setCorePoolSize(newThreads);
    }
  }
  
  public int getBackgroundProcessorDelay()
  {
    return this.backgroundProcessorDelay;
  }
  
  public void setBackgroundProcessorDelay(int delay)
  {
    this.backgroundProcessorDelay = delay;
  }
  
  public String getInfo()
  {
    return getClass().getName();
  }
  
  public Loader getLoader()
  {
    if (this.loader != null) {
      return this.loader;
    }
    if (this.parent != null) {
      return this.parent.getLoader();
    }
    return null;
  }
  
  public synchronized void setLoader(Loader loader)
  {
    Loader oldLoader = this.loader;
    if (oldLoader == loader) {
      return;
    }
    this.loader = loader;
    if ((getState().isAvailable()) && (oldLoader != null) && ((oldLoader instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)oldLoader).stop();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setLoader: stop: ", e);
      }
    }
    if (loader != null) {
      loader.setContainer(this);
    }
    if ((getState().isAvailable()) && (loader != null) && ((loader instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)loader).start();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setLoader: start: ", e);
      }
    }
    this.support.firePropertyChange("loader", oldLoader, this.loader);
  }
  
  public Log getLogger()
  {
    if (this.logger != null) {
      return this.logger;
    }
    this.logger = LogFactory.getLog(logName());
    return this.logger;
  }
  
  public Manager getManager()
  {
    if (this.manager != null) {
      return this.manager;
    }
    if (this.parent != null) {
      return this.parent.getManager();
    }
    return null;
  }
  
  public synchronized void setManager(Manager manager)
  {
    Manager oldManager = this.manager;
    if (oldManager == manager) {
      return;
    }
    this.manager = manager;
    if ((getState().isAvailable()) && (oldManager != null) && ((oldManager instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)oldManager).stop();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setManager: stop: ", e);
      }
    }
    if (manager != null) {
      manager.setContainer(this);
    }
    if ((getState().isAvailable()) && (manager != null) && ((manager instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)manager).start();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setManager: start: ", e);
      }
    }
    this.support.firePropertyChange("manager", oldManager, this.manager);
  }
  
  @Deprecated
  public Object getMappingObject()
  {
    return this;
  }
  
  public Cluster getCluster()
  {
    if (this.cluster != null) {
      return this.cluster;
    }
    if (this.parent != null) {
      return this.parent.getCluster();
    }
    return null;
  }
  
  public synchronized void setCluster(Cluster cluster)
  {
    Cluster oldCluster = this.cluster;
    if (oldCluster == cluster) {
      return;
    }
    this.cluster = cluster;
    if ((getState().isAvailable()) && (oldCluster != null) && ((oldCluster instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)oldCluster).stop();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setCluster: stop: ", e);
      }
    }
    if (cluster != null) {
      cluster.setContainer(this);
    }
    if ((getState().isAvailable()) && (cluster != null) && ((cluster instanceof Lifecycle))) {
      try
      {
        ((Lifecycle)cluster).start();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.setCluster: start: ", e);
      }
    }
    this.support.firePropertyChange("cluster", oldCluster, this.cluster);
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    String oldName = this.name;
    this.name = name;
    this.support.firePropertyChange("name", oldName, this.name);
  }
  
  public boolean getStartChildren()
  {
    return this.startChildren;
  }
  
  public void setStartChildren(boolean startChildren)
  {
    boolean oldStartChildren = this.startChildren;
    this.startChildren = startChildren;
    this.support.firePropertyChange("startChildren", oldStartChildren, this.startChildren);
  }
  
  public Container getParent()
  {
    return this.parent;
  }
  
  public void setParent(Container container)
  {
    Container oldParent = this.parent;
    this.parent = container;
    this.support.firePropertyChange("parent", oldParent, this.parent);
  }
  
  public ClassLoader getParentClassLoader()
  {
    if (this.parentClassLoader != null) {
      return this.parentClassLoader;
    }
    if (this.parent != null) {
      return this.parent.getParentClassLoader();
    }
    return ClassLoader.getSystemClassLoader();
  }
  
  public void setParentClassLoader(ClassLoader parent)
  {
    ClassLoader oldParentClassLoader = this.parentClassLoader;
    this.parentClassLoader = parent;
    this.support.firePropertyChange("parentClassLoader", oldParentClassLoader, this.parentClassLoader);
  }
  
  public Pipeline getPipeline()
  {
    return this.pipeline;
  }
  
  public Realm getRealm()
  {
    Lock l = this.realmLock.readLock();
    try
    {
      l.lock();
      Realm localRealm;
      if (this.realm != null) {
        return this.realm;
      }
      if (this.parent != null) {
        return this.parent.getRealm();
      }
      return null;
    }
    finally
    {
      l.unlock();
    }
  }
  
  protected Realm getRealmInternal()
  {
    Lock l = this.realmLock.readLock();
    try
    {
      l.lock();
      return this.realm;
    }
    finally
    {
      l.unlock();
    }
  }
  
  public void setRealm(Realm realm)
  {
    Lock l = this.realmLock.writeLock();
    try
    {
      l.lock();
      
      Realm oldRealm = this.realm;
      if (oldRealm == realm) {
        return;
      }
      this.realm = realm;
      if ((getState().isAvailable()) && (oldRealm != null) && ((oldRealm instanceof Lifecycle))) {
        try
        {
          ((Lifecycle)oldRealm).stop();
        }
        catch (LifecycleException e)
        {
          log.error("ContainerBase.setRealm: stop: ", e);
        }
      }
      if (realm != null) {
        realm.setContainer(this);
      }
      if ((getState().isAvailable()) && (realm != null) && ((realm instanceof Lifecycle))) {
        try
        {
          ((Lifecycle)realm).start();
        }
        catch (LifecycleException e)
        {
          log.error("ContainerBase.setRealm: start: ", e);
        }
      }
      this.support.firePropertyChange("realm", oldRealm, this.realm);
    }
    finally
    {
      l.unlock();
    }
  }
  
  public DirContext getResources()
  {
    if (this.resources != null) {
      return this.resources;
    }
    if (this.parent != null) {
      return this.parent.getResources();
    }
    return null;
  }
  
  public synchronized void setResources(DirContext resources)
  {
    DirContext oldResources = this.resources;
    if (oldResources == resources) {
      return;
    }
    Hashtable<String, String> env = new Hashtable();
    if (getParent() != null) {
      env.put("host", getParent().getName());
    }
    env.put("context", getName());
    this.resources = new ProxyDirContext(env, resources);
    
    this.support.firePropertyChange("resources", oldResources, this.resources);
  }
  
  public void addChild(Container child)
  {
    if (Globals.IS_SECURITY_ENABLED)
    {
      PrivilegedAction<Void> dp = new PrivilegedAddChild(child);
      
      AccessController.doPrivileged(dp);
    }
    else
    {
      addChildInternal(child);
    }
  }
  
  private void addChildInternal(Container child)
  {
    if (log.isDebugEnabled()) {
      log.debug("Add child " + child + " " + this);
    }
    synchronized (this.children)
    {
      if (this.children.get(child.getName()) != null) {
        throw new IllegalArgumentException("addChild:  Child name '" + child.getName() + "' is not unique");
      }
      child.setParent(this);
      this.children.put(child.getName(), child);
    }
    if (((getState().isAvailable()) || (LifecycleState.STARTING_PREP.equals(getState()))) && (this.startChildren)) {
      try
      {
        child.start();
      }
      catch (LifecycleException e)
      {
        log.error("ContainerBase.addChild: start: ", e);
        throw new IllegalStateException("ContainerBase.addChild: start: " + e);
      }
    }
    fireContainerEvent("addChild", child);
  }
  
  public void addContainerListener(ContainerListener listener)
  {
    this.listeners.add(listener);
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }

  public Container findChild(String name)
  {
	    if (name == null)
            return (null);
        synchronized (children) {
            return children.get(name);
        }
  }
  
  public Container[] findChildren()
  {
    synchronized (this.children)
    {
      Container[] results = new Container[this.children.size()];
      return (Container[])this.children.values().toArray(results);
    }
  }
  
  public ContainerListener[] findContainerListeners()
  {
    ContainerListener[] results = new ContainerListener[0];
    
    return (ContainerListener[])this.listeners.toArray(results);
  }
  
  public void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    this.pipeline.getFirst().invoke(request, response);
  }
  
  public void removeChild(Container child)
  {
    if (child == null) {
      return;
    }
    synchronized (this.children)
    {
      if (this.children.get(child.getName()) == null) {
        return;
      }
      this.children.remove(child.getName());
    }
    try
    {
      if (child.getState().isAvailable()) {
        child.stop();
      }
    }
    catch (LifecycleException e)
    {
      log.error("ContainerBase.removeChild: stop: ", e);
    }
    fireContainerEvent("removeChild", child);
    try
    {
      if (!LifecycleState.DESTROYING.equals(child.getState())) {
        child.destroy();
      }
    }
    catch (LifecycleException e)
    {
      log.error("ContainerBase.removeChild: destroy: ", e);
    }
  }
  
  public void removeContainerListener(ContainerListener listener)
  {
    this.listeners.remove(listener);
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    BlockingQueue<Runnable> startStopQueue = new LinkedBlockingQueue();
    
    this.startStopExecutor = new ThreadPoolExecutor(getStartStopThreadsInternal(), getStartStopThreadsInternal(), 10L, TimeUnit.SECONDS, startStopQueue, new StartStopThreadFactory(getName() + "-startStop-"));
    
    this.startStopExecutor.allowCoreThreadTimeOut(true);
    super.initInternal();
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    if ((this.loader != null) && ((this.loader instanceof Lifecycle))) {
      ((Lifecycle)this.loader).start();
    }
    this.logger = null;
    getLogger();
    if ((this.manager != null) && ((this.manager instanceof Lifecycle))) {
      ((Lifecycle)this.manager).start();
    }
    if ((this.cluster != null) && ((this.cluster instanceof Lifecycle))) {
      ((Lifecycle)this.cluster).start();
    }
    Realm realm = getRealmInternal();
    if ((realm != null) && ((realm instanceof Lifecycle))) {
      ((Lifecycle)realm).start();
    }
    if ((this.resources != null) && ((this.resources instanceof Lifecycle))) {
      ((Lifecycle)this.resources).start();
    }
    Container[] children = findChildren();
    List<Future<Void>> results = new ArrayList();
    for (int i = 0; i < children.length; i++) {
      results.add(this.startStopExecutor.submit(new StartChild(children[i])));
    }
    boolean fail = false;
    for (Future<Void> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        log.error(sm.getString("containerBase.threadedStartFailed"), e);
        fail = true;
      }
    }
    if (fail) {
      throw new LifecycleException(sm.getString("containerBase.threadedStartFailed"));
    }
    if ((this.pipeline instanceof Lifecycle)) {
      ((Lifecycle)this.pipeline).start();
    }
    setState(LifecycleState.STARTING);
    
    threadStart();
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    threadStop();
    
    setState(LifecycleState.STOPPING);
    if (((this.pipeline instanceof Lifecycle)) && (((Lifecycle)this.pipeline).getState().isAvailable())) {
      ((Lifecycle)this.pipeline).stop();
    }
    Container[] children = findChildren();
    List<Future<Void>> results = new ArrayList();
    for (int i = 0; i < children.length; i++) {
      results.add(this.startStopExecutor.submit(new StopChild(children[i])));
    }
    boolean fail = false;
    for (Future<Void> result : results) {
      try
      {
        result.get();
      }
      catch (Exception e)
      {
        log.error(sm.getString("containerBase.threadedStopFailed"), e);
        fail = true;
      }
    }
    if (fail) {
      throw new LifecycleException(sm.getString("containerBase.threadedStopFailed"));
    }
    if ((this.resources != null) && ((this.resources instanceof Lifecycle))) {
      ((Lifecycle)this.resources).stop();
    }
    Realm realm = getRealmInternal();
    if ((realm != null) && ((realm instanceof Lifecycle))) {
      ((Lifecycle)realm).stop();
    }
    if ((this.cluster != null) && ((this.cluster instanceof Lifecycle))) {
      ((Lifecycle)this.cluster).stop();
    }
    if ((this.manager != null) && ((this.manager instanceof Lifecycle)) && (((Lifecycle)this.manager).getState().isAvailable())) {
      ((Lifecycle)this.manager).stop();
    }
    if ((this.loader != null) && ((this.loader instanceof Lifecycle))) {
      ((Lifecycle)this.loader).stop();
    }
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    if ((this.manager != null) && ((this.manager instanceof Lifecycle))) {
      ((Lifecycle)this.manager).destroy();
    }
    Realm realm = getRealmInternal();
    if ((realm != null) && ((realm instanceof Lifecycle))) {
      ((Lifecycle)realm).destroy();
    }
    if ((this.cluster != null) && ((this.cluster instanceof Lifecycle))) {
      ((Lifecycle)this.cluster).destroy();
    }
    if ((this.loader != null) && ((this.loader instanceof Lifecycle))) {
      ((Lifecycle)this.loader).destroy();
    }
    if ((this.pipeline instanceof Lifecycle)) {
      ((Lifecycle)this.pipeline).destroy();
    }
    for (Container child : findChildren()) {
      removeChild(child);
    }
    if (this.parent != null) {
      this.parent.removeChild(this);
    }
    if (this.startStopExecutor != null) {
      this.startStopExecutor.shutdownNow();
    }
    super.destroyInternal();
  }
  
  public void logAccess(Request request, Response response, long time, boolean useDefault)
  {
    boolean logged = false;
    if (getAccessLog() != null)
    {
      getAccessLog().log(request, response, time);
      logged = true;
    }
    if (getParent() != null) {
      getParent().logAccess(request, response, time, (useDefault) && (!logged));
    }
  }
  
  public AccessLog getAccessLog()
  {
    if (this.accessLogScanComplete) {
      return this.accessLog;
    }
    AccessLogAdapter adapter = null;
    Valve[] valves = getPipeline().getValves();
    for (Valve valve : valves) {
      if ((valve instanceof AccessLog)) {
        if (adapter == null) {
          adapter = new AccessLogAdapter((AccessLog)valve);
        } else {
          adapter.add((AccessLog)valve);
        }
      }
    }
    if (adapter != null) {
      this.accessLog = adapter;
    }
    this.accessLogScanComplete = true;
    return this.accessLog;
  }
  
  public synchronized void addValve(Valve valve)
  {
    this.pipeline.addValve(valve);
  }
  
  public void backgroundProcess()
  {
    if (!getState().isAvailable()) {
      return;
    }
    if (this.cluster != null) {
      try
      {
        this.cluster.backgroundProcess();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("containerBase.backgroundProcess.cluster", new Object[] { this.cluster }), e);
      }
    }
    if (this.loader != null) {
      try
      {
        this.loader.backgroundProcess();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("containerBase.backgroundProcess.loader", new Object[] { this.loader }), e);
      }
    }
    if (this.manager != null) {
      try
      {
        this.manager.backgroundProcess();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("containerBase.backgroundProcess.manager", new Object[] { this.manager }), e);
      }
    }
    Realm realm = getRealmInternal();
    if (realm != null) {
      try
      {
        realm.backgroundProcess();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("containerBase.backgroundProcess.realm", new Object[] { realm }), e);
      }
    }
    Valve current = this.pipeline.getFirst();
    while (current != null)
    {
      try
      {
        current.backgroundProcess();
      }
      catch (Exception e)
      {
        log.warn(sm.getString("containerBase.backgroundProcess.valve", new Object[] { current }), e);
      }
      current = current.getNext();
    }
    fireLifecycleEvent("periodic", null);
  }
  
  public void fireContainerEvent(String type, Object data)
  {
    if (this.listeners.size() < 1) {
      return;
    }
    ContainerEvent event = new ContainerEvent(this, type, data);
    for (ContainerListener listener : this.listeners) {
      listener.containerEvent(event);
    }
  }
  
  protected String logName()
  {
    if (this.logName != null) {
      return this.logName;
    }
    String loggerName = null;
    Container current = this;
    while (current != null)
    {
      String name = current.getName();
      if ((name == null) || (name.equals(""))) {
        name = "/";
      } else if (name.startsWith("##")) {
        name = "/" + name;
      }
      loggerName = "[" + name + "]" + (loggerName != null ? "." + loggerName : "");
      
      current = current.getParent();
    }
    this.logName = (ContainerBase.class.getName() + "." + loggerName);
    return this.logName;
  }
  
  protected String getDomainInternal()
  {
    return MBeanUtils.getDomain(this);
  }
  
  public ObjectName[] getChildren()
  {
    ObjectName[] result = new ObjectName[this.children.size()];
    Iterator<Container> it = this.children.values().iterator();
    int i = 0;
    while (it.hasNext())
    {
      Object next = it.next();
      if ((next instanceof ContainerBase)) {
        result[(i++)] = ((ContainerBase)next).getObjectName();
      }
    }
    return result;
  }
  
  protected void threadStart()
  {
    if (this.thread != null) {
      return;
    }
    if (this.backgroundProcessorDelay <= 0) {
      return;
    }
    this.threadDone = false;
    String threadName = "ContainerBackgroundProcessor[" + toString() + "]";
    this.thread = new Thread(new ContainerBackgroundProcessor(), threadName);
    this.thread.setDaemon(true);
    this.thread.start();
  }
  
  protected void threadStop()
  {
    if (this.thread == null) {
      return;
    }
    this.threadDone = true;
    this.thread.interrupt();
    try
    {
      this.thread.join();
    }
    catch (InterruptedException e) {}
    this.thread = null;
  }
  
  protected class ContainerBackgroundProcessor
    implements Runnable
  {
    protected ContainerBackgroundProcessor() {}
    
    public void run()
    {
      Throwable t = null;
      String unexpectedDeathMessage = ContainerBase.sm.getString("containerBase.backgroundProcess.unexpectedThreadDeath", new Object[] { Thread.currentThread().getName() });
      try
      {
        while (!ContainerBase.this.threadDone)
        {
          try
          {
            Thread.sleep(ContainerBase.this.backgroundProcessorDelay * 1000L);
          }
          catch (InterruptedException e) {}
          if (!ContainerBase.this.threadDone)
          {
            Container parent = (Container)ContainerBase.this.getMappingObject();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (parent.getLoader() != null) {
              cl = parent.getLoader().getClassLoader();
            }
            processChildren(parent, cl);
          }
        }
      }
      catch (RuntimeException e)
      {
        t = e;
        throw e;
      }
      catch (Error e)
      {
        t = e;
        throw e;
      }
      finally
      {
        if (!ContainerBase.this.threadDone) {
          ContainerBase.log.error(unexpectedDeathMessage, t);
        }
      }
    }
    
    protected void processChildren(Container container, ClassLoader cl)
    {
      try
      {
        if (container.getLoader() != null) {
          Thread.currentThread().setContextClassLoader(container.getLoader().getClassLoader());
        }
        container.backgroundProcess();
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        ContainerBase.log.error("Exception invoking periodic operation: ", t);
      }
      finally
      {
        Thread.currentThread().setContextClassLoader(cl);
      }
      Container[] children = container.findChildren();
      for (int i = 0; i < children.length; i++) {
        if (children[i].getBackgroundProcessorDelay() <= 0) {
          processChildren(children[i], cl);
        }
      }
    }
  }
  
  private static class StartChild
    implements Callable<Void>
  {
    private Container child;
    
    public StartChild(Container child)
    {
      this.child = child;
    }
    
    public Void call()
      throws LifecycleException
    {
      this.child.start();
      return null;
    }
  }
  
  private static class StopChild
    implements Callable<Void>
  {
    private Container child;
    
    public StopChild(Container child)
    {
      this.child = child;
    }
    
    public Void call()
      throws LifecycleException
    {
      if (this.child.getState().isAvailable()) {
        this.child.stop();
      }
      return null;
    }
  }
  
  private static class StartStopThreadFactory
    implements ThreadFactory
  {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    
    public StartStopThreadFactory(String namePrefix)
    {
      SecurityManager s = System.getSecurityManager();
      this.group = (s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup());
      this.namePrefix = namePrefix;
    }
    
    public Thread newThread(Runnable r)
    {
      Thread thread = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ContainerBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */