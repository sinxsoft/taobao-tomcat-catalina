package org.apache.naming.factory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.ResourceRef;

public class ResourceFactory
  implements ObjectFactory
{
  public ResourceFactory() {}
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws Exception
  {
    if ((obj instanceof ResourceRef))
    {
      Reference ref = (Reference)obj;
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
          catch (Exception e)
          {
            if ((e instanceof NamingException)) {
              throw ((NamingException)e);
            }
            NamingException ex = new NamingException("Could not create resource factory instance");
            
            ex.initCause(e);
            throw ex;
          }
        }
      }
      else if (ref.getClassName().equals("javax.sql.DataSource"))
      {
        String javaxSqlDataSourceFactoryClassName = System.getProperty("javax.sql.DataSource.Factory", "org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory");
        try
        {
          factory = (ObjectFactory)Class.forName(javaxSqlDataSourceFactoryClassName).newInstance();
        }
        catch (Exception e)
        {
          NamingException ex = new NamingException("Could not create resource factory instance");
          
          ex.initCause(e);
          throw ex;
        }
      }
      else if (ref.getClassName().equals("javax.mail.Session"))
      {
        String javaxMailSessionFactoryClassName = System.getProperty("javax.mail.Session.Factory", "org.apache.naming.factory.MailSessionFactory");
        try
        {
          factory = (ObjectFactory)Class.forName(javaxMailSessionFactoryClassName).newInstance();
        }
        catch (Throwable t)
        {
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


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\ResourceFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */