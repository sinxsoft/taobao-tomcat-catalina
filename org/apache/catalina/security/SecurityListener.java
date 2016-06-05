package org.apache.catalina.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

public class SecurityListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(SecurityListener.class);
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.security");
  private static final String UMASK_PROPERTY_NAME = "org.apache.catalina.security.SecurityListener.UMASK";
  private static final String UMASK_FORMAT = "%04o";
  private Set<String> checkedOsUsers = new HashSet();
  private Integer minimumUmask = Integer.valueOf(7);
  
  public SecurityListener()
  {
    this.checkedOsUsers.add("root");
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if (event.getType().equals("before_init")) {
      doChecks();
    }
  }
  
  public void setCheckedOsUsers(String userNameList)
  {
    if ((userNameList == null) || (userNameList.length() == 0))
    {
      this.checkedOsUsers.clear();
    }
    else
    {
      String[] userNames = userNameList.split(",");
      for (String userName : userNames) {
        if (userName.length() > 0) {
          this.checkedOsUsers.add(userName);
        }
      }
    }
  }
  
  public String getCheckedOsUsers()
  {
    if (this.checkedOsUsers.size() == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    Iterator<String> iter = this.checkedOsUsers.iterator();
    result.append((String)iter.next());
    while (iter.hasNext())
    {
      result.append(',');
      result.append((String)iter.next());
    }
    return result.toString();
  }
  
  public void setMinimumUmask(String umask)
  {
    if ((umask == null) || (umask.length() == 0)) {
      this.minimumUmask = Integer.valueOf(0);
    } else {
      this.minimumUmask = Integer.valueOf(umask, 8);
    }
  }
  
  public String getMinimumUmask()
  {
    return String.format("%04o", new Object[] { this.minimumUmask });
  }
  
  protected void doChecks()
  {
    checkOsUser();
    checkUmask();
  }
  
  protected void checkOsUser()
  {
    String userName = System.getProperty("user.name");
    if (userName != null)
    {
      String userNameLC = userName.toLowerCase();
      if (this.checkedOsUsers.contains(userNameLC)) {
        throw new Error(sm.getString("SecurityListener.checkUserWarning", new Object[] { userName }));
      }
    }
  }
  
  protected void checkUmask()
  {
    String prop = System.getProperty("org.apache.catalina.security.SecurityListener.UMASK");
    Integer umask = null;
    if (prop != null) {
      try
      {
        umask = Integer.valueOf(prop, 8);
      }
      catch (NumberFormatException nfe)
      {
        log.warn(sm.getString("SecurityListener.checkUmaskParseFail", new Object[] { prop }));
      }
    }
    if (umask == null)
    {
      if ("\r\n".equals(Constants.LINE_SEP))
      {
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("SecurityListener.checkUmaskSkip"));
        }
        return;
      }
      if (this.minimumUmask.intValue() > 0) {
        log.warn(sm.getString("SecurityListener.checkUmaskNone", new Object[] { "org.apache.catalina.security.SecurityListener.UMASK", getMinimumUmask() }));
      }
      return;
    }
    if ((umask.intValue() & this.minimumUmask.intValue()) != this.minimumUmask.intValue()) {
      throw new Error(sm.getString("SecurityListener.checkUmaskFail", new Object[] { String.format("%04o", new Object[] { umask }), getMinimumUmask() }));
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\security\SecurityListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */