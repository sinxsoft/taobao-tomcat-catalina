package org.apache.naming;

import java.util.Iterator;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class NamingContextBindingsEnumeration
  implements NamingEnumeration<Binding>
{
  protected Iterator<NamingEntry> iterator;
  private Context ctx;
  
  public NamingContextBindingsEnumeration(Iterator<NamingEntry> entries, Context ctx)
  {
    this.iterator = entries;
    this.ctx = ctx;
  }
  
  public Binding next()
    throws NamingException
  {
    return nextElementInternal();
  }
  
  public boolean hasMore()
    throws NamingException
  {
    return this.iterator.hasNext();
  }
  
  public void close()
    throws NamingException
  {}
  
  public boolean hasMoreElements()
  {
    return this.iterator.hasNext();
  }
  
  public Binding nextElement()
  {
    try
    {
      return nextElementInternal();
    }
    catch (NamingException e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  private Binding nextElementInternal()
    throws NamingException
  {
    NamingEntry entry = (NamingEntry)this.iterator.next();
    Object value;
    if ((entry.type == 2) || (entry.type == 1)) {
      try
      {
        value = this.ctx.lookup(new CompositeName(entry.name));
      }
      catch (NamingException e)
      {
        throw e;
      }
      catch (Exception e)
      {
        NamingException ne = new NamingException(e.getMessage());
        ne.initCause(e);
        throw ne;
      }
    } else {
      value = entry.value;
    }
    return new Binding(entry.name, value.getClass().getName(), value, true);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\NamingContextBindingsEnumeration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */