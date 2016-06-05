package org.apache.catalina.core;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.security.SecurityUtil;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.modeler.Util;
import org.apache.tomcat.util.res.StringManager;

public final class ApplicationFilterConfig
  implements FilterConfig, Serializable
{
  private static final long serialVersionUID = 1L;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  private static final Log log = LogFactory.getLog(ApplicationFilterConfig.class);
  private static final List<String> emptyString = Collections.emptyList();
  
  ApplicationFilterConfig(Context context, FilterDef filterDef)
    throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException, ServletException, InvocationTargetException, NamingException
  {
    this.context = context;
    this.filterDef = filterDef;
    if (filterDef.getFilter() == null)
    {
      getFilter();
    }
    else
    {
      this.filter = filterDef.getFilter();
      getInstanceManager().newInstance(this.filter);
      initFilter();
    }
  }
  
  private transient Context context = null;
  private transient Filter filter = null;
  private final FilterDef filterDef;
  private transient InstanceManager instanceManager;
  private ObjectName oname;
  
  public String getFilterName()
  {
    return this.filterDef.getFilterName();
  }
  
  public String getFilterClass()
  {
    return this.filterDef.getFilterClass();
  }
  
  public String getInitParameter(String name)
  {
    Map<String, String> map = this.filterDef.getParameterMap();
    if (map == null) {
      return null;
    }
    return (String)map.get(name);
  }
  
  public Enumeration<String> getInitParameterNames()
  {
    Map<String, String> map = this.filterDef.getParameterMap();
    if (map == null) {
      return Collections.enumeration(emptyString);
    }
    return Collections.enumeration(map.keySet());
  }
  
  public ServletContext getServletContext()
  {
    return this.context.getServletContext();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ApplicationFilterConfig[");
    sb.append("name=");
    sb.append(this.filterDef.getFilterName());
    sb.append(", filterClass=");
    sb.append(this.filterDef.getFilterClass());
    sb.append("]");
    return sb.toString();
  }
  
  public Map<String, String> getFilterInitParameterMap()
  {
    return Collections.unmodifiableMap(this.filterDef.getParameterMap());
  }
  
  Filter getFilter()
    throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException, ServletException, InvocationTargetException, NamingException
  {
    if (this.filter != null) {
      return this.filter;
    }
    String filterClass = this.filterDef.getFilterClass();
    this.filter = ((Filter)getInstanceManager().newInstance(filterClass));
    
    initFilter();
    
    return this.filter;
  }
  
  private void initFilter()
    throws ServletException
  {
    if (((this.context instanceof StandardContext)) && (this.context.getSwallowOutput())) {
      try
      {
        SystemLogHandler.startCapture();
        this.filter.init(this);
      }
      finally
      {
        String capturedlog;
        capturedlog = SystemLogHandler.stopCapture();
        if ((capturedlog != null) && (capturedlog.length() > 0)) {
          getServletContext().log(capturedlog);
        }
      }
    } else {
      this.filter.init(this);
    }
    registerJMX();
  }
  
  FilterDef getFilterDef()
  {
    return this.filterDef;
  }
  
  void release()
  {
    unregisterJMX();
    if (this.filter != null)
    {
      try
      {
        if (Globals.IS_SECURITY_ENABLED) {
          try
          {
            SecurityUtil.doAsPrivilege("destroy", this.filter);
          }
          finally
          {
            SecurityUtil.remove(this.filter);
          }
        } else {
          this.filter.destroy();
        }
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        this.context.getLogger().error(sm.getString("applicationFilterConfig.release", new Object[] { this.filterDef.getFilterName(), this.filterDef.getFilterClass() }), t);
      }
      if (!this.context.getIgnoreAnnotations()) {
        try
        {
          ((StandardContext)this.context).getInstanceManager().destroyInstance(this.filter);
        }
        catch (Exception e)
        {
          Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
          
          ExceptionUtils.handleThrowable(t);
          this.context.getLogger().error("ApplicationFilterConfig.preDestroy", t);
        }
      }
    }
    this.filter = null;
  }
  
  private InstanceManager getInstanceManager()
  {
    if (this.instanceManager == null) {
      if ((this.context instanceof StandardContext)) {
        this.instanceManager = ((StandardContext)this.context).getInstanceManager();
      } else {
        this.instanceManager = new DefaultInstanceManager(null, new HashMap(), this.context, getClass().getClassLoader());
      }
    }
    return this.instanceManager;
  }
  
  private void registerJMX()
  {
    String parentName = this.context.getName();
    if (!parentName.startsWith("/")) {
      parentName = "/" + parentName;
    }
    String hostName = this.context.getParent().getName();
    hostName = hostName == null ? "DEFAULT" : hostName;
    
    String domain = this.context.getParent().getParent().getName();
    
    String webMod = "//" + hostName + parentName;
    String onameStr = null;
    String filterName = this.filterDef.getFilterName();
    if (Util.objectNameValueNeedsQuote(filterName)) {
      filterName = ObjectName.quote(filterName);
    }
    if ((this.context instanceof StandardContext))
    {
      StandardContext standardContext = (StandardContext)this.context;
      onameStr = domain + ":j2eeType=Filter,name=" + filterName + ",WebModule=" + webMod + ",J2EEApplication=" + standardContext.getJ2EEApplication() + ",J2EEServer=" + standardContext.getJ2EEServer();
    }
    else
    {
      onameStr = domain + ":j2eeType=Filter,name=" + filterName + ",WebModule=" + webMod;
    }
    try
    {
      this.oname = new ObjectName(onameStr);
      Registry.getRegistry(null, null).registerComponent(this, this.oname, null);
    }
    catch (Exception ex)
    {
      log.info(sm.getString("applicationFilterConfig.jmxRegisterFail", new Object[] { getFilterClass(), getFilterName() }), ex);
    }
  }
  
  private void unregisterJMX()
  {
    if (this.oname != null) {
      try
      {
        Registry.getRegistry(null, null).unregisterComponent(this.oname);
        if (log.isDebugEnabled()) {
          log.debug(sm.getString("applicationFilterConfig.jmxUnregister", new Object[] { getFilterClass(), getFilterName() }));
        }
      }
      catch (Exception ex)
      {
        log.error(sm.getString("applicationFilterConfig.jmxUnregisterFail", new Object[] { getFilterClass(), getFilterName() }), ex);
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationFilterConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */