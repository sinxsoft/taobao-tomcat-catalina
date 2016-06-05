package org.apache.catalina.startup;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;

final class TaglibUriRule
  extends Rule
{
  private boolean duplicateUri;
  
  public TaglibUriRule() {}
  
  public void body(String namespace, String name, String text)
    throws Exception
  {
    TldConfig tldConfig = (TldConfig)this.digester.peek(this.digester.getCount() - 1);
    if (tldConfig.isKnownTaglibUri(text))
    {
      this.duplicateUri = true;
      if (tldConfig.isKnownWebxmlTaglibUri(text))
      {
        if (this.digester.getLogger().isDebugEnabled()) {
          this.digester.getLogger().debug("TLD skipped. URI: " + text + " is already defined");
        }
      }
      else {
        this.digester.getLogger().info("TLD skipped. URI: " + text + " is already defined");
      }
    }
    else
    {
      tldConfig.addTaglibUri(text);
    }
  }
  
  public boolean isDuplicateUri()
  {
    return this.duplicateUri;
  }
  
  public void setDuplicateUri(boolean duplciateUri)
  {
    this.duplicateUri = duplciateUri;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TaglibUriRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */