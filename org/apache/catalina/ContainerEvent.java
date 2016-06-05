package org.apache.catalina;

import java.util.EventObject;

public final class ContainerEvent
  extends EventObject
{
  private static final long serialVersionUID = 1L;
  private Object data = null;
  private String type = null;
  
  public ContainerEvent(Container container, String type, Object data)
  {
    super(container);
    this.type = type;
    this.data = data;
  }
  
  public Object getData()
  {
    return this.data;
  }
  
  public Container getContainer()
  {
    return (Container)getSource();
  }
  
  public String getType()
  {
    return this.type;
  }
  
  public String toString()
  {
    return "ContainerEvent['" + getContainer() + "','" + getType() + "','" + getData() + "']";
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ContainerEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */