package org.apache.catalina.ssi;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.tomcat.util.ExceptionUtils;

public class ResponseIncludeWrapper
  extends HttpServletResponseWrapper
{
  protected long lastModified = -1L;
  private String contentType = null;
  private static final DateFormat RFC1123_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  private static final String CONTENT_TYPE = "content-type";
  private static final String LAST_MODIFIED = "last-modified";
  private static final String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
  protected ServletOutputStream captureServletOutputStream;
  protected ServletOutputStream servletOutputStream;
  protected PrintWriter printWriter;
  private ServletContext context;
  private HttpServletRequest request;
  
  static
  {
    RFC1123_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  
  public ResponseIncludeWrapper(ServletContext context, HttpServletRequest request, HttpServletResponse response, ServletOutputStream captureServletOutputStream)
  {
    super(response);
    this.context = context;
    this.request = request;
    this.captureServletOutputStream = captureServletOutputStream;
  }
  
  public void flushOutputStreamOrWriter()
    throws IOException
  {
    if (this.servletOutputStream != null) {
      this.servletOutputStream.flush();
    }
    if (this.printWriter != null) {
      this.printWriter.flush();
    }
  }
  
  public PrintWriter getWriter()
    throws IOException
  {
    if (this.servletOutputStream == null)
    {
      if (this.printWriter == null)
      {
        setCharacterEncoding(getCharacterEncoding());
        this.printWriter = new PrintWriter(new OutputStreamWriter(this.captureServletOutputStream, getCharacterEncoding()));
      }
      return this.printWriter;
    }
    throw new IllegalStateException();
  }
  
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    if (this.printWriter == null)
    {
      if (this.servletOutputStream == null) {
        this.servletOutputStream = this.captureServletOutputStream;
      }
      return this.servletOutputStream;
    }
    throw new IllegalStateException();
  }
  
  public long getLastModified()
  {
    if (this.lastModified == -1L) {
      return -1L;
    }
    return this.lastModified;
  }
  
  public void setLastModified(long lastModified)
  {
    this.lastModified = lastModified;
    ((HttpServletResponse)getResponse()).setDateHeader("last-modified", lastModified);
  }
  
  public String getContentType()
  {
    if (this.contentType == null)
    {
      String url = this.request.getRequestURI();
      String mime = this.context.getMimeType(url);
      if (mime != null) {
        setContentType(mime);
      } else {
        setContentType("application/x-octet-stream");
      }
    }
    return this.contentType;
  }
  
  public void setContentType(String mime)
  {
    this.contentType = mime;
    if (this.contentType != null) {
      getResponse().setContentType(this.contentType);
    }
  }
  
  public void addDateHeader(String name, long value)
  {
    super.addDateHeader(name, value);
    String lname = name.toLowerCase(Locale.ENGLISH);
    if (lname.equals("last-modified")) {
      this.lastModified = value;
    }
  }
  
  public void addHeader(String name, String value)
  {
    super.addHeader(name, value);
    String lname = name.toLowerCase(Locale.ENGLISH);
    if (lname.equals("last-modified")) {
      try
      {
        synchronized (RFC1123_FORMAT)
        {
          this.lastModified = RFC1123_FORMAT.parse(value).getTime();
        }
      }
      catch (Throwable ignore)
      {
        ExceptionUtils.handleThrowable(ignore);
      }
    } else if (lname.equals("content-type")) {
      this.contentType = value;
    }
  }
  
  public void setDateHeader(String name, long value)
  {
    super.setDateHeader(name, value);
    String lname = name.toLowerCase(Locale.ENGLISH);
    if (lname.equals("last-modified")) {
      this.lastModified = value;
    }
  }
  
  public void setHeader(String name, String value)
  {
    super.setHeader(name, value);
    String lname = name.toLowerCase(Locale.ENGLISH);
    if (lname.equals("last-modified")) {
      try
      {
        synchronized (RFC1123_FORMAT)
        {
          this.lastModified = RFC1123_FORMAT.parse(value).getTime();
        }
      }
      catch (Throwable ignore)
      {
        ExceptionUtils.handleThrowable(ignore);
      }
    } else if (lname.equals("content-type")) {
      this.contentType = value;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\ResponseIncludeWrapper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */