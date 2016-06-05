package org.apache.catalina.deploy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ContextService
  extends ResourceBase
{
  private static final long serialVersionUID = 1L;
  private String displayname = null;
  
  public ContextService() {}
  
  public String getDisplayname()
  {
    return this.displayname;
  }
  
  public void setDisplayname(String displayname)
  {
    this.displayname = displayname;
  }
  
  private String largeIcon = null;
  
  public String getLargeIcon()
  {
    return this.largeIcon;
  }
  
  public void setLargeIcon(String largeIcon)
  {
    this.largeIcon = largeIcon;
  }
  
  private String smallIcon = null;
  
  public String getSmallIcon()
  {
    return this.smallIcon;
  }
  
  public void setSmallIcon(String smallIcon)
  {
    this.smallIcon = smallIcon;
  }
  
  private String serviceInterface = null;
  
  public String getInterface()
  {
    return this.serviceInterface;
  }
  
  public void setInterface(String serviceInterface)
  {
    this.serviceInterface = serviceInterface;
  }
  
  private String wsdlfile = null;
  
  public String getWsdlfile()
  {
    return this.wsdlfile;
  }
  
  public void setWsdlfile(String wsdlfile)
  {
    this.wsdlfile = wsdlfile;
  }
  
  private String jaxrpcmappingfile = null;
  
  public String getJaxrpcmappingfile()
  {
    return this.jaxrpcmappingfile;
  }
  
  public void setJaxrpcmappingfile(String jaxrpcmappingfile)
  {
    this.jaxrpcmappingfile = jaxrpcmappingfile;
  }
  
  private String[] serviceqname = new String[2];
  
  public String[] getServiceqname()
  {
    return this.serviceqname;
  }
  
  public String getServiceqname(int i)
  {
    return this.serviceqname[i];
  }
  
  public String getServiceqnameNamespaceURI()
  {
    return this.serviceqname[0];
  }
  
  public String getServiceqnameLocalpart()
  {
    return this.serviceqname[1];
  }
  
  public void setServiceqname(String[] serviceqname)
  {
    this.serviceqname = serviceqname;
  }
  
  public void setServiceqname(String serviceqname, int i)
  {
    this.serviceqname[i] = serviceqname;
  }
  
  public void setServiceqnameNamespaceURI(String namespaceuri)
  {
    this.serviceqname[0] = namespaceuri;
  }
  
  public void setServiceqnameLocalpart(String localpart)
  {
    this.serviceqname[1] = localpart;
  }
  
  public Iterator<String> getServiceendpoints()
  {
    return listProperties();
  }
  
  public String getPortlink(String serviceendpoint)
  {
    return (String)getProperty(serviceendpoint);
  }
  
  public void addPortcomponent(String serviceendpoint, String portlink)
  {
    if (portlink == null) {
      portlink = "";
    }
    setProperty(serviceendpoint, portlink);
  }
  
  private final HashMap<String, ContextHandler> handlers = new HashMap();
  
  public Iterator<String> getHandlers()
  {
    return this.handlers.keySet().iterator();
  }
  
  public ContextHandler getHandler(String handlername)
  {
    return (ContextHandler)this.handlers.get(handlername);
  }
  
  public void addHandler(ContextHandler handler)
  {
    this.handlers.put(handler.getName(), handler);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ContextService[");
    sb.append("name=");
    sb.append(getName());
    if (getDescription() != null)
    {
      sb.append(", description=");
      sb.append(getDescription());
    }
    if (getType() != null)
    {
      sb.append(", type=");
      sb.append(getType());
    }
    if (this.displayname != null)
    {
      sb.append(", displayname=");
      sb.append(this.displayname);
    }
    if (this.largeIcon != null)
    {
      sb.append(", largeIcon=");
      sb.append(this.largeIcon);
    }
    if (this.smallIcon != null)
    {
      sb.append(", smallIcon=");
      sb.append(this.smallIcon);
    }
    if (this.wsdlfile != null)
    {
      sb.append(", wsdl-file=");
      sb.append(this.wsdlfile);
    }
    if (this.jaxrpcmappingfile != null)
    {
      sb.append(", jaxrpc-mapping-file=");
      sb.append(this.jaxrpcmappingfile);
    }
    if (this.serviceqname[0] != null)
    {
      sb.append(", service-qname/namespaceURI=");
      sb.append(this.serviceqname[0]);
    }
    if (this.serviceqname[1] != null)
    {
      sb.append(", service-qname/localpart=");
      sb.append(this.serviceqname[1]);
    }
    if (getServiceendpoints() != null)
    {
      sb.append(", port-component/service-endpoint-interface=");
      sb.append(getServiceendpoints());
    }
    if (this.handlers != null)
    {
      sb.append(", handler=");
      sb.append(this.handlers);
    }
    sb.append("]");
    return sb.toString();
  }
  
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = 31 * result + (this.displayname == null ? 0 : this.displayname.hashCode());
    
    result = 31 * result + (this.handlers == null ? 0 : this.handlers.hashCode());
    
    result = 31 * result + (this.jaxrpcmappingfile == null ? 0 : this.jaxrpcmappingfile.hashCode());
    
    result = 31 * result + (this.largeIcon == null ? 0 : this.largeIcon.hashCode());
    
    result = 31 * result + (this.serviceInterface == null ? 0 : this.serviceInterface.hashCode());
    
    result = 31 * result + Arrays.hashCode(this.serviceqname);
    result = 31 * result + (this.smallIcon == null ? 0 : this.smallIcon.hashCode());
    
    result = 31 * result + (this.wsdlfile == null ? 0 : this.wsdlfile.hashCode());
    
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
    ContextService other = (ContextService)obj;
    if (this.displayname == null)
    {
      if (other.displayname != null) {
        return false;
      }
    }
    else if (!this.displayname.equals(other.displayname)) {
      return false;
    }
    if (this.handlers == null)
    {
      if (other.handlers != null) {
        return false;
      }
    }
    else if (!this.handlers.equals(other.handlers)) {
      return false;
    }
    if (this.jaxrpcmappingfile == null)
    {
      if (other.jaxrpcmappingfile != null) {
        return false;
      }
    }
    else if (!this.jaxrpcmappingfile.equals(other.jaxrpcmappingfile)) {
      return false;
    }
    if (this.largeIcon == null)
    {
      if (other.largeIcon != null) {
        return false;
      }
    }
    else if (!this.largeIcon.equals(other.largeIcon)) {
      return false;
    }
    if (this.serviceInterface == null)
    {
      if (other.serviceInterface != null) {
        return false;
      }
    }
    else if (!this.serviceInterface.equals(other.serviceInterface)) {
      return false;
    }
    if (!Arrays.equals(this.serviceqname, other.serviceqname)) {
      return false;
    }
    if (this.smallIcon == null)
    {
      if (other.smallIcon != null) {
        return false;
      }
    }
    else if (!this.smallIcon.equals(other.smallIcon)) {
      return false;
    }
    if (this.wsdlfile == null)
    {
      if (other.wsdlfile != null) {
        return false;
      }
    }
    else if (!this.wsdlfile.equals(other.wsdlfile)) {
      return false;
    }
    return true;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextService.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */