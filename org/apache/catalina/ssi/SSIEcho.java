package org.apache.catalina.ssi;

import java.io.PrintWriter;

public class SSIEcho
  implements SSICommand
{
  protected static final String DEFAULT_ENCODING = "entity";
  protected static final String MISSING_VARIABLE_VALUE = "(none)";
  
  public SSIEcho() {}
  
  public long process(SSIMediator ssiMediator, String commandName, String[] paramNames, String[] paramValues, PrintWriter writer)
  {
    String encoding = "entity";
    String originalValue = null;
    String errorMessage = ssiMediator.getConfigErrMsg();
    for (int i = 0; i < paramNames.length; i++)
    {
      String paramName = paramNames[i];
      String paramValue = paramValues[i];
      if (paramName.equalsIgnoreCase("var"))
      {
        originalValue = paramValue;
      }
      else if (paramName.equalsIgnoreCase("encoding"))
      {
        if (isValidEncoding(paramValue))
        {
          encoding = paramValue;
        }
        else
        {
          ssiMediator.log("#echo--Invalid encoding: " + paramValue);
          writer.write(errorMessage);
        }
      }
      else
      {
        ssiMediator.log("#echo--Invalid attribute: " + paramName);
        writer.write(errorMessage);
      }
    }
    String variableValue = ssiMediator.getVariableValue(originalValue, encoding);
    if (variableValue == null) {
      variableValue = "(none)";
    }
    writer.write(variableValue);
    return System.currentTimeMillis();
  }
  
  protected boolean isValidEncoding(String encoding)
  {
    return (encoding.equalsIgnoreCase("url")) || (encoding.equalsIgnoreCase("entity")) || (encoding.equalsIgnoreCase("none"));
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIEcho.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */