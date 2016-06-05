package org.apache.naming;

import java.util.Iterator;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class NamingContextEnumeration
  implements NamingEnumeration<NameClassPair>
{
  protected Iterator<NamingEntry> iterator;
  
  public NamingContextEnumeration(Iterator<NamingEntry> entries)
  {
    this.iterator = entries;
  }
  
  public NameClassPair next()
    throws NamingException
  {
    return nextElement();
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
  
  public NameClassPair nextElement()
  {
    NamingEntry entry = (NamingEntry)this.iterator.next();
    return new NameClassPair(entry.name, entry.value.getClass().getName());
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\NamingContextEnumeration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */