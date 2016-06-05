package org.apache.catalina;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

public abstract interface Wrapper
  extends Container
{
  public static final String ADD_MAPPING_EVENT = "addMapping";
  public static final String REMOVE_MAPPING_EVENT = "removeMapping";
  
  public abstract long getAvailable();
  
  public abstract void setAvailable(long paramLong);
  
  public abstract int getLoadOnStartup();
  
  public abstract void setLoadOnStartup(int paramInt);
  
  public abstract String getRunAs();
  
  public abstract void setRunAs(String paramString);
  
  public abstract String getServletClass();
  
  public abstract void setServletClass(String paramString);
  
  public abstract String[] getServletMethods()
    throws ServletException;
  
  public abstract boolean isUnavailable();
  
  public abstract Servlet getServlet();
  
  public abstract void setServlet(Servlet paramServlet);
  
  public abstract void addInitParameter(String paramString1, String paramString2);
  
  public abstract void addInstanceListener(InstanceListener paramInstanceListener);
  
  public abstract void addMapping(String paramString);
  
  public abstract void addSecurityReference(String paramString1, String paramString2);
  
  public abstract Servlet allocate()
    throws ServletException;
  
  public abstract void deallocate(Servlet paramServlet)
    throws ServletException;
  
  public abstract String findInitParameter(String paramString);
  
  public abstract String[] findInitParameters();
  
  public abstract String[] findMappings();
  
  public abstract String findSecurityReference(String paramString);
  
  public abstract String[] findSecurityReferences();
  
  public abstract void incrementErrorCount();
  
  public abstract void load()
    throws ServletException;
  
  public abstract void removeInitParameter(String paramString);
  
  public abstract void removeInstanceListener(InstanceListener paramInstanceListener);
  
  public abstract void removeMapping(String paramString);
  
  public abstract void removeSecurityReference(String paramString);
  
  public abstract void unavailable(UnavailableException paramUnavailableException);
  
  public abstract void unload()
    throws ServletException;
  
  public abstract MultipartConfigElement getMultipartConfigElement();
  
  public abstract void setMultipartConfigElement(MultipartConfigElement paramMultipartConfigElement);
  
  public abstract boolean isAsyncSupported();
  
  public abstract void setAsyncSupported(boolean paramBoolean);
  
  public abstract boolean isEnabled();
  
  public abstract void setEnabled(boolean paramBoolean);
  
  public abstract void setServletSecurityAnnotationScanRequired(boolean paramBoolean);
  
  public abstract void servletSecurityAnnotationScan()
    throws ServletException;
  
  public abstract boolean isOverridable();
  
  public abstract void setOverridable(boolean paramBoolean);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Wrapper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */