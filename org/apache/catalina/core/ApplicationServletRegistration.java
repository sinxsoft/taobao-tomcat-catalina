package org.apache.catalina.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.ParameterMap;
import org.apache.tomcat.util.res.StringManager;

public class ApplicationServletRegistration
  implements ServletRegistration.Dynamic
{
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private Wrapper wrapper;
  private Context context;
  
  public ApplicationServletRegistration(Wrapper wrapper, Context context)
  {
    this.wrapper = wrapper;
    this.context = context;
  }
  
  public String getClassName()
  {
    return this.wrapper.getServletClass();
  }
  
  public String getInitParameter(String name)
  {
    return this.wrapper.findInitParameter(name);
  }
  
  public Map<String, String> getInitParameters()
  {
    ParameterMap<String, String> result = new ParameterMap();
    
    String[] parameterNames = this.wrapper.findInitParameters();
    for (String parameterName : parameterNames) {
      result.put(parameterName, this.wrapper.findInitParameter(parameterName));
    }
    result.setLocked(true);
    return result;
  }
  
  public String getName()
  {
    return this.wrapper.getName();
  }
  
  public boolean setInitParameter(String name, String value)
  {
    if ((name == null) || (value == null)) {
      throw new IllegalArgumentException(sm.getString("applicationFilterRegistration.nullInitParam", new Object[] { name, value }));
    }
    if (getInitParameter(name) != null) {
      return false;
    }
    this.wrapper.addInitParameter(name, value);
    
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
    if (conflicts.isEmpty()) {
      for (Map.Entry<String, String> entry : initParameters.entrySet()) {
        setInitParameter((String)entry.getKey(), (String)entry.getValue());
      }
    }
    return conflicts;
  }
  
  public void setAsyncSupported(boolean asyncSupported)
  {
    this.wrapper.setAsyncSupported(asyncSupported);
  }
  
  public void setLoadOnStartup(int loadOnStartup)
  {
    this.wrapper.setLoadOnStartup(loadOnStartup);
  }
  
  public void setMultipartConfig(MultipartConfigElement multipartConfig)
  {
    this.wrapper.setMultipartConfigElement(multipartConfig);
  }
  
  public void setRunAsRole(String roleName)
  {
    this.wrapper.setRunAs(roleName);
  }
  
  public Set<String> setServletSecurity(ServletSecurityElement constraint)
  {
    if (constraint == null) {
      throw new IllegalArgumentException(sm.getString("applicationServletRegistration.setServletSecurity.iae", new Object[] { getName(), this.context.getName() }));
    }
    if (!this.context.getState().equals(LifecycleState.STARTING_PREP)) {
      throw new IllegalStateException(sm.getString("applicationServletRegistration.setServletSecurity.ise", new Object[] { getName(), this.context.getName() }));
    }
    return this.context.addServletSecurity(this, constraint);
  }
  
  public Set<String> addMapping(String... urlPatterns)
  {
    if (urlPatterns == null) {
      return Collections.emptySet();
    }
    Set<String> conflicts = new HashSet();
    for (String urlPattern : urlPatterns)
    {
      String wrapperName = this.context.findServletMapping(urlPattern);
      if (wrapperName != null)
      {
        Wrapper wrapper = (Wrapper)this.context.findChild(wrapperName);
        if (wrapper.isOverridable()) {
          this.context.removeServletMapping(urlPattern);
        } else {
          conflicts.add(urlPattern);
        }
      }
    }
    if (!conflicts.isEmpty()) {
      return conflicts;
    }
    for (String urlPattern : urlPatterns) {
      this.context.addServletMapping(urlPattern, this.wrapper.getName());
    }
    return Collections.emptySet();
  }
  
  public Collection<String> getMappings()
  {
    Set<String> result = new HashSet();
    String servletName = this.wrapper.getName();
    
    String[] urlPatterns = this.context.findServletMappings();
    for (String urlPattern : urlPatterns)
    {
      String name = this.context.findServletMapping(urlPattern);
      if (name.equals(servletName)) {
        result.add(urlPattern);
      }
    }
    return result;
  }
  
  public String getRunAsRole()
  {
    return this.wrapper.getRunAs();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationServletRegistration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */