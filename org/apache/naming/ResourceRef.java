package org.apache.naming;

import java.util.Enumeration;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

public class ResourceRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.ResourceFactory";
  public static final String DESCRIPTION = "description";
  public static final String SCOPE = "scope";
  public static final String AUTH = "auth";
  public static final String SINGLETON = "singleton";
  
  public ResourceRef(String resourceClass, String description, String scope, String auth, boolean singleton)
  {
    this(resourceClass, description, scope, auth, singleton, null, null);
  }
  
  public ResourceRef(String resourceClass, String description, String scope, String auth, boolean singleton, String factory, String factoryLocation)
  {
    super(resourceClass, factory, factoryLocation);
    StringRefAddr refAddr = null;
    if (description != null)
    {
      refAddr = new StringRefAddr("description", description);
      add(refAddr);
    }
    if (scope != null)
    {
      refAddr = new StringRefAddr("scope", scope);
      add(refAddr);
    }
    if (auth != null)
    {
      refAddr = new StringRefAddr("auth", auth);
      add(refAddr);
    }
    refAddr = new StringRefAddr("singleton", Boolean.toString(singleton));
    add(refAddr);
  }
  
  public String getFactoryClassName()
  {
    String factory = super.getFactoryClassName();
    if (factory != null) {
      return factory;
    }
    factory = System.getProperty("java.naming.factory.object");
    if (factory != null) {
      return null;
    }
    return "org.apache.naming.factory.ResourceFactory";
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ResourceRef[");
    sb.append("className=");
    sb.append(getClassName());
    sb.append(",factoryClassLocation=");
    sb.append(getFactoryClassLocation());
    sb.append(",factoryClassName=");
    sb.append(getFactoryClassName());
    Enumeration<RefAddr> refAddrs = getAll();
    while (refAddrs.hasMoreElements())
    {
      RefAddr refAddr = (RefAddr)refAddrs.nextElement();
      sb.append(",{type=");
      sb.append(refAddr.getType());
      sb.append(",content=");
      sb.append(refAddr.getContent());
      sb.append("}");
    }
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\ResourceRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */