package org.apache.naming.resources;

import java.util.Enumeration;
import java.util.Vector;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class RecyclableNamingEnumeration<E>
  implements NamingEnumeration<E>
{
  protected Vector<E> entries;
  protected Enumeration<E> enumeration;
  
  public RecyclableNamingEnumeration(Vector<E> entries)
  {
    this.entries = entries;
    recycle();
  }
  
  public E next()
    throws NamingException
  {
    return (E)nextElement();
  }
  
  public boolean hasMore()
    throws NamingException
  {
    return this.enumeration.hasMoreElements();
  }
  
  public void close()
    throws NamingException
  {}
  
  public boolean hasMoreElements()
  {
    return this.enumeration.hasMoreElements();
  }
  
  public E nextElement()
  {
    return (E)this.enumeration.nextElement();
  }
  
  void recycle()
  {
    this.enumeration = this.entries.elements();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\RecyclableNamingEnumeration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */