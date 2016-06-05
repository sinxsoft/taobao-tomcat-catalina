package org.apache.naming;

import javax.naming.Reference;

public class TransactionRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.TransactionFactory";
  
  public TransactionRef()
  {
    this(null, null);
  }
  
  public TransactionRef(String factory, String factoryLocation)
  {
    super("javax.transaction.UserTransaction", factory, factoryLocation);
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
    return "org.apache.naming.factory.TransactionFactory";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\TransactionRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */