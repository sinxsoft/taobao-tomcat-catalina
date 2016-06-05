package org.apache.catalina.startup;

import java.util.HashSet;
import java.util.Set;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Deprecated
public class XmlErrorHandler
  implements ErrorHandler
{
  private static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  private Set<SAXParseException> errors = new HashSet();
  private Set<SAXParseException> warnings = new HashSet();
  
  public XmlErrorHandler() {}
  
  public void error(SAXParseException exception)
    throws SAXException
  {
    this.errors.add(exception);
  }
  
  public void fatalError(SAXParseException exception)
    throws SAXException
  {
    throw exception;
  }
  
  public void warning(SAXParseException exception)
    throws SAXException
  {
    this.warnings.add(exception);
  }
  
  public Set<SAXParseException> getErrors()
  {
    return this.errors;
  }
  
  public Set<SAXParseException> getWarnings()
  {
    return this.warnings;
  }
  
  public void logFindings(Log log, String source)
  {
    for (SAXParseException e : getWarnings()) {
      log.warn(sm.getString("xmlErrorHandler.warning", new Object[] { e.getMessage(), source }));
    }
    for (SAXParseException e : getErrors()) {
      log.warn(sm.getString("xmlErrorHandler.error", new Object[] { e.getMessage(), source }));
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\XmlErrorHandler.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */