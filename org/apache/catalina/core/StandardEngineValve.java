package org.apache.catalina.core;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Host;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.res.StringManager;

final class StandardEngineValve
  extends ValveBase
{
  private static final String info = "org.apache.catalina.core.StandardEngineValve/1.0";
  
  public StandardEngineValve()
  {
    super(true);
  }
  
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public String getInfo()
  {
    return "org.apache.catalina.core.StandardEngineValve/1.0";
  }
  
  public final void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    Host host = request.getHost();
    if (host == null)
    {
      response.sendError(400, sm.getString("standardEngine.noHost", new Object[] { request.getServerName() }));
      
      return;
    }
    if (request.isAsyncSupported()) {
      request.setAsyncSupported(host.getPipeline().isAsyncSupported());
    }
    host.getPipeline().getFirst().invoke(request, response);
  }
  
  public final void event(Request request, Response response, CometEvent event)
    throws IOException, ServletException
  {
    request.getHost().getPipeline().getFirst().event(request, response, event);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\StandardEngineValve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */