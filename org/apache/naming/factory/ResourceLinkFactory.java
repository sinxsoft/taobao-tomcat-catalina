package org.apache.naming.factory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.ResourceLinkRef;

public class ResourceLinkFactory
  implements ObjectFactory
{
  private static Context globalContext = null;
  
  public ResourceLinkFactory() {}
  
  public static void setGlobalContext(Context newGlobalContext)
  {
    globalContext = newGlobalContext;
  }
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws NamingException
  {
    if (!(obj instanceof ResourceLinkRef)) {
      return null;
    }
    Reference ref = (Reference)obj;
    
    String globalName = null;
    RefAddr refAddr = ref.get("globalName");
    if (refAddr != null)
    {
      globalName = refAddr.getContent().toString();
      Object result = null;
      result = globalContext.lookup(globalName);
      
      return result;
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\ResourceLinkFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */