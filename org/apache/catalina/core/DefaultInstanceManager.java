package org.apache.catalina.core;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.xml.ws.WebServiceRef;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Globals;
import org.apache.catalina.Loader;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.Introspection;
import org.apache.juli.logging.Log;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class DefaultInstanceManager
  implements InstanceManager
{
  private static final AnnotationCacheEntry[] ANNOTATIONS_EMPTY = new AnnotationCacheEntry[0];
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private final javax.naming.Context context;
  private final Map<String, Map<String, String>> injectionMap;
  protected final ClassLoader classLoader;
  protected final ClassLoader containerClassLoader;
  protected boolean privileged;
  protected boolean ignoreAnnotations;
  private final Properties restrictedFilters;
  private final Properties restrictedListeners;
  private final Properties restrictedServlets;
  private final Map<Class<?>, AnnotationCacheEntry[]> annotationCache = new WeakHashMap();
  private final Map<String, String> postConstructMethods;
  private final Map<String, String> preDestroyMethods;
  
  public DefaultInstanceManager(javax.naming.Context context, Map<String, Map<String, String>> injectionMap, org.apache.catalina.Context catalinaContext, ClassLoader containerClassLoader)
  {
    this.classLoader = catalinaContext.getLoader().getClassLoader();
    this.privileged = catalinaContext.getPrivileged();
    this.containerClassLoader = containerClassLoader;
    this.ignoreAnnotations = catalinaContext.getIgnoreAnnotations();
    StringManager sm = StringManager.getManager("org.apache.catalina.core");
    this.restrictedServlets = loadProperties("org/apache/catalina/core/RestrictedServlets.properties", sm.getString("defaultInstanceManager.restrictedServletsResource"), catalinaContext.getLogger());
    
    this.restrictedListeners = loadProperties("org/apache/catalina/core/RestrictedListeners.properties", "defaultInstanceManager.restrictedListenersResources", catalinaContext.getLogger());
    
    this.restrictedFilters = loadProperties("org/apache/catalina/core/RestrictedFilters.properties", "defaultInstanceManager.restrictedFiltersResource", catalinaContext.getLogger());
    
    this.context = context;
    this.injectionMap = injectionMap;
    this.postConstructMethods = catalinaContext.findPostConstructMethods();
    this.preDestroyMethods = catalinaContext.findPreDestroyMethods();
  }
  
  public Object newInstance(Class<?> clazz)
    throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException
  {
    return newInstance(clazz.newInstance(), clazz);
  }
  
  public Object newInstance(String className)
    throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException
  {
    Class<?> clazz = loadClassMaybePrivileged(className, this.classLoader);
    return newInstance(clazz.newInstance(), clazz);
  }
  
  public Object newInstance(String className, ClassLoader classLoader)
    throws IllegalAccessException, NamingException, InvocationTargetException, InstantiationException, ClassNotFoundException
  {
    Class<?> clazz = classLoader.loadClass(className);
    return newInstance(clazz.newInstance(), clazz);
  }
  
  public void newInstance(Object o)
    throws IllegalAccessException, InvocationTargetException, NamingException
  {
    newInstance(o, o.getClass());
  }
  
  private Object newInstance(Object instance, Class<?> clazz)
    throws IllegalAccessException, InvocationTargetException, NamingException
  {
    if (!this.ignoreAnnotations)
    {
      Map<String, String> injections = assembleInjectionsFromClassHierarchy(clazz);
      populateAnnotationsCache(clazz, injections);
      processAnnotations(instance, injections);
      postConstruct(instance, clazz);
    }
    return instance;
  }
  
  private Map<String, String> assembleInjectionsFromClassHierarchy(Class<?> clazz)
  {
    Map<String, String> injections = new HashMap();
    Map<String, String> currentInjections = null;
    while (clazz != null)
    {
      currentInjections = (Map)this.injectionMap.get(clazz.getName());
      if (currentInjections != null) {
        injections.putAll(currentInjections);
      }
      clazz = clazz.getSuperclass();
    }
    return injections;
  }
  
  public void destroyInstance(Object instance)
    throws IllegalAccessException, InvocationTargetException
  {
    if (!this.ignoreAnnotations) {
      preDestroy(instance, instance.getClass());
    }
  }
  
  protected void postConstruct(Object instance, Class<?> clazz)
    throws IllegalAccessException, InvocationTargetException
  {
    if (this.context == null) {
      return;
    }
    Class<?> superClass = clazz.getSuperclass();
    if (superClass != Object.class) {
      postConstruct(instance, superClass);
    }
    AnnotationCacheEntry[] annotations;
    synchronized (this.annotationCache)
    {
      annotations = (AnnotationCacheEntry[])this.annotationCache.get(clazz);
    }
    for (AnnotationCacheEntry entry : annotations) {
      if (entry.getType() == AnnotationCacheEntryType.POST_CONSTRUCT)
      {
        Method postConstruct = getMethod(clazz, entry);
        synchronized (postConstruct)
        {
          boolean accessibility = postConstruct.isAccessible();
          postConstruct.setAccessible(true);
          postConstruct.invoke(instance, new Object[0]);
          postConstruct.setAccessible(accessibility);
        }
      }
    }
  }
  
  protected void preDestroy(Object instance, Class<?> clazz)
    throws IllegalAccessException, InvocationTargetException
  {
    Class<?> superClass = clazz.getSuperclass();
    if (superClass != Object.class) {
      preDestroy(instance, superClass);
    }
    AnnotationCacheEntry[] annotations = null;
    synchronized (this.annotationCache)
    {
      annotations = (AnnotationCacheEntry[])this.annotationCache.get(clazz);
    }
    if (annotations == null) {
      return;
    }
    for (AnnotationCacheEntry entry : annotations) {
      if (entry.getType() == AnnotationCacheEntryType.PRE_DESTROY)
      {
        Method preDestroy = getMethod(clazz, entry);
        synchronized (preDestroy)
        {
          boolean accessibility = preDestroy.isAccessible();
          preDestroy.setAccessible(true);
          preDestroy.invoke(instance, new Object[0]);
          preDestroy.setAccessible(accessibility);
        }
      }
    }
  }
  
  protected void populateAnnotationsCache(Class<?> clazz, Map<String, String> injections)
    throws IllegalAccessException, InvocationTargetException, NamingException
  {
    List<AnnotationCacheEntry> annotations = null;
    while (clazz != null)
    {
      AnnotationCacheEntry[] annotationsArray = null;
      synchronized (this.annotationCache)
      {
        annotationsArray = (AnnotationCacheEntry[])this.annotationCache.get(clazz);
      }
      if (annotationsArray == null)
      {
        if (annotations == null) {
          annotations = new ArrayList();
        } else {
          annotations.clear();
        }
        if (this.context != null)
        {
          Field[] fields = Introspection.getDeclaredFields(clazz);
          for (Field field : fields) {
            if ((injections != null) && (injections.containsKey(field.getName())))
            {
              annotations.add(new AnnotationCacheEntry(field.getName(), null, (String)injections.get(field.getName()), AnnotationCacheEntryType.FIELD));
            }
            else
            {
              Resource resourceAnnotation;
              if ((resourceAnnotation = (Resource)field.getAnnotation(Resource.class)) != null)
              {
                annotations.add(new AnnotationCacheEntry(field.getName(), null, resourceAnnotation.name(), AnnotationCacheEntryType.FIELD));
              }
              else
              {
                EJB ejbAnnotation;
                if ((ejbAnnotation = (EJB)field.getAnnotation(EJB.class)) != null)
                {
                  annotations.add(new AnnotationCacheEntry(field.getName(), null, ejbAnnotation.name(), AnnotationCacheEntryType.FIELD));
                }
                else
                {
                  WebServiceRef webServiceRefAnnotation;
                  if ((webServiceRefAnnotation = (WebServiceRef)field.getAnnotation(WebServiceRef.class)) != null)
                  {
                    annotations.add(new AnnotationCacheEntry(field.getName(), null, webServiceRefAnnotation.name(), AnnotationCacheEntryType.FIELD));
                  }
                  else
                  {
                    PersistenceContext persistenceContextAnnotation;
                    if ((persistenceContextAnnotation = (PersistenceContext)field.getAnnotation(PersistenceContext.class)) != null)
                    {
                      annotations.add(new AnnotationCacheEntry(field.getName(), null, persistenceContextAnnotation.name(), AnnotationCacheEntryType.FIELD));
                    }
                    else
                    {
                      PersistenceUnit persistenceUnitAnnotation;
                      if ((persistenceUnitAnnotation = (PersistenceUnit)field.getAnnotation(PersistenceUnit.class)) != null) {
                        annotations.add(new AnnotationCacheEntry(field.getName(), null, persistenceUnitAnnotation.name(), AnnotationCacheEntryType.FIELD));
                      }
                    }
                  }
                }
              }
            }
          }
        }
        Method[] methods = Introspection.getDeclaredMethods(clazz);
        Method postConstruct = null;
        String postConstructFromXml = (String)this.postConstructMethods.get(clazz.getName());
        Method preDestroy = null;
        String preDestroyFromXml = (String)this.preDestroyMethods.get(clazz.getName());
        for (Method method : methods)
        {
          if (this.context != null)
          {
            if ((injections != null) && (Introspection.isValidSetter(method)))
            {
              String fieldName = Introspection.getPropertyName(method);
              if (injections.containsKey(fieldName))
              {
                annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), (String)injections.get(fieldName), AnnotationCacheEntryType.SETTER));
                
                continue;
              }
            }
            Resource resourceAnnotation;
            if ((resourceAnnotation = (Resource)method.getAnnotation(Resource.class)) != null)
            {
              annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), resourceAnnotation.name(), AnnotationCacheEntryType.SETTER));
            }
            else
            {
              EJB ejbAnnotation;
              if ((ejbAnnotation = (EJB)method.getAnnotation(EJB.class)) != null)
              {
                annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), ejbAnnotation.name(), AnnotationCacheEntryType.SETTER));
              }
              else
              {
                WebServiceRef webServiceRefAnnotation;
                if ((webServiceRefAnnotation = (WebServiceRef)method.getAnnotation(WebServiceRef.class)) != null)
                {
                  annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), webServiceRefAnnotation.name(), AnnotationCacheEntryType.SETTER));
                }
                else
                {
                  PersistenceContext persistenceContextAnnotation;
                  if ((persistenceContextAnnotation = (PersistenceContext)method.getAnnotation(PersistenceContext.class)) != null)
                  {
                    annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), persistenceContextAnnotation.name(), AnnotationCacheEntryType.SETTER));
                  }
                  else
                  {
                    PersistenceUnit persistenceUnitAnnotation;
                    if ((persistenceUnitAnnotation = (PersistenceUnit)method.getAnnotation(PersistenceUnit.class)) != null) {
                      annotations.add(new AnnotationCacheEntry(method.getName(), method.getParameterTypes(), persistenceUnitAnnotation.name(), AnnotationCacheEntryType.SETTER));
                    }
                  }
                }
              }
            }
          }
          postConstruct = findPostConstruct(postConstruct, postConstructFromXml, method);
          
          preDestroy = findPreDestroy(preDestroy, preDestroyFromXml, method);
        }
        if (postConstruct != null) {
          annotations.add(new AnnotationCacheEntry(postConstruct.getName(), postConstruct.getParameterTypes(), null, AnnotationCacheEntryType.POST_CONSTRUCT));
        } else if (postConstructFromXml != null) {
          throw new IllegalArgumentException("Post construct method " + postConstructFromXml + " for class " + clazz.getName() + " is declared in deployment descriptor but cannot be found.");
        }
        if (preDestroy != null) {
          annotations.add(new AnnotationCacheEntry(preDestroy.getName(), preDestroy.getParameterTypes(), null, AnnotationCacheEntryType.PRE_DESTROY));
        } else if (preDestroyFromXml != null) {
          throw new IllegalArgumentException("Pre destroy method " + preDestroyFromXml + " for class " + clazz.getName() + " is declared in deployment descriptor but cannot be found.");
        }
        if (annotations.isEmpty()) {
          annotationsArray = ANNOTATIONS_EMPTY;
        } else {
          annotationsArray = (AnnotationCacheEntry[])annotations.toArray(new AnnotationCacheEntry[annotations.size()]);
        }
        synchronized (this.annotationCache)
        {
          this.annotationCache.put(clazz, annotationsArray);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }
  
  protected void processAnnotations(Object instance, Map<String, String> injections)
    throws IllegalAccessException, InvocationTargetException, NamingException
  {
    if (this.context == null) {
      return;
    }
    Class<?> clazz = instance.getClass();
    while (clazz != null)
    {
      AnnotationCacheEntry[] annotations;
      synchronized (this.annotationCache)
      {
        annotations = (AnnotationCacheEntry[])this.annotationCache.get(clazz);
      }
      for (AnnotationCacheEntry entry : annotations) {
        if (entry.getType() == AnnotationCacheEntryType.SETTER) {
          lookupMethodResource(this.context, instance, getMethod(clazz, entry), entry.getName(), clazz);
        } else if (entry.getType() == AnnotationCacheEntryType.FIELD) {
          lookupFieldResource(this.context, instance, getField(clazz, entry), entry.getName(), clazz);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }
  

  protected int getAnnotationCacheSize()
  {
	    synchronized (annotationCache) {
            return annotationCache.size();
        }
  }
  
  protected Class<?> loadClassMaybePrivileged(final String className, final ClassLoader classLoader)
    throws ClassNotFoundException
  {
    Class<?> clazz;
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        clazz = (Class)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Class<?> run()
            throws Exception
          {
            return DefaultInstanceManager.this.loadClass(className, classLoader);
          }
        });
      }
      catch (PrivilegedActionException e)
      {
        Throwable t = e.getCause();
        if ((t instanceof ClassNotFoundException)) {
          throw ((ClassNotFoundException)t);
        }
        throw new RuntimeException(t);
      }
    } else {
      clazz = loadClass(className, classLoader);
    }
    checkAccess(clazz);
    return clazz;
  }
  
  protected Class<?> loadClass(String className, ClassLoader classLoader)
    throws ClassNotFoundException
  {
    if (className.startsWith("org.apache.catalina")) {
      return this.containerClassLoader.loadClass(className);
    }
    try
    {
      Class<?> clazz = this.containerClassLoader.loadClass(className);
      if (ContainerServlet.class.isAssignableFrom(clazz)) {
        return clazz;
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    return classLoader.loadClass(className);
  }
  
  private void checkAccess(Class<?> clazz)
  {
    if (this.privileged) {
      return;
    }
    if (Filter.class.isAssignableFrom(clazz))
    {
      checkAccess(clazz, this.restrictedFilters);
    }
    else if (Servlet.class.isAssignableFrom(clazz))
    {
      if (ContainerServlet.class.isAssignableFrom(clazz)) {
        throw new SecurityException("Restricted (ContainerServlet) " + clazz);
      }
      checkAccess(clazz, this.restrictedServlets);
    }
    else
    {
      checkAccess(clazz, this.restrictedListeners);
    }
  }
  
  private void checkAccess(Class<?> clazz, Properties restricted)
  {
    while (clazz != null)
    {
      if ("restricted".equals(restricted.getProperty(clazz.getName()))) {
        throw new SecurityException("Restricted " + clazz);
      }
      clazz = clazz.getSuperclass();
    }
  }
  
  protected static void lookupFieldResource(javax.naming.Context context, Object instance, Field field, String name, Class<?> clazz)
    throws NamingException, IllegalAccessException
  {
    String normalizedName = normalize(name);
    Object lookedupResource;
    
    if ((normalizedName != null) && (normalizedName.length() > 0)) {
      lookedupResource = context.lookup(normalizedName);
    } else {
      lookedupResource = context.lookup(clazz.getName() + "/" + field.getName());
    }
    synchronized (field)
    {
      boolean accessibility = field.isAccessible();
      field.setAccessible(true);
      field.set(instance, lookedupResource);
      field.setAccessible(accessibility);
    }
  }
  
  protected static void lookupMethodResource(javax.naming.Context context, Object instance, Method method, String name, Class<?> clazz)
    throws NamingException, IllegalAccessException, InvocationTargetException
  {
    if (!Introspection.isValidSetter(method)) {
      throw new IllegalArgumentException(sm.getString("defaultInstanceManager.invalidInjection"));
    }
    String normalizedName = normalize(name);
    Object lookedupResource;
    
    if ((normalizedName != null) && (normalizedName.length() > 0)) {
      lookedupResource = context.lookup(normalizedName);
    } else {
      lookedupResource = context.lookup(clazz.getName() + "/" + Introspection.getPropertyName(method));
    }
    synchronized (method)
    {
      boolean accessibility = method.isAccessible();
      method.setAccessible(true);
      method.invoke(instance, new Object[] { lookedupResource });
      method.setAccessible(accessibility);
    }
  }
  
  @Deprecated
  public static String getName(Method setter)
  {
    return Introspector.decapitalize(setter.getName().substring(3));
  }
  
  private static Properties loadProperties(String resourceName, String errorString, Log log)
  {
	  Properties result = new Properties();
    ClassLoader cl = DefaultInstanceManager.class.getClassLoader();
    InputStream is = null;
    try
    {
      is = cl.getResourceAsStream(resourceName);
      if (is == null) {
        log.error(errorString);
      } else {
        result.load(is);
      }
      return result;
    }
    catch (IOException ioe)
    {
      log.error(errorString, ioe);
    }
    finally
    {
      if (is != null) {
        try
        {
          is.close();
        }
        catch (IOException e) {}
      }
    }
    return result;
  }
  
  private static String normalize(String jndiName)
  {
    if ((jndiName != null) && (jndiName.startsWith("java:comp/env/"))) {
      return jndiName.substring(14);
    }
    return jndiName;
  }
  
  private static Method getMethod(Class<?> clazz, final AnnotationCacheEntry entry)
  {
    Method result = null;
    if (Globals.IS_SECURITY_ENABLED) {
      result = (Method)AccessController.doPrivileged(new PrivilegedAction()
      {
        public Method run()
        {
          Method result = null;
          try
          {
            result = clazz.getDeclaredMethod(entry.getAccessibleObjectName(), entry.getParamTypes());
          }
          catch (NoSuchMethodException e) {}
          return result;
        }
      });
    } else {
      try
      {
        result = clazz.getDeclaredMethod(entry.getAccessibleObjectName(), entry.getParamTypes());
      }
      catch (NoSuchMethodException e) {}
    }
    return result;
  }
  
  private static Field getField(Class<?> clazz, final AnnotationCacheEntry entry)
  {
    Field result = null;
    if (Globals.IS_SECURITY_ENABLED) {
      result = (Field)AccessController.doPrivileged(new PrivilegedAction()
      {
        public Field run()
        {
          Field result = null;
          try
          {
            result = clazz.getDeclaredField(entry.getAccessibleObjectName());
          }
          catch (NoSuchFieldException e) {}
          return result;
        }
      });
    } else {
      try
      {
        result = clazz.getDeclaredField(entry.getAccessibleObjectName());
      }
      catch (NoSuchFieldException e) {}
    }
    return result;
  }
  
  private static Method findPostConstruct(Method currentPostConstruct, String postConstructFromXml, Method method)
  {
    return findLifecycleCallback(currentPostConstruct, postConstructFromXml, method, PostConstruct.class);
  }
  
  private static Method findPreDestroy(Method currentPreDestroy, String preDestroyFromXml, Method method)
  {
    return findLifecycleCallback(currentPreDestroy, preDestroyFromXml, method, PreDestroy.class);
  }
  
  private static Method findLifecycleCallback(Method currentMethod, String methodNameFromXml, Method method, Class<? extends Annotation> annotation)
  {
    Method result = currentMethod;
    if (methodNameFromXml != null)
    {
      if (method.getName().equals(methodNameFromXml))
      {
        if (!Introspection.isValidLifecycleCallback(method)) {
          throw new IllegalArgumentException("Invalid " + annotation.getName() + " annotation");
        }
        result = method;
      }
    }
    else if (method.isAnnotationPresent(annotation))
    {
      if ((currentMethod != null) || (!Introspection.isValidLifecycleCallback(method))) {
        throw new IllegalArgumentException("Invalid " + annotation.getName() + " annotation");
      }
      result = method;
    }
    return result;
  }
  
  private static final class AnnotationCacheEntry
  {
    private final String accessibleObjectName;
    private final Class<?>[] paramTypes;
    private final String name;
    private final DefaultInstanceManager.AnnotationCacheEntryType type;
    
    public AnnotationCacheEntry(String accessibleObjectName, Class<?>[] paramTypes, String name, DefaultInstanceManager.AnnotationCacheEntryType type)
    {
      this.accessibleObjectName = accessibleObjectName;
      this.paramTypes = paramTypes;
      this.name = name;
      this.type = type;
    }
    
    public String getAccessibleObjectName()
    {
      return this.accessibleObjectName;
    }
    
    public Class<?>[] getParamTypes()
    {
      return this.paramTypes;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public DefaultInstanceManager.AnnotationCacheEntryType getType()
    {
      return this.type;
    }
  }
  
  private static enum AnnotationCacheEntryType
  {
    FIELD,  SETTER,  POST_CONSTRUCT,  PRE_DESTROY;
    
    private AnnotationCacheEntryType() {}
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\DefaultInstanceManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */