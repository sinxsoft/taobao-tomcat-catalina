package org.apache.catalina.mbeans;

import java.util.ArrayList;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.tomcat.util.modeler.BaseModelMBean;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

public class NamingResourcesMBean
  extends BaseModelMBean
{
  protected Registry registry = MBeanUtils.createRegistry();
  protected ManagedBean managed = this.registry.findManagedBean("NamingResources");
  
  public NamingResourcesMBean()
    throws MBeanException, RuntimeOperationsException
  {}
  
  public String[] getEnvironments()
  {
    ContextEnvironment[] envs = ((NamingResources)this.resource).findEnvironments();
    
    ArrayList<String> results = new ArrayList();
    for (int i = 0; i < envs.length; i++) {
      try
      {
        ObjectName oname = MBeanUtils.createObjectName(this.managed.getDomain(), envs[i]);
        
        results.add(oname.toString());
      }
      catch (MalformedObjectNameException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for environment " + envs[i]);
        
        iae.initCause(e);
        throw iae;
      }
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String[] getResources()
  {
    ContextResource[] resources = ((NamingResources)this.resource).findResources();
    
    ArrayList<String> results = new ArrayList();
    for (int i = 0; i < resources.length; i++) {
      try
      {
        ObjectName oname = MBeanUtils.createObjectName(this.managed.getDomain(), resources[i]);
        
        results.add(oname.toString());
      }
      catch (MalformedObjectNameException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for resource " + resources[i]);
        
        iae.initCause(e);
        throw iae;
      }
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String[] getResourceLinks()
  {
    ContextResourceLink[] resourceLinks = ((NamingResources)this.resource).findResourceLinks();
    
    ArrayList<String> results = new ArrayList();
    for (int i = 0; i < resourceLinks.length; i++) {
      try
      {
        ObjectName oname = MBeanUtils.createObjectName(this.managed.getDomain(), resourceLinks[i]);
        
        results.add(oname.toString());
      }
      catch (MalformedObjectNameException e)
      {
        IllegalArgumentException iae = new IllegalArgumentException("Cannot create object name for resource " + resourceLinks[i]);
        
        iae.initCause(e);
        throw iae;
      }
    }
    return (String[])results.toArray(new String[results.size()]);
  }
  
  public String addEnvironment(String envName, String type, String value)
    throws MalformedObjectNameException
  {
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return null;
    }
    ContextEnvironment env = nresources.findEnvironment(envName);
    if (env != null) {
      throw new IllegalArgumentException("Invalid environment name - already exists '" + envName + "'");
    }
    env = new ContextEnvironment();
    env.setName(envName);
    env.setType(type);
    env.setValue(value);
    nresources.addEnvironment(env);
    
    ManagedBean managed = this.registry.findManagedBean("ContextEnvironment");
    ObjectName oname = MBeanUtils.createObjectName(managed.getDomain(), env);
    
    return oname.toString();
  }
  
  public String addResource(String resourceName, String type)
    throws MalformedObjectNameException
  {
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return null;
    }
    ContextResource resource = nresources.findResource(resourceName);
    if (resource != null) {
      throw new IllegalArgumentException("Invalid resource name - already exists'" + resourceName + "'");
    }
    resource = new ContextResource();
    resource.setName(resourceName);
    resource.setType(type);
    nresources.addResource(resource);
    
    ManagedBean managed = this.registry.findManagedBean("ContextResource");
    ObjectName oname = MBeanUtils.createObjectName(managed.getDomain(), resource);
    
    return oname.toString();
  }
  
  public String addResourceLink(String resourceLinkName, String type)
    throws MalformedObjectNameException
  {
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return null;
    }
    ContextResourceLink resourceLink = nresources.findResourceLink(resourceLinkName);
    if (resourceLink != null) {
      throw new IllegalArgumentException("Invalid resource link name - already exists'" + resourceLinkName + "'");
    }
    resourceLink = new ContextResourceLink();
    resourceLink.setName(resourceLinkName);
    resourceLink.setType(type);
    nresources.addResourceLink(resourceLink);
    
    ManagedBean managed = this.registry.findManagedBean("ContextResourceLink");
    ObjectName oname = MBeanUtils.createObjectName(managed.getDomain(), resourceLink);
    
    return oname.toString();
  }
  
  public void removeEnvironment(String envName)
  {
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return;
    }
    ContextEnvironment env = nresources.findEnvironment(envName);
    if (env == null) {
      throw new IllegalArgumentException("Invalid environment name '" + envName + "'");
    }
    nresources.removeEnvironment(envName);
  }
  
  public void removeResource(String resourceName)
  {
    resourceName = ObjectName.unquote(resourceName);
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return;
    }
    ContextResource resource = nresources.findResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Invalid resource name '" + resourceName + "'");
    }
    nresources.removeResource(resourceName);
  }
  
  public void removeResourceLink(String resourceLinkName)
  {
    resourceLinkName = ObjectName.unquote(resourceLinkName);
    NamingResources nresources = (NamingResources)this.resource;
    if (nresources == null) {
      return;
    }
    ContextResourceLink resourceLink = nresources.findResourceLink(resourceLinkName);
    if (resourceLink == null) {
      throw new IllegalArgumentException("Invalid resource Link name '" + resourceLinkName + "'");
    }
    nresources.removeResourceLink(resourceLinkName);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\NamingResourcesMBean.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */