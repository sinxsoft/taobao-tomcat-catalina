package org.apache.catalina.startup;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

public final class HomesUserDatabase
  implements UserDatabase
{
  private Hashtable<String, String> homes = new Hashtable();
  private UserConfig userConfig = null;
  
  public HomesUserDatabase() {}
  
  public UserConfig getUserConfig()
  {
    return this.userConfig;
  }
  
  public void setUserConfig(UserConfig userConfig)
  {
    this.userConfig = userConfig;
    init();
  }
  
  public String getHome(String user)
  {
    return (String)this.homes.get(user);
  }
  
  public Enumeration<String> getUsers()
  {
    return this.homes.keys();
  }
  
  private void init()
  {
    String homeBase = this.userConfig.getHomeBase();
    File homeBaseDir = new File(homeBase);
    if ((!homeBaseDir.exists()) || (!homeBaseDir.isDirectory())) {
      return;
    }
    String[] homeBaseFiles = homeBaseDir.list();
    for (int i = 0; i < homeBaseFiles.length; i++)
    {
      File homeDir = new File(homeBaseDir, homeBaseFiles[i]);
      if ((homeDir.isDirectory()) && (homeDir.canRead())) {
        this.homes.put(homeBaseFiles[i], homeDir.toString());
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\HomesUserDatabase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */