package org.apache.naming;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

public class NameParserImpl
  implements NameParser
{
  public NameParserImpl() {}
  
  public Name parse(String name)
    throws NamingException
  {
    return new CompositeName(name);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\NameParserImpl.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */