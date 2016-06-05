package org.apache.catalina.core;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class JasperListener
  implements LifecycleListener
{
  private static final Log log = LogFactory.getLog(JasperListener.class);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.core");
  
  public JasperListener() {}
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    if ("before_init".equals(event.getType())) {
      try
      {
        Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, getClass().getClassLoader());
      }
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
        
        log.warn("Couldn't initialize Jasper", t);
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\JasperListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */