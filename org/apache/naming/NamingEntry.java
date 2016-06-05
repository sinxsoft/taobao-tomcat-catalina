package org.apache.naming;

public class NamingEntry
{
  public static final int ENTRY = 0;
  public static final int LINK_REF = 1;
  public static final int REFERENCE = 2;
  public static final int CONTEXT = 10;
  public int type;
  public String name;
  public Object value;
  
  public NamingEntry(String name, Object value, int type)
  {
    this.name = name;
    this.value = value;
    this.type = type;
  }
  
  public boolean equals(Object obj)
  {
    if ((obj instanceof NamingEntry)) {
      return this.name.equals(((NamingEntry)obj).name);
    }
    return false;
  }
  
  public int hashCode()
  {
    return this.name.hashCode();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\NamingEntry.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */