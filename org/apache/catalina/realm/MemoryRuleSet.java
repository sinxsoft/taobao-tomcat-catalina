package org.apache.catalina.realm;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class MemoryRuleSet
  extends RuleSetBase
{
  protected String prefix = null;
  
  public MemoryRuleSet()
  {
    this("tomcat-users/");
  }
  
  public MemoryRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public void addRuleInstances(Digester digester)
  {
    digester.addRule(this.prefix + "user", new MemoryUserRule());
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\MemoryRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */