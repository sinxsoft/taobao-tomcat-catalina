package org.apache.catalina.core;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import org.apache.catalina.Context;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.ParameterMap;
import org.apache.tomcat.util.res.StringManager;

public class ApplicationFilterRegistration
  implements FilterRegistration.Dynamic
{
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private FilterDef filterDef;
  private Context context;
  
  public ApplicationFilterRegistration(FilterDef filterDef, Context context)
  {
    this.filterDef = filterDef;
    this.context = context;
  }
  
  public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames)
  {
    FilterMap filterMap = new FilterMap();
    
    filterMap.setFilterName(this.filterDef.getFilterName());
    if (dispatcherTypes != null) {
      for (DispatcherType dispatcherType : dispatcherTypes) {
        filterMap.setDispatcher(dispatcherType.name());
      }
    }
    if (servletNames != null)
    {
      for (String servletName : servletNames) {
        filterMap.addServletName(servletName);
      }
      if (isMatchAfter) {
        this.context.addFilterMap(filterMap);
      } else {
        this.context.addFilterMapBefore(filterMap);
      }
    }
  }
  
  public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns)
  {
    FilterMap filterMap = new FilterMap();
    
    filterMap.setFilterName(this.filterDef.getFilterName());
    if (dispatcherTypes != null) {
      for (DispatcherType dispatcherType : dispatcherTypes) {
        filterMap.setDispatcher(dispatcherType.name());
      }
    }
    if (urlPatterns != null)
    {
      for (String urlPattern : urlPatterns) {
        filterMap.addURLPattern(urlPattern);
      }
      if (isMatchAfter) {
        this.context.addFilterMap(filterMap);
      } else {
        this.context.addFilterMapBefore(filterMap);
      }
    }
  }
  
  public Collection<String> getServletNameMappings()
  {
    Collection<String> result = new HashSet();
    
    FilterMap[] filterMaps = this.context.findFilterMaps();
    for (FilterMap filterMap : filterMaps) {
      if (filterMap.getFilterName().equals(this.filterDef.getFilterName())) {
        for (String servletName : filterMap.getServletNames()) {
          result.add(servletName);
        }
      }
    }
    return result;
  }
  
  public Collection<String> getUrlPatternMappings()
  {
    Collection<String> result = new HashSet();
    
    FilterMap[] filterMaps = this.context.findFilterMaps();
    for (FilterMap filterMap : filterMaps) {
      if (filterMap.getFilterName().equals(this.filterDef.getFilterName())) {
        for (String urlPattern : filterMap.getURLPatterns()) {
          result.add(urlPattern);
        }
      }
    }
    return result;
  }
  
  public String getClassName()
  {
    return this.filterDef.getFilterClass();
  }
  
  public String getInitParameter(String name)
  {
    return (String)this.filterDef.getParameterMap().get(name);
  }
  
  public Map<String, String> getInitParameters()
  {
    ParameterMap<String, String> result = new ParameterMap();
    result.putAll(this.filterDef.getParameterMap());
    result.setLocked(true);
    return result;
  }
  
  public String getName()
  {
    return this.filterDef.getFilterName();
  }
  
  public boolean setInitParameter(String name, String value)
  {
    if ((name == null) || (value == null)) {
      throw new IllegalArgumentException(sm.getString("applicationFilterRegistration.nullInitParam", new Object[] { name, value }));
    }
    if (getInitParameter(name) != null) {
      return false;
    }
    this.filterDef.addInitParameter(name, value);
    
    return true;
  }
  
  public Set<String> setInitParameters(Map<String, String> initParameters)
  {
    Set<String> conflicts = new HashSet();
    for (Map.Entry<String, String> entry : initParameters.entrySet())
    {
      if ((entry.getKey() == null) || (entry.getValue() == null)) {
        throw new IllegalArgumentException(sm.getString("applicationFilterRegistration.nullInitParams", new Object[] { entry.getKey(), entry.getValue() }));
      }
      if (getInitParameter((String)entry.getKey()) != null) {
        conflicts.add(entry.getKey());
      }
    }
    for (Map.Entry<String, String> entry : initParameters.entrySet()) {
      setInitParameter((String)entry.getKey(), (String)entry.getValue());
    }
    return conflicts;
  }
  
  public void setAsyncSupported(boolean asyncSupported)
  {
    this.filterDef.setAsyncSupported(Boolean.valueOf(asyncSupported).toString());
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationFilterRegistration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */