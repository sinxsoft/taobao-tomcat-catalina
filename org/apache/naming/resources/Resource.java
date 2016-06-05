package org.apache.naming.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Resource
{
  public Resource() {}
  
  public Resource(InputStream inputStream)
  {
    setContent(inputStream);
  }
  
  public Resource(byte[] binaryContent)
  {
    setContent(binaryContent);
  }
  
  protected byte[] binaryContent = null;
  protected InputStream inputStream = null;
  
  public InputStream streamContent()
    throws IOException
  {
    if (this.binaryContent != null) {
      return new ByteArrayInputStream(this.binaryContent);
    }
    return this.inputStream;
  }
  
  public byte[] getContent()
  {
    return this.binaryContent;
  }
  
  public void setContent(InputStream inputStream)
  {
    this.inputStream = inputStream;
  }
  
  public void setContent(byte[] binaryContent)
  {
    this.binaryContent = binaryContent;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\Resource.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */