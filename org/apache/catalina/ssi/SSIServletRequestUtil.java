package org.apache.catalina.ssi;

import javax.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.RequestUtil;

public class SSIServletRequestUtil
{
  public SSIServletRequestUtil() {}
  
  public static String getRelativePath(HttpServletRequest request)
  {
    if (request.getAttribute("javax.servlet.include.request_uri") != null)
    {
      String result = (String)request.getAttribute("javax.servlet.include.path_info");
      if (result == null) {
        result = (String)request.getAttribute("javax.servlet.include.servlet_path");
      }
      if ((result == null) || (result.equals(""))) {
        result = "/";
      }
      return result;
    }
    String result = request.getPathInfo();
    if (result == null) {
      result = request.getServletPath();
    }
    if ((result == null) || (result.equals(""))) {
      result = "/";
    }
    return RequestUtil.normalize(result);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIServletRequestUtil.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */