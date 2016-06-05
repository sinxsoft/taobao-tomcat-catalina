package org.apache.catalina.ssi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;
import org.apache.catalina.util.Strftime;

public final class SSIFlastmod
  implements SSICommand
{
  public SSIFlastmod() {}
  
  public long process(SSIMediator ssiMediator, String commandName, String[] paramNames, String[] paramValues, PrintWriter writer)
  {
    long lastModified = 0L;
    String configErrMsg = ssiMediator.getConfigErrMsg();
    for (int i = 0; i < paramNames.length; i++)
    {
      String paramName = paramNames[i];
      String paramValue = paramValues[i];
      String substitutedValue = ssiMediator.substituteVariables(paramValue);
      try
      {
        if ((paramName.equalsIgnoreCase("file")) || (paramName.equalsIgnoreCase("virtual")))
        {
          boolean virtual = paramName.equalsIgnoreCase("virtual");
          lastModified = ssiMediator.getFileLastModified(substitutedValue, virtual);
          
          Date date = new Date(lastModified);
          String configTimeFmt = ssiMediator.getConfigTimeFmt();
          writer.write(formatDate(date, configTimeFmt));
        }
        else
        {
          ssiMediator.log("#flastmod--Invalid attribute: " + paramName);
          
          writer.write(configErrMsg);
        }
      }
      catch (IOException e)
      {
        ssiMediator.log("#flastmod--Couldn't get last modified for file: " + substitutedValue, e);
        
        writer.write(configErrMsg);
      }
    }
    return lastModified;
  }
  
  protected String formatDate(Date date, String configTimeFmt)
  {
    Strftime strftime = new Strftime(configTimeFmt, Locale.US);
    return strftime.format(date);
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIFlastmod.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */