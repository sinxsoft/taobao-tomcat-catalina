package com.taobao.tomcat.container.context.pandora;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SharedClassRepository
  implements ClassRepository
{
  private Map<String, Class<?>> moduleRepoAccessor;
  
  public SharedClassRepository() {}
  
  public Class<?> resolveClass(String className)
  {
    if (this.moduleRepoAccessor != null) {
      return (Class)this.moduleRepoAccessor.get(className);
    }
    return null;
  }
  
  public Collection<Class<?>> getClasses()
  {
    if (this.moduleRepoAccessor != null) {
      return this.moduleRepoAccessor.values();
    }
    return Collections.emptySet();
  }
  
  public void setModuleRepository(Map<String, Class<?>> moduleRepoAccessor)
  {
    this.moduleRepoAccessor = moduleRepoAccessor;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\tomcat\container\context\pandora\SharedClassRepository.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */