package org.apache.catalina.mbeans;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import org.apache.catalina.Executor;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.modeler.BaseModelMBean;

public class ServiceMBean
  extends BaseModelMBean
{
  public ServiceMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public void addConnector(String address, int port, boolean isAjp, boolean isSSL)
    throws MBeanException
  {
    Service service;
    try
    {
      service = (Service)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (RuntimeOperationsException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    Connector connector = new Connector();
    if ((address != null) && (address.length() > 0)) {
      connector.setProperty("address", address);
    }
    connector.setPort(port);
    connector.setProtocol(isAjp ? "AJP/1.3" : "HTTP/1.1");
    connector.setSecure(isSSL);
    connector.setScheme(isSSL ? "https" : "http");
    
    service.addConnector(connector);
  }
  
  public void addExecutor(String type)
    throws MBeanException
  {
    Service service;
    try
    {
      service = (Service)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (RuntimeOperationsException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    Executor executor;
    try
    {
      executor = (Executor)Class.forName(type).newInstance();
    }
    catch (InstantiationException e)
    {
      throw new MBeanException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new MBeanException(e);
    }
    catch (ClassNotFoundException e)
    {
      throw new MBeanException(e);
    }
    service.addExecutor(executor);
  }
  
  public String[] findConnectors()
    throws MBeanException
  {
    Service service;
    try
    {
      service = (Service)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (RuntimeOperationsException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    Connector[] connectors = service.findConnectors();
    String[] str = new String[connectors.length];
    for (int i = 0; i < connectors.length; i++) {
      str[i] = connectors[i].toString();
    }
    return str;
  }
  
  public String[] findExecutors()
    throws MBeanException
  {
    Service service;
    try
    {
      service = (Service)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (RuntimeOperationsException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    Executor[] executors = service.findExecutors();
    String[] str = new String[executors.length];
    for (int i = 0; i < executors.length; i++) {
      str[i] = executors[i].toString();
    }
    return str;
  }
  
  public String getExecutor(String name)
    throws MBeanException
  {
    Service service;
    try
    {
      service = (Service)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (RuntimeOperationsException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    Executor executor = service.getExecutor(name);
    return executor.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\ServiceMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */