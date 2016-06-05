package org.apache.catalina.loader;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class VirtualWebappLoader
  extends WebappLoader
{
  private static final Log log = LogFactory.getLog(VirtualWebappLoader.class);
  private String virtualClasspath = "";
  
  public VirtualWebappLoader() {}
  
  public VirtualWebappLoader(ClassLoader parent)
  {
    super(parent);
  }
  
  public void setVirtualClasspath(String path)
  {
    this.virtualClasspath = path;
  }
  
  public boolean getSearchVirtualFirst()
  {
    return getSearchExternalFirst();
  }
  
  public void setSearchVirtualFirst(boolean searchVirtualFirst)
  {
    setSearchExternalFirst(searchVirtualFirst);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    StringTokenizer tkn = new StringTokenizer(this.virtualClasspath, ";");
    Set<String> set = new LinkedHashSet();
    while (tkn.hasMoreTokens())
    {
      String token = tkn.nextToken().trim();
      if (!token.isEmpty())
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("virtualWebappLoader.token", new Object[] { token }));
        }
        if (token.endsWith("*.jar"))
        {
          token = token.substring(0, token.length() - "*.jar".length());
          
          File directory = new File(token);
          if (!directory.isDirectory())
          {
            if (log.isDebugEnabled()) {
              log.debug(sm.getString("virtualWebappLoader.token.notDirectory", new Object[] { directory.getAbsolutePath() }));
            }
          }
          else
          {
            if (log.isDebugEnabled()) {
              log.debug(sm.getString("virtualWebappLoader.token.glob.dir", new Object[] { directory.getAbsolutePath() }));
            }
            String[] filenames = directory.list();
            Arrays.sort(filenames);
            for (int j = 0; j < filenames.length; j++)
            {
              String filename = filenames[j].toLowerCase(Locale.ENGLISH);
              if (filename.endsWith(".jar"))
              {
                File file = new File(directory, filenames[j]);
                if (!file.isFile())
                {
                  if (log.isDebugEnabled()) {
                    log.debug(sm.getString("virtualWebappLoader.token.notFile", new Object[] { file.getAbsolutePath() }));
                  }
                }
                else
                {
                  if (log.isDebugEnabled()) {
                    log.debug(sm.getString("virtualWebappLoader.token.file", new Object[] { file.getAbsolutePath() }));
                  }
                  set.add(file.toURI().toString());
                }
              }
            }
          }
        }
        else
        {
          File file = new File(token);
          if (!file.exists())
          {
            if (log.isDebugEnabled()) {
              log.debug(sm.getString("virtualWebappLoader.token.notExists", new Object[] { file.getAbsolutePath() }));
            }
          }
          else
          {
            if (log.isDebugEnabled()) {
              log.debug(sm.getString("virtualWebappLoader.token.file", new Object[] { file.getAbsolutePath() }));
            }
            set.add(file.toURI().toString());
          }
        }
      }
    }
    for (String repository : set) {
      addRepository(repository);
    }
    super.startInternal();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\VirtualWebappLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */