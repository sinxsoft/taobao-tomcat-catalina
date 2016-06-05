package org.apache.catalina.startup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.apache.catalina.Host;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class ExpandWar
{
  private static final Log log = LogFactory.getLog(ExpandWar.class);
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  
  public ExpandWar() {}
  
  public static String expand(Host host, URL war, String pathname)
    throws IOException
  {
    File appBase = new File(host.getAppBase());
    if (!appBase.isAbsolute()) {
      appBase = new File(System.getProperty("catalina.base"), host.getAppBase());
    }
    if ((!appBase.exists()) || (!appBase.isDirectory())) {
      throw new IOException(sm.getString("hostConfig.appBase", new Object[] { appBase.getAbsolutePath() }));
    }
    File docBase = new File(appBase, pathname);
    if (docBase.exists()) {
      return docBase.getAbsolutePath();
    }
    if ((!docBase.mkdir()) && (!docBase.isDirectory())) {
      throw new IOException(sm.getString("expandWar.createFailed", new Object[] { docBase }));
    }
    String canonicalDocBasePrefix = docBase.getCanonicalPath();
    if (!canonicalDocBasePrefix.endsWith(File.separator)) {
      canonicalDocBasePrefix = canonicalDocBasePrefix + File.separator;
    }
    JarURLConnection juc = (JarURLConnection)war.openConnection();
    juc.setUseCaches(false);
    JarFile jarFile = null;
    InputStream input = null;
    boolean success = false;
    try
    {
      jarFile = juc.getJarFile();
      Enumeration<JarEntry> jarEntries = jarFile.entries();
      while (jarEntries.hasMoreElements())
      {
        JarEntry jarEntry = (JarEntry)jarEntries.nextElement();
        String name = jarEntry.getName();
        File expandedFile = new File(docBase, name);
        if (!expandedFile.getCanonicalPath().startsWith(canonicalDocBasePrefix)) {
          throw new IllegalArgumentException(sm.getString("expandWar.illegalPath", new Object[] { war, name, expandedFile.getCanonicalPath(), canonicalDocBasePrefix }));
        }
        int last = name.lastIndexOf('/');
        if (last >= 0)
        {
          File parent = new File(docBase, name.substring(0, last));
          if ((!parent.mkdirs()) && (!parent.isDirectory())) {
            throw new IOException(sm.getString("expandWar.createFailed", new Object[] { parent }));
          }
        }
        if (!name.endsWith("/"))
        {
          input = jarFile.getInputStream(jarEntry);
          if (null == input) {
            throw new ZipException(sm.getString("expandWar.missingJarEntry", new Object[] { jarEntry.getName() }));
          }
          expand(input, expandedFile);
          long lastModified = jarEntry.getTime();
          if ((lastModified != -1L) && (lastModified != 0L)) {
            expandedFile.setLastModified(lastModified);
          }
          input.close();
          input = null;
        }
      }
      success = true;
    }
    catch (IOException e)
    {
      throw e;
    }
    finally
    {
      if (!success) {
        deleteDir(docBase);
      }
      if (input != null)
      {
        try
        {
          input.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        input = null;
      }
      if (jarFile != null)
      {
        try
        {
          jarFile.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        jarFile = null;
      }
    }
    return docBase.getAbsolutePath();
  }
  
  public static void validate(Host host, URL war, String pathname)
    throws IOException
  {
    File appBase = new File(host.getAppBase());
    if (!appBase.isAbsolute()) {
      appBase = new File(System.getProperty("catalina.base"), host.getAppBase());
    }
    File docBase = new File(appBase, pathname);
    
    String canonicalDocBasePrefix = docBase.getCanonicalPath();
    if (!canonicalDocBasePrefix.endsWith(File.separator)) {
      canonicalDocBasePrefix = canonicalDocBasePrefix + File.separator;
    }
    JarURLConnection juc = (JarURLConnection)war.openConnection();
    juc.setUseCaches(false);
    JarFile jarFile = null;
    try
    {
      jarFile = juc.getJarFile();
      Enumeration<JarEntry> jarEntries = jarFile.entries();
      while (jarEntries.hasMoreElements())
      {
        JarEntry jarEntry = (JarEntry)jarEntries.nextElement();
        String name = jarEntry.getName();
        File expandedFile = new File(docBase, name);
        if (!expandedFile.getCanonicalPath().startsWith(canonicalDocBasePrefix)) {
          throw new IllegalArgumentException(sm.getString("expandWar.illegalPath", new Object[] { war, name, expandedFile.getCanonicalPath(), canonicalDocBasePrefix }));
        }
      }
    }
    catch (IOException e)
    {
      throw e;
    }
    finally
    {
      if (jarFile != null)
      {
        try
        {
          jarFile.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        jarFile = null;
      }
    }
  }
  
  public static boolean copy(File src, File dest)
  {
    boolean result = true;
    
    String[] files = null;
    if (src.isDirectory())
    {
      files = src.list();
      result = dest.mkdir();
    }
    else
    {
      files = new String[1];
      files[0] = "";
    }
    if (files == null) {
      files = new String[0];
    }
    for (int i = 0; (i < files.length) && (result);)
    {
      File fileSrc = new File(src, files[i]);
      File fileDest = new File(dest, files[i]);
      if (fileSrc.isDirectory())
      {
        result = copy(fileSrc, fileDest);
      }
      else
      {
        FileChannel ic = null;
        FileChannel oc = null;
        try
        {
          ic = new FileInputStream(fileSrc).getChannel();
          oc = new FileOutputStream(fileDest).getChannel();
          ic.transferTo(0L, ic.size(), oc);
          if (ic != null) {
            try
            {
              ic.close();
            }
            catch (IOException e) {}
          }
          if (oc != null) {
            try
            {
              oc.close();
            }
            catch (IOException e) {}
          }
          i++;
        }
        catch (IOException e)
        {
          log.error(sm.getString("expandWar.copy", new Object[] { fileSrc, fileDest }), e);
          
          result = false;
        }
        finally
        {
          if (ic != null) {
            try
            {
              ic.close();
            }
            catch (IOException e) {}
          }
          if (oc != null) {
            try
            {
              oc.close();
            }
            catch (IOException e) {}
          }
        }
      }
    }
    return result;
  }
  
  public static boolean delete(File dir)
  {
    return delete(dir, true);
  }
  
  public static boolean delete(File dir, boolean logFailure)
  {
    boolean result;

    if (dir.isDirectory())
    {
      result = deleteDir(dir, logFailure);
    }
    else
    {

      if (dir.exists()) {
        result = dir.delete();
      } else {
        result = true;
      }
    }
    if ((logFailure) && (!result)) {
      log.error(sm.getString("expandWar.deleteFailed", new Object[] { dir.getAbsolutePath() }));
    }
    return result;
  }
  
  public static boolean deleteDir(File dir)
  {
    return deleteDir(dir, true);
  }
  
  public static boolean deleteDir(File dir, boolean logFailure)
  {
    String[] files = dir.list();
    if (files == null) {
      files = new String[0];
    }
    for (int i = 0; i < files.length; i++)
    {
      File file = new File(dir, files[i]);
      if (file.isDirectory()) {
        deleteDir(file, logFailure);
      } else {
        file.delete();
      }
    }
    boolean result;
    
    if (dir.exists()) {
      result = dir.delete();
    } else {
      result = true;
    }
    if ((logFailure) && (!result)) {
      log.error(sm.getString("expandWar.deleteFailed", new Object[] { dir.getAbsolutePath() }));
    }
    return result;
  }
  
  private static void expand(InputStream input, File file)
    throws IOException
  {
    BufferedOutputStream output = null;
    try
    {
      output = new BufferedOutputStream(new FileOutputStream(file));
      
      byte[] buffer = new byte['ࠀ'];
      for (;;)
      {
        int n = input.read(buffer);
        if (n <= 0) {
          break;
        }
        output.write(buffer, 0, n);
      }
      return;
    }
    finally
    {
      if (output != null) {
        try
        {
          output.close();
        }
        catch (IOException e) {}
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\ExpandWar.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */