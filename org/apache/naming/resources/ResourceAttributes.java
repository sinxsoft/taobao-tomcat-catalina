package org.apache.naming.resources;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

public class ResourceAttributes
  implements Attributes
{
  private static final long serialVersionUID = 1L;
  public static final String CREATION_DATE = "creationdate";
  public static final String ALTERNATE_CREATION_DATE = "creation-date";
  public static final String LAST_MODIFIED = "getlastmodified";
  public static final String ALTERNATE_LAST_MODIFIED = "last-modified";
  public static final String NAME = "displayname";
  public static final String TYPE = "resourcetype";
  public static final String ALTERNATE_TYPE = "content-type";
  public static final String CONTENT_TYPE = "getcontenttype";
  public static final String CONTENT_LENGTH = "getcontentlength";
  public static final String ALTERNATE_CONTENT_LENGTH = "content-length";
  public static final String ETAG = "getetag";
  public static final String ALTERNATE_ETAG = "etag";
  public static final String COLLECTION_TYPE = "<collection/>";
  protected static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
  protected static final SimpleDateFormat[] formats = { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US), new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US), new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };
  protected static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");
  
  static
  {
    format.setTimeZone(gmtZone);
    
    formats[0].setTimeZone(gmtZone);
    formats[1].setTimeZone(gmtZone);
    formats[2].setTimeZone(gmtZone);
  }
  
  public ResourceAttributes(Attributes attributes)
  {
    this.attributes = attributes;
  }
  
  protected boolean collection = false;
  protected long contentLength = -1L;
  protected long creation = -1L;
  protected Date creationDate = null;
  protected long lastModified = -1L;
  protected Date lastModifiedDate = null;
  protected String lastModifiedHttp = null;
  protected String mimeType = null;
  protected String name = null;
  protected String weakETag = null;
  protected String strongETag = null;
  protected Attributes attributes = null;
  
  public boolean isCollection()
  {
    if (this.attributes != null) {
      return "<collection/>".equals(getResourceType());
    }
    return this.collection;
  }
  
  public void setCollection(boolean collection)
  {
    this.collection = collection;
    if (this.attributes != null)
    {
      String value = "";
      if (collection) {
        value = "<collection/>";
      }
      this.attributes.put("resourcetype", value);
    }
  }
  
  public long getContentLength()
  {
    if (this.contentLength != -1L) {
      return this.contentLength;
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("getcontentlength");
      if (attribute != null) {
        try
        {
          Object value = attribute.get();
          if ((value instanceof Long)) {
            this.contentLength = ((Long)value).longValue();
          } else {
            try
            {
              this.contentLength = Long.parseLong(value.toString());
            }
            catch (NumberFormatException e) {}
          }
        }
        catch (NamingException e) {}
      }
    }
    return this.contentLength;
  }
  
  public void setContentLength(long contentLength)
  {
    this.contentLength = contentLength;
    if (this.attributes != null) {
      this.attributes.put("getcontentlength", Long.valueOf(contentLength));
    }
  }
  
  public long getCreation()
  {
    if (this.creation != -1L) {
      return this.creation;
    }
    if (this.creationDate != null) {
      return this.creationDate.getTime();
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("creationdate");
      if (attribute != null) {
        try
        {
          Object value = attribute.get();
          if ((value instanceof Long))
          {
            this.creation = ((Long)value).longValue();
          }
          else if ((value instanceof Date))
          {
            this.creation = ((Date)value).getTime();
            this.creationDate = ((Date)value);
          }
          else
          {
            String creationDateValue = value.toString();
            Date result = null;
            for (int i = 0; (result == null) && (i < formats.length); i++) {
              try
              {
                result = formats[i].parse(creationDateValue);
              }
              catch (ParseException e) {}
            }
            if (result != null)
            {
              this.creation = result.getTime();
              this.creationDate = result;
            }
          }
        }
        catch (NamingException e) {}
      }
    }
    return this.creation;
  }
  
  @Deprecated
  public void setCreation(long creation)
  {
    this.creation = creation;
    this.creationDate = null;
    if (this.attributes != null) {
      this.attributes.put("creationdate", new Date(creation));
    }
  }
  
  public Date getCreationDate()
  {
    if (this.creationDate != null) {
      return this.creationDate;
    }
    if (this.creation != -1L)
    {
      this.creationDate = new Date(this.creation);
      return this.creationDate;
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("creationdate");
      if (attribute != null) {
        try
        {
          Object value = attribute.get();
          if ((value instanceof Long))
          {
            this.creation = ((Long)value).longValue();
            this.creationDate = new Date(this.creation);
          }
          else if ((value instanceof Date))
          {
            this.creation = ((Date)value).getTime();
            this.creationDate = ((Date)value);
          }
          else
          {
            String creationDateValue = value.toString();
            Date result = null;
            for (int i = 0; (result == null) && (i < formats.length); i++) {
              try
              {
                result = formats[i].parse(creationDateValue);
              }
              catch (ParseException e) {}
            }
            if (result != null)
            {
              this.creation = result.getTime();
              this.creationDate = result;
            }
          }
        }
        catch (NamingException e) {}
      }
    }
    return this.creationDate;
  }
  
  public void setCreationDate(Date creationDate)
  {
    this.creation = creationDate.getTime();
    this.creationDate = creationDate;
    if (this.attributes != null) {
      this.attributes.put("creationdate", creationDate);
    }
  }
  
  public long getLastModified()
  {
    if (this.lastModified != -1L) {
      return this.lastModified;
    }
    if (this.lastModifiedDate != null) {
      return this.lastModifiedDate.getTime();
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("getlastmodified");
      if (attribute != null) {
        try
        {
          Object value = attribute.get();
          if ((value instanceof Long))
          {
            this.lastModified = ((Long)value).longValue();
          }
          else if ((value instanceof Date))
          {
            this.lastModified = ((Date)value).getTime();
            this.lastModifiedDate = ((Date)value);
          }
          else
          {
            String lastModifiedDateValue = value.toString();
            Date result = null;
            for (int i = 0; (result == null) && (i < formats.length); i++) {
              try
              {
                result = formats[i].parse(lastModifiedDateValue);
              }
              catch (ParseException e) {}
            }
            if (result != null)
            {
              this.lastModified = result.getTime();
              this.lastModifiedDate = result;
            }
          }
        }
        catch (NamingException e) {}
      }
    }
    return this.lastModified;
  }
  
  public void setLastModified(long lastModified)
  {
    this.lastModified = lastModified;
    this.lastModifiedDate = null;
    if (this.attributes != null) {
      this.attributes.put("getlastmodified", new Date(lastModified));
    }
  }
  
  public Date getLastModifiedDate()
  {
    if (this.lastModifiedDate != null) {
      return this.lastModifiedDate;
    }
    if (this.lastModified != -1L)
    {
      this.lastModifiedDate = new Date(this.lastModified);
      return this.lastModifiedDate;
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("getlastmodified");
      if (attribute != null) {
        try
        {
          Object value = attribute.get();
          if ((value instanceof Long))
          {
            this.lastModified = ((Long)value).longValue();
            this.lastModifiedDate = new Date(this.lastModified);
          }
          else if ((value instanceof Date))
          {
            this.lastModified = ((Date)value).getTime();
            this.lastModifiedDate = ((Date)value);
          }
          else
          {
            String lastModifiedDateValue = value.toString();
            Date result = null;
            for (int i = 0; (result == null) && (i < formats.length); i++) {
              try
              {
                result = formats[i].parse(lastModifiedDateValue);
              }
              catch (ParseException e) {}
            }
            if (result != null)
            {
              this.lastModified = result.getTime();
              this.lastModifiedDate = result;
            }
          }
        }
        catch (NamingException e) {}
      }
    }
    return this.lastModifiedDate;
  }
  
  @Deprecated
  public void setLastModifiedDate(Date lastModifiedDate)
  {
    this.lastModified = lastModifiedDate.getTime();
    this.lastModifiedDate = lastModifiedDate;
    if (this.attributes != null) {
      this.attributes.put("getlastmodified", lastModifiedDate);
    }
  }
  
  public String getLastModifiedHttp()
  {
    if (this.lastModifiedHttp != null) {
      return this.lastModifiedHttp;
    }
    Date modifiedDate = getLastModifiedDate();
    if (modifiedDate == null) {
      modifiedDate = getCreationDate();
    }
    if (modifiedDate == null) {
      modifiedDate = new Date();
    }
    synchronized (format)
    {
      this.lastModifiedHttp = format.format(modifiedDate);
    }
    return this.lastModifiedHttp;
  }
  
  @Deprecated
  public void setLastModifiedHttp(String lastModifiedHttp)
  {
    this.lastModifiedHttp = lastModifiedHttp;
  }
  
  public String getMimeType()
  {
    return this.mimeType;
  }
  
  public void setMimeType(String mimeType)
  {
    this.mimeType = mimeType;
  }
  
  public String getName()
  {
    if (this.name != null) {
      return this.name;
    }
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("displayname");
      if (attribute != null) {
        try
        {
          this.name = attribute.get().toString();
        }
        catch (NamingException e) {}
      }
    }
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
    if (this.attributes != null) {
      this.attributes.put("displayname", name);
    }
  }
  
  public String getResourceType()
  {
    String result = null;
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("resourcetype");
      if (attribute != null) {
        try
        {
          result = attribute.get().toString();
        }
        catch (NamingException e) {}
      }
    }
    if ((result == null) && 
      (this.collection)) {
      result = "<collection/>";
    }
    return result;
  }
  
  public void setResourceType(String resourceType)
  {
    this.collection = resourceType.equals("<collection/>");
    if (this.attributes != null) {
      this.attributes.put("resourcetype", resourceType);
    }
  }
  
  public String getETag()
  {
    String result = null;
    if (this.attributes != null)
    {
      Attribute attribute = this.attributes.get("getetag");
      if (attribute != null) {
        try
        {
          result = attribute.get().toString();
        }
        catch (NamingException e) {}
      }
    }
    if (result == null) {
      if (this.strongETag != null)
      {
        result = this.strongETag;
      }
      else
      {
        if (this.weakETag == null)
        {
          long contentLength = getContentLength();
          long lastModified = getLastModified();
          if ((contentLength >= 0L) || (lastModified >= 0L)) {
            this.weakETag = ("W/\"" + contentLength + "-" + lastModified + "\"");
          }
        }
        result = this.weakETag;
      }
    }
    return result;
  }
  
  public void setETag(String eTag)
  {
    this.strongETag = eTag;
    if (this.attributes != null) {
      this.attributes.put("getetag", eTag);
    }
  }
  
  public String getCanonicalPath()
  {
    return null;
  }
  
  public Attribute get(String attrID)
  {
    if (this.attributes == null)
    {
      if (attrID.equals("creationdate"))
      {
        Date creationDate = getCreationDate();
        if (creationDate == null) {
          return null;
        }
        return new BasicAttribute("creationdate", creationDate);
      }
      if (attrID.equals("creation-date"))
      {
        Date creationDate = getCreationDate();
        if (creationDate == null) {
          return null;
        }
        return new BasicAttribute("creation-date", creationDate);
      }
      if (attrID.equals("getlastmodified"))
      {
        Date lastModifiedDate = getLastModifiedDate();
        if (lastModifiedDate == null) {
          return null;
        }
        return new BasicAttribute("getlastmodified", lastModifiedDate);
      }
      if (attrID.equals("last-modified"))
      {
        Date lastModifiedDate = getLastModifiedDate();
        if (lastModifiedDate == null) {
          return null;
        }
        return new BasicAttribute("last-modified", lastModifiedDate);
      }
      if (attrID.equals("displayname"))
      {
        String name = getName();
        if (name == null) {
          return null;
        }
        return new BasicAttribute("displayname", name);
      }
      if (attrID.equals("resourcetype"))
      {
        String resourceType = getResourceType();
        if (resourceType == null) {
          return null;
        }
        return new BasicAttribute("resourcetype", resourceType);
      }
      if (attrID.equals("content-type"))
      {
        String resourceType = getResourceType();
        if (resourceType == null) {
          return null;
        }
        return new BasicAttribute("content-type", resourceType);
      }
      if (attrID.equals("getcontentlength"))
      {
        long contentLength = getContentLength();
        if (contentLength < 0L) {
          return null;
        }
        return new BasicAttribute("getcontentlength", Long.valueOf(contentLength));
      }
      if (attrID.equals("content-length"))
      {
        long contentLength = getContentLength();
        if (contentLength < 0L) {
          return null;
        }
        return new BasicAttribute("content-length", Long.valueOf(contentLength));
      }
      if (attrID.equals("getetag"))
      {
        String etag = getETag();
        if (etag == null) {
          return null;
        }
        return new BasicAttribute("getetag", etag);
      }
      if (attrID.equals("etag"))
      {
        String etag = getETag();
        if (etag == null) {
          return null;
        }
        return new BasicAttribute("etag", etag);
      }
    }
    else
    {
      return this.attributes.get(attrID);
    }
    return null;
  }
  
  public Attribute put(Attribute attribute)
  {
    if (this.attributes == null) {
      try
      {
        return put(attribute.getID(), attribute.get());
      }
      catch (NamingException e)
      {
        return null;
      }
    }
    return this.attributes.put(attribute);
  }
  
  public Attribute put(String attrID, Object val)
  {
    if (this.attributes == null) {
      return null;
    }
    return this.attributes.put(attrID, val);
  }
  
  public Attribute remove(String attrID)
  {
    if (this.attributes == null) {
      return null;
    }
    return this.attributes.remove(attrID);
  }
  
  public NamingEnumeration<? extends Attribute> getAll()
  {
    if (this.attributes == null)
    {
      Vector<BasicAttribute> attributes = new Vector();
      Date creationDate = getCreationDate();
      if (creationDate != null)
      {
        attributes.addElement(new BasicAttribute("creationdate", creationDate));
        
        attributes.addElement(new BasicAttribute("creation-date", creationDate));
      }
      Date lastModifiedDate = getLastModifiedDate();
      if (lastModifiedDate != null)
      {
        attributes.addElement(new BasicAttribute("getlastmodified", lastModifiedDate));
        
        attributes.addElement(new BasicAttribute("last-modified", lastModifiedDate));
      }
      String name = getName();
      if (name != null) {
        attributes.addElement(new BasicAttribute("displayname", name));
      }
      String resourceType = getResourceType();
      if (resourceType != null)
      {
        attributes.addElement(new BasicAttribute("resourcetype", resourceType));
        attributes.addElement(new BasicAttribute("content-type", resourceType));
      }
      long contentLength = getContentLength();
      if (contentLength >= 0L)
      {
        Long contentLengthLong = Long.valueOf(contentLength);
        attributes.addElement(new BasicAttribute("getcontentlength", contentLengthLong));
        attributes.addElement(new BasicAttribute("content-length", contentLengthLong));
      }
      String etag = getETag();
      if (etag != null)
      {
        attributes.addElement(new BasicAttribute("getetag", etag));
        attributes.addElement(new BasicAttribute("etag", etag));
      }
      return new RecyclableNamingEnumeration(attributes);
    }
    return this.attributes.getAll();
  }
  
  public NamingEnumeration<String> getIDs()
  {
    if (this.attributes == null)
    {
      Vector<String> attributeIDs = new Vector();
      Date creationDate = getCreationDate();
      if (creationDate != null)
      {
        attributeIDs.addElement("creationdate");
        attributeIDs.addElement("creation-date");
      }
      Date lastModifiedDate = getLastModifiedDate();
      if (lastModifiedDate != null)
      {
        attributeIDs.addElement("getlastmodified");
        attributeIDs.addElement("last-modified");
      }
      if (getName() != null) {
        attributeIDs.addElement("displayname");
      }
      String resourceType = getResourceType();
      if (resourceType != null)
      {
        attributeIDs.addElement("resourcetype");
        attributeIDs.addElement("content-type");
      }
      long contentLength = getContentLength();
      if (contentLength >= 0L)
      {
        attributeIDs.addElement("getcontentlength");
        attributeIDs.addElement("content-length");
      }
      String etag = getETag();
      if (etag != null)
      {
        attributeIDs.addElement("getetag");
        attributeIDs.addElement("etag");
      }
      return new RecyclableNamingEnumeration(attributeIDs);
    }
    return this.attributes.getIDs();
  }
  
  public int size()
  {
    if (this.attributes == null)
    {
      int size = 0;
      if (getCreationDate() != null) {
        size += 2;
      }
      if (getLastModifiedDate() != null) {
        size += 2;
      }
      if (getName() != null) {
        size++;
      }
      if (getResourceType() != null) {
        size += 2;
      }
      if (getContentLength() >= 0L) {
        size += 2;
      }
      if (getETag() != null) {
        size += 2;
      }
      return size;
    }
    return this.attributes.size();
  }
  
  public Object clone()
  {
    return this;
  }
  
  public boolean isCaseIgnored()
  {
    return false;
  }
  
  public ResourceAttributes() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\ResourceAttributes.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */