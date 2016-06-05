package org.apache.catalina.ssi;

import java.io.PrintWriter;

public abstract interface SSICommand
{
  public abstract long process(SSIMediator paramSSIMediator, String paramString, String[] paramArrayOfString1, String[] paramArrayOfString2, PrintWriter paramPrintWriter)
    throws SSIStopProcessingException;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSICommand.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */