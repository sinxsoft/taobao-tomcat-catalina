package org.apache.catalina.startup;

import com.taobao.catalina.startup.HostConfigRule;
import com.taobao.catalina.startup.HostCreateRule;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class HostRuleSet
  extends RuleSetBase
{
  protected String prefix = null;
  
  public HostRuleSet()
  {
    this("");
  }
  
  public HostRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public void addRuleInstances(Digester digester)
  {
    digester.addRule(this.prefix + "Host", new HostCreateRule("org.apache.catalina.core.StandardHost", "className"));
    
    digester.addSetProperties(this.prefix + "Host");
    digester.addRule(this.prefix + "Host", new CopyParentClassLoaderRule());
    
    digester.addRule(this.prefix + "Host", new HostConfigRule("org.apache.catalina.startup.HostConfig", "hostConfigClass"));
    
    digester.addSetNext(this.prefix + "Host", "addChild", "org.apache.catalina.Container");
    
    digester.addCallMethod(this.prefix + "Host/Alias", "addAlias", 0);
    
    digester.addObjectCreate(this.prefix + "Host/Cluster", null, "className");
    
    digester.addSetProperties(this.prefix + "Host/Cluster");
    digester.addSetNext(this.prefix + "Host/Cluster", "setCluster", "org.apache.catalina.Cluster");
    
    digester.addObjectCreate(this.prefix + "Host/Listener", null, "className");
    
    digester.addSetProperties(this.prefix + "Host/Listener");
    digester.addSetNext(this.prefix + "Host/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");
    
    digester.addRuleSet(new RealmRuleSet(this.prefix + "Host/"));
    
    digester.addObjectCreate(this.prefix + "Host/Valve", null, "className");
    
    digester.addSetProperties(this.prefix + "Host/Valve");
    digester.addSetNext(this.prefix + "Host/Valve", "addValve", "org.apache.catalina.Valve");
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\HostRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */