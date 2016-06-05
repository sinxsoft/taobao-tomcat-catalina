package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;

final class TaglibListenerRule
  extends Rule
{
  private final TaglibUriRule taglibUriRule;
  
  public TaglibListenerRule(TaglibUriRule taglibUriRule)
  {
    this.taglibUriRule = taglibUriRule;
  }
  
  public void body(String namespace, String name, String text)
    throws Exception
  {
    TldConfig tldConfig = (TldConfig)this.digester.peek(this.digester.getCount() - 1);
    if (!this.taglibUriRule.isDuplicateUri()) {
      tldConfig.addApplicationListener(text.trim());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TaglibListenerRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */