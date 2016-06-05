package org.apache.catalina.realm;

import java.security.Principal;
import java.security.cert.X509Certificate;

public class X509SubjectDnRetriever
  implements X509UsernameRetriever
{
  public X509SubjectDnRetriever() {}
  
  public String getUsername(X509Certificate clientCert)
  {
    return clientCert.getSubjectDN().getName();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\X509SubjectDnRetriever.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */