package org.apache.catalina.connector;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.management.ObjectName;
import org.apache.catalina.Executor;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.coyote.Adapter;
import org.apache.coyote.ProtocolHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.res.StringManager;

public class Connector
  extends LifecycleMBeanBase
{
  private static final Log log = LogFactory.getLog(Connector.class);
  public static final boolean RECYCLE_FACADES = Boolean.valueOf(System.getProperty("org.apache.catalina.connector.RECYCLE_FACADES", "false")).booleanValue();
  
  public Connector()
  {
    this(null);
  }
  
  public Connector(String protocol)
  {
    setProtocol(protocol);
    try
    {
      Class<?> clazz = Class.forName(this.protocolHandlerClassName);
      this.protocolHandler = ((ProtocolHandler)clazz.newInstance());
    }
    catch (Exception e)
    {
      log.error(sm.getString("coyoteConnector.protocolHandlerInstantiationFailed"), e);
    }
  }
  
  protected Service service = null;
  protected boolean allowTrace = false;
  protected long asyncTimeout = 10000L;
  protected boolean enableLookups = false;
  protected boolean xpoweredBy = false;
  protected static final String info = "org.apache.catalina.connector.Connector/2.1";
  protected int port = -1;
  protected String proxyName = null;
  protected int proxyPort = 0;
  protected int redirectPort = 443;
  protected String scheme = "http";
  protected boolean secure = false;
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.connector");
  protected int maxParameterCount = 10000;
  protected int maxPostSize = 2097152;
  protected int maxSavePostSize = 4096;
  protected String parseBodyMethods = "POST";
  protected HashSet<String> parseBodyMethodsSet;
  protected boolean useIPVHosts = false;
  protected String protocolHandlerClassName = "org.apache.coyote.http11.Http11Protocol";
  protected ProtocolHandler protocolHandler = null;
  protected Adapter adapter = null;
  protected Mapper mapper = new Mapper();
  protected MapperListener mapperListener = new MapperListener(this.mapper, this);
  protected String URIEncoding = null;
  protected boolean useBodyEncodingForURI = false;
  protected static HashMap<String, String> replacements = new HashMap();
  
  static
  {
    replacements.put("acceptCount", "backlog");
    replacements.put("connectionLinger", "soLinger");
    replacements.put("connectionTimeout", "soTimeout");
    replacements.put("rootFile", "rootfile");
  }
  
  public Object getProperty(String name)
  {
    String repl = name;
    if (replacements.get(name) != null) {
      repl = (String)replacements.get(name);
    }
    return IntrospectionUtils.getProperty(this.protocolHandler, repl);
  }
  
  public boolean setProperty(String name, String value)
  {
    String repl = name;
    if (replacements.get(name) != null) {
      repl = (String)replacements.get(name);
    }
    return IntrospectionUtils.setProperty(this.protocolHandler, repl, value);
  }
  
  public Object getAttribute(String name)
  {
    return getProperty(name);
  }
  
  public void setAttribute(String name, Object value)
  {
    setProperty(name, String.valueOf(value));
  }
  
  public Service getService()
  {
    return this.service;
  }
  
  public void setService(Service service)
  {
    this.service = service;
  }
  
  public boolean getAllowTrace()
  {
    return this.allowTrace;
  }
  
  public void setAllowTrace(boolean allowTrace)
  {
    this.allowTrace = allowTrace;
    setProperty("allowTrace", String.valueOf(allowTrace));
  }
  
  public long getAsyncTimeout()
  {
    return this.asyncTimeout;
  }
  
  public void setAsyncTimeout(long asyncTimeout)
  {
    this.asyncTimeout = asyncTimeout;
    setProperty("asyncTimeout", String.valueOf(asyncTimeout));
  }
  
  public boolean getEnableLookups()
  {
    return this.enableLookups;
  }
  
  public void setEnableLookups(boolean enableLookups)
  {
    this.enableLookups = enableLookups;
    setProperty("enableLookups", String.valueOf(enableLookups));
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.connector.Connector/2.1";
  }
  
  public Mapper getMapper()
  {
    return this.mapper;
  }
  
  public int getMaxHeaderCount()
  {
    return ((Integer)getProperty("maxHeaderCount")).intValue();
  }
  
  public void setMaxHeaderCount(int maxHeaderCount)
  {
    setProperty("maxHeaderCount", String.valueOf(maxHeaderCount));
  }
  
  public int getMaxParameterCount()
  {
    return this.maxParameterCount;
  }
  
  public void setMaxParameterCount(int maxParameterCount)
  {
    this.maxParameterCount = maxParameterCount;
  }
  
  public int getMaxPostSize()
  {
    return this.maxPostSize;
  }
  
  public void setMaxPostSize(int maxPostSize)
  {
    this.maxPostSize = maxPostSize;
  }
  
  public int getMaxSavePostSize()
  {
    return this.maxSavePostSize;
  }
  
  public void setMaxSavePostSize(int maxSavePostSize)
  {
    this.maxSavePostSize = maxSavePostSize;
    setProperty("maxSavePostSize", String.valueOf(maxSavePostSize));
  }
  
  public String getParseBodyMethods()
  {
    return this.parseBodyMethods;
  }
  
  public void setParseBodyMethods(String methods)
  {
    HashSet<String> methodSet = new HashSet();
    if (null != methods) {
      methodSet.addAll(Arrays.asList(methods.split("\\s*,\\s*")));
    }
    if (methodSet.contains("TRACE")) {
      throw new IllegalArgumentException(sm.getString("coyoteConnector.parseBodyMethodNoTrace"));
    }
    this.parseBodyMethods = methods;
    this.parseBodyMethodsSet = methodSet;
  }
  
  protected boolean isParseBodyMethod(String method)
  {
    return this.parseBodyMethodsSet.contains(method);
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public void setPort(int port)
  {
    this.port = port;
    setProperty("port", String.valueOf(port));
  }
  
  public int getLocalPort()
  {
    return ((Integer)getProperty("localPort")).intValue();
  }
  
  public String getProtocol()
  {
    if (("org.apache.coyote.http11.Http11Protocol".equals(getProtocolHandlerClassName())) || ("org.apache.coyote.http11.Http11AprProtocol".equals(getProtocolHandlerClassName()))) {
      return "HTTP/1.1";
    }
    if (("org.apache.coyote.ajp.AjpProtocol".equals(getProtocolHandlerClassName())) || ("org.apache.coyote.ajp.AjpAprProtocol".equals(getProtocolHandlerClassName()))) {
      return "AJP/1.3";
    }
    return getProtocolHandlerClassName();
  }
  
  public void setProtocol(String protocol)
  {
    if (AprLifecycleListener.isAprAvailable())
    {
      if ("HTTP/1.1".equals(protocol)) {
        setProtocolHandlerClassName("org.apache.coyote.http11.Http11AprProtocol");
      } else if ("AJP/1.3".equals(protocol)) {
        setProtocolHandlerClassName("org.apache.coyote.ajp.AjpAprProtocol");
      } else if (protocol != null) {
        setProtocolHandlerClassName(protocol);
      } else {
        setProtocolHandlerClassName("org.apache.coyote.http11.Http11AprProtocol");
      }
    }
    else if ("HTTP/1.1".equals(protocol)) {
      setProtocolHandlerClassName("org.apache.coyote.http11.Http11Protocol");
    } else if ("AJP/1.3".equals(protocol)) {
      setProtocolHandlerClassName("org.apache.coyote.ajp.AjpProtocol");
    } else if (protocol != null) {
      setProtocolHandlerClassName(protocol);
    }
  }
  
  public String getProtocolHandlerClassName()
  {
    return this.protocolHandlerClassName;
  }
  
  public void setProtocolHandlerClassName(String protocolHandlerClassName)
  {
    this.protocolHandlerClassName = protocolHandlerClassName;
  }
  
  public ProtocolHandler getProtocolHandler()
  {
    return this.protocolHandler;
  }
  
  public String getProxyName()
  {
    return this.proxyName;
  }
  
  public void setProxyName(String proxyName)
  {
    if ((proxyName != null) && (proxyName.length() > 0))
    {
      this.proxyName = proxyName;
      setProperty("proxyName", proxyName);
    }
    else
    {
      this.proxyName = null;
    }
  }
  
  public int getProxyPort()
  {
    return this.proxyPort;
  }
  
  public void setProxyPort(int proxyPort)
  {
    this.proxyPort = proxyPort;
    setProperty("proxyPort", String.valueOf(proxyPort));
  }
  
  public int getRedirectPort()
  {
    return this.redirectPort;
  }
  
  public void setRedirectPort(int redirectPort)
  {
    this.redirectPort = redirectPort;
    setProperty("redirectPort", String.valueOf(redirectPort));
  }
  
  public String getScheme()
  {
    return this.scheme;
  }
  
  public void setScheme(String scheme)
  {
    this.scheme = scheme;
  }
  
  public boolean getSecure()
  {
    return this.secure;
  }
  
  public void setSecure(boolean secure)
  {
    this.secure = secure;
    setProperty("secure", Boolean.toString(secure));
  }
  
  public String getURIEncoding()
  {
    return this.URIEncoding;
  }
  
  public void setURIEncoding(String URIEncoding)
  {
    this.URIEncoding = URIEncoding;
    setProperty("uRIEncoding", URIEncoding);
  }
  
  public boolean getUseBodyEncodingForURI()
  {
    return this.useBodyEncodingForURI;
  }
  
  public void setUseBodyEncodingForURI(boolean useBodyEncodingForURI)
  {
    this.useBodyEncodingForURI = useBodyEncodingForURI;
    setProperty("useBodyEncodingForURI", String.valueOf(useBodyEncodingForURI));
  }
  
  public boolean getXpoweredBy()
  {
    return this.xpoweredBy;
  }
  
  public void setXpoweredBy(boolean xpoweredBy)
  {
    this.xpoweredBy = xpoweredBy;
    setProperty("xpoweredBy", String.valueOf(xpoweredBy));
  }
  
  public void setUseIPVHosts(boolean useIPVHosts)
  {
    this.useIPVHosts = useIPVHosts;
    setProperty("useIPVHosts", String.valueOf(useIPVHosts));
  }
  
  public boolean getUseIPVHosts()
  {
    return this.useIPVHosts;
  }
  
  public String getExecutorName()
  {
    Object obj = this.protocolHandler.getExecutor();
    if ((obj instanceof Executor)) {
      return ((Executor)obj).getName();
    }
    return "Internal";
  }
  
  public Request createRequest()
  {
    Request request = new Request();
    request.setConnector(this);
    return request;
  }
  
  public Response createResponse()
  {
    Response response = new Response();
    response.setConnector(this);
    return response;
  }
  
  protected String createObjectNameKeyProperties(String type)
  {
    Object addressObj = getProperty("address");
    
    StringBuilder sb = new StringBuilder("type=");
    sb.append(type);
    sb.append(",port=");
    int port = getPort();
    if (port > 0)
    {
      sb.append(getPort());
    }
    else
    {
      sb.append("auto-");
      sb.append(getProperty("nameIndex"));
    }
    String address = "";
    if ((addressObj instanceof InetAddress)) {
      address = ((InetAddress)addressObj).getHostAddress();
    } else if (addressObj != null) {
      address = addressObj.toString();
    }
    if (address.length() > 0)
    {
      sb.append(",address=");
      sb.append(ObjectName.quote(address));
    }
    return sb.toString();
  }
  
  public void pause()
  {
    try
    {
      this.protocolHandler.pause();
    }
    catch (Exception e)
    {
      log.error(sm.getString("coyoteConnector.protocolHandlerPauseFailed"), e);
    }
  }
  
  public void resume()
  {
    try
    {
      this.protocolHandler.resume();
    }
    catch (Exception e)
    {
      log.error(sm.getString("coyoteConnector.protocolHandlerResumeFailed"), e);
    }
  }
  
  protected void initInternal()
    throws LifecycleException
  {
    super.initInternal();
    
    this.adapter = new CoyoteAdapter(this);
    this.protocolHandler.setAdapter(this.adapter);
    if (null == this.parseBodyMethodsSet) {
      setParseBodyMethods(getParseBodyMethods());
    }
    if ((this.protocolHandler.isAprRequired()) && (!AprLifecycleListener.isAprAvailable())) {
      throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerNoApr", new Object[] { getProtocolHandlerClassName() }));
    }
    try
    {
      this.protocolHandler.init();
    }
    catch (Exception e)
    {
      throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerInitializationFailed"), e);
    }
    this.mapperListener.init();
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    if (getPort() < 0) {
      throw new LifecycleException(sm.getString("coyoteConnector.invalidPort", new Object[] { Integer.valueOf(getPort()) }));
    }
    setState(LifecycleState.STARTING);
    try
    {
      this.protocolHandler.start();
    }
    catch (Exception e)
    {
      String errPrefix = "";
      if (this.service != null) {
        errPrefix = errPrefix + "service.getName(): \"" + this.service.getName() + "\"; ";
      }
      throw new LifecycleException(errPrefix + " " + sm.getString("coyoteConnector.protocolHandlerStartFailed"), e);
    }
    this.mapperListener.start();
  }
  
  protected void stopInternal()
    throws LifecycleException
  {
    setState(LifecycleState.STOPPING);
    try
    {
      this.protocolHandler.stop();
    }
    catch (Exception e)
    {
      throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerStopFailed"), e);
    }
    this.mapperListener.stop();
  }
  
  protected void destroyInternal()
    throws LifecycleException
  {
    this.mapperListener.destroy();
    try
    {
      this.protocolHandler.destroy();
    }
    catch (Exception e)
    {
      throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerDestroyFailed"), e);
    }
    if (getService() != null) {
      getService().removeConnector(this);
    }
    super.destroyInternal();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("Connector[");
    sb.append(getProtocol());
    sb.append('-');
    int port = getPort();
    if (port > 0)
    {
      sb.append(getPort());
    }
    else
    {
      sb.append("auto-");
      sb.append(getProperty("nameIndex"));
    }
    sb.append(']');
    return sb.toString();
  }
  
  protected String getDomainInternal()
  {
    return MBeanUtils.getDomain(getService());
  }
  
  protected String getObjectNameKeyProperties()
  {
    return createObjectNameKeyProperties("Connector");
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\connector\Connector.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */