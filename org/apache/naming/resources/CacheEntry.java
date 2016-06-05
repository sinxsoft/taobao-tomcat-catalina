package org.apache.naming.resources;

import javax.naming.directory.DirContext;

public class CacheEntry
{
  public long timestamp = -1L;
  public String name = null;
  public ResourceAttributes attributes = null;
  public Resource resource = null;
  public DirContext context = null;
  public boolean exists = true;
  public long accessCount = 0L;
  public int size = 1;
  
  public CacheEntry() {}
  
  public void recycle()
  {
    this.timestamp = -1L;
    this.name = null;
    this.attributes = null;
    this.resource = null;
    this.context = null;
    this.exists = true;
    this.accessCount = 0L;
    this.size = 1;
  }
  
  public String toString()
  {
    return "Cache entry: " + this.name + "\n" + "Exists: " + this.exists + "\n" + "Attributes: " + this.attributes + "\n" + "Resource: " + this.resource + "\n" + "Context: " + this.context;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\CacheEntry.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */