package org.apache.catalina.core;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class ApplicationHttpResponse
  extends HttpServletResponseWrapper
{
  @Deprecated
  public ApplicationHttpResponse(HttpServletResponse response)
  {
    this(response, false);
  }
  
  public ApplicationHttpResponse(HttpServletResponse response, boolean included)
  {
    super(response);
    setIncluded(included);
  }
  
  protected boolean included = false;
  protected static final String info = "org.apache.catalina.core.ApplicationHttpResponse/1.0";
  
  public void reset()
  {
    if ((!this.included) || (getResponse().isCommitted())) {
      getResponse().reset();
    }
  }
  
  public void setContentLength(int len)
  {
    if (!this.included) {
      getResponse().setContentLength(len);
    }
  }
  
  public void setContentType(String type)
  {
    if (!this.included) {
      getResponse().setContentType(type);
    }
  }
  
  public void setLocale(Locale loc)
  {
    if (!this.included) {
      getResponse().setLocale(loc);
    }
  }
  
  public void setBufferSize(int size)
  {
    if (!this.included) {
      getResponse().setBufferSize(size);
    }
  }
  
  public void addCookie(Cookie cookie)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).addCookie(cookie);
    }
  }
  
  public void addDateHeader(String name, long value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).addDateHeader(name, value);
    }
  }
  
  public void addHeader(String name, String value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).addHeader(name, value);
    }
  }
  
  public void addIntHeader(String name, int value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).addIntHeader(name, value);
    }
  }
  
  public void sendError(int sc)
    throws IOException
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).sendError(sc);
    }
  }
  
  public void sendError(int sc, String msg)
    throws IOException
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).sendError(sc, msg);
    }
  }
  
  public void sendRedirect(String location)
    throws IOException
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).sendRedirect(location);
    }
  }
  
  public void setDateHeader(String name, long value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).setDateHeader(name, value);
    }
  }
  
  public void setHeader(String name, String value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).setHeader(name, value);
    }
  }
  
  public void setIntHeader(String name, int value)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).setIntHeader(name, value);
    }
  }
  
  public void setStatus(int sc)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).setStatus(sc);
    }
  }
  
  @Deprecated
  public void setStatus(int sc, String msg)
  {
    if (!this.included) {
      ((HttpServletResponse)getResponse()).setStatus(sc, msg);
    }
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.core.ApplicationHttpResponse/1.0";
  }
  
  @Deprecated
  boolean isIncluded()
  {
    return this.included;
  }
  
  void setIncluded(boolean included)
  {
    this.included = included;
  }
  
  void setResponse(HttpServletResponse response)
  {
    super.setResponse(response);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationHttpResponse.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */