package org.apache.catalina;

import org.apache.catalina.core.StandardPipeline;

@Deprecated
public class CatalinaFactory
{
  private static CatalinaFactory factory = new CatalinaFactory();
  
  public static CatalinaFactory getFactory()
  {
    return factory;
  }
  
  private CatalinaFactory() {}
  
  @Deprecated
  public String getDefaultPipelineClassName()
  {
    return StandardPipeline.class.getName();
  }
  
  public Pipeline createPipeline(Container container)
  {
    Pipeline pipeline = new StandardPipeline();
    pipeline.setContainer(container);
    return pipeline;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\CatalinaFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */