package org.apache.catalina.deploy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import org.apache.tomcat.util.res.StringManager;

public class FilterDef
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.deploy");
  private String description = null;
  
  public FilterDef() {}
  
  public String getDescription()
  {
    return this.description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  private String displayName = null;
  
  public String getDisplayName()
  {
    return this.displayName;
  }
  
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }
  
  private transient Filter filter = null;
  
  public Filter getFilter()
  {
    return this.filter;
  }
  
  public void setFilter(Filter filter)
  {
    this.filter = filter;
  }
  
  private String filterClass = null;
  
  public String getFilterClass()
  {
    return this.filterClass;
  }
  
  public void setFilterClass(String filterClass)
  {
    this.filterClass = filterClass;
  }
  
  private String filterName = null;
  
  public String getFilterName()
  {
    return this.filterName;
  }
  
  public void setFilterName(String filterName)
  {
    if ((filterName == null) || (filterName.equals(""))) {
      throw new IllegalArgumentException(sm.getString("filterDef.invalidFilterName", new Object[] { filterName }));
    }
    this.filterName = filterName;
  }
  
  private String largeIcon = null;
  
  public String getLargeIcon()
  {
    return this.largeIcon;
  }
  
  public void setLargeIcon(String largeIcon)
  {
    this.largeIcon = largeIcon;
  }
  
  private Map<String, String> parameters = new HashMap();
  
  public Map<String, String> getParameterMap()
  {
    return this.parameters;
  }
  
  private String smallIcon = null;
  
  public String getSmallIcon()
  {
    return this.smallIcon;
  }
  
  public void setSmallIcon(String smallIcon)
  {
    this.smallIcon = smallIcon;
  }
  
  private String asyncSupported = null;
  
  public String getAsyncSupported()
  {
    return this.asyncSupported;
  }
  
  public void setAsyncSupported(String asyncSupported)
  {
    this.asyncSupported = asyncSupported;
  }
  
  public void addInitParameter(String name, String value)
  {
    if (this.parameters.containsKey(name)) {
      return;
    }
    this.parameters.put(name, value);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("FilterDef[");
    sb.append("filterName=");
    sb.append(this.filterName);
    sb.append(", filterClass=");
    sb.append(this.filterClass);
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\FilterDef.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */