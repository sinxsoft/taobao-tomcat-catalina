package org.apache.naming;

import java.util.Hashtable;

public class ContextAccessController
{
  private static Hashtable<Object, Object> readOnlyContexts = new Hashtable();
  private static Hashtable<Object, Object> securityTokens = new Hashtable();
  
  public ContextAccessController() {}
  
  public static void setSecurityToken(Object name, Object token)
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(new RuntimePermission(ContextAccessController.class.getName() + ".setSecurityToken"));
    }
    if ((!securityTokens.containsKey(name)) && (token != null)) {
      securityTokens.put(name, token);
    }
  }
  
  public static void unsetSecurityToken(Object name, Object token)
  {
    if (checkSecurityToken(name, token)) {
      securityTokens.remove(name);
    }
  }
  
  public static boolean checkSecurityToken(Object name, Object token)
  {
    Object refToken = securityTokens.get(name);
    return (refToken == null) || (refToken.equals(token));
  }
  
  public static void setWritable(Object name, Object token)
  {
    if (checkSecurityToken(name, token)) {
      readOnlyContexts.remove(name);
    }
  }
  
  public static void setReadOnly(Object name)
  {
    readOnlyContexts.put(name, name);
  }
  
  public static boolean isWritable(Object name)
  {
    return !readOnlyContexts.containsKey(name);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\ContextAccessController.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */