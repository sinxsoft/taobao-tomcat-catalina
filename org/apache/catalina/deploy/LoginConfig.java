package org.apache.catalina.deploy;

import java.io.Serializable;
import org.apache.catalina.util.RequestUtil;

public class LoginConfig
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  public LoginConfig() {}
  
  public LoginConfig(String authMethod, String realmName, String loginPage, String errorPage)
  {
    setAuthMethod(authMethod);
    setRealmName(realmName);
    setLoginPage(loginPage);
    setErrorPage(errorPage);
  }
  
  private String authMethod = null;
  
  public String getAuthMethod()
  {
    return this.authMethod;
  }
  
  public void setAuthMethod(String authMethod)
  {
    this.authMethod = authMethod;
  }
  
  private String errorPage = null;
  
  public String getErrorPage()
  {
    return this.errorPage;
  }
  
  public void setErrorPage(String errorPage)
  {
    this.errorPage = RequestUtil.URLDecode(errorPage);
  }
  
  private String loginPage = null;
  
  public String getLoginPage()
  {
    return this.loginPage;
  }
  
  public void setLoginPage(String loginPage)
  {
    this.loginPage = RequestUtil.URLDecode(loginPage);
  }
  
  private String realmName = null;
  
  public String getRealmName()
  {
    return this.realmName;
  }
  
  public void setRealmName(String realmName)
  {
    this.realmName = realmName;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("LoginConfig[");
    sb.append("authMethod=");
    sb.append(this.authMethod);
    if (this.realmName != null)
    {
      sb.append(", realmName=");
      sb.append(this.realmName);
    }
    if (this.loginPage != null)
    {
      sb.append(", loginPage=");
      sb.append(this.loginPage);
    }
    if (this.errorPage != null)
    {
      sb.append(", errorPage=");
      sb.append(this.errorPage);
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.authMethod == null ? 0 : this.authMethod.hashCode());
    
    result = 31 * result + (this.errorPage == null ? 0 : this.errorPage.hashCode());
    
    result = 31 * result + (this.loginPage == null ? 0 : this.loginPage.hashCode());
    
    result = 31 * result + (this.realmName == null ? 0 : this.realmName.hashCode());
    
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LoginConfig)) {
      return false;
    }
    LoginConfig other = (LoginConfig)obj;
    if (this.authMethod == null)
    {
      if (other.authMethod != null) {
        return false;
      }
    }
    else if (!this.authMethod.equals(other.authMethod)) {
      return false;
    }
    if (this.errorPage == null)
    {
      if (other.errorPage != null) {
        return false;
      }
    }
    else if (!this.errorPage.equals(other.errorPage)) {
      return false;
    }
    if (this.loginPage == null)
    {
      if (other.loginPage != null) {
        return false;
      }
    }
    else if (!this.loginPage.equals(other.loginPage)) {
      return false;
    }
    if (this.realmName == null)
    {
      if (other.realmName != null) {
        return false;
      }
    }
    else if (!this.realmName.equals(other.realmName)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\LoginConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */