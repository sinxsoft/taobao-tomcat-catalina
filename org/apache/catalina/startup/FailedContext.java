package org.apache.catalina.startup;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletSecurityElement;
import javax.servlet.descriptor.JspConfigDescriptor;
import org.apache.catalina.AccessLog;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.deploy.ApplicationListener;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.juli.logging.Log;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.res.StringManager;

public class FailedContext
  extends LifecycleMBeanBase
  implements Context
{
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.startup");
  private URL configFile;
  private String docBase;
  
  public FailedContext() {}
  
  public URL getConfigFile()
  {
    return this.configFile;
  }
  
  public void setConfigFile(URL configFile)
  {
    this.configFile = configFile;
  }
  
  public String getDocBase()
  {
    return this.docBase;
  }
  
  public void setDocBase(String docBase)
  {
    this.docBase = docBase;
  }
  
  private String name = null;
  private Container parent;
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public Container getParent()
  {
    return this.parent;
  }
  
  public void setParent(Container parent)
  {
    this.parent = parent;
  }
  
  private String path = null;
  
  public String getPath()
  {
    return this.path;
  }
  
  public void setPath(String path)
  {
    this.path = path;
  }
  
  private String webappVersion = null;
  
  public String getWebappVersion()
  {
    return this.webappVersion;
  }
  
  public void setWebappVersion(String webappVersion)
  {
    this.webappVersion = webappVersion;
  }
  
  @Deprecated
  protected String getDomainInternal()
  {
    return MBeanUtils.getDomain(this);
  }
  
  protected String getObjectNameKeyProperties()
  {
    StringBuilder keyProperties = new StringBuilder("j2eeType=WebModule,name=//");
    
    String hostname = getParent().getName();
    if (hostname == null) {
      keyProperties.append("DEFAULT");
    } else {
      keyProperties.append(hostname);
    }
    String contextName = getName();
    if (!contextName.startsWith("/")) {
      keyProperties.append('/');
    }
    keyProperties.append(contextName);
    
    keyProperties.append(",J2EEApplication=none,J2EEServer=none");
    
    return keyProperties.toString();
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    throw new LifecycleException(sm.getString("failedContext.start", new Object[] { getName() }));
  }
  
  protected void stopInternal()
    throws LifecycleException
  {}
  
  public void addWatchedResource(String name) {}
  
  public String[] findWatchedResources()
  {
    return new String[0];
  }
  
  public void removeWatchedResource(String name) {}
  
  public void addChild(Container child) {}
  
  public Container findChild(String name)
  {
    return null;
  }
  
  public Container[] findChildren()
  {
    return new Container[0];
  }
  
  public void removeChild(Container child) {}
  
  public String toString()
  {
    return getName();
  }
  
  public Loader getLoader()
  {
    return null;
  }
  
  public void setLoader(Loader loader) {}
  
  public Log getLogger()
  {
    return null;
  }
  
  public Manager getManager()
  {
    return null;
  }
  
  public void setManager(Manager manager) {}
  
  public Pipeline getPipeline()
  {
    return null;
  }
  
  public Cluster getCluster()
  {
    return null;
  }
  
  public void setCluster(Cluster cluster) {}
  
  public int getBackgroundProcessorDelay()
  {
    return -1;
  }
  
  public void setBackgroundProcessorDelay(int delay) {}
  
  public ClassLoader getParentClassLoader()
  {
    return null;
  }
  
  public void setParentClassLoader(ClassLoader parent) {}
  
  public Realm getRealm()
  {
    return null;
  }
  
  public void setRealm(Realm realm) {}
  
  public DirContext getResources()
  {
    return null;
  }
  
  public void setResources(DirContext resources) {}
  
  public void backgroundProcess() {}
  
  public void addContainerListener(ContainerListener listener) {}
  
  public ContainerListener[] findContainerListeners()
  {
    return null;
  }
  
  public void removeContainerListener(ContainerListener listener) {}
  
  public void addPropertyChangeListener(PropertyChangeListener listener) {}
  
  public void removePropertyChangeListener(PropertyChangeListener listener) {}
  
  public void invoke(Request request, Response response)
    throws IOException, ServletException
  {}
  
  public void fireContainerEvent(String type, Object data) {}
  
  public void logAccess(Request request, Response response, long time, boolean useDefault) {}
  
  public AccessLog getAccessLog()
  {
    return null;
  }
  
  public int getStartStopThreads()
  {
    return 0;
  }
  
  public void setStartStopThreads(int startStopThreads) {}
  
  public boolean getAllowCasualMultipartParsing()
  {
    return false;
  }
  
  public void setAllowCasualMultipartParsing(boolean allowCasualMultipartParsing) {}
  
  public Object[] getApplicationEventListeners()
  {
    return null;
  }
  
  public void setApplicationEventListeners(Object[] listeners) {}
  
  public Object[] getApplicationLifecycleListeners()
  {
    return null;
  }
  
  public void setApplicationLifecycleListeners(Object[] listeners) {}
  
  public boolean getAvailable()
  {
    return false;
  }
  
  @Deprecated
  public CharsetMapper getCharsetMapper()
  {
    return null;
  }
  
  @Deprecated
  public void setCharsetMapper(CharsetMapper mapper) {}
  
  public String getCharset(Locale locale)
  {
    return null;
  }
  
  public boolean getConfigured()
  {
    return false;
  }
  
  public void setConfigured(boolean configured) {}
  
  public boolean getCookies()
  {
    return false;
  }
  
  public void setCookies(boolean cookies) {}
  
  public String getSessionCookieName()
  {
    return null;
  }
  
  public void setSessionCookieName(String sessionCookieName) {}
  
  public boolean getUseHttpOnly()
  {
    return false;
  }
  
  public void setUseHttpOnly(boolean useHttpOnly) {}
  
  public String getSessionCookieDomain()
  {
    return null;
  }
  
  public void setSessionCookieDomain(String sessionCookieDomain) {}
  
  public String getSessionCookiePath()
  {
    return null;
  }
  
  public void setSessionCookiePath(String sessionCookiePath) {}
  
  public boolean getSessionCookiePathUsesTrailingSlash()
  {
    return false;
  }
  
  public void setSessionCookiePathUsesTrailingSlash(boolean sessionCookiePathUsesTrailingSlash) {}
  
  public boolean getCrossContext()
  {
    return false;
  }
  
  public void setCrossContext(boolean crossContext) {}
  
  public String getAltDDName()
  {
    return null;
  }
  
  public void setAltDDName(String altDDName) {}
  
  public String getDisplayName()
  {
    return null;
  }
  
  public void setDisplayName(String displayName) {}
  
  public boolean getDistributable()
  {
    return false;
  }
  
  public void setDistributable(boolean distributable) {}
  
  public String getEncodedPath()
  {
    return null;
  }
  
  public boolean getIgnoreAnnotations()
  {
    return false;
  }
  
  public void setIgnoreAnnotations(boolean ignoreAnnotations) {}
  
  public LoginConfig getLoginConfig()
  {
    return null;
  }
  
  public void setLoginConfig(LoginConfig config) {}
  
  public Mapper getMapper()
  {
    return null;
  }
  
  public NamingResources getNamingResources()
  {
    return null;
  }
  
  public void setNamingResources(NamingResources namingResources) {}
  
  public String getPublicId()
  {
    return null;
  }
  
  public void setPublicId(String publicId) {}
  
  public boolean getReloadable()
  {
    return false;
  }
  
  public void setReloadable(boolean reloadable) {}
  
  public boolean getOverride()
  {
    return false;
  }
  
  public void setOverride(boolean override) {}
  
  public boolean getPrivileged()
  {
    return false;
  }
  
  public void setPrivileged(boolean privileged) {}
  
  public ServletContext getServletContext()
  {
    return null;
  }
  
  public int getSessionTimeout()
  {
    return 0;
  }
  
  public void setSessionTimeout(int timeout) {}
  
  public boolean getSwallowAbortedUploads()
  {
    return false;
  }
  
  public void setSwallowAbortedUploads(boolean swallowAbortedUploads) {}
  
  public boolean getSwallowOutput()
  {
    return false;
  }
  
  public void setSwallowOutput(boolean swallowOutput) {}
  
  public String getWrapperClass()
  {
    return null;
  }
  
  public void setWrapperClass(String wrapperClass) {}
  
  public boolean getXmlNamespaceAware()
  {
    return false;
  }
  
  public void setXmlNamespaceAware(boolean xmlNamespaceAware) {}
  
  public boolean getXmlValidation()
  {
    return false;
  }
  
  public void setXmlValidation(boolean xmlValidation) {}
  
  public void setTldValidation(boolean tldValidation) {}
  
  public boolean getXmlBlockExternal()
  {
    return true;
  }
  
  public void setXmlBlockExternal(boolean xmlBlockExternal) {}
  
  public boolean getTldValidation()
  {
    return false;
  }
  
  public boolean getTldNamespaceAware()
  {
    return true;
  }
  
  public void setTldNamespaceAware(boolean tldNamespaceAware) {}
  
  public JarScanner getJarScanner()
  {
    return null;
  }
  
  public void setJarScanner(JarScanner jarScanner) {}
  
  public Authenticator getAuthenticator()
  {
    return null;
  }
  
  public void setLogEffectiveWebXml(boolean logEffectiveWebXml) {}
  
  public boolean getLogEffectiveWebXml()
  {
    return false;
  }
  
  public void addApplicationListener(ApplicationListener listener) {}
  
  public void addApplicationListener(String listener) {}
  
  public String[] findApplicationListeners()
  {
    return null;
  }
  
  public void removeApplicationListener(String listener) {}
  
  public void addApplicationParameter(ApplicationParameter parameter) {}
  
  public ApplicationParameter[] findApplicationParameters()
  {
    return null;
  }
  
  public void removeApplicationParameter(String name) {}
  
  public void addConstraint(SecurityConstraint constraint) {}
  
  public SecurityConstraint[] findConstraints()
  {
    return null;
  }
  
  public void removeConstraint(SecurityConstraint constraint) {}
  
  public void addErrorPage(ErrorPage errorPage) {}
  
  public ErrorPage findErrorPage(int errorCode)
  {
    return null;
  }
  
  public ErrorPage findErrorPage(String exceptionType)
  {
    return null;
  }
  
  public ErrorPage[] findErrorPages()
  {
    return null;
  }
  
  public void removeErrorPage(ErrorPage errorPage) {}
  
  public void addFilterDef(FilterDef filterDef) {}
  
  public FilterDef findFilterDef(String filterName)
  {
    return null;
  }
  
  public FilterDef[] findFilterDefs()
  {
    return null;
  }
  
  public void removeFilterDef(FilterDef filterDef) {}
  
  public void addFilterMap(FilterMap filterMap) {}
  
  public void addFilterMapBefore(FilterMap filterMap) {}
  
  public FilterMap[] findFilterMaps()
  {
    return null;
  }
  
  public void removeFilterMap(FilterMap filterMap) {}
  
  public void addInstanceListener(String listener) {}
  
  public String[] findInstanceListeners()
  {
    return null;
  }
  
  public void removeInstanceListener(String listener) {}
  
  public void addLocaleEncodingMappingParameter(String locale, String encoding) {}
  
  public void addMimeMapping(String extension, String mimeType) {}
  
  public String findMimeMapping(String extension)
  {
    return null;
  }
  
  public String[] findMimeMappings()
  {
    return null;
  }
  
  public void removeMimeMapping(String extension) {}
  
  public void addParameter(String name, String value) {}
  
  public String findParameter(String name)
  {
    return null;
  }
  
  public String[] findParameters()
  {
    return null;
  }
  
  public void removeParameter(String name) {}
  
  public void addRoleMapping(String role, String link) {}
  
  public String findRoleMapping(String role)
  {
    return null;
  }
  
  public void removeRoleMapping(String role) {}
  
  public void addSecurityRole(String role) {}
  
  public boolean findSecurityRole(String role)
  {
    return false;
  }
  
  public String[] findSecurityRoles()
  {
    return null;
  }
  
  public void removeSecurityRole(String role) {}
  
  public void addServletMapping(String pattern, String name) {}
  
  public void addServletMapping(String pattern, String name, boolean jspWildcard) {}
  
  public String findServletMapping(String pattern)
  {
    return null;
  }
  
  public String[] findServletMappings()
  {
    return null;
  }
  
  public void removeServletMapping(String pattern) {}
  
  public void addWelcomeFile(String name) {}
  
  public boolean findWelcomeFile(String name)
  {
    return false;
  }
  
  public String[] findWelcomeFiles()
  {
    return null;
  }
  
  public void removeWelcomeFile(String name) {}
  
  public void addWrapperLifecycle(String listener) {}
  
  public String[] findWrapperLifecycles()
  {
    return null;
  }
  
  public void removeWrapperLifecycle(String listener) {}
  
  public void addWrapperListener(String listener) {}
  
  public String[] findWrapperListeners()
  {
    return null;
  }
  
  public void removeWrapperListener(String listener) {}
  
  public Wrapper createWrapper()
  {
    return null;
  }
  
  public String findStatusPage(int status)
  {
    return null;
  }
  
  public int[] findStatusPages()
  {
    return null;
  }
  
  public boolean fireRequestInitEvent(ServletRequest request)
  {
    return false;
  }
  
  public boolean fireRequestDestroyEvent(ServletRequest request)
  {
    return false;
  }
  
  public void reload() {}
  
  public String getRealPath(String path)
  {
    return null;
  }
  
  public int getEffectiveMajorVersion()
  {
    return 0;
  }
  
  public void setEffectiveMajorVersion(int major) {}
  
  public int getEffectiveMinorVersion()
  {
    return 0;
  }
  
  public void setEffectiveMinorVersion(int minor) {}
  
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    return null;
  }
  
  public void addResourceJarUrl(URL url) {}
  
  public void addServletContainerInitializer(ServletContainerInitializer sci, Set<Class<?>> classes) {}
  
  public boolean getPaused()
  {
    return false;
  }
  
  public boolean isServlet22()
  {
    return false;
  }
  
  public Set<String> addServletSecurity(ApplicationServletRegistration registration, ServletSecurityElement servletSecurityElement)
  {
    return null;
  }
  
  public void setResourceOnlyServlets(String resourceOnlyServlets) {}
  
  public String getResourceOnlyServlets()
  {
    return null;
  }
  
  public boolean isResourceOnlyServlet(String servletName)
  {
    return false;
  }
  
  public String getBaseName()
  {
    return null;
  }
  
  public void setFireRequestListenersOnForwards(boolean enable) {}
  
  public boolean getFireRequestListenersOnForwards()
  {
    return false;
  }
  
  public void setPreemptiveAuthentication(boolean enable) {}
  
  public boolean getPreemptiveAuthentication()
  {
    return false;
  }
  
  public void setSendRedirectBody(boolean enable) {}
  
  public boolean getSendRedirectBody()
  {
    return false;
  }
  
  public synchronized void addValve(Valve valve) {}
  
  public String getInfo()
  {
    return null;
  }
  
  public Object getMappingObject()
  {
    return null;
  }
  
  public void addPostConstructMethod(String clazz, String method) {}
  
  public void addPreDestroyMethod(String clazz, String method) {}
  
  public void removePostConstructMethod(String clazz) {}
  
  public void removePreDestroyMethod(String clazz) {}
  
  public String findPostConstructMethod(String clazz)
  {
    return null;
  }
  
  public String findPreDestroyMethod(String clazz)
  {
    return null;
  }
  
  public Map<String, String> findPostConstructMethods()
  {
    return null;
  }
  
  public Map<String, String> findPreDestroyMethods()
  {
    return null;
  }
  
  public InstanceManager getInstanceManager()
  {
    return null;
  }
  
  public void setInstanceManager(InstanceManager instanceManager) {}
  
  public void setContainerSciFilter(String containerSciFilter) {}
  
  public String getContainerSciFilter()
  {
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\FailedContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */