package org.apache.catalina.filters;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometEvent.EventType;
import org.apache.catalina.comet.CometFilter;
import org.apache.catalina.comet.CometFilterChain;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class FailedRequestFilter
  extends FilterBase
  implements CometFilter
{
  private static final Log log = LogFactory.getLog(FailedRequestFilter.class);
  
  protected Log getLogger()
  {
    return log;
  }
  
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException
  {
    if (!isGoodRequest(request))
    {
      ((HttpServletResponse)response).sendError(400);
      
      return;
    }
    chain.doFilter(request, response);
  }
  
  public void doFilterEvent(CometEvent event, CometFilterChain chain)
    throws IOException, ServletException
  {
    if ((event.getEventType() == CometEvent.EventType.BEGIN) && (!isGoodRequest(event.getHttpServletRequest())))
    {
      event.getHttpServletResponse().sendError(400);
      
      event.close();
      return;
    }
    chain.doFilterEvent(event);
  }
  
  private boolean isGoodRequest(ServletRequest request)
  {
    request.getParameter("none");
    if (request.getAttribute("org.apache.catalina.parameter_parse_failed") != null) {
      return false;
    }
    return true;
  }
  
  protected boolean isConfigProblemFatal()
  {
    return true;
  }
}
