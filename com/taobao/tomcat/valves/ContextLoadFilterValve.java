package com.taobao.tomcat.valves;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class ContextLoadFilterValve
  extends ValveBase
{
  private static final Log log = LogFactory.getLog(ContextLoadFilterValve.class);
  private String filterClassName;
  private Filter filter;
  private String initConfig;
  private FilterChainAdapter filterChainAdapter;
  private boolean failedOnStart;
  
  public ContextLoadFilterValve() {}
  
  protected void initInternal()
    throws LifecycleException
  {
    if (!(getContainer() instanceof Context)) {
      throw new LifecycleException("[ContextLoadFilterValve] must belong to Context.");
    }
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    if (this.filterClassName != null) {
      try
      {
        final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(this.filterClassName);
        if ((clazz != null) && (contains(clazz.getInterfaces(), Filter.class)))
        {
          this.filter = ((Filter)clazz.newInstance());
          
          final StringProperties prop = new StringProperties(this.initConfig, ";", "=");
          
          FilterConfig filterConfig = new FilterConfig()
          {
            public String getFilterName()
            {
              return clazz.getName();
            }
            
            public ServletContext getServletContext()
            {
              Container container = ContextLoadFilterValve.this.getContainer();
              if ((container instanceof Context)) {
                return ((Context)container).getServletContext();
              }
              return null;
            }
            
            public String getInitParameter(String name)
            {
              return prop.getAttribute(name);
            }
            
            public Enumeration<String> getInitParameterNames()
            {
              if (prop.getKeys() != null) {
                return Collections.enumeration(prop.getKeys());
              }
              return null;
            }
          };
          this.filter.init(filterConfig);
          
          this.filterChainAdapter = new FilterChainAdapter(getNext());
        }
      }
      catch (Exception ex)
      {
        this.filter = null;
        this.filterChainAdapter = null;
        if (this.failedOnStart) {
          throw new LifecycleException(this.filterClassName + " can not initial.", ex);
        }
        log.warn("[ContextLoadFilterValve] can not initial filter, name is " + this.filterClassName);
      }
    }
    super.startInternal();
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    if (this.filter != null) {
      this.filter.destroy();
    }
    super.stopInternal();
  }
  
  public void invoke(Request request, Response response)
    throws IOException, ServletException
  {
    if (this.filter != null) {
      this.filter.doFilter(request, response, this.filterChainAdapter);
    } else {
      getNext().invoke(request, response);
    }
  }
  
  public void setFilterClassName(String filterClassName)
  {
    this.filterClassName = filterClassName;
  }
  
  public void setFilter(Filter filter)
  {
    this.filter = filter;
  }
  
  public void setInitConfig(String initConfig)
  {
    this.initConfig = initConfig;
  }
  
  public void setFailedOnStart(String failedOnStart)
  {
    this.failedOnStart = Boolean.valueOf(failedOnStart).booleanValue();
  }
  
  private class FilterChainAdapter
    implements FilterChain
  {
    private Valve nextValve;
    
    public FilterChainAdapter(Valve nextValve)
    {
      this.nextValve = nextValve;
    }
    
    public void doFilter(ServletRequest request, ServletResponse response)
      throws IOException, ServletException
    {
      this.nextValve.invoke((Request)request, (Response)response);
    }
  }
  
  private final boolean contains(Class<?>[] sources, Class<?> target)
  {
    boolean result = false;
    if ((sources != null) && (target != null)) {
      for (Class<?> source : sources) {
        if (source == target)
        {
          result = true;
          break;
        }
      }
    }
    return result;
  }
  
  private class StringProperties
  {
    private String source;
    private String mainSplit;
    private String subSplit;
    private Map<String, String> configMap;
    
    public StringProperties(String source, String mainSplit, String subSplit)
    {
      this.source = source;
      this.mainSplit = mainSplit;
      this.subSplit = subSplit;
      parse();
    }
    
    public String getAttribute(String key)
    {
      if (this.configMap != null) {
        return (String)this.configMap.get(key);
      }
      return null;
    }
    
    public Set<String> getKeys()
    {
      if (this.configMap != null) {
        return this.configMap.keySet();
      }
      return null;
    }
    
    private void parse()
    {
      if ((this.source == null) || (this.mainSplit == null) || (this.subSplit == null)) {
        return;
      }
      String[] mainRows = this.source.split(this.mainSplit);
      if (mainRows != null)
      {
        this.configMap = new HashMap(mainRows.length);
        for (String mainRow : mainRows) {
          if (mainRow != null)
          {
            String[] subRows = mainRow.split(this.subSplit);
            if ((subRows != null) && (subRows.length >= 2)) {
              this.configMap.put(subRows[0], subRows[1]);
            }
          }
        }
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\valves\ContextLoadFilterValve.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */