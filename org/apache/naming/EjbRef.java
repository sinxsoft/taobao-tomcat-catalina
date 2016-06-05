package org.apache.naming;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

public class EjbRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.EjbFactory";
  public static final String TYPE = "type";
  public static final String REMOTE = "remote";
  public static final String LINK = "link";
  
  public EjbRef(String ejbType, String home, String remote, String link)
  {
    this(ejbType, home, remote, link, null, null);
  }
  
  public EjbRef(String ejbType, String home, String remote, String link, String factory, String factoryLocation)
  {
    super(home, factory, factoryLocation);
    StringRefAddr refAddr = null;
    if (ejbType != null)
    {
      refAddr = new StringRefAddr("type", ejbType);
      add(refAddr);
    }
    if (remote != null)
    {
      refAddr = new StringRefAddr("remote", remote);
      add(refAddr);
    }
    if (link != null)
    {
      refAddr = new StringRefAddr("link", link);
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
    return "org.apache.naming.factory.EjbFactory";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\EjbRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */