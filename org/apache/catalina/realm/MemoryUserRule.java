package org.apache.catalina.realm;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class MemoryUserRule
  extends Rule
{
  public MemoryUserRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    String username = attributes.getValue("username");
    if (username == null) {
      username = attributes.getValue("name");
    }
    String password = attributes.getValue("password");
    String roles = attributes.getValue("roles");
    
    MemoryRealm realm = (MemoryRealm)this.digester.peek(this.digester.getCount() - 1);
    
    realm.addUser(username, password, roles);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\MemoryUserRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */