package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class RealmRuleSet
  extends RuleSetBase
{
  private static final int MAX_NESTED_REALM_LEVELS = Integer.getInteger("org.apache.catalina.startup.RealmRuleSet.MAX_NESTED_REALM_LEVELS", 3).intValue();
  protected String prefix = null;
  
  public RealmRuleSet()
  {
    this("");
  }
  
  public RealmRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public void addRuleInstances(Digester digester)
  {
    String pattern = this.prefix;
    for (int i = 0; i < MAX_NESTED_REALM_LEVELS; i++)
    {
      if (i > 0) {
        pattern = pattern + "/";
      }
      pattern = pattern + "Realm";
      
      digester.addObjectCreate(pattern, null, "className");
      
      digester.addSetProperties(pattern);
      if (i == 0) {
        digester.addSetNext(pattern, "setRealm", "org.apache.catalina.Realm");
      } else {
        digester.addSetNext(pattern, "addRealm", "org.apache.catalina.Realm");
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\RealmRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */