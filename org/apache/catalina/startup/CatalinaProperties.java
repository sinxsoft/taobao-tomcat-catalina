package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class CatalinaProperties
{
  private static final Log log = LogFactory.getLog(CatalinaProperties.class);
  private static Properties properties = null;
  
  static
  {
    loadProperties();
  }
  
  public static String getProperty(String name)
  {
    return properties.getProperty(name);
  }
  
  @Deprecated
  public static String getProperty(String name, String defaultValue)
  {
    return properties.getProperty(name, defaultValue);
  }
  
  private static void loadProperties()
  {
      InputStream is = null;
      Throwable error = null;

      try {
          String configUrl = getConfigUrl();
          if (configUrl != null) {
              is = (new URL(configUrl)).openStream();
          }
      } catch (Throwable t) {
          handleThrowable(t);
      }

      if (is == null) {
          try {
              File home = new File(getCatalinaBase());
              File conf = new File(home, "conf");
              File propsFile = new File(conf, "catalina.properties");
              is = new FileInputStream(propsFile);
          } catch (Throwable t) {
              handleThrowable(t);
          }
      }

      if (is == null) {
          try {
              is = CatalinaProperties.class.getResourceAsStream
                  ("/org/apache/catalina/startup/catalina.properties");
          } catch (Throwable t) {
              handleThrowable(t);
          }
      }

      if (is != null) {
          try {
              properties = new Properties();
              properties.load(is);
          } catch (Throwable t) {
              handleThrowable(t);
              error = t;
          }
          finally
          {
              try {
                  is.close();
              } catch (IOException ioe) {
                  log.warn("Could not close catalina.properties", ioe);
              }
          }
      }

      if ((is == null) || (error != null)) {
          // Do something
          log.warn("Failed to load catalina.properties", error);
          // That's fine - we have reasonable defaults.
          properties=new Properties();
      }

      // Register the properties as system properties
      Enumeration<?> enumeration = properties.propertyNames();
      while (enumeration.hasMoreElements()) {
          String name = (String) enumeration.nextElement();
          String value = properties.getProperty(name);
          if (value != null) {
              System.setProperty(name, value);
          }
      }
  }
  
  private static String getCatalinaHome()
  {
    return System.getProperty("catalina.home", System.getProperty("user.dir"));
  }
  
  private static String getCatalinaBase()
  {
    return System.getProperty("catalina.base", getCatalinaHome());
  }
  
  private static String getConfigUrl()
  {
    return System.getProperty("catalina.config");
  }
  
  private static void handleThrowable(Throwable t)
  {
    if ((t instanceof ThreadDeath)) {
      throw ((ThreadDeath)t);
    }
    if ((t instanceof VirtualMachineError)) {
      throw ((VirtualMachineError)t);
    }
  }
  
  public CatalinaProperties() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\CatalinaProperties.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */