package org.apache.catalina;

public abstract interface Pipeline
{
  public abstract Valve getBasic();
  
  public abstract void setBasic(Valve paramValve);
  
  public abstract void addValve(Valve paramValve);
  
  public abstract Valve[] getValves();
  
  public abstract void removeValve(Valve paramValve);
  
  public abstract Valve getFirst();
  
  public abstract boolean isAsyncSupported();
  
  public abstract Container getContainer();
  
  public abstract void setContainer(Container paramContainer);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Pipeline.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */