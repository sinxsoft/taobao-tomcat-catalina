package org.apache.naming.factory;

import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.EjbRef;

public class OpenEjbFactory
  implements ObjectFactory
{
  protected static final String DEFAULT_OPENEJB_FACTORY = "org.openejb.client.LocalInitialContextFactory";
  
  public OpenEjbFactory() {}
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws Exception
  {
    Object beanObj = null;
    if ((obj instanceof EjbRef))
    {
      Reference ref = (Reference)obj;
      
      String factory = "org.openejb.client.LocalInitialContextFactory";
      RefAddr factoryRefAddr = ref.get("openejb.factory");
      if (factoryRefAddr != null) {
        factory = factoryRefAddr.getContent().toString();
      }
      Properties env = new Properties();
      env.put("java.naming.factory.initial", factory);
      
      RefAddr linkRefAddr = ref.get("openejb.link");
      if (linkRefAddr != null)
      {
        String ejbLink = linkRefAddr.getContent().toString();
        beanObj = new InitialContext(env).lookup(ejbLink);
      }
    }
    return beanObj;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\OpenEjbFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */