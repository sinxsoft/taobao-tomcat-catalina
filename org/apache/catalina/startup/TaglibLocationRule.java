package org.apache.catalina.startup;

import org.apache.catalina.deploy.WebXml;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

final class TaglibLocationRule
  extends Rule
{
  final boolean isServlet24OrLater;
  
  public TaglibLocationRule(boolean isServlet24OrLater)
  {
    this.isServlet24OrLater = isServlet24OrLater;
  }
  
  public void begin(String namespace, String name, Attributes attributes)
    throws Exception
  {
    WebXml webXml = (WebXml)this.digester.peek(this.digester.getCount() - 1);
    
    boolean havePublicId = webXml.getPublicId() != null;
    if (havePublicId == this.isServlet24OrLater) {
      throw new IllegalArgumentException("taglib definition not consistent with specification version");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TaglibLocationRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */