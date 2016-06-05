package org.apache.catalina.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

public class ApplicationJspConfigDescriptor
  implements JspConfigDescriptor
{
  private Collection<JspPropertyGroupDescriptor> jspPropertyGroups = new LinkedHashSet();
  private Collection<TaglibDescriptor> taglibs = new HashSet();
  
  public ApplicationJspConfigDescriptor() {}
  
  public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
  {
    return this.jspPropertyGroups;
  }
  
  public Collection<TaglibDescriptor> getTaglibs()
  {
    return this.taglibs;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationJspConfigDescriptor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */