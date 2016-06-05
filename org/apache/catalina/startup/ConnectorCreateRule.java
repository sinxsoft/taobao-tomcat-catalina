package org.apache.catalina.startup;

import java.lang.reflect.Method;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public class ConnectorCreateRule
  extends Rule
{
  private static final Log log = LogFactory.getLog(ConnectorCreateRule.class);
  
  public ConnectorCreateRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    Service svc = (Service)this.digester.peek();
    org.apache.catalina.Executor ex = null;
    if (attributes.getValue("executor") != null) {
      ex = svc.getExecutor(attributes.getValue("executor"));
    }
    Connector con = new Connector(attributes.getValue("protocol"));
    if (ex != null) {
      _setExecutor(con, ex);
    }
    this.digester.push(con);
  }
  
  public void _setExecutor(Connector con, org.apache.catalina.Executor ex)
    throws Exception
  {
    Method m = IntrospectionUtils.findMethod(con.getProtocolHandler().getClass(), "setExecutor", new Class[] { java.util.concurrent.Executor.class });
    if (m != null) {
      m.invoke(con.getProtocolHandler(), new Object[] { ex });
    } else {
      log.warn("Connector [" + con + "] does not support external executors. Method setExecutor(java.util.concurrent.Executor) not found.");
    }
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    this.digester.pop();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\ConnectorCreateRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */