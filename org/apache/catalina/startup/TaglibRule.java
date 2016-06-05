package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;

final class TaglibRule
  extends Rule
{
  private final TaglibUriRule taglibUriRule;
  
  public TaglibRule(TaglibUriRule taglibUriRule)
  {
    this.taglibUriRule = taglibUriRule;
  }
  
  public void body(String namespace, String name, String text)
    throws Exception
  {
    this.taglibUriRule.setDuplicateUri(false);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TaglibRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */