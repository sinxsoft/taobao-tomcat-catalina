package org.apache.catalina.ssi;

import java.io.PrintWriter;

public class SSISet
  implements SSICommand
{
  public SSISet() {}
  
  public long process(SSIMediator ssiMediator, String commandName, String[] paramNames, String[] paramValues, PrintWriter writer)
    throws SSIStopProcessingException
  {
    long lastModified = 0L;
    String errorMessage = ssiMediator.getConfigErrMsg();
    String variableName = null;
    for (int i = 0; i < paramNames.length; i++)
    {
      String paramName = paramNames[i];
      String paramValue = paramValues[i];
      if (paramName.equalsIgnoreCase("var"))
      {
        variableName = paramValue;
      }
      else if (paramName.equalsIgnoreCase("value"))
      {
        if (variableName != null)
        {
          String substitutedValue = ssiMediator.substituteVariables(paramValue);
          
          ssiMediator.setVariableValue(variableName, substitutedValue);
          
          lastModified = System.currentTimeMillis();
        }
        else
        {
          ssiMediator.log("#set--no variable specified");
          writer.write(errorMessage);
          throw new SSIStopProcessingException();
        }
      }
      else
      {
        ssiMediator.log("#set--Invalid attribute: " + paramName);
        writer.write(errorMessage);
        throw new SSIStopProcessingException();
      }
    }
    return lastModified;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSISet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */