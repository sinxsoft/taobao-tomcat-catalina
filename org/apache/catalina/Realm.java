package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.ietf.jgss.GSSContext;

public abstract interface Realm
{
  public abstract Container getContainer();
  
  public abstract void setContainer(Container paramContainer);
  
  public abstract String getInfo();
  
  public abstract void addPropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
  
  public abstract Principal authenticate(String paramString1, String paramString2);
  
  public abstract Principal authenticate(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6, String paramString7, String paramString8);
  
  public abstract Principal authenticate(GSSContext paramGSSContext, boolean paramBoolean);
  
  public abstract Principal authenticate(X509Certificate[] paramArrayOfX509Certificate);
  
  public abstract void backgroundProcess();
  
  public abstract SecurityConstraint[] findSecurityConstraints(Request paramRequest, Context paramContext);
  
  public abstract boolean hasResourcePermission(Request paramRequest, Response paramResponse, SecurityConstraint[] paramArrayOfSecurityConstraint, Context paramContext)
    throws IOException;
  
  public abstract boolean hasRole(Wrapper paramWrapper, Principal paramPrincipal, String paramString);
  
  public abstract boolean hasUserDataPermission(Request paramRequest, Response paramResponse, SecurityConstraint[] paramArrayOfSecurityConstraint)
    throws IOException;
  
  public abstract void removePropertyChangeListener(PropertyChangeListener paramPropertyChangeListener);
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\Realm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */