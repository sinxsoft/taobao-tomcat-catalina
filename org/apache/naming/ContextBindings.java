package org.apache.naming;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;

public class ContextBindings
{
  private static final Hashtable<Object, Context> contextNameBindings = new Hashtable();
  private static final Hashtable<Thread, Context> threadBindings = new Hashtable();
  private static final Hashtable<Thread, Object> threadNameBindings = new Hashtable();
  private static final Hashtable<ClassLoader, Context> clBindings = new Hashtable();
  private static final Hashtable<ClassLoader, Object> clNameBindings = new Hashtable();
  protected static final StringManager sm = StringManager.getManager("org.apache.naming");
  
  public ContextBindings() {}
  
  public static void bindContext(Object name, Context context)
  {
    bindContext(name, context, null);
  }
  
  public static void bindContext(Object name, Context context, Object token)
  {
    if (ContextAccessController.checkSecurityToken(name, token)) {
      contextNameBindings.put(name, context);
    }
  }
  
  @Deprecated
  public static void unbindContext(Object name)
  {
    unbindContext(name, null);
  }
  
  public static void unbindContext(Object name, Object token)
  {
    if (ContextAccessController.checkSecurityToken(name, token)) {
      contextNameBindings.remove(name);
    }
  }
  
  static Context getContext(Object name)
  {
    return (Context)contextNameBindings.get(name);
  }
  
  @Deprecated
  public static void bindThread(Object name)
    throws NamingException
  {
    bindThread(name, null);
  }
  
  public static void bindThread(Object name, Object token)
    throws NamingException
  {
    if (ContextAccessController.checkSecurityToken(name, token))
    {
      Context context = (Context)contextNameBindings.get(name);
      if (context == null) {
        throw new NamingException(sm.getString("contextBindings.unknownContext", new Object[] { name }));
      }
      threadBindings.put(Thread.currentThread(), context);
      threadNameBindings.put(Thread.currentThread(), name);
    }
  }
  
  @Deprecated
  public static void unbindThread(Object name)
  {
    unbindThread(name, null);
  }
  
  public static void unbindThread(Object name, Object token)
  {
    if (ContextAccessController.checkSecurityToken(name, token))
    {
      threadBindings.remove(Thread.currentThread());
      threadNameBindings.remove(Thread.currentThread());
    }
  }
  
  public static Context getThread()
    throws NamingException
  {
    Context context = (Context)threadBindings.get(Thread.currentThread());
    if (context == null) {
      throw new NamingException(sm.getString("contextBindings.noContextBoundToThread"));
    }
    return context;
  }
  
  static Object getThreadName()
    throws NamingException
  {
    Object name = threadNameBindings.get(Thread.currentThread());
    if (name == null) {
      throw new NamingException(sm.getString("contextBindings.noContextBoundToThread"));
    }
    return name;
  }
  
  public static boolean isThreadBound()
  {
    return threadBindings.containsKey(Thread.currentThread());
  }
  
  @Deprecated
  public static void bindClassLoader(Object name)
    throws NamingException
  {
    bindClassLoader(name, null);
  }
  
  @Deprecated
  public static void bindClassLoader(Object name, Object token)
    throws NamingException
  {
    bindClassLoader(name, token, Thread.currentThread().getContextClassLoader());
  }
  
  public static void bindClassLoader(Object name, Object token, ClassLoader classLoader)
    throws NamingException
  {
    if (ContextAccessController.checkSecurityToken(name, token))
    {
      Context context = (Context)contextNameBindings.get(name);
      if (context == null) {
        throw new NamingException(sm.getString("contextBindings.unknownContext", new Object[] { name }));
      }
      clBindings.put(classLoader, context);
      clNameBindings.put(classLoader, name);
    }
  }
  
  @Deprecated
  public static void unbindClassLoader(Object name)
  {
    unbindClassLoader(name, null);
  }
  
  @Deprecated
  public static void unbindClassLoader(Object name, Object token)
  {
    unbindClassLoader(name, token, Thread.currentThread().getContextClassLoader());
  }
  
  public static void unbindClassLoader(Object name, Object token, ClassLoader classLoader)
  {
    if (ContextAccessController.checkSecurityToken(name, token))
    {
      Object n = clNameBindings.get(classLoader);
      if ((n == null) || (!n.equals(name))) {
        return;
      }
      clBindings.remove(classLoader);
      clNameBindings.remove(classLoader);
    }
  }
  
  public static Context getClassLoader()
    throws NamingException
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Context context = null;
    do
    {
      context = (Context)clBindings.get(cl);
      if (context != null) {
        return context;
      }
    } while ((cl = cl.getParent()) != null);
    throw new NamingException(sm.getString("contextBindings.noContextBoundToCL"));
  }
  
  static Object getClassLoaderName()
    throws NamingException
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Object name = null;
    do
    {
      name = clNameBindings.get(cl);
      if (name != null) {
        return name;
      }
    } while ((cl = cl.getParent()) != null);
    throw new NamingException(sm.getString("contextBindings.noContextBoundToCL"));
  }
  
  public static boolean isClassLoaderBound()
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    do
    {
      if (clBindings.containsKey(cl)) {
        return true;
      }
    } while ((cl = cl.getParent()) != null);
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\ContextBindings.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */