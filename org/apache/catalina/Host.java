package org.apache.catalina;

import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public abstract interface Host
  extends Container
{
  public static final String ADD_ALIAS_EVENT = "addAlias";
  public static final String REMOVE_ALIAS_EVENT = "removeAlias";
  
  public abstract String getXmlBase();
  
  public abstract void setXmlBase(String paramString);
  
  public abstract String getAppBase();
  
  public abstract void setAppBase(String paramString);
  
  public abstract boolean getAutoDeploy();
  
  public abstract void setAutoDeploy(boolean paramBoolean);
  
  public abstract String getConfigClass();
  
  public abstract void setConfigClass(String paramString);
  
  public abstract boolean getDeployOnStartup();
  
  public abstract void setDeployOnStartup(boolean paramBoolean);
  
  public abstract String getDeployIgnore();
  
  public abstract Pattern getDeployIgnorePattern();
  
  public abstract void setDeployIgnore(String paramString);
  
  public abstract ExecutorService getStartStopExecutor();
  
  public abstract boolean getUndeployOldVersions();
  
  public abstract void setUndeployOldVersions(boolean paramBoolean);
  
  public abstract void addAlias(String paramString);
  
  public abstract String[] findAliases();
  
  public abstract void removeAlias(String paramString);
  
  public abstract boolean getCreateDirs();
  
  public abstract void setCreateDirs(boolean paramBoolean);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Host.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */