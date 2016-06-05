package org.apache.catalina.connector;

import java.io.IOException;
import javax.servlet.ServletOutputStream;

public class CoyoteOutputStream
  extends ServletOutputStream
{
  protected OutputBuffer ob;
  
  protected CoyoteOutputStream(OutputBuffer ob)
  {
    this.ob = ob;
  }
  
  protected Object clone()
    throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }
  
  void clear()
  {
    this.ob = null;
  }
  
  public void write(int i)
    throws IOException
  {
    this.ob.writeByte(i);
  }
  
  public void write(byte[] b)
    throws IOException
  {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len)
    throws IOException
  {
    this.ob.write(b, off, len);
  }
  
  public void flush()
    throws IOException
  {
    this.ob.flush();
  }
  
  public void close()
    throws IOException
  {
    this.ob.close();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CoyoteOutputStream.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */