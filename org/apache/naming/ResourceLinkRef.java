package org.apache.naming;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

public class ResourceLinkRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.ResourceLinkFactory";
  public static final String GLOBALNAME = "globalName";
  
  public ResourceLinkRef(String resourceClass, String globalName)
  {
    this(resourceClass, globalName, null, null);
  }
  
  public ResourceLinkRef(String resourceClass, String globalName, String factory, String factoryLocation)
  {
    super(resourceClass, factory, factoryLocation);
    StringRefAddr refAddr = null;
    if (globalName != null)
    {
      refAddr = new StringRefAddr("globalName", globalName);
      add(refAddr);
    }
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
    return "org.apache.naming.factory.ResourceLinkFactory";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\ResourceLinkRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */