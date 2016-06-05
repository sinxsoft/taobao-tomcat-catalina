package org.apache.catalina.deploy;

public class InjectionTarget
{
  private String targetClass;
  private String targetName;
  
  public InjectionTarget() {}
  
  public InjectionTarget(String targetClass, String targetName)
  {
    this.targetClass = targetClass;
    this.targetName = targetName;
  }
  
  public String getTargetClass()
  {
    return this.targetClass;
  }
  
  public void setTargetClass(String targetClass)
  {
    this.targetClass = targetClass;
  }
  
  public String getTargetName()
  {
    return this.targetName;
  }
  
  public void setTargetName(String targetName)
  {
    this.targetName = targetName;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\InjectionTarget.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */