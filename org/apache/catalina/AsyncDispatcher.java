package org.apache.catalina;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract interface AsyncDispatcher
{
  public abstract void dispatch(ServletRequest paramServletRequest, ServletResponse paramServletResponse)
    throws ServletException, IOException;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\AsyncDispatcher.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */