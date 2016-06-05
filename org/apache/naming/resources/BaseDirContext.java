package org.apache.naming.resources;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
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
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.NameParserImpl;
import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.NamingEntry;
import org.apache.naming.StringManager;

public abstract class BaseDirContext
  implements DirContext
{
  private static final Log log = LogFactory.getLog(BaseDirContext.class);
  
  public BaseDirContext()
  {
    this.env = new Hashtable();
  }
  
  public BaseDirContext(Hashtable<String, Object> env)
  {
    this.env = env;
  }
  
  protected String docBase = null;
  protected Hashtable<String, Object> env;
  protected static final StringManager sm = StringManager.getManager("org.apache.naming.resources");
  protected final NameParser nameParser = new NameParserImpl();
  protected boolean cached = true;
  protected int cacheTTL = 5000;
  protected int cacheMaxSize = 10240;
  protected int cacheObjectMaxSize = 512;
  protected Map<String, BaseDirContext> aliases = new HashMap();
  protected List<DirContext> altDirContexts = new ArrayList();
  
  public void addResourcesJar(URL url)
  {
    try
    {
      JarURLConnection conn = (JarURLConnection)url.openConnection();
      JarFile jarFile = conn.getJarFile();
      ZipEntry entry = jarFile.getEntry("/");
      WARDirContext warDirContext = new WARDirContext(jarFile, new WARDirContext.Entry("/", entry));
      
      warDirContext.loadEntries();
      this.altDirContexts.add(warDirContext);
    }
    catch (IOException ioe)
    {
      log.warn(sm.getString("resources.addResourcesJarFail", new Object[] { url }), ioe);
    }
  }
  
  public void addAltDirContext(DirContext altDirContext)
  {
    this.altDirContexts.add(altDirContext);
  }
  
  public void addAlias(String path, BaseDirContext dirContext)
  {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException(sm.getString("resources.invalidAliasPath", new Object[] { path }));
    }
    this.aliases.put(path, dirContext);
  }
  
  public void removeAlias(String path)
  {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException(sm.getString("resources.invalidAliasPath", new Object[] { path }));
    }
    this.aliases.remove(path);
  }
  
  public String getAliases()
  {
    StringBuilder result = new StringBuilder();
    Iterator<Map.Entry<String, BaseDirContext>> iter = this.aliases.entrySet().iterator();
    
    boolean first = true;
    while (iter.hasNext())
    {
      if (first) {
        first = false;
      } else {
        result.append(',');
      }
      Map.Entry<String, BaseDirContext> entry = (Map.Entry)iter.next();
      result.append((String)entry.getKey());
      result.append('=');
      result.append(((BaseDirContext)entry.getValue()).getDocBase());
    }
    return result.toString();
  }
  
  public void setAliases(String theAliases)
  {
    this.aliases.clear();
    if ((theAliases == null) || (theAliases.length() == 0)) {
      return;
    }
    String[] kvps = theAliases.split(",");
    for (String kvp : kvps)
    {
      kvp = kvp.trim();
      if (0 != kvp.length())
      {
        String[] kv = kvp.split("=");
        if (kv.length != 2) {
          throw new IllegalArgumentException(sm.getString("resources.invalidAliasMapping", new Object[] { kvp }));
        }
        kv[0] = kv[0].trim();
        kv[1] = kv[1].trim();
        if ((kv[0].length() == 0) || (kv[1].length() == 0)) {
          throw new IllegalArgumentException(sm.getString("resources.invalidAliasMapping", new Object[] { kvp }));
        }
        if (kv[0].equals("/")) {
          throw new IllegalArgumentException(sm.getString("resources.invalidAliasNotAllowed", new Object[] { kv[0] }));
        }
        File aliasLoc = new File(kv[1]);
        if (!aliasLoc.exists()) {
          throw new IllegalArgumentException(sm.getString("resources.invalidAliasNotExist", new Object[] { kv[1] }));
        }
        BaseDirContext context;
        if ((kv[1].endsWith(".war")) && (!aliasLoc.isDirectory()))
        {
          context = new WARDirContext();
        }
        else
        {

          if (aliasLoc.isDirectory()) {
            context = new FileDirContext();
          } else {
            throw new IllegalArgumentException(sm.getString("resources.invalidAliasFile", new Object[] { kv[1] }));
          }
        }

        context.setDocBase(kv[1]);
        addAlias(kv[0], context);
      }
    }
  }
  
  public String getDocBase()
  {
    return this.docBase;
  }
  
  public void setDocBase(String docBase)
  {
    if (docBase == null) {
      throw new IllegalArgumentException(sm.getString("resources.null"));
    }
    this.docBase = docBase;
  }
  
  public void setCached(boolean cached)
  {
    this.cached = cached;
  }
  
  public boolean isCached()
  {
    return this.cached;
  }
  
  public void setCacheTTL(int cacheTTL)
  {
    this.cacheTTL = cacheTTL;
  }
  
  public int getCacheTTL()
  {
    return this.cacheTTL;
  }
  
  public int getCacheMaxSize()
  {
    return this.cacheMaxSize;
  }
  
  public void setCacheMaxSize(int cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize;
  }
  
  public int getCacheObjectMaxSize()
  {
    return this.cacheObjectMaxSize;
  }
  
  public void setCacheObjectMaxSize(int cacheObjectMaxSize)
  {
    this.cacheObjectMaxSize = cacheObjectMaxSize;
  }
  
  public void allocate() {}
  
  public void release()
  {
    for (BaseDirContext bcontext : this.aliases.values()) {
      bcontext.release();
    }
    this.aliases.clear();
    for (DirContext dcontext : this.altDirContexts) {
      if ((dcontext instanceof BaseDirContext)) {
        ((BaseDirContext)dcontext).release();
      }
    }
    this.altDirContexts.clear();
  }
  
  public String getRealPath(String name)
  {
    if (!this.aliases.isEmpty())
    {
      AliasResult result = findAlias(name);
      if (result.dirContext != null) {
        return result.dirContext.doGetRealPath(result.aliasName);
      }
    }
    String path = doGetRealPath(name);
    if (path != null) {
      return path;
    }
    String resourceName = "/META-INF/resources" + name;
    for (DirContext altDirContext : this.altDirContexts) {
      if ((altDirContext instanceof BaseDirContext))
      {
        path = ((BaseDirContext)altDirContext).getRealPath(resourceName);
        if (path != null) {
          return path;
        }
      }
    }
    return null;
  }
  
  public final Object lookup(Name name)
    throws NamingException
  {
    return lookup(name.toString());
  }
  
  public final Object lookup(String name)
    throws NamingException
  {
    Object obj = doLookupWithoutNNFE(name);
    if (obj != null) {
      return obj;
    }
    throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { name }));
  }
  
  private Object doLookupWithoutNNFE(String name)
    throws NamingException
  {
    if (!this.aliases.isEmpty())
    {
      AliasResult result = findAlias(name);
      if (result.dirContext != null) {
        return result.dirContext.lookup(result.aliasName);
      }
    }
    Object obj = doLookup(name);
    if (obj != null) {
      return obj;
    }
    String resourceName = "/META-INF/resources" + name;
    for (DirContext altDirContext : this.altDirContexts)
    {
      if ((altDirContext instanceof BaseDirContext)) {
        obj = ((BaseDirContext)altDirContext).doLookupWithoutNNFE(resourceName);
      } else {
        try
        {
          obj = altDirContext.lookup(resourceName);
        }
        catch (NamingException ex) {}
      }
      if (obj != null) {
        return obj;
      }
    }
    return null;
  }
  
  public void bind(Name name, Object obj)
    throws NamingException
  {
    bind(name.toString(), obj);
  }
  
  public void bind(String name, Object obj)
    throws NamingException
  {
    bind(name, obj, null);
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    rebind(name.toString(), obj);
  }
  
  public void rebind(String name, Object obj)
    throws NamingException
  {
    rebind(name, obj, null);
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    unbind(name.toString());
  }
  
  public abstract void unbind(String paramString)
    throws NamingException;
  
  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    rename(oldName.toString(), newName.toString());
  }
  
  public abstract void rename(String paramString1, String paramString2)
    throws NamingException;
  
  public NamingEnumeration<NameClassPair> list(Name name)
    throws NamingException
  {
    return list(name.toString());
  }
  
  public NamingEnumeration<NameClassPair> list(String name)
    throws NamingException
  {
    if (!this.aliases.isEmpty())
    {
      AliasResult result = findAlias(name);
      if (result.dirContext != null) {
        return result.dirContext.list(result.aliasName);
      }
    }
    List<NamingEntry> bindings = doListBindings(name);
    
    List<NamingEntry> altBindings = null;
    
    String resourceName = "/META-INF/resources" + name;
    for (DirContext altDirContext : this.altDirContexts)
    {
      if ((altDirContext instanceof BaseDirContext)) {
        altBindings = ((BaseDirContext)altDirContext).doListBindings(resourceName);
      }
      if (altBindings != null) {
        if (bindings == null) {
          bindings = altBindings;
        } else {
          bindings.addAll(altBindings);
        }
      }
    }
    if (bindings != null) {
      return new NamingContextEnumeration(bindings.iterator());
    }
    throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { name }));
  }
  
  public final NamingEnumeration<Binding> listBindings(Name name)
    throws NamingException
  {
    return listBindings(name.toString());
  }
  
  public final NamingEnumeration<Binding> listBindings(String name)
    throws NamingException
  {
    if (!this.aliases.isEmpty())
    {
      AliasResult result = findAlias(name);
      if (result.dirContext != null) {
        return result.dirContext.listBindings(result.aliasName);
      }
    }
    List<NamingEntry> bindings = doListBindings(name);
    
    List<NamingEntry> altBindings = null;
    
    String resourceName = "/META-INF/resources" + name;
    for (DirContext altDirContext : this.altDirContexts)
    {
      if ((altDirContext instanceof BaseDirContext)) {
        altBindings = ((BaseDirContext)altDirContext).doListBindings(resourceName);
      }
      if (altBindings != null) {
        if (bindings == null) {
          bindings = altBindings;
        } else {
          bindings.addAll(altBindings);
        }
      }
    }
    if (bindings != null) {
      return new NamingContextBindingsEnumeration(bindings.iterator(), this);
    }
    throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { name }));
  }
  
  public void destroySubcontext(Name name)
    throws NamingException
  {
    destroySubcontext(name.toString());
  }
  
  public abstract void destroySubcontext(String paramString)
    throws NamingException;
  
  public Context createSubcontext(Name name)
    throws NamingException
  {
    return createSubcontext(name.toString());
  }
  
  public Context createSubcontext(String name)
    throws NamingException
  {
    return createSubcontext(name, null);
  }
  
  public Object lookupLink(Name name)
    throws NamingException
  {
    return lookupLink(name.toString());
  }
  
  public abstract Object lookupLink(String paramString)
    throws NamingException;
  
  public NameParser getNameParser(Name name)
    throws NamingException
  {
    return new NameParserImpl();
  }
  
  public NameParser getNameParser(String name)
    throws NamingException
  {
    return new NameParserImpl();
  }
  
  public Name composeName(Name name, Name prefix)
    throws NamingException
  {
    Name clone = (Name)prefix.clone();
    return clone.addAll(name);
  }
  
  public String composeName(String name, String prefix)
    throws NamingException
  {
    return prefix + "/" + name;
  }
  
  public Object addToEnvironment(String propName, Object propVal)
    throws NamingException
  {
    return this.env.put(propName, propVal);
  }
  
  public Object removeFromEnvironment(String propName)
    throws NamingException
  {
    return this.env.remove(propName);
  }
  
  public Hashtable<String, Object> getEnvironment()
    throws NamingException
  {
    return this.env;
  }
  
  public void close()
    throws NamingException
  {
    this.env.clear();
  }
  
  public abstract String getNameInNamespace()
    throws NamingException;
  
  public Attributes getAttributes(Name name)
    throws NamingException
  {
    return getAttributes(name.toString());
  }
  
  public Attributes getAttributes(String name)
    throws NamingException
  {
    return getAttributes(name, null);
  }
  
  public Attributes getAttributes(Name name, String[] attrIds)
    throws NamingException
  {
    return getAttributes(name.toString(), attrIds);
  }
  
  public final Attributes getAttributes(String name, String[] attrIds)
    throws NamingException
  {
    if (!this.aliases.isEmpty())
    {
      AliasResult result = findAlias(name);
      if (result.dirContext != null) {
        return result.dirContext.getAttributes(result.aliasName, attrIds);
      }
    }
    Attributes attrs = doGetAttributes(name, attrIds);
    if (attrs != null) {
      return attrs;
    }
    String resourceName = "/META-INF/resources" + name;
    for (DirContext altDirContext : this.altDirContexts)
    {
      if ((altDirContext instanceof BaseDirContext)) {
        attrs = ((BaseDirContext)altDirContext).doGetAttributes(resourceName, attrIds);
      } else {
        try
        {
          attrs = altDirContext.getAttributes(name, attrIds);
        }
        catch (NamingException ne) {}
      }
      if (attrs != null) {
        return attrs;
      }
    }
    throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { name }));
  }
  
  public void modifyAttributes(Name name, int mod_op, Attributes attrs)
    throws NamingException
  {
    modifyAttributes(name.toString(), mod_op, attrs);
  }
  
  public abstract void modifyAttributes(String paramString, int paramInt, Attributes paramAttributes)
    throws NamingException;
  
  public void modifyAttributes(Name name, ModificationItem[] mods)
    throws NamingException
  {
    modifyAttributes(name.toString(), mods);
  }
  
  public abstract void modifyAttributes(String paramString, ModificationItem[] paramArrayOfModificationItem)
    throws NamingException;
  
  public void bind(Name name, Object obj, Attributes attrs)
    throws NamingException
  {
    bind(name.toString(), obj, attrs);
  }
  
  public abstract void bind(String paramString, Object paramObject, Attributes paramAttributes)
    throws NamingException;
  
  public void rebind(Name name, Object obj, Attributes attrs)
    throws NamingException
  {
    rebind(name.toString(), obj, attrs);
  }
  
  public abstract void rebind(String paramString, Object paramObject, Attributes paramAttributes)
    throws NamingException;
  
  public DirContext createSubcontext(Name name, Attributes attrs)
    throws NamingException
  {
    return createSubcontext(name.toString(), attrs);
  }
  
  public abstract DirContext createSubcontext(String paramString, Attributes paramAttributes)
    throws NamingException;
  
  public DirContext getSchema(Name name)
    throws NamingException
  {
    return getSchema(name.toString());
  }
  
  public abstract DirContext getSchema(String paramString)
    throws NamingException;
  
  public DirContext getSchemaClassDefinition(Name name)
    throws NamingException
  {
    return getSchemaClassDefinition(name.toString());
  }
  
  public abstract DirContext getSchemaClassDefinition(String paramString)
    throws NamingException;
  
  public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException
  {
    return search(name.toString(), matchingAttributes, attributesToReturn);
  }
  
  public abstract NamingEnumeration<SearchResult> search(String paramString, Attributes paramAttributes, String[] paramArrayOfString)
    throws NamingException;
  
  public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
    throws NamingException
  {
    return search(name.toString(), matchingAttributes);
  }
  
  public abstract NamingEnumeration<SearchResult> search(String paramString, Attributes paramAttributes)
    throws NamingException;
  
  public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons)
    throws NamingException
  {
    return search(name.toString(), filter, cons);
  }
  
  public abstract NamingEnumeration<SearchResult> search(String paramString1, String paramString2, SearchControls paramSearchControls)
    throws NamingException;
  
  public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException
  {
    return search(name.toString(), filterExpr, filterArgs, cons);
  }
  
  public abstract NamingEnumeration<SearchResult> search(String paramString1, String paramString2, Object[] paramArrayOfObject, SearchControls paramSearchControls)
    throws NamingException;
  
  protected abstract Attributes doGetAttributes(String paramString, String[] paramArrayOfString)
    throws NamingException;
  
  protected abstract Object doLookup(String paramString);
  
  protected abstract List<NamingEntry> doListBindings(String paramString)
    throws NamingException;
  
  protected abstract String doGetRealPath(String paramString);
  
  private AliasResult findAlias(String name)
  {
    AliasResult result = new AliasResult();
    boolean slashAppended;
    String searchName;

    if (name.length() == 0)
    {
      searchName = "/";
      slashAppended = true;
    }
    else
    {

      if (name.charAt(0) == '/')
      {
        searchName = name;
        slashAppended = false;
      }
      else
      {
        searchName = "/" + name;
        slashAppended = true;
      }
    }
    result.dirContext = ((BaseDirContext)this.aliases.get(searchName));
    while (result.dirContext == null)
    {
      int slash = searchName.lastIndexOf('/');
      if (slash < 0) {
        break;
      }
      searchName = searchName.substring(0, slash);
      result.dirContext = ((BaseDirContext)this.aliases.get(searchName));
    }
    if (result.dirContext != null) {
      if (slashAppended) {
        result.aliasName = name.substring(searchName.length() - 1);
      } else {
        result.aliasName = name.substring(searchName.length());
      }
    }
    return result;
  }
  
  private static class AliasResult
  {
    BaseDirContext dirContext;
    String aliasName;
    
    private AliasResult() {}
  }
}

