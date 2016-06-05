package org.apache.catalina.connector;

import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import org.apache.catalina.Globals;
import org.apache.coyote.ActionCode;
import org.apache.coyote.Response;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.ByteChunk.ByteOutputChannel;
import org.apache.tomcat.util.buf.C2BConverter;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.CharChunk.CharOutputChannel;

public class OutputBuffer
  extends Writer
  implements ByteChunk.ByteOutputChannel, CharChunk.CharOutputChannel
{
  public static final String DEFAULT_ENCODING = "ISO-8859-1";
  public static final int DEFAULT_BUFFER_SIZE = 8192;
  private final ByteChunk bb;
  private final CharChunk cb;
  private boolean initial = true;
  private long bytesWritten = 0L;
  private long charsWritten = 0L;
  private boolean closed = false;
  private boolean doFlush = false;
  private final ByteChunk outputChunk = new ByteChunk();
  private CharChunk outputCharChunk = new CharChunk();
  private String enc;
  private boolean gotEnc = false;
  protected HashMap<String, C2BConverter> encoders = new HashMap();
  protected C2BConverter conv;
  private Response coyoteResponse;
  private boolean suspended = false;
  
  public OutputBuffer()
  {
    this(8192);
  }
  
  public OutputBuffer(int size)
  {
    this.bb = new ByteChunk(size);
    this.bb.setLimit(size);
    this.bb.setByteOutputChannel(this);
    this.cb = new CharChunk(size);
    this.cb.setLimit(size);
    this.cb.setOptimizedWrite(false);
    this.cb.setCharOutputChannel(this);
  }
  
  public void setResponse(Response coyoteResponse)
  {
    this.coyoteResponse = coyoteResponse;
  }
  
  @Deprecated
  public Response getResponse()
  {
    return this.coyoteResponse;
  }
  
  public boolean isSuspended()
  {
    return this.suspended;
  }
  
  public void setSuspended(boolean suspended)
  {
    this.suspended = suspended;
  }
  
  public boolean isClosed()
  {
    return this.closed;
  }
  
  public void recycle()
  {
    this.initial = true;
    this.bytesWritten = 0L;
    this.charsWritten = 0L;
    
    this.bb.recycle();
    this.cb.recycle();
    this.outputCharChunk.setChars(null, 0, 0);
    this.closed = false;
    this.suspended = false;
    this.doFlush = false;
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
    if (this.closed) {
      return;
    }
    if (this.suspended) {
      return;
    }
    if (this.cb.getLength() > 0) {
      this.cb.flushBuffer();
    }
    if ((!this.coyoteResponse.isCommitted()) && (this.coyoteResponse.getContentLengthLong() == -1L)) {
      if (!this.coyoteResponse.isCommitted()) {
        this.coyoteResponse.setContentLength(this.bb.getLength());
      }
    }
    if (this.coyoteResponse.getStatus() == 101) {
      doFlush(true);
    } else {
      doFlush(false);
    }
    this.closed = true;
    
    Request req = (Request)this.coyoteResponse.getRequest().getNote(1);
    
    req.inputBuffer.close();
    
    this.coyoteResponse.finish();
  }
  
  public void flush()
    throws IOException
  {
    doFlush(true);
  }
  
  protected void doFlush(boolean realFlush)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    try
    {
      this.doFlush = true;
      if (this.initial)
      {
        this.coyoteResponse.sendHeaders();
        this.initial = false;
      }
      if (this.cb.getLength() > 0) {
        this.cb.flushBuffer();
      }
      if (this.bb.getLength() > 0) {
        this.bb.flushBuffer();
      }
    }
    finally
    {
      this.doFlush = false;
    }
    if (realFlush)
    {
      this.coyoteResponse.action(ActionCode.CLIENT_FLUSH, null);
      if (this.coyoteResponse.isExceptionPresent()) {
        throw new ClientAbortException(this.coyoteResponse.getErrorException());
      }
    }
  }
  
  public void realWriteBytes(byte[] buf, int off, int cnt)
    throws IOException
  {
    if (this.closed) {
      return;
    }
    if (this.coyoteResponse == null) {
      return;
    }
    if (cnt > 0)
    {
      this.outputChunk.setBytes(buf, off, cnt);
      try
      {
        this.coyoteResponse.doWrite(this.outputChunk);
      }
      catch (IOException e)
      {
        throw new ClientAbortException(e);
      }
    }
  }
  
  public void write(byte[] b, int off, int len)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    writeBytes(b, off, len);
  }
  
  private void writeBytes(byte[] b, int off, int len)
    throws IOException
  {
    if (this.closed) {
      return;
    }
    this.bb.append(b, off, len);
    this.bytesWritten += len;
    if (this.doFlush) {
      this.bb.flushBuffer();
    }
  }
  
  public void writeByte(int b)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    this.bb.append((byte)b);
    this.bytesWritten += 1L;
  }
  
  public void realWriteChars(char[] buf, int off, int len)
    throws IOException
  {
    this.outputCharChunk.setChars(buf, off, len);
    while (this.outputCharChunk.getLength() > 0)
    {
      this.conv.convert(this.outputCharChunk, this.bb);
      if (this.bb.getLength() == 0) {
        break;
      }
      if (this.outputCharChunk.getLength() > 0) {
        if ((this.bb.getBuffer().length == this.bb.getEnd()) && (this.bb.getLength() < this.bb.getLimit())) {
          this.bb.makeSpace(this.outputCharChunk.getLength());
        } else {
          this.bb.flushBuffer();
        }
      }
    }
  }
  
  public void write(int c)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    this.cb.append((char)c);
    this.charsWritten += 1L;
  }
  
  public void write(char[] c)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    write(c, 0, c.length);
  }
  
  public void write(char[] c, int off, int len)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    this.cb.append(c, off, len);
    this.charsWritten += len;
  }
  
  public void write(String s, int off, int len)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    this.charsWritten += len;
    if (s == null) {
      s = "null";
    }
    this.cb.append(s, off, len);
    this.charsWritten += len;
  }
  
  public void write(String s)
    throws IOException
  {
    if (this.suspended) {
      return;
    }
    if (s == null) {
      s = "null";
    }
    this.cb.append(s);
    this.charsWritten += s.length();
  }
  
  public void setEncoding(String s)
  {
    this.enc = s;
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
    if (this.coyoteResponse != null) {
      this.enc = this.coyoteResponse.getCharacterEncoding();
    }
    this.gotEnc = true;
    if (this.enc == null) {
      this.enc = "ISO-8859-1";
    }
    this.conv = ((C2BConverter)this.encoders.get(this.enc));
    if (this.conv == null)
    {
      if (Globals.IS_SECURITY_ENABLED) {
        try
        {
          this.conv = ((C2BConverter)AccessController.doPrivileged(new PrivilegedExceptionAction()
          {
            public C2BConverter run()
              throws IOException
            {
              return new C2BConverter(OutputBuffer.this.enc);
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
        this.conv = new C2BConverter(this.enc);
      }
      this.encoders.put(this.enc, this.conv);
    }
  }
  
  public long getContentWritten()
  {
    return this.bytesWritten + this.charsWritten;
  }
  
  public boolean isNew()
  {
    return (this.bytesWritten == 0L) && (this.charsWritten == 0L);
  }
  
  public void setBufferSize(int size)
  {
    if (size > this.bb.getLimit()) {
      this.bb.setLimit(size);
    }
  }
  
  public void reset()
  {
    reset(false);
  }
  
  public void reset(boolean resetWriterStreamFlags)
  {
    this.bb.recycle();
    this.cb.recycle();
    this.bytesWritten = 0L;
    this.charsWritten = 0L;
    if (resetWriterStreamFlags)
    {
      this.gotEnc = false;
      this.enc = null;
    }
    this.initial = true;
  }
  
  public int getBufferSize()
  {
    return this.bb.getLimit();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\OutputBuffer.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */