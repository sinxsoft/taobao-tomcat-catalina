package org.apache.catalina.core;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public final class StandardWrapperFacade
  implements ServletConfig
{
  public StandardWrapperFacade(StandardWrapper config)
  {
    this.config = config;
  }
  
  private ServletConfig config = null;
  private ServletContext context = null;
  
  public String getServletName()
  {
    return this.config.getServletName();
  }
  
  public ServletContext getServletContext()
  {
    if (this.context == null)
    {
      this.context = this.config.getServletContext();
      if ((this.context != null) && ((this.context instanceof ApplicationContext))) {
        this.context = ((ApplicationContext)this.context).getFacade();
      }
    }
    return this.context;
  }
  
  public String getInitParameter(String name)
  {
    return this.config.getInitParameter(name);
  }
  
  public Enumeration<String> getInitParameterNames()
  {
    return this.config.getInitParameterNames();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardWrapperFacade.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */