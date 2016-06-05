package org.apache.naming;

import java.util.Enumeration;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

public class HandlerRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.HandlerFactory";
  public static final String HANDLER_NAME = "handlername";
  public static final String HANDLER_CLASS = "handlerclass";
  public static final String HANDLER_LOCALPART = "handlerlocalpart";
  public static final String HANDLER_NAMESPACE = "handlernamespace";
  public static final String HANDLER_PARAMNAME = "handlerparamname";
  public static final String HANDLER_PARAMVALUE = "handlerparamvalue";
  public static final String HANDLER_SOAPROLE = "handlersoaprole";
  public static final String HANDLER_PORTNAME = "handlerportname";
  
  public HandlerRef(String refname, String handlerClass)
  {
    this(refname, handlerClass, null, null);
  }
  
  public HandlerRef(String refname, String handlerClass, String factory, String factoryLocation)
  {
    super(refname, factory, factoryLocation);
    StringRefAddr refAddr = null;
    if (refname != null)
    {
      refAddr = new StringRefAddr("handlername", refname);
      add(refAddr);
    }
    if (handlerClass != null)
    {
      refAddr = new StringRefAddr("handlerclass", handlerClass);
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
    return "org.apache.naming.factory.HandlerFactory";
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("HandlerRef[");
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


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\HandlerRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */