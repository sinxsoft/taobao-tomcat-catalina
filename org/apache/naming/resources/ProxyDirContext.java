package org.apache.naming.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.naming.StringManager;

public class ProxyDirContext
  implements DirContext
{
  public static final String CONTEXT = "context";
  public static final String HOST = "host";
  protected static final NameNotFoundException NOT_FOUND_EXCEPTION = new ImmutableNameNotFoundException();
  
  public ProxyDirContext(Hashtable<String, String> env, DirContext dirContext)
  {
    this.env = env;
    this.dirContext = dirContext;
    if ((dirContext instanceof BaseDirContext))
    {
      BaseDirContext baseDirContext = (BaseDirContext)dirContext;
      if (baseDirContext.isCached())
      {
        try
        {
          this.cache = ((ResourceCache)Class.forName(this.cacheClassName).newInstance());
        }
        catch (Exception e)
        {
          throw new IllegalArgumentException(sm.getString("resources.invalidCache", new Object[] { this.cacheClassName }), e);
        }
        this.cache.setCacheMaxSize(baseDirContext.getCacheMaxSize());
        this.cacheTTL = baseDirContext.getCacheTTL();
        this.cacheObjectMaxSize = baseDirContext.getCacheObjectMaxSize();
        if (this.cacheObjectMaxSize > baseDirContext.getCacheMaxSize() / 20) {
          this.cacheObjectMaxSize = (baseDirContext.getCacheMaxSize() / 20);
        }
      }
    }
    this.hostName = ((String)env.get("host"));
    this.contextName = ((String)env.get("context"));
    int i = this.contextName.indexOf('#');
    if (i == -1) {
      this.contextPath = this.contextName;
    } else {
      this.contextPath = this.contextName.substring(0, i);
    }
  }
  
  protected ProxyDirContext proxy = this;
  protected Hashtable<String, String> env;
  protected static final StringManager sm = StringManager.getManager("org.apache.naming.resources");
  protected DirContext dirContext;
  protected String vPath = null;
  protected String hostName;
  protected String contextName;
  protected String contextPath;
  protected String cacheClassName = "org.apache.naming.resources.ResourceCache";
  protected ResourceCache cache = null;
  protected int cacheTTL = 5000;
  protected int cacheObjectMaxSize = 512;
  protected String[] nonCacheable = { "/WEB-INF/lib/", "/WEB-INF/classes/" };
  
  public ResourceCache getCache()
  {
    return this.cache;
  }
  
  @Deprecated
  public DirContext getDirContext()
  {
    return this.dirContext;
  }
  
  @Deprecated
  public String getDocBase()
  {
    if ((this.dirContext instanceof BaseDirContext)) {
      return ((BaseDirContext)this.dirContext).getDocBase();
    }
    return "";
  }
  
  public String getHostName()
  {
    return this.hostName;
  }
  
  public String getContextName()
  {
    return this.contextName;
  }
  
  public String getContextPath()
  {
    return this.contextPath;
  }
  
  public Object lookup(Name name)
    throws NamingException
  {
    CacheEntry entry = cacheLookup(name.toString());
    if (entry != null)
    {
      if (!entry.exists) {
        throw NOT_FOUND_EXCEPTION;
      }
      if (entry.resource != null) {
        return entry.resource;
      }
      return entry.context;
    }
    Object object = this.dirContext.lookup(parseName(name));
    if ((object instanceof InputStream)) {
      return new Resource((InputStream)object);
    }
    return object;
  }
  
  public Object lookup(String name)
    throws NamingException
  {
    CacheEntry entry = cacheLookup(name);
    if (entry != null)
    {
      if (!entry.exists) {
        throw NOT_FOUND_EXCEPTION;
      }
      if (entry.resource != null) {
        return entry.resource;
      }
      return entry.context;
    }
    Object object = this.dirContext.lookup(parseName(name));
    if ((object instanceof InputStream)) {
      return new Resource((InputStream)object);
    }
    if ((object instanceof DirContext)) {
      return object;
    }
    if ((object instanceof Resource)) {
      return object;
    }
    return new Resource(new ByteArrayInputStream(object.toString().getBytes(Charset.defaultCharset())));
  }
  
  public void bind(Name name, Object obj)
    throws NamingException
  {
    this.dirContext.bind(parseName(name), obj);
    cacheUnload(name.toString());
  }
  
  public void bind(String name, Object obj)
    throws NamingException
  {
    this.dirContext.bind(parseName(name), obj);
    cacheUnload(name);
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    this.dirContext.rebind(parseName(name), obj);
    cacheUnload(name.toString());
  }
  
  public void rebind(String name, Object obj)
    throws NamingException
  {
    this.dirContext.rebind(parseName(name), obj);
    cacheUnload(name);
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    this.dirContext.unbind(parseName(name));
    cacheUnload(name.toString());
  }
  
  public void unbind(String name)
    throws NamingException
  {
    this.dirContext.unbind(parseName(name));
    cacheUnload(name);
  }
  
  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    this.dirContext.rename(parseName(oldName), parseName(newName));
    cacheUnload(oldName.toString());
  }
  
  public void rename(String oldName, String newName)
    throws NamingException
  {
    this.dirContext.rename(parseName(oldName), parseName(newName));
    cacheUnload(oldName);
  }
  
  public NamingEnumeration<NameClassPair> list(Name name)
    throws NamingException
  {
    return this.dirContext.list(parseName(name));
  }
  
  public NamingEnumeration<NameClassPair> list(String name)
    throws NamingException
  {
    return this.dirContext.list(parseName(name));
  }
  
  public NamingEnumeration<Binding> listBindings(Name name)
    throws NamingException
  {
    return this.dirContext.listBindings(parseName(name));
  }
  
  public NamingEnumeration<Binding> listBindings(String name)
    throws NamingException
  {
    return this.dirContext.listBindings(parseName(name));
  }
  
  public void destroySubcontext(Name name)
    throws NamingException
  {
    this.dirContext.destroySubcontext(parseName(name));
    cacheUnload(name.toString());
  }
  
  public void destroySubcontext(String name)
    throws NamingException
  {
    this.dirContext.destroySubcontext(parseName(name));
    cacheUnload(name);
  }
  
  public Context createSubcontext(Name name)
    throws NamingException
  {
    Context context = this.dirContext.createSubcontext(parseName(name));
    cacheUnload(name.toString());
    return context;
  }
  
  public Context createSubcontext(String name)
    throws NamingException
  {
    Context context = this.dirContext.createSubcontext(parseName(name));
    cacheUnload(name);
    return context;
  }
  
  public Object lookupLink(Name name)
    throws NamingException
  {
    return this.dirContext.lookupLink(parseName(name));
  }
  
  public Object lookupLink(String name)
    throws NamingException
  {
    return this.dirContext.lookupLink(parseName(name));
  }
  
  public NameParser getNameParser(Name name)
    throws NamingException
  {
    return this.dirContext.getNameParser(parseName(name));
  }
  
  public NameParser getNameParser(String name)
    throws NamingException
  {
    return this.dirContext.getNameParser(parseName(name));
  }
  
  public Name composeName(Name name, Name prefix)
    throws NamingException
  {
    Name prefixClone = (Name)prefix.clone();
    return prefixClone.addAll(name);
  }
  
  public String composeName(String name, String prefix)
    throws NamingException
  {
    return prefix + "/" + name;
  }
  
  public Object addToEnvironment(String propName, Object propVal)
    throws NamingException
  {
    return this.dirContext.addToEnvironment(propName, propVal);
  }
  
  public Object removeFromEnvironment(String propName)
    throws NamingException
  {
    return this.dirContext.removeFromEnvironment(propName);
  }
  
  public Hashtable<?, ?> getEnvironment()
    throws NamingException
  {
    return this.dirContext.getEnvironment();
  }
  
  public void close()
    throws NamingException
  {
    this.dirContext.close();
  }
  
  public String getNameInNamespace()
    throws NamingException
  {
    return this.dirContext.getNameInNamespace();
  }
  
  public Attributes getAttributes(Name name)
    throws NamingException
  {
    CacheEntry entry = cacheLookup(name.toString());
    if (entry != null)
    {
      if (!entry.exists) {
        throw NOT_FOUND_EXCEPTION;
      }
      return entry.attributes;
    }
    Attributes attributes = this.dirContext.getAttributes(parseName(name));
    if (!(attributes instanceof ResourceAttributes)) {
      attributes = new ResourceAttributes(attributes);
    }
    return attributes;
  }
  
  public Attributes getAttributes(String name)
    throws NamingException
  {
    CacheEntry entry = cacheLookup(name);
    if (entry != null)
    {
      if (!entry.exists) {
        throw NOT_FOUND_EXCEPTION;
      }
      return entry.attributes;
    }
    Attributes attributes = this.dirContext.getAttributes(parseName(name));
    if (!(attributes instanceof ResourceAttributes)) {
      attributes = new ResourceAttributes(attributes);
    }
    return attributes;
  }
  
  public Attributes getAttributes(Name name, String[] attrIds)
    throws NamingException
  {
    Attributes attributes = this.dirContext.getAttributes(parseName(name), attrIds);
    if (!(attributes instanceof ResourceAttributes)) {
      attributes = new ResourceAttributes(attributes);
    }
    return attributes;
  }
  
  public Attributes getAttributes(String name, String[] attrIds)
    throws NamingException
  {
    Attributes attributes = this.dirContext.getAttributes(parseName(name), attrIds);
    if (!(attributes instanceof ResourceAttributes)) {
      attributes = new ResourceAttributes(attributes);
    }
    return attributes;
  }
  
  public void modifyAttributes(Name name, int mod_op, Attributes attrs)
    throws NamingException
  {
    this.dirContext.modifyAttributes(parseName(name), mod_op, attrs);
    cacheUnload(name.toString());
  }
  
  public void modifyAttributes(String name, int mod_op, Attributes attrs)
    throws NamingException
  {
    this.dirContext.modifyAttributes(parseName(name), mod_op, attrs);
    cacheUnload(name);
  }
  
  public void modifyAttributes(Name name, ModificationItem[] mods)
    throws NamingException
  {
    this.dirContext.modifyAttributes(parseName(name), mods);
    cacheUnload(name.toString());
  }
  
  public void modifyAttributes(String name, ModificationItem[] mods)
    throws NamingException
  {
    this.dirContext.modifyAttributes(parseName(name), mods);
    cacheUnload(name);
  }
  
  public void bind(Name name, Object obj, Attributes attrs)
    throws NamingException
  {
    this.dirContext.bind(parseName(name), obj, attrs);
    cacheUnload(name.toString());
  }
  
  public void bind(String name, Object obj, Attributes attrs)
    throws NamingException
  {
    this.dirContext.bind(parseName(name), obj, attrs);
    cacheUnload(name);
  }
  
  public void rebind(Name name, Object obj, Attributes attrs)
    throws NamingException
  {
    this.dirContext.rebind(parseName(name), obj, attrs);
    cacheUnload(name.toString());
  }
  
  public void rebind(String name, Object obj, Attributes attrs)
    throws NamingException
  {
    this.dirContext.rebind(parseName(name), obj, attrs);
    cacheUnload(name);
  }
  
  public DirContext createSubcontext(Name name, Attributes attrs)
    throws NamingException
  {
    DirContext context = this.dirContext.createSubcontext(parseName(name), attrs);
    
    cacheUnload(name.toString());
    return context;
  }
  
  public DirContext createSubcontext(String name, Attributes attrs)
    throws NamingException
  {
    DirContext context = this.dirContext.createSubcontext(parseName(name), attrs);
    
    cacheUnload(name);
    return context;
  }
  
  public DirContext getSchema(Name name)
    throws NamingException
  {
    return this.dirContext.getSchema(parseName(name));
  }
  
  public DirContext getSchema(String name)
    throws NamingException
  {
    return this.dirContext.getSchema(parseName(name));
  }
  
  public DirContext getSchemaClassDefinition(Name name)
    throws NamingException
  {
    return this.dirContext.getSchemaClassDefinition(parseName(name));
  }
  
  public DirContext getSchemaClassDefinition(String name)
    throws NamingException
  {
    return this.dirContext.getSchemaClassDefinition(parseName(name));
  }
  
  public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), matchingAttributes, attributesToReturn);
  }
  
  public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), matchingAttributes, attributesToReturn);
  }
  
  public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), matchingAttributes);
  }
  
  public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), matchingAttributes);
  }
  
  public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), filter, cons);
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), filter, cons);
  }
  
  public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), filterExpr, filterArgs, cons);
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException
  {
    return this.dirContext.search(parseName(name), filterExpr, filterArgs, cons);
  }
  
  public CacheEntry lookupCache(String name)
  {
    CacheEntry entry = cacheLookup(name);
    if (entry == null)
    {
      entry = new CacheEntry();
      entry.name = name;
      try
      {
        Object object = this.dirContext.lookup(parseName(name));
        if ((object instanceof InputStream)) {
          entry.resource = new Resource((InputStream)object);
        } else if ((object instanceof DirContext)) {
          entry.context = ((DirContext)object);
        } else if ((object instanceof Resource)) {
          entry.resource = ((Resource)object);
        } else {
          entry.resource = new Resource(new ByteArrayInputStream(object.toString().getBytes(Charset.defaultCharset())));
        }
        Attributes attributes = this.dirContext.getAttributes(parseName(name));
        if (!(attributes instanceof ResourceAttributes)) {
          attributes = new ResourceAttributes(attributes);
        }
        entry.attributes = ((ResourceAttributes)attributes);
      }
      catch (NamingException e)
      {
        entry.exists = false;
      }
    }
    return entry;
  }
  
  protected String parseName(String name)
    throws NamingException
  {
    return name;
  }
  
  protected Name parseName(Name name)
    throws NamingException
  {
    return name;
  }
  
  protected CacheEntry cacheLookup(String lookupName)
  {
    if (this.cache == null) {
      return null;
    }
    String name;

    if (lookupName == null) {
      name = "";
    } else {
      name = lookupName;
    }
    for (int i = 0; i < this.nonCacheable.length; i++) {
      if (name.startsWith(this.nonCacheable[i])) {
        return null;
      }
    }
    CacheEntry cacheEntry = this.cache.lookup(name);
    if (cacheEntry == null)
    {
      cacheEntry = new CacheEntry();
      cacheEntry.name = name;
      
      cacheLoad(cacheEntry);
    }
    else
    {
      if (!validate(cacheEntry))
      {
        if (!revalidate(cacheEntry))
        {
          cacheUnload(cacheEntry.name);
          return null;
        }
        cacheEntry.timestamp = (System.currentTimeMillis() + this.cacheTTL);
      }
      cacheEntry.accessCount += 1L;
    }
    return cacheEntry;
  }
  
  protected boolean validate(CacheEntry entry)
  {
    if (((!entry.exists) || (entry.context != null) || ((entry.resource != null) && (entry.resource.getContent() != null))) && (System.currentTimeMillis() < entry.timestamp)) {
      return true;
    }
    return false;
  }
  
  protected boolean revalidate(CacheEntry entry)
  {
    if (!entry.exists) {
      return false;
    }
    if (entry.attributes == null) {
      return false;
    }
    long lastModified = entry.attributes.getLastModified();
    long contentLength = entry.attributes.getContentLength();
    if (lastModified <= 0L) {
      return false;
    }
    try
    {
      Attributes tempAttributes = this.dirContext.getAttributes(entry.name);
      ResourceAttributes attributes = null;
      if (!(tempAttributes instanceof ResourceAttributes)) {
        attributes = new ResourceAttributes(tempAttributes);
      } else {
        attributes = (ResourceAttributes)tempAttributes;
      }
      long lastModified2 = attributes.getLastModified();
      long contentLength2 = attributes.getContentLength();
      return (lastModified == lastModified2) && (contentLength == contentLength2);
    }
    catch (NamingException e) {}
    return false;
  }
  
  protected void cacheLoad(CacheEntry entry)
  {
    String name = entry.name;
    
    boolean exists = true;
    if (entry.attributes == null) {
      try
      {
        Attributes attributes = this.dirContext.getAttributes(name);
        if (!(attributes instanceof ResourceAttributes)) {
          entry.attributes = new ResourceAttributes(attributes);
        } else {
          entry.attributes = ((ResourceAttributes)attributes);
        }
      }
      catch (NamingException e)
      {
        exists = false;
      }
    }
    if ((exists) && (entry.resource == null) && (entry.context == null)) {
      try
      {
        Object object = this.dirContext.lookup(name);
        if ((object instanceof InputStream)) {
          entry.resource = new Resource((InputStream)object);
        } else if ((object instanceof DirContext)) {
          entry.context = ((DirContext)object);
        } else if ((object instanceof Resource)) {
          entry.resource = ((Resource)object);
        } else {
          entry.resource = new Resource(new ByteArrayInputStream(object.toString().getBytes(Charset.defaultCharset())));
        }
      }
      catch (NamingException e)
      {
        exists = false;
      }
    }
    if ((exists) && (entry.resource != null) && (entry.resource.getContent() == null) && (entry.attributes.getContentLength() >= 0L) && (entry.attributes.getContentLength() < this.cacheObjectMaxSize * 1024))
    {
      int length = (int)entry.attributes.getContentLength(); CacheEntry 
      
        tmp263_262 = entry;tmp263_262.size = ((int)(tmp263_262.size + entry.attributes.getContentLength() / 1024L));
      InputStream is = null;
      try
      {
        is = entry.resource.streamContent();
        int pos = 0;
        byte[] b = new byte[length];
        while (pos < length)
        {
          int n = is.read(b, pos, length - pos);
          if (n < 0) {
            break;
          }
          pos += n;
        }
        entry.resource.setContent(b);
        try
        {
          if (is != null) {
            is.close();
          }
        }
        catch (IOException e) {}
        entry.exists = exists;
      }
      catch (IOException e) {}finally
      {
        try
        {
          if (is != null) {
            is.close();
          }
        }
        catch (IOException e) {}
      }
    }
    entry.timestamp = (System.currentTimeMillis() + this.cacheTTL);
    synchronized (this.cache)
    {
      if ((this.cache.lookup(name) == null) && (this.cache.allocate(entry.size))) {
        this.cache.load(entry);
      }
    }
  }
  
  protected boolean cacheUnload(String name)
  {
    if (this.cache == null) {
      return false;
    }
    String name2;

    if (name.endsWith("/")) {
      name2 = name.substring(0, name.length() - 1);
    } else {
      name2 = name + "/";
    }
    synchronized (this.cache)
    {
      boolean result = this.cache.unload(name);
      this.cache.unload(name2);
      return result;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\ProxyDirContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */