package org.apache.catalina.mbeans;

import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import org.apache.tomcat.util.modeler.BaseModelMBean;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

public class RoleMBean
  extends BaseModelMBean
{
  protected Registry registry = MBeanUtils.createRegistry();
  protected ManagedBean managed = this.registry.findManagedBean("Role");
  
  public RoleMBean()
    throws MBeanException, RuntimeOperationsException
  {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\RoleMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */