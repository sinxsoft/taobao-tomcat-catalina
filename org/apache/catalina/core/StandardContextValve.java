package org.apache.catalina.core;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.res.StringManager;

final class StandardContextValve
  extends ValveBase
{
  private static final String info = "org.apache.catalina.core.StandardContextValve/1.0";
  
  public StandardContextValve()
  {
    super(true);
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardContextValve/1.0";
  }
  
  public void setContainer(Container container)
  {
    super.setContainer(container);
  }
  
  public final void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    MessageBytes requestPathMB = request.getRequestPathMB();
    if ((requestPathMB.startsWithIgnoreCase("/META-INF/", 0)) || (requestPathMB.equalsIgnoreCase("/META-INF")) || (requestPathMB.startsWithIgnoreCase("/WEB-INF/", 0)) || (requestPathMB.equalsIgnoreCase("/WEB-INF")))
    {
      response.sendError(404);
      return;
    }
    Wrapper wrapper = request.getWrapper();
    if ((wrapper == null) || (wrapper.isUnavailable()))
    {
      response.sendError(404);
      return;
    }
    try
    {
      response.sendAcknowledgement();
    }
    catch (IOException ioe)
    {
      this.container.getLogger().error(sm.getString("standardContextValve.acknowledgeException"), ioe);
      
      request.setAttribute("javax.servlet.error.exception", ioe);
      response.sendError(500);
      return;
    }
    if (request.isAsyncSupported()) {
      request.setAsyncSupported(wrapper.getPipeline().isAsyncSupported());
    }
    wrapper.getPipeline().getFirst().invoke(request, response);
  }
  
  public final void event(Request request, Response response, CometEvent event)
    throws IOException, ServletException
  {
    Wrapper wrapper = request.getWrapper();
    
    wrapper.getPipeline().getFirst().event(request, response, event);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardContextValve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */