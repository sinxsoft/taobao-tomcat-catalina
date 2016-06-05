package org.apache.naming.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.sql.DataSource;

public class DataSourceLinkFactory
  extends ResourceLinkFactory
{
  public DataSourceLinkFactory() {}
  
  public static void setGlobalContext(Context newGlobalContext)
  {
    ResourceLinkFactory.setGlobalContext(newGlobalContext);
  }
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws NamingException
  {
    Object result = super.getObjectInstance(obj, name, nameCtx, environment);
    if (result != null)
    {
      Reference ref = (Reference)obj;
      RefAddr userAttr = ref.get("username");
      RefAddr passAttr = ref.get("password");
      if ((userAttr.getContent() != null) && (passAttr.getContent() != null)) {
        result = wrapDataSource(result, userAttr.getContent().toString(), passAttr.getContent().toString());
      }
    }
    return result;
  }
  
  protected Object wrapDataSource(Object datasource, String username, String password)
    throws NamingException
  {
    try
    {
      Class<?> proxyClass = Proxy.getProxyClass(datasource.getClass().getClassLoader(), datasource.getClass().getInterfaces());
      Constructor<?> proxyConstructor = proxyClass.getConstructor(new Class[] { InvocationHandler.class });
      DataSourceHandler handler = new DataSourceHandler((DataSource)datasource, username, password);
      return proxyConstructor.newInstance(new Object[] { handler });
    }
    catch (Exception x)
    {
      if ((x instanceof InvocationTargetException))
      {
        Throwable cause = x.getCause();
        if ((cause instanceof ThreadDeath)) {
          throw ((ThreadDeath)cause);
        }
        if ((cause instanceof VirtualMachineError)) {
          throw ((VirtualMachineError)cause);
        }
        if ((cause instanceof Exception)) {
          x = (Exception)cause;
        }
      }
      if ((x instanceof NamingException)) {
        throw ((NamingException)x);
      }
      NamingException nx = new NamingException(x.getMessage());
      nx.initCause(x);
      throw nx;
    }
  }
  
  public static class DataSourceHandler
    implements InvocationHandler
  {
    private final DataSource ds;
    private final String username;
    private final String password;
    private final Method getConnection;
    
    public DataSourceHandler(DataSource ds, String username, String password)
      throws Exception
    {
      this.ds = ds;
      this.username = username;
      this.password = password;
      this.getConnection = ds.getClass().getMethod("getConnection", new Class[] { String.class, String.class });
    }
    
    public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable
    {
      if (("getConnection".equals(method.getName())) && ((args == null) || (args.length == 0)))
      {
        args = new String[] { this.username, this.password };
        method = this.getConnection;
      }
      else if ("unwrap".equals(method.getName()))
      {
        return unwrap((Class)args[0]);
      }
      try
      {
        return method.invoke(this.ds, args);
      }
      catch (Throwable t)
      {
        if (((t instanceof InvocationTargetException)) && (t.getCause() != null)) {
          throw t.getCause();
        }
        throw t;
      }
    }
    
    public Object unwrap(Class<?> iface)
      throws SQLException
    {
      if (iface == DataSource.class) {
        return this.ds;
      }
      throw new SQLException("Not a wrapper of " + iface.getName());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\DataSourceLinkFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */