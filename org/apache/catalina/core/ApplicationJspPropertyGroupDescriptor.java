package org.apache.catalina.core;

import java.util.Collection;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import org.apache.catalina.deploy.JspPropertyGroup;

public class ApplicationJspPropertyGroupDescriptor
  implements JspPropertyGroupDescriptor
{
  @Deprecated
  JspPropertyGroup jspPropertyGroup;
  
  public ApplicationJspPropertyGroupDescriptor(JspPropertyGroup jspPropertyGroup)
  {
    this.jspPropertyGroup = jspPropertyGroup;
  }
  
  public String getBuffer()
  {
    return this.jspPropertyGroup.getBuffer();
  }
  
  public String getDefaultContentType()
  {
    return this.jspPropertyGroup.getDefaultContentType();
  }
  
  public String getDeferredSyntaxAllowedAsLiteral()
  {
    String result = null;
    if (this.jspPropertyGroup.getDeferredSyntax() != null) {
      result = this.jspPropertyGroup.getDeferredSyntax().toString();
    }
    return result;
  }
  
  public String getElIgnored()
  {
    String result = null;
    if (this.jspPropertyGroup.getElIgnored() != null) {
      result = this.jspPropertyGroup.getElIgnored().toString();
    }
    return result;
  }
  
  public String getErrorOnUndeclaredNamespace()
  {
    String result = null;
    if (this.jspPropertyGroup.getErrorOnUndeclaredNamespace() != null) {
      result = this.jspPropertyGroup.getErrorOnUndeclaredNamespace().toString();
    }
    return result;
  }
  
  public Collection<String> getIncludeCodas()
  {
    return this.jspPropertyGroup.getIncludeCodas();
  }
  
  public Collection<String> getIncludePreludes()
  {
    return this.jspPropertyGroup.getIncludePreludes();
  }
  
  public String getIsXml()
  {
    String result = null;
    if (this.jspPropertyGroup.getIsXml() != null) {
      result = this.jspPropertyGroup.getIsXml().toString();
    }
    return result;
  }
  
  public String getPageEncoding()
  {
    return this.jspPropertyGroup.getPageEncoding();
  }
  
  public String getScriptingInvalid()
  {
    String result = null;
    if (this.jspPropertyGroup.getScriptingInvalid() != null) {
      result = this.jspPropertyGroup.getScriptingInvalid().toString();
    }
    return result;
  }
  
  public String getTrimDirectiveWhitespaces()
  {
    String result = null;
    if (this.jspPropertyGroup.getTrimWhitespace() != null) {
      result = this.jspPropertyGroup.getTrimWhitespace().toString();
    }
    return result;
  }
  
  public Collection<String> getUrlPatterns()
  {
    return this.jspPropertyGroup.getUrlPatterns();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationJspPropertyGroupDescriptor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */