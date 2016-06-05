package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.catalina.DistributedManager;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.security.SecurityUtil;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public abstract class PersistentManagerBase
  extends ManagerBase
  implements DistributedManager
{
  private static final Log log = LogFactory.getLog(PersistentManagerBase.class);
  private static final String info = "PersistentManagerBase/1.1";
  public PersistentManagerBase() {}
  
  private class PrivilegedStoreClear
    implements PrivilegedExceptionAction<Void>
  {
    PrivilegedStoreClear() {}
    
    public Void run()
      throws Exception
    {
      PersistentManagerBase.this.store.clear();
      return null;
    }
  }
  
  private class PrivilegedStoreRemove
    implements PrivilegedExceptionAction<Void>
  {
    private String id;
    
    PrivilegedStoreRemove(String id)
    {
      this.id = id;
    }
    
    public Void run()
      throws Exception
    {
      PersistentManagerBase.this.store.remove(this.id);
      return null;
    }
  }
  
  private class PrivilegedStoreLoad
    implements PrivilegedExceptionAction<Session>
  {
    private String id;
    
    PrivilegedStoreLoad(String id)
    {
      this.id = id;
    }
    
    public Session run()
      throws Exception
    {
      return PersistentManagerBase.this.store.load(this.id);
    }
  }
  
  private class PrivilegedStoreSave
    implements PrivilegedExceptionAction<Void>
  {
    private Session session;
    
    PrivilegedStoreSave(Session session)
    {
      this.session = session;
    }
    
    public Void run()
      throws Exception
    {
      PersistentManagerBase.this.store.save(this.session);
      return null;
    }
  }
  
  private class PrivilegedStoreKeys
    implements PrivilegedExceptionAction<String[]>
  {
    PrivilegedStoreKeys() {}
    
    public String[] run()
      throws Exception
    {
      return PersistentManagerBase.this.store.keys();
    }
  }
  
  private static String name = "PersistentManagerBase";
  private static final String PERSISTED_LAST_ACCESSED_TIME = "org.apache.catalina.session.PersistentManagerBase.persistedLastAccessedTime";
  protected Store store = null;
  protected boolean saveOnRestart = true;
  protected int maxIdleBackup = -1;
  protected int minIdleSwap = -1;
  protected int maxIdleSwap = -1;
  private final Map<String, Object> sessionSwapInLocks = new HashMap();
  
  public int getMaxIdleBackup()
  {
    return this.maxIdleBackup;
  }
  
  public void setMaxIdleBackup(int backup)
  {
    if (backup == this.maxIdleBackup) {
      return;
    }
    int oldBackup = this.maxIdleBackup;
    this.maxIdleBackup = backup;
    this.support.firePropertyChange("maxIdleBackup", Integer.valueOf(oldBackup), Integer.valueOf(this.maxIdleBackup));
  }
  
  public int getMaxIdleSwap()
  {
    return this.maxIdleSwap;
  }
  
  public void setMaxIdleSwap(int max)
  {
    if (max == this.maxIdleSwap) {
      return;
    }
    int oldMaxIdleSwap = this.maxIdleSwap;
    this.maxIdleSwap = max;
    this.support.firePropertyChange("maxIdleSwap", Integer.valueOf(oldMaxIdleSwap), Integer.valueOf(this.maxIdleSwap));
  }
  
  public int getMinIdleSwap()
  {
    return this.minIdleSwap;
  }
  
  public void setMinIdleSwap(int min)
  {
    if (this.minIdleSwap == min) {
      return;
    }
    int oldMinIdleSwap = this.minIdleSwap;
    this.minIdleSwap = min;
    this.support.firePropertyChange("minIdleSwap", Integer.valueOf(oldMinIdleSwap), Integer.valueOf(this.minIdleSwap));
  }
  
  public String getInfo()
  {
    return "PersistentManagerBase/1.1";
  }
  
  public boolean isLoaded(String id)
  {
    try
    {
      if (super.findSession(id) != null) {
        return true;
      }
    }
    catch (IOException e)
    {
      log.error("checking isLoaded for id, " + id + ", " + e.getMessage(), e);
    }
    return false;
  }
  
  public String getName()
  {
    return name;
  }
  
  public void setStore(Store store)
  {
    this.store = store;
    store.setManager(this);
  }
  
  public Store getStore()
  {
    return this.store;
  }
  
  public boolean getSaveOnRestart()
  {
    return this.saveOnRestart;
  }
  
  public void setSaveOnRestart(boolean saveOnRestart)
  {
    if (saveOnRestart == this.saveOnRestart) {
      return;
    }
    boolean oldSaveOnRestart = this.saveOnRestart;
    this.saveOnRestart = saveOnRestart;
    this.support.firePropertyChange("saveOnRestart", Boolean.valueOf(oldSaveOnRestart), Boolean.valueOf(this.saveOnRestart));
  }
  
  public void clearStore()
  {
    if (this.store == null) {
      return;
    }
    try
    {
      if (SecurityUtil.isPackageProtectionEnabled()) {
        try
        {
          AccessController.doPrivileged(new PrivilegedStoreClear());
        }
        catch (PrivilegedActionException ex)
        {
          Exception exception = ex.getException();
          log.error("Exception clearing the Store: " + exception, exception);
        }
      } else {
        this.store.clear();
      }
    }
    catch (IOException e)
    {
      log.error("Exception clearing the Store: " + e, e);
    }
  }
  
  public void processExpires()
  {
    long timeNow = System.currentTimeMillis();
    Session[] sessions = findSessions();
    int expireHere = 0;
    if (log.isDebugEnabled()) {
      log.debug("Start expire sessions " + getName() + " at " + timeNow + " sessioncount " + sessions.length);
    }
    for (int i = 0; i < sessions.length; i++) {
      if (!sessions[i].isValid())
      {
        this.expiredSessions.incrementAndGet();
        expireHere++;
      }
    }
    processPersistenceChecks();
    if ((getStore() != null) && ((getStore() instanceof StoreBase))) {
      ((StoreBase)getStore()).processExpires();
    }
    long timeEnd = System.currentTimeMillis();
    if (log.isDebugEnabled()) {
      log.debug("End expire sessions " + getName() + " processingTime " + (timeEnd - timeNow) + " expired sessions: " + expireHere);
    }
    this.processingTime += timeEnd - timeNow;
  }
  
  public void processPersistenceChecks()
  {
    processMaxIdleSwaps();
    processMaxActiveSwaps();
    processMaxIdleBackups();
  }
  
  public Session findSession(String id)
    throws IOException
  {
    Session session = super.findSession(id);
    if (session != null) {
      synchronized (session)
      {
        session = super.findSession(session.getIdInternal());
        if (session != null)
        {
          session.access();
          session.endAccess();
        }
      }
    }
    if (session != null) {
      return session;
    }
    session = swapIn(id);
    return session;
  }
  
  public void removeSuper(Session session)
  {
    super.remove(session, false);
  }
  
  public void load()
  {
    this.sessions.clear();
    if (this.store == null) {
      return;
    }
    String[] ids = null;
    try
    {
      if (SecurityUtil.isPackageProtectionEnabled()) {
        try
        {
          ids = (String[])AccessController.doPrivileged(new PrivilegedStoreKeys());
        }
        catch (PrivilegedActionException ex)
        {
          Exception exception = ex.getException();
          log.error("Exception in the Store during load: " + exception, exception);
          
          return;
        }
      } else {
        ids = this.store.keys();
      }
    }
    catch (IOException e)
    {
      log.error("Can't load sessions from store, " + e.getMessage(), e);
      return;
    }
    int n = ids.length;
    if (n == 0) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("persistentManager.loading", new Object[] { String.valueOf(n) }));
    }
    for (int i = 0; i < n; i++) {
      try
      {
        swapIn(ids[i]);
      }
      catch (IOException e)
      {
        log.error("Failed load session from store, " + e.getMessage(), e);
      }
    }
  }
  
  public void remove(Session session, boolean update)
  {
    super.remove(session, update);
    if (this.store != null) {
      removeSession(session.getIdInternal());
    }
  }
  
  protected void removeSession(String id)
  {
    try
    {
      if (SecurityUtil.isPackageProtectionEnabled()) {
        try
        {
          AccessController.doPrivileged(new PrivilegedStoreRemove(id));
        }
        catch (PrivilegedActionException ex)
        {
          Exception exception = ex.getException();
          log.error("Exception in the Store during removeSession: " + exception, exception);
        }
      } else {
        this.store.remove(id);
      }
    }
    catch (IOException e)
    {
      log.error("Exception removing session  " + e.getMessage(), e);
    }
  }
  
  public void unload()
  {
    if (this.store == null) {
      return;
    }
    Session[] sessions = findSessions();
    int n = sessions.length;
    if (n == 0) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("persistentManager.unloading", new Object[] { String.valueOf(n) }));
    }
    for (int i = 0; i < n; i++) {
      try
      {
        swapOut(sessions[i]);
      }
      catch (IOException e) {}
    }
  }
  
  public int getActiveSessionsFull()
  {
    int result = getActiveSessions();
    try
    {
      result += getStore().getSize();
    }
    catch (IOException ioe)
    {
      log.warn(sm.getString("persistentManager.storeSizeException"));
    }
    return result;
  }
  
  public Set<String> getSessionIdsFull()
  {
    Set<String> sessionIds = new HashSet();
    
    sessionIds.addAll(this.sessions.keySet());
    try
    {
      String[] storeKeys = getStore().keys();
      for (String storeKey : storeKeys) {
        sessionIds.add(storeKey);
      }
    }
    catch (IOException e)
    {
      log.warn(sm.getString("persistentManager.storeKeysException"));
    }
    return sessionIds;
  }
  
  protected Session swapIn(String id)
    throws IOException
  {
    if (this.store == null) {
      return null;
    }
    Object swapInLock = null;
    synchronized (this)
    {
      swapInLock = this.sessionSwapInLocks.get(id);
      if (swapInLock == null)
      {
        swapInLock = new Object();
        this.sessionSwapInLocks.put(id, swapInLock);
      }
    }
    Session session = null;
    synchronized (swapInLock)
    {
      session = (Session)this.sessions.get(id);
      if (session == null)
      {
        try
        {
          if (SecurityUtil.isPackageProtectionEnabled()) {
            try
            {
              session = (Session)AccessController.doPrivileged(new PrivilegedStoreLoad(id));
            }
            catch (PrivilegedActionException ex)
            {
              Exception e = ex.getException();
              log.error(sm.getString("persistentManager.swapInException", new Object[] { id }), e);
              if ((e instanceof IOException)) {
                throw ((IOException)e);
              }
              if ((e instanceof ClassNotFoundException)) {
                throw ((ClassNotFoundException)e);
              }
            }
          } else {
            session = this.store.load(id);
          }
        }
        catch (ClassNotFoundException e)
        {
          String msg = sm.getString("persistentManager.deserializeError", new Object[] { id });
          
          log.error(msg, e);
          throw new IllegalStateException(msg, e);
        }
        if ((session != null) && (!session.isValid()))
        {
          log.error(sm.getString("persistentManager.swapInInvalid", new Object[] { id }));
          
          session.expire();
          removeSession(id);
          session = null;
        }
        if (session != null)
        {
          if (log.isDebugEnabled()) {
            log.debug(sm.getString("persistentManager.swapIn", new Object[] { id }));
          }
          session.setManager(this);
          
          ((StandardSession)session).tellNew();
          add(session);
          ((StandardSession)session).activate();
          
          session.access();
          session.endAccess();
        }
      }
    }
    synchronized (this)
    {
      this.sessionSwapInLocks.remove(id);
    }
    return session;
  }
  
  protected void swapOut(Session session)
    throws IOException
  {
    if ((this.store == null) || (!session.isValid())) {
      return;
    }
    ((StandardSession)session).passivate();
    writeSession(session);
    super.remove(session, true);
    session.recycle();
  }
  
  protected void writeSession(Session session)
    throws IOException
  {
    if ((this.store == null) || (!session.isValid())) {
      return;
    }
    try
    {
      if (SecurityUtil.isPackageProtectionEnabled()) {
        try
        {
          AccessController.doPrivileged(new PrivilegedStoreSave(session));
        }
        catch (PrivilegedActionException ex)
        {
          Exception exception = ex.getException();
          if ((exception instanceof IOException)) {
            throw ((IOException)exception);
          }
          log.error("Exception in the Store during writeSession: " + exception, exception);
        }
      } else {
        this.store.save(session);
      }
    }
    catch (IOException e)
    {
      log.error(sm.getString("persistentManager.serializeError", new Object[] { session.getIdInternal(), e }));
      
      throw e;
    }
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    super.startInternal();
    if (this.store == null) {
      log.error("No Store configured, persistence disabled");
    } else if ((this.store instanceof Lifecycle)) {
      ((Lifecycle)this.store).start();
    }
    setState(LifecycleState.STARTING);
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    if (log.isDebugEnabled()) {
      log.debug("Stopping");
    }
    setState(LifecycleState.STOPPING);
    if ((getStore() != null) && (this.saveOnRestart))
    {
      unload();
    }
    else
    {
      Session[] sessions = findSessions();
      for (int i = 0; i < sessions.length; i++)
      {
        StandardSession session = (StandardSession)sessions[i];
        if (session.isValid()) {
          session.expire();
        }
      }
    }
    if ((getStore() != null) && ((getStore() instanceof Lifecycle))) {
      ((Lifecycle)getStore()).stop();
    }
    super.stopInternal();
  }
  
  protected void processMaxIdleSwaps()
  {
    if ((!getState().isAvailable()) || (this.maxIdleSwap < 0)) {
      return;
    }
    Session[] sessions = findSessions();
    long timeNow = System.currentTimeMillis();
    if (this.maxIdleSwap >= 0) {
      for (int i = 0; i < sessions.length; i++)
      {
        StandardSession session = (StandardSession)sessions[i];
        synchronized (session)
        {
          if (session.isValid())
          {
            int timeIdle;
            
            if (StandardSession.LAST_ACCESS_AT_START) {
              timeIdle = (int)((timeNow - session.getLastAccessedTimeInternal()) / 1000L);
            } else {
              timeIdle = (int)((timeNow - session.getThisAccessedTimeInternal()) / 1000L);
            }
            if ((timeIdle >= this.maxIdleSwap) && (timeIdle >= this.minIdleSwap))
            {
              if ((session.accessCount != null) && (session.accessCount.get() > 0)) {
                continue;
              }
              if (log.isDebugEnabled()) {
                log.debug(sm.getString("persistentManager.swapMaxIdle", new Object[] { session.getIdInternal(), Integer.valueOf(timeIdle) }));
              }
              try
              {
                swapOut(session);
              }
              catch (IOException e) {}
            }
          }
        }
      }
    }
  }
  
  protected void processMaxActiveSwaps()
  {
    if ((!getState().isAvailable()) || (getMaxActiveSessions() < 0)) {
      return;
    }
    Session[] sessions = findSessions();
    if (getMaxActiveSessions() >= sessions.length) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("persistentManager.tooManyActive", new Object[] { Integer.valueOf(sessions.length) }));
    }
    int toswap = sessions.length - getMaxActiveSessions();
    long timeNow = System.currentTimeMillis();
    for (int i = 0; (i < sessions.length) && (toswap > 0); i++)
    {
      StandardSession session = (StandardSession)sessions[i];
      synchronized (session)
      {
        int timeIdle;
        
        if (StandardSession.LAST_ACCESS_AT_START) {
          timeIdle = (int)((timeNow - session.getLastAccessedTimeInternal()) / 1000L);
        } else {
          timeIdle = (int)((timeNow - session.getThisAccessedTimeInternal()) / 1000L);
        }
        if (timeIdle >= this.minIdleSwap)
        {
          if ((session.accessCount != null) && (session.accessCount.get() > 0)) {
            continue;
          }
          if (log.isDebugEnabled()) {
            log.debug(sm.getString("persistentManager.swapTooManyActive", new Object[] { session.getIdInternal(), Integer.valueOf(timeIdle) }));
          }
          try
          {
            swapOut(session);
          }
          catch (IOException e) {}
          toswap--;
        }
      }
    }
  }
  
  protected void processMaxIdleBackups()
  {
    if ((!getState().isAvailable()) || (this.maxIdleBackup < 0)) {
      return;
    }
    Session[] sessions = findSessions();
    long timeNow = System.currentTimeMillis();
    if (this.maxIdleBackup >= 0) {
      for (int i = 0; i < sessions.length; i++)
      {
        StandardSession session = (StandardSession)sessions[i];
        synchronized (session)
        {
          if (session.isValid())
          {
            long lastAccessedTime = session.getLastAccessedTimeInternal();
            Long persistedLastAccessedTime = (Long)session.getNote("org.apache.catalina.session.PersistentManagerBase.persistedLastAccessedTime");
            if ((persistedLastAccessedTime == null) || (lastAccessedTime != persistedLastAccessedTime.longValue()))
            {
              int timeIdle;
              
              if (StandardSession.LAST_ACCESS_AT_START) {
                timeIdle = (int)((timeNow - session.getLastAccessedTimeInternal()) / 1000L);
              } else {
                timeIdle = (int)((timeNow - session.getThisAccessedTimeInternal()) / 1000L);
              }
              if (timeIdle >= this.maxIdleBackup)
              {
                if (log.isDebugEnabled()) {
                  log.debug(sm.getString("persistentManager.backupMaxIdle", new Object[] { session.getIdInternal(), Integer.valueOf(timeIdle) }));
                }
                try
                {
                  writeSession(session);
                }
                catch (IOException e) {}
                session.setNote("org.apache.catalina.session.PersistentManagerBase.persistedLastAccessedTime", Long.valueOf(lastAccessedTime));
              }
            }
          }
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\PersistentManagerBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */