package org.apache.catalina;

import java.util.EventObject;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public final class InstanceEvent
  extends EventObject
{
  private static final long serialVersionUID = 1L;
  public static final String BEFORE_INIT_EVENT = "beforeInit";
  public static final String AFTER_INIT_EVENT = "afterInit";
  public static final String BEFORE_SERVICE_EVENT = "beforeService";
  public static final String AFTER_SERVICE_EVENT = "afterService";
  public static final String BEFORE_DESTROY_EVENT = "beforeDestroy";
  public static final String AFTER_DESTROY_EVENT = "afterDestroy";
  public static final String BEFORE_DISPATCH_EVENT = "beforeDispatch";
  public static final String AFTER_DISPATCH_EVENT = "afterDispatch";
  public static final String BEFORE_FILTER_EVENT = "beforeFilter";
  public static final String AFTER_FILTER_EVENT = "afterFilter";
  
  public InstanceEvent(Wrapper wrapper, Filter filter, String type)
  {
    super(wrapper);
    this.filter = filter;
    this.servlet = null;
    this.type = type;
  }
  
  public InstanceEvent(Wrapper wrapper, Filter filter, String type, Throwable exception)
  {
    super(wrapper);
    this.filter = filter;
    this.servlet = null;
    this.type = type;
    this.exception = exception;
  }
  
  public InstanceEvent(Wrapper wrapper, Filter filter, String type, ServletRequest request, ServletResponse response)
  {
    super(wrapper);
    this.filter = filter;
    this.servlet = null;
    this.type = type;
    this.request = request;
    this.response = response;
  }
  
  public InstanceEvent(Wrapper wrapper, Filter filter, String type, ServletRequest request, ServletResponse response, Throwable exception)
  {
    super(wrapper);
    this.filter = filter;
    this.servlet = null;
    this.type = type;
    this.request = request;
    this.response = response;
    this.exception = exception;
  }
  
  public InstanceEvent(Wrapper wrapper, Servlet servlet, String type)
  {
    super(wrapper);
    this.filter = null;
    this.servlet = servlet;
    this.type = type;
  }
  
  public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, Throwable exception)
  {
    super(wrapper);
    this.filter = null;
    this.servlet = servlet;
    this.type = type;
    this.exception = exception;
  }
  
  public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, ServletRequest request, ServletResponse response)
  {
    super(wrapper);
    this.filter = null;
    this.servlet = servlet;
    this.type = type;
    this.request = request;
    this.response = response;
  }
  
  public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, ServletRequest request, ServletResponse response, Throwable exception)
  {
    super(wrapper);
    this.filter = null;
    this.servlet = servlet;
    this.type = type;
    this.request = request;
    this.response = response;
    this.exception = exception;
  }
  
  private Throwable exception = null;
  private transient Filter filter = null;
  private transient ServletRequest request = null;
  private transient ServletResponse response = null;
  private transient Servlet servlet = null;
  private String type = null;
  
  public Throwable getException()
  {
    return this.exception;
  }
  
  public Filter getFilter()
  {
    return this.filter;
  }
  
  public ServletRequest getRequest()
  {
    return this.request;
  }
  
  public ServletResponse getResponse()
  {
    return this.response;
  }
  
  public Servlet getServlet()
  {
    return this.servlet;
  }
  
  public String getType()
  {
    return this.type;
  }
  
  public Wrapper getWrapper()
  {
    return (Wrapper)getSource();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\InstanceEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */