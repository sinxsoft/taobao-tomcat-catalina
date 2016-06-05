package com.taobao.tomcat.util.http;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.apache.tomcat.util.buf.ByteChunk;

public class CookieWarn
{
  private Exception exception;
  private String message;
  private ByteChunk cookie;
  
  public CookieWarn() {}
  
  public Exception getException()
  {
    return this.exception;
  }
  
  public void setException(Exception exception)
  {
    this.exception = exception;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public ByteChunk getCookie()
  {
    return this.cookie;
  }
  
  public void setCookie(ByteChunk cookie)
  {
    this.cookie = cookie;
  }
  
  public String getCookieString()
  {
    ByteBuffer bb = ByteBuffer.wrap(this.cookie.getBytes(), this.cookie.getOffset(), this.cookie.getLength());
    CharBuffer cb = Charset.defaultCharset().decode(bb);
    return new String(cb.array(), cb.arrayOffset(), cb.length());
  }
}
