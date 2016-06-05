package org.apache.catalina.ssi;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

public abstract interface SSIExternalResolver
{
  public abstract void addVariableNames(Collection<String> paramCollection);
  
  public abstract String getVariableValue(String paramString);
  
  public abstract void setVariableValue(String paramString1, String paramString2);
  
  public abstract Date getCurrentDate();
  
  public abstract long getFileSize(String paramString, boolean paramBoolean)
    throws IOException;
  
  public abstract long getFileLastModified(String paramString, boolean paramBoolean)
    throws IOException;
  
  public abstract String getFileText(String paramString, boolean paramBoolean)
    throws IOException;
  
  public abstract void log(String paramString, Throwable paramThrowable);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIExternalResolver.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */