package org.apache.naming;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class StringManager
{
  private ResourceBundle bundle;
  private Locale locale;
  
  private StringManager(String packageName)
  {
    String bundleName = packageName + ".LocalStrings";
    try
    {
      this.bundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());
    }
    catch (MissingResourceException ex)
    {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl != null) {
        try
        {
          this.bundle = ResourceBundle.getBundle(bundleName, Locale.getDefault(), cl);
        }
        catch (MissingResourceException ex2) {}
      }
    }
    if (this.bundle != null) {
      this.locale = this.bundle.getLocale();
    }
  }
  
  public String getString(String key)
  {
    if (key == null)
    {
      String msg = "key may not have a null value";
      
      throw new IllegalArgumentException(msg);
    }
    String str = null;
    try
    {
      str = this.bundle.getString(key);
    }
    catch (MissingResourceException mre)
    {
      str = null;
    }
    return str;
  }
  
  public String getString(String key, Object... args)
  {
    String value = getString(key);
    if (value == null) {
      value = key;
    }
    MessageFormat mf = new MessageFormat(value);
    mf.setLocale(this.locale);
    return mf.format(args, new StringBuffer(), null).toString();
  }
  
  private static Hashtable<String, StringManager> managers = new Hashtable();
  
  public static final synchronized StringManager getManager(String packageName)
  {
    StringManager mgr = (StringManager)managers.get(packageName);
    if (mgr == null)
    {
      mgr = new StringManager(packageName);
      managers.put(packageName, mgr);
    }
    return mgr;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\StringManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */