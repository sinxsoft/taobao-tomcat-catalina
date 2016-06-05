package org.apache.catalina.mbeans;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.NamingResources;
import org.apache.tomcat.util.modeler.BaseModelMBean;

public class ContextResourceMBean
  extends BaseModelMBean
{
  public ContextResourceMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public Object getAttribute(String name)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    if (name == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name is null"), "Attribute name is null");
    }
    ContextResource cr = null;
    try
    {
      cr = (ContextResource)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    String value = null;
    if ("auth".equals(name)) {
      return cr.getAuth();
    }
    if ("description".equals(name)) {
      return cr.getDescription();
    }
    if ("name".equals(name)) {
      return cr.getName();
    }
    if ("scope".equals(name)) {
      return cr.getScope();
    }
    if ("type".equals(name)) {
      return cr.getType();
    }
    value = (String)cr.getProperty(name);
    if (value == null) {
      throw new AttributeNotFoundException("Cannot find attribute " + name);
    }
    return value;
  }
  
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    if (attribute == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Attribute is null"), "Attribute is null");
    }
    String name = attribute.getName();
    Object value = attribute.getValue();
    if (name == null) {
      throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name is null"), "Attribute name is null");
    }
    ContextResource cr = null;
    try
    {
      cr = (ContextResource)getManagedResource();
    }
    catch (InstanceNotFoundException e)
    {
      throw new MBeanException(e);
    }
    catch (InvalidTargetObjectTypeException e)
    {
      throw new MBeanException(e);
    }
    if ("auth".equals(name)) {
      cr.setAuth((String)value);
    } else if ("description".equals(name)) {
      cr.setDescription((String)value);
    } else if ("name".equals(name)) {
      cr.setName((String)value);
    } else if ("scope".equals(name)) {
      cr.setScope((String)value);
    } else if ("type".equals(name)) {
      cr.setType((String)value);
    } else {
      cr.setProperty(name, "" + value);
    }
    NamingResources nr = cr.getNamingResources();
    nr.removeResource(cr.getName());
    nr.addResource(cr);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\ContextResourceMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */