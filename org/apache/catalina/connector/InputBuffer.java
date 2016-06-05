package org.apache.catalina.connector;

import java.io.IOException;
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import org.apache.catalina.security.SecurityUtil;
import org.apache.coyote.ActionCode;
import org.apache.coyote.Request;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.ByteChunk.ByteInputChannel;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.CharChunk.CharInputChannel;
import org.apache.tomcat.util.buf.CharChunk.CharOutputChannel;
import org.apache.tomcat.util.res.StringManager;

public class InputBuffer
  extends Reader
  implements ByteChunk.ByteInputChannel, CharChunk.CharInputChannel, CharChunk.CharOutputChannel
{
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  public static final String DEFAULT_ENCODING = "ISO-8859-1";
  public static final int DEFAULT_BUFFER_SIZE = 8192;
  public final int INITIAL_STATE = 0;
  public final int CHAR_STATE = 1;
  public final int BYTE_STATE = 2;
  private final ByteChunk bb;
  private CharChunk cb;
  private int state = 0;
  private boolean closed = false;
  private String enc;
  private boolean gotEnc = false;
  protected HashMap<String, B2CConverter> encoders = new HashMap();
  protected B2CConverter conv;
  private Request coyoteRequest;
  private int markPos = -1;
  private int size = -1;
  
  public InputBuffer()
  {
    this(8192);
  }
  
  public InputBuffer(int size)
  {
    this.size = size;
    this.bb = new ByteChunk(size);
    this.bb.setLimit(size);
    this.bb.setByteInputChannel(this);
    this.cb = new CharChunk(size);
    this.cb.setLimit(size);
    this.cb.setOptimizedWrite(false);
    this.cb.setCharInputChannel(this);
    this.cb.setCharOutputChannel(this);
  }
  
  public void setRequest(Request coyoteRequest)
  {
    this.coyoteRequest = coyoteRequest;
  }
  
  @Deprecated
  public Request getRequest()
  {
    return this.coyoteRequest;
  }
  
  public void recycle()
  {
    this.state = 0;
    if (this.cb.getChars().length > this.size)
    {
      this.cb = new CharChunk(this.size);
      this.cb.setLimit(this.size);
      this.cb.setOptimizedWrite(false);
      this.cb.setCharInputChannel(this);
      this.cb.setCharOutputChannel(this);
    }
    else
    {
      this.cb.recycle();
    }
    this.markPos = -1;
    this.bb.recycle();
    this.closed = false;
    if (this.conv != null) {
      this.conv.recycle();
    }
    this.gotEnc = false;
    this.enc = null;
  }
  
  public void clearEncoders()
  {
    this.encoders.clear();
  }
  
  public void close()
    throws IOException
  {
    this.closed = true;
  }
  
  public int available()
  {
    int available = 0;
    if (this.state == 2) {
      available = this.bb.getLength();
    } else if (this.state == 1) {
      available = this.cb.getLength();
    }
    if (available == 0)
    {
      this.coyoteRequest.action(ActionCode.AVAILABLE, null);
      available = this.coyoteRequest.getAvailable() > 0 ? 1 : 0;
    }
    return available;
  }
  
  public int realReadBytes(byte[] cbuf, int off, int len)
    throws IOException
  {
    if (this.closed) {
      return -1;
    }
    if (this.coyoteRequest == null) {
      return -1;
    }
    if (this.state == 0) {
      this.state = 2;
    }
    int result = this.coyoteRequest.doRead(this.bb);
    
    return result;
  }
  
  public int readByte()
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return this.bb.substract();
  }
  
  public int read(byte[] b, int off, int len)
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return this.bb.substract(b, off, len);
  }
  
  public void realWriteChars(char[] c, int off, int len)
    throws IOException
  {
    this.markPos = -1;
    this.cb.setOffset(0);
    this.cb.setEnd(0);
  }
  
  public void setEncoding(String s)
  {
    this.enc = s;
  }
  
  public int realReadChars(char[] cbuf, int off, int len)
    throws IOException
  {
    if (!this.gotEnc) {
      setConverter();
    }
    boolean eof = false;
    if (this.bb.getLength() <= 0)
    {
      int nRead = realReadBytes(this.bb.getBytes(), 0, this.bb.getBytes().length);
      if (nRead < 0) {
        eof = true;
      }
    }
    if (this.markPos == -1)
    {
      this.cb.setOffset(0);
      this.cb.setEnd(0);
    }
    else
    {
      this.cb.makeSpace(this.bb.getLength());
      if (this.cb.getBuffer().length - this.cb.getEnd() == 0)
      {
        this.cb.setOffset(0);
        this.cb.setEnd(0);
        this.markPos = -1;
      }
    }
    this.state = 1;
    this.conv.convert(this.bb, this.cb, eof);
    if ((this.cb.getLength() == 0) && (eof)) {
      return -1;
    }
    return this.cb.getLength();
  }
  
  public int read()
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return this.cb.substract();
  }
  
  public int read(char[] cbuf)
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return read(cbuf, 0, cbuf.length);
  }
  
  public int read(char[] cbuf, int off, int len)
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return this.cb.substract(cbuf, off, len);
  }
  
  public long skip(long n)
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    if (n < 0L) {
      throw new IllegalArgumentException();
    }
    long nRead = 0L;
    while (nRead < n) {
      if (this.cb.getLength() >= n)
      {
        this.cb.setOffset(this.cb.getStart() + (int)n);
        nRead = n;
      }
      else
      {
        nRead += this.cb.getLength();
        this.cb.setOffset(this.cb.getEnd());
        int toRead = 0;
        if (this.cb.getChars().length < n - nRead) {
          toRead = this.cb.getChars().length;
        } else {
          toRead = (int)(n - nRead);
        }
        int nb = realReadChars(this.cb.getChars(), 0, toRead);
        if (nb < 0) {
          break;
        }
      }
    }
    return nRead;
  }
  
  public boolean ready()
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    return available() > 0;
  }
  
  public boolean markSupported()
  {
    return true;
  }
  
  public void mark(int readAheadLimit)
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    if (this.cb.getLength() <= 0)
    {
      this.cb.setOffset(0);
      this.cb.setEnd(0);
    }
    else if ((this.cb.getBuffer().length > 2 * this.size) && (this.cb.getLength() < this.cb.getStart()))
    {
      System.arraycopy(this.cb.getBuffer(), this.cb.getStart(), this.cb.getBuffer(), 0, this.cb.getLength());
      
      this.cb.setEnd(this.cb.getLength());
      this.cb.setOffset(0);
    }
    this.cb.setLimit(this.cb.getStart() + readAheadLimit + this.size);
    this.markPos = this.cb.getStart();
  }
  
  public void reset()
    throws IOException
  {
    if (this.closed) {
      throw new IOException(sm.getString("inputBuffer.streamClosed"));
    }
    if (this.state == 1)
    {
      if (this.markPos < 0)
      {
        this.cb.recycle();
        this.markPos = -1;
        throw new IOException();
      }
      this.cb.setOffset(this.markPos);
    }
    else
    {
      this.bb.recycle();
    }
  }
  
  public void checkConverter()
    throws IOException
  {
    if (!this.gotEnc) {
      setConverter();
    }
  }
  
  protected void setConverter()
    throws IOException
  {
    if (this.coyoteRequest != null) {
      this.enc = this.coyoteRequest.getCharacterEncoding();
    }
    this.gotEnc = true;
    if (this.enc == null) {
      this.enc = "ISO-8859-1";
    }
    this.conv = ((B2CConverter)this.encoders.get(this.enc));
    if (this.conv == null)
    {
      if (SecurityUtil.isPackageProtectionEnabled()) {
        try
        {
          this.conv = ((B2CConverter)AccessController.doPrivileged(new PrivilegedExceptionAction()
          {
            public B2CConverter run()
              throws IOException
            {
              return new B2CConverter(InputBuffer.this.enc);
            }
          }));
        }
        catch (PrivilegedActionException ex)
        {
          Exception e = ex.getException();
          if ((e instanceof IOException)) {
            throw ((IOException)e);
          }
        }
      } else {
        this.conv = new B2CConverter(this.enc);
      }
      this.encoders.put(this.enc, this.conv);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\InputBuffer.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */