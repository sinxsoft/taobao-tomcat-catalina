package org.apache.catalina.core;

import java.util.Locale;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

class ApplicationResponse
  extends ServletResponseWrapper
{
  @Deprecated
  public ApplicationResponse(ServletResponse response)
  {
    this(response, false);
  }
  
  public ApplicationResponse(ServletResponse response, boolean included)
  {
    super(response);
    setIncluded(included);
  }
  
  protected boolean included = false;
  
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
  
  public void setResponse(ServletResponse response)
  {
    super.setResponse(response);
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
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationResponse.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */