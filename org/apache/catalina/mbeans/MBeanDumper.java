package org.apache.catalina.mbeans;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;

public class MBeanDumper
{
  private static final Log log = LogFactory.getLog(MBeanDumper.class);
  private static final String CRLF = "\r\n";
  
  public MBeanDumper() {}
  
  public static String dumpBeans(MBeanServer mbeanServer, Set<ObjectName> names)
  {
    StringBuilder buf = new StringBuilder();
    Iterator<ObjectName> it = names.iterator();
    while (it.hasNext())
    {
      ObjectName oname = (ObjectName)it.next();
      buf.append("Name: ");
      buf.append(oname.toString());
      buf.append("\r\n");
      try
      {
        MBeanInfo minfo = mbeanServer.getMBeanInfo(oname);
        
        String code = minfo.getClassName();
        if ("org.apache.commons.modeler.BaseModelMBean".equals(code)) {
          code = (String)mbeanServer.getAttribute(oname, "modelerType");
        }
        buf.append("modelerType: ");
        buf.append(code);
        buf.append("\r\n");
        
        MBeanAttributeInfo[] attrs = minfo.getAttributes();
        Object value = null;
        for (int i = 0; i < attrs.length; i++) {
          if (attrs[i].isReadable())
          {
            String attName = attrs[i].getName();
            if ((!"modelerType".equals(attName)) && 
              (attName.indexOf("=") < 0) && (attName.indexOf(":") < 0) && (attName.indexOf(" ") < 0))
            {
              try
              {
                value = mbeanServer.getAttribute(oname, attName);
              }
              catch (JMRuntimeException rme)
              {
                Throwable cause = rme.getCause();
                if ((cause instanceof UnsupportedOperationException))
                {
                  if (log.isDebugEnabled()) {
                    log.debug("Error getting attribute " + oname + " " + attName, rme);
                  }
                }
                else if ((cause instanceof NullPointerException))
                {
                  if (log.isDebugEnabled()) {
                    log.debug("Error getting attribute " + oname + " " + attName, rme);
                  }
                }
                else {
                  log.error("Error getting attribute " + oname + " " + attName, rme);
                }
                continue;
              }
              catch (Throwable t)
              {
                ExceptionUtils.handleThrowable(t);
                log.error("Error getting attribute " + oname + " " + attName, t);
                
                continue;
              }
              if (value != null) {
                try
                {
                  Class<?> c = value.getClass();
                  String valueString;
                  
                  if (c.isArray())
                  {
                    int len = Array.getLength(value);
                    StringBuilder sb = new StringBuilder("Array[" + c.getComponentType().getName() + "] of length " + len);
                    if (len > 0) {
                      sb.append("\r\n");
                    }
                    for (int j = 0; j < len; j++)
                    {
                      sb.append("\t");
                      Object item = Array.get(value, j);
                      if (item == null) {
                        sb.append("NULL VALUE");
                      } else {
                        try
                        {
                          sb.append(escape(item.toString()));
                        }
                        catch (Throwable t)
                        {
                          ExceptionUtils.handleThrowable(t);
                          sb.append("NON-STRINGABLE VALUE");
                        }
                      }
                      if (j < len - 1) {
                        sb.append("\r\n");
                      }
                    }
                    valueString = sb.toString();
                  }
                  else
                  {
                    valueString = escape(value.toString());
                  }
                  buf.append(attName);
                  buf.append(": ");
                  buf.append(valueString);
                  buf.append("\r\n");
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
      catch (Throwable t)
      {
        ExceptionUtils.handleThrowable(t);
      }
      buf.append("\r\n");
    }
    return buf.toString();
  }
  
  public static String escape(String value)
  {
    int idx = value.indexOf("\n");
    if (idx < 0) {
      return value;
    }
    int prev = 0;
    StringBuilder sb = new StringBuilder();
    while (idx >= 0)
    {
      appendHead(sb, value, prev, idx);
      
      sb.append("\\n\n ");
      prev = idx + 1;
      if (idx == value.length() - 1) {
        break;
      }
      idx = value.indexOf('\n', idx + 1);
    }
    if (prev < value.length()) {
      appendHead(sb, value, prev, value.length());
    }
    return sb.toString();
  }
  
  private static void appendHead(StringBuilder sb, String value, int start, int end)
  {
    if (end < 1) {
      return;
    }
    int pos = start;
    while (end - pos > 78)
    {
      sb.append(value.substring(pos, pos + 78));
      sb.append("\n ");
      pos += 78;
    }
    sb.append(value.substring(pos, end));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\MBeanDumper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */