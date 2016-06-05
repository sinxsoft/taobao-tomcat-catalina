package org.apache.catalina.ssi;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public final class SSIFsize
  implements SSICommand
{
  protected static final int ONE_KILOBYTE = 1024;
  protected static final int ONE_MEGABYTE = 1048576;
  
  public SSIFsize() {}
  
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
          
          long size = ssiMediator.getFileSize(substitutedValue, virtual);
          
          String configSizeFmt = ssiMediator.getConfigSizeFmt();
          writer.write(formatSize(size, configSizeFmt));
        }
        else
        {
          ssiMediator.log("#fsize--Invalid attribute: " + paramName);
          writer.write(configErrMsg);
        }
      }
      catch (IOException e)
      {
        ssiMediator.log("#fsize--Couldn't get size for file: " + substitutedValue, e);
        
        writer.write(configErrMsg);
      }
    }
    return lastModified;
  }
  
  public String repeat(char aChar, int numChars)
  {
    if (numChars < 0) {
      throw new IllegalArgumentException("Num chars can't be negative");
    }
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < numChars; i++) {
      buf.append(aChar);
    }
    return buf.toString();
  }
  
  public String padLeft(String str, int maxChars)
  {
    String result = str;
    int charsToAdd = maxChars - str.length();
    if (charsToAdd > 0) {
      result = repeat(' ', charsToAdd) + str;
    }
    return result;
  }
  
  protected String formatSize(long size, String format)
  {
    String retString = "";
    if (format.equalsIgnoreCase("bytes"))
    {
      DecimalFormat decimalFormat = new DecimalFormat("#,##0");
      retString = decimalFormat.format(size);
    }
    else
    {
      if (size == 0L)
      {
        retString = "0k";
      }
      else if (size < 1024L)
      {
        retString = "1k";
      }
      else if (size < 1048576L)
      {
        retString = Long.toString((size + 512L) / 1024L);
        retString = retString + "k";
      }
      else if (size < 103809024L)
      {
        DecimalFormat decimalFormat = new DecimalFormat("0.0M");
        retString = decimalFormat.format(size / 1048576.0D);
      }
      else
      {
        retString = Long.toString((size + 541696L) / 1048576L);
        
        retString = retString + "M";
      }
      retString = padLeft(retString, 5);
    }
    return retString;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIFsize.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */