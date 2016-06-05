package org.apache.catalina.mbeans;

import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import org.apache.tomcat.util.modeler.BaseModelMBean;

public class ClassNameMBean
  extends BaseModelMBean
{
  public ClassNameMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public String getClassName()
  {
    return this.resource.getClass().getName();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\ClassNameMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */