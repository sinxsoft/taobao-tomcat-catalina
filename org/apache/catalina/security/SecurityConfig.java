package org.apache.catalina.security;

import java.security.Security;
import org.apache.catalina.startup.CatalinaProperties;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public final class SecurityConfig
{
  private static SecurityConfig singleton = null;
  private static final Log log = LogFactory.getLog(SecurityConfig.class);
  private static final String PACKAGE_ACCESS = "sun.,org.apache.catalina.,org.apache.jasper.,org.apache.coyote.,org.apache.tomcat.";
  private static final String PACKAGE_DEFINITION = "java.,sun.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper.";
  private String packageDefinition;
  private String packageAccess;
  
  private SecurityConfig()
  {
    try
    {
      this.packageDefinition = CatalinaProperties.getProperty("package.definition");
      this.packageAccess = CatalinaProperties.getProperty("package.access");
    }
    catch (Exception ex)
    {
      if (log.isDebugEnabled()) {
        log.debug("Unable to load properties using CatalinaProperties", ex);
      }
    }
  }
  
  public static SecurityConfig newInstance()
  {
    if (singleton == null) {
      singleton = new SecurityConfig();
    }
    return singleton;
  }
  
  public void setPackageAccess()
  {
    if (this.packageAccess == null) {
      setSecurityProperty("package.access", "sun.,org.apache.catalina.,org.apache.jasper.,org.apache.coyote.,org.apache.tomcat.");
    } else {
      setSecurityProperty("package.access", this.packageAccess);
    }
  }
  
  public void setPackageDefinition()
  {
    if (this.packageDefinition == null) {
      setSecurityProperty("package.definition", "java.,sun.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper.");
    } else {
      setSecurityProperty("package.definition", this.packageDefinition);
    }
  }
  
  private final void setSecurityProperty(String properties, String packageList)
  {
    if (System.getSecurityManager() != null)
    {
      String definition = Security.getProperty(properties);
      if ((definition != null) && (definition.length() > 0))
      {
        if (packageList.length() > 0) {
          definition = definition + ',' + packageList;
        }
      }
      else {
        definition = packageList;
      }
      Security.setProperty(properties, definition);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\security\SecurityConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */