package org.apache.naming.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
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
import org.apache.tomcat.util.http.RequestUtil;

public class FileDirContext
  extends BaseDirContext
{
  private static final Log log = LogFactory.getLog(FileDirContext.class);
  protected static final int BUFFER_SIZE = 2048;
  
  public FileDirContext() {}
  
  public FileDirContext(Hashtable<String, Object> env)
  {
    super(env);
  }
  
  protected File base = null;
  protected String absoluteBase = null;
  protected boolean allowLinking = false;
  
  public void setDocBase(String docBase)
  {
    if (docBase == null) {
      throw new IllegalArgumentException(sm.getString("resources.null"));
    }
    this.base = new File(docBase);
    try
    {
      this.base = this.base.getCanonicalFile();
    }
    catch (IOException e) {}
    if ((!this.base.exists()) || (!this.base.isDirectory()) || (!this.base.canRead())) {
      throw new IllegalArgumentException(sm.getString("fileResources.base", new Object[] { docBase }));
    }
    this.absoluteBase = this.base.getAbsolutePath();
    super.setDocBase(docBase);
  }
  
  public void setAllowLinking(boolean allowLinking)
  {
    this.allowLinking = allowLinking;
  }
  
  public boolean getAllowLinking()
  {
    return this.allowLinking;
  }
  
  public void release()
  {
    super.release();
  }
  
  protected String doGetRealPath(String path)
  {
    File file = new File(getDocBase(), path);
    return file.getAbsolutePath();
  }
  
  protected Object doLookup(String name)
  {
    Object result = null;
    File file = file(name);
    if (file == null) {
      return null;
    }
    if (file.isDirectory())
    {
      FileDirContext tempContext = new FileDirContext(this.env);
      tempContext.setDocBase(file.getPath());
      tempContext.setAllowLinking(getAllowLinking());
      result = tempContext;
    }
    else
    {
      result = new FileResource(file);
    }
    return result;
  }
  
  public void unbind(String name)
    throws NamingException
  {
    File file = file(name);
    if (file == null) {
      throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { name }));
    }
    if (!file.delete()) {
      throw new NamingException(sm.getString("resources.unbindFailed", new Object[] { name }));
    }
  }
  
  public void rename(String oldName, String newName)
    throws NamingException
  {
    File file = file(oldName);
    if (file == null) {
      throw new NameNotFoundException(sm.getString("resources.notFound", new Object[] { oldName }));
    }
    File newFile = new File(this.base, newName);
    if (!file.renameTo(newFile)) {
      throw new NamingException(sm.getString("resources.renameFail", new Object[] { oldName, newName }));
    }
  }
  
  protected List<NamingEntry> doListBindings(String name)
    throws NamingException
  {
    File file = file(name);
    if (file == null) {
      return null;
    }
    return list(file);
  }
  
  public void destroySubcontext(String name)
    throws NamingException
  {
    unbind(name);
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
    File file = file(name);
    if (file == null) {
      return null;
    }
    return new FileResourceAttributes(file);
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
    File file = new File(this.base, name);
    if (file.exists()) {
      throw new NameAlreadyBoundException(sm.getString("resources.alreadyBound", new Object[] { name }));
    }
    rebind(name, obj, attrs);
  }
  
  public void rebind(String name, Object obj, Attributes attrs)
    throws NamingException
  {
    File file = new File(this.base, name);
    
    InputStream is = null;
    if ((obj instanceof Resource))
    {
      try
      {
        is = ((Resource)obj).streamContent();
      }
      catch (IOException e) {}
    }
    else if ((obj instanceof InputStream))
    {
      is = (InputStream)obj;
    }
    else if ((obj instanceof DirContext))
    {
      if ((file.exists()) && 
        (!file.delete())) {
        throw new NamingException(sm.getString("resources.bindFailed", new Object[] { name }));
      }
      if (!file.mkdir()) {
        throw new NamingException(sm.getString("resources.bindFailed", new Object[] { name }));
      }
    }
    if (is == null) {
      throw new NamingException(sm.getString("resources.bindFailed", new Object[] { name }));
    }
    try
    {
      FileOutputStream os = null;
      byte[] buffer = new byte['ࠀ'];
      int len = -1;
      try
      {
        os = new FileOutputStream(file);
        for (;;)
        {
          len = is.read(buffer);
          if (len == -1) {
            break;
          }
          os.write(buffer, 0, len);
        }
      }
      finally
      {
        if (os != null) {
          os.close();
        }
        is.close();
      }
    }
    catch (IOException e)
    {
      NamingException ne = new NamingException(sm.getString("resources.bindFailed", new Object[] { e }));
      
      ne.initCause(e);
      throw ne;
    }
  }
  
  public DirContext createSubcontext(String name, Attributes attrs)
    throws NamingException
  {
    File file = new File(this.base, name);
    if (file.exists()) {
      throw new NameAlreadyBoundException(sm.getString("resources.alreadyBound", new Object[] { name }));
    }
    if (!file.mkdir()) {
      throw new NamingException(sm.getString("resources.bindFailed", new Object[] { name }));
    }
    return (DirContext)lookup(name);
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
    return null;
  }
  
  public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
    throws NamingException
  {
    return null;
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
    throws NamingException
  {
    return null;
  }
  
  public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException
  {
    return null;
  }
  
  protected String normalize(String path)
  {
    return RequestUtil.normalize(path, File.separatorChar == '\\');
  }
  
  protected File file(String name)
  {
    File file = new File(this.base, name);
    if ((file.exists()) && (file.canRead()))
    {
      if (this.allowLinking) {
        return file;
      }
      String canPath = null;
      try
      {
        canPath = file.getCanonicalPath();
      }
      catch (IOException e) {}
      if (canPath == null) {
        return null;
      }
      if (!canPath.startsWith(this.absoluteBase)) {
        return null;
      }
      String fileAbsPath = file.getAbsolutePath();
      if (fileAbsPath.endsWith(".")) {
        fileAbsPath = fileAbsPath + "/";
      }
      String absPath = normalize(fileAbsPath);
      canPath = normalize(canPath);
      if ((this.absoluteBase.length() < absPath.length()) && (this.absoluteBase.length() < canPath.length()))
      {
        absPath = absPath.substring(this.absoluteBase.length() + 1);
        if (absPath == null) {
          return null;
        }
        if (absPath.equals("")) {
          absPath = "/";
        }
        canPath = canPath.substring(this.absoluteBase.length() + 1);
        if (canPath.equals("")) {
          canPath = "/";
        }
        if (!canPath.equals(absPath)) {
          return null;
        }
      }
    }
    else
    {
      return null;
    }
    return file;
  }
  
  protected List<NamingEntry> list(File file)
  {
    List<NamingEntry> entries = new ArrayList();
    if (!file.isDirectory()) {
      return entries;
    }
    String[] names = file.list();
    if (names == null)
    {
      log.warn(sm.getString("fileResources.listingNull", new Object[] { file.getAbsolutePath() }));
      
      return entries;
    }
    Arrays.sort(names);
    NamingEntry entry = null;
    for (int i = 0; i < names.length; i++)
    {
      File currentFile = new File(file, names[i]);
      Object object = null;
      if (currentFile.isDirectory())
      {
        FileDirContext tempContext = new FileDirContext(this.env);
        tempContext.setDocBase(currentFile.getPath());
        tempContext.setAllowLinking(getAllowLinking());
        object = tempContext;
      }
      else
      {
        object = new FileResource(currentFile);
      }
      entry = new NamingEntry(names[i], object, 0);
      entries.add(entry);
    }
    return entries;
  }
  
  protected static class FileResource
    extends Resource
  {
    protected File file;
    
    public FileResource(File file)
    {
      this.file = file;
    }
    
    public InputStream streamContent()
      throws IOException
    {
      if (this.binaryContent == null)
      {
        FileInputStream fis = new FileInputStream(this.file);
        this.inputStream = fis;
        return fis;
      }
      return super.streamContent();
    }
  }
  
  protected static class FileResourceAttributes
    extends ResourceAttributes
  {
    private static final long serialVersionUID = 1L;
    protected File file;
    
    public FileResourceAttributes(File file)
    {
      this.file = file;
      getCreation();
      getLastModified();
    }
    
    protected boolean accessed = false;
    protected String canonicalPath = null;
    
    public boolean isCollection()
    {
      if (!this.accessed)
      {
        this.collection = this.file.isDirectory();
        this.accessed = true;
      }
      return super.isCollection();
    }
    
    public long getContentLength()
    {
      if (this.contentLength != -1L) {
        return this.contentLength;
      }
      this.contentLength = this.file.length();
      return this.contentLength;
    }
    
    public long getCreation()
    {
      if (this.creation != -1L) {
        return this.creation;
      }
      this.creation = getLastModified();
      return this.creation;
    }
    
    public Date getCreationDate()
    {
      if (this.creation == -1L) {
        this.creation = getCreation();
      }
      return super.getCreationDate();
    }
    
    public long getLastModified()
    {
      if (this.lastModified != -1L) {
        return this.lastModified;
      }
      this.lastModified = this.file.lastModified();
      return this.lastModified;
    }
    
    public Date getLastModifiedDate()
    {
      if (this.lastModified == -1L) {
        this.lastModified = getLastModified();
      }
      return super.getLastModifiedDate();
    }
    
    public String getName()
    {
      if (this.name == null) {
        this.name = this.file.getName();
      }
      return this.name;
    }
    
    public String getResourceType()
    {
      if (!this.accessed)
      {
        this.collection = this.file.isDirectory();
        this.accessed = true;
      }
      return super.getResourceType();
    }
    
    public String getCanonicalPath()
    {
      if (this.canonicalPath == null) {
        try
        {
          this.canonicalPath = this.file.getCanonicalPath();
        }
        catch (IOException e) {}
      }
      return this.canonicalPath;
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\FileDirContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */