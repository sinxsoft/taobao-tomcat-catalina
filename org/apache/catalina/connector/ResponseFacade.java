package org.apache.catalina.connector;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityUtil;
import org.apache.tomcat.util.res.StringManager;

public class ResponseFacade
  implements HttpServletResponse
{
  private final class SetContentTypePrivilegedAction
    implements PrivilegedAction<Void>
  {
    private final String contentType;
    
    public SetContentTypePrivilegedAction(String contentType)
    {
      this.contentType = contentType;
    }
    
    public Void run()
    {
      ResponseFacade.this.response.setContentType(this.contentType);
      return null;
    }
  }
  
  private final class DateHeaderPrivilegedAction
    implements PrivilegedAction<Void>
  {
    private final String name;
    private final long value;
    private final boolean add;
    
    DateHeaderPrivilegedAction(String name, long value, boolean add)
    {
      this.name = name;
      this.value = value;
      this.add = add;
    }
    
    public Void run()
    {
      if (this.add) {
        ResponseFacade.this.response.addDateHeader(this.name, this.value);
      } else {
        ResponseFacade.this.response.setDateHeader(this.name, this.value);
      }
      return null;
    }
  }
  
  public ResponseFacade(Response response)
  {
    this.response = response;
  }
  
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  protected Response response = null;
  
  public void clear()
  {
    this.response = null;
  }
  
  protected Object clone()
    throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }
  
  public void finish()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    this.response.setSuspended(true);
  }
  
  public boolean isFinished()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.isSuspended();
  }
  
  public long getContentWritten()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.getContentWritten();
  }
  
  public String getCharacterEncoding()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.getCharacterEncoding();
  }
  
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    ServletOutputStream sos = this.response.getOutputStream();
    if (isFinished()) {
      this.response.setSuspended(true);
    }
    return sos;
  }
  
  public PrintWriter getWriter()
    throws IOException
  {
    PrintWriter writer = this.response.getWriter();
    if (isFinished()) {
      this.response.setSuspended(true);
    }
    return writer;
  }
  
  public void setContentLength(int len)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setContentLength(len);
  }
  
  public void setContentType(String type)
  {
    if (isCommitted()) {
      return;
    }
    if (SecurityUtil.isPackageProtectionEnabled()) {
      AccessController.doPrivileged(new SetContentTypePrivilegedAction(type));
    } else {
      this.response.setContentType(type);
    }
  }
  
  public void setBufferSize(int size)
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.setBufferSize.ise"));
    }
    this.response.setBufferSize(size);
  }
  
  public int getBufferSize()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.getBufferSize();
  }
  
  public void flushBuffer()
    throws IOException
  {
    if (isFinished()) {
      return;
    }
    if (SecurityUtil.isPackageProtectionEnabled())
    {
      try
      {
        AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Void run()
            throws IOException
          {
            ResponseFacade.this.response.setAppCommitted(true);
            
            ResponseFacade.this.response.flushBuffer();
            return null;
          }
        });
      }
      catch (PrivilegedActionException e)
      {
        Exception ex = e.getException();
        if ((ex instanceof IOException)) {
          throw ((IOException)ex);
        }
      }
    }
    else
    {
      this.response.setAppCommitted(true);
      
      this.response.flushBuffer();
    }
  }
  
  public void resetBuffer()
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.resetBuffer.ise"));
    }
    this.response.resetBuffer();
  }
  
  public boolean isCommitted()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.isAppCommitted();
  }
  
  public void reset()
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.reset.ise"));
    }
    this.response.reset();
  }
  
  public void setLocale(Locale loc)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setLocale(loc);
  }
  
  public Locale getLocale()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.getLocale();
  }
  
  public void addCookie(Cookie cookie)
  {
    if (isCommitted()) {
      return;
    }
    this.response.addCookie(cookie);
  }
  
  public boolean containsHeader(String name)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.containsHeader(name);
  }
  
  public String encodeURL(String url)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.encodeURL(url);
  }
  
  public String encodeRedirectURL(String url)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.encodeRedirectURL(url);
  }
  
  public String encodeUrl(String url)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.encodeURL(url);
  }
  
  public String encodeRedirectUrl(String url)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.encodeRedirectURL(url);
  }
  
  public void sendError(int sc, String msg)
    throws IOException
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.sendError.ise"));
    }
    this.response.setAppCommitted(true);
    
    this.response.sendError(sc, msg);
  }
  
  public void sendError(int sc)
    throws IOException
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.sendError.ise"));
    }
    this.response.setAppCommitted(true);
    
    this.response.sendError(sc);
  }
  
  public void sendRedirect(String location)
    throws IOException
  {
    if (isCommitted()) {
      throw new IllegalStateException(sm.getString("coyoteResponse.sendRedirect.ise"));
    }
    this.response.setAppCommitted(true);
    
    this.response.sendRedirect(location);
  }
  
  public void setDateHeader(String name, long date)
  {
    if (isCommitted()) {
      return;
    }
    if (Globals.IS_SECURITY_ENABLED) {
      AccessController.doPrivileged(new DateHeaderPrivilegedAction(name, date, false));
    } else {
      this.response.setDateHeader(name, date);
    }
  }
  
  public void addDateHeader(String name, long date)
  {
    if (isCommitted()) {
      return;
    }
    if (Globals.IS_SECURITY_ENABLED) {
      AccessController.doPrivileged(new DateHeaderPrivilegedAction(name, date, true));
    } else {
      this.response.addDateHeader(name, date);
    }
  }
  
  public void setHeader(String name, String value)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setHeader(name, value);
  }
  
  public void addHeader(String name, String value)
  {
    if (isCommitted()) {
      return;
    }
    this.response.addHeader(name, value);
  }
  
  public void setIntHeader(String name, int value)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setIntHeader(name, value);
  }
  
  public void addIntHeader(String name, int value)
  {
    if (isCommitted()) {
      return;
    }
    this.response.addIntHeader(name, value);
  }
  
  public void setStatus(int sc)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setStatus(sc);
  }
  
  public void setStatus(int sc, String sm)
  {
    if (isCommitted()) {
      return;
    }
    this.response.setStatus(sc, sm);
  }
  
  public String getContentType()
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    return this.response.getContentType();
  }
  
  public void setCharacterEncoding(String arg0)
  {
    if (this.response == null) {
      throw new IllegalStateException(sm.getString("responseFacade.nullResponse"));
    }
    this.response.setCharacterEncoding(arg0);
  }
  
  public int getStatus()
  {
    return this.response.getStatus();
  }
  
  public String getHeader(String name)
  {
    return this.response.getHeader(name);
  }
  
  public Collection<String> getHeaderNames()
  {
    return this.response.getHeaderNames();
  }
  
  public Collection<String> getHeaders(String name)
  {
    return this.response.getHeaders(name);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\ResponseFacade.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */