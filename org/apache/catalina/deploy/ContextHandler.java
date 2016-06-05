package org.apache.catalina.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ContextHandler
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String handlerclass = null;
  
  public ContextHandler() {}
  
  public String getHandlerclass()
  {
    return this.handlerclass;
  }
  
  public void setHandlerclass(String handlerclass)
  {
    this.handlerclass = handlerclass;
  }
  
  private final HashMap<String, String> soapHeaders = new HashMap();
  
  public Iterator<String> getLocalparts()
  {
    return this.soapHeaders.keySet().iterator();
  }
  
  public String getNamespaceuri(String localpart)
  {
    return (String)this.soapHeaders.get(localpart);
  }
  
  public void addSoapHeaders(String localpart, String namespaceuri)
  {
    this.soapHeaders.put(localpart, namespaceuri);
  }
  
  public void setProperty(String name, String value)
  {
    setProperty(name, value);
  }
  
  private final ArrayList<String> soapRoles = new ArrayList();
  
  public String getSoapRole(int i)
  {
    return (String)this.soapRoles.get(i);
  }
  
  public int getSoapRolesSize()
  {
    return this.soapRoles.size();
  }
  
  public void addSoapRole(String soapRole)
  {
    this.soapRoles.add(soapRole);
  }
  
  private final ArrayList<String> portNames = new ArrayList();
  
  public String getPortName(int i)
  {
    return (String)this.portNames.get(i);
  }
  
  public int getPortNamesSize()
  {
    return this.portNames.size();
  }
  
  public void addPortName(String portName)
  {
    this.portNames.add(portName);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextHandler[");
    sb.append("name=");
    sb.append(getName());
    if (this.handlerclass != null)
    {
      sb.append(", class=");
      sb.append(this.handlerclass);
    }
    if (this.soapHeaders != null)
    {
      sb.append(", soap-headers=");
      sb.append(this.soapHeaders);
    }
    if (getSoapRolesSize() > 0)
    {
      sb.append(", soap-roles=");
      sb.append(this.soapRoles);
    }
    if (getPortNamesSize() > 0)
    {
      sb.append(", port-name=");
      sb.append(this.portNames);
    }
    if (listProperties() != null)
    {
      sb.append(", init-param=");
      sb.append(listProperties());
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.handlerclass == null ? 0 : this.handlerclass.hashCode());
    
    result = 31 * result + (this.portNames == null ? 0 : this.portNames.hashCode());
    
    result = 31 * result + (this.soapHeaders == null ? 0 : this.soapHeaders.hashCode());
    
    result = 31 * result + (this.soapRoles == null ? 0 : this.soapRoles.hashCode());
    
    return result;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ContextHandler other = (ContextHandler)obj;
    if (this.handlerclass == null)
    {
      if (other.handlerclass != null) {
        return false;
      }
    }
    else if (!this.handlerclass.equals(other.handlerclass)) {
      return false;
    }
    if (this.portNames == null)
    {
      if (other.portNames != null) {
        return false;
      }
    }
    else if (!this.portNames.equals(other.portNames)) {
      return false;
    }
    if (this.soapHeaders == null)
    {
      if (other.soapHeaders != null) {
        return false;
      }
    }
    else if (!this.soapHeaders.equals(other.soapHeaders)) {
      return false;
    }
    if (this.soapRoles == null)
    {
      if (other.soapRoles != null) {
        return false;
      }
    }
    else if (!this.soapRoles.equals(other.soapRoles)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextHandler.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */