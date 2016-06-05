package org.apache.catalina.startup;

import java.net.URL;
import org.apache.catalina.util.SchemaResolver;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;

@Deprecated
public class DigesterFactory
{
  private static final Log log = LogFactory.getLog(DigesterFactory.class);
  
  public DigesterFactory() {}
  
  @Deprecated
  public static Digester newDigester()
  {
    return newDigester(false, false, null);
  }
  
  @Deprecated
  public static Digester newDigester(RuleSet rule)
  {
    return newDigester(false, false, rule);
  }
  
  public static Digester newDigester(boolean xmlValidation, boolean xmlNamespaceAware, RuleSet rule)
  {
    Digester digester = new Digester();
    digester.setNamespaceAware(xmlNamespaceAware);
    digester.setValidating(xmlValidation);
    digester.setUseContextClassLoader(true);
    
    SchemaResolver schemaResolver = new SchemaResolver(digester);
    registerLocalSchema(schemaResolver);
    
    digester.setEntityResolver(schemaResolver);
    if (rule != null) {
      digester.addRuleSet(rule);
    }
    return digester;
  }
  
  protected static void registerLocalSchema(SchemaResolver schemaResolver)
  {
    register("/javax/servlet/resources/j2ee_1_4.xsd", "j2ee_1_4.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_5.xsd", "javaee_5.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_6.xsd", "javaee_6.xsd", schemaResolver);
    
    register("/javax/servlet/resources/xml.xsd", "xml.xsd", schemaResolver);
    
    register("/javax/servlet/resources/XMLSchema.dtd", "XMLSchema.dtd", schemaResolver);
    
    register("/javax/servlet/resources/datatypes.dtd", "datatypes.dtd", schemaResolver);
    
    register("/javax/servlet/jsp/resources/jsp_2_0.xsd", "jsp_2_0.xsd", schemaResolver);
    
    register("/javax/servlet/jsp/resources/jsp_2_1.xsd", "jsp_2_1.xsd", schemaResolver);
    
    register("/javax/servlet/jsp/resources/jsp_2_2.xsd", "jsp_2_2.xsd", schemaResolver);
    
    register("/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd", "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN", schemaResolver);
    
    register("/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd", "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN", schemaResolver);
    
    register("/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd", "web-jsptaglibrary_2_0.xsd", schemaResolver);
    
    register("/javax/servlet/jsp/resources/web-jsptaglibrary_2_1.xsd", "web-jsptaglibrary_2_1.xsd", schemaResolver);
    
    register("/javax/servlet/resources/web-app_2_2.dtd", "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", schemaResolver);
    
    register("/javax/servlet/resources/web-app_2_3.dtd", "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", schemaResolver);
    
    register("/javax/servlet/resources/web-app_2_4.xsd", "web-app_2_4.xsd", schemaResolver);
    
    register("/javax/servlet/resources/web-app_2_5.xsd", "web-app_2_5.xsd", schemaResolver);
    
    register("/javax/servlet/resources/web-app_3_0.xsd", "web-app_3_0.xsd", schemaResolver);
    
    register("/javax/servlet/resources/web-common_3_0.xsd", "web-common_3_0.xsd", schemaResolver);
    
    register("/javax/servlet/resources/web-fragment_3_0.xsd", "web-fragment_3_0.xsd", schemaResolver);
    
    register("/javax/servlet/resources/j2ee_web_services_1_1.xsd", "j2ee_web_services_1_1.xsd", schemaResolver);
    
    register("/javax/servlet/resources/j2ee_web_services_client_1_1.xsd", "j2ee_web_services_client_1_1.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_web_services_1_2.xsd", "javaee_web_services_1_2.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_web_services_client_1_2.xsd", "javaee_web_services_client_1_2.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_web_services_1_3.xsd", "javaee_web_services_1_3.xsd", schemaResolver);
    
    register("/javax/servlet/resources/javaee_web_services_client_1_3.xsd", "javaee_web_services_client_1_3.xsd", schemaResolver);
  }
  
  protected static void register(String resourceURL, String resourcePublicId, SchemaResolver schemaResolver)
  {
    URL url = DigesterFactory.class.getResource(resourceURL);
    if (url == null) {
      log.warn("Could not get url for " + resourceURL);
    } else {
      schemaResolver.register(resourcePublicId, url.toString());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\DigesterFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */