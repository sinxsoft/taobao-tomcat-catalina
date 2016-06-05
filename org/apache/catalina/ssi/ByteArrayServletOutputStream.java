package org.apache.catalina.ssi;

import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;

public class ByteArrayServletOutputStream
  extends ServletOutputStream
{
  protected ByteArrayOutputStream buf = null;
  
  public ByteArrayServletOutputStream()
  {
    this.buf = new ByteArrayOutputStream();
  }
  
  public byte[] toByteArray()
  {
    return this.buf.toByteArray();
  }
  
  public void write(int b)
  {
    this.buf.write(b);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\ByteArrayServletOutputStream.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */