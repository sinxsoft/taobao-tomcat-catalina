package org.apache.catalina.ssi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SSIFilter
  implements Filter
{
  protected FilterConfig config = null;
  protected int debug = 0;
  protected Long expires = null;
  protected boolean isVirtualWebappRelative = false;
  protected Pattern contentTypeRegEx = null;
  protected Pattern shtmlRegEx = Pattern.compile("text/x-server-parsed-html(;.*)?");
  protected boolean allowExec = false;
  
  public SSIFilter() {}
  
  public void init(FilterConfig config)
    throws ServletException
  {
    this.config = config;
    if (config.getInitParameter("debug") != null) {
      this.debug = Integer.parseInt(config.getInitParameter("debug"));
    }
    if (config.getInitParameter("contentType") != null) {
      this.contentTypeRegEx = Pattern.compile(config.getInitParameter("contentType"));
    } else {
      this.contentTypeRegEx = this.shtmlRegEx;
    }
    this.isVirtualWebappRelative = Boolean.parseBoolean(config.getInitParameter("isVirtualWebappRelative"));
    if (config.getInitParameter("expires") != null) {
      this.expires = Long.valueOf(config.getInitParameter("expires"));
    }
    this.allowExec = Boolean.parseBoolean(config.getInitParameter("allowExec"));
    if (this.debug > 0) {
      config.getServletContext().log("SSIFilter.init() SSI invoker started with 'debug'=" + this.debug);
    }
  }
  
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest)request;
    HttpServletResponse res = (HttpServletResponse)response;
    
    req.setAttribute("org.apache.catalina.ssi.SSIServlet", "true");
    
    ByteArrayServletOutputStream basos = new ByteArrayServletOutputStream();
    ResponseIncludeWrapper responseIncludeWrapper = new ResponseIncludeWrapper(this.config.getServletContext(), req, res, basos);
    
    chain.doFilter(req, responseIncludeWrapper);
    
    responseIncludeWrapper.flushOutputStreamOrWriter();
    byte[] bytes = basos.toByteArray();
    
    String contentType = responseIncludeWrapper.getContentType();
    if (this.contentTypeRegEx.matcher(contentType).matches())
    {
      String encoding = res.getCharacterEncoding();
      
      SSIExternalResolver ssiExternalResolver = new SSIServletExternalResolver(this.config.getServletContext(), req, res, this.isVirtualWebappRelative, this.debug, encoding);
      
      SSIProcessor ssiProcessor = new SSIProcessor(ssiExternalResolver, this.debug, this.allowExec);
      
      Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), encoding);
      
      ByteArrayOutputStream ssiout = new ByteArrayOutputStream();
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(ssiout, encoding));
      
      long lastModified = ssiProcessor.process(reader, responseIncludeWrapper.getLastModified(), writer);
      
      writer.flush();
      bytes = ssiout.toByteArray();
      if (this.expires != null) {
        res.setDateHeader("expires", new Date().getTime() + this.expires.longValue() * 1000L);
      }
      if (lastModified > 0L) {
        res.setDateHeader("last-modified", lastModified);
      }
      res.setContentLength(bytes.length);
      
      Matcher shtmlMatcher = this.shtmlRegEx.matcher(responseIncludeWrapper.getContentType());
      if (shtmlMatcher.matches())
      {
        String enc = shtmlMatcher.group(1);
        res.setContentType("text/html" + (enc != null ? enc : ""));
      }
    }
    OutputStream out = null;
    try
    {
      out = res.getOutputStream();
    }
    catch (IllegalStateException e) {}
    if (out == null) {
      res.getWriter().write(new String(bytes));
    } else {
      out.write(bytes);
    }
  }
  
  public void destroy() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIFilter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */