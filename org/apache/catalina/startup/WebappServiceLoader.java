package org.apache.catalina.startup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

public class WebappServiceLoader<T>
{
  private static final String LIB = "/WEB-INF/lib/";
  private static final String SERVICES = "META-INF/services/";
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private final ServletContext context;
  private final Pattern containerSciFilterPattern;
  
  public WebappServiceLoader(ServletContext context, String containerSciFilter)
  {
    this.context = context;
    if ((containerSciFilter != null) && (containerSciFilter.length() > 0)) {
      this.containerSciFilterPattern = Pattern.compile(containerSciFilter);
    } else {
      this.containerSciFilterPattern = null;
    }
  }
  
  public List<T> load(Class<T> serviceType)
    throws IOException
  {
    String configFile = "META-INF/services/" + serviceType.getName();
    
    LinkedHashSet<String> applicationServicesFound = new LinkedHashSet();
    LinkedHashSet<String> containerServicesFound = new LinkedHashSet();
    
    ClassLoader loader = this.context.getClassLoader();
    
    List<String> orderedLibs = (List)this.context.getAttribute("javax.servlet.context.orderedLibs");
    if (orderedLibs != null)
    {
      for (String lib : orderedLibs)
      {
        URL jarUrl = this.context.getResource("/WEB-INF/lib/" + lib);
        if (jarUrl != null)
        {
          String base = jarUrl.toExternalForm();

          URL url;
          if (base.endsWith("/")) {
            url = new URL(base + configFile);
          } else {
            url = new URL("jar:" + base + "!/" + configFile);
          }
          try
          {
            parseConfigFile(applicationServicesFound, url);
          }
          catch (FileNotFoundException e) {}
        }
      }
      loader = loader.getParent();
    }
    Enumeration<URL> resources;

    if (loader == null) {
      resources = ClassLoader.getSystemResources(configFile);
    } else {
      resources = loader.getResources(configFile);
    }
    while (resources.hasMoreElements()) {
      parseConfigFile(containerServicesFound, (URL)resources.nextElement());
    }
    if (this.containerSciFilterPattern != null)
    {
      Iterator<String> iter = containerServicesFound.iterator();
      while (iter.hasNext()) {
        if (this.containerSciFilterPattern.matcher((CharSequence)iter.next()).find()) {
          iter.remove();
        }
      }
    }
    containerServicesFound.addAll(applicationServicesFound);
    if (containerServicesFound.isEmpty()) {
      return Collections.emptyList();
    }
    return loadServices(serviceType, containerServicesFound);
  }
  
  private void parseConfigFile(LinkedHashSet<String> servicesFound, URL url)
    throws IOException
  {
    InputStream is = null;
    try
    {
      is = url.openStream();
      InputStreamReader in = new InputStreamReader(is, UTF8);
      BufferedReader reader = new BufferedReader(in);
      String line;
      while ((line = reader.readLine()) != null)
      {
        int i = line.indexOf('#');
        if (i >= 0) {
          line = line.substring(0, i);
        }
        line = line.trim();
        if (line.length() != 0) {
          servicesFound.add(line);
        }
      }
    }
    finally
    {
      if (is != null) {
        is.close();
      }
    }
  }
  
  private List<T> loadServices(Class<T> serviceType, LinkedHashSet<String> servicesFound)
    throws IOException
  {
    ClassLoader loader = this.context.getClassLoader();
    List<T> services = new ArrayList(servicesFound.size());
    for (String serviceClass : servicesFound) {
      try
      {
        Class<?> clazz = Class.forName(serviceClass, true, loader);
        services.add(serviceType.cast(clazz.newInstance()));
      }
      catch (ClassNotFoundException e)
      {
        throw new IOException(e);
      }
      catch (InstantiationException e)
      {
        throw new IOException(e);
      }
      catch (IllegalAccessException e)
      {
        throw new IOException(e);
      }
      catch (ClassCastException e)
      {
        throw new IOException(e);
      }
    }
    return Collections.unmodifiableList(services);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\WebappServiceLoader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */