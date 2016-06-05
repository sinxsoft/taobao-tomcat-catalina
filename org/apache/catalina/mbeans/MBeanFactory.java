package org.apache.catalina.mbeans;

import java.io.File;
import java.net.URI;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.DataSourceRealm;
import org.apache.catalina.realm.JDBCRealm;
import org.apache.catalina.realm.JNDIRealm;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.HostConfig;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteAddrValve;
import org.apache.catalina.valves.RemoteHostValve;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class MBeanFactory
{
  private static final Log log = LogFactory.getLog(MBeanFactory.class);
  private static MBeanServer mserver = MBeanUtils.createServer();
  private Object container;
  
  public MBeanFactory() {}
  
  public void setContainer(Object container)
  {
    this.container = container;
  }
  
  @Deprecated
  public String findObjectName(String type)
  {
    if (type.equals("org.apache.catalina.core.StandardContext")) {
      return "StandardContext";
    }
    if (type.equals("org.apache.catalina.core.StandardEngine")) {
      return "Engine";
    }
    if (type.equals("org.apache.catalina.core.StandardHost")) {
      return "Host";
    }
    return null;
  }
  
  private final String getPathStr(String t)
  {
    if ((t == null) || (t.equals("/"))) {
      return "";
    }
    return t;
  }
  
  private ContainerBase getParentContainerFromParent(ObjectName pname)
    throws Exception
  {
    String type = pname.getKeyProperty("type");
    String j2eeType = pname.getKeyProperty("j2eeType");
    Service service = getService(pname);
    StandardEngine engine = (StandardEngine)service.getContainer();
    if ((j2eeType != null) && (j2eeType.equals("WebModule")))
    {
      String name = pname.getKeyProperty("name");
      name = name.substring(2);
      int i = name.indexOf("/");
      String hostName = name.substring(0, i);
      String path = name.substring(i);
      Host host = (Host)engine.findChild(hostName);
      String pathStr = getPathStr(path);
      StandardContext context = (StandardContext)host.findChild(pathStr);
      return context;
    }
    if (type != null)
    {
      if (type.equals("Engine")) {
        return engine;
      }
      if (type.equals("Host"))
      {
        String hostName = pname.getKeyProperty("host");
        StandardHost host = (StandardHost)engine.findChild(hostName);
        return host;
      }
    }
    return null;
  }
  
  private ContainerBase getParentContainerFromChild(ObjectName oname)
    throws Exception
  {
    String hostName = oname.getKeyProperty("host");
    String path = oname.getKeyProperty("path");
    Service service = getService(oname);
    StandardEngine engine = (StandardEngine)service.getContainer();
    if (hostName == null) {
      return engine;
    }
    if (path == null)
    {
      StandardHost host = (StandardHost)engine.findChild(hostName);
      return host;
    }
    StandardHost host = (StandardHost)engine.findChild(hostName);
    path = getPathStr(path);
    StandardContext context = (StandardContext)host.findChild(path);
    return context;
  }
  
  private Service getService(ObjectName oname)
    throws Exception
  {
    if ((this.container instanceof Service)) {
      return (Service)this.container;
    }
    StandardService service = null;
    String domain = oname.getDomain();
    if ((this.container instanceof Server))
    {
      Service[] services = ((Server)this.container).findServices();
      for (int i = 0; i < services.length; i++)
      {
        service = (StandardService)services[i];
        if (domain.equals(service.getObjectName().getDomain())) {
          break;
        }
      }
    }
    if ((service == null) || (!service.getObjectName().getDomain().equals(domain))) {
      throw new Exception("Service with the domain is not found");
    }
    return service;
  }
  
  @Deprecated
  public String createAccessLoggerValve(String parent)
    throws Exception
  {
    ObjectName pname = new ObjectName(parent);
    
    AccessLogValve accessLogger = new AccessLogValve();
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.getPipeline().addValve(accessLogger);
    ObjectName oname = accessLogger.getObjectName();
    return oname.toString();
  }
  
  public String createAjpConnector(String parent, String address, int port)
    throws Exception
  {
    return createConnector(parent, address, port, true, false);
  }
  
  public String createDataSourceRealm(String parent, String dataSourceName, String roleNameCol, String userCredCol, String userNameCol, String userRoleTable, String userTable)
    throws Exception
  {
    DataSourceRealm realm = new DataSourceRealm();
    realm.setDataSourceName(dataSourceName);
    realm.setRoleNameCol(roleNameCol);
    realm.setUserCredCol(userCredCol);
    realm.setUserNameCol(userNameCol);
    realm.setUserRoleTable(userRoleTable);
    realm.setUserTable(userTable);
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.setRealm(realm);
    
    ObjectName oname = realm.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  public String createHttpConnector(String parent, String address, int port)
    throws Exception
  {
    return createConnector(parent, address, port, false, false);
  }
  
  private String createConnector(String parent, String address, int port, boolean isAjp, boolean isSSL)
    throws Exception
  {
    Connector retobj = new Connector();
    if ((address != null) && (address.length() > 0)) {
      retobj.setProperty("address", address);
    }
    retobj.setPort(port);
    
    retobj.setProtocol(isAjp ? "AJP/1.3" : "HTTP/1.1");
    
    retobj.setSecure(isSSL);
    retobj.setScheme(isSSL ? "https" : "http");
    
    ObjectName pname = new ObjectName(parent);
    Service service = getService(pname);
    service.addConnector(retobj);
    
    ObjectName coname = retobj.getObjectName();
    
    return coname.toString();
  }
  
  public String createHttpsConnector(String parent, String address, int port)
    throws Exception
  {
    return createConnector(parent, address, port, false, true);
  }
  
  public String createJDBCRealm(String parent, String driverName, String connectionName, String connectionPassword, String connectionURL)
    throws Exception
  {
    JDBCRealm realm = new JDBCRealm();
    realm.setDriverName(driverName);
    realm.setConnectionName(connectionName);
    realm.setConnectionPassword(connectionPassword);
    realm.setConnectionURL(connectionURL);
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.setRealm(realm);
    
    ObjectName oname = realm.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  public String createJNDIRealm(String parent)
    throws Exception
  {
    JNDIRealm realm = new JNDIRealm();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.setRealm(realm);
    
    ObjectName oname = realm.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  public String createMemoryRealm(String parent)
    throws Exception
  {
    MemoryRealm realm = new MemoryRealm();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.setRealm(realm);
    
    ObjectName oname = realm.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  @Deprecated
  public String createRemoteAddrValve(String parent)
    throws Exception
  {
    RemoteAddrValve valve = new RemoteAddrValve();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    containerBase.getPipeline().addValve(valve);
    ObjectName oname = valve.getObjectName();
    return oname.toString();
  }
  
  @Deprecated
  public String createRemoteHostValve(String parent)
    throws Exception
  {
    RemoteHostValve valve = new RemoteHostValve();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    containerBase.getPipeline().addValve(valve);
    ObjectName oname = valve.getObjectName();
    return oname.toString();
  }
  
  @Deprecated
  public String createSingleSignOn(String parent)
    throws Exception
  {
    SingleSignOn valve = new SingleSignOn();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    containerBase.getPipeline().addValve(valve);
    ObjectName oname = valve.getObjectName();
    return oname.toString();
  }
  
  public String createStandardContext(String parent, String path, String docBase)
    throws Exception
  {
    return createStandardContext(parent, path, docBase, false, false, false, false);
  }
  
  public String createStandardContext(String parent, String path, String docBase, boolean xmlValidation, boolean xmlNamespaceAware, boolean tldValidation, boolean tldNamespaceAware)
    throws Exception
  {
    StandardContext context = new StandardContext();
    path = getPathStr(path);
    context.setPath(path);
    context.setDocBase(docBase);
    context.setXmlValidation(xmlValidation);
    context.setXmlNamespaceAware(xmlNamespaceAware);
    context.setTldValidation(tldValidation);
    context.setTldNamespaceAware(tldNamespaceAware);
    
    ContextConfig contextConfig = new ContextConfig();
    context.addLifecycleListener(contextConfig);
    
    ObjectName pname = new ObjectName(parent);
    ObjectName deployer = new ObjectName(pname.getDomain() + ":type=Deployer,host=" + pname.getKeyProperty("host"));
    if (mserver.isRegistered(deployer))
    {
      String contextName = context.getName();
      mserver.invoke(deployer, "addServiced", new Object[] { contextName }, new String[] { "java.lang.String" });
      
      String configPath = (String)mserver.getAttribute(deployer, "configBaseName");
      
      String baseName = context.getBaseName();
      File configFile = new File(new File(configPath), baseName + ".xml");
      if (configFile.isFile()) {
        context.setConfigFile(configFile.toURI().toURL());
      }
      mserver.invoke(deployer, "manageApp", new Object[] { context }, new String[] { "org.apache.catalina.Context" });
      
      mserver.invoke(deployer, "removeServiced", new Object[] { contextName }, new String[] { "java.lang.String" });
    }
    else
    {
      log.warn("Deployer not found for " + pname.getKeyProperty("host"));
      Service service = getService(pname);
      Engine engine = (Engine)service.getContainer();
      Host host = (Host)engine.findChild(pname.getKeyProperty("host"));
      host.addChild(context);
    }
    return context.getObjectName().toString();
  }
  
  public String createStandardHost(String parent, String name, String appBase, boolean autoDeploy, boolean deployOnStartup, boolean deployXML, boolean unpackWARs)
    throws Exception
  {
    StandardHost host = new StandardHost();
    host.setName(name);
    host.setAppBase(appBase);
    host.setAutoDeploy(autoDeploy);
    host.setDeployOnStartup(deployOnStartup);
    host.setDeployXML(deployXML);
    host.setUnpackWARs(unpackWARs);
    
    HostConfig hostConfig = new HostConfig();
    host.addLifecycleListener(hostConfig);
    
    ObjectName pname = new ObjectName(parent);
    Service service = getService(pname);
    Engine engine = (Engine)service.getContainer();
    engine.addChild(host);
    
    return host.getObjectName().toString();
  }
  
  public String createStandardServiceEngine(String domain, String defaultHost, String baseDir)
    throws Exception
  {
    if (!(this.container instanceof Server)) {
      throw new Exception("Container not Server");
    }
    StandardEngine engine = new StandardEngine();
    engine.setDomain(domain);
    engine.setName(domain);
    engine.setDefaultHost(defaultHost);
    engine.setBaseDir(baseDir);
    
    Service service = new StandardService();
    service.setContainer(engine);
    service.setName(domain);
    
    ((Server)this.container).addService(service);
    
    return engine.getObjectName().toString();
  }
  
  public String createStandardManager(String parent)
    throws Exception
  {
    StandardManager manager = new StandardManager();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    if (containerBase != null) {
      containerBase.setManager(manager);
    }
    ObjectName oname = manager.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  public String createUserDatabaseRealm(String parent, String resourceName)
    throws Exception
  {
    UserDatabaseRealm realm = new UserDatabaseRealm();
    realm.setResourceName(resourceName);
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    
    containerBase.setRealm(realm);
    
    ObjectName oname = realm.getObjectName();
    if (oname != null) {
      return oname.toString();
    }
    return null;
  }
  
  public String createValve(String className, String parent)
    throws Exception
  {
    ObjectName parentName = new ObjectName(parent);
    Container container = getParentContainerFromParent(parentName);
    if (container == null) {
      throw new IllegalArgumentException();
    }
    Valve valve = (Valve)Class.forName(className).newInstance();
    
    container.getPipeline().addValve(valve);
    if ((valve instanceof LifecycleMBeanBase)) {
      return ((LifecycleMBeanBase)valve).getObjectName().toString();
    }
    return null;
  }
  
  public String createWebappLoader(String parent)
    throws Exception
  {
    WebappLoader loader = new WebappLoader();
    
    ObjectName pname = new ObjectName(parent);
    ContainerBase containerBase = getParentContainerFromParent(pname);
    if (containerBase != null) {
      containerBase.setLoader(loader);
    }
    ObjectName oname = MBeanUtils.createObjectName(pname.getDomain(), loader);
    
    return oname.toString();
  }
  
  public void removeConnector(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    Service service = getService(oname);
    String port = oname.getKeyProperty("port");
    
    Connector[] conns = service.findConnectors();
    for (int i = 0; i < conns.length; i++)
    {
      String connAddress = String.valueOf(conns[i].getProperty("address"));
      String connPort = "" + conns[i].getPort();
      if ((connAddress == null) && (port.equals(connPort)))
      {
        service.removeConnector(conns[i]);
        conns[i].destroy();
        break;
      }
      if (port.equals(connPort))
      {
        service.removeConnector(conns[i]);
        conns[i].destroy();
        break;
      }
    }
  }
  
  public void removeContext(String contextName)
    throws Exception
  {
    ObjectName oname = new ObjectName(contextName);
    String domain = oname.getDomain();
    StandardService service = (StandardService)getService(oname);
    
    Engine engine = (Engine)service.getContainer();
    String name = oname.getKeyProperty("name");
    name = name.substring(2);
    int i = name.indexOf("/");
    String hostName = name.substring(0, i);
    String path = name.substring(i);
    ObjectName deployer = new ObjectName(domain + ":type=Deployer,host=" + hostName);
    
    String pathStr = getPathStr(path);
    if (mserver.isRegistered(deployer))
    {
      mserver.invoke(deployer, "addServiced", new Object[] { pathStr }, new String[] { "java.lang.String" });
      
      mserver.invoke(deployer, "unmanageApp", new Object[] { pathStr }, new String[] { "java.lang.String" });
      
      mserver.invoke(deployer, "removeServiced", new Object[] { pathStr }, new String[] { "java.lang.String" });
    }
    else
    {
      log.warn("Deployer not found for " + hostName);
      Host host = (Host)engine.findChild(hostName);
      Context context = (Context)host.findChild(pathStr);
      
      host.removeChild(context);
      if ((context instanceof StandardContext)) {
        try
        {
          ((StandardContext)context).destroy();
        }
        catch (Exception e)
        {
          log.warn("Error during context [" + context.getName() + "] destroy ", e);
        }
      }
    }
  }
  
  public void removeHost(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    String hostName = oname.getKeyProperty("host");
    Service service = getService(oname);
    Engine engine = (Engine)service.getContainer();
    Host host = (Host)engine.findChild(hostName);
    if (host != null) {
      engine.removeChild(host);
    }
  }
  
  public void removeLoader(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    
    ContainerBase container = getParentContainerFromChild(oname);
    container.setLoader(null);
  }
  
  public void removeManager(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    
    ContainerBase container = getParentContainerFromChild(oname);
    container.setManager(null);
  }
  
  public void removeRealm(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    
    ContainerBase container = getParentContainerFromChild(oname);
    container.setRealm(null);
  }
  
  public void removeService(String name)
    throws Exception
  {
    if (!(this.container instanceof Server)) {
      throw new Exception();
    }
    ObjectName oname = new ObjectName(name);
    Service service = getService(oname);
    ((Server)this.container).removeService(service);
  }
  
  public void removeValve(String name)
    throws Exception
  {
    ObjectName oname = new ObjectName(name);
    ContainerBase container = getParentContainerFromChild(oname);
    Valve[] valves = container.getPipeline().getValves();
    for (int i = 0; i < valves.length; i++)
    {
      ObjectName voname = ((ValveBase)valves[i]).getObjectName();
      if (voname.equals(oname)) {
        container.getPipeline().removeValve(valves[i]);
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\mbeans\MBeanFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */