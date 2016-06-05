package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;

public abstract interface Store
{
  public abstract String getInfo();
  
  public abstract Manager getManager();
  
  public abstract void setManager(Manager paramManager);
  
  public abstract int getSize()
    throws IOException;
  
  public abstract void addPropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract String[] keys()
    throws IOException;
  
  public abstract Session load(String paramString)
    throws ClassNotFoundException, IOException;
  
  public abstract void remove(String paramString)
    throws IOException;
  
  public abstract void clear()
    throws IOException;
  
  public abstract void removePropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract void save(Session paramSession)
    throws IOException;
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Store.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */