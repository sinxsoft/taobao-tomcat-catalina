package org.apache.naming.factory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.EjbRef;

public class EjbFactory
  implements ObjectFactory
{
  public EjbFactory() {}
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws Exception
  {
    if ((obj instanceof EjbRef))
    {
      Reference ref = (Reference)obj;
      
      RefAddr linkRefAddr = ref.get("link");
      if (linkRefAddr != null)
      {
        String ejbLink = linkRefAddr.getContent().toString();
        Object beanObj = new InitialContext().lookup(ejbLink);
        
        return beanObj;
      }
      ObjectFactory factory = null;
      RefAddr factoryRefAddr = ref.get("factory");
      if (factoryRefAddr != null)
      {
        String factoryClassName = factoryRefAddr.getContent().toString();
        
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        
        Class<?> factoryClass = null;
        if (tcl != null) {
          try
          {
            factoryClass = tcl.loadClass(factoryClassName);
          }
          catch (ClassNotFoundException e)
          {
            NamingException ex = new NamingException("Could not load resource factory class");
            
            ex.initCause(e);
            throw ex;
          }
        } else {
          try
          {
            factoryClass = Class.forName(factoryClassName);
          }
          catch (ClassNotFoundException e)
          {
            NamingException ex = new NamingException("Could not load resource factory class");
            
            ex.initCause(e);
            throw ex;
          }
        }
        if (factoryClass != null) {
          try
          {
            factory = (ObjectFactory)factoryClass.newInstance();
          }
          catch (Throwable t)
          {
            NamingException ex = new NamingException("Could not load resource factory class");
            
            ex.initCause(t);
            throw ex;
          }
        }
      }
      else
      {
        String javaxEjbFactoryClassName = System.getProperty("javax.ejb.Factory", "org.apache.naming.factory.OpenEjbFactory");
        try
        {
          factory = (ObjectFactory)Class.forName(javaxEjbFactoryClassName).newInstance();
        }
        catch (Throwable t)
        {
          if ((t instanceof NamingException)) {
            throw ((NamingException)t);
          }
          NamingException ex = new NamingException("Could not create resource factory instance");
          
          ex.initCause(t);
          throw ex;
        }
      }
      if (factory != null) {
        return factory.getObjectInstance(obj, name, nameCtx, environment);
      }
      throw new NamingException("Cannot create resource instance");
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\EjbFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */