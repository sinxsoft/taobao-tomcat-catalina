package org.apache.catalina.deploy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ResourceBase
  implements Serializable, Injectable
{
  private static final long serialVersionUID = 1L;
  private String description = null;
  
  public ResourceBase() {}
  
  public String getDescription()
  {
    return this.description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  private String name = null;
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  private String type = null;
  
  public String getType()
  {
    return this.type;
  }
  
  public void setType(String type)
  {
    this.type = type;
  }
  
  private final HashMap<String, Object> properties = new HashMap();
  
  public Object getProperty(String name)
  {
    return this.properties.get(name);
  }
  
  public void setProperty(String name, Object value)
  {
    this.properties.put(name, value);
  }
  
  public void removeProperty(String name)
  {
    this.properties.remove(name);
  }
  
  public Iterator<String> listProperties()
  {
    return this.properties.keySet().iterator();
  }
  
  private final List<InjectionTarget> injectionTargets = new ArrayList();
  
  public void addInjectionTarget(String injectionTargetName, String jndiName)
  {
    InjectionTarget target = new InjectionTarget(injectionTargetName, jndiName);
    this.injectionTargets.add(target);
  }
  
  public List<InjectionTarget> getInjectionTargets()
  {
    return this.injectionTargets;
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.description == null ? 0 : this.description.hashCode());
    
    result = 31 * result + (this.injectionTargets == null ? 0 : this.injectionTargets.hashCode());
    
    result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
    result = 31 * result + (this.properties == null ? 0 : this.properties.hashCode());
    
    result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ResourceBase other = (ResourceBase)obj;
    if (this.description == null)
    {
      if (other.description != null) {
        return false;
      }
    }
    else if (!this.description.equals(other.description)) {
      return false;
    }
    if (this.injectionTargets == null)
    {
      if (other.injectionTargets != null) {
        return false;
      }
    }
    else if (!this.injectionTargets.equals(other.injectionTargets)) {
      return false;
    }
    if (this.name == null)
    {
      if (other.name != null) {
        return false;
      }
    }
    else if (!this.name.equals(other.name)) {
      return false;
    }
    if (this.properties == null)
    {
      if (other.properties != null) {
        return false;
      }
    }
    else if (!this.properties.equals(other.properties)) {
      return false;
    }
    if (this.type == null)
    {
      if (other.type != null) {
        return false;
      }
    }
    else if (!this.type.equals(other.type)) {
      return false;
    }
    return true;
  }
  
  protected NamingResources resources = null;
  
  public NamingResources getNamingResources()
  {
    return this.resources;
  }
  
  void setNamingResources(NamingResources resources)
  {
    this.resources = resources;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ResourceBase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */