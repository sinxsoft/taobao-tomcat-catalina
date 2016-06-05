package org.apache.catalina.realm;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import javax.security.auth.login.LoginContext;
import org.ietf.jgss.GSSCredential;

public class GenericPrincipal
  implements Principal
{
  @Deprecated
  public GenericPrincipal(String name, String password)
  {
    this(name, password, null);
  }
  
  public GenericPrincipal(String name, String password, List<String> roles)
  {
    this(name, password, roles, null);
  }
  
  public GenericPrincipal(String name, String password, List<String> roles, Principal userPrincipal)
  {
    this(name, password, roles, userPrincipal, null);
  }
  
  public GenericPrincipal(String name, String password, List<String> roles, Principal userPrincipal, LoginContext loginContext)
  {
    this(name, password, roles, userPrincipal, loginContext, null);
  }
  
  public GenericPrincipal(String name, String password, List<String> roles, Principal userPrincipal, LoginContext loginContext, GSSCredential gssCredential)
  {
    this.name = name;
    this.password = password;
    this.userPrincipal = userPrincipal;
    if (roles != null)
    {
      this.roles = new String[roles.size()];
      this.roles = ((String[])roles.toArray(this.roles));
      if (this.roles.length > 1) {
        Arrays.sort(this.roles);
      }
    }
    this.loginContext = loginContext;
    this.gssCredential = gssCredential;
  }
  
  protected String name = null;
  
  public String getName()
  {
    return this.name;
  }
  
  protected String password = null;
  
  public String getPassword()
  {
    return this.password;
  }
  
  protected String[] roles = new String[0];
  
  public String[] getRoles()
  {
    return this.roles;
  }
  
  protected Principal userPrincipal = null;
  
  public Principal getUserPrincipal()
  {
    if (this.userPrincipal != null) {
      return this.userPrincipal;
    }
    return this;
  }
  
  protected LoginContext loginContext = null;
  protected GSSCredential gssCredential = null;
  
  public GSSCredential getGssCredential()
  {
    return this.gssCredential;
  }
  
  protected void setGssCredential(GSSCredential gssCredential)
  {
    this.gssCredential = gssCredential;
  }
  
  public boolean hasRole(String role)
  {
    if ("*".equals(role)) {
      return true;
    }
    if (role == null) {
      return false;
    }
    return Arrays.binarySearch(this.roles, role) >= 0;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("GenericPrincipal[");
    sb.append(this.name);
    sb.append("(");
    for (int i = 0; i < this.roles.length; i++) {
      sb.append(this.roles[i]).append(",");
    }
    sb.append(")]");
    return sb.toString();
  }
  
  public void logout()
    throws Exception
  {
    if (this.loginContext != null) {
      this.loginContext.logout();
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\GenericPrincipal.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */