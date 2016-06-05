package org.apache.catalina.deploy;

import java.util.LinkedHashSet;
import java.util.Set;

public class JspPropertyGroup
{
  private Boolean deferredSyntax = null;
  
  public JspPropertyGroup() {}
  
  public void setDeferredSyntax(String deferredSyntax)
  {
    this.deferredSyntax = Boolean.valueOf(deferredSyntax);
  }
  
  public Boolean getDeferredSyntax()
  {
    return this.deferredSyntax;
  }
  
  private Boolean elIgnored = null;
  
  public void setElIgnored(String elIgnored)
  {
    this.elIgnored = Boolean.valueOf(elIgnored);
  }
  
  public Boolean getElIgnored()
  {
    return this.elIgnored;
  }
  
  private Set<String> includeCodas = new LinkedHashSet();
  
  public void addIncludeCoda(String includeCoda)
  {
    this.includeCodas.add(includeCoda);
  }
  
  public Set<String> getIncludeCodas()
  {
    return this.includeCodas;
  }
  
  private Set<String> includePreludes = new LinkedHashSet();
  
  public void addIncludePrelude(String includePrelude)
  {
    this.includePreludes.add(includePrelude);
  }
  
  public Set<String> getIncludePreludes()
  {
    return this.includePreludes;
  }
  
  private Boolean isXml = null;
  
  public void setIsXml(String isXml)
  {
    this.isXml = Boolean.valueOf(isXml);
  }
  
  public Boolean getIsXml()
  {
    return this.isXml;
  }
  
  private String pageEncoding = null;
  
  public void setPageEncoding(String pageEncoding)
  {
    this.pageEncoding = pageEncoding;
  }
  
  public String getPageEncoding()
  {
    return this.pageEncoding;
  }
  
  private Boolean scriptingInvalid = null;
  
  public void setScriptingInvalid(String scriptingInvalid)
  {
    this.scriptingInvalid = Boolean.valueOf(scriptingInvalid);
  }
  
  public Boolean getScriptingInvalid()
  {
    return this.scriptingInvalid;
  }
  
  private Boolean trimWhitespace = null;
  
  public void setTrimWhitespace(String trimWhitespace)
  {
    this.trimWhitespace = Boolean.valueOf(trimWhitespace);
  }
  
  public Boolean getTrimWhitespace()
  {
    return this.trimWhitespace;
  }
  
  private LinkedHashSet<String> urlPattern = new LinkedHashSet();
  
  public void addUrlPattern(String urlPattern)
  {
    this.urlPattern.add(urlPattern);
  }
  
  public Set<String> getUrlPatterns()
  {
    return this.urlPattern;
  }
  
  private String defaultContentType = null;
  
  public void setDefaultContentType(String defaultContentType)
  {
    this.defaultContentType = defaultContentType;
  }
  
  public String getDefaultContentType()
  {
    return this.defaultContentType;
  }
  
  private String buffer = null;
  
  public void setBuffer(String buffer)
  {
    this.buffer = buffer;
  }
  
  public String getBuffer()
  {
    return this.buffer;
  }
  
  private Boolean errorOnUndeclaredNamespace = null;
  
  public void setErrorOnUndeclaredNamespace(String errorOnUndeclaredNamespace)
  {
    this.errorOnUndeclaredNamespace = Boolean.valueOf(errorOnUndeclaredNamespace);
  }
  
  public Boolean getErrorOnUndeclaredNamespace()
  {
    return this.errorOnUndeclaredNamespace;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\JspPropertyGroup.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */