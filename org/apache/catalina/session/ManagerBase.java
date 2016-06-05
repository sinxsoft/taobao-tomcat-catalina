package org.apache.catalina.session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public abstract class ManagerBase
  extends LifecycleMBeanBase
  implements Manager, PropertyChangeListener
{
  private final Log log;
  protected Container container;
  protected boolean distributable;
  private static final String info = "ManagerBase/1.0";
  private static final String name = "ManagerBase";
  protected int maxInactiveInterval;
  protected static final int SESSION_ID_LENGTH_UNSET = -1;
  protected int sessionIdLength;
  protected String secureRandomClass;
  protected String secureRandomAlgorithm;
  protected String secureRandomProvider;
  protected SessionIdGenerator sessionIdGenerator;
  protected Class<? extends SessionIdGenerator> sessionIdGeneratorClass;
  protected volatile int sessionMaxAliveTime;
  private final Object sessionMaxAliveTimeUpdateLock;
  protected static final int TIMING_STATS_CACHE_SIZE = 100;
  protected final Deque<SessionTiming> sessionCreationTiming;
  protected final Deque<SessionTiming> sessionExpirationTiming;
  protected final AtomicLong expiredSessions;
  protected Map<String, Session> sessions;
  protected long sessionCounter;
  protected volatile int maxActive;
  private final Object maxActiveUpdateLock;
  protected int maxActiveSessions;
  protected int rejectedSessions;
  protected volatile int duplicates;
  protected long processingTime;
  private int count;
  protected int processExpiresFrequency;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.session");
  protected final PropertyChangeSupport support;
  
  public ManagerBase()
  {
    this.log = LogFactory.getLog(ManagerBase.class);
    
    this.maxInactiveInterval = 1800;
    
    this.sessionIdLength = -1;
    
    this.secureRandomClass = null;
    
    this.secureRandomAlgorithm = "SHA1PRNG";
    
    this.secureRandomProvider = null;
    
    this.sessionIdGenerator = null;
    this.sessionIdGeneratorClass = null;
    
    this.sessionMaxAliveTimeUpdateLock = new Object();
    
    this.sessionCreationTiming = new LinkedList();
    
    this.sessionExpirationTiming = new LinkedList();
    
    this.expiredSessions = new AtomicLong(0L);
    
    this.sessions = new ConcurrentHashMap();
    
    this.sessionCounter = 0L;
    
    this.maxActive = 0;
    
    this.maxActiveUpdateLock = new Object();
    
    this.maxActiveSessions = -1;
    
    this.rejectedSessions = 0;
    
    this.duplicates = 0;
    
    this.processingTime = 0L;
    
    this.count = 0;
    
    this.processExpiresFrequency = 6;
    
    this.support = new PropertyChangeSupport(this);
  }
  
  public Container getContainer()
  {
    return this.container;
  }
  
  public void setContainer(Container container)
  {
    if ((this.container != null) && ((this.container instanceof Context))) {
      ((Context)this.container).removePropertyChangeListener(this);
    }
    Container oldContainer = this.container;
    this.container = container;
    this.support.firePropertyChange("container", oldContainer, this.container);
    if ((this.container != null) && ((this.container instanceof Context)))
    {
      setMaxInactiveInterval(((Context)this.container).getSessionTimeout() * 60);
      
      ((Context)this.container).addPropertyChangeListener(this);
    }
  }
  
  public String getClassName()
  {
    return getClass().getName();
  }
  
  public boolean getDistributable()
  {
    return this.distributable;
  }
  
  public void setDistributable(boolean distributable)
  {
    boolean oldDistributable = this.distributable;
    this.distributable = distributable;
    this.support.firePropertyChange("distributable", Boolean.valueOf(oldDistributable), Boolean.valueOf(this.distributable));
  }
  
  public String getInfo()
  {
    return "ManagerBase/1.0";
  }
  
  public int getMaxInactiveInterval()
  {
    return this.maxInactiveInterval;
  }
  
  public void setMaxInactiveInterval(int interval)
  {
    int oldMaxInactiveInterval = this.maxInactiveInterval;
    this.maxInactiveInterval = interval;
    this.support.firePropertyChange("maxInactiveInterval", Integer.valueOf(oldMaxInactiveInterval), Integer.valueOf(this.maxInactiveInterval));
  }
  
  @Deprecated
  public int getSessionIdLength()
  {
    return this.sessionIdLength;
  }
  
  @Deprecated
  public void setSessionIdLength(int idLength)
  {
    int oldSessionIdLength = this.sessionIdLength;
    this.sessionIdLength = idLength;
    this.support.firePropertyChange("sessionIdLength", Integer.valueOf(oldSessionIdLength), Integer.valueOf(this.sessionIdLength));
  }
  
  public SessionIdGenerator getSessionIdGenerator()
  {
    if (this.sessionIdGenerator != null) {
      return this.sessionIdGenerator;
    }
    if (this.sessionIdGeneratorClass != null) {
      try
      {
        this.sessionIdGenerator = ((SessionIdGenerator)this.sessionIdGeneratorClass.newInstance());
        return this.sessionIdGenerator;
      }
      catch (IllegalAccessException ex) {}catch (InstantiationException ex) {}
    }
    return null;
  }
  
  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator)
  {
    this.sessionIdGenerator = sessionIdGenerator;
    this.sessionIdGeneratorClass = sessionIdGenerator.getClass();
  }
  
  public String getName()
  {
    return "ManagerBase";
  }
  
  public String getSecureRandomClass()
  {
    return this.secureRandomClass;
  }
  
  public void setSecureRandomClass(String secureRandomClass)
  {
    String oldSecureRandomClass = this.secureRandomClass;
    this.secureRandomClass = secureRandomClass;
    this.support.firePropertyChange("secureRandomClass", oldSecureRandomClass, this.secureRandomClass);
  }
  
  public String getSecureRandomAlgorithm()
  {
    return this.secureRandomAlgorithm;
  }
  
  public void setSecureRandomAlgorithm(String secureRandomAlgorithm)
  {
    this.secureRandomAlgorithm = secureRandomAlgorithm;
  }
  
  public String getSecureRandomProvider()
  {
    return this.secureRandomProvider;
  }
  
  public void setSecureRandomProvider(String secureRandomProvider)
  {
    this.secureRandomProvider = secureRandomProvider;
  }
  
  public int getRejectedSessions()
  {
    return this.rejectedSessions;
  }
  
  public long getExpiredSessions()
  {
    return this.expiredSessions.get();
  }
  
  public void setExpiredSessions(long expiredSessions)
  {
    this.expiredSessions.set(expiredSessions);
  }
  
  public long getProcessingTime()
  {
    return this.processingTime;
  }
  
  public void setProcessingTime(long processingTime)
  {
    this.processingTime = processingTime;
  }
  
  public int getProcessExpiresFrequency()
  {
    return this.processExpiresFrequency;
  }
  
  public void setProcessExpiresFrequency(int processExpiresFrequency)
  {
    if (processExpiresFrequency <= 0) {
      return;
    }
    int oldProcessExpiresFrequency = this.processExpiresFrequency;
    this.processExpiresFrequency = processExpiresFrequency;
    this.support.firePropertyChange("processExpiresFrequency", Integer.valueOf(oldProcessExpiresFrequency), Integer.valueOf(this.processExpiresFrequency));
  }
  
  public void backgroundProcess()
  {
    this.count = ((this.count + 1) % this.processExpiresFrequency);
    if (this.count == 0) {
      processExpires();
    }
  }
  
  public void processExpires()
  {
    long timeNow = System.currentTimeMillis();
    Session[] sessions = findSessions();
    int expireHere = 0;
    if (this.log.isDebugEnabled()) {
      this.log.debug("Start expire sessions " + getName() + " at " + timeNow + " sessioncount " + sessions.length);
    }
    for (int i = 0; i < sessions.length; i++) {
      if ((sessions[i] != null) && (!sessions[i].isValid())) {
        expireHere++;
      }
    }
    long timeEnd = System.currentTimeMillis();
    if (this.log.isDebugEnabled()) {
      this.log.debug("End expire sessions " + getName() + " processingTime " + (timeEnd - timeNow) + " expired sessions: " + expireHere);
    }
    this.processingTime += timeEnd - timeNow;
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
    
    setDistributable(((Context)getContainer()).getDistributable());
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    while (this.sessionCreationTiming.size() < 100) {
      this.sessionCreationTiming.add(null);
    }
    while (this.sessionExpirationTiming.size() < 100) {
      this.sessionExpirationTiming.add(null);
    }
    SessionIdGenerator sessionIdGenerator = getSessionIdGenerator();
    if (sessionIdGenerator == null)
    {
      sessionIdGenerator = new StandardSessionIdGenerator();
      setSessionIdGenerator(sessionIdGenerator);
    }
    if (this.sessionIdLength != -1) {
      sessionIdGenerator.setSessionIdLength(this.sessionIdLength);
    }
    sessionIdGenerator.setJvmRoute(getJvmRoute());
    if ((sessionIdGenerator instanceof SessionIdGeneratorBase))
    {
      SessionIdGeneratorBase sig = (SessionIdGeneratorBase)sessionIdGenerator;
      sig.setSecureRandomAlgorithm(getSecureRandomAlgorithm());
      sig.setSecureRandomClass(getSecureRandomClass());
      sig.setSecureRandomProvider(getSecureRandomProvider());
    }
    if ((sessionIdGenerator instanceof Lifecycle))
    {
      ((Lifecycle)sessionIdGenerator).start();
    }
    else
    {
      if (this.log.isDebugEnabled()) {
        this.log.debug("Force random number initialization starting");
      }
      sessionIdGenerator.generateSessionId();
      if (this.log.isDebugEnabled()) {
        this.log.debug("Force random number initialization completed");
      }
    }
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    if ((this.sessionIdGenerator instanceof Lifecycle)) {
      ((Lifecycle)this.sessionIdGenerator).stop();
    }
  }
  
  public void add(Session session)
  {
    this.sessions.put(session.getIdInternal(), session);
    int size = getActiveSessions();
    if (size > this.maxActive) {
      synchronized (this.maxActiveUpdateLock)
      {
        if (size > this.maxActive) {
          this.maxActive = size;
        }
      }
    }
  }
  
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.addPropertyChangeListener(listener);
  }
  
  public Session createSession(String sessionId)
  {
    if ((this.maxActiveSessions >= 0) && (getActiveSessions() >= this.maxActiveSessions))
    {
      this.rejectedSessions += 1;
      throw new TooManyActiveSessionsException(sm.getString("managerBase.createSession.ise"), this.maxActiveSessions);
    }
    Session session = createEmptySession();
    
    session.setNew(true);
    session.setValid(true);
    session.setCreationTime(System.currentTimeMillis());
    session.setMaxInactiveInterval(this.maxInactiveInterval);
    String id = sessionId;
    if (id == null) {
      id = generateSessionId();
    }
    session.setId(id);
    this.sessionCounter += 1L;
    
    SessionTiming timing = new SessionTiming(session.getCreationTime(), 0);
    synchronized (this.sessionCreationTiming)
    {
      this.sessionCreationTiming.add(timing);
      this.sessionCreationTiming.poll();
    }
    return session;
  }
  
  public Session createEmptySession()
  {
    return getNewSession();
  }
  
  public Session findSession(String id)
    throws IOException
  {
    if (id == null) {
      return null;
    }
    return (Session)this.sessions.get(id);
  }
  
  public Session[] findSessions()
  {
    return (Session[])this.sessions.values().toArray(new Session[0]);
  }
  
  public void remove(Session session)
  {
    remove(session, false);
  }
  
  public void remove(Session session, boolean update)
  {
    if (update)
    {
      long timeNow = System.currentTimeMillis();
      int timeAlive = (int)(timeNow - session.getCreationTimeInternal()) / 1000;
      
      updateSessionMaxAliveTime(timeAlive);
      this.expiredSessions.incrementAndGet();
      SessionTiming timing = new SessionTiming(timeNow, timeAlive);
      synchronized (this.sessionExpirationTiming)
      {
        this.sessionExpirationTiming.add(timing);
        this.sessionExpirationTiming.poll();
      }
    }
    if (session.getIdInternal() != null) {
      this.sessions.remove(session.getIdInternal());
    }
  }
  
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.support.removePropertyChangeListener(listener);
  }
  
  public void changeSessionId(Session session)
  {
    String oldId = session.getIdInternal();
    session.setId(generateSessionId(), false);
    String newId = session.getIdInternal();
    this.container.fireContainerEvent("changeSessionId", new String[] { oldId, newId });
  }
  
  protected StandardSession getNewSession()
  {
    return new StandardSession(this);
  }
  
  protected String generateSessionId()
  {
    String result = null;
    do
    {
      if (result != null) {
        this.duplicates += 1;
      }
      result = this.sessionIdGenerator.generateSessionId();
    } while (this.sessions.containsKey(result));
    return result;
  }
  
  public Engine getEngine()
  {
    Engine e = null;
    for (Container c = getContainer(); (e == null) && (c != null); c = c.getParent()) {
      if ((c instanceof Engine)) {
        e = (Engine)c;
      }
    }
    return e;
  }
  
  public String getJvmRoute()
  {
    Engine e = getEngine();
    return e == null ? null : e.getJvmRoute();
  }
  
  public void setSessionCounter(long sessionCounter)
  {
    this.sessionCounter = sessionCounter;
  }
  
  public long getSessionCounter()
  {
    return this.sessionCounter;
  }
  
  public int getDuplicates()
  {
    return this.duplicates;
  }
  
  public void setDuplicates(int duplicates)
  {
    this.duplicates = duplicates;
  }
  
  public int getActiveSessions()
  {
    return this.sessions.size();
  }
  
  public int getMaxActive()
  {
    return this.maxActive;
  }
  
  public void setMaxActive(int maxActive)
  {
    synchronized (this.maxActiveUpdateLock)
    {
      this.maxActive = maxActive;
    }
  }
  
  public int getMaxActiveSessions()
  {
    return this.maxActiveSessions;
  }
  
  public void setMaxActiveSessions(int max)
  {
    int oldMaxActiveSessions = this.maxActiveSessions;
    this.maxActiveSessions = max;
    this.support.firePropertyChange("maxActiveSessions", Integer.valueOf(oldMaxActiveSessions), Integer.valueOf(this.maxActiveSessions));
  }
  
  public int getSessionMaxAliveTime()
  {
    return this.sessionMaxAliveTime;
  }
  
  public void setSessionMaxAliveTime(int sessionMaxAliveTime)
  {
    synchronized (this.sessionMaxAliveTimeUpdateLock)
    {
      this.sessionMaxAliveTime = sessionMaxAliveTime;
    }
  }
  
  public void updateSessionMaxAliveTime(int sessionAliveTime)
  {
    if (sessionAliveTime > this.sessionMaxAliveTime) {
      synchronized (this.sessionMaxAliveTimeUpdateLock)
      {
        if (sessionAliveTime > this.sessionMaxAliveTime) {
          this.sessionMaxAliveTime = sessionAliveTime;
        }
      }
    }
  }
  
  public int getSessionAverageAliveTime()
  {
    List<SessionTiming> copy = new ArrayList();
    synchronized (this.sessionExpirationTiming)
    {
      copy.addAll(this.sessionExpirationTiming);
    }
    int counter = 0;
    int result = 0;
    Iterator<SessionTiming> iter = copy.iterator();
    while (iter.hasNext())
    {
      SessionTiming timing = (SessionTiming)iter.next();
      if (timing != null)
      {
        int timeAlive = timing.getDuration();
        counter++;
        
        result = result * ((counter - 1) / counter) + timeAlive / counter;
      }
    }
    return result;
  }
  
  public int getSessionCreateRate()
  {
    long now = System.currentTimeMillis();
    
    List<SessionTiming> copy = new ArrayList();
    synchronized (this.sessionCreationTiming)
    {
      copy.addAll(this.sessionCreationTiming);
    }
    long oldest = now;
    int counter = 0;
    int result = 0;
    Iterator<SessionTiming> iter = copy.iterator();
    while (iter.hasNext())
    {
      SessionTiming timing = (SessionTiming)iter.next();
      if (timing != null)
      {
        counter++;
        if (timing.getTimestamp() < oldest) {
          oldest = timing.getTimestamp();
        }
      }
    }
    if (counter > 0) {
      if (oldest < now) {
        result = 60000 * counter / (int)(now - oldest);
      } else {
        result = Integer.MAX_VALUE;
      }
    }
    return result;
  }
  
  public int getSessionExpireRate()
  {
    long now = System.currentTimeMillis();
    
    List<SessionTiming> copy = new ArrayList();
    synchronized (this.sessionExpirationTiming)
    {
      copy.addAll(this.sessionExpirationTiming);
    }
    long oldest = now;
    int counter = 0;
    int result = 0;
    Iterator<SessionTiming> iter = copy.iterator();
    while (iter.hasNext())
    {
      SessionTiming timing = (SessionTiming)iter.next();
      if (timing != null)
      {
        counter++;
        if (timing.getTimestamp() < oldest) {
          oldest = timing.getTimestamp();
        }
      }
    }
    if (counter > 0) {
      if (oldest < now) {
        result = 60000 * counter / (int)(now - oldest);
      } else {
        result = Integer.MAX_VALUE;
      }
    }
    return result;
  }
  
  public String listSessionIds()
  {
    StringBuilder sb = new StringBuilder();
    Iterator<String> keys = this.sessions.keySet().iterator();
    while (keys.hasNext()) {
      sb.append((String)keys.next()).append(" ");
    }
    return sb.toString();
  }
  
  public String getSessionAttribute(String sessionId, String key)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return null;
    }
    Object o = s.getSession().getAttribute(key);
    if (o == null) {
      return null;
    }
    return o.toString();
  }
  
  public HashMap<String, String> getSession(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return null;
    }
    Enumeration<String> ee = s.getSession().getAttributeNames();
    if ((ee == null) || (!ee.hasMoreElements())) {
      return null;
    }
    HashMap<String, String> map = new HashMap();
    while (ee.hasMoreElements())
    {
      String attrName = (String)ee.nextElement();
      map.put(attrName, getSessionAttribute(sessionId, attrName));
    }
    return map;
  }
  
  public void expireSession(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return;
    }
    s.expire();
  }
  
  public long getThisAccessedTimestamp(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null) {
      return -1L;
    }
    return s.getThisAccessedTime();
  }
  
  public String getThisAccessedTime(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return "";
    }
    return new Date(s.getThisAccessedTime()).toString();
  }
  
  public long getLastAccessedTimestamp(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null) {
      return -1L;
    }
    return s.getLastAccessedTime();
  }
  
  public String getLastAccessedTime(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return "";
    }
    return new Date(s.getLastAccessedTime()).toString();
  }
  
  public String getCreationTime(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null)
    {
      if (this.log.isInfoEnabled()) {
        this.log.info("Session not found " + sessionId);
      }
      return "";
    }
    return new Date(s.getCreationTime()).toString();
  }
  
  public long getCreationTimestamp(String sessionId)
  {
    Session s = (Session)this.sessions.get(sessionId);
    if (s == null) {
      return -1L;
    }
    return s.getCreationTime();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append('[');
    if (this.container == null) {
      sb.append("Container is null");
    } else {
      sb.append(this.container.getName());
    }
    sb.append(']');
    return sb.toString();
  }
  
  public String getObjectNameKeyProperties()
  {
    StringBuilder name = new StringBuilder("type=Manager");
    if ((this.container instanceof Context))
    {
      name.append(",context=");
      String contextName = this.container.getName();
      if (!contextName.startsWith("/")) {
        name.append('/');
      }
      name.append(contextName);
      
      Context context = (Context)this.container;
      name.append(",host=");
      name.append(context.getParent().getName());
    }
    else
    {
      name.append(",container=");
      name.append(this.container.getName());
    }
    return name.toString();
  }
  
  public String getDomainInternal()
  {
    return MBeanUtils.getDomain(this.container);
  }
  
  public void propertyChange(PropertyChangeEvent event)
  {
    if (!(event.getSource() instanceof Context)) {
      return;
    }
    if (event.getPropertyName().equals("sessionTimeout")) {
      try
      {
        setMaxInactiveInterval(((Integer)event.getNewValue()).intValue() * 60);
      }
      catch (NumberFormatException e)
      {
        this.log.error(sm.getString("managerBase.sessionTimeout", new Object[] { event.getNewValue() }));
      }
    }
  }
  
  protected static final class SessionTiming
  {
    private final long timestamp;
    private final int duration;
    
    public SessionTiming(long timestamp, int duration)
    {
      this.timestamp = timestamp;
      this.duration = duration;
    }
    
    public long getTimestamp()
    {
      return this.timestamp;
    }
    
    public int getDuration()
    {
      return this.duration;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\ManagerBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */