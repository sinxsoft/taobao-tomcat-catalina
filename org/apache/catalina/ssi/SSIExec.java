package org.apache.catalina.ssi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.apache.catalina.util.IOTools;

public class SSIExec
  implements SSICommand
{
  protected SSIInclude ssiInclude = new SSIInclude();
  protected static final int BUFFER_SIZE = 1024;
  
  public SSIExec() {}
  
  public long process(SSIMediator ssiMediator, String commandName, String[] paramNames, String[] paramValues, PrintWriter writer)
  {
    long lastModified = 0L;
    String configErrMsg = ssiMediator.getConfigErrMsg();
    String paramName = paramNames[0];
    String paramValue = paramValues[0];
    String substitutedValue = ssiMediator.substituteVariables(paramValue);
    if (paramName.equalsIgnoreCase("cgi"))
    {
      lastModified = this.ssiInclude.process(ssiMediator, "include", new String[] { "virtual" }, new String[] { substitutedValue }, writer);
    }
    else if (paramName.equalsIgnoreCase("cmd"))
    {
      boolean foundProgram = false;
      try
      {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(substitutedValue);
        foundProgram = true;
        BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        
        BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        
        char[] buf = new char['Ѐ'];
        IOTools.flow(stdErrReader, writer, buf);
        IOTools.flow(stdOutReader, writer, buf);
        proc.waitFor();
        lastModified = System.currentTimeMillis();
      }
      catch (InterruptedException e)
      {
        ssiMediator.log("Couldn't exec file: " + substitutedValue, e);
        writer.write(configErrMsg);
      }
      catch (IOException e)
      {
        if (!foundProgram) {}
        ssiMediator.log("Couldn't exec file: " + substitutedValue, e);
      }
    }
    return lastModified;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIExec.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */