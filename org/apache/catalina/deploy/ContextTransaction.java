package org.apache.catalina.deploy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ContextTransaction
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private HashMap<String, Object> properties = new HashMap();
  
  public ContextTransaction() {}
  
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
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("Transaction[");
    sb.append("]");
    return sb.toString();
  }
  
  @Deprecated
  protected NamingResources resources = null;
  
  @Deprecated
  public NamingResources getNamingResources()
  {
    return this.resources;
  }
  
  @Deprecated
  void setNamingResources(NamingResources resources)
  {
    this.resources = resources;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ContextTransaction.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */