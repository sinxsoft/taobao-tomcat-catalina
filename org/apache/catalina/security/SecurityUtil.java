package org.apache.catalina.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Globals;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public final class SecurityUtil
{
  private static final int INIT = 0;
  private static final int SERVICE = 1;
  private static final int DOFILTER = 1;
  private static final int EVENT = 2;
  private static final int DOFILTEREVENT = 2;
  private static final int DESTROY = 3;
  private static final String INIT_METHOD = "init";
  private static final String DOFILTER_METHOD = "doFilter";
  private static final String SERVICE_METHOD = "service";
  private static final String EVENT_METHOD = "event";
  private static final String DOFILTEREVENT_METHOD = "doFilterEvent";
  private static final String DESTROY_METHOD = "destroy";
  private static final Map<Object, Method[]> classCache = new ConcurrentHashMap();
  private static final Log log = LogFactory.getLog(SecurityUtil.class);
  private static boolean packageDefinitionEnabled = (System.getProperty("package.definition") != null) || (System.getProperty("package.access") != null);
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.security");
  
  public SecurityUtil() {}
  
  public static void doAsPrivilege(String methodName, Servlet targetObject)
    throws Exception
  {
    doAsPrivilege(methodName, targetObject, null, null, null);
  }
  
  public static void doAsPrivilege(String methodName, Servlet targetObject, Class<?>[] targetType, Object[] targetArguments)
    throws Exception
  {
    doAsPrivilege(methodName, targetObject, targetType, targetArguments, null);
  }
  
  public static void doAsPrivilege(String methodName, Servlet targetObject, Class<?>[] targetParameterTypes, Object[] targetArguments, Principal principal)
    throws Exception
  {
    Method method = null;
    Method[] methodsCache = (Method[])classCache.get(Servlet.class);
    if (methodsCache == null)
    {
      method = createMethodAndCacheIt(methodsCache, Servlet.class, methodName, targetParameterTypes);
    }
    else
    {
      method = findMethod(methodsCache, methodName);
      if (method == null) {
        method = createMethodAndCacheIt(methodsCache, Servlet.class, methodName, targetParameterTypes);
      }
    }
    execute(method, targetObject, targetArguments, principal);
  }
  
  public static void doAsPrivilege(String methodName, Filter targetObject)
    throws Exception
  {
    doAsPrivilege(methodName, targetObject, null, null);
  }
  
  public static void doAsPrivilege(String methodName, Filter targetObject, Class<?>[] targetType, Object[] targetArguments)
    throws Exception
  {
    doAsPrivilege(methodName, targetObject, targetType, targetArguments, null);
  }
  
  public static void doAsPrivilege(String methodName, Filter targetObject, Class<?>[] targetParameterTypes, Object[] targetParameterValues, Principal principal)
    throws Exception
  {
    Method method = null;
    Method[] methodsCache = (Method[])classCache.get(Filter.class);
    if (methodsCache == null)
    {
      method = createMethodAndCacheIt(methodsCache, Filter.class, methodName, targetParameterTypes);
    }
    else
    {
      method = findMethod(methodsCache, methodName);
      if (method == null) {
        method = createMethodAndCacheIt(methodsCache, Filter.class, methodName, targetParameterTypes);
      }
    }
    execute(method, targetObject, targetParameterValues, principal);
  }
  
  private static void execute(Method method, final Object targetObject, final Object[] targetArguments, Principal principal)
    throws Exception
  {
    try
    {
      Subject subject = null;
      PrivilegedExceptionAction<Void> pea = new PrivilegedExceptionAction()
      {
        public Void run()
          throws Exception
        {
          method.invoke(targetObject, targetArguments);
          return null;
        }
      };
      if ((targetArguments != null) && ((targetArguments[0] instanceof HttpServletRequest)))
      {
        HttpServletRequest request = (HttpServletRequest)targetArguments[0];
        
        boolean hasSubject = false;
        HttpSession session = request.getSession(false);
        if (session != null)
        {
          subject = (Subject)session.getAttribute("javax.security.auth.subject");
          
          hasSubject = subject != null;
        }
        if (subject == null)
        {
          subject = new Subject();
          if (principal != null) {
            subject.getPrincipals().add(principal);
          }
        }
        if ((session != null) && (!hasSubject)) {
          session.setAttribute("javax.security.auth.subject", subject);
        }
      }
      Subject.doAsPrivileged(subject, pea, null);
    }
    catch (PrivilegedActionException pe)
    {
      Throwable e;
      if ((pe.getException() instanceof InvocationTargetException))
      {
        e = pe.getException().getCause();
        ExceptionUtils.handleThrowable(e);
      }
      else
      {
        e = pe;
      }
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("SecurityUtil.doAsPrivilege"), e);
      }
      if ((e instanceof UnavailableException)) {
        throw ((UnavailableException)e);
      }
      if ((e instanceof ServletException)) {
        throw ((ServletException)e);
      }
      if ((e instanceof IOException)) {
        throw ((IOException)e);
      }
      if ((e instanceof RuntimeException)) {
        throw ((RuntimeException)e);
      }
      throw new ServletException(e.getMessage(), e);
    }
  }
  
  private static Method findMethod(Method[] methodsCache, String methodName)
  {
    if (methodName.equals("init")) {
      return methodsCache[0];
    }
    if (methodName.equals("destroy")) {
      return methodsCache[3];
    }
    if (methodName.equals("service")) {
      return methodsCache[1];
    }
    if (methodName.equals("doFilter")) {
      return methodsCache[1];
    }
    if (methodName.equals("event")) {
      return methodsCache[2];
    }
    if (methodName.equals("doFilterEvent")) {
      return methodsCache[2];
    }
    return null;
  }
  
  private static Method createMethodAndCacheIt(Method[] methodsCache, Class<?> targetType, String methodName, Class<?>[] parameterTypes)
    throws Exception
  {
    if (methodsCache == null) {
      methodsCache = new Method[4];
    }
    Method method = targetType.getMethod(methodName, parameterTypes);
    if (methodName.equals("init")) {
      methodsCache[0] = method;
    } else if (methodName.equals("destroy")) {
      methodsCache[3] = method;
    } else if (methodName.equals("service")) {
      methodsCache[1] = method;
    } else if (methodName.equals("doFilter")) {
      methodsCache[1] = method;
    } else if (methodName.equals("event")) {
      methodsCache[2] = method;
    } else if (methodName.equals("doFilterEvent")) {
      methodsCache[2] = method;
    }
    classCache.put(targetType, methodsCache);
    
    return method;
  }
  
  public static void remove(Object cachedObject)
  {
    classCache.remove(cachedObject);
  }
  
  public static boolean isPackageProtectionEnabled()
  {
    if ((packageDefinitionEnabled) && (Globals.IS_SECURITY_ENABLED)) {
      return true;
    }
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\security\SecurityUtil.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */