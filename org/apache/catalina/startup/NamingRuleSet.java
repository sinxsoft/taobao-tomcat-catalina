package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class NamingRuleSet
  extends RuleSetBase
{
  protected String prefix = null;
  
  public NamingRuleSet()
  {
    this("");
  }
  
  public NamingRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public void addRuleInstances(Digester digester)
  {
    digester.addObjectCreate(this.prefix + "Ejb", "org.apache.catalina.deploy.ContextEjb");
    
    digester.addRule(this.prefix + "Ejb", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "Ejb", new SetNextNamingRule("addEjb", "org.apache.catalina.deploy.ContextEjb"));
    
    digester.addObjectCreate(this.prefix + "Environment", "org.apache.catalina.deploy.ContextEnvironment");
    
    digester.addSetProperties(this.prefix + "Environment");
    digester.addRule(this.prefix + "Environment", new SetNextNamingRule("addEnvironment", "org.apache.catalina.deploy.ContextEnvironment"));
    
    digester.addObjectCreate(this.prefix + "LocalEjb", "org.apache.catalina.deploy.ContextLocalEjb");
    
    digester.addRule(this.prefix + "LocalEjb", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "LocalEjb", new SetNextNamingRule("addLocalEjb", "org.apache.catalina.deploy.ContextLocalEjb"));
    
    digester.addObjectCreate(this.prefix + "Resource", "org.apache.catalina.deploy.ContextResource");
    
    digester.addRule(this.prefix + "Resource", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "Resource", new SetNextNamingRule("addResource", "org.apache.catalina.deploy.ContextResource"));
    
    digester.addObjectCreate(this.prefix + "ResourceEnvRef", "org.apache.catalina.deploy.ContextResourceEnvRef");
    
    digester.addRule(this.prefix + "ResourceEnvRef", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "ResourceEnvRef", new SetNextNamingRule("addResourceEnvRef", "org.apache.catalina.deploy.ContextResourceEnvRef"));
    
    digester.addObjectCreate(this.prefix + "ServiceRef", "org.apache.catalina.deploy.ContextService");
    
    digester.addRule(this.prefix + "ServiceRef", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "ServiceRef", new SetNextNamingRule("addService", "org.apache.catalina.deploy.ContextService"));
    
    digester.addObjectCreate(this.prefix + "Transaction", "org.apache.catalina.deploy.ContextTransaction");
    
    digester.addRule(this.prefix + "Transaction", new SetAllPropertiesRule());
    digester.addRule(this.prefix + "Transaction", new SetNextNamingRule("setTransaction", "org.apache.catalina.deploy.ContextTransaction"));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\NamingRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */