package org.apache.catalina.startup;

import com.taobao.catalina.startup.LoaderCreateRule;
import com.taobao.catalina.startup.PandoraManagerCreateRule;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class ContextRuleSet
  extends RuleSetBase
{
  protected String prefix = null;
  protected boolean create = true;
  
  public ContextRuleSet()
  {
    this("");
  }
  
  public ContextRuleSet(String prefix)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
  }
  
  public ContextRuleSet(String prefix, boolean create)
  {
    this.namespaceURI = null;
    this.prefix = prefix;
    this.create = create;
  }
  
  public void addRuleInstances(Digester digester)
  {
    if (this.create)
    {
      digester.addObjectCreate(this.prefix + "Context", "org.apache.catalina.core.StandardContext", "className");
      
      digester.addSetProperties(this.prefix + "Context");
    }
    else
    {
      digester.addRule(this.prefix + "Context", new SetContextPropertiesRule());
    }
    if (this.create)
    {
      digester.addRule(this.prefix + "Context", new LifecycleListenerRule("org.apache.catalina.startup.ContextConfig", "configClass"));
      
      digester.addSetNext(this.prefix + "Context", "addChild", "org.apache.catalina.Container");
    }
    digester.addCallMethod(this.prefix + "Context/InstanceListener", "addInstanceListener", 0);
    
    digester.addObjectCreate(this.prefix + "Context/Listener", null, "className");
    
    digester.addSetProperties(this.prefix + "Context/Listener");
    digester.addSetNext(this.prefix + "Context/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");
    
    digester.addRule(this.prefix + "Context/Loader", new LoaderCreateRule("org.apache.catalina.loader.WebappLoader", "className"));
    
    digester.addSetProperties(this.prefix + "Context/Loader");
    digester.addSetNext(this.prefix + "Context/Loader", "setLoader", "org.apache.catalina.Loader");
    
    digester.addObjectCreate(this.prefix + "Context/Manager", "org.apache.catalina.session.StandardManager", "className");
    
    digester.addSetProperties(this.prefix + "Context/Manager");
    digester.addSetNext(this.prefix + "Context/Manager", "setManager", "org.apache.catalina.Manager");
    
    digester.addObjectCreate(this.prefix + "Context/Manager/Store", null, "className");
    
    digester.addSetProperties(this.prefix + "Context/Manager/Store");
    digester.addSetNext(this.prefix + "Context/Manager/Store", "setStore", "org.apache.catalina.Store");
    
    digester.addObjectCreate(this.prefix + "Context/Manager/SessionIdGenerator", "org.apache.catalina.util.StandardSessionIdGenerator", "className");
    
    digester.addSetProperties(this.prefix + "Context/Manager/SessionIdGenerator");
    digester.addSetNext(this.prefix + "Context/Manager/SessionIdGenerator", "setSessionIdGenerator", "org.apache.catalina.SessionIdGenerator");
    
    digester.addObjectCreate(this.prefix + "Context/Parameter", "org.apache.catalina.deploy.ApplicationParameter");
    
    digester.addSetProperties(this.prefix + "Context/Parameter");
    digester.addSetNext(this.prefix + "Context/Parameter", "addApplicationParameter", "org.apache.catalina.deploy.ApplicationParameter");
    
    digester.addRuleSet(new RealmRuleSet(this.prefix + "Context/"));
    
    digester.addObjectCreate(this.prefix + "Context/Resources", "org.apache.naming.resources.FileDirContext", "className");
    
    digester.addSetProperties(this.prefix + "Context/Resources");
    digester.addSetNext(this.prefix + "Context/Resources", "setResources", "javax.naming.directory.DirContext");
    
    digester.addObjectCreate(this.prefix + "Context/ResourceLink", "org.apache.catalina.deploy.ContextResourceLink");
    
    digester.addSetProperties(this.prefix + "Context/ResourceLink");
    digester.addRule(this.prefix + "Context/ResourceLink", new SetNextNamingRule("addResourceLink", "org.apache.catalina.deploy.ContextResourceLink"));
    
    digester.addObjectCreate(this.prefix + "Context/Valve", null, "className");
    
    digester.addSetProperties(this.prefix + "Context/Valve");
    digester.addSetNext(this.prefix + "Context/Valve", "addValve", "org.apache.catalina.Valve");
    
    digester.addRule(this.prefix + "Context/Pandora", new PandoraManagerCreateRule(this.prefix + "Context/Pandora", "com.taobao.tomcat.container.context.pandora.PandoraManager", "className"));
    
    digester.addSetProperties(this.prefix + "Context/Pandora");
    digester.addSetNext(this.prefix + "Context/Pandora", "setPandoraManager", "com.taobao.tomcat.container.context.pandora.PandoraManager");
    
    digester.addCallMethod(this.prefix + "Context/WatchedResource", "addWatchedResource", 0);
    
    digester.addCallMethod(this.prefix + "Context/WrapperLifecycle", "addWrapperLifecycle", 0);
    
    digester.addCallMethod(this.prefix + "Context/WrapperListener", "addWrapperListener", 0);
    
    digester.addObjectCreate(this.prefix + "Context/JarScanner", "org.apache.tomcat.util.scan.StandardJarScanner", "className");
    
    digester.addSetProperties(this.prefix + "Context/JarScanner");
    digester.addSetNext(this.prefix + "Context/JarScanner", "setJarScanner", "org.apache.tomcat.JarScanner");
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\ContextRuleSet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */