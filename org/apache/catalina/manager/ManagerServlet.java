package org.apache.catalina.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.ExpandWar;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.tomcat.util.Diagnostics;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

public class ManagerServlet
  extends HttpServlet
  implements ContainerServlet
{
  private static final long serialVersionUID = 1L;
  protected static final boolean LAST_ACCESS_AT_START;
  
  static
  {
    String lastAccessAtStart = System.getProperty("org.apache.catalina.session.StandardSession.LAST_ACCESS_AT_START");
    if (lastAccessAtStart == null) {
      LAST_ACCESS_AT_START = Globals.STRICT_SERVLET_COMPLIANCE;
    } else {
      LAST_ACCESS_AT_START = Boolean.valueOf(lastAccessAtStart).booleanValue();
    }
  }
  
  protected File configBase = null;
  protected transient org.apache.catalina.Context context = null;
  protected int debug = 1;
  protected File deployed = null;
  protected File versioned = null;
  @Deprecated
  protected File contextDescriptors = null;
  protected transient Host host = null;
  @Deprecated
  protected File appBase = null;
  protected transient MBeanServer mBeanServer = null;
  protected ObjectName oname = null;
  protected transient javax.naming.Context global = null;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.manager");
  protected transient Wrapper wrapper = null;
  
  public Wrapper getWrapper()
  {
    return this.wrapper;
  }
  
  public void setWrapper(Wrapper wrapper)
  {
    this.wrapper = wrapper;
    if (wrapper == null)
    {
      this.context = null;
      this.host = null;
      this.oname = null;
    }
    else
    {
      this.context = ((org.apache.catalina.Context)wrapper.getParent());
      this.host = ((Host)this.context.getParent());
      Engine engine = (Engine)this.host.getParent();
      String name = engine.getName() + ":type=Deployer,host=" + this.host.getName();
      try
      {
        this.oname = new ObjectName(name);
      }
      catch (Exception e)
      {
        log(sm.getString("managerServlet.objectNameFail", new Object[] { name }), e);
      }
    }
    this.mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
  }
  
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    StringManager smClient = StringManager.getManager("org.apache.catalina.manager", request.getLocales());
    
    String command = request.getPathInfo();
    if (command == null) {
      command = request.getServletPath();
    }
    String config = request.getParameter("config");
    String path = request.getParameter("path");
    ContextName cn = null;
    if (path != null) {
      cn = new ContextName(path, request.getParameter("version"));
    }
    String type = request.getParameter("type");
    String war = request.getParameter("war");
    String tag = request.getParameter("tag");
    boolean update = false;
    if ((request.getParameter("update") != null) && (request.getParameter("update").equals("true"))) {
      update = true;
    }
    boolean statusLine = false;
    if ("true".equals(request.getParameter("statusLine"))) {
      statusLine = true;
    }
    response.setContentType("text/plain; charset=utf-8");
    PrintWriter writer = response.getWriter();
    if (command == null) {
      writer.println(smClient.getString("managerServlet.noCommand"));
    } else if (command.equals("/deploy"))
    {
      if ((war != null) || (config != null)) {
        deploy(writer, config, cn, war, update, smClient);
      } else if (tag != null) {
        deploy(writer, cn, tag, smClient);
      } else {
        writer.println(smClient.getString("managerServlet.invalidCommand", new Object[] { command }));
      }
    }
    else if (command.equals("/list")) {
      list(writer, smClient);
    } else if (command.equals("/reload")) {
      reload(writer, cn, smClient);
    } else if (command.equals("/resources")) {
      resources(writer, type, smClient);
    } else if (command.equals("/save")) {
      save(writer, path, smClient);
    } else if (command.equals("/serverinfo")) {
      serverinfo(writer, smClient);
    } else if (command.equals("/sessions")) {
      expireSessions(writer, cn, request, smClient);
    } else if (command.equals("/expire")) {
      expireSessions(writer, cn, request, smClient);
    } else if (command.equals("/start")) {
      start(writer, cn, smClient);
    } else if (command.equals("/stop")) {
      stop(writer, cn, smClient);
    } else if (command.equals("/undeploy")) {
      undeploy(writer, cn, smClient);
    } else if (command.equals("/findleaks")) {
      findleaks(statusLine, writer, smClient);
    } else if (command.equals("/vminfo")) {
      vmInfo(writer, smClient, request.getLocales());
    } else if (command.equals("/threaddump")) {
      threadDump(writer, smClient, request.getLocales());
    } else {
      writer.println(smClient.getString("managerServlet.unknownCommand", new Object[] { command }));
    }
    writer.flush();
    writer.close();
  }
  
  public void doPut(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    StringManager smClient = StringManager.getManager("org.apache.catalina.manager", request.getLocales());
    
    String command = request.getPathInfo();
    if (command == null) {
      command = request.getServletPath();
    }
    String path = request.getParameter("path");
    ContextName cn = null;
    if (path != null) {
      cn = new ContextName(path, request.getParameter("version"));
    }
    String tag = request.getParameter("tag");
    boolean update = false;
    if ((request.getParameter("update") != null) && (request.getParameter("update").equals("true"))) {
      update = true;
    }
    response.setContentType("text/plain;charset=utf-8");
    PrintWriter writer = response.getWriter();
    if (command == null) {
      writer.println(smClient.getString("managerServlet.noCommand"));
    } else if (command.equals("/deploy")) {
      deploy(writer, cn, tag, update, request, smClient);
    } else {
      writer.println(smClient.getString("managerServlet.unknownCommand", new Object[] { command }));
    }
    writer.flush();
    writer.close();
  }
  
  public void init()
    throws ServletException
  {
    if ((this.wrapper == null) || (this.context == null)) {
      throw new UnavailableException(sm.getString("managerServlet.noWrapper"));
    }
    String value = null;
    try
    {
      value = getServletConfig().getInitParameter("debug");
      this.debug = Integer.parseInt(value);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
    }
    Server server = ((Engine)this.host.getParent()).getService().getServer();
    if (server != null) {
      this.global = server.getGlobalNamingContext();
    }
    this.versioned = ((File)getServletContext().getAttribute("javax.servlet.context.tempdir"));
    
    String appBase = ((Host)this.context.getParent()).getAppBase();
    this.deployed = new File(appBase);
    if (!this.deployed.isAbsolute()) {
      this.deployed = new File(System.getProperty("catalina.base"), appBase);
    }
    this.configBase = new File(System.getProperty("catalina.base"), "conf");
    Container container = this.context;
    Container host = null;
    Container engine = null;
    while (container != null)
    {
      if ((container instanceof Host)) {
        host = container;
      }
      if ((container instanceof Engine)) {
        engine = container;
      }
      container = container.getParent();
    }
    if (engine != null) {
      this.configBase = new File(this.configBase, engine.getName());
    }
    if (host != null) {
      this.configBase = new File(this.configBase, host.getName());
    }
    if (this.debug >= 1)
    {
      log("init: Associated with Deployer '" + this.oname + "'");
      if (this.global != null) {
        log("init: Global resources are available");
      }
    }
  }
  
  protected void findleaks(boolean statusLine, PrintWriter writer, StringManager smClient)
  {
    if (!(this.host instanceof StandardHost))
    {
      writer.println(smClient.getString("managerServlet.findleaksFail"));
      return;
    }
    String[] results = ((StandardHost)this.host).findReloadedContextMemoryLeaks();
    if (results.length > 0)
    {
      if (statusLine) {
        writer.println(smClient.getString("managerServlet.findleaksList"));
      }
      for (String result : results)
      {
        if ("".equals(result)) {
          result = "/";
        }
        writer.println(result);
      }
    }
    else if (statusLine)
    {
      writer.println(smClient.getString("managerServlet.findleaksNone"));
    }
  }
  
  protected void vmInfo(PrintWriter writer, StringManager smClient, Enumeration<Locale> requestedLocales)
  {
    writer.println(smClient.getString("managerServlet.vminfo"));
    writer.print(Diagnostics.getVMInfo(requestedLocales));
  }
  
  protected void threadDump(PrintWriter writer, StringManager smClient, Enumeration<Locale> requestedLocales)
  {
    writer.println(smClient.getString("managerServlet.threaddump"));
    writer.print(Diagnostics.getThreadDump(requestedLocales));
  }
  
  protected synchronized void save(PrintWriter writer, String path, StringManager smClient)
  {
    Server server = ((Engine)this.host.getParent()).getService().getServer();
    if (!(server instanceof StandardServer))
    {
      writer.println(smClient.getString("managerServlet.saveFail", new Object[] { server }));
      
      return;
    }
    if ((path == null) || (path.length() == 0) || (!path.startsWith("/")))
    {
      try
      {
        ((StandardServer)server).storeConfig();
        writer.println(smClient.getString("managerServlet.saved"));
      }
      catch (Exception e)
      {
        log("managerServlet.storeConfig", e);
        writer.println(smClient.getString("managerServlet.exception", new Object[] { e.toString() }));
        
        return;
      }
    }
    else
    {
      String contextPath = path;
      if (path.equals("/")) {
        contextPath = "";
      }
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(contextPath);
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { path }));
        
        return;
      }
      try
      {
        ((StandardServer)server).storeContext(context);
        writer.println(smClient.getString("managerServlet.savedContext", new Object[] { path }));
      }
      catch (Exception e)
      {
        log("managerServlet.save[" + path + "]", e);
        writer.println(smClient.getString("managerServlet.exception", new Object[] { e.toString() }));
        
        return;
      }
    }
  }
  
  protected synchronized void deploy(PrintWriter writer, ContextName cn, String tag, boolean update, HttpServletRequest request, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("deploy: Deploying web application '" + cn + "'");
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String name = cn.getName();
    String baseName = cn.getBaseName();
    String displayPath = cn.getDisplayName();
    
    org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(name);
    if ((context != null) && (!update))
    {
      writer.println(smClient.getString("managerServlet.alreadyContext", new Object[] { displayPath }));
      
      return;
    }
    File deployedWar = new File(this.deployed, baseName + ".war");
    File uploadedWar;
    
    if (tag == null)
    {
      if (update)
      {
        uploadedWar = new File(deployedWar.getAbsolutePath() + ".tmp");
        if ((uploadedWar.exists()) && (!uploadedWar.delete())) {
          writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { uploadedWar }));
        }
      }
      else
      {
        uploadedWar = deployedWar;
      }
    }
    else
    {
      File uploadPath = new File(this.versioned, tag);
      if ((!uploadPath.mkdirs()) && (!uploadPath.isDirectory()))
      {
        writer.println(smClient.getString("managerServlet.mkdirFail", new Object[] { uploadPath }));
        
        return;
      }
      uploadedWar = new File(uploadPath, baseName + ".war");
    }
    if (this.debug >= 2) {
      log("Uploading WAR file to " + uploadedWar);
    }
    try
    {
      if (isServiced(name))
      {
        writer.println(smClient.getString("managerServlet.inService", new Object[] { displayPath }));
      }
      else
      {
        addServiced(name);
        try
        {
          uploadWar(writer, request, uploadedWar, smClient);
          if ((update) && (tag == null))
          {
            if ((deployedWar.exists()) && (!deployedWar.delete()))
            {
              writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { deployedWar })); return;
            }
            uploadedWar.renameTo(deployedWar);
          }
          if (tag != null) {
            copy(uploadedWar, deployedWar);
          }
          check(name);
        }
        finally
        {
          removeServiced(name);
        }
      }
    }
    catch (Exception e)
    {
      log("managerServlet.check[" + displayPath + "]", e);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { e.toString() }));
      
      return;
    }
    writeDeployResult(writer, smClient, name, displayPath);
  }
  
  protected void deploy(PrintWriter writer, ContextName cn, String tag, StringManager smClient)
  {
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String baseName = cn.getBaseName();
    String name = cn.getName();
    String displayPath = cn.getDisplayName();
    
    File localWar = new File(new File(this.versioned, tag), baseName + ".war");
    
    File deployedWar = new File(this.deployed, baseName + ".war");
    try
    {
      if (isServiced(name))
      {
        writer.println(smClient.getString("managerServlet.inService", new Object[] { displayPath }));
      }
      else
      {
        addServiced(name);
        try
        {
          if (!deployedWar.delete())
          {
            writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { deployedWar })); return;
          }
          copy(localWar, deployedWar);
          
          check(name);
        }
        finally
        {
          removeServiced(name);
        }
      }
    }
    catch (Exception e)
    {
      log("managerServlet.check[" + displayPath + "]", e);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { e.toString() }));
      
      return;
    }
    writeDeployResult(writer, smClient, name, displayPath);
  }
  
  protected void deploy(PrintWriter writer, String config, ContextName cn, String war, boolean update, StringManager smClient)
  {
    if ((config != null) && (config.length() == 0)) {
      config = null;
    }
    if ((war != null) && (war.length() == 0)) {
      war = null;
    }
    if (this.debug >= 1) {
      if ((config != null) && (config.length() > 0))
      {
        if (war != null) {
          log("install: Installing context configuration at '" + config + "' from '" + war + "'");
        } else {
          log("install: Installing context configuration at '" + config + "'");
        }
      }
      else if (cn != null) {
        log("install: Installing web application '" + cn + "' from '" + war + "'");
      } else {
        log("install: Installing web application from '" + war + "'");
      }
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String name = cn.getName();
    String baseName = cn.getBaseName();
    String displayPath = cn.getDisplayName();
    
    org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(name);
    if ((context != null) && (!update))
    {
      writer.println(smClient.getString("managerServlet.alreadyContext", new Object[] { displayPath }));
      
      return;
    }
    if ((config != null) && (config.startsWith("file:"))) {
      config = config.substring("file:".length());
    }
    if ((war != null) && (war.startsWith("file:"))) {
      war = war.substring("file:".length());
    }
    try
    {
      if (isServiced(name))
      {
        writer.println(smClient.getString("managerServlet.inService", new Object[] { displayPath }));
      }
      else
      {
        addServiced(name);
        try
        {
          if (config != null)
          {
            if ((!this.configBase.mkdirs()) && (!this.configBase.isDirectory()))
            {
              writer.println(smClient.getString("managerServlet.mkdirFail", new Object[] { this.configBase })); return;
            }
            File localConfig = new File(this.configBase, baseName + ".xml");
            if ((localConfig.isFile()) && (!localConfig.delete()))
            {
              writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { localConfig })); return;
            }
            copy(new File(config), localConfig);
          }
          if (war != null)
          {
            File localWar;

            if (war.endsWith(".war")) {
              localWar = new File(this.deployed, baseName + ".war");
            } else {
              localWar = new File(this.deployed, baseName);
            }
            if ((localWar.exists()) && (!ExpandWar.delete(localWar)))
            {
              writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { localWar })); return;
            }
            copy(new File(war), localWar);
          }
          check(name);
        }
        finally
        {
          removeServiced(name);
        }
      }
      writeDeployResult(writer, smClient, name, displayPath);
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.install[" + displayPath + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  private void writeDeployResult(PrintWriter writer, StringManager smClient, String name, String displayPath)
  {
    org.apache.catalina.Context deployed = (org.apache.catalina.Context)this.host.findChild(name);
    if ((deployed != null) && (deployed.getConfigured()) && (deployed.getState().isAvailable())) {
      writer.println(smClient.getString("managerServlet.deployed", new Object[] { displayPath }));
    } else if ((deployed != null) && (!deployed.getState().isAvailable())) {
      writer.println(smClient.getString("managerServlet.deployedButNotStarted", new Object[] { displayPath }));
    } else {
      writer.println(smClient.getString("managerServlet.deployFailed", new Object[] { displayPath }));
    }
  }
  
  protected void list(PrintWriter writer, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("list: Listing contexts for virtual host '" + this.host.getName() + "'");
    }
    writer.println(smClient.getString("managerServlet.listed", new Object[] { this.host.getName() }));
    
    Container[] contexts = this.host.findChildren();
    for (int i = 0; i < contexts.length; i++)
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)contexts[i];
      if (context != null)
      {
        String displayPath = context.getPath();
        if (displayPath.equals("")) {
          displayPath = "/";
        }
        if (context.getState().isAvailable()) {
          writer.println(smClient.getString("managerServlet.listitem", new Object[] { displayPath, "running", "" + context.getManager().findSessions().length, context.getDocBase() }));
        } else {
          writer.println(smClient.getString("managerServlet.listitem", new Object[] { displayPath, "stopped", "0", context.getDocBase() }));
        }
      }
    }
  }
  
  protected void reload(PrintWriter writer, ContextName cn, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("restart: Reloading web application '" + cn + "'");
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    try
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(cn.getName());
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { RequestUtil.filter(cn.getDisplayName()) }));
        
        return;
      }
      if (context.getName().equals(this.context.getName()))
      {
        writer.println(smClient.getString("managerServlet.noSelf"));
        return;
      }
      context.reload();
      writer.println(smClient.getString("managerServlet.reloaded", new Object[] { cn.getDisplayName() }));
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.reload[" + cn.getDisplayName() + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  protected void resources(PrintWriter writer, String type, StringManager smClient)
  {
    if (this.debug >= 1) {
      if (type != null) {
        log("resources:  Listing resources of type " + type);
      } else {
        log("resources:  Listing resources of all types");
      }
    }
    if (this.global == null)
    {
      writer.println(smClient.getString("managerServlet.noGlobal"));
      return;
    }
    if (type != null) {
      writer.println(smClient.getString("managerServlet.resourcesType", new Object[] { type }));
    } else {
      writer.println(smClient.getString("managerServlet.resourcesAll"));
    }
    Class<?> clazz = null;
    try
    {
      if (type != null) {
        clazz = Class.forName(type);
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.resources[" + type + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
      
      return;
    }
    printResources(writer, "", this.global, type, clazz, smClient);
  }
  
  protected void printResources(PrintWriter writer, String prefix, javax.naming.Context namingContext, String type, Class<?> clazz, StringManager smClient)
  {
    try
    {
      NamingEnumeration<Binding> items = namingContext.listBindings("");
      while (items.hasMore())
      {
        Binding item = (Binding)items.next();
        if ((item.getObject() instanceof javax.naming.Context))
        {
          printResources(writer, prefix + item.getName() + "/", (javax.naming.Context)item.getObject(), type, clazz, smClient);
        }
        else if ((clazz == null) || (clazz.isInstance(item.getObject())))
        {
          writer.print(prefix + item.getName());
          writer.print(':');
          writer.print(item.getClassName());
          
          writer.println();
        }
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.resources[" + type + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  protected void serverinfo(PrintWriter writer, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("serverinfo");
    }
    try
    {
      StringBuilder props = new StringBuilder();
      props.append("OK - Server info");
      props.append("\nTomcat Version: ");
      props.append(ServerInfo.getServerInfo());
      props.append("\nOS Name: ");
      props.append(System.getProperty("os.name"));
      props.append("\nOS Version: ");
      props.append(System.getProperty("os.version"));
      props.append("\nOS Architecture: ");
      props.append(System.getProperty("os.arch"));
      props.append("\nJVM Version: ");
      props.append(System.getProperty("java.runtime.version"));
      props.append("\nJVM Vendor: ");
      props.append(System.getProperty("java.vm.vendor"));
      writer.println(props.toString());
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      getServletContext().log("ManagerServlet.serverinfo", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  protected void sessions(PrintWriter writer, ContextName cn, int idle, StringManager smClient)
  {
    if (this.debug >= 1)
    {
      log("sessions: Session information for web application '" + cn + "'");
      if (idle >= 0) {
        log("sessions: Session expiration for " + idle + " minutes '" + cn + "'");
      }
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String displayPath = cn.getDisplayName();
    try
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(cn.getName());
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      Manager manager = context.getManager();
      if (manager == null)
      {
        writer.println(smClient.getString("managerServlet.noManager", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      int maxCount = 60;
      int histoInterval = 1;
      int maxInactiveInterval = manager.getMaxInactiveInterval() / 60;
      if (maxInactiveInterval > 0)
      {
        histoInterval = maxInactiveInterval / maxCount;
        if (histoInterval * maxCount < maxInactiveInterval) {
          histoInterval++;
        }
        if (0 == histoInterval) {
          histoInterval = 1;
        }
        maxCount = maxInactiveInterval / histoInterval;
        if (histoInterval * maxCount < maxInactiveInterval) {
          maxCount++;
        }
      }
      writer.println(smClient.getString("managerServlet.sessions", new Object[] { displayPath }));
      
      writer.println(smClient.getString("managerServlet.sessiondefaultmax", new Object[] { "" + maxInactiveInterval }));
      
      Session[] sessions = manager.findSessions();
      int[] timeout = new int[maxCount + 1];
      int notimeout = 0;
      int expired = 0;
      long now = System.currentTimeMillis();
      for (int i = 0; i < sessions.length; i++)
      {
        int time;
        if (LAST_ACCESS_AT_START) {
          time = (int)((now - sessions[i].getLastAccessedTimeInternal()) / 1000L);
        } else {
          time = (int)((now - sessions[i].getThisAccessedTimeInternal()) / 1000L);
        }
        if ((idle >= 0) && (time >= idle * 60))
        {
          sessions[i].expire();
          expired++;
        }
        time = time / 60 / histoInterval;
        if (time < 0) {
          notimeout++;
        } else if (time >= maxCount) {
          timeout[maxCount] += 1;
        } else {
          timeout[time] += 1;
        }
      }
      if (timeout[0] > 0) {
        writer.println(smClient.getString("managerServlet.sessiontimeout", new Object[] { "<" + histoInterval, "" + timeout[0] }));
      }
      for (int i = 1; i < maxCount; i++) {
        if (timeout[i] > 0) {
          writer.println(smClient.getString("managerServlet.sessiontimeout", new Object[] { "" + i * histoInterval + " - <" + (i + 1) * histoInterval, "" + timeout[i] }));
        }
      }
      if (timeout[maxCount] > 0) {
        writer.println(smClient.getString("managerServlet.sessiontimeout", new Object[] { ">=" + maxCount * histoInterval, "" + timeout[maxCount] }));
      }
      if (notimeout > 0) {
        writer.println(smClient.getString("managerServlet.sessiontimeout.unlimited", new Object[] { "" + notimeout }));
      }
      if (idle >= 0) {
        writer.println(smClient.getString("managerServlet.sessiontimeout.expired", new Object[] { ">" + idle, "" + expired }));
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.sessions[" + displayPath + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  @Deprecated
  protected void sessions(PrintWriter writer, ContextName cn, StringManager smClient)
  {
    sessions(writer, cn, -1, smClient);
  }
  
  protected void expireSessions(PrintWriter writer, ContextName cn, HttpServletRequest req, StringManager smClient)
  {
    int idle = -1;
    String idleParam = req.getParameter("idle");
    if (idleParam != null) {
      try
      {
        idle = Integer.parseInt(idleParam);
      }
      catch (NumberFormatException e)
      {
        log("Could not parse idle parameter to an int: " + idleParam);
      }
    }
    sessions(writer, cn, idle, smClient);
  }
  
  protected void start(PrintWriter writer, ContextName cn, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("start: Starting web application '" + cn + "'");
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String displayPath = cn.getDisplayName();
    try
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(cn.getName());
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      context.start();
      if (context.getState().isAvailable()) {
        writer.println(smClient.getString("managerServlet.started", new Object[] { displayPath }));
      } else {
        writer.println(smClient.getString("managerServlet.startFailed", new Object[] { displayPath }));
      }
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      getServletContext().log(sm.getString("managerServlet.startFailed", new Object[] { displayPath }), t);
      
      writer.println(smClient.getString("managerServlet.startFailed", new Object[] { displayPath }));
      
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  protected void stop(PrintWriter writer, ContextName cn, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("stop: Stopping web application '" + cn + "'");
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String displayPath = cn.getDisplayName();
    try
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(cn.getName());
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      if (context.getName().equals(this.context.getName()))
      {
        writer.println(smClient.getString("managerServlet.noSelf"));
        return;
      }
      context.stop();
      writer.println(smClient.getString("managerServlet.stopped", new Object[] { displayPath }));
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.stop[" + displayPath + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  protected void undeploy(PrintWriter writer, ContextName cn, StringManager smClient)
  {
    if (this.debug >= 1) {
      log("undeploy: Undeploying web application at '" + cn + "'");
    }
    if (!validateContextName(cn, writer, smClient)) {
      return;
    }
    String name = cn.getName();
    String baseName = cn.getBaseName();
    String displayPath = cn.getDisplayName();
    try
    {
      org.apache.catalina.Context context = (org.apache.catalina.Context)this.host.findChild(name);
      if (context == null)
      {
        writer.println(smClient.getString("managerServlet.noContext", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      if (!isDeployed(name))
      {
        writer.println(smClient.getString("managerServlet.notDeployed", new Object[] { RequestUtil.filter(displayPath) }));
        
        return;
      }
      if (isServiced(name))
      {
        writer.println(smClient.getString("managerServlet.inService", new Object[] { displayPath }));
      }
      else
      {
        addServiced(name);
        try
        {
          context.stop();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        try
        {
          File war = new File(this.deployed, baseName + ".war");
          File dir = new File(this.deployed, baseName);
          File xml = new File(this.configBase, baseName + ".xml");
          if ((war.exists()) && (!war.delete()))
          {
            writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { war })); return;
          }
          if ((dir.exists()) && (!undeployDir(dir)))
          {
            writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { dir })); return;
          }
          if ((xml.exists()) && (!xml.delete()))
          {
            writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { xml })); return;
          }
          check(name);
        }
        finally
        {
          removeServiced(name);
        }
      }
      writer.println(smClient.getString("managerServlet.undeployed", new Object[] { displayPath }));
    }
    catch (Throwable t)
    {
      ExceptionUtils.handleThrowable(t);
      log("ManagerServlet.undeploy[" + displayPath + "]", t);
      writer.println(smClient.getString("managerServlet.exception", new Object[] { t.toString() }));
    }
  }
  
  @Deprecated
  protected File getAppBase()
  {
    if (this.appBase != null) {
      return this.appBase;
    }
    File file = new File(this.host.getAppBase());
    if (!file.isAbsolute()) {
      file = new File(System.getProperty("catalina.base"), this.host.getAppBase());
    }
    try
    {
      this.appBase = file.getCanonicalFile();
    }
    catch (IOException e)
    {
      this.appBase = file;
    }
    return this.appBase;
  }
  
  protected boolean isDeployed(String name)
    throws Exception
  {
    String[] params = { name };
    String[] signature = { "java.lang.String" };
    Boolean result = (Boolean)this.mBeanServer.invoke(this.oname, "isDeployed", params, signature);
    
    return result.booleanValue();
  }
  
  protected void check(String name)
    throws Exception
  {
    String[] params = { name };
    String[] signature = { "java.lang.String" };
    this.mBeanServer.invoke(this.oname, "check", params, signature);
  }
  
  protected boolean isServiced(String name)
    throws Exception
  {
    String[] params = { name };
    String[] signature = { "java.lang.String" };
    Boolean result = (Boolean)this.mBeanServer.invoke(this.oname, "isServiced", params, signature);
    
    return result.booleanValue();
  }
  
  protected void addServiced(String name)
    throws Exception
  {
    String[] params = { name };
    String[] signature = { "java.lang.String" };
    this.mBeanServer.invoke(this.oname, "addServiced", params, signature);
  }
  
  protected void removeServiced(String name)
    throws Exception
  {
    String[] params = { name };
    String[] signature = { "java.lang.String" };
    this.mBeanServer.invoke(this.oname, "removeServiced", params, signature);
  }
  
  protected boolean undeployDir(File dir)
  {
    String[] files = dir.list();
    if (files == null) {
      files = new String[0];
    }
    for (int i = 0; i < files.length; i++)
    {
      File file = new File(dir, files[i]);
      if (file.isDirectory())
      {
        if (!undeployDir(file)) {
          return false;
        }
      }
      else if (!file.delete()) {
        return false;
      }
    }
    return dir.delete();
  }
  
  protected void uploadWar(PrintWriter writer, HttpServletRequest request, File war, StringManager smClient)
    throws IOException
  {
    if ((war.exists()) && (!war.delete()))
    {
      String msg = smClient.getString("managerServlet.deleteFail", new Object[] { war });
      throw new IOException(msg);
    }
    ServletInputStream istream = null;
    BufferedOutputStream ostream = null;
    try
    {
      istream = request.getInputStream();
      ostream = new BufferedOutputStream(new FileOutputStream(war), 1024);
      
      byte[] buffer = new byte['Ѐ'];
      for (;;)
      {
        int n = istream.read(buffer);
        if (n < 0) {
          break;
        }
        ostream.write(buffer, 0, n);
      }
      ostream.flush();
      ostream.close();
      ostream = null;
      istream.close();
      istream = null;
    }
    catch (IOException e)
    {
      if ((war.exists()) && (!war.delete())) {
        writer.println(smClient.getString("managerServlet.deleteFail", new Object[] { war }));
      }
      throw e;
    }
    finally
    {
      if (ostream != null)
      {
        try
        {
          ostream.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        ostream = null;
      }
      if (istream != null)
      {
        try
        {
          istream.close();
        }
        catch (Throwable t)
        {
          ExceptionUtils.handleThrowable(t);
        }
        istream = null;
      }
    }
  }
  
  @Deprecated
  protected StringManager getStringManager(HttpServletRequest req)
  {
    Enumeration<Locale> requestedLocales = req.getLocales();
    while (requestedLocales.hasMoreElements())
    {
      Locale locale = (Locale)requestedLocales.nextElement();
      StringManager result = StringManager.getManager("org.apache.catalina.manager", locale);
      if (result.getLocale().equals(locale)) {
        return result;
      }
    }
    return sm;
  }
  
  protected static boolean validateContextName(ContextName cn, PrintWriter writer, StringManager sm)
  {
    if ((cn != null) && ((cn.getPath().startsWith("/")) || (cn.getPath().equals("")))) {
      return true;
    }
    String path = null;
    if (cn != null) {
      path = RequestUtil.filter(cn.getPath());
    }
    writer.println(sm.getString("managerServlet.invalidPath", new Object[] { path }));
    return false;
  }
  
  public static boolean copy(File src, File dest)
  {
    boolean result = false;
    try
    {
      if ((src != null) && (!src.getCanonicalPath().equals(dest.getCanonicalPath()))) {
        result = copyInternal(src, dest, new byte['က']);
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return result;
  }
  
  public static boolean copyInternal(File src, File dest, byte[] buf)
  {
    boolean result = true;
    
    String[] files = null;
    if (src.isDirectory())
    {
      files = src.list();
      result = dest.mkdir();
    }
    else
    {
      files = new String[1];
      files[0] = "";
    }
    if (files == null) {
      files = new String[0];
    }
    for (int i = 0; (i < files.length) && (result);)
    {
      File fileSrc = new File(src, files[i]);
      File fileDest = new File(dest, files[i]);
      if (fileSrc.isDirectory())
      {
        result = copyInternal(fileSrc, fileDest, buf);
      }
      else
      {
        FileInputStream is = null;
        FileOutputStream os = null;
        try
        {
          is = new FileInputStream(fileSrc);
          os = new FileOutputStream(fileDest);
          int len = 0;
          for (;;)
          {
            len = is.read(buf);
            if (len == -1) {
              break;
            }
            os.write(buf, 0, len);
          }
          if (is != null) {
            try
            {
              is.close();
            }
            catch (IOException e) {}
          }
          if (os != null) {
            try
            {
              os.close();
            }
            catch (IOException e) {}
          }
          i++;
        }
        catch (IOException e)
        {
          e.printStackTrace();
          result = false;
        }
        finally
        {
          if (is != null) {
            try
            {
              is.close();
            }
            catch (IOException e) {}
          }
          if (os != null) {
            try
            {
              os.close();
            }
            catch (IOException e) {}
          }
        }
      }
    }
    return result;
  }
  
  public ManagerServlet() {}
  
  public void destroy() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\manager\ManagerServlet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */