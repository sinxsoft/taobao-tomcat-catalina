package org.apache.catalina;

import java.beans.PropertyChangeListener;

public abstract interface Loader
{
  public abstract void backgroundProcess();
  
  public abstract ClassLoader getClassLoader();
  
  public abstract Container getContainer();
  
  public abstract void setContainer(Container paramContainer);
  
  public abstract boolean getDelegate();
  
  public abstract void setDelegate(boolean paramBoolean);
  
  public abstract String getInfo();
  
  public abstract boolean getReloadable();
  
  public abstract void setReloadable(boolean paramBoolean);
  
  public abstract void addPropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract void addRepository(String paramString);
  
  public abstract String[] findRepositories();
  
  public abstract boolean modified();
  
  public abstract void removePropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Loader.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */