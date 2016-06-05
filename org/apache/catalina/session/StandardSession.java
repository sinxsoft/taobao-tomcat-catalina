package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.security.SecurityUtil;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.security.PrivilegedSetTccl;

public class StandardSession
  implements HttpSession, Session, Serializable
{
  private static final long serialVersionUID = 1L;
  protected static final boolean STRICT_SERVLET_COMPLIANCE = Globals.STRICT_SERVLET_COMPLIANCE;
  protected static final boolean ACTIVITY_CHECK;
  protected static final boolean LAST_ACCESS_AT_START;
  
  static
  {
    String activityCheck = System.getProperty("org.apache.catalina.session.StandardSession.ACTIVITY_CHECK");
    if (activityCheck == null) {
      ACTIVITY_CHECK = STRICT_SERVLET_COMPLIANCE;
    } else {
      ACTIVITY_CHECK = Boolean.valueOf(activityCheck).booleanValue();
    }
    String lastAccessAtStart = System.getProperty("org.apache.catalina.session.StandardSession.LAST_ACCESS_AT_START");
    if (lastAccessAtStart == null) {
      LAST_ACCESS_AT_START = STRICT_SERVLET_COMPLIANCE;
    } else {
      LAST_ACCESS_AT_START = Boolean.valueOf(lastAccessAtStart).booleanValue();
    }
  }
  
  public StandardSession(Manager manager)
  {
    this.manager = manager;
    if (ACTIVITY_CHECK) {
      this.accessCount = new AtomicInteger();
    }
  }
  
  protected static final String[] EMPTY_ARRAY = new String[0];
  protected static final String NOT_SERIALIZED = "___NOT_SERIALIZABLE_EXCEPTION___";
  protected Map<String, Object> attributes = new ConcurrentHashMap();
  protected transient String authType = null;
  protected long creationTime = 0L;
  protected static final String[] excludedAttributes = { "javax.security.auth.subject", "org.apache.catalina.realm.GSS_CREDENTIAL" };
  protected volatile transient boolean expiring = false;
  protected transient StandardSessionFacade facade = null;
  protected String id = null;
  protected static final String info = "StandardSession/1.0";
  protected volatile long lastAccessedTime = this.creationTime;
  protected transient ArrayList<SessionListener> listeners = new ArrayList();
  protected transient Manager manager = null;
  protected int maxInactiveInterval = -1;
  protected boolean isNew = false;
  protected volatile boolean isValid = false;
  protected transient Map<String, Object> notes = new Hashtable();
  protected transient Principal principal = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.session");
  @Deprecated
  protected static volatile HttpSessionContext sessionContext = null;
  protected transient PropertyChangeSupport support = new PropertyChangeSupport(this);
  protected volatile long thisAccessedTime = this.creationTime;
  protected transient AtomicInteger accessCount = null;
  
  public String getAuthType()
  {
    return this.authType;
  }
  
  public void setAuthType(String authType)
  {
    String oldAuthType = this.authType;
    this.authType = authType;
    this.support.firePropertyChange("authType", oldAuthType, this.authType);
  }
  
  public void setCreationTime(long time)
  {
    this.creationTime = time;
    this.lastAccessedTime = time;
    this.thisAccessedTime = time;
  }
  
  public String getId()
  {
    return this.id;
  }
  
  public String getIdInternal()
  {
    return this.id;
  }
  
  public void setId(String id)
  {
    setId(id, true);
  }
  
  public void setId(String id, boolean notify)
  {
    if ((this.id != null) && (this.manager != null)) {
      this.manager.remove(this);
    }
    this.id = id;
    if (this.manager != null) {
      this.manager.add(this);
    }
    if (notify) {
      tellNew();
    }
  }
  
  public void tellNew()
  {
    fireSessionEvent("createSession", null);
    
    Context context = (Context)this.manager.getContainer();
    Object[] listeners = context.getApplicationLifecycleListeners();
    if (listeners != null)
    {
      HttpSessionEvent event = new HttpSessionEvent(getSession());
      for (int i = 0; i < listeners.length; i++) {
        if ((listeners[i] instanceof HttpSessionListener))
        {
          HttpSessionListener listener = (HttpSessionListener)listeners[i];
          try
          {
            context.fireContainerEvent("beforeSessionCreated", listener);
            
            listener.sessionCreated(event);
            context.fireContainerEvent("afterSessionCreated", listener);
          }
          catch (Throwable t)
          {
            ExceptionUtils.handleThrowable(t);
            try
            {
              context.fireContainerEvent("afterSessionCreated", listener);
            }
            catch (Exception e) {}
            this.manager.getContainer().getLogger().error(sm.getString("standardSession.sessionEvent"), t);
          }
        }
      }
    }
  }
  
  public String getInfo()
  {
    return "StandardSession/1.0";
  }
  
  public long getThisAccessedTime()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getThisAccessedTime.ise"));
    }
    return this.thisAccessedTime;
  }
  
  public long getThisAccessedTimeInternal()
  {
    return this.thisAccessedTime;
  }
  
  public long getLastAccessedTime()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getLastAccessedTime.ise"));
    }
    return this.lastAccessedTime;
  }
  
  public long getLastAccessedTimeInternal()
  {
    return this.lastAccessedTime;
  }
  
  public Manager getManager()
  {
    return this.manager;
  }
  
  public void setManager(Manager manager)
  {
    this.manager = manager;
  }
  
  public int getMaxInactiveInterval()
  {
    return this.maxInactiveInterval;
  }
  
  public void setMaxInactiveInterval(int interval)
  {
    this.maxInactiveInterval = interval;
  }
  
  public void setNew(boolean isNew)
  {
    this.isNew = isNew;
  }
  
  public Principal getPrincipal()
  {
    return this.principal;
  }
  
  public void setPrincipal(Principal principal)
  {
    Principal oldPrincipal = this.principal;
    this.principal = principal;
    this.support.firePropertyChange("principal", oldPrincipal, this.principal);
  }
  
  public HttpSession getSession()
  {
    if (this.facade == null) {
      if (SecurityUtil.isPackageProtectionEnabled())
      {
        final StandardSession fsession = this;
        this.facade = ((StandardSessionFacade)AccessController.doPrivileged(new PrivilegedAction()
        {
          public StandardSessionFacade run()
          {
            return new StandardSessionFacade(fsession);
          }
        }));
      }
      else
      {
        this.facade = new StandardSessionFacade(this);
      }
    }
    return this.facade;
  }
  
  public boolean isValid()
  {
    if (!this.isValid) {
      return false;
    }
    if (this.expiring) {
      return true;
    }
    if ((ACTIVITY_CHECK) && (this.accessCount.get() > 0)) {
      return true;
    }
    if (this.maxInactiveInterval > 0)
    {
      long timeNow = System.currentTimeMillis();
      int timeIdle;
      
      if (LAST_ACCESS_AT_START) {
        timeIdle = (int)((timeNow - this.lastAccessedTime) / 1000L);
      } else {
        timeIdle = (int)((timeNow - this.thisAccessedTime) / 1000L);
      }
      if (timeIdle >= this.maxInactiveInterval) {
        expire(true);
      }
    }
    return this.isValid;
  }
  
  public void setValid(boolean isValid)
  {
    this.isValid = isValid;
  }
  
  public void access()
  {
    this.thisAccessedTime = System.currentTimeMillis();
    if (ACTIVITY_CHECK) {
      this.accessCount.incrementAndGet();
    }
  }
  
  public void endAccess()
  {
    this.isNew = false;
    if (LAST_ACCESS_AT_START)
    {
      this.lastAccessedTime = this.thisAccessedTime;
      this.thisAccessedTime = System.currentTimeMillis();
    }
    else
    {
      this.thisAccessedTime = System.currentTimeMillis();
      this.lastAccessedTime = this.thisAccessedTime;
    }
    if (ACTIVITY_CHECK) {
      this.accessCount.decrementAndGet();
    }
  }
  
  public void addSessionListener(SessionListener listener)
  {
    this.listeners.add(listener);
  }
  
  public void expire()
  {
    expire(true);
  }
  
  public void expire(boolean notify)
  {
    if (!this.isValid) {
      return;
    }
    synchronized (this)
    {
      if ((this.expiring) || (!this.isValid)) {
        return;
      }
      if (this.manager == null) {
        return;
      }
      this.expiring = true;
      
      Context context = (Context)this.manager.getContainer();
      
      ClassLoader oldTccl = null;
      if ((context.getLoader() != null) && (context.getLoader().getClassLoader() != null))
      {
        oldTccl = Thread.currentThread().getContextClassLoader();
        if (Globals.IS_SECURITY_ENABLED)
        {
          PrivilegedAction<Void> pa = new PrivilegedSetTccl(context.getLoader().getClassLoader());
          
          AccessController.doPrivileged(pa);
        }
        else
        {
          Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
        }
      }
      try
      {
        Object[] listeners = context.getApplicationLifecycleListeners();
        if ((notify) && (listeners != null))
        {
          HttpSessionEvent event = new HttpSessionEvent(getSession());
          for (int i = 0; i < listeners.length; i++)
          {
            int j = listeners.length - 1 - i;
            if ((listeners[j] instanceof HttpSessionListener))
            {
              HttpSessionListener listener = (HttpSessionListener)listeners[j];
              try
              {
                context.fireContainerEvent("beforeSessionDestroyed", listener);
                
                listener.sessionDestroyed(event);
                context.fireContainerEvent("afterSessionDestroyed", listener);
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
                try
                {
                  context.fireContainerEvent("afterSessionDestroyed", listener);
                }
                catch (Exception e) {}
                this.manager.getContainer().getLogger().error(sm.getString("standardSession.sessionEvent"), t);
              }
            }
          }
        }
      }
      finally
      {
        PrivilegedAction<Void> pa;
        if (oldTccl != null) {
          if (Globals.IS_SECURITY_ENABLED)
          {
            pa = new PrivilegedSetTccl(oldTccl);
            
            AccessController.doPrivileged(pa);
          }
          else
          {
            Thread.currentThread().setContextClassLoader(oldTccl);
          }
        }
      }
      if (ACTIVITY_CHECK) {
        this.accessCount.set(0);
      }
      this.manager.remove(this, true);
      if (notify) {
        fireSessionEvent("destroySession", null);
      }
      if ((this.principal instanceof GenericPrincipal))
      {
        GenericPrincipal gp = (GenericPrincipal)this.principal;
        try
        {
          gp.logout();
        }
        catch (Exception e)
        {
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.logoutfail"), e);
        }
      }
      setValid(false);
      this.expiring = false;
      
      String[] keys = keys();
      if (oldTccl != null) {
        if (Globals.IS_SECURITY_ENABLED)
        {
          PrivilegedAction<Void> pa = new PrivilegedSetTccl(context.getLoader().getClassLoader());
          
          AccessController.doPrivileged(pa);
        }
        else
        {
          Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
        }
      }
      try
      {
        for (int i = 0; i < keys.length; i++) {
          removeAttributeInternal(keys[i], notify);
        }
      }
      finally
      {
        PrivilegedAction<Void> pa;
        if (oldTccl != null) {
          if (Globals.IS_SECURITY_ENABLED)
          {
            pa = new PrivilegedSetTccl(oldTccl);
            
            AccessController.doPrivileged(pa);
          }
          else
          {
            Thread.currentThread().setContextClassLoader(oldTccl);
          }
        }
      }
    }
  }
  
  public void passivate()
  {
    fireSessionEvent("passivateSession", null);
    
    HttpSessionEvent event = null;
    String[] keys = keys();
    for (int i = 0; i < keys.length; i++)
    {
      Object attribute = this.attributes.get(keys[i]);
      if ((attribute instanceof HttpSessionActivationListener))
      {
        if (event == null) {
          event = new HttpSessionEvent(getSession());
        }
        try
        {
          ((HttpSessionActivationListener)attribute).sessionWillPassivate(event);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.attributeEvent"), t);
        }
      }
    }
  }
  
  public void activate()
  {
    if (ACTIVITY_CHECK) {
      this.accessCount = new AtomicInteger();
    }
    fireSessionEvent("activateSession", null);
    
    HttpSessionEvent event = null;
    String[] keys = keys();
    for (int i = 0; i < keys.length; i++)
    {
      Object attribute = this.attributes.get(keys[i]);
      if ((attribute instanceof HttpSessionActivationListener))
      {
        if (event == null) {
          event = new HttpSessionEvent(getSession());
        }
        try
        {
          ((HttpSessionActivationListener)attribute).sessionDidActivate(event);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.attributeEvent"), t);
        }
      }
    }
  }
  
  public Object getNote(String name)
  {
    return this.notes.get(name);
  }
  
  public Iterator<String> getNoteNames()
  {
    return this.notes.keySet().iterator();
  }
  
  public void recycle()
  {
    this.attributes.clear();
    setAuthType(null);
    this.creationTime = 0L;
    this.expiring = false;
    this.id = null;
    this.lastAccessedTime = 0L;
    this.maxInactiveInterval = -1;
    this.notes.clear();
    setPrincipal(null);
    this.isNew = false;
    this.isValid = false;
    this.manager = null;
  }
  
  public void removeNote(String name)
  {
    this.notes.remove(name);
  }
  
  public void removeSessionListener(SessionListener listener)
  {
    this.listeners.remove(listener);
  }
  
  public void setNote(String name, Object value)
  {
    this.notes.put(name, value);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("StandardSession[");
    sb.append(this.id);
    sb.append("]");
    return sb.toString();
  }
  
  public void readObjectData(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    readObject(stream);
  }
  
  public void writeObjectData(ObjectOutputStream stream)
    throws IOException
  {
    writeObject(stream);
  }
  
  public long getCreationTime()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getCreationTime.ise"));
    }
    return this.creationTime;
  }
  
  public long getCreationTimeInternal()
  {
    return this.creationTime;
  }
  
  public ServletContext getServletContext()
  {
    if (this.manager == null) {
      return null;
    }
    Context context = (Context)this.manager.getContainer();
    if (context == null) {
      return null;
    }
    return context.getServletContext();
  }
  
  @Deprecated
  public HttpSessionContext getSessionContext()
  {
    if (sessionContext == null) {
      sessionContext = new StandardSessionContext();
    }
    return sessionContext;
  }
  
  public Object getAttribute(String name)
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getAttribute.ise"));
    }
    if (name == null) {
      return null;
    }
    return this.attributes.get(name);
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getAttributeNames.ise"));
    }
    Set<String> names = new HashSet();
    names.addAll(this.attributes.keySet());
    return Collections.enumeration(names);
  }
  
  @Deprecated
  public Object getValue(String name)
  {
    return getAttribute(name);
  }
  
  @Deprecated
  public String[] getValueNames()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.getValueNames.ise"));
    }
    return keys();
  }
  
  public void invalidate()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.invalidate.ise"));
    }
    expire();
  }
  
  public boolean isNew()
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.isNew.ise"));
    }
    return this.isNew;
  }
  
  @Deprecated
  public void putValue(String name, Object value)
  {
    setAttribute(name, value);
  }
  
  public void removeAttribute(String name)
  {
    removeAttribute(name, true);
  }
  
  public void removeAttribute(String name, boolean notify)
  {
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.removeAttribute.ise"));
    }
    removeAttributeInternal(name, notify);
  }
  
  @Deprecated
  public void removeValue(String name)
  {
    removeAttribute(name);
  }
  
  public void setAttribute(String name, Object value)
  {
    setAttribute(name, value, true);
  }
  
  public void setAttribute(String name, Object value, boolean notify)
  {
    if (name == null) {
      throw new IllegalArgumentException(sm.getString("standardSession.setAttribute.namenull"));
    }
    if (value == null)
    {
      removeAttribute(name);
      return;
    }
    if (!isValidInternal()) {
      throw new IllegalStateException(sm.getString("standardSession.setAttribute.ise", new Object[] { getIdInternal() }));
    }
    if ((this.manager != null) && (this.manager.getDistributable()) && (!isAttributeDistributable(name, value))) {
      throw new IllegalArgumentException(sm.getString("standardSession.setAttribute.iae", new Object[] { name }));
    }
    HttpSessionBindingEvent event = null;
    if ((notify) && ((value instanceof HttpSessionBindingListener)))
    {
      Object oldValue = this.attributes.get(name);
      if (value != oldValue)
      {
        event = new HttpSessionBindingEvent(getSession(), name, value);
        try
        {
          ((HttpSessionBindingListener)value).valueBound(event);
        }
        catch (Throwable t)
        {
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.bindingEvent"), t);
        }
      }
    }
    Object unbound = this.attributes.put(name, value);
    if ((notify) && (unbound != null) && (unbound != value) && ((unbound instanceof HttpSessionBindingListener))) {
      try
      {
        ((HttpSessionBindingListener)unbound).valueUnbound(new HttpSessionBindingEvent(getSession(), name));
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        this.manager.getContainer().getLogger().error(sm.getString("standardSession.bindingEvent"), t);
      }
    }
    if (!notify) {
      return;
    }
    Context context = (Context)this.manager.getContainer();
    Object[] listeners = context.getApplicationEventListeners();
    if (listeners == null) {
      return;
    }
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof HttpSessionAttributeListener))
      {
        HttpSessionAttributeListener listener = (HttpSessionAttributeListener)listeners[i];
        try
        {
          if (unbound != null)
          {
            context.fireContainerEvent("beforeSessionAttributeReplaced", listener);
            if (event == null) {
              event = new HttpSessionBindingEvent(getSession(), name, unbound);
            }
            listener.attributeReplaced(event);
            context.fireContainerEvent("afterSessionAttributeReplaced", listener);
          }
          else
          {
            context.fireContainerEvent("beforeSessionAttributeAdded", listener);
            if (event == null) {
              event = new HttpSessionBindingEvent(getSession(), name, value);
            }
            listener.attributeAdded(event);
            context.fireContainerEvent("afterSessionAttributeAdded", listener);
          }
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          try
          {
            if (unbound != null) {
              context.fireContainerEvent("afterSessionAttributeReplaced", listener);
            } else {
              context.fireContainerEvent("afterSessionAttributeAdded", listener);
            }
          }
          catch (Exception e) {}
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.attributeEvent"), t);
        }
      }
    }
  }
  
  protected boolean isValidInternal()
  {
    return this.isValid;
  }
  
  protected boolean isAttributeDistributable(String name, Object value)
  {
    return value instanceof Serializable;
  }
  
  protected void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    this.authType = null;
    this.creationTime = ((Long)stream.readObject()).longValue();
    this.lastAccessedTime = ((Long)stream.readObject()).longValue();
    this.maxInactiveInterval = ((Integer)stream.readObject()).intValue();
    this.isNew = ((Boolean)stream.readObject()).booleanValue();
    this.isValid = ((Boolean)stream.readObject()).booleanValue();
    this.thisAccessedTime = ((Long)stream.readObject()).longValue();
    this.principal = null;
    
    this.id = ((String)stream.readObject());
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug("readObject() loading session " + this.id);
    }
    if (this.attributes == null) {
      this.attributes = new ConcurrentHashMap();
    }
    int n = ((Integer)stream.readObject()).intValue();
    boolean isValidSave = this.isValid;
    this.isValid = true;
    for (int i = 0; i < n; i++)
    {
      String name = (String)stream.readObject();
      Object value = stream.readObject();
      if ((!(value instanceof String)) || (!value.equals("___NOT_SERIALIZABLE_EXCEPTION___")))
      {
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
          this.manager.getContainer().getLogger().debug("  loading attribute '" + name + "' with value '" + value + "'");
        }
        this.attributes.put(name, value);
      }
    }
    this.isValid = isValidSave;
    if (this.listeners == null) {
      this.listeners = new ArrayList();
    }
    if (this.notes == null) {
      this.notes = new Hashtable();
    }
  }
  
  protected void writeObject(ObjectOutputStream stream)
    throws IOException
  {
    stream.writeObject(Long.valueOf(this.creationTime));
    stream.writeObject(Long.valueOf(this.lastAccessedTime));
    stream.writeObject(Integer.valueOf(this.maxInactiveInterval));
    stream.writeObject(Boolean.valueOf(this.isNew));
    stream.writeObject(Boolean.valueOf(this.isValid));
    stream.writeObject(Long.valueOf(this.thisAccessedTime));
    stream.writeObject(this.id);
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug("writeObject() storing session " + this.id);
    }
    String[] keys = keys();
    ArrayList<String> saveNames = new ArrayList();
    ArrayList<Object> saveValues = new ArrayList();
    for (int i = 0; i < keys.length; i++)
    {
      Object value = this.attributes.get(keys[i]);
      if (value != null) {
        if (((value instanceof Serializable)) && (!exclude(keys[i])))
        {
          saveNames.add(keys[i]);
          saveValues.add(value);
        }
        else
        {
          removeAttributeInternal(keys[i], true);
        }
      }
    }
    int n = saveNames.size();
    stream.writeObject(Integer.valueOf(n));
    for (int i = 0; i < n; i++)
    {
      stream.writeObject(saveNames.get(i));
      try
      {
        stream.writeObject(saveValues.get(i));
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
          this.manager.getContainer().getLogger().debug("  storing attribute '" + (String)saveNames.get(i) + "' with value '" + saveValues.get(i) + "'");
        }
      }
      catch (NotSerializableException e)
      {
        this.manager.getContainer().getLogger().warn(sm.getString("standardSession.notSerializable", new Object[] { saveNames.get(i), this.id }), e);
        
        stream.writeObject("___NOT_SERIALIZABLE_EXCEPTION___");
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
          this.manager.getContainer().getLogger().debug("  storing attribute '" + (String)saveNames.get(i) + "' with value NOT_SERIALIZED");
        }
      }
    }
  }
  
  protected boolean exclude(String name)
  {
    for (int i = 0; i < excludedAttributes.length; i++) {
      if (name.equalsIgnoreCase(excludedAttributes[i])) {
        return true;
      }
    }
    return false;
  }
  
  @Deprecated
  protected void fireContainerEvent(Context context, String type, Object data)
    throws Exception
  {
    if ((context instanceof StandardContext)) {
      ((StandardContext)context).fireContainerEvent(type, data);
    }
  }
  
  public void fireSessionEvent(String type, Object data)
  {
    if (this.listeners.size() < 1) {
      return;
    }
    SessionEvent event = new SessionEvent(this, type, data);
    SessionListener[] list = new SessionListener[0];
    synchronized (this.listeners)
    {
      list = (SessionListener[])this.listeners.toArray(list);
    }
    for (int i = 0; i < list.length; i++) {
      list[i].sessionEvent(event);
    }
  }
  
  protected String[] keys()
  {
    return (String[])this.attributes.keySet().toArray(EMPTY_ARRAY);
  }
  
  protected void removeAttributeInternal(String name, boolean notify)
  {
    if (name == null) {
      return;
    }
    Object value = this.attributes.remove(name);
    if ((!notify) || (value == null)) {
      return;
    }
    HttpSessionBindingEvent event = null;
    if ((value instanceof HttpSessionBindingListener))
    {
      event = new HttpSessionBindingEvent(getSession(), name, value);
      ((HttpSessionBindingListener)value).valueUnbound(event);
    }
    Context context = (Context)this.manager.getContainer();
    Object[] listeners = context.getApplicationEventListeners();
    if (listeners == null) {
      return;
    }
    for (int i = 0; i < listeners.length; i++) {
      if ((listeners[i] instanceof HttpSessionAttributeListener))
      {
        HttpSessionAttributeListener listener = (HttpSessionAttributeListener)listeners[i];
        try
        {
          context.fireContainerEvent("beforeSessionAttributeRemoved", listener);
          if (event == null) {
            event = new HttpSessionBindingEvent(getSession(), name, value);
          }
          listener.attributeRemoved(event);
          context.fireContainerEvent("afterSessionAttributeRemoved", listener);
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
          try
          {
            context.fireContainerEvent("afterSessionAttributeRemoved", listener);
          }
          catch (Exception e) {}
          this.manager.getContainer().getLogger().error(sm.getString("standardSession.attributeEvent"), t);
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\StandardSession.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */