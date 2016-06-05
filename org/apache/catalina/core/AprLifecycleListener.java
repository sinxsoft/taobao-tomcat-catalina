package org.apache.catalina.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class AprLifecycleListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(AprLifecycleListener.class);
  private static boolean instanceCreated = false;
  private static final List<String> initInfoLogMessages = new ArrayList(3);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  protected static final int TCN_REQUIRED_MAJOR = 1;
  protected static final int TCN_REQUIRED_MINOR = 1;
  protected static final int TCN_REQUIRED_PATCH = 32;
  protected static final int TCN_RECOMMENDED_MINOR = 1;
  protected static final int TCN_RECOMMENDED_PV = 32;
  protected static String SSLEngine = "on";
  protected static String FIPSMode = "off";
  protected static String SSLRandomSeed = "builtin";
  protected static boolean sslInitialized = false;
  protected static boolean aprInitialized = false;
  @Deprecated
  protected static boolean sslAvailable = false;
  protected static boolean aprAvailable = false;
  protected static boolean fipsModeActive = false;
  private static final int FIPS_ON = 1;
  private static final int FIPS_OFF = 0;
  protected static final Object lock = new Object();
  
  public static boolean isAprAvailable()
  {
    if (instanceCreated) {
      synchronized (lock)
      {
        init();
      }
    }
    return aprAvailable;
  }
  
  public AprLifecycleListener()
  {
    instanceCreated = true;
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if ("before_init".equals(event.getType())) {
      synchronized (lock)
      {
        init();
        for (String msg : initInfoLogMessages) {
          log.info(msg);
        }
        initInfoLogMessages.clear();
        if (aprAvailable) {
          try
          {
            initializeSSL();
          }
          catch (Throwable t)
          {
            t = ExceptionUtils.unwrapInvocationTargetException(t);
            ExceptionUtils.handleThrowable(t);
            log.error(sm.getString("aprListener.sslInit"), t);
          }
        }
        if ((null != FIPSMode) && (!"off".equalsIgnoreCase(FIPSMode)) && (!isFIPSModeActive()))
        {
          Error e = new Error(sm.getString("aprListener.initializeFIPSFailed"));
          
          log.fatal(e.getMessage(), e);
          throw e;
        }
      }
    } else if ("after_destroy".equals(event.getType())) {
      synchronized (lock)
      {
        if (!aprAvailable) {
          return;
        }
        try
        {
          terminateAPR();
        }
        catch (Throwable t)
        {
          t = ExceptionUtils.unwrapInvocationTargetException(t);
          ExceptionUtils.handleThrowable(t);
          log.info(sm.getString("aprListener.aprDestroy"));
        }
      }
    }
  }
  
  private static void terminateAPR()
    throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
  {
    String methodName = "terminate";
    Method method = Class.forName("org.apache.tomcat.jni.Library").getMethod(methodName, (Class[])null);
    
    method.invoke(null, (Object[])null);
    aprAvailable = false;
    aprInitialized = false;
    sslInitialized = false;
    sslAvailable = false;
    fipsModeActive = false;
  }
  
  private static void init()
  {
    int major = 0;
    int minor = 0;
    int patch = 0;
    int apver = 0;
    int rqver = 1132;
    int rcver = 1132;
    if (aprInitialized) {
      return;
    }
    aprInitialized = true;
    try
    {
      String methodName = "initialize";
      Class<?>[] paramTypes = new Class[1];
      paramTypes[0] = String.class;
      Object[] paramValues = new Object[1];
      paramValues[0] = null;
      Class<?> clazz = Class.forName("org.apache.tomcat.jni.Library");
      Method method = clazz.getMethod(methodName, paramTypes);
      method.invoke(null, paramValues);
      major = clazz.getField("TCN_MAJOR_VERSION").getInt(null);
      minor = clazz.getField("TCN_MINOR_VERSION").getInt(null);
      patch = clazz.getField("TCN_PATCH_VERSION").getInt(null);
      apver = major * 1000 + minor * 100 + patch;
    }
    catch (Throwable t)
    {
      t = ExceptionUtils.unwrapInvocationTargetException(t);
      ExceptionUtils.handleThrowable(t);
      initInfoLogMessages.add(sm.getString("aprListener.aprInit", new Object[] { System.getProperty("java.library.path") }));
      
      return;
    }
    if (apver < rqver)
    {
      log.error(sm.getString("aprListener.tcnInvalid", new Object[] { major + "." + minor + "." + patch, "1.1.32" }));
      try
      {
        terminateAPR();
      }
      catch (Throwable t)
      {
        t = ExceptionUtils.unwrapInvocationTargetException(t);
        ExceptionUtils.handleThrowable(t);
      }
      return;
    }
    if (apver < rcver) {
      initInfoLogMessages.add(sm.getString("aprListener.tcnVersion", new Object[] { major + "." + minor + "." + patch, "1.1.32" }));
    }
    initInfoLogMessages.add(sm.getString("aprListener.tcnValid", new Object[] { major + "." + minor + "." + patch, Library.APR_MAJOR_VERSION + "." + Library.APR_MINOR_VERSION + "." + Library.APR_PATCH_VERSION }));
    
    initInfoLogMessages.add(sm.getString("aprListener.flags", new Object[] { Boolean.valueOf(Library.APR_HAVE_IPV6), Boolean.valueOf(Library.APR_HAS_SENDFILE), Boolean.valueOf(Library.APR_HAS_SO_ACCEPTFILTER), Boolean.valueOf(Library.APR_HAS_RANDOM) }));
    
    aprAvailable = true;
  }
  
  private static void initializeSSL()
    throws Exception
  {
    if ("off".equalsIgnoreCase(SSLEngine)) {
      return;
    }
    if (sslInitialized) {
      return;
    }
    sslInitialized = true;
    
    String methodName = "randSet";
    Class<?>[] paramTypes = new Class[1];
    paramTypes[0] = String.class;
    Object[] paramValues = new Object[1];
    paramValues[0] = SSLRandomSeed;
    Class<?> clazz = Class.forName("org.apache.tomcat.jni.SSL");
    Method method = clazz.getMethod(methodName, paramTypes);
    method.invoke(null, paramValues);
    
    methodName = "initialize";
    paramValues[0] = ("on".equalsIgnoreCase(SSLEngine) ? null : SSLEngine);
    method = clazz.getMethod(methodName, paramTypes);
    method.invoke(null, paramValues);
    if ((null != FIPSMode) && (!"off".equalsIgnoreCase(FIPSMode)))
    {
      fipsModeActive = false;
      
      int fipsModeState = SSL.fipsModeGet();
      if (log.isDebugEnabled()) {
        log.debug(sm.getString("aprListener.currentFIPSMode", new Object[] { Integer.valueOf(fipsModeState) }));
      }
      boolean enterFipsMode;
      if ("on".equalsIgnoreCase(FIPSMode))
      {
        
        if (fipsModeState == 1)
        {
          log.info(sm.getString("aprListener.skipFIPSInitialization"));
          fipsModeActive = true;
          enterFipsMode = false;
        }
        else
        {
          enterFipsMode = true;
        }
      }
      else if ("require".equalsIgnoreCase(FIPSMode))
      {
        
        if (fipsModeState == 1)
        {
          fipsModeActive = true;
          enterFipsMode = false;
        }
        else
        {
          throw new IllegalStateException(sm.getString("aprListener.requireNotInFIPSMode"));
        }
      }
      else if ("enter".equalsIgnoreCase(FIPSMode))
      {
        
        if (fipsModeState == 0) {
          enterFipsMode = true;
        } else {
          throw new IllegalStateException(sm.getString("aprListener.enterAlreadyInFIPSMode", new Object[] { Integer.valueOf(fipsModeState) }));
        }
      }
      else
      {
        throw new IllegalArgumentException(sm.getString("aprListener.wrongFIPSMode", new Object[] { FIPSMode }));
      }
      
      if (enterFipsMode)
      {
        log.info(sm.getString("aprListener.initializingFIPS"));
        
        fipsModeState = SSL.fipsModeSet(1);
        if (fipsModeState != 1)
        {
          String message = sm.getString("aprListener.initializeFIPSFailed");
          log.error(message);
          throw new IllegalStateException(message);
        }
        fipsModeActive = true;
        log.info(sm.getString("aprListener.initializeFIPSSuccess"));
      }
    }
    log.info(sm.getString("aprListener.initializedOpenSSL", new Object[] { SSL.versionString() }));
    
    sslAvailable = true;
  }
  
  public String getSSLEngine()
  {
    return SSLEngine;
  }
  
  public void setSSLEngine(String SSLEngine)
  {
    if (!SSLEngine.equals(SSLEngine))
    {
      if (sslInitialized) {
        throw new IllegalStateException(sm.getString("aprListener.tooLateForSSLEngine"));
      }
      SSLEngine = SSLEngine;
    }
  }
  
  public String getSSLRandomSeed()
  {
    return SSLRandomSeed;
  }
  
  public void setSSLRandomSeed(String SSLRandomSeed)
  {
    if (!SSLRandomSeed.equals(SSLRandomSeed))
    {
      if (sslInitialized) {
        throw new IllegalStateException(sm.getString("aprListener.tooLateForSSLRandomSeed"));
      }
      SSLRandomSeed = SSLRandomSeed;
    }
  }
  
  public String getFIPSMode()
  {
    return FIPSMode;
  }
  
  public void setFIPSMode(String FIPSMode)
  {
    if (!FIPSMode.equals(FIPSMode))
    {
      if (sslInitialized) {
        throw new IllegalStateException(sm.getString("aprListener.tooLateForFIPSMode"));
      }
      FIPSMode = FIPSMode;
    }
  }
  
  public boolean isFIPSModeActive()
  {
    return fipsModeActive;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\AprLifecycleListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */