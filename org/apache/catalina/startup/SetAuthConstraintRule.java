package org.apache.catalina.startup;

import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class SetAuthConstraintRule
  extends Rule
{
  public SetAuthConstraintRule() {}
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    SecurityConstraint securityConstraint = (SecurityConstraint)this.digester.peek();
    
    securityConstraint.setAuthConstraint(true);
    if (this.digester.getLogger().isDebugEnabled()) {
      this.digester.getLogger().debug("Calling SecurityConstraint.setAuthConstraint(true)");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\SetAuthConstraintRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */