package org.apache.catalina.core;

import java.util.Arrays;
import org.apache.catalina.AccessLog;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class AccessLogAdapter
  implements AccessLog
{
  private AccessLog[] logs;
  
  public AccessLogAdapter(AccessLog log)
  {
    if (log == null) {
      throw new NullPointerException();
    }
    this.logs = new AccessLog[] { log };
  }
  
  public void add(AccessLog log)
  {
    if (log == null) {
      throw new NullPointerException();
    }
    AccessLog[] newArray = (AccessLog[])Arrays.copyOf(this.logs, this.logs.length + 1);
    newArray[(newArray.length - 1)] = log;
    this.logs = newArray;
  }
  
  public void log(Request request, Response response, long time)
  {
    for (AccessLog log : this.logs) {
      log.log(request, response, time);
    }
  }
  
  public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {}
  
  public boolean getRequestAttributesEnabled()
  {
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\AccessLogAdapter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */