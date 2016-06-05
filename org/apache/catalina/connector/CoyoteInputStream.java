package org.apache.catalina.connector;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.servlet.ServletInputStream;
import org.apache.catalina.security.SecurityUtil;

public class CoyoteInputStream
  extends ServletInputStream
{
  protected InputBuffer ib;
  
  protected CoyoteInputStream(InputBuffer ib)
  {
    this.ib = ib;
  }
  
  void clear()
  {
    this.ib = null;
  }
  
  protected Object clone()
    throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }
  
  public int read()
    throws IOException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        Integer result = (Integer)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Integer run()
            throws IOException
          {
            Integer integer = Integer.valueOf(CoyoteInputStream.this.ib.readByte());
            return integer;
          }
        });
        return result.intValue();
      }
      catch (PrivilegedActionException pae)
      {
        Exception e = pae.getException();
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return this.ib.readByte();
  }
  
  public int available()
    throws IOException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        Integer result = (Integer)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Integer run()
            throws IOException
          {
            Integer integer = Integer.valueOf(CoyoteInputStream.this.ib.available());
            return integer;
          }
        });
        return result.intValue();
      }
      catch (PrivilegedActionException pae)
      {
        Exception e = pae.getException();
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return this.ib.available();
  }
  
  public int read(final byte[] b)
    throws IOException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        Integer result = (Integer)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Integer run()
            throws IOException
          {
            Integer integer = Integer.valueOf(CoyoteInputStream.this.ib.read(b, 0, b.length));
            
            return integer;
          }
        });
        return result.intValue();
      }
      catch (PrivilegedActionException pae)
      {
        Exception e = pae.getException();
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return this.ib.read(b, 0, b.length);
  }
  
  public int read(final byte[] b, final int off, final int len)
    throws IOException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        Integer result = (Integer)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Integer run()
            throws IOException
          {
            Integer integer = Integer.valueOf(CoyoteInputStream.this.ib.read(b, off, len));
            
            return integer;
          }
        });
        return result.intValue();
      }
      catch (PrivilegedActionException pae)
      {
        Exception e = pae.getException();
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return this.ib.read(b, off, len);
  }
  
  public int readLine(byte[] b, int off, int len)
    throws IOException
  {
    return super.readLine(b, off, len);
  }
  
  public void close()
    throws IOException
  {
    if (SecurityUtil.isPackageProtectionEnabled()) {
      try
      {
        AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Void run()
            throws IOException
          {
            CoyoteInputStream.this.ib.close();
            return null;
          }
        });
      }
      catch (PrivilegedActionException pae)
      {
        Exception e = pae.getException();
        if ((e instanceof IOException)) {
          throw ((IOException)e);
        }
        throw new RuntimeException(e.getMessage(), e);
      }
    } else {
      this.ib.close();
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\CoyoteInputStream.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */