package org.apache.catalina.core;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import org.apache.catalina.Globals;
import org.apache.catalina.Wrapper;
import org.apache.catalina.comet.CometFilter;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.FilterMap;
import org.apache.tomcat.util.ExceptionUtils;

public final class ApplicationFilterFactory
{
  @Deprecated
  public static final String DISPATCHER_TYPE_ATTR = "org.apache.catalina.core.DISPATCHER_TYPE";
  @Deprecated
  public static final String DISPATCHER_REQUEST_PATH_ATTR = "org.apache.catalina.core.DISPATCHER_REQUEST_PATH";
  private static ApplicationFilterFactory factory = null;
  
  private ApplicationFilterFactory() {}
  
  public static ApplicationFilterFactory getInstance()
  {
    if (factory == null) {
      factory = new ApplicationFilterFactory();
    }
    return factory;
  }
  
  public ApplicationFilterChain createFilterChain(ServletRequest request, Wrapper wrapper, Servlet servlet)
  {
    DispatcherType dispatcher = null;
    if (request.getAttribute("org.apache.catalina.core.DISPATCHER_TYPE") != null) {
      dispatcher = (DispatcherType)request.getAttribute("org.apache.catalina.core.DISPATCHER_TYPE");
    }
    String requestPath = null;
    Object attribute = request.getAttribute("org.apache.catalina.core.DISPATCHER_REQUEST_PATH");
    if (attribute != null) {
      requestPath = attribute.toString();
    }
    if (servlet == null) {
      return null;
    }
    boolean comet = false;
    
    ApplicationFilterChain filterChain = null;
    if ((request instanceof Request))
    {
      Request req = (Request)request;
      comet = req.isComet();
      if (Globals.IS_SECURITY_ENABLED)
      {
        filterChain = new ApplicationFilterChain();
        if (comet) {
          req.setFilterChain(filterChain);
        }
      }
      else
      {
        filterChain = (ApplicationFilterChain)req.getFilterChain();
        if (filterChain == null)
        {
          filterChain = new ApplicationFilterChain();
          req.setFilterChain(filterChain);
        }
      }
    }
    else
    {
      filterChain = new ApplicationFilterChain();
    }
    filterChain.setServlet(servlet);
    
    filterChain.setSupport(((StandardWrapper)wrapper).getInstanceSupport());
    
    StandardContext context = (StandardContext)wrapper.getParent();
    FilterMap[] filterMaps = context.findFilterMaps();
    if ((filterMaps == null) || (filterMaps.length == 0)) {
      return filterChain;
    }
    String servletName = wrapper.getName();
    for (int i = 0; i < filterMaps.length; i++) {
      if (matchDispatcher(filterMaps[i], dispatcher)) {
        if (matchFiltersURL(filterMaps[i], requestPath))
        {
          ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)context.findFilterConfig(filterMaps[i].getFilterName());
          if (filterConfig != null)
          {
            boolean isCometFilter = false;
            if (comet)
            {
              try
              {
                isCometFilter = filterConfig.getFilter() instanceof CometFilter;
              }
              catch (Exception e)
              {
                Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(t);
              }
              if (isCometFilter) {
                filterChain.addFilter(filterConfig);
              }
            }
            else
            {
              filterChain.addFilter(filterConfig);
            }
          }
        }
      }
    }
    for (int i = 0; i < filterMaps.length; i++) {
      if (matchDispatcher(filterMaps[i], dispatcher)) {
        if (matchFiltersServlet(filterMaps[i], servletName))
        {
          ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)context.findFilterConfig(filterMaps[i].getFilterName());
          if (filterConfig != null)
          {
            boolean isCometFilter = false;
            if (comet)
            {
              try
              {
                isCometFilter = filterConfig.getFilter() instanceof CometFilter;
              }
              catch (Exception e) {}
              if (isCometFilter) {
                filterChain.addFilter(filterConfig);
              }
            }
            else
            {
              filterChain.addFilter(filterConfig);
            }
          }
        }
      }
    }
    return filterChain;
  }
  
  private boolean matchFiltersURL(FilterMap filterMap, String requestPath)
  {
    if (filterMap.getMatchAllUrlPatterns()) {
      return true;
    }
    if (requestPath == null) {
      return false;
    }
    String[] testPaths = filterMap.getURLPatterns();
    for (int i = 0; i < testPaths.length; i++) {
      if (matchFiltersURL(testPaths[i], requestPath)) {
        return true;
      }
    }
    return false;
  }
  
  private boolean matchFiltersURL(String testPath, String requestPath)
  {
    if (testPath == null) {
      return false;
    }
    if (testPath.equals(requestPath)) {
      return true;
    }
    if (testPath.equals("/*")) {
      return true;
    }
    if (testPath.endsWith("/*"))
    {
      if (testPath.regionMatches(0, requestPath, 0, testPath.length() - 2))
      {
        if (requestPath.length() == testPath.length() - 2) {
          return true;
        }
        if ('/' == requestPath.charAt(testPath.length() - 2)) {
          return true;
        }
      }
      return false;
    }
    if (testPath.startsWith("*."))
    {
      int slash = requestPath.lastIndexOf('/');
      int period = requestPath.lastIndexOf('.');
      if ((slash >= 0) && (period > slash) && (period != requestPath.length() - 1) && (requestPath.length() - period == testPath.length() - 1)) {
        return testPath.regionMatches(2, requestPath, period + 1, testPath.length() - 2);
      }
    }
    return false;
  }
  
  private boolean matchFiltersServlet(FilterMap filterMap, String servletName)
  {
    if (servletName == null) {
      return false;
    }
    if (filterMap.getMatchAllServletNames()) {
      return true;
    }
    String[] servletNames = filterMap.getServletNames();
    for (int i = 0; i < servletNames.length; i++) {
      if (servletName.equals(servletNames[i])) {
        return true;
      }
    }
    return false;
  }
  
  private boolean matchDispatcher(FilterMap filterMap, DispatcherType type)
  {
    switch (type)
    {
    case FORWARD: 
      if ((filterMap.getDispatcherMapping() & 0x2) > 0) {
        return true;
      }
      break;
    case INCLUDE: 
      if ((filterMap.getDispatcherMapping() & 0x4) > 0) {
        return true;
      }
      break;
    case REQUEST: 
      if ((filterMap.getDispatcherMapping() & 0x8) > 0) {
        return true;
      }
      break;
    case ERROR: 
      if ((filterMap.getDispatcherMapping() & 0x1) > 0) {
        return true;
      }
      break;
    case ASYNC: 
      if ((filterMap.getDispatcherMapping() & 0x10) > 0) {
        return true;
      }
      break;
    }
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationFilterFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */