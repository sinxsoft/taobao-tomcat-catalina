package org.apache.naming.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.NamingEntry;
import org.apache.naming.StringManager;

public class WARDirContext
  extends BaseDirContext
{
  private static final Log log = LogFactory.getLog(WARDirContext.class);
  
  public WARDirContext() {}
  
  public WARDirContext(Hashtable<String, Object> env)
  {
    super(env);
  }
  
  protected WARDirContext(ZipFile base, Entry entries)
  {
    this.base = base;
    this.entries = entries;
  }
  
  protected ZipFile base = null;
  protected Entry entries = null;
  
  public void setDocBase(String docBase)
  {
    if (docBase == null) {
      throw new IllegalArgumentException(sm.getString("resources.null"));
    }
    if (!docBase.endsWith(".war")) {
      throw new IllegalArgumentException(sm.getString("warResources.notWar"));
    }
    File base = new File(docBase);
    if ((!base.exists()) || (!base.canRead()) || (base.isDirectory())) {
      throw new IllegalArgumentException(sm.getString("warResources.invalidWar", new Object[] { docBase }));
    }
    try
    {
      this.base = new ZipFile(base);
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException(sm.getString("warResources.invalidWar", new Object[] { e.getMessage() }));
    }
    super.setDocBase(docBase);
    
    loadEntries();
  }
  
  public void release()
  {
    this.entries = null;
    if (this.base != null) {
      try
      {
        this.base.close();
      }
      catch (IOException e)
      {
        log.warn("Exception closing WAR File " + this.base.getName(), e);
      }
    }
    this.base = null;
    super.release();
  }
  
  protected String doGetRealPath(String path)
  {
    return null;
  }
  
  protected Object doLookup(String strName)
  {
    Name name;
    try
    {
      name = getEscapedJndiName(strName);
    }
    catch (InvalidNameException e)
    {
      log.info(sm.getString("resources.invalidName", new Object[] { strName }), e);
      return null;
    }
    if (name.isEmpty()) {
      return this;
    }
    Entry entry = treeLookup(name);
    if (entry == null) {
      return null;
    }
    ZipEntry zipEntry = entry.getEntry();
    if (zipEntry.isDirectory()) {
      return new WARDirContext(this.base, entry);
    }
    return new WARResource(entry.getEntry());
  }
  
  private Name getEscapedJndiName(String name)
    throws InvalidNameException
  {
    return new CompositeName(name.replace("'", "\\'").replace("\"", ""));
  }
  
  public void unbind(String name)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public void rename(String oldName, String newName)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  protected List<NamingEntry> doListBindings(String strName)
    throws NamingException
  {
    Name name = getEscapedJndiName(strName);
    if (name.isEmpty()) {
      return list(this.entries);
    }
    Entry entry = treeLookup(name);
    if (entry == null) {
      return null;
    }
    return list(entry);
  }
  
  public void destroySubcontext(String name)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public Object lookupLink(String name)
    throws NamingException
  {
    return lookup(name);
  }
  
  public String getNameInNamespace()
    throws NamingException
  {
    return this.docBase;
  }
  
  protected Attributes doGetAttributes(String name, String[] attrIds)
    throws NamingException
  {
    return getAttributes(getEscapedJndiName(name), attrIds);
  }
  
  public Attributes getAttributes(Name name, String[] attrIds)
    throws NamingException
  {
    Entry entry = null;
    if (name.isEmpty()) {
      entry = this.entries;
    } else {
      entry = treeLookup(name);
    }
    if (entry == null) {
      return null;
    }
    ZipEntry zipEntry = entry.getEntry();
    
    ResourceAttributes attrs = new ResourceAttributes();
    attrs.setCreationDate(new Date(zipEntry.getTime()));
    attrs.setName(entry.getName());
    if (!zipEntry.isDirectory()) {
      attrs.setResourceType("");
    } else {
      attrs.setCollection(true);
    }
    attrs.setContentLength(zipEntry.getSize());
    attrs.setLastModified(zipEntry.getTime());
    
    return attrs;
  }
  
  public void modifyAttributes(String name, int mod_op, Attributes attrs)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public void modifyAttributes(String name, ModificationItem[] mods)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public void bind(String name, Object obj, Attributes attrs)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public void rebind(String name, Object obj, Attributes attrs)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public DirContext createSubcontext(String name, Attributes attrs)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public DirContext getSchema(String name)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public DirContext getSchemaClassDefinition(String name)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  protected String normalize(ZipEntry entry)
  {
    String result = "/" + entry.getName();
    if (entry.isDirectory()) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
  
  protected void loadEntries()
  {
    try
    {
      Enumeration<? extends ZipEntry> entryList = this.base.entries();
      this.entries = new Entry("/", new ZipEntry("/"));
      while (entryList.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)entryList.nextElement();
        String name = normalize(entry);
        int pos = name.lastIndexOf('/');
        
        int currentPos = -1;
        int lastPos = 0;
        while ((currentPos = name.indexOf('/', lastPos)) != -1)
        {
          Name parentName = getEscapedJndiName(name.substring(0, lastPos));
          Name childName = getEscapedJndiName(name.substring(0, currentPos));
          String entryName = name.substring(lastPos, currentPos);
          
          Entry parent = treeLookup(parentName);
          Entry child = treeLookup(childName);
          if (child == null)
          {
            String zipName = name.substring(1, currentPos) + "/";
            child = new Entry(entryName, new ZipEntry(zipName));
            if (parent != null) {
              parent.addChild(child);
            }
          }
          lastPos = currentPos + 1;
        }
        String entryName = name.substring(pos + 1, name.length());
        Name compositeName = getEscapedJndiName(name.substring(0, pos));
        Entry parent = treeLookup(compositeName);
        Entry child = new Entry(entryName, entry);
        if (parent != null) {
          parent.addChild(child);
        }
      }
    }
    catch (Exception e) {}
  }
  
  protected Entry treeLookup(Name name)
  {
    if ((name.isEmpty()) || (this.entries == null)) {
      return this.entries;
    }
    Entry currentEntry = this.entries;
    for (int i = 0; i < name.size(); i++) {
      if (name.get(i).length() != 0)
      {
        currentEntry = currentEntry.getChild(name.get(i));
        if (currentEntry == null) {
          return null;
        }
      }
    }
    return currentEntry;
  }
  
  protected ArrayList<NamingEntry> list(Entry entry)
  {
    ArrayList<NamingEntry> entries = new ArrayList();
    Entry[] children = entry.getChildren();
    Arrays.sort(children);
    NamingEntry namingEntry = null;
    for (int i = 0; i < children.length; i++)
    {
      ZipEntry current = children[i].getEntry();
      Object object = null;
      if (current.isDirectory()) {
        object = new WARDirContext(this.base, children[i]);
      } else {
        object = new WARResource(current);
      }
      namingEntry = new NamingEntry(children[i].getName(), object, 0);
      
      entries.add(namingEntry);
    }
    return entries;
  }
  
  protected static class Entry
    implements Comparable<Object>
  {
    public Entry(String name, ZipEntry entry)
    {
      this.name = name;
      this.entry = entry;
    }
    
    protected String name = null;
    protected ZipEntry entry = null;
    protected Entry[] children = new Entry[0];
    
    public int compareTo(Object o)
    {
      if (!(o instanceof Entry)) {
        return 1;
      }
      return this.name.compareTo(((Entry)o).getName());
    }
    
    public boolean equals(Object o)
    {
      if (!(o instanceof Entry)) {
        return false;
      }
      return this.name.equals(((Entry)o).getName());
    }
    
    public int hashCode()
    {
      return this.name.hashCode();
    }
    
    public ZipEntry getEntry()
    {
      return this.entry;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public void addChild(Entry entry)
    {
      Entry[] newChildren = new Entry[this.children.length + 1];
      for (int i = 0; i < this.children.length; i++) {
        newChildren[i] = this.children[i];
      }
      newChildren[this.children.length] = entry;
      this.children = newChildren;
    }
    
    public Entry[] getChildren()
    {
      return this.children;
    }
    
    public Entry getChild(String name)
    {
      for (int i = 0; i < this.children.length; i++) {
        if (this.children[i].name.equals(name)) {
          return this.children[i];
        }
      }
      return null;
    }
  }
  
  protected class WARResource
    extends Resource
  {
    protected ZipEntry entry;
    
    public WARResource(ZipEntry entry)
    {
      this.entry = entry;
    }
    
    public InputStream streamContent()
      throws IOException
    {
      try
      {
        if (this.binaryContent == null)
        {
          InputStream is = WARDirContext.this.base.getInputStream(this.entry);
          this.inputStream = is;
          return is;
        }
      }
      catch (ZipException e)
      {
        throw new IOException(e.getMessage(), e);
      }
      return super.streamContent();
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\WARDirContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */