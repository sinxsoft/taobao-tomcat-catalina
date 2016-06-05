package org.apache.catalina.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.Part;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemHeaders;
import org.apache.tomcat.util.http.fileupload.ParameterParser;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;

public class ApplicationPart
  implements Part
{
  private final FileItem fileItem;
  private final File location;
  
  public ApplicationPart(FileItem fileItem, File location)
  {
    this.fileItem = fileItem;
    this.location = location;
  }
  
  public void delete()
    throws IOException
  {
    this.fileItem.delete();
  }
  
  public String getContentType()
  {
    return this.fileItem.getContentType();
  }
  
  public String getHeader(String name)
  {
    if ((this.fileItem instanceof DiskFileItem)) {
      return ((DiskFileItem)this.fileItem).getHeaders().getHeader(name);
    }
    return null;
  }
  
  public Collection<String> getHeaderNames()
  {
    if ((this.fileItem instanceof DiskFileItem))
    {
      HashSet<String> headerNames = new HashSet();
      Iterator<String> iter = ((DiskFileItem)this.fileItem).getHeaders().getHeaderNames();
      while (iter.hasNext()) {
        headerNames.add(iter.next());
      }
      return headerNames;
    }
    return Collections.emptyList();
  }
  
  public Collection<String> getHeaders(String name)
  {
    if ((this.fileItem instanceof DiskFileItem))
    {
      HashSet<String> headers = new HashSet();
      Iterator<String> iter = ((DiskFileItem)this.fileItem).getHeaders().getHeaders(name);
      while (iter.hasNext()) {
        headers.add(iter.next());
      }
      return headers;
    }
    return Collections.emptyList();
  }
  
  public InputStream getInputStream()
    throws IOException
  {
    return this.fileItem.getInputStream();
  }
  
  public String getName()
  {
    return this.fileItem.getFieldName();
  }
  
  public long getSize()
  {
    return this.fileItem.getSize();
  }
  
  public void write(String fileName)
    throws IOException
  {
    File file = new File(fileName);
    if (!file.isAbsolute()) {
      file = new File(this.location, fileName);
    }
    try
    {
      this.fileItem.write(file);
    }
    catch (Exception e)
    {
      throw new IOException(e);
    }
  }
  
  public String getString(String encoding)
    throws UnsupportedEncodingException
  {
    return this.fileItem.getString(encoding);
  }
  
  @Deprecated
  public String getFilename()
  {
    return getSubmittedFileName();
  }
  
  public String getSubmittedFileName()
  {
    String fileName = null;
    String cd = getHeader("Content-Disposition");
    if (cd != null)
    {
      String cdl = cd.toLowerCase(Locale.ENGLISH);
      if ((cdl.startsWith("form-data")) || (cdl.startsWith("attachment")))
      {
        ParameterParser paramParser = new ParameterParser();
        paramParser.setLowerCaseNames(true);
        
        Map<String, String> params = paramParser.parse(cd, ';');
        if (params.containsKey("filename"))
        {
          fileName = (String)params.get("filename");
          if (fileName != null) {
            fileName = fileName.trim();
          } else {
            fileName = "";
          }
        }
      }
    }
    return fileName;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\core\ApplicationPart.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */