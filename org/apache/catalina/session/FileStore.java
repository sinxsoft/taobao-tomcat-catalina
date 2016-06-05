package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

public final class FileStore
  extends StoreBase
{
  private static final String FILE_EXT = ".session";
  private String directory = ".";
  private File directoryFile = null;
  private static final String info = "FileStore/1.0";
  private static final String storeName = "fileStore";
  private static final String threadName = "FileStore";
  
  public FileStore() {}
  
  public String getDirectory()
  {
    return this.directory;
  }
  
  public void setDirectory(String path)
  {
    String oldDirectory = this.directory;
    this.directory = path;
    this.directoryFile = null;
    this.support.firePropertyChange("directory", oldDirectory, this.directory);
  }
  
  public String getInfo()
  {
    return "FileStore/1.0";
  }
  
  public String getThreadName()
  {
    return "FileStore";
  }
  
  public String getStoreName()
  {
    return "fileStore";
  }
  
  public int getSize()
    throws IOException
  {
    File file = directory();
    if (file == null) {
      return 0;
    }
    String[] files = file.list();
    
    int keycount = 0;
    for (int i = 0; i < files.length; i++) {
      if (files[i].endsWith(".session")) {
        keycount++;
      }
    }
    return keycount;
  }
  
  public void clear()
    throws IOException
  {
    String[] keys = keys();
    for (int i = 0; i < keys.length; i++) {
      remove(keys[i]);
    }
  }
  
  public String[] keys()
    throws IOException
  {
    File file = directory();
    if (file == null) {
      return new String[0];
    }
    String[] files = file.list();
    if ((files == null) || (files.length < 1)) {
      return new String[0];
    }
    ArrayList<String> list = new ArrayList();
    int n = ".session".length();
    for (int i = 0; i < files.length; i++) {
      if (files[i].endsWith(".session")) {
        list.add(files[i].substring(0, files[i].length() - n));
      }
    }
    return (String[])list.toArray(new String[list.size()]);
  }
  
  public Session load(String id)
    throws ClassNotFoundException, IOException
  {
    File file = file(id);
    if (file == null) {
      return null;
    }
    if (!file.exists()) {
      return null;
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".loading", new Object[] { id, file.getAbsolutePath() }));
    }
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    ObjectInputStream ois = null;
    Loader loader = null;
    ClassLoader classLoader = null;
    ClassLoader oldThreadContextCL = Thread.currentThread().getContextClassLoader();
    try
    {
      fis = new FileInputStream(file.getAbsolutePath());
      bis = new BufferedInputStream(fis);
      Container container = this.manager.getContainer();
      if (container != null) {
        loader = container.getLoader();
      }
      if (loader != null) {
        classLoader = loader.getClassLoader();
      }
      if (classLoader != null)
      {
        Thread.currentThread().setContextClassLoader(classLoader);
        ois = new CustomObjectInputStream(bis, classLoader);
      }
      else
      {
        ois = new ObjectInputStream(bis);
      }
      StandardSession session = (StandardSession)this.manager.createEmptySession();
      
      session.readObjectData(ois);
      session.setManager(this.manager);
      return session;
    }
    catch (FileNotFoundException e)
    {
      StandardSession session;
      if (this.manager.getContainer().getLogger().isDebugEnabled()) {
        this.manager.getContainer().getLogger().debug("No persisted data file found");
      }
      return null;
    }
    catch (IOException e)
    {
      if (bis != null) {
        try
        {
          bis.close();
        }
        catch (IOException f) {}
      }
      if (fis != null) {
        try
        {
          fis.close();
        }
        catch (IOException f) {}
      }
      throw e;
    }
    finally
    {
      if (ois != null) {
        try
        {
          ois.close();
        }
        catch (IOException f) {}
      }
      Thread.currentThread().setContextClassLoader(oldThreadContextCL);
    }
  }
  
  public void remove(String id)
    throws IOException
  {
    File file = file(id);
    if (file == null) {
      return;
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".removing", new Object[] { id, file.getAbsolutePath() }));
    }
    file.delete();
  }
  
  public void save(Session session)
    throws IOException
  {
    File file = file(session.getIdInternal());
    if (file == null) {
      return;
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".saving", new Object[] { session.getIdInternal(), file.getAbsolutePath() }));
    }
    FileOutputStream fos = null;
    ObjectOutputStream oos = null;
    try
    {
      fos = new FileOutputStream(file.getAbsolutePath());
      oos = new ObjectOutputStream(new BufferedOutputStream(fos));
    }
    catch (IOException e)
    {
      if (fos != null) {
        try
        {
          fos.close();
        }
        catch (IOException f) {}
      }
      throw e;
    }
    try
    {
      ((StandardSession)session).writeObjectData(oos);
    }
    finally
    {
      oos.close();
    }
  }
  
  private File directory()
    throws IOException
  {
    if (this.directory == null) {
      return null;
    }
    if (this.directoryFile != null) {
      return this.directoryFile;
    }
    File file = new File(this.directory);
    if (!file.isAbsolute())
    {
      Container container = this.manager.getContainer();
      if ((container instanceof Context))
      {
        ServletContext servletContext = ((Context)container).getServletContext();
        
        File work = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
        
        file = new File(work, this.directory);
      }
      else
      {
        throw new IllegalArgumentException("Parent Container is not a Context");
      }
    }
    if ((!file.exists()) || (!file.isDirectory()))
    {
      if ((!file.delete()) && (file.exists())) {
        throw new IOException(sm.getString("fileStore.deleteFailed", new Object[] { file }));
      }
      if ((!file.mkdirs()) && (!file.isDirectory())) {
        throw new IOException(sm.getString("fileStore.createFailed", new Object[] { file }));
      }
    }
    this.directoryFile = file;
    return file;
  }
  
  private File file(String id)
    throws IOException
  {
    if (this.directory == null) {
      return null;
    }
    String filename = id + ".session";
    File file = new File(directory(), filename);
    return file;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\FileStore.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */