package org.apache.catalina.core;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;

class ApplicationRequest
  extends ServletRequestWrapper
{
  protected static final String[] specials = { "javax.servlet.include.request_uri", "javax.servlet.include.context_path", "javax.servlet.include.servlet_path", "javax.servlet.include.path_info", "javax.servlet.include.query_string", "javax.servlet.forward.request_uri", "javax.servlet.forward.context_path", "javax.servlet.forward.servlet_path", "javax.servlet.forward.path_info", "javax.servlet.forward.query_string" };
  
  public ApplicationRequest(ServletRequest request)
  {
    super(request);
    setRequest(request);
  }
  
  protected HashMap<String, Object> attributes = new HashMap();
  

  public Object getAttribute(String name)
  {
	     synchronized (attributes) {
	            return (attributes.get(name));
	        }
  }
  

  public Enumeration<String> getAttributeNames()
  {
	    synchronized (attributes) {
            return Collections.enumeration(attributes.keySet());
        }
  }
  
  public void removeAttribute(String name)
  {
    synchronized (this.attributes)
    {
      this.attributes.remove(name);
      if (!isSpecial(name)) {
        getRequest().removeAttribute(name);
      }
    }
  }
  
  public void setAttribute(String name, Object value)
  {
    synchronized (this.attributes)
    {
      this.attributes.put(name, value);
      if (!isSpecial(name)) {
        getRequest().setAttribute(name, value);
      }
    }
  }
  
  public void setRequest(ServletRequest request)
  {
    super.setRequest(request);
    synchronized (this.attributes)
    {
      this.attributes.clear();
      Enumeration<String> names = request.getAttributeNames();
      while (names.hasMoreElements())
      {
        String name = (String)names.nextElement();
        Object value = request.getAttribute(name);
        this.attributes.put(name, value);
      }
    }
  }
  
  protected boolean isSpecial(String name)
  {
    for (int i = 0; i < specials.length; i++) {
      if (specials[i].equals(name)) {
        return true;
      }
    }
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationRequest.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */