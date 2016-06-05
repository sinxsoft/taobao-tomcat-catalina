package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;

public abstract interface Container
  extends Lifecycle
{
  public static final String ADD_CHILD_EVENT = "addChild";
  @Deprecated
  public static final String ADD_MAPPER_EVENT = "addMapper";
  public static final String ADD_VALVE_EVENT = "addValve";
  public static final String REMOVE_CHILD_EVENT = "removeChild";
  @Deprecated
  public static final String REMOVE_MAPPER_EVENT = "removeMapper";
  public static final String REMOVE_VALVE_EVENT = "removeValve";
  
  public abstract String getInfo();
  
  public abstract Loader getLoader();
  
  public abstract void setLoader(Loader paramLoader);
  
  public abstract Log getLogger();
  
  public abstract Manager getManager();
  
  public abstract void setManager(Manager paramManager);
  
  @Deprecated
  public abstract Object getMappingObject();
  
  public abstract ObjectName getObjectName();
  
  public abstract Pipeline getPipeline();
  
  public abstract Cluster getCluster();
  
  public abstract void setCluster(Cluster paramCluster);
  
  public abstract int getBackgroundProcessorDelay();
  
  public abstract void setBackgroundProcessorDelay(int paramInt);
  
  public abstract String getName();
  
  public abstract void setName(String paramString);
  
  public abstract Container getParent();
  
  public abstract void setParent(Container paramContainer);
  
  public abstract ClassLoader getParentClassLoader();
  
  public abstract void setParentClassLoader(ClassLoader paramClassLoader);
  
  public abstract Realm getRealm();
  
  public abstract void setRealm(Realm paramRealm);
  
  public abstract DirContext getResources();
  
  public abstract void setResources(DirContext paramDirContext);
  
  public abstract void backgroundProcess();
  
  public abstract void addChild(Container paramContainer);
  
  public abstract void addContainerListener(ContainerListener paramContainerListener);
  
  public abstract void addPropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract Container findChild(String paramString);
  
  public abstract Container[] findChildren();
  
  public abstract ContainerListener[] findContainerListeners();
  
  @Deprecated
  public abstract void invoke(Request paramRequest, Response paramResponse)
    throws IOException, ServletException;
  
  public abstract void removeChild(Container paramContainer);
  
  public abstract void removeContainerListener(ContainerListener paramContainerListener);
  
  public abstract void removePropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract void fireContainerEvent(String paramString, Object paramObject);
  
  public abstract void logAccess(Request paramRequest, Response paramResponse, long paramLong, boolean paramBoolean);
  
  public abstract AccessLog getAccessLog();
  
  public abstract int getStartStopThreads();
  
  public abstract void setStartStopThreads(int paramInt);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Container.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */