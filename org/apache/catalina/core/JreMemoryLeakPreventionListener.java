package org.apache.catalina.core;

import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Security;
import java.sql.DriverManager;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class JreMemoryLeakPreventionListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(JreMemoryLeakPreventionListener.class);
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private static final boolean IS_JAVA_7_OR_LATER;
  
  static
  {
    boolean isJava7OrLater;
    try
    {
      Class.forName("java.util.Objects");
      isJava7OrLater = true;
    }
    catch (ClassNotFoundException e)
    {
      isJava7OrLater = false;
    }
    IS_JAVA_7_OR_LATER = isJava7OrLater;
  }
  
  private boolean appContextProtection = !IS_JAVA_7_OR_LATER;
  
  public boolean isAppContextProtection()
  {
    return this.appContextProtection;
  }
  
  public void setAppContextProtection(boolean appContextProtection)
  {
    this.appContextProtection = appContextProtection;
  }
  
  private boolean awtThreadProtection = false;
  
  public boolean isAWTThreadProtection()
  {
    return this.awtThreadProtection;
  }
  
  public void setAWTThreadProtection(boolean awtThreadProtection)
  {
    this.awtThreadProtection = awtThreadProtection;
  }
  
  private boolean java2dDisposerProtection = false;
  
  public boolean isJava2DDisposerProtection()
  {
    return this.java2dDisposerProtection;
  }
  
  public void setJava2DDisposerProtection(boolean java2dDisposerProtection)
  {
    this.java2dDisposerProtection = java2dDisposerProtection;
  }
  
  private boolean gcDaemonProtection = true;
  
  public boolean isGcDaemonProtection()
  {
    return this.gcDaemonProtection;
  }
  
  public void setGcDaemonProtection(boolean gcDaemonProtection)
  {
    this.gcDaemonProtection = gcDaemonProtection;
  }
  
  private boolean securityPolicyProtection = true;
  
  public boolean isSecurityPolicyProtection()
  {
    return this.securityPolicyProtection;
  }
  
  public void setSecurityPolicyProtection(boolean securityPolicyProtection)
  {
    this.securityPolicyProtection = securityPolicyProtection;
  }
  
  private boolean securityLoginConfigurationProtection = true;
  
  public boolean isSecurityLoginConfigurationProtection()
  {
    return this.securityLoginConfigurationProtection;
  }
  
  public void setSecurityLoginConfigurationProtection(boolean securityLoginConfigurationProtection)
  {
    this.securityLoginConfigurationProtection = securityLoginConfigurationProtection;
  }
  
  private boolean tokenPollerProtection = true;
  
  public boolean isTokenPollerProtection()
  {
    return this.tokenPollerProtection;
  }
  
  public void setTokenPollerProtection(boolean tokenPollerProtection)
  {
    this.tokenPollerProtection = tokenPollerProtection;
  }
  
  private boolean urlCacheProtection = true;
  
  public boolean isUrlCacheProtection()
  {
    return this.urlCacheProtection;
  }
  
  public void setUrlCacheProtection(boolean urlCacheProtection)
  {
    this.urlCacheProtection = urlCacheProtection;
  }
  
  private boolean xmlParsingProtection = true;
  
  public boolean isXmlParsingProtection()
  {
    return this.xmlParsingProtection;
  }
  
  public void setXmlParsingProtection(boolean xmlParsingProtection)
  {
    this.xmlParsingProtection = xmlParsingProtection;
  }
  
  private boolean ldapPoolProtection = true;
  
  public boolean isLdapPoolProtection()
  {
    return this.ldapPoolProtection;
  }
  
  public void setLdapPoolProtection(boolean ldapPoolProtection)
  {
    this.ldapPoolProtection = ldapPoolProtection;
  }
  
  private boolean driverManagerProtection = true;
  
  public boolean isDriverManagerProtection()
  {
    return this.driverManagerProtection;
  }
  
  public void setDriverManagerProtection(boolean driverManagerProtection)
  {
    this.driverManagerProtection = driverManagerProtection;
  }
  
  private String classesToInitialize = null;
  
  public String getClassesToInitialize()
  {
    return this.classesToInitialize;
  }
  
  public void setClassesToInitialize(String classesToInitialize)
  {
    this.classesToInitialize = classesToInitialize;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if ("before_init".equals(event.getType()))
    {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      try
      {
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        if (this.driverManagerProtection) {
          DriverManager.getDrivers();
        }
        if (this.appContextProtection) {
          ImageIO.getCacheDirectory();
        }
        if (this.awtThreadProtection) {
          Toolkit.getDefaultToolkit();
        }
        if (this.java2dDisposerProtection) {
          try
          {
            Class.forName("sun.java2d.Disposer");
          }
          catch (ClassNotFoundException cnfe) {}
        }
        if (this.gcDaemonProtection) {
          try
          {
            Class<?> clazz = Class.forName("sun.misc.GC");
            Method method = clazz.getDeclaredMethod("requestLatency", new Class[] { Long.TYPE });
            
            method.invoke(null, new Object[] { Long.valueOf(9223372036854775806L) });
          }
          catch (ClassNotFoundException e)
          {
            if (Globals.IS_ORACLE_JVM) {
              log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
            } else {
              log.debug(sm.getString("jreLeakListener.gcDaemonFail"), e);
            }
          }
          catch (SecurityException e)
          {
            log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
          }
          catch (NoSuchMethodException e)
          {
            log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
          }
          catch (IllegalArgumentException e)
          {
            log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
          }
          catch (IllegalAccessException e)
          {
            log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
          }
          catch (InvocationTargetException e)
          {
            ExceptionUtils.handleThrowable(e.getCause());
            log.error(sm.getString("jreLeakListener.gcDaemonFail"), e);
          }
        }
        if (this.securityPolicyProtection) {
          try
          {
            Class<?> policyClass = Class.forName("javax.security.auth.Policy");
            
            Method method = policyClass.getMethod("getPolicy", new Class[0]);
            method.invoke(null, new Object[0]);
          }
          catch (ClassNotFoundException e) {}catch (SecurityException e) {}catch (NoSuchMethodException e)
          {
            log.warn(sm.getString("jreLeakListener.authPolicyFail"), e);
          }
          catch (IllegalArgumentException e)
          {
            log.warn(sm.getString("jreLeakListener.authPolicyFail"), e);
          }
          catch (IllegalAccessException e)
          {
            log.warn(sm.getString("jreLeakListener.authPolicyFail"), e);
          }
          catch (InvocationTargetException e)
          {
            ExceptionUtils.handleThrowable(e.getCause());
            log.warn(sm.getString("jreLeakListener.authPolicyFail"), e);
          }
        }
        if (this.securityLoginConfigurationProtection) {
          try
          {
            Class.forName("javax.security.auth.login.Configuration", true, ClassLoader.getSystemClassLoader());
          }
          catch (ClassNotFoundException e) {}
        }
        if (this.tokenPollerProtection) {
          Security.getProviders();
        }
        if (this.urlCacheProtection) {
          try
          {
            URL url = new URL("jar:file://dummy.jar!/");
            URLConnection uConn = url.openConnection();
            uConn.setDefaultUseCaches(false);
          }
          catch (MalformedURLException e)
          {
            log.error(sm.getString("jreLeakListener.jarUrlConnCacheFail"), e);
          }
          catch (IOException e)
          {
            log.error(sm.getString("jreLeakListener.jarUrlConnCacheFail"), e);
          }
        }
        if (this.xmlParsingProtection)
        {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          try
          {
            factory.newDocumentBuilder();
          }
          catch (ParserConfigurationException e)
          {
            log.error(sm.getString("jreLeakListener.xmlParseFail"), e);
          }
        }
        if (this.ldapPoolProtection) {
          try
          {
            Class.forName("com.sun.jndi.ldap.LdapPoolManager");
          }
          catch (ClassNotFoundException e)
          {
            if (Globals.IS_ORACLE_JVM) {
              log.error(sm.getString("jreLeakListener.ldapPoolManagerFail"), e);
            } else {
              log.debug(sm.getString("jreLeakListener.ldapPoolManagerFail"), e);
            }
          }
        }
        if (this.classesToInitialize != null)
        {
          StringTokenizer strTok = new StringTokenizer(this.classesToInitialize, ", \r\n\t");
          while (strTok.hasMoreTokens())
          {
            String classNameToLoad = strTok.nextToken();
            try
            {
              Class.forName(classNameToLoad);
            }
            catch (ClassNotFoundException e)
            {
              log.error(sm.getString("jreLeakListener.classToInitializeFail", new Object[] { classNameToLoad }), e);
            }
          }
        }
      }
      finally
      {
        Thread.currentThread().setContextClassLoader(loader);
      }
    }
  }
  
  public JreMemoryLeakPreventionListener() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\JreMemoryLeakPreventionListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */