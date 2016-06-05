package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.scan.Jar;
import org.apache.tomcat.util.scan.JarFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class TldConfig
  implements LifecycleListener
{
  private static final String TLD_EXT = ".tld";
  private static final String WEB_INF = "/WEB-INF/";
  private static final String WEB_INF_LIB = "/WEB-INF/lib/";
  private static volatile Set<String> noTldJars = null;
  private static final Log log = LogFactory.getLog(TldConfig.class);
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  private static Digester[] tldDigesters = new Digester[4];
  private Context context;
  private Digester tldDigester;
  private Set<String> taglibUris;
  private Set<String> webxmlTaglibUris;
  private ArrayList<String> listeners;
  
  private static synchronized Digester createTldDigester(boolean validation, boolean blockExternal)
  {
    int cacheIndex = 0;
    if (validation) {
      cacheIndex++;
    }
    if (blockExternal) {
      cacheIndex += 2;
    }
    Digester digester = tldDigesters[cacheIndex];
    if (digester == null)
    {
      digester = DigesterFactory.newDigester(validation, true, new TldRuleSet(), blockExternal);
      
      digester.getParser();
      tldDigesters[cacheIndex] = digester;
    }
    return digester;
  }
  
  static
  {
    StringBuilder jarList = new StringBuilder(System.getProperty("tomcat.util.scan.DefaultJarScanner.jarsToSkip", ""));
    
    String tldJars = System.getProperty("org.apache.catalina.startup.TldConfig.jarsToSkip", "");
    if (tldJars.length() > 0)
    {
      if (jarList.length() > 0) {
        jarList.append(',');
      }
      jarList.append(tldJars);
    }
    if (jarList.length() > 0) {
      setNoTldJars(jarList.toString());
    }
  }
  
  public static synchronized void setNoTldJars(String jarNames)
  {
    if (jarNames == null)
    {
      noTldJars = null;
    }
    else
    {
      if (noTldJars == null) {
        noTldJars = new HashSet();
      } else {
        noTldJars.clear();
      }
      StringTokenizer tokenizer = new StringTokenizer(jarNames, ",");
      while (tokenizer.hasMoreElements())
      {
        String token = tokenizer.nextToken().trim();
        if (token.length() > 0) {
          noTldJars.add(token);
        }
      }
    }
  }
  
  public TldConfig()
  {
    this.context = null;
    
    this.tldDigester = null;
    
    this.taglibUris = new HashSet();
    
    this.webxmlTaglibUris = new HashSet();
    
    this.listeners = new ArrayList();
  }
  
  public void addTaglibUri(String uri)
  {
    this.taglibUris.add(uri);
  }
  
  public boolean isKnownTaglibUri(String uri)
  {
    return this.taglibUris.contains(uri);
  }
  
  public boolean isKnownWebxmlTaglibUri(String uri)
  {
    return this.webxmlTaglibUris.contains(uri);
  }
  
  @Deprecated
  public Context getContext()
  {
    return this.context;
  }
  
  @Deprecated
  public void setContext(Context context)
  {
    this.context = context;
  }
  
  public void addApplicationListener(String s)
  {
    if (log.isDebugEnabled()) {
      log.debug("Add tld listener " + s);
    }
    this.listeners.add(s);
  }
  
  public String[] getTldListeners()
  {
    String[] result = new String[this.listeners.size()];
    this.listeners.toArray(result);
    return result;
  }
  
  public void execute()
  {
    long t1 = System.currentTimeMillis();
    
    tldScanWebXml();
    
    tldScanResourcePaths("/WEB-INF/");
    
    JarScanner jarScanner = this.context.getJarScanner();
    jarScanner.scan(this.context.getServletContext(), this.context.getLoader().getClassLoader(), new TldJarScannerCallback(), noTldJars);
    
    String[] list = getTldListeners();
    if (log.isDebugEnabled()) {
      log.debug(sm.getString("tldConfig.addListeners", new Object[] { Integer.valueOf(list.length) }));
    }
    for (int i = 0; (list != null) && (i < list.length); i++) {
      this.context.addApplicationListener(new ApplicationListener(list[i], true));
    }
    long t2 = System.currentTimeMillis();
    if ((this.context instanceof StandardContext)) {
      ((StandardContext)this.context).setTldScanTime(t2 - t1);
    }
  }
  
  private class TldJarScannerCallback
    implements JarScannerCallback
  {
    private TldJarScannerCallback() {}
    
    public void scan(JarURLConnection urlConn)
      throws IOException
    {
      TldConfig.this.tldScanJar(urlConn);
    }
    
    public void scan(File file)
    {
      File metaInf = new File(file, "META-INF");
      if (metaInf.isDirectory()) {
        TldConfig.this.tldScanDir(metaInf);
      }
    }
  }
  
  private void tldScanWebXml()
  {
    if (log.isTraceEnabled()) {
      log.trace(sm.getString("tldConfig.webxmlStart"));
    }
    Collection<TaglibDescriptor> descriptors = this.context.getJspConfigDescriptor().getTaglibs();
    
    Iterator i$ = descriptors.iterator();
    for (;;)
    {
      if (i$.hasNext())
      {
        TaglibDescriptor descriptor = (TaglibDescriptor)i$.next();
        String resourcePath = descriptor.getTaglibLocation();
        if (!resourcePath.startsWith("/")) {
          resourcePath = "/WEB-INF/" + resourcePath;
        }
        if (this.taglibUris.contains(descriptor.getTaglibURI()))
        {
          log.warn(sm.getString("tldConfig.webxmlSkip", new Object[] { resourcePath, descriptor.getTaglibURI() }));
        }
        else
        {
          if (log.isTraceEnabled()) {
            log.trace(sm.getString("tldConfig.webxmlAdd", new Object[] { resourcePath, descriptor.getTaglibURI() }));
          }
          InputStream stream = null;
          try
          {
            stream = this.context.getServletContext().getResourceAsStream(resourcePath);
            if (stream != null)
            {
              XmlErrorHandler handler = tldScanStream(stream);
              handler.logFindings(log, resourcePath);
              this.taglibUris.add(descriptor.getTaglibURI());
              this.webxmlTaglibUris.add(descriptor.getTaglibURI());
            }
            else
            {
              log.warn(sm.getString("tldConfig.webxmlFailPathDoesNotExist", new Object[] { resourcePath, descriptor.getTaglibURI() }));
            }
            if (stream != null) {
              try
              {
                stream.close();
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
              }
            }
          }
          catch (IOException ioe)
          {
            log.warn(sm.getString("tldConfig.webxmlFail", new Object[] { resourcePath, descriptor.getTaglibURI() }), ioe);
          }
          finally
          {
            if (stream != null) {
              try
              {
                stream.close();
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
              }
            }
          }
        }
      }
    }
  }
  
  private void tldScanResourcePaths(String startPath)
  {
    if (log.isTraceEnabled()) {
      log.trace(sm.getString("tldConfig.webinfScan", new Object[] { startPath }));
    }
    ServletContext ctxt = this.context.getServletContext();
    
    Set<String> dirList = ctxt.getResourcePaths(startPath);
    if (dirList != null)
    {
      Iterator<String> it = dirList.iterator();
      while (it.hasNext())
      {
        String path = (String)it.next();
        if ((path.endsWith(".tld")) || ((!path.startsWith("/WEB-INF/lib/")) && (!path.startsWith("/WEB-INF/classes/"))))
        {
          if (path.endsWith(".tld"))
          {
            if ((path.startsWith("/WEB-INF/tags/")) && (!path.endsWith("implicit.tld"))) {
              continue;
            }
            InputStream stream = ctxt.getResourceAsStream(path);
            try
            {
              XmlErrorHandler handler = tldScanStream(stream);
              handler.logFindings(log, path);
            }
            catch (IOException ioe)
            {
              log.warn(sm.getString("tldConfig.webinfFail", new Object[] { path }), ioe);
            }
            finally
            {
              if (stream != null) {
                try
                {
                  stream.close();
                }
                catch (Throwable t)
                {
                  ExceptionUtils.handleThrowable(t);
                }
              }
            }
          }
          tldScanResourcePaths(path);
        }
      }
    }
  }
  
  private void tldScanDir(File start)
  {
    if (log.isTraceEnabled()) {
      log.trace(sm.getString("tldConfig.dirScan", new Object[] { start.getAbsolutePath() }));
    }
    File[] fileList = start.listFiles();
    int i;
    if (fileList != null) {
      for (i = 0; i < fileList.length;) {
        if (fileList[i].isDirectory())
        {
          tldScanDir(fileList[i]);
        }
        else if (fileList[i].getAbsolutePath().endsWith(".tld"))
        {
          InputStream stream = null;
          try
          {
            stream = new FileInputStream(fileList[i]);
            XmlErrorHandler handler = tldScanStream(stream);
            handler.logFindings(log, fileList[i].getAbsolutePath());
            if (stream != null) {
              try
              {
                stream.close();
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
              }
            }
            i++;
          }
          catch (IOException ioe)
          {
            log.warn(sm.getString("tldConfig.dirFail", new Object[] { fileList[i].getAbsolutePath() }), ioe);
          }
          finally
          {
            if (stream != null) {
              try
              {
                stream.close();
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
              }
            }
          }
        }
      }
    }
  }
  
  private void tldScanJar(JarURLConnection jarConn)
  {
    Jar jar = null;
    try
    {
      jar = JarFactory.newInstance(jarConn.getURL());
      
      jar.nextEntry();
      String entryName = jar.getEntryName();
      while (entryName != null)
      {
        if ((entryName.startsWith("META-INF/")) && (entryName.endsWith(".tld")))
        {
          InputStream is = null;
          try
          {
            is = jar.getEntryInputStream();
            XmlErrorHandler handler = tldScanStream(is);
            handler.logFindings(log, jarConn.getURL() + entryName);
            if (is != null) {
              try
              {
                is.close();
              }
              catch (IOException ioe) {}
            }
            jar.nextEntry();
          }
          finally
          {
            if (is != null) {
              try
              {
                is.close();
              }
              catch (IOException ioe) {}
            }
          }
        }
        entryName = jar.getEntryName();
      }
    }
    catch (IOException ioe)
    {
      log.warn(sm.getString("tldConfig.jarFail", new Object[] { jarConn.getURL() }), ioe);
    }
    finally
    {
      if (jar != null) {
        jar.close();
      }
    }
  }
  
  private XmlErrorHandler tldScanStream(InputStream resourceStream)
    throws IOException
  {
    InputSource source = new InputSource(resourceStream);
    
    XmlErrorHandler result = new XmlErrorHandler();
    synchronized (this.tldDigester)
    {
      try
      {
        this.tldDigester.setErrorHandler(result);
        this.tldDigester.push(this);
        this.tldDigester.parse(source);
      }
      catch (SAXException s)
      {
        throw new IOException(s);
      }
      finally
      {
        this.tldDigester.reset();
      }
      return result;
    }
  }
  
  public void lifecycleEvent(LifecycleEvent event)
  {
    try
    {
      this.context = ((Context)event.getLifecycle());
    }
    catch (ClassCastException e)
    {
      log.error(sm.getString("tldConfig.cce", new Object[] { event.getLifecycle() }), e);
      return;
    }
    if (event.getType().equals("after_init"))
    {
      init();
    }
    else if (event.getType().equals("configure_start"))
    {
      try
      {
        execute();
      }
      catch (Exception e)
      {
        log.error(sm.getString("tldConfig.execute", new Object[] { this.context.getName() }), e);
      }
    }
    else if (event.getType().equals("stop"))
    {
      this.taglibUris.clear();
      this.webxmlTaglibUris.clear();
      this.listeners.clear();
    }
  }
  
  private void init()
  {
    if (this.tldDigester == null) {
      this.tldDigester = createTldDigester(this.context.getTldValidation(), this.context.getXmlBlockExternal());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\TldConfig.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */