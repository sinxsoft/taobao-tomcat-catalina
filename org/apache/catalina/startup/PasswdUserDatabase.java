package org.apache.catalina.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public final class PasswdUserDatabase
  implements UserDatabase
{
  private static final String PASSWORD_FILE = "/etc/passwd";
  private Hashtable<String, String> homes = new Hashtable();
  private UserConfig userConfig = null;
  
  public PasswdUserDatabase() {}
  
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
    BufferedReader reader = null;
    try
    {
      reader = new BufferedReader(new FileReader("/etc/passwd"));
      for (;;)
      {
        StringBuilder buffer = new StringBuilder();
        for (;;)
        {
          int ch = reader.read();
          if ((ch < 0) || (ch == 10)) {
            break;
          }
          buffer.append((char)ch);
        }
        String line = buffer.toString();
        if (line.length() < 1) {
          break;
        }
        int n = 0;
        String[] tokens = new String[7];
        for (int i = 0; i < tokens.length; i++) {
          tokens[i] = null;
        }
        while (n < tokens.length)
        {
          String token = null;
          int colon = line.indexOf(':');
          if (colon >= 0)
          {
            token = line.substring(0, colon);
            line = line.substring(colon + 1);
          }
          else
          {
            token = line;
            line = "";
          }
          tokens[(n++)] = token;
        }
        if ((tokens[0] != null) && (tokens[5] != null)) {
          this.homes.put(tokens[0], tokens[5]);
        }
      }
      reader.close();
      reader = null;
    }
    catch (Exception e)
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }
        catch (IOException f) {}
        reader = null;
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\PasswdUserDatabase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */