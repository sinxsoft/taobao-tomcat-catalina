package com.taobao.tomcat.container.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JbossCompat
{
  public JbossCompat() {}
  
  public static synchronized String parseJbossWebXml(File appDir)
  {
    File jbossWebXml = new File(appDir, "WEB-INF" + File.separator + "jboss-web.xml");
    if (!jbossWebXml.exists()) {
      return null;
    }
    Document doc = getDocument(jbossWebXml);
    if (doc == null) {
      return null;
    }
    NodeList jbossWebNodeList = doc.getElementsByTagName("jboss-web");
    if ((jbossWebNodeList != null) && (jbossWebNodeList.getLength() > 0))
    {
      Node jbossWebNode = jbossWebNodeList.item(0);
      return getContextRootPath(jbossWebNode.getChildNodes());
    }
    return null;
  }
  
  public static synchronized String parseJbossWebXml(InputStream stream)
  {
    Document doc = getDocument(stream);
    if (doc == null) {
      return null;
    }
    NodeList jbossWebNodeList = doc.getElementsByTagName("jboss-web");
    if ((jbossWebNodeList != null) && (jbossWebNodeList.getLength() > 0))
    {
      Node jbossWebNode = jbossWebNodeList.item(0);
      return getContextRootPath(jbossWebNode.getChildNodes());
    }
    return null;
  }
  
  public static synchronized String parseApplicationXml(File dir)
  {
    File applicationXml = new File(dir, "META-INF" + File.separator + "application.xml");
    if (!applicationXml.exists()) {
      return null;
    }
    Document doc = getDocument(applicationXml);
    if (doc == null) {
      return null;
    }
    NodeList applicationNodeList = doc.getElementsByTagName("application");
    if ((applicationNodeList != null) && (applicationNodeList.getLength() > 0))
    {
      Node applicationNode = applicationNodeList.item(0);
      if (applicationNode != null)
      {
        Node moduleNode = getNode(applicationNode.getChildNodes(), "module");
        if (moduleNode != null)
        {
          Node webNode = getNode(moduleNode.getChildNodes(), "web");
          if (webNode != null) {
            return getContextRootPath(webNode.getChildNodes());
          }
        }
      }
    }
    return null;
  }
  
  private static Document getDocument(File file)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    Document doc;
    try
    {
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(new JbossEntityResolver());
      doc = builder.parse(file);
    }
    catch (Exception e)
    {
      System.err.println("Warning: failed to parse xml file: " + file + ". error message: " + e.getLocalizedMessage());
      
      return null;
    }
    doc.getDocumentElement().normalize();
    
    return doc;
  }
  
  private static Document getDocument(InputStream stream)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    Document doc;
    try
    {
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(new JbossEntityResolver());
      doc = builder.parse(stream);
    }
    catch (Exception e)
    {
      System.err.println("Warning: failed to parse Stream file: jboss-web.xml error message: " + e.getLocalizedMessage());
      
      return null;
    }
    doc.getDocumentElement().normalize();
    
    return doc;
  }
  
  private static String getContextRootPath(NodeList list)
  {
    String path = null;
    Node contextRootNode = getNode(list, "context-root");
    if (contextRootNode != null) {
      path = ((Element)contextRootNode).getTextContent();
    }
    if (path != null)
    {
      path = path.trim();
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
    }
    return path;
  }
  
  private static Node getNode(NodeList list, String tag)
  {
    if ((list != null) && (list.getLength() > 0)) {
      for (int i = 0; i < list.getLength(); i++)
      {
        Node node = list.item(i);
        if ((node.getNodeType() == 1) && (tag.equals(node.getNodeName()))) {
          return node;
        }
      }
    }
    return null;
  }
  
  private static Map<String, URL> redirectMap = new HashMap();
  private static URL jbossWebUrl;
  private static URL applicationUrl;
  
  static
  {
    ClassLoader cl = JbossCompat.class.getClassLoader();
    jbossWebUrl = cl.getResource("com/taobao/tomcat/util/jboss-web_4_0.dtd");
    applicationUrl = cl.getResource("com/taobao/tomcat/util/application_1_3.dtd");
    
    redirectMap.put("-//JBoss//DTD Web Application 2.4//EN", jbossWebUrl);
    redirectMap.put("http://www.jboss.org/j2ee/dtd/jboss-web_4_0.dtd", jbossWebUrl);
    
    redirectMap.put("-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN", applicationUrl);
    redirectMap.put("http://java.sun.com/dtd/application_1_3.dtd", applicationUrl);
    
    URL serviceRefUrl = cl.getResource("com/taobao/tomcat/util/service-ref_4_0.dtd");
    redirectMap.put("-//JBoss//DTD Web Service Reference 4.0//EN", serviceRefUrl);
    redirectMap.put("http://www.jboss.org/j2ee/dtd/service-ref_4_0.dtd", serviceRefUrl);
  }
  
  private static class JbossEntityResolver
    implements EntityResolver
  {
    private JbossEntityResolver() {}
    
    public InputSource resolveEntity(String pid, String sid)
      throws SAXException, IOException
    {
      URL entity = null;
      if (pid != null) {
        entity = (URL)JbossCompat.redirectMap.get(pid);
      }
      if (entity == null) {
        entity = (URL)JbossCompat.redirectMap.get(sid);
      }
      if (entity == null) {
        if (((pid != null) && (pid.contains("JBoss"))) || ((sid != null) && (sid.contains("jboss-web")))) {
          entity = JbossCompat.jbossWebUrl;
        } else if (((pid != null) && (pid.contains("Sun Microsystems"))) || ((sid != null) && (sid.contains("application")))) {
          entity = JbossCompat.applicationUrl;
        }
      }
      if (entity != null) {
        try
        {
          InputStream in = entity.openStream();
          InputSource is = new InputSource(in);
          is.setSystemId(sid);
          return is;
        }
        catch (IOException e)
        {
          throw new RuntimeException(e);
        }
      }
      throw new RuntimeException("Cannot find DTD: publicId='" + pid + "' systemId='" + sid + "'.");
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\host\JbossCompat.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */