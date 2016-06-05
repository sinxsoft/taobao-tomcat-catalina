package org.apache.naming.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import org.apache.naming.Constants;
import org.apache.naming.JndiPermission;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.buf.UEncoder;
import org.apache.tomcat.util.buf.UEncoder.SafeCharsSet;
import org.apache.tomcat.util.http.FastHttpDateFormat;

public class DirContextURLConnection
  extends URLConnection
{
  private static final UDecoder URL_DECODER = new UDecoder();
  protected DirContext context;
  protected Resource resource;
  protected DirContext collection;
  protected Object object;
  protected Attributes attributes;
  protected long date;
  protected Permission permission;
  
  public DirContextURLConnection(DirContext context, URL url)
  {
    super(url);
    if (context == null) {
      throw new IllegalArgumentException("Directory context can't be null");
    }
    if (Constants.IS_SECURITY_ENABLED) {
      this.permission = new JndiPermission(url.toString());
    }
    this.context = context;
  }
  
  private String path = null;
  
  public void connect()
    throws IOException
  {
    if (!this.connected)
    {
      try
      {
        this.date = System.currentTimeMillis();
        this.path = URL_DECODER.convert(getURL().getFile(), false);
        if ((this.context instanceof ProxyDirContext))
        {
          ProxyDirContext proxyDirContext = (ProxyDirContext)this.context;
          
          String hostName = proxyDirContext.getHostName();
          String contextPath = proxyDirContext.getContextPath();
          if (hostName != null)
          {
            if (!this.path.startsWith("/" + hostName + "/")) {
              return;
            }
            this.path = this.path.substring(hostName.length() + 1);
          }
          if (contextPath != null)
          {
            if (!this.path.startsWith(contextPath + "/")) {
              return;
            }
            this.path = this.path.substring(contextPath.length());
          }
        }
        this.object = this.context.lookup(this.path);
        this.attributes = this.context.getAttributes(this.path);
        if ((this.object instanceof Resource)) {
          this.resource = ((Resource)this.object);
        }
        if ((this.object instanceof DirContext)) {
          this.collection = ((DirContext)this.object);
        }
      }
      catch (NamingException e) {}
      this.connected = true;
    }
  }
  
  public int getContentLength()
  {
    return getHeaderFieldInt("getcontentlength", -1);
  }
  
  public String getContentType()
  {
    return getHeaderField("getcontenttype");
  }
  
  public long getDate()
  {
    return this.date;
  }
  
  public long getLastModified()
  {
    if (!this.connected) {
      try
      {
        connect();
      }
      catch (IOException e) {}
    }
    if (this.attributes == null) {
      return 0L;
    }
    Attribute lastModified = this.attributes.get("getlastmodified");
    if (lastModified != null) {
      try
      {
        Date lmDate = (Date)lastModified.get();
        return lmDate.getTime();
      }
      catch (Exception e) {}
    }
    return 0L;
  }
  
  protected String getHeaderValueAsString(Object headerValue)
  {
    if (headerValue == null) {
      return null;
    }
    if ((headerValue instanceof Date)) {
      return FastHttpDateFormat.formatDate(((Date)headerValue).getTime(), null);
    }
    return headerValue.toString();
  }
  
  public Map<String, List<String>> getHeaderFields()
  {
    if (!this.connected) {
      try
      {
        connect();
      }
      catch (IOException e) {}
    }
    if (this.attributes == null) {
      return Collections.emptyMap();
    }
    HashMap<String, List<String>> headerFields = new HashMap(this.attributes.size());
    
    NamingEnumeration<String> attributeEnum = this.attributes.getIDs();
    try
    {
      while (attributeEnum.hasMore())
      {
        String attributeID = (String)attributeEnum.next();
        Attribute attribute = this.attributes.get(attributeID);
        if (attribute != null)
        {
          ArrayList<String> attributeValueList = new ArrayList(attribute.size());
          
          NamingEnumeration<?> attributeValues = attribute.getAll();
          while (attributeValues.hasMore())
          {
            Object attrValue = attributeValues.next();
            attributeValueList.add(getHeaderValueAsString(attrValue));
          }
          attributeValueList.trimToSize();
          headerFields.put(attributeID, Collections.unmodifiableList(attributeValueList));
        }
      }
    }
    catch (NamingException ne) {}
    return Collections.unmodifiableMap(headerFields);
  }
  
  public String getHeaderField(String name)
  {
    if (!this.connected) {
      try
      {
        connect();
      }
      catch (IOException e) {}
    }
    if (this.attributes == null) {
      return null;
    }
    NamingEnumeration<String> attributeEnum = this.attributes.getIDs();
    try
    {
      while (attributeEnum.hasMore())
      {
        String attributeID = (String)attributeEnum.next();
        if (attributeID.equalsIgnoreCase(name))
        {
          Attribute attribute = this.attributes.get(attributeID);
          if (attribute == null) {
            return null;
          }
          Object attrValue = attribute.get(attribute.size() - 1);
          return getHeaderValueAsString(attrValue);
        }
      }
    }
    catch (NamingException ne) {}
    return null;
  }
  
  public Object getContent()
    throws IOException
  {
    if (!this.connected) {
      connect();
    }
    if (this.resource != null) {
      return getInputStream();
    }
    if (this.collection != null) {
      return this.collection;
    }
    if (this.object != null) {
      return this.object;
    }
    throw new FileNotFoundException(getURL() == null ? "null" : getURL().toString());
  }
  
  public Object getContent(Class[] classes)
    throws IOException
  {
    Object obj = getContent();
    for (int i = 0; i < classes.length; i++) {
      if (classes[i].isInstance(obj)) {
        return obj;
      }
    }
    return null;
  }
  
  public InputStream getInputStream()
    throws IOException
  {
    if (!this.connected) {
      connect();
    }
    if (this.resource == null) {
      throw new FileNotFoundException(getURL() == null ? "null" : getURL().toString());
    }
    try
    {
      this.resource = ((Resource)this.context.lookup(this.path));
    }
    catch (NamingException e) {}
    return this.resource.streamContent();
  }
  
  public Permission getPermission()
  {
    return this.permission;
  }
  
  public Enumeration<String> list()
    throws IOException
  {
    if (!this.connected) {
      connect();
    }
    if ((this.resource == null) && (this.collection == null)) {
      throw new FileNotFoundException(getURL() == null ? "null" : getURL().toString());
    }
    Vector<String> result = new Vector();
    if (this.collection != null) {
      try
      {
        NamingEnumeration<NameClassPair> enumeration = this.collection.list("/");
        
        UEncoder urlEncoder = new UEncoder(UEncoder.SafeCharsSet.WITH_SLASH);
        while (enumeration.hasMoreElements())
        {
          NameClassPair ncp = (NameClassPair)enumeration.nextElement();
          String s = ncp.getName();
          result.addElement(urlEncoder.encodeURL(s, 0, s.length()).toString());
        }
      }
      catch (NamingException e)
      {
        throw new FileNotFoundException(getURL() == null ? "null" : getURL().toString());
      }
    }
    return result.elements();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\DirContextURLConnection.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */