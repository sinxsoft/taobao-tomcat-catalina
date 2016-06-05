package org.apache.catalina.comet;

import java.io.IOException;
import javax.servlet.ServletException;

public abstract interface CometFilterChain
{
  public abstract void doFilterEvent(CometEvent paramCometEvent)
    throws IOException, ServletException;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\comet\CometFilterChain.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */