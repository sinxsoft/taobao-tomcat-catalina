package org.apache.catalina.mbeans;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.NamingResources;
import org.apache.tomcat.util.modeler.BaseModelMBean;

public class ContextEnvironmentMBean
  extends BaseModelMBean
{
  public ContextEnvironmentMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    super.setAttribute(attribute);
    
    ContextEnvironment ce = null;
    try
    {
      ce = (ContextEnvironment)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    NamingResources nr = ce.getNamingResources();
    nr.removeEnvironment(ce.getName());
    nr.addEnvironment(ce);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\ContextEnvironmentMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */