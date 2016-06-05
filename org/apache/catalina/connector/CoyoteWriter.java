package org.apache.catalina.connector;

import java.io.IOException;
import java.io.PrintWriter;

public class CoyoteWriter
  extends PrintWriter
{
  private static final char[] LINE_SEP = System.getProperty("line.separator").toCharArray();
  protected OutputBuffer ob;
  protected boolean error = false;
  
  public CoyoteWriter(OutputBuffer ob)
  {
    super(ob);
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
  
  void recycle()
  {
    this.error = false;
  }
  
  public void flush()
  {
    if (this.error) {
      return;
    }
    try
    {
      this.ob.flush();
    }
    catch (IOException e)
    {
      this.error = true;
    }
  }
  
  public void close()
  {
    try
    {
      this.ob.close();
    }
    catch (IOException ex) {}
    this.error = false;
  }
  
  public boolean checkError()
  {
    flush();
    return this.error;
  }
  
  public void write(int c)
  {
    if (this.error) {
      return;
    }
    try
    {
      this.ob.write(c);
    }
    catch (IOException e)
    {
      this.error = true;
    }
  }
  
  public void write(char[] buf, int off, int len)
  {
    if (this.error) {
      return;
    }
    try
    {
      this.ob.write(buf, off, len);
    }
    catch (IOException e)
    {
      this.error = true;
    }
  }
  
  public void write(char[] buf)
  {
    write(buf, 0, buf.length);
  }
  
  public void write(String s, int off, int len)
  {
    if (this.error) {
      return;
    }
    try
    {
      this.ob.write(s, off, len);
    }
    catch (IOException e)
    {
      this.error = true;
    }
  }
  
  public void write(String s)
  {
    write(s, 0, s.length());
  }
  
  public void print(boolean b)
  {
    if (b) {
      write("true");
    } else {
      write("false");
    }
  }
  
  public void print(char c)
  {
    write(c);
  }
  
  public void print(int i)
  {
    write(String.valueOf(i));
  }
  
  public void print(long l)
  {
    write(String.valueOf(l));
  }
  
  public void print(float f)
  {
    write(String.valueOf(f));
  }
  
  public void print(double d)
  {
    write(String.valueOf(d));
  }
  
  public void print(char[] s)
  {
    write(s);
  }
  
  public void print(String s)
  {
    if (s == null) {
      s = "null";
    }
    write(s);
  }
  
  public void print(Object obj)
  {
    write(String.valueOf(obj));
  }
  
  public void println()
  {
    write(LINE_SEP);
  }
  
  public void println(boolean b)
  {
    print(b);
    println();
  }
  
  public void println(char c)
  {
    print(c);
    println();
  }
  
  public void println(int i)
  {
    print(i);
    println();
  }
  
  public void println(long l)
  {
    print(l);
    println();
  }
  
  public void println(float f)
  {
    print(f);
    println();
  }
  
  public void println(double d)
  {
    print(d);
    println();
  }
  
  public void println(char[] c)
  {
    print(c);
    println();
  }
  
  public void println(String s)
  {
    print(s);
    println();
  }
  
  public void println(Object o)
  {
    print(o);
    println();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CoyoteWriter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */