package org.apache.catalina.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

public class ResourceEntry
{
  public long lastModified = -1L;
  public byte[] binaryContent = null;
  public volatile Class<?> loadedClass = null;
  public URL source = null;
  public URL codeBase = null;
  public Manifest manifest = null;
  public Certificate[] certificates = null;
  
  public ResourceEntry() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\ResourceEntry.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */