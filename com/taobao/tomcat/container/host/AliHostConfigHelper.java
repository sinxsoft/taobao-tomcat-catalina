package com.taobao.tomcat.container.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.catalina.util.ContextName;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class AliHostConfigHelper
{
  private static final Log log = LogFactory.getLog(AliHostConfigHelper.class);
  
  public AliHostConfigHelper() {}
  
  public static boolean isPandoraSarFile(String sarFile)
  {
    return sarFile.toLowerCase().endsWith(".sar");
  }
  
  public static ContextName parseJbossWebXmlForWar(File war)
  {
    JarFile warFile = null;
    JarEntry jbossWebXmlEntry = null;
    InputStream istream = null;
    ContextName cn = null;
    try
    {
      warFile = new JarFile(war);
      jbossWebXmlEntry = warFile.getJarEntry("WEB-INF/jboss-web.xml");
      if (jbossWebXmlEntry != null)
      {
        istream = warFile.getInputStream(jbossWebXmlEntry);
        String contextPath = JbossCompat.parseJbossWebXml(istream);
        if (contextPath != null)
        {
          log.info("Get contextPath: '" + contextPath + "' from WEB-INF/jboss-web.xml");
          
          return new ContextName(contextPath, "");
        }
      }
      return new ContextName(war.getName(), true);
    }
    catch (IOException e) {}finally
    {
      if (warFile != null) {
        try
        {
          warFile.close();
        }
        catch (IOException ioe) {}
      }
    }
    return cn;
  }
  
  public static ContextName parseJbossWebXml(File appDir)
  {
    String dirName = appDir.getName();
    ContextName cn = null;
    
    String contextPath = JbossCompat.parseJbossWebXml(appDir);
    if (contextPath != null)
    {
      log.info("Get contextPath: '" + contextPath + "' from WEB-INF/jboss-web.xml");
      
      cn = new ContextName(contextPath, "");
    }
    else
    {
      cn = new ContextName(dirName, true);
    }
    return cn;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\host\AliHostConfigHelper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */