package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class TldRuleSet
  extends RuleSetBase
{
  protected String prefix = null;
  
  public TldRuleSet()
  {
    this("");
  }
  
  public TldRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public void addRuleInstances(Digester digester)
  {
    TaglibUriRule taglibUriRule = new TaglibUriRule();
    
    digester.addRule(this.prefix + "taglib", new TaglibRule(taglibUriRule));
    
    digester.addRule(this.prefix + "taglib/uri", taglibUriRule);
    
    digester.addRule(this.prefix + "taglib/listener/listener-class", new TaglibListenerRule(taglibUriRule));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TldRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */