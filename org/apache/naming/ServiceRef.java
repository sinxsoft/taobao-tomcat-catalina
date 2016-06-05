package org.apache.naming;

import java.util.Enumeration;
import java.util.Vector;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

public class ServiceRef
  extends Reference
{
  private static final long serialVersionUID = 1L;
  public static final String DEFAULT_FACTORY = "org.apache.naming.factory.webservices.ServiceRefFactory";
  public static final String SERVICE_INTERFACE = "serviceInterface";
  public static final String SERVICE_NAMESPACE = "service namespace";
  public static final String SERVICE_LOCAL_PART = "service local part";
  public static final String WSDL = "wsdl";
  public static final String JAXRPCMAPPING = "jaxrpcmapping";
  public static final String PORTCOMPONENTLINK = "portcomponentlink";
  public static final String SERVICEENDPOINTINTERFACE = "serviceendpointinterface";
  private Vector<HandlerRef> handlers = new Vector();
  
  public ServiceRef(String refname, String serviceInterface, String[] serviceQname, String wsdl, String jaxrpcmapping)
  {
    this(refname, serviceInterface, serviceQname, wsdl, jaxrpcmapping, null, null);
  }
  
  public ServiceRef(String refname, String serviceInterface, String[] serviceQname, String wsdl, String jaxrpcmapping, String factory, String factoryLocation)
  {
    super(serviceInterface, factory, factoryLocation);
    StringRefAddr refAddr = null;
    if (serviceInterface != null)
    {
      refAddr = new StringRefAddr("serviceInterface", serviceInterface);
      add(refAddr);
    }
    if (serviceQname[0] != null)
    {
      refAddr = new StringRefAddr("service namespace", serviceQname[0]);
      add(refAddr);
    }
    if (serviceQname[1] != null)
    {
      refAddr = new StringRefAddr("service local part", serviceQname[1]);
      add(refAddr);
    }
    if (wsdl != null)
    {
      refAddr = new StringRefAddr("wsdl", wsdl);
      add(refAddr);
    }
    if (jaxrpcmapping != null)
    {
      refAddr = new StringRefAddr("jaxrpcmapping", jaxrpcmapping);
      add(refAddr);
    }
  }
  
  public HandlerRef getHandler()
  {
    return (HandlerRef)this.handlers.remove(0);
  }
  
  public int getHandlersSize()
  {
    return this.handlers.size();
  }
  
  public void addHandler(HandlerRef handler)
  {
    this.handlers.add(handler);
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
    return "org.apache.naming.factory.webservices.ServiceRefFactory";
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ServiceRef[");
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


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\ServiceRef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */