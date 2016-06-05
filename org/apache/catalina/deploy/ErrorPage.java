package org.apache.catalina.deploy;

import java.io.Serializable;
import org.apache.catalina.util.RequestUtil;

public class ErrorPage
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private int errorCode = 0;
  private String exceptionType = null;
  private String location = null;
  
  public ErrorPage() {}
  
  public int getErrorCode()
  {
    return this.errorCode;
  }
  
  public void setErrorCode(int errorCode)
  {
    this.errorCode = errorCode;
  }
  
  public void setErrorCode(String errorCode)
  {
    try
    {
      this.errorCode = Integer.parseInt(errorCode);
    }
    catch (NumberFormatException nfe)
    {
      this.errorCode = 0;
    }
  }
  
  public String getExceptionType()
  {
    return this.exceptionType;
  }
  
  public void setExceptionType(String exceptionType)
  {
    this.exceptionType = exceptionType;
  }
  
  public String getLocation()
  {
    return this.location;
  }
  
  public void setLocation(String location)
  {
    this.location = RequestUtil.URLDecode(location);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ErrorPage[");
    if (this.exceptionType == null)
    {
      sb.append("errorCode=");
      sb.append(this.errorCode);
    }
    else
    {
      sb.append("exceptionType=");
      sb.append(this.exceptionType);
    }
    sb.append(", location=");
    sb.append(this.location);
    sb.append("]");
    return sb.toString();
  }
  
  public String getName()
  {
    if (this.exceptionType == null) {
      return Integer.toString(this.errorCode);
    }
    return this.exceptionType;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\ErrorPage.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */