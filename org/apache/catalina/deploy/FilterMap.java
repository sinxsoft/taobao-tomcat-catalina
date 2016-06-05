package org.apache.catalina.deploy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import javax.servlet.DispatcherType;
import org.apache.catalina.util.RequestUtil;

public class FilterMap
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  public static final int ERROR = 1;
  public static final int FORWARD = 2;
  public static final int INCLUDE = 4;
  public static final int REQUEST = 8;
  public static final int ASYNC = 16;
  private static final int NOT_SET = 0;
  private int dispatcherMapping = 0;
  private String filterName = null;
  
  public FilterMap() {}
  
  public String getFilterName()
  {
    return this.filterName;
  }
  
  public void setFilterName(String filterName)
  {
    this.filterName = filterName;
  }
  
  private String[] servletNames = new String[0];
  
  public String[] getServletNames()
  {
    if (this.matchAllServletNames) {
      return new String[0];
    }
    return this.servletNames;
  }
  
  public void addServletName(String servletName)
  {
    if ("*".equals(servletName))
    {
      this.matchAllServletNames = true;
    }
    else
    {
      String[] results = new String[this.servletNames.length + 1];
      System.arraycopy(this.servletNames, 0, results, 0, this.servletNames.length);
      results[this.servletNames.length] = servletName;
      this.servletNames = results;
    }
  }
  
  private boolean matchAllUrlPatterns = false;
  
  public boolean getMatchAllUrlPatterns()
  {
    return this.matchAllUrlPatterns;
  }
  
  private boolean matchAllServletNames = false;
  
  public boolean getMatchAllServletNames()
  {
    return this.matchAllServletNames;
  }
  
  private String[] urlPatterns = new String[0];
  
  public String[] getURLPatterns()
  {
    if (this.matchAllUrlPatterns) {
      return new String[0];
    }
    return this.urlPatterns;
  }
  
  public void addURLPattern(String urlPattern)
  {
    if ("*".equals(urlPattern))
    {
      this.matchAllUrlPatterns = true;
    }
    else
    {
      String[] results = new String[this.urlPatterns.length + 1];
      System.arraycopy(this.urlPatterns, 0, results, 0, this.urlPatterns.length);
      results[this.urlPatterns.length] = RequestUtil.URLDecode(urlPattern);
      this.urlPatterns = results;
    }
  }
  
  public void setDispatcher(String dispatcherString)
  {
    String dispatcher = dispatcherString.toUpperCase(Locale.ENGLISH);
    if (dispatcher.equals(DispatcherType.FORWARD.name())) {
      this.dispatcherMapping |= 0x2;
    } else if (dispatcher.equals(DispatcherType.INCLUDE.name())) {
      this.dispatcherMapping |= 0x4;
    } else if (dispatcher.equals(DispatcherType.REQUEST.name())) {
      this.dispatcherMapping |= 0x8;
    } else if (dispatcher.equals(DispatcherType.ERROR.name())) {
      this.dispatcherMapping |= 0x1;
    } else if (dispatcher.equals(DispatcherType.ASYNC.name())) {
      this.dispatcherMapping |= 0x10;
    }
  }
  
  public int getDispatcherMapping()
  {
    if (this.dispatcherMapping == 0) {
      return 8;
    }
    return this.dispatcherMapping;
  }
  
  public String[] getDispatcherNames()
  {
    ArrayList<String> result = new ArrayList();
    if ((this.dispatcherMapping & 0x2) > 0) {
      result.add(DispatcherType.FORWARD.name());
    }
    if ((this.dispatcherMapping & 0x4) > 0) {
      result.add(DispatcherType.INCLUDE.name());
    }
    if ((this.dispatcherMapping & 0x8) > 0) {
      result.add(DispatcherType.REQUEST.name());
    }
    if ((this.dispatcherMapping & 0x1) > 0) {
      result.add(DispatcherType.ERROR.name());
    }
    if ((this.dispatcherMapping & 0x10) > 0) {
      result.add(DispatcherType.ASYNC.name());
    }
    return (String[])result.toArray(new String[result.size()]);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("FilterMap[");
    sb.append("filterName=");
    sb.append(this.filterName);
    for (int i = 0; i < this.servletNames.length; i++)
    {
      sb.append(", servletName=");
      sb.append(this.servletNames[i]);
    }
    for (int i = 0; i < this.urlPatterns.length; i++)
    {
      sb.append(", urlPattern=");
      sb.append(this.urlPatterns[i]);
    }
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\FilterMap.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */