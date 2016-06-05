package org.apache.catalina.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationListener;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.Injectable;
import org.apache.catalina.deploy.InjectionTarget;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.MessageDestination;
import org.apache.catalina.deploy.MessageDestinationRef;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.TldConfig;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.util.ExtensionValidator;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.URLEncoder;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.ContextBindings;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.WARDirContext;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.scan.StandardJarScanner;

import com.taobao.tomcat.container.context.pandora.PandoraManager;

public class StandardContext extends ContainerBase implements org.apache.catalina.Context, NotificationEmitter {
	private static final Log log = LogFactory.getLog(StandardContext.class);
	private PandoraManager pandoraManager;
	private static final String info = "org.apache.catalina.core.StandardContext/1.0";

	public StandardContext() {
		this.pipeline.setBasic(new StandardContextValve());
		this.broadcaster = new NotificationBroadcasterSupport();
		if (!Globals.STRICT_SERVLET_COMPLIANCE) {
			this.resourceOnlyServlets.add("jsp");
		}
	}

	protected static URLEncoder urlEncoder = new URLEncoder();

	static {
		urlEncoder.addSafeCharacter('~');
		urlEncoder.addSafeCharacter('-');
		urlEncoder.addSafeCharacter('_');
		urlEncoder.addSafeCharacter('.');
		urlEncoder.addSafeCharacter('*');
		urlEncoder.addSafeCharacter('/');
	}

	protected boolean allowCasualMultipartParsing = false;
	private boolean swallowAbortedUploads = true;
	private String altDDName = null;
	private InstanceManager instanceManager = null;
	private String hostName;
	private boolean antiJARLocking = false;
	private boolean antiResourceLocking = false;
	private ApplicationListener[] applicationListeners = new ApplicationListener[0];
	private final Object applicationListenersLock = new Object();
	private final Set<Object> noPluggabilityListeners = new HashSet();
	private Object[] applicationEventListenersObjects = new Object[0];
	private Object[] applicationLifecycleListenersObjects = new Object[0];
	private Map<ServletContainerInitializer, Set<Class<?>>> initializers = new LinkedHashMap();
	private ApplicationParameter[] applicationParameters = new ApplicationParameter[0];
	private final Object applicationParametersLock = new Object();
	private NotificationBroadcasterSupport broadcaster = null;
	private CharsetMapper charsetMapper = null;
	private String charsetMapperClass = "org.apache.catalina.util.CharsetMapper";
	private URL configFile = null;
	private boolean configured = false;
	private volatile SecurityConstraint[] constraints = new SecurityConstraint[0];
	private final Object constraintsLock = new Object();
	protected ApplicationContext context = null;
	private NoPluggabilityServletContext noPluggabilityServletContext = null;
	private String compilerClasspath = null;
	private boolean cookies = true;
	private boolean crossContext = false;
	private String encodedPath = null;
	private String path = null;
	private boolean delegate = false;
	private String displayName = null;
	private String defaultContextXml;
	private String defaultWebXml;
	private boolean distributable = false;
	private String docBase = null;
	private HashMap<String, ErrorPage> exceptionPages = new HashMap();
	private HashMap<String, ApplicationFilterConfig> filterConfigs = new HashMap();
	private HashMap<String, FilterDef> filterDefs = new HashMap();
	private final ContextFilterMaps filterMaps = new ContextFilterMaps();
	private boolean ignoreAnnotations = false;
	private String[] instanceListeners = new String[0];
	private final Object instanceListenersLock = new Object();
	private LoginConfig loginConfig = null;
	private Mapper mapper = new Mapper();
	private NamingContextListener namingContextListener = null;
	private NamingResources namingResources = null;
	private HashMap<String, MessageDestination> messageDestinations = new HashMap();
	private HashMap<String, String> mimeMappings = new HashMap();
	private ErrorPage okErrorPage = null;
	private HashMap<String, String> parameters = new HashMap();
	private volatile boolean paused = false;
	private String publicId = null;
	private boolean reloadable = false;
	private boolean unpackWAR = true;
	private boolean copyXML = false;
	private boolean override = false;
	private String originalDocBase = null;
	private boolean privileged = false;
	private boolean replaceWelcomeFiles = false;
	private HashMap<String, String> roleMappings = new HashMap();
	private String[] securityRoles = new String[0];
	private final Object securityRolesLock = new Object();
	private HashMap<String, String> servletMappings = new HashMap();
	private final Object servletMappingsLock = new Object();
	private int sessionTimeout = 30;
	private AtomicLong sequenceNumber = new AtomicLong(0L);
	private HashMap<Integer, ErrorPage> statusPages = new HashMap();
	private boolean swallowOutput = false;
	private long unloadDelay = 2000L;
	private String[] watchedResources = new String[0];
	private final Object watchedResourcesLock = new Object();
	private String[] welcomeFiles = new String[0];
	private final Object welcomeFilesLock = new Object();
	private String[] wrapperLifecycles = new String[0];
	private final Object wrapperLifecyclesLock = new Object();
	private String[] wrapperListeners = new String[0];
	private final Object wrapperListenersLock = new Object();
	private String workDir = null;
	private String wrapperClassName = StandardWrapper.class.getName();
	private Class<?> wrapperClass = null;
	private boolean useNaming = true;
	private boolean filesystemBased = false;
	private String namingContextName = null;
	private boolean cachingAllowed = true;
	protected boolean allowLinking = false;
	protected int cacheMaxSize = 10240;
	private boolean xmlBlockExternal = true;
	protected int cacheObjectMaxSize = 512;
	protected int cacheTTL = 5000;
	private String aliases = null;
	private DirContext webappResources = null;
	private long startupTime;
	private long startTime;
	private long tldScanTime;
	private String j2EEApplication = "none";
	private String j2EEServer = "none";
	private boolean webXmlValidation = Globals.STRICT_SERVLET_COMPLIANCE;
	private boolean webXmlNamespaceAware = Globals.STRICT_SERVLET_COMPLIANCE;
	private boolean processTlds = true;
	private boolean tldValidation = Globals.STRICT_SERVLET_COMPLIANCE;
	private boolean saveConfig = true;
	private String sessionCookieName;
	private boolean useHttpOnly = true;
	private String sessionCookieDomain;
	private String sessionCookiePath;
	private boolean sessionCookiePathUsesTrailingSlash = true;
	private JarScanner jarScanner = null;
	private boolean clearReferencesStatic = false;
	private boolean clearReferencesStopThreads = false;
	private boolean clearReferencesStopTimerThreads = false;
	private boolean clearReferencesHttpClientKeepAliveThread = true;
	private boolean renewThreadsWhenStoppingContext = true;
	private boolean logEffectiveWebXml = false;
	private int effectiveMajorVersion = 3;
	private int effectiveMinorVersion = 0;
	private JspConfigDescriptor jspConfigDescriptor = new ApplicationJspConfigDescriptor();
	private Set<String> resourceOnlyServlets = new HashSet();
	private String webappVersion = "";
	private boolean addWebinfClassesResources = false;
	private boolean fireRequestListenersOnForwards = false;
	private Set<Servlet> createdServlets = new HashSet();
	private boolean preemptiveAuthentication = false;
	private boolean sendRedirectBody = false;
	private boolean jndiExceptionOnFailedWrite = true;
	private Map<String, String> postConstructMethods = new HashMap();
	private Map<String, String> preDestroyMethods = new HashMap();
	private String containerSciFilter;
	private Boolean failCtxIfServletStartFails;
	private MBeanNotificationInfo[] notificationInfo;

	public void setContainerSciFilter(String containerSciFilter) {
		this.containerSciFilter = containerSciFilter;
	}

	public String getContainerSciFilter() {
		return this.containerSciFilter;
	}

	public boolean getSendRedirectBody() {
		return this.sendRedirectBody;
	}

	public void setSendRedirectBody(boolean sendRedirectBody) {
		this.sendRedirectBody = sendRedirectBody;
	}

	public boolean getPreemptiveAuthentication() {
		return this.preemptiveAuthentication;
	}

	public void setPreemptiveAuthentication(boolean preemptiveAuthentication) {
		this.preemptiveAuthentication = preemptiveAuthentication;
	}

	public void setFireRequestListenersOnForwards(boolean enable) {
		this.fireRequestListenersOnForwards = enable;
	}

	public boolean getFireRequestListenersOnForwards() {
		return this.fireRequestListenersOnForwards;
	}

	public void setAddWebinfClassesResources(boolean addWebinfClassesResources) {
		this.addWebinfClassesResources = addWebinfClassesResources;
	}

	public boolean getAddWebinfClassesResources() {
		return this.addWebinfClassesResources;
	}

	public void setWebappVersion(String webappVersion) {
		if (null == webappVersion) {
			this.webappVersion = "";
		} else {
			this.webappVersion = webappVersion;
		}
	}

	public void setPandoraManager(PandoraManager pandoraManager) {
		this.pandoraManager = pandoraManager;
	}

	public PandoraManager getPandoraManager() {
		return this.pandoraManager;
	}

	public String getWebappVersion() {
		return this.webappVersion;
	}

	public String getBaseName() {
		return new ContextName(this.path, this.webappVersion).getBaseName();
	}

	public String getResourceOnlyServlets() {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (String servletName : this.resourceOnlyServlets) {
			if (first) {
				first = false;
			} else {
				result.append(',');
			}
			result.append(servletName);
		}
		return result.toString();
	}

	public void setResourceOnlyServlets(String resourceOnlyServlets) {
		this.resourceOnlyServlets.clear();
		if (resourceOnlyServlets == null) {
			return;
		}
		for (String servletName : resourceOnlyServlets.split(",")) {
			servletName = servletName.trim();
			if (servletName.length() > 0) {
				this.resourceOnlyServlets.add(servletName);
			}
		}
	}

	public boolean isResourceOnlyServlet(String servletName) {
		return this.resourceOnlyServlets.contains(servletName);
	}

	public int getEffectiveMajorVersion() {
		return this.effectiveMajorVersion;
	}

	public void setEffectiveMajorVersion(int effectiveMajorVersion) {
		this.effectiveMajorVersion = effectiveMajorVersion;
	}

	public int getEffectiveMinorVersion() {
		return this.effectiveMinorVersion;
	}

	public void setEffectiveMinorVersion(int effectiveMinorVersion) {
		this.effectiveMinorVersion = effectiveMinorVersion;
	}

	public void setLogEffectiveWebXml(boolean logEffectiveWebXml) {
		this.logEffectiveWebXml = logEffectiveWebXml;
	}

	public boolean getLogEffectiveWebXml() {
		return this.logEffectiveWebXml;
	}

	public Authenticator getAuthenticator() {
		if ((this instanceof Authenticator)) {
			return (Authenticator) this;
		}
		Pipeline pipeline = getPipeline();
		if (pipeline != null) {
			Valve basic = pipeline.getBasic();
			if ((basic != null) && ((basic instanceof Authenticator))) {
				return (Authenticator) basic;
			}
			Valve[] valves = pipeline.getValves();
			for (int i = 0; i < valves.length; i++) {
				if ((valves[i] instanceof Authenticator)) {
					return (Authenticator) valves[i];
				}
			}
		}
		return null;
	}

	public JarScanner getJarScanner() {
		if (this.jarScanner == null) {
			this.jarScanner = new StandardJarScanner();
		}
		return this.jarScanner;
	}

	public void setJarScanner(JarScanner jarScanner) {
		this.jarScanner = jarScanner;
	}

	public InstanceManager getInstanceManager() {
		return this.instanceManager;
	}

	public void setInstanceManager(InstanceManager instanceManager) {
		this.instanceManager = instanceManager;
	}

	public String getEncodedPath() {
		return this.encodedPath;
	}

	public boolean isCachingAllowed() {
		return this.cachingAllowed;
	}

	public void setCachingAllowed(boolean cachingAllowed) {
		this.cachingAllowed = cachingAllowed;
	}

	public void setAllowLinking(boolean allowLinking) {
		this.allowLinking = allowLinking;
	}

	public boolean isAllowLinking() {
		return this.allowLinking;
	}

	public void setAllowCasualMultipartParsing(boolean allowCasualMultipartParsing) {
		this.allowCasualMultipartParsing = allowCasualMultipartParsing;
	}

	public boolean getAllowCasualMultipartParsing() {
		return this.allowCasualMultipartParsing;
	}

	public void setSwallowAbortedUploads(boolean swallowAbortedUploads) {
		this.swallowAbortedUploads = swallowAbortedUploads;
	}

	public boolean getSwallowAbortedUploads() {
		return this.swallowAbortedUploads;
	}

	public void setCacheTTL(int cacheTTL) {
		this.cacheTTL = cacheTTL;
	}

	public int getCacheTTL() {
		return this.cacheTTL;
	}

	public int getCacheMaxSize() {
		return this.cacheMaxSize;
	}

	public void setCacheMaxSize(int cacheMaxSize) {
		this.cacheMaxSize = cacheMaxSize;
	}

	public int getCacheObjectMaxSize() {
		return this.cacheObjectMaxSize;
	}

	public void setCacheObjectMaxSize(int cacheObjectMaxSize) {
		this.cacheObjectMaxSize = cacheObjectMaxSize;
	}

	public String getAliases() {
		return this.aliases;
	}

	public void addResourceJarUrl(URL url) {
		if ((this.webappResources instanceof BaseDirContext)) {
			((BaseDirContext) this.webappResources).addResourcesJar(url);
		} else {
			log.error(sm.getString("standardContext.noResourceJar", new Object[] { url, getName() }));
		}
	}

	public void addResourcesDirContext(DirContext altDirContext) {
		if ((this.webappResources instanceof BaseDirContext)) {
			((BaseDirContext) this.webappResources).addAltDirContext(altDirContext);
		} else {
			log.error(sm.getString("standardContext.noResourceJar", new Object[] { altDirContext, getName() }));
		}
	}

	public void setAliases(String aliases) {
		this.aliases = aliases;
	}

	public void addServletContainerInitializer(ServletContainerInitializer sci, Set<Class<?>> classes) {
		this.initializers.put(sci, classes);
	}

	public boolean getDelegate() {
		return this.delegate;
	}

	public void setDelegate(boolean delegate) {
		boolean oldDelegate = this.delegate;
		this.delegate = delegate;
		this.support.firePropertyChange("delegate", oldDelegate, this.delegate);
	}

	public boolean isUseNaming() {
		return this.useNaming;
	}

	public void setUseNaming(boolean useNaming) {
		this.useNaming = useNaming;
	}

	@Deprecated
	public boolean isFilesystemBased() {
		return this.filesystemBased;
	}

	public Object[] getApplicationEventListeners() {
		return this.applicationEventListenersObjects;
	}

	public void setApplicationEventListeners(Object[] listeners) {
		this.applicationEventListenersObjects = listeners;
	}

	public void addApplicationEventListener(Object listener) {
		int len = this.applicationEventListenersObjects.length;
		Object[] newListeners = Arrays.copyOf(this.applicationEventListenersObjects, len + 1);

		newListeners[len] = listener;
		this.applicationEventListenersObjects = newListeners;
	}

	public Object[] getApplicationLifecycleListeners() {
		return this.applicationLifecycleListenersObjects;
	}

	public void setApplicationLifecycleListeners(Object[] listeners) {
		this.applicationLifecycleListenersObjects = listeners;
	}

	public void addApplicationLifecycleListener(Object listener) {
		int len = this.applicationLifecycleListenersObjects.length;
		Object[] newListeners = Arrays.copyOf(this.applicationLifecycleListenersObjects, len + 1);

		newListeners[len] = listener;
		this.applicationLifecycleListenersObjects = newListeners;
	}

	public boolean getAntiJARLocking() {
		return this.antiJARLocking;
	}

	public boolean getAntiResourceLocking() {
		return this.antiResourceLocking;
	}

	public void setAntiJARLocking(boolean antiJARLocking) {
		boolean oldAntiJARLocking = this.antiJARLocking;
		this.antiJARLocking = antiJARLocking;
		this.support.firePropertyChange("antiJARLocking", oldAntiJARLocking, this.antiJARLocking);
	}

	public void setAntiResourceLocking(boolean antiResourceLocking) {
		boolean oldAntiResourceLocking = this.antiResourceLocking;
		this.antiResourceLocking = antiResourceLocking;
		this.support.firePropertyChange("antiResourceLocking", oldAntiResourceLocking, this.antiResourceLocking);
	}

	public boolean getAvailable() {
		return getState().isAvailable();
	}

	public CharsetMapper getCharsetMapper() {
		if (this.charsetMapper == null) {
			try {
				Class<?> clazz = Class.forName(this.charsetMapperClass);
				this.charsetMapper = ((CharsetMapper) clazz.newInstance());
			} catch (Throwable t) {
				ExceptionUtils.handleThrowable(t);
				this.charsetMapper = new CharsetMapper();
			}
		}
		return this.charsetMapper;
	}

	public void setCharsetMapper(CharsetMapper mapper) {
		CharsetMapper oldCharsetMapper = this.charsetMapper;
		this.charsetMapper = mapper;
		if (mapper != null) {
			this.charsetMapperClass = mapper.getClass().getName();
		}
		this.support.firePropertyChange("charsetMapper", oldCharsetMapper, this.charsetMapper);
	}

	public String getCharset(Locale locale) {
		return getCharsetMapper().getCharset(locale);
	}

	public URL getConfigFile() {
		return this.configFile;
	}

	public void setConfigFile(URL configFile) {
		this.configFile = configFile;
	}

	public boolean getConfigured() {
		return this.configured;
	}

	public void setConfigured(boolean configured) {
		boolean oldConfigured = this.configured;
		this.configured = configured;
		this.support.firePropertyChange("configured", oldConfigured, this.configured);
	}

	public boolean getCookies() {
		return this.cookies;
	}

	public void setCookies(boolean cookies) {
		boolean oldCookies = this.cookies;
		this.cookies = cookies;
		this.support.firePropertyChange("cookies", oldCookies, this.cookies);
	}

	public String getSessionCookieName() {
		return this.sessionCookieName;
	}

	public void setSessionCookieName(String sessionCookieName) {
		String oldSessionCookieName = this.sessionCookieName;
		this.sessionCookieName = sessionCookieName;
		this.support.firePropertyChange("sessionCookieName", oldSessionCookieName, sessionCookieName);
	}

	public boolean getUseHttpOnly() {
		return this.useHttpOnly;
	}

	public void setUseHttpOnly(boolean useHttpOnly) {
		boolean oldUseHttpOnly = this.useHttpOnly;
		this.useHttpOnly = useHttpOnly;
		this.support.firePropertyChange("useHttpOnly", oldUseHttpOnly, this.useHttpOnly);
	}

	public String getSessionCookieDomain() {
		return this.sessionCookieDomain;
	}

	public void setSessionCookieDomain(String sessionCookieDomain) {
		String oldSessionCookieDomain = this.sessionCookieDomain;
		this.sessionCookieDomain = sessionCookieDomain;
		this.support.firePropertyChange("sessionCookieDomain", oldSessionCookieDomain, sessionCookieDomain);
	}

	public String getSessionCookiePath() {
		return this.sessionCookiePath;
	}

	public void setSessionCookiePath(String sessionCookiePath) {
		String oldSessionCookiePath = this.sessionCookiePath;
		this.sessionCookiePath = sessionCookiePath;
		this.support.firePropertyChange("sessionCookiePath", oldSessionCookiePath, sessionCookiePath);
	}

	public boolean getSessionCookiePathUsesTrailingSlash() {
		return this.sessionCookiePathUsesTrailingSlash;
	}

	public void setSessionCookiePathUsesTrailingSlash(boolean sessionCookiePathUsesTrailingSlash) {
		this.sessionCookiePathUsesTrailingSlash = sessionCookiePathUsesTrailingSlash;
	}

	public boolean getCrossContext() {
		return this.crossContext;
	}

	public void setCrossContext(boolean crossContext) {
		boolean oldCrossContext = this.crossContext;
		this.crossContext = crossContext;
		this.support.firePropertyChange("crossContext", oldCrossContext, this.crossContext);
	}

	public String getDefaultContextXml() {
		return this.defaultContextXml;
	}

	public void setDefaultContextXml(String defaultContextXml) {
		this.defaultContextXml = defaultContextXml;
	}

	public String getDefaultWebXml() {
		return this.defaultWebXml;
	}

	public void setDefaultWebXml(String defaultWebXml) {
		this.defaultWebXml = defaultWebXml;
	}

	public long getStartupTime() {
		return this.startupTime;
	}

	public void setStartupTime(long startupTime) {
		this.startupTime = startupTime;
	}

	public long getTldScanTime() {
		return this.tldScanTime;
	}

	public void setTldScanTime(long tldScanTime) {
		this.tldScanTime = tldScanTime;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getAltDDName() {
		return this.altDDName;
	}

	public void setAltDDName(String altDDName) {
		this.altDDName = altDDName;
		if (this.context != null) {
			this.context.setAttribute("org.apache.catalina.deploy.alt_dd", altDDName);
		}
	}

	@Deprecated
	public String getCompilerClasspath() {
		return this.compilerClasspath;
	}

	@Deprecated
	public void setCompilerClasspath(String compilerClasspath) {
		this.compilerClasspath = compilerClasspath;
	}

	public void setDisplayName(String displayName) {
		String oldDisplayName = this.displayName;
		this.displayName = displayName;
		this.support.firePropertyChange("displayName", oldDisplayName, this.displayName);
	}

	public boolean getDistributable() {
		return this.distributable;
	}

	public void setDistributable(boolean distributable) {
		boolean oldDistributable = this.distributable;
		this.distributable = distributable;
		this.support.firePropertyChange("distributable", oldDistributable, this.distributable);
		if (getManager() != null) {
			if (log.isDebugEnabled()) {
				log.debug("Propagating distributable=" + distributable + " to manager");
			}
			getManager().setDistributable(distributable);
		}
	}

	public String getDocBase() {
		return this.docBase;
	}

	public void setDocBase(String docBase) {
		this.docBase = docBase;
	}

	public String getInfo() {
		return "org.apache.catalina.core.StandardContext/1.0";
	}

	public String getJ2EEApplication() {
		return this.j2EEApplication;
	}

	public void setJ2EEApplication(String j2EEApplication) {
		this.j2EEApplication = j2EEApplication;
	}

	public String getJ2EEServer() {
		return this.j2EEServer;
	}

	public void setJ2EEServer(String j2EEServer) {
		this.j2EEServer = j2EEServer;
	}

	public synchronized void setLoader(Loader loader) {
		super.setLoader(loader);
	}

	public boolean getIgnoreAnnotations() {
		return this.ignoreAnnotations;
	}

	public void setIgnoreAnnotations(boolean ignoreAnnotations) {
		boolean oldIgnoreAnnotations = this.ignoreAnnotations;
		this.ignoreAnnotations = ignoreAnnotations;
		this.support.firePropertyChange("ignoreAnnotations", oldIgnoreAnnotations, this.ignoreAnnotations);
	}

	public LoginConfig getLoginConfig() {
		return this.loginConfig;
	}

	public void setLoginConfig(LoginConfig config) {
		if (config == null) {
			throw new IllegalArgumentException(sm.getString("standardContext.loginConfig.required"));
		}
		String loginPage = config.getLoginPage();
		if ((loginPage != null) && (!loginPage.startsWith("/"))) {
			if (isServlet22()) {
				if (log.isDebugEnabled()) {
					log.debug(sm.getString("standardContext.loginConfig.loginWarning", new Object[] { loginPage }));
				}
				config.setLoginPage("/" + loginPage);
			} else {
				throw new IllegalArgumentException(
						sm.getString("standardContext.loginConfig.loginPage", new Object[] { loginPage }));
			}
		}
		String errorPage = config.getErrorPage();
		if ((errorPage != null) && (!errorPage.startsWith("/"))) {
			if (isServlet22()) {
				if (log.isDebugEnabled()) {
					log.debug(sm.getString("standardContext.loginConfig.errorWarning", new Object[] { errorPage }));
				}
				config.setErrorPage("/" + errorPage);
			} else {
				throw new IllegalArgumentException(
						sm.getString("standardContext.loginConfig.errorPage", new Object[] { errorPage }));
			}
		}
		LoginConfig oldLoginConfig = this.loginConfig;
		this.loginConfig = config;
		this.support.firePropertyChange("loginConfig", oldLoginConfig, this.loginConfig);
	}

	public Mapper getMapper() {
		return this.mapper;
	}

	public NamingResources getNamingResources() {
		if (this.namingResources == null) {
			setNamingResources(new NamingResources());
		}
		return this.namingResources;
	}

	public void setNamingResources(NamingResources namingResources) {
		NamingResources oldNamingResources = this.namingResources;
		this.namingResources = namingResources;
		if (namingResources != null) {
			namingResources.setContainer(this);
		}
		this.support.firePropertyChange("namingResources", oldNamingResources, this.namingResources);
		if ((getState() == LifecycleState.NEW) || (getState() == LifecycleState.INITIALIZING)
				|| (getState() == LifecycleState.INITIALIZED)) {
			return;
		}
		if (oldNamingResources != null) {
			try {
				oldNamingResources.stop();
				oldNamingResources.destroy();
			} catch (LifecycleException e) {
				log.warn("standardContext.namingResource.destroy.fail", e);
			}
		}
		if (namingResources != null) {
			try {
				namingResources.init();
				namingResources.start();
			} catch (LifecycleException e) {
				log.warn("standardContext.namingResource.init.fail", e);
			}
		}
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		boolean invalid = false;
		if ((path == null) || (path.equals("/"))) {
			invalid = true;
			this.path = "";
		} else if (("".equals(path)) || (path.startsWith("/"))) {
			this.path = path;
		} else {
			invalid = true;
			this.path = ("/" + path);
		}
		if (this.path.endsWith("/")) {
			invalid = true;
			this.path = this.path.substring(0, this.path.length() - 1);
		}
		if (invalid) {
			log.warn(sm.getString("standardContext.pathInvalid", new Object[] { path, this.path }));
		}
		this.encodedPath = urlEncoder.encode(this.path);
		if (getName() == null) {
			setName(this.path);
		}
	}

	public String getPublicId() {
		return this.publicId;
	}

	public void setPublicId(String publicId) {
		if (log.isDebugEnabled()) {
			log.debug("Setting deployment descriptor public ID to '" + publicId + "'");
		}
		String oldPublicId = this.publicId;
		this.publicId = publicId;
		this.support.firePropertyChange("publicId", oldPublicId, publicId);
	}

	public boolean getReloadable() {
		return this.reloadable;
	}

	public boolean getOverride() {
		return this.override;
	}

	public String getOriginalDocBase() {
		return this.originalDocBase;
	}

	public void setOriginalDocBase(String docBase) {
		this.originalDocBase = docBase;
	}

	public ClassLoader getParentClassLoader() {
		if (this.parentClassLoader != null) {
			return this.parentClassLoader;
		}
		if (getPrivileged()) {
			return getClass().getClassLoader();
		}
		if (this.parent != null) {
			return this.parent.getParentClassLoader();
		}
		return ClassLoader.getSystemClassLoader();
	}

	public boolean getPrivileged() {
		return this.privileged;
	}

	public void setPrivileged(boolean privileged) {
		boolean oldPrivileged = this.privileged;
		this.privileged = privileged;
		this.support.firePropertyChange("privileged", oldPrivileged, this.privileged);
	}

	public void setReloadable(boolean reloadable) {
		boolean oldReloadable = this.reloadable;
		this.reloadable = reloadable;
		this.support.firePropertyChange("reloadable", oldReloadable, this.reloadable);
	}

	public void setOverride(boolean override) {
		boolean oldOverride = this.override;
		this.override = override;
		this.support.firePropertyChange("override", oldOverride, this.override);
	}

	@Deprecated
	public boolean isReplaceWelcomeFiles() {
		return this.replaceWelcomeFiles;
	}

	public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {
		boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
		this.replaceWelcomeFiles = replaceWelcomeFiles;
		this.support.firePropertyChange("replaceWelcomeFiles", oldReplaceWelcomeFiles, this.replaceWelcomeFiles);
	}

	public ServletContext getServletContext() {
		if (this.context == null) {
			this.context = new ApplicationContext(this);
			if (this.altDDName != null) {
				this.context.setAttribute("org.apache.catalina.deploy.alt_dd", this.altDDName);
			}
		}
		return this.context.getFacade();
	}

	public int getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(int timeout) {
		int oldSessionTimeout = this.sessionTimeout;

		this.sessionTimeout = (timeout == 0 ? -1 : timeout);
		this.support.firePropertyChange("sessionTimeout", oldSessionTimeout, this.sessionTimeout);
	}

	public boolean getSwallowOutput() {
		return this.swallowOutput;
	}

	public void setSwallowOutput(boolean swallowOutput) {
		boolean oldSwallowOutput = this.swallowOutput;
		this.swallowOutput = swallowOutput;
		this.support.firePropertyChange("swallowOutput", oldSwallowOutput, this.swallowOutput);
	}

	public long getUnloadDelay() {
		return this.unloadDelay;
	}

	public void setUnloadDelay(long unloadDelay) {
		long oldUnloadDelay = this.unloadDelay;
		this.unloadDelay = unloadDelay;
		this.support.firePropertyChange("unloadDelay", Long.valueOf(oldUnloadDelay), Long.valueOf(this.unloadDelay));
	}

	public boolean getUnpackWAR() {
		return this.unpackWAR;
	}

	public void setUnpackWAR(boolean unpackWAR) {
		this.unpackWAR = unpackWAR;
	}

	public boolean getCopyXML() {
		return this.copyXML;
	}

	public void setCopyXML(boolean copyXML) {
		this.copyXML = copyXML;
	}

	public String getWrapperClass() {
		return this.wrapperClassName;
	}

	public void setWrapperClass(String wrapperClassName) {
		this.wrapperClassName = wrapperClassName;
		try {
			this.wrapperClass = Class.forName(wrapperClassName);
			if (!StandardWrapper.class.isAssignableFrom(this.wrapperClass)) {
				throw new IllegalArgumentException(
						sm.getString("standardContext.invalidWrapperClass", new Object[] { wrapperClassName }));
			}
		} catch (ClassNotFoundException cnfe) {
			throw new IllegalArgumentException(cnfe.getMessage());
		}
	}

	public synchronized void setResources(DirContext resources) {
		if (getState().isAvailable()) {
			throw new IllegalStateException(sm.getString("standardContext.resources.started"));
		}
		DirContext oldResources = this.webappResources;
		if (oldResources == resources) {
			return;
		}
		if ((resources instanceof BaseDirContext)) {
			((BaseDirContext) resources).setCached(isCachingAllowed());
			((BaseDirContext) resources).setCacheTTL(getCacheTTL());
			((BaseDirContext) resources).setCacheMaxSize(getCacheMaxSize());
			((BaseDirContext) resources).setCacheObjectMaxSize(getCacheObjectMaxSize());

			((BaseDirContext) resources).setAliases(getAliases());
		}
		if ((resources instanceof FileDirContext)) {
			this.filesystemBased = true;
			((FileDirContext) resources).setAllowLinking(isAllowLinking());
		}
		this.webappResources = resources;

		this.resources = null;

		this.support.firePropertyChange("resources", oldResources, this.webappResources);
	}

	public JspConfigDescriptor getJspConfigDescriptor() {
		return this.jspConfigDescriptor;
	}

	public boolean getJndiExceptionOnFailedWrite() {
		return this.jndiExceptionOnFailedWrite;
	}

	public void setJndiExceptionOnFailedWrite(boolean jndiExceptionOnFailedWrite) {
		this.jndiExceptionOnFailedWrite = jndiExceptionOnFailedWrite;
	}

	public String getCharsetMapperClass() {
		return this.charsetMapperClass;
	}

	public void setCharsetMapperClass(String mapper) {
		String oldCharsetMapperClass = this.charsetMapperClass;
		this.charsetMapperClass = mapper;
		this.support.firePropertyChange("charsetMapperClass", oldCharsetMapperClass, this.charsetMapperClass);
	}

	public String getWorkPath() {
		if (getWorkDir() == null) {
			return null;
		}
		File workDir = new File(getWorkDir());
		if (!workDir.isAbsolute()) {
			File catalinaHome = engineBase();
			String catalinaHomePath = null;
			try {
				catalinaHomePath = catalinaHome.getCanonicalPath();
				workDir = new File(catalinaHomePath, getWorkDir());
			} catch (IOException e) {
				log.warn(sm.getString("standardContext.workPath", new Object[] { getName() }), e);
			}
		}
		return workDir.getAbsolutePath();
	}

	public String getWorkDir() {
		return this.workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
		if (getState().isAvailable()) {
			postWorkDirectory();
		}
	}

	@Deprecated
	public boolean isSaveConfig() {
		return this.saveConfig;
	}

	@Deprecated
	public void setSaveConfig(boolean saveConfig) {
		this.saveConfig = saveConfig;
	}

	public boolean getClearReferencesStatic() {
		return this.clearReferencesStatic;
	}

	public void setClearReferencesStatic(boolean clearReferencesStatic) {
		boolean oldClearReferencesStatic = this.clearReferencesStatic;
		this.clearReferencesStatic = clearReferencesStatic;
		this.support.firePropertyChange("clearReferencesStatic", oldClearReferencesStatic, this.clearReferencesStatic);
	}

	public boolean getClearReferencesStopThreads() {
		return this.clearReferencesStopThreads;
	}

	public void setClearReferencesStopThreads(boolean clearReferencesStopThreads) {
		boolean oldClearReferencesStopThreads = this.clearReferencesStopThreads;
		this.clearReferencesStopThreads = clearReferencesStopThreads;
		this.support.firePropertyChange("clearReferencesStopThreads", oldClearReferencesStopThreads,
				this.clearReferencesStopThreads);
	}

	public boolean getClearReferencesStopTimerThreads() {
		return this.clearReferencesStopTimerThreads;
	}

	public void setClearReferencesStopTimerThreads(boolean clearReferencesStopTimerThreads) {
		boolean oldClearReferencesStopTimerThreads = this.clearReferencesStopTimerThreads;

		this.clearReferencesStopTimerThreads = clearReferencesStopTimerThreads;
		this.support.firePropertyChange("clearReferencesStopTimerThreads", oldClearReferencesStopTimerThreads,
				this.clearReferencesStopTimerThreads);
	}

	public boolean getClearReferencesHttpClientKeepAliveThread() {
		return this.clearReferencesHttpClientKeepAliveThread;
	}

	public void setClearReferencesHttpClientKeepAliveThread(boolean clearReferencesHttpClientKeepAliveThread) {
		this.clearReferencesHttpClientKeepAliveThread = clearReferencesHttpClientKeepAliveThread;
	}

	public boolean getRenewThreadsWhenStoppingContext() {
		return this.renewThreadsWhenStoppingContext;
	}

	public void setRenewThreadsWhenStoppingContext(boolean renewThreadsWhenStoppingContext) {
		boolean oldRenewThreadsWhenStoppingContext = this.renewThreadsWhenStoppingContext;

		this.renewThreadsWhenStoppingContext = renewThreadsWhenStoppingContext;
		this.support.firePropertyChange("renewThreadsWhenStoppingContext", oldRenewThreadsWhenStoppingContext,
				this.renewThreadsWhenStoppingContext);
	}

	public Boolean getFailCtxIfServletStartFails() {
		return this.failCtxIfServletStartFails;
	}

	public void setFailCtxIfServletStartFails(Boolean failCtxIfServletStartFails) {
		Boolean oldFailCtxIfServletStartFails = this.failCtxIfServletStartFails;
		this.failCtxIfServletStartFails = failCtxIfServletStartFails;
		this.support.firePropertyChange("failCtxIfServletStartFails", oldFailCtxIfServletStartFails,
				failCtxIfServletStartFails);
	}

	protected boolean getComputedFailCtxIfServletStartFails() {
		if (this.failCtxIfServletStartFails != null) {
			return this.failCtxIfServletStartFails.booleanValue();
		}
		if ((getParent() instanceof StandardHost)) {
			return ((StandardHost) getParent()).isFailCtxIfServletStartFails();
		}
		return false;
	}

	public void addApplicationListener(String listener) {
		addApplicationListener(new ApplicationListener(listener, false));
	}

	public void addApplicationListener(ApplicationListener listener) {
		synchronized (this.applicationListenersLock) {
			ApplicationListener[] results = new ApplicationListener[this.applicationListeners.length + 1];
			for (int i = 0; i < this.applicationListeners.length; i++) {
				if (listener.equals(this.applicationListeners[i])) {
					log.info(sm.getString("standardContext.duplicateListener",
							new Object[] { listener.getClassName() }));

					return;
				}
				results[i] = this.applicationListeners[i];
			}
			results[this.applicationListeners.length] = listener;
			this.applicationListeners = results;
		}
		fireContainerEvent("addApplicationListener", listener);
	}

	public void addApplicationParameter(ApplicationParameter parameter) {
		synchronized (this.applicationParametersLock) {
			String newName = parameter.getName();
			for (ApplicationParameter p : this.applicationParameters) {
				if ((newName.equals(p.getName())) && (!p.getOverride())) {
					return;
				}
			}
			ApplicationParameter[] results = (ApplicationParameter[]) Arrays.copyOf(this.applicationParameters,
					this.applicationParameters.length + 1);

			results[this.applicationParameters.length] = parameter;
			this.applicationParameters = results;
		}
		fireContainerEvent("addApplicationParameter", parameter);
	}

	public void addChild(Container child) {
		Wrapper oldJspServlet = null;
		if (!(child instanceof Wrapper)) {
			throw new IllegalArgumentException(sm.getString("standardContext.notWrapper"));
		}
		boolean isJspServlet = "jsp".equals(child.getName());
		if (isJspServlet) {
			oldJspServlet = (Wrapper) findChild("jsp");
			if (oldJspServlet != null) {
				removeChild(oldJspServlet);
			}
		}
		super.addChild(child);
		if ((isJspServlet) && (oldJspServlet != null)) {
			String[] jspMappings = oldJspServlet.findMappings();
			for (int i = 0; (jspMappings != null) && (i < jspMappings.length); i++) {
				addServletMapping(jspMappings[i], child.getName());
			}
		}
	}

	public void addConstraint(SecurityConstraint constraint) {
		SecurityCollection[] collections = constraint.findCollections();
		for (int i = 0; i < collections.length; i++) {
			String[] patterns = collections[i].findPatterns();
			for (int j = 0; j < patterns.length; j++) {
				patterns[j] = adjustURLPattern(patterns[j]);
				if (!validateURLPattern(patterns[j])) {
					throw new IllegalArgumentException(
							sm.getString("standardContext.securityConstraint.pattern", new Object[] { patterns[j] }));
				}
			}
			if ((collections[i].findMethods().length > 0) && (collections[i].findOmittedMethods().length > 0)) {
				throw new IllegalArgumentException(sm.getString("standardContext.securityConstraint.mixHttpMethod"));
			}
		}
		synchronized (this.constraintsLock) {
			SecurityConstraint[] results = new SecurityConstraint[this.constraints.length + 1];
			for (int i = 0; i < this.constraints.length; i++) {
				results[i] = this.constraints[i];
			}
			results[this.constraints.length] = constraint;
			this.constraints = results;
		}
	}

	public void addErrorPage(ErrorPage errorPage) {
		if (errorPage == null) {
			throw new IllegalArgumentException(sm.getString("standardContext.errorPage.required"));
		}
		String location = errorPage.getLocation();
		if ((location != null) && (!location.startsWith("/"))) {
			if (isServlet22()) {
				if (log.isDebugEnabled()) {
					log.debug(sm.getString("standardContext.errorPage.warning", new Object[] { location }));
				}
				errorPage.setLocation("/" + location);
			} else {
				throw new IllegalArgumentException(
						sm.getString("standardContext.errorPage.error", new Object[] { location }));
			}
		}
		String exceptionType = errorPage.getExceptionType();
		if (exceptionType != null) {
			synchronized (this.exceptionPages) {
				this.exceptionPages.put(exceptionType, errorPage);
			}
		} else {
			synchronized (this.statusPages) {
				if (errorPage.getErrorCode() == 200) {
					this.okErrorPage = errorPage;
				}
				this.statusPages.put(Integer.valueOf(errorPage.getErrorCode()), errorPage);
			}
		}
		fireContainerEvent("addErrorPage", errorPage);
	}

	public void addFilterDef(FilterDef filterDef) {
		synchronized (this.filterDefs) {
			this.filterDefs.put(filterDef.getFilterName(), filterDef);
		}
		fireContainerEvent("addFilterDef", filterDef);
	}

	public void addFilterMap(FilterMap filterMap) {
		validateFilterMap(filterMap);

		this.filterMaps.add(filterMap);
		fireContainerEvent("addFilterMap", filterMap);
	}

	public void addFilterMapBefore(FilterMap filterMap) {
		validateFilterMap(filterMap);

		this.filterMaps.addBefore(filterMap);
		fireContainerEvent("addFilterMap", filterMap);
	}

	private void validateFilterMap(FilterMap filterMap) {
		String filterName = filterMap.getFilterName();
		String[] servletNames = filterMap.getServletNames();
		String[] urlPatterns = filterMap.getURLPatterns();
		if (findFilterDef(filterName) == null) {
			throw new IllegalArgumentException(
					sm.getString("standardContext.filterMap.name", new Object[] { filterName }));
		}
		if ((!filterMap.getMatchAllServletNames()) && (!filterMap.getMatchAllUrlPatterns())
				&& (servletNames.length == 0) && (urlPatterns.length == 0)) {
			throw new IllegalArgumentException(sm.getString("standardContext.filterMap.either"));
		}
		for (int i = 0; i < urlPatterns.length; i++) {
			if (!validateURLPattern(urlPatterns[i])) {
				throw new IllegalArgumentException(
						sm.getString("standardContext.filterMap.pattern", new Object[] { urlPatterns[i] }));
			}
		}
	}

	public void addInstanceListener(String listener) {
		synchronized (this.instanceListenersLock) {
			String[] results = new String[this.instanceListeners.length + 1];
			for (int i = 0; i < this.instanceListeners.length; i++) {
				results[i] = this.instanceListeners[i];
			}
			results[this.instanceListeners.length] = listener;
			this.instanceListeners = results;
		}
		fireContainerEvent("addInstanceListener", listener);
	}

	public void addLocaleEncodingMappingParameter(String locale, String encoding) {
		getCharsetMapper().addCharsetMappingFromDeploymentDescriptor(locale, encoding);
	}

	public void addMessageDestination(MessageDestination md) {
		synchronized (this.messageDestinations) {
			this.messageDestinations.put(md.getName(), md);
		}
		fireContainerEvent("addMessageDestination", md.getName());
	}

	public void addMessageDestinationRef(MessageDestinationRef mdr) {
		this.namingResources.addMessageDestinationRef(mdr);
		fireContainerEvent("addMessageDestinationRef", mdr.getName());
	}

	public void addMimeMapping(String extension, String mimeType) {
		synchronized (this.mimeMappings) {
			this.mimeMappings.put(extension, mimeType);
		}
		fireContainerEvent("addMimeMapping", extension);
	}

	public void addParameter(String name, String value) {
		if ((name == null) || (value == null)) {
			throw new IllegalArgumentException(sm.getString("standardContext.parameter.required"));
		}
		if (this.parameters.get(name) != null) {
			throw new IllegalArgumentException(
					sm.getString("standardContext.parameter.duplicate", new Object[] { name }));
		}
		synchronized (this.parameters) {
			this.parameters.put(name, value);
		}
		fireContainerEvent("addParameter", name);
	}

	public void addRoleMapping(String role, String link) {
		synchronized (this.roleMappings) {
			this.roleMappings.put(role, link);
		}
		fireContainerEvent("addRoleMapping", role);
	}

	public void addSecurityRole(String role) {
		synchronized (this.securityRolesLock) {
			String[] results = new String[this.securityRoles.length + 1];
			for (int i = 0; i < this.securityRoles.length; i++) {
				results[i] = this.securityRoles[i];
			}
			results[this.securityRoles.length] = role;
			this.securityRoles = results;
		}
		fireContainerEvent("addSecurityRole", role);
	}

	public void addServletMapping(String pattern, String name) {
		addServletMapping(pattern, name, false);
	}

	public void addServletMapping(String pattern, String name, boolean jspWildCard) {
		if (findChild(name) == null) {
			throw new IllegalArgumentException(sm.getString("standardContext.servletMap.name", new Object[] { name }));
		}
		String decodedPattern = adjustURLPattern(RequestUtil.URLDecode(pattern));
		if (!validateURLPattern(decodedPattern)) {
			throw new IllegalArgumentException(
					sm.getString("standardContext.servletMap.pattern", new Object[] { decodedPattern }));
		}
		synchronized (this.servletMappingsLock) {
			String name2 = (String) this.servletMappings.get(decodedPattern);
			if (name2 != null) {
				Wrapper wrapper = (Wrapper) findChild(name2);
				wrapper.removeMapping(decodedPattern);
				this.mapper.removeWrapper(decodedPattern);
			}
			this.servletMappings.put(decodedPattern, name);
		}
		Wrapper wrapper = (Wrapper) findChild(name);
		wrapper.addMapping(decodedPattern);

		this.mapper.addWrapper(decodedPattern, wrapper, jspWildCard, this.resourceOnlyServlets.contains(name));

		fireContainerEvent("addServletMapping", decodedPattern);
	}

	public void addWatchedResource(String name) {
		synchronized (this.watchedResourcesLock) {
			String[] results = new String[this.watchedResources.length + 1];
			for (int i = 0; i < this.watchedResources.length; i++) {
				results[i] = this.watchedResources[i];
			}
			results[this.watchedResources.length] = name;
			this.watchedResources = results;
		}
		fireContainerEvent("addWatchedResource", name);
	}

	public void addWelcomeFile(String name) {
		synchronized (this.welcomeFilesLock) {
			if (this.replaceWelcomeFiles) {
				fireContainerEvent("clearWelcomeFiles", null);
				this.welcomeFiles = new String[0];
				setReplaceWelcomeFiles(false);
			}
			String[] results = new String[this.welcomeFiles.length + 1];
			for (int i = 0; i < this.welcomeFiles.length; i++) {
				results[i] = this.welcomeFiles[i];
			}
			results[this.welcomeFiles.length] = name;
			this.welcomeFiles = results;
		}
		if (getState().equals(LifecycleState.STARTED)) {
			fireContainerEvent("addWelcomeFile", name);
		}
	}

	public void addWrapperLifecycle(String listener) {
		synchronized (this.wrapperLifecyclesLock) {
			String[] results = new String[this.wrapperLifecycles.length + 1];
			for (int i = 0; i < this.wrapperLifecycles.length; i++) {
				results[i] = this.wrapperLifecycles[i];
			}
			results[this.wrapperLifecycles.length] = listener;
			this.wrapperLifecycles = results;
		}
		fireContainerEvent("addWrapperLifecycle", listener);
	}

	public void addWrapperListener(String listener) {
		synchronized (this.wrapperListenersLock) {
			String[] results = new String[this.wrapperListeners.length + 1];
			for (int i = 0; i < this.wrapperListeners.length; i++) {
				results[i] = this.wrapperListeners[i];
			}
			results[this.wrapperListeners.length] = listener;
			this.wrapperListeners = results;
		}
		fireContainerEvent("addWrapperListener", listener);
	}

	public Wrapper createWrapper() {
		Wrapper wrapper = null;
		if (this.wrapperClass != null) {
			try {
				wrapper = (Wrapper) this.wrapperClass.newInstance();
			} catch (Throwable t) {
				ExceptionUtils.handleThrowable(t);
				log.error("createWrapper", t);
				return null;
			}
		} else {
			wrapper = new StandardWrapper();
		}
		synchronized (this.instanceListenersLock) {
			for (int i = 0; i < this.instanceListeners.length; i++) {
				try {
					Class<?> clazz = Class.forName(this.instanceListeners[i]);
					InstanceListener listener = (InstanceListener) clazz.newInstance();

					wrapper.addInstanceListener(listener);
				} catch (Throwable t) {
					ExceptionUtils.handleThrowable(t);
					log.error("createWrapper", t);
					return null;
				}
			}
		}
		synchronized (this.wrapperLifecyclesLock) {
			for (int i = 0; i < this.wrapperLifecycles.length; i++) {
				try {
					Class<?> clazz = Class.forName(this.wrapperLifecycles[i]);
					LifecycleListener listener = (LifecycleListener) clazz.newInstance();

					wrapper.addLifecycleListener(listener);
				} catch (Throwable t) {
					ExceptionUtils.handleThrowable(t);
					log.error("createWrapper", t);
					return null;
				}
			}
		}
		synchronized (this.wrapperListenersLock) {
			for (int i = 0; i < this.wrapperListeners.length; i++) {
				try {
					Class<?> clazz = Class.forName(this.wrapperListeners[i]);
					ContainerListener listener = (ContainerListener) clazz.newInstance();

					wrapper.addContainerListener(listener);
				} catch (Throwable t) {
					ExceptionUtils.handleThrowable(t);
					log.error("createWrapper", t);
					return null;
				}
			}
		}
		return wrapper;
	}

	public String[] findApplicationListeners() {
		ArrayList<String> list = new ArrayList(this.applicationListeners.length);
		for (ApplicationListener applicationListener : this.applicationListeners) {
			list.add(applicationListener.getClassName());
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	public SecurityConstraint[] findConstraints() {
		return this.constraints;
	}

	public ErrorPage findErrorPage(int errorCode) {
		if (errorCode == 200) {
			return this.okErrorPage;
		}
		return (ErrorPage) this.statusPages.get(Integer.valueOf(errorCode));
	}

	public ErrorPage[] findErrorPages() {
		synchronized (this.exceptionPages) {
			synchronized (this.statusPages) {
				ErrorPage[] results1 = new ErrorPage[this.exceptionPages.size()];
				results1 = (ErrorPage[]) this.exceptionPages.values().toArray(results1);
				ErrorPage[] results2 = new ErrorPage[this.statusPages.size()];
				results2 = (ErrorPage[]) this.statusPages.values().toArray(results2);
				ErrorPage[] results = new ErrorPage[results1.length + results2.length];
				for (int i = 0; i < results1.length; i++) {
					results[i] = results1[i];
				}
				for (int i = results1.length; i < results.length; i++) {
					results[i] = results2[(i - results1.length)];
				}
				return results;
			}
		}
	}

	public FilterDef[] findFilterDefs() {
		synchronized (this.filterDefs) {
			FilterDef[] results = new FilterDef[this.filterDefs.size()];
			return (FilterDef[]) this.filterDefs.values().toArray(results);
		}
	}

	public FilterMap[] findFilterMaps() {
		return this.filterMaps.asArray();
	}

	@Deprecated
	public org.apache.catalina.Context findMappingObject() {
		return (org.apache.catalina.Context) getMappingObject();
	}

	public MessageDestination[] findMessageDestinations() {
		synchronized (this.messageDestinations) {
			MessageDestination[] results = new MessageDestination[this.messageDestinations.size()];

			return (MessageDestination[]) this.messageDestinations.values().toArray(results);
		}
	}

	public MessageDestinationRef findMessageDestinationRef(String name) {
		return this.namingResources.findMessageDestinationRef(name);
	}

	public MessageDestinationRef[] findMessageDestinationRefs() {
		return this.namingResources.findMessageDestinationRefs();
	}

	public String findMimeMapping(String extension) {
		return (String) this.mimeMappings.get(extension);
	}

	public String[] findMimeMappings() {
		synchronized (this.mimeMappings) {
			String[] results = new String[this.mimeMappings.size()];
			return (String[]) this.mimeMappings.keySet().toArray(results);
		}
	}

	public String[] findParameters() {
		synchronized (this.parameters) {
			String[] results = new String[this.parameters.size()];
			return (String[]) this.parameters.keySet().toArray(results);
		}
	}

	public String findRoleMapping(String role) {
		String realRole = null;
		synchronized (this.roleMappings) {
			realRole = (String) this.roleMappings.get(role);
		}
		if (realRole != null) {
			return realRole;
		}
		return role;
	}

	public boolean findSecurityRole(String role) {
		synchronized (this.securityRolesLock) {
			for (int i = 0; i < this.securityRoles.length; i++) {
				if (role.equals(this.securityRoles[i])) {
					return true;
				}
			}
		}
		return false;
	}

	public String[] findServletMappings() {
		synchronized (this.servletMappingsLock) {
			String[] results = new String[this.servletMappings.size()];
			return (String[]) this.servletMappings.keySet().toArray(results);
		}
	}

	public String findStatusPage(int status) {
		ErrorPage errorPage = (ErrorPage) this.statusPages.get(Integer.valueOf(status));
		if (errorPage != null) {
			return errorPage.getLocation();
		}
		return null;
	}

	public int[] findStatusPages() {
		synchronized (this.statusPages) {
			int[] results = new int[this.statusPages.size()];
			Iterator<Integer> elements = this.statusPages.keySet().iterator();
			int i = 0;
			while (elements.hasNext()) {
				results[(i++)] = ((Integer) elements.next()).intValue();
			}
			return results;
		}
	}

	public boolean findWelcomeFile(String name) {
		synchronized (this.welcomeFilesLock) {
			for (int i = 0; i < this.welcomeFiles.length; i++) {
				if (name.equals(this.welcomeFiles[i])) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized void reload() {
		if (!getState().isAvailable()) {
			throw new IllegalStateException(sm.getString("standardContext.notStarted", new Object[] { getName() }));
		}
		if (log.isInfoEnabled()) {
			log.info(sm.getString("standardContext.reloadingStarted", new Object[] { getName() }));
		}
		setPaused(true);
		try {
			stop();
		} catch (LifecycleException e) {
			log.error(sm.getString("standardContext.stoppingContext", new Object[] { getName() }), e);
		}
		try {
			start();
		} catch (LifecycleException e) {
			log.error(sm.getString("standardContext.startingContext", new Object[] { getName() }), e);
		}
		setPaused(false);
		if (log.isInfoEnabled()) {
			log.info(sm.getString("standardContext.reloadingCompleted", new Object[] { getName() }));
		}
	}

	public void removeApplicationListener(String listener) {
		synchronized (this.applicationListenersLock) {
			int n = -1;
			for (int i = 0; i < this.applicationListeners.length; i++) {
				if (this.applicationListeners[i].getClassName().equals(listener)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			ApplicationListener[] results = new ApplicationListener[this.applicationListeners.length - 1];
			for (int i = 0; i < this.applicationListeners.length; i++) {
				if (i != n) {
					results[(j++)] = this.applicationListeners[i];
				}
			}
			this.applicationListeners = results;
		}
		fireContainerEvent("removeApplicationListener", listener);
	}

	public void removeApplicationParameter(String name) {
		synchronized (this.applicationParametersLock) {
			int n = -1;
			for (int i = 0; i < this.applicationParameters.length; i++) {
				if (name.equals(this.applicationParameters[i].getName())) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			ApplicationParameter[] results = new ApplicationParameter[this.applicationParameters.length - 1];
			for (int i = 0; i < this.applicationParameters.length; i++) {
				if (i != n) {
					results[(j++)] = this.applicationParameters[i];
				}
			}
			this.applicationParameters = results;
		}
		fireContainerEvent("removeApplicationParameter", name);
	}

	public void removeChild(Container child) {
		if (!(child instanceof Wrapper)) {
			throw new IllegalArgumentException(sm.getString("standardContext.notWrapper"));
		}
		super.removeChild(child);
	}

	public void removeConstraint(SecurityConstraint constraint) {
		synchronized (this.constraintsLock) {
			int n = -1;
			for (int i = 0; i < this.constraints.length; i++) {
				if (this.constraints[i].equals(constraint)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			SecurityConstraint[] results = new SecurityConstraint[this.constraints.length - 1];
			for (int i = 0; i < this.constraints.length; i++) {
				if (i != n) {
					results[(j++)] = this.constraints[i];
				}
			}
			this.constraints = results;
		}
		fireContainerEvent("removeConstraint", constraint);
	}

	public void removeErrorPage(ErrorPage errorPage) {
		String exceptionType = errorPage.getExceptionType();
		if (exceptionType != null) {
			synchronized (this.exceptionPages) {
				this.exceptionPages.remove(exceptionType);
			}
		} else {
			synchronized (this.statusPages) {
				if (errorPage.getErrorCode() == 200) {
					this.okErrorPage = null;
				}
				this.statusPages.remove(Integer.valueOf(errorPage.getErrorCode()));
			}
		}
		fireContainerEvent("removeErrorPage", errorPage);
	}

	public void removeFilterDef(FilterDef filterDef) {
		synchronized (this.filterDefs) {
			this.filterDefs.remove(filterDef.getFilterName());
		}
		fireContainerEvent("removeFilterDef", filterDef);
	}

	public void removeFilterMap(FilterMap filterMap) {
		this.filterMaps.remove(filterMap);

		fireContainerEvent("removeFilterMap", filterMap);
	}

	public void removeInstanceListener(String listener) {
		synchronized (this.instanceListenersLock) {
			int n = -1;
			for (int i = 0; i < this.instanceListeners.length; i++) {
				if (this.instanceListeners[i].equals(listener)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.instanceListeners.length - 1];
			for (int i = 0; i < this.instanceListeners.length; i++) {
				if (i != n) {
					results[(j++)] = this.instanceListeners[i];
				}
			}
			this.instanceListeners = results;
		}
		fireContainerEvent("removeInstanceListener", listener);
	}

	public void removeMessageDestination(String name) {
		synchronized (this.messageDestinations) {
			this.messageDestinations.remove(name);
		}
		fireContainerEvent("removeMessageDestination", name);
	}

	public void removeMessageDestinationRef(String name) {
		this.namingResources.removeMessageDestinationRef(name);
		fireContainerEvent("removeMessageDestinationRef", name);
	}

	public void removeMimeMapping(String extension) {
		synchronized (this.mimeMappings) {
			this.mimeMappings.remove(extension);
		}
		fireContainerEvent("removeMimeMapping", extension);
	}

	public void removeParameter(String name) {
		synchronized (this.parameters) {
			this.parameters.remove(name);
		}
		fireContainerEvent("removeParameter", name);
	}

	public void removeRoleMapping(String role) {
		synchronized (this.roleMappings) {
			this.roleMappings.remove(role);
		}
		fireContainerEvent("removeRoleMapping", role);
	}

	public void removeSecurityRole(String role) {
		synchronized (this.securityRolesLock) {
			int n = -1;
			for (int i = 0; i < this.securityRoles.length; i++) {
				if (role.equals(this.securityRoles[i])) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.securityRoles.length - 1];
			for (int i = 0; i < this.securityRoles.length; i++) {
				if (i != n) {
					results[(j++)] = this.securityRoles[i];
				}
			}
			this.securityRoles = results;
		}
		fireContainerEvent("removeSecurityRole", role);
	}

	public void removeServletMapping(String pattern) {
		String name = null;
		synchronized (this.servletMappingsLock) {
			name = (String) this.servletMappings.remove(pattern);
		}
		Wrapper wrapper = (Wrapper) findChild(name);
		if (wrapper != null) {
			wrapper.removeMapping(pattern);
		}
		this.mapper.removeWrapper(pattern);
		fireContainerEvent("removeServletMapping", pattern);
	}

	public void removeWatchedResource(String name) {
		synchronized (this.watchedResourcesLock) {
			int n = -1;
			for (int i = 0; i < this.watchedResources.length; i++) {
				if (this.watchedResources[i].equals(name)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.watchedResources.length - 1];
			for (int i = 0; i < this.watchedResources.length; i++) {
				if (i != n) {
					results[(j++)] = this.watchedResources[i];
				}
			}
			this.watchedResources = results;
		}
		fireContainerEvent("removeWatchedResource", name);
	}

	public void removeWelcomeFile(String name) {
		synchronized (this.welcomeFilesLock) {
			int n = -1;
			for (int i = 0; i < this.welcomeFiles.length; i++) {
				if (this.welcomeFiles[i].equals(name)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.welcomeFiles.length - 1];
			for (int i = 0; i < this.welcomeFiles.length; i++) {
				if (i != n) {
					results[(j++)] = this.welcomeFiles[i];
				}
			}
			this.welcomeFiles = results;
		}
		if (getState().equals(LifecycleState.STARTED)) {
			fireContainerEvent("removeWelcomeFile", name);
		}
	}

	public void removeWrapperLifecycle(String listener) {
		synchronized (this.wrapperLifecyclesLock) {
			int n = -1;
			for (int i = 0; i < this.wrapperLifecycles.length; i++) {
				if (this.wrapperLifecycles[i].equals(listener)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.wrapperLifecycles.length - 1];
			for (int i = 0; i < this.wrapperLifecycles.length; i++) {
				if (i != n) {
					results[(j++)] = this.wrapperLifecycles[i];
				}
			}
			this.wrapperLifecycles = results;
		}
		fireContainerEvent("removeWrapperLifecycle", listener);
	}

	public void removeWrapperListener(String listener) {
		synchronized (this.wrapperListenersLock) {
			int n = -1;
			for (int i = 0; i < this.wrapperListeners.length; i++) {
				if (this.wrapperListeners[i].equals(listener)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.wrapperListeners.length - 1];
			for (int i = 0; i < this.wrapperListeners.length; i++) {
				if (i != n) {
					results[(j++)] = this.wrapperListeners[i];
				}
			}
			this.wrapperListeners = results;
		}
		fireContainerEvent("removeWrapperListener", listener);
	}

	public long getProcessingTime() {
		long result = 0L;

		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				result += ((StandardWrapper) children[i]).getProcessingTime();
			}
		}
		return result;
	}

	public long getMaxTime() {
		long result = 0L;

		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				long time = ((StandardWrapper) children[i]).getMaxTime();
				if (time > result) {
					result = time;
				}
			}
		}
		return result;
	}

	public long getMinTime() {
		long result = -1L;

		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				long time = ((StandardWrapper) children[i]).getMinTime();
				if ((result < 0L) || (time < result)) {
					result = time;
				}
			}
		}
		return result;
	}

	public int getRequestCount() {
		int result = 0;

		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				result += ((StandardWrapper) children[i]).getRequestCount();
			}
		}
		return result;
	}

	public int getErrorCount() {
		int result = 0;

		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				result += ((StandardWrapper) children[i]).getErrorCount();
			}
		}
		return result;
	}

	public String getRealPath(String path) {
		if ((this.webappResources instanceof BaseDirContext)) {
			return ((BaseDirContext) this.webappResources).getRealPath(path);
		}
		return null;
	}

	public ServletRegistration.Dynamic dynamicServletAdded(Wrapper wrapper) {
		Servlet s = wrapper.getServlet();
		if ((s != null) && (this.createdServlets.contains(s))) {
			wrapper.setServletSecurityAnnotationScanRequired(true);
		}
		return new ApplicationServletRegistration(wrapper, this);
	}

	public void dynamicServletCreated(Servlet servlet) {
		this.createdServlets.add(servlet);
	}

	private static final class ContextFilterMaps {
		private final Object lock = new Object();
		private FilterMap[] array = new FilterMap[0];
		private int insertPoint = 0;

		private ContextFilterMaps() {
		}

		public FilterMap[] asArray() {
			synchronized (lock) {
				return array;
			}
		}

		public void add(FilterMap filterMap) {
			synchronized (this.lock) {
				FilterMap[] results = (FilterMap[]) Arrays.copyOf(this.array, this.array.length + 1);
				results[this.array.length] = filterMap;
				this.array = results;
			}
		}

		public void addBefore(FilterMap filterMap) {
			synchronized (this.lock) {
				FilterMap[] results = new FilterMap[this.array.length + 1];
				System.arraycopy(this.array, 0, results, 0, this.insertPoint);
				System.arraycopy(this.array, this.insertPoint, results, this.insertPoint + 1,
						this.array.length - this.insertPoint);

				results[this.insertPoint] = filterMap;
				this.array = results;
				this.insertPoint += 1;
			}
		}

		public void remove(FilterMap filterMap) {
			synchronized (this.lock) {
				int n = -1;
				for (int i = 0; i < this.array.length; i++) {
					if (this.array[i] == filterMap) {
						n = i;
						break;
					}
				}
				if (n < 0) {
					return;
				}
				FilterMap[] results = new FilterMap[this.array.length - 1];
				System.arraycopy(this.array, 0, results, 0, n);
				System.arraycopy(this.array, n + 1, results, n, this.array.length - 1 - n);

				this.array = results;
				if (n < this.insertPoint) {
					this.insertPoint -= 1;
				}
			}
		}
	}

	public boolean filterStart() {
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Starting filters");
		}
		boolean ok = true;
		synchronized (this.filterConfigs) {
			this.filterConfigs.clear();
			Iterator<String> names = this.filterDefs.keySet().iterator();
			while (names.hasNext()) {
				String name = (String) names.next();
				if (getLogger().isDebugEnabled()) {
					getLogger().debug(" Starting filter '" + name + "'");
				}
				ApplicationFilterConfig filterConfig = null;
				try {
					filterConfig = new ApplicationFilterConfig(this, (FilterDef) this.filterDefs.get(name));

					this.filterConfigs.put(name, filterConfig);
				} catch (Throwable t) {
					t = ExceptionUtils.unwrapInvocationTargetException(t);
					ExceptionUtils.handleThrowable(t);
					getLogger().error(sm.getString("standardContext.filterStart", new Object[] { name }), t);

					ok = false;
				}
			}
		}
		return ok;
	}

	public boolean filterStop() {
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Stopping filters");
		}
		synchronized (this.filterConfigs) {
			Iterator<String> names = this.filterConfigs.keySet().iterator();
			while (names.hasNext()) {
				String name = (String) names.next();
				if (getLogger().isDebugEnabled()) {
					getLogger().debug(" Stopping filter '" + name + "'");
				}
				ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) this.filterConfigs.get(name);
				filterConfig.release();
			}
			this.filterConfigs.clear();
		}
		return true;
	}

	public FilterConfig findFilterConfig(String name) {
		return (FilterConfig) this.filterConfigs.get(name);
	}

	public boolean listenerStart() {
		if (log.isDebugEnabled()) {
			log.debug("Configuring application event listeners");
		}
		ApplicationListener[] listeners = this.applicationListeners;
		Object[] results = new Object[listeners.length];
		boolean ok = true;
		for (int i = 0; i < results.length; i++) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug(" Configuring event listener class '" + listeners[i] + "'");
			}
			try {
				ApplicationListener listener = listeners[i];
				results[i] = this.instanceManager.newInstance(listener.getClassName());
				if (listener.isPluggabilityBlocked()) {
					this.noPluggabilityListeners.add(results[i]);
				}
			} catch (Throwable t) {
				t = ExceptionUtils.unwrapInvocationTargetException(t);
				ExceptionUtils.handleThrowable(t);
				getLogger().error(sm.getString("standardContext.applicationListener",
						new Object[] { listeners[i].getClassName() }), t);

				ok = false;
			}
		}
		if (!ok) {
			getLogger().error(sm.getString("standardContext.applicationSkipped"));
			return false;
		}
		ArrayList<Object> eventListeners = new ArrayList();
		ArrayList<Object> lifecycleListeners = new ArrayList();
		for (int i = 0; i < results.length; i++) {
			if (((results[i] instanceof ServletContextAttributeListener))
					|| ((results[i] instanceof ServletRequestAttributeListener))
					|| ((results[i] instanceof ServletRequestListener))
					|| ((results[i] instanceof HttpSessionAttributeListener))) {
				eventListeners.add(results[i]);
			}
			if (((results[i] instanceof ServletContextListener)) || ((results[i] instanceof HttpSessionListener))) {
				lifecycleListeners.add(results[i]);
			}
		}
		for (Object eventListener : getApplicationEventListeners()) {
			eventListeners.add(eventListener);
		}
		setApplicationEventListeners(eventListeners.toArray());
		for (Object lifecycleListener : getApplicationLifecycleListeners()) {
			lifecycleListeners.add(lifecycleListener);
			if ((lifecycleListener instanceof ServletContextListener)) {
				this.noPluggabilityListeners.add(lifecycleListener);
			}
		}
		setApplicationLifecycleListeners(lifecycleListeners.toArray());
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Sending application start events");
		}
		getServletContext();
		this.context.setNewServletContextListenerAllowed(false);

		Object[] instances = getApplicationLifecycleListeners();
		if ((instances == null) || (instances.length == 0)) {
			return ok;
		}
		ServletContextEvent event = new ServletContextEvent(getServletContext());
		ServletContextEvent tldEvent = null;
		if (this.noPluggabilityListeners.size() > 0) {
			this.noPluggabilityServletContext = new NoPluggabilityServletContext(getServletContext());
			tldEvent = new ServletContextEvent(this.noPluggabilityServletContext);
		}
		for (int i = 0; i < instances.length; i++) {
			if (instances[i] != null) {
				if ((instances[i] instanceof ServletContextListener)) {
					ServletContextListener listener = (ServletContextListener) instances[i];
					try {
						fireContainerEvent("beforeContextInitialized", listener);
						if (this.noPluggabilityListeners.contains(listener)) {
							listener.contextInitialized(tldEvent);
						} else {
							listener.contextInitialized(event);
						}
						fireContainerEvent("afterContextInitialized", listener);
					} catch (Throwable t) {
						ExceptionUtils.handleThrowable(t);
						fireContainerEvent("afterContextInitialized", listener);
						getLogger().error(sm.getString("standardContext.listenerStart",
								new Object[] { instances[i].getClass().getName() }), t);

						ok = false;
					}
				}
			}
		}
		return ok;
	}

	public boolean listenerStop() {
		if (log.isDebugEnabled()) {
			log.debug("Sending application stop events");
		}
		boolean ok = true;
		Object[] listeners = getApplicationLifecycleListeners();
		if (listeners != null) {
			ServletContextEvent event = new ServletContextEvent(getServletContext());
			ServletContextEvent tldEvent = null;
			if (this.noPluggabilityServletContext != null) {
				tldEvent = new ServletContextEvent(this.noPluggabilityServletContext);
			}
			for (int i = 0; i < listeners.length; i++) {
				int j = listeners.length - 1 - i;
				if (listeners[j] != null) {
					if ((listeners[j] instanceof ServletContextListener)) {
						ServletContextListener listener = (ServletContextListener) listeners[j];
						try {
							fireContainerEvent("beforeContextDestroyed", listener);
							if (this.noPluggabilityListeners.contains(listener)) {
								listener.contextDestroyed(tldEvent);
							} else {
								listener.contextDestroyed(event);
							}
							fireContainerEvent("afterContextDestroyed", listener);
						} catch (Throwable t) {
							ExceptionUtils.handleThrowable(t);
							fireContainerEvent("afterContextDestroyed", listener);
							getLogger().error(sm.getString("standardContext.listenerStop",
									new Object[] { listeners[j].getClass().getName() }), t);

							ok = false;
						}
					}
					try {
						getInstanceManager().destroyInstance(listeners[j]);
					} catch (Throwable t) {
						t = ExceptionUtils.unwrapInvocationTargetException(t);
						ExceptionUtils.handleThrowable(t);
						getLogger().error(sm.getString("standardContext.listenerStop",
								new Object[] { listeners[j].getClass().getName() }), t);

						ok = false;
					}
				}
			}
		}
		listeners = getApplicationEventListeners();
		if (listeners != null) {
			for (int i = 0; i < listeners.length; i++) {
				int j = listeners.length - 1 - i;
				if (listeners[j] != null) {
					try {
						getInstanceManager().destroyInstance(listeners[j]);
					} catch (Throwable t) {
						t = ExceptionUtils.unwrapInvocationTargetException(t);
						ExceptionUtils.handleThrowable(t);
						getLogger().error(sm.getString("standardContext.listenerStop",
								new Object[] { listeners[j].getClass().getName() }), t);

						ok = false;
					}
				}
			}
		}
		setApplicationEventListeners(null);
		setApplicationLifecycleListeners(null);

		this.noPluggabilityServletContext = null;
		this.noPluggabilityListeners.clear();

		return ok;
	}

	public boolean resourcesStart() {
		boolean ok = true;

		Hashtable<String, String> env = new Hashtable();
		if (getParent() != null) {
			env.put("host", getParent().getName());
		}
		env.put("context", getName());
		try {
			ProxyDirContext proxyDirContext = new ProxyDirContext(env, this.webappResources);
			if ((this.webappResources instanceof FileDirContext)) {
				this.filesystemBased = true;
				((FileDirContext) this.webappResources).setAllowLinking(isAllowLinking());
			}
			if ((this.webappResources instanceof BaseDirContext)) {
				((BaseDirContext) this.webappResources).setDocBase(getBasePath());
				((BaseDirContext) this.webappResources).setCached(isCachingAllowed());

				((BaseDirContext) this.webappResources).setCacheTTL(getCacheTTL());
				((BaseDirContext) this.webappResources).setCacheMaxSize(getCacheMaxSize());

				((BaseDirContext) this.webappResources).allocate();

				((BaseDirContext) this.webappResources).setAliases(getAliases());
				if ((this.effectiveMajorVersion >= 3) && (this.addWebinfClassesResources)) {
					try {
						DirContext webInfCtx = (DirContext) this.webappResources.lookup("/WEB-INF/classes");

						webInfCtx.lookup("META-INF/resources");
						((BaseDirContext) this.webappResources).addAltDirContext(webInfCtx);
					} catch (NamingException e) {
					}
				}
			}
			if (isCachingAllowed()) {
				String contextName = getName();
				if (!contextName.startsWith("/")) {
					contextName = "/" + contextName;
				}
				ObjectName resourcesName = new ObjectName(
						getDomain() + ":type=Cache,host=" + getHostname() + ",context=" + contextName);

				Registry.getRegistry(null, null).registerComponent(proxyDirContext.getCache(), resourcesName, null);
			}
			this.resources = proxyDirContext;
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			log.error(sm.getString("standardContext.resourcesStart"), t);
			ok = false;
		}
		return ok;
	}

	public boolean resourcesStop() {
		boolean ok = true;
		try {
			if (this.resources != null) {
				if ((this.resources instanceof Lifecycle)) {
					((Lifecycle) this.resources).stop();
				}
				if ((this.webappResources instanceof BaseDirContext)) {
					((BaseDirContext) this.webappResources).release();
				}
				if (isCachingAllowed()) {
					String contextName = getName();
					if (!contextName.startsWith("/")) {
						contextName = "/" + contextName;
					}
					ObjectName resourcesName = new ObjectName(
							getDomain() + ":type=Cache,host=" + getHostname() + ",context=" + contextName);

					Registry.getRegistry(null, null).unregisterComponent(resourcesName);
				}
			}
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			log.error(sm.getString("standardContext.resourcesStop"), t);
			ok = false;
		}
		this.resources = null;

		return ok;
	}

	public boolean loadOnStartup(Container[] children) {
		TreeMap<Integer, ArrayList<Wrapper>> map = new TreeMap();
		for (int i = 0; i < children.length; i++) {
			Wrapper wrapper = (Wrapper) children[i];
			int loadOnStartup = wrapper.getLoadOnStartup();
			if (loadOnStartup >= 0) {
				Integer key = Integer.valueOf(loadOnStartup);
				ArrayList<Wrapper> list = (ArrayList) map.get(key);
				if (list == null) {
					list = new ArrayList();
					map.put(key, list);
				}
				list.add(wrapper);
			}
		}
		for (ArrayList<Wrapper> list : map.values()) {
			for (Wrapper wrapper : list) {
				try {
					wrapper.load();
				} catch (ServletException e) {
					getLogger().error(sm.getString("standardContext.loadOnStartup.loadException",
							new Object[] { getName(), wrapper.getName() }), StandardWrapper.getRootCause(e));
					if (getComputedFailCtxIfServletStartFails()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	protected synchronized void startInternal() throws LifecycleException {
		if (log.isDebugEnabled()) {
			log.debug("Starting " + getBaseName());
		}
		if (getObjectName() != null) {
			Notification notification = new Notification("j2ee.state.starting", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
		setConfigured(false);
		boolean ok = true;
		if (this.namingResources != null) {
			this.namingResources.start();
		}
		if (this.webappResources == null) {
			if (log.isDebugEnabled()) {
				log.debug("Configuring default Resources");
			}
			try {
				if ((getDocBase() != null) && (getDocBase().endsWith(".war"))
						&& (!new File(getBasePath()).isDirectory())) {
					setResources(new WARDirContext());
				} else {
					setResources(new FileDirContext());
				}
			} catch (IllegalArgumentException e) {
				log.error("Error initializing resources: " + e.getMessage());
				ok = false;
			}
		}
		if ((ok) && (!resourcesStart())) {
			throw new LifecycleException("Error in resourceStart()");
		}
		if (getLoader() == null) {
			WebappLoader webappLoader = new WebappLoader(getParentClassLoader());

			webappLoader.setLoaderClass("com.taobao.tomcat.container.context.loader.AliWebappClassLoader");

			webappLoader.setDelegate(getDelegate());
			setLoader(webappLoader);
		}
		getCharsetMapper();

		postWorkDirectory();

		boolean dependencyCheck = true;
		try {
			dependencyCheck = ExtensionValidator.validateApplication(getResources(), this);
		} catch (IOException ioe) {
			log.error("Error in dependencyCheck", ioe);
			dependencyCheck = false;
		}
		if (!dependencyCheck) {
			ok = false;
		}
		String useNamingProperty = System.getProperty("catalina.useNaming");
		if ((useNamingProperty != null) && (useNamingProperty.equals("false"))) {
			this.useNaming = false;
		}
		if ((ok) && (isUseNaming()) && (getNamingContextListener() == null)) {
			NamingContextListener ncl = new NamingContextListener();
			ncl.setName(getNamingContextName());
			ncl.setExceptionOnFailedWrite(getJndiExceptionOnFailedWrite());
			addLifecycleListener(ncl);
			setNamingContextListener(ncl);
		}
		if (log.isDebugEnabled()) {
			log.debug("Processing standard container startup");
		}
		ClassLoader oldCCL = bindThread();
		try {
			if (ok) {
				if ((this.loader != null) && ((this.loader instanceof Lifecycle))) {
					((Lifecycle) this.loader).start();
				}
				if (this.pandoraManager == null) {
					this.pandoraManager = new PandoraManager();
					this.pandoraManager.setContext(this);
				}
				this.pandoraManager.start();

				unbindThread(oldCCL);
				oldCCL = bindThread();

				this.logger = null;
				getLogger();
				if ((this.cluster != null) && ((this.cluster instanceof Lifecycle))) {
					((Lifecycle) this.cluster).start();
				}
				Realm realm = getRealmInternal();
				if ((realm != null) && ((realm instanceof Lifecycle))) {
					((Lifecycle) realm).start();
				}
				if ((this.resources != null) && ((this.resources instanceof Lifecycle))) {
					((Lifecycle) this.resources).start();
				}
				fireLifecycleEvent("configure_start", null);
				for (Container child : findChildren()) {
					if (!child.getState().isAvailable()) {
						child.start();
					}
				}
				if ((this.pipeline instanceof Lifecycle)) {
					((Lifecycle) this.pipeline).start();
				}
				Manager contextManager = null;
				if (this.manager == null) {
					if (log.isDebugEnabled()) {
						log.debug(sm.getString("standardContext.cluster.noManager",
								new Object[] { Boolean.valueOf(getCluster() != null ? true : false),
										Boolean.valueOf(this.distributable) }));
					}
					if ((getCluster() != null) && (this.distributable)) {
						try {
							contextManager = getCluster().createManager(getName());
						} catch (Exception ex) {
							log.error("standardContext.clusterFail", ex);
							ok = false;
						}
					} else {
						contextManager = new StandardManager();
					}
				}
				if (contextManager != null) {
					if (log.isDebugEnabled()) {
						log.debug(sm.getString("standardContext.manager",
								new Object[] { contextManager.getClass().getName() }));
					}
					setManager(contextManager);
				}
				if ((this.manager != null) && (getCluster() != null) && (this.distributable)) {
					getCluster().registerManager(this.manager);
				}
			}
		} finally {
			unbindThread(oldCCL);
		}
		if (!getConfigured()) {
			log.error("Error getConfigured");
			ok = false;
		}
		if (ok) {
			getServletContext().setAttribute("org.apache.catalina.resources", getResources());
		}
		this.mapper.setContext(getPath(), this.welcomeFiles, this.resources);

		oldCCL = bindThread();
		if ((ok) && (getInstanceManager() == null)) {
			javax.naming.Context context = null;
			if ((isUseNaming()) && (getNamingContextListener() != null)) {
				context = getNamingContextListener().getEnvContext();
			}
			Map<String, Map<String, String>> injectionMap = buildInjectionMap(
					getIgnoreAnnotations() ? new NamingResources() : getNamingResources());

			setInstanceManager(new DefaultInstanceManager(context, injectionMap, this, getClass().getClassLoader()));

			getServletContext().setAttribute(InstanceManager.class.getName(), getInstanceManager());
		}
		try {
			if (ok) {
				getServletContext().setAttribute(JarScanner.class.getName(), getJarScanner());
			}
			mergeParameters();
			for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : this.initializers.entrySet()) {
				try {
					((ServletContainerInitializer) entry.getKey()).onStartup((Set) entry.getValue(),
							getServletContext());
				} catch (ServletException e) {
					log.error(sm.getString("standardContext.sciFail"), e);
					ok = false;
					break;
				}
			}
			if ((ok) && (!listenerStart())) {
				log.error("Error listenerStart");
				ok = false;
			}
			try {
				if ((this.manager != null) && ((this.manager instanceof Lifecycle))) {
					((Lifecycle) getManager()).start();
				}
			} catch (Exception e) {
				log.error("Error manager.start()", e);
				ok = false;
			}
			if ((ok) && (!filterStart())) {
				log.error("Error filterStart");
				ok = false;
			}
			if ((ok) && (!loadOnStartup(findChildren()))) {
				log.error("Error loadOnStartup");
				ok = false;
			}
			super.threadStart();
		} finally {
			unbindThread(oldCCL);
		}
		if (ok) {
			if (log.isDebugEnabled()) {
				log.debug("Starting completed");
			}
		} else {
			log.error(sm.getString("standardContext.startFailed", new Object[] { getName() }));
		}
		this.startTime = System.currentTimeMillis();
		if ((ok) && (getObjectName() != null)) {
			Notification notification = new Notification("j2ee.state.running", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
		if ((getLoader() instanceof WebappLoader)) {
			((WebappLoader) getLoader()).closeJARs(true);
		}
		if (!ok) {
			setState(LifecycleState.FAILED);
		} else {
			setState(LifecycleState.STARTING);
		}
	}

	private Map<String, Map<String, String>> buildInjectionMap(NamingResources namingResources) {
		Map<String, Map<String, String>> injectionMap = new HashMap();
		for (Injectable resource : namingResources.findLocalEjbs()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findEjbs()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findEnvironments()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findMessageDestinationRefs()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findResourceEnvRefs()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findResources()) {
			addInjectionTarget(resource, injectionMap);
		}
		for (Injectable resource : namingResources.findServices()) {
			addInjectionTarget(resource, injectionMap);
		}
		return injectionMap;
	}

	private void addInjectionTarget(Injectable resource, Map<String, Map<String, String>> injectionMap) {
		List<InjectionTarget> injectionTargets = resource.getInjectionTargets();
		String jndiName;
		if ((injectionTargets != null) && (injectionTargets.size() > 0)) {
			jndiName = resource.getName();
			for (InjectionTarget injectionTarget : injectionTargets) {
				String clazz = injectionTarget.getTargetClass();
				Map<String, String> injections = (Map) injectionMap.get(clazz);
				if (injections == null) {
					injections = new HashMap();
					injectionMap.put(clazz, injections);
				}
				injections.put(injectionTarget.getTargetName(), jndiName);
			}
		}
	}

	private void mergeParameters() {
		Map<String, String> mergedParams = new HashMap();

		String[] names = findParameters();
		for (int i = 0; i < names.length; i++) {
			mergedParams.put(names[i], findParameter(names[i]));
		}
		ApplicationParameter[] params = findApplicationParameters();
		for (int i = 0; i < params.length; i++) {
			if (params[i].getOverride()) {
				if (mergedParams.get(params[i].getName()) == null) {
					mergedParams.put(params[i].getName(), params[i].getValue());
				}
			} else {
				mergedParams.put(params[i].getName(), params[i].getValue());
			}
		}
		ServletContext sc = getServletContext();
		for (Map.Entry<String, String> entry : mergedParams.entrySet()) {
			sc.setInitParameter((String) entry.getKey(), (String) entry.getValue());
		}
	}

	protected synchronized void stopInternal() throws LifecycleException {
		if (getObjectName() != null) {
			Notification notification = new Notification("j2ee.state.stopping", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
		setState(LifecycleState.STOPPING);

		ClassLoader oldCCL = bindThread();
		try {
			Container[] children = findChildren();

			ClassLoader old = bindThread();
			try {
				threadStop();
				for (int i = 0; i < children.length; i++) {
					children[i].stop();
				}
				filterStop();
				if ((this.manager != null) && ((this.manager instanceof Lifecycle))
						&& (((Lifecycle) this.manager).getState().isAvailable())) {
					((Lifecycle) this.manager).stop();
				}
				listenerStop();
			} finally {
			}
			setCharsetMapper(null);
			if (log.isDebugEnabled()) {
				log.debug("Processing standard container shutdown");
			}
			if (this.namingResources != null) {
				this.namingResources.stop();
			}
			fireLifecycleEvent("configure_stop", null);
			if (((this.pipeline instanceof Lifecycle)) && (((Lifecycle) this.pipeline).getState().isAvailable())) {
				((Lifecycle) this.pipeline).stop();
			}
			if (this.context != null) {
				this.context.clearAttributes();
			}
			resourcesStop();

			Realm realm = getRealmInternal();
			if ((realm != null) && ((realm instanceof Lifecycle))) {
				((Lifecycle) realm).stop();
			}
			if ((this.cluster != null) && ((this.cluster instanceof Lifecycle))) {
				((Lifecycle) this.cluster).stop();
			}
			if (this.pandoraManager != null) {
				this.pandoraManager.stop();
			}
			if ((this.loader != null) && ((this.loader instanceof Lifecycle))) {
				((Lifecycle) this.loader).stop();
			}
		} finally {
			unbindThread(oldCCL);
		}
		if (getObjectName() != null) {
			Notification notification = new Notification("j2ee.state.stopped", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
		this.context = null;
		try {
			resetContext();
		} catch (Exception ex) {
			log.error("Error reseting context " + this + " " + ex, ex);
		}
		this.instanceManager = null;
		if (log.isDebugEnabled()) {
			log.debug("Stopping complete");
		}
	}

	protected void destroyInternal() throws LifecycleException {
		if (getObjectName() != null) {
			Notification notification = new Notification("j2ee.object.deleted", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
		if (this.namingResources != null) {
			this.namingResources.destroy();
		}
		synchronized (this.instanceListenersLock) {
			this.instanceListeners = new String[0];
		}
		super.destroyInternal();
		if (this.pandoraManager != null) {
			this.pandoraManager.destroy();
		}
	}

	private void resetContext() throws Exception {
		for (Container child : findChildren()) {
			removeChild(child);
		}
		this.startupTime = 0L;
		this.startTime = 0L;
		this.tldScanTime = 0L;

		this.distributable = false;

		this.applicationListeners = new ApplicationListener[0];
		this.applicationEventListenersObjects = new Object[0];
		this.applicationLifecycleListenersObjects = new Object[0];
		this.jspConfigDescriptor = new ApplicationJspConfigDescriptor();

		this.initializers.clear();

		this.createdServlets.clear();

		this.postConstructMethods.clear();
		this.preDestroyMethods.clear();
		if (log.isDebugEnabled()) {
			log.debug("resetContext " + getObjectName());
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (getParent() != null) {
			sb.append(getParent().toString());
			sb.append(".");
		}
		sb.append("StandardContext[");
		sb.append(getName());
		sb.append("]");
		return sb.toString();
	}

	protected String adjustURLPattern(String urlPattern) {
		if (urlPattern == null) {
			return urlPattern;
		}
		if ((urlPattern.startsWith("/")) || (urlPattern.startsWith("*."))) {
			return urlPattern;
		}
		if (!isServlet22()) {
			return urlPattern;
		}
		if (log.isDebugEnabled()) {
			log.debug(sm.getString("standardContext.urlPattern.patternWarning", new Object[] { urlPattern }));
		}
		return "/" + urlPattern;
	}

	public boolean isServlet22() {
		return "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN".equals(this.publicId);
	}

	public Set<String> addServletSecurity(ApplicationServletRegistration registration,
			ServletSecurityElement servletSecurityElement) {
		Set<String> conflicts = new HashSet();

		Collection<String> urlPatterns = registration.getMappings();
		for (String urlPattern : urlPatterns) {
			boolean foundConflict = false;

			SecurityConstraint[] securityConstraints = findConstraints();
			for (SecurityConstraint securityConstraint : securityConstraints) {
				SecurityCollection[] collections = securityConstraint.findCollections();
				for (SecurityCollection collection : collections) {
					if (collection.findPattern(urlPattern)) {
						if (collection.isFromDescriptor()) {
							foundConflict = true;
							conflicts.add(urlPattern);
						} else {
							removeConstraint(securityConstraint);
						}
					}
					if (foundConflict) {
						break;
					}
				}
				if (foundConflict) {
					break;
				}
			}
			if (!foundConflict) {
				SecurityConstraint[] newSecurityConstraints = SecurityConstraint
						.createConstraints(servletSecurityElement, urlPattern);
				for (SecurityConstraint securityConstraint : newSecurityConstraints) {
					addConstraint(securityConstraint);
				}
			}
		}
		return conflicts;
	}

	protected File engineBase() {
		String base = System.getProperty("catalina.base");
		if (base == null) {
			StandardEngine eng = (StandardEngine) getParent().getParent();
			base = eng.getBaseDir();
		}
		return new File(base);
	}

	protected ClassLoader bindThread() {
		ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
		if (getResources() == null) {
			return oldContextClassLoader;
		}
		if ((getLoader() != null) && (getLoader().getClassLoader() != null)) {
			Thread.currentThread().setContextClassLoader(getLoader().getClassLoader());
		}
		DirContextURLStreamHandler.bindThread(getResources());
		if (isUseNaming()) {
			try {
				ContextBindings.bindThread(this, this);
			} catch (NamingException e) {
			}
		}
		return oldContextClassLoader;
	}

	protected void unbindThread(ClassLoader oldContextClassLoader) {
		if (isUseNaming()) {
			ContextBindings.unbindThread(this, this);
		}
		DirContextURLStreamHandler.unbindThread();

		Thread.currentThread().setContextClassLoader(oldContextClassLoader);
	}

	protected String getBasePath() {
		String docBase = null;
		Container container = this;
		while ((container != null) && (!(container instanceof Host))) {
			container = container.getParent();
		}
		File file = new File(getDocBase());
		if (!file.isAbsolute()) {
			if (container == null) {
				docBase = new File(engineBase(), getDocBase()).getPath();
			} else {
				String appBase = ((Host) container).getAppBase();
				file = new File(appBase);
				if (!file.isAbsolute()) {
					file = new File(engineBase(), appBase);
				}
				docBase = new File(file, getDocBase()).getPath();
			}
		} else {
			docBase = file.getPath();
		}
		return docBase;
	}

	protected String getAppBase() {
		String appBase = null;
		Container container = this;
		while ((container != null) && (!(container instanceof Host))) {
			container = container.getParent();
		}
		if (container != null) {
			appBase = ((Host) container).getAppBase();
		}
		return appBase;
	}

	private String getNamingContextName() {
		if (this.namingContextName == null) {
			Container parent = getParent();
			if (parent == null) {
				this.namingContextName = getName();
			} else {
				Stack<String> stk = new Stack();
				StringBuilder buff = new StringBuilder();
				while (parent != null) {
					stk.push(parent.getName());
					parent = parent.getParent();
				}
				while (!stk.empty()) {
					buff.append("/" + (String) stk.pop());
				}
				buff.append(getName());
				this.namingContextName = buff.toString();
			}
		}
		return this.namingContextName;
	}

	public NamingContextListener getNamingContextListener() {
		return this.namingContextListener;
	}

	public void setNamingContextListener(NamingContextListener namingContextListener) {
		this.namingContextListener = namingContextListener;
	}

	public boolean getPaused() {
		return this.paused;
	}

	@Deprecated
	public String getHostname() {
		Container parentHost = getParent();
		if (parentHost != null) {
			this.hostName = parentHost.getName();
		}
		if ((this.hostName == null) || (this.hostName.length() < 1)) {
			this.hostName = "_";
		}
		return this.hostName;
	}

	public boolean fireRequestInitEvent(ServletRequest request) {
		Object[] instances = getApplicationEventListeners();
		if ((instances != null) && (instances.length > 0)) {
			ServletRequestEvent event = new ServletRequestEvent(getServletContext(), request);
			for (int i = 0; i < instances.length; i++) {
				if (instances[i] != null) {
					if ((instances[i] instanceof ServletRequestListener)) {
						ServletRequestListener listener = (ServletRequestListener) instances[i];
						try {
							listener.requestInitialized(event);
						} catch (Throwable t) {
							ExceptionUtils.handleThrowable(t);
							getLogger().error(sm.getString("standardContext.requestListener.requestInit",
									new Object[] { instances[i].getClass().getName() }), t);

							request.setAttribute("javax.servlet.error.exception", t);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public boolean fireRequestDestroyEvent(ServletRequest request) {
		Object[] instances = getApplicationEventListeners();
		if ((instances != null) && (instances.length > 0)) {
			ServletRequestEvent event = new ServletRequestEvent(getServletContext(), request);
			for (int i = 0; i < instances.length; i++) {
				int j = instances.length - 1 - i;
				if (instances[j] != null) {
					if ((instances[j] instanceof ServletRequestListener)) {
						ServletRequestListener listener = (ServletRequestListener) instances[j];
						try {
							listener.requestDestroyed(event);
						} catch (Throwable t) {
							ExceptionUtils.handleThrowable(t);
							getLogger().error(sm.getString("standardContext.requestListener.requestInit",
									new Object[] { instances[j].getClass().getName() }), t);

							request.setAttribute("javax.servlet.error.exception", t);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public void addPostConstructMethod(String clazz, String method) {
		if ((clazz == null) || (method == null)) {
			throw new IllegalArgumentException(sm.getString("standardContext.postconstruct.required"));
		}
		if (this.postConstructMethods.get(clazz) != null) {
			throw new IllegalArgumentException(
					sm.getString("standardContext.postconstruct.duplicate", new Object[] { clazz }));
		}
		this.postConstructMethods.put(clazz, method);
		fireContainerEvent("addPostConstructMethod", clazz);
	}

	public void removePostConstructMethod(String clazz) {
		this.postConstructMethods.remove(clazz);
		fireContainerEvent("removePostConstructMethod", clazz);
	}

	public void addPreDestroyMethod(String clazz, String method) {
		if ((clazz == null) || (method == null)) {
			throw new IllegalArgumentException(sm.getString("standardContext.predestroy.required"));
		}
		if (this.preDestroyMethods.get(clazz) != null) {
			throw new IllegalArgumentException(
					sm.getString("standardContext.predestroy.duplicate", new Object[] { clazz }));
		}
		this.preDestroyMethods.put(clazz, method);
		fireContainerEvent("addPreDestroyMethod", clazz);
	}

	public void removePreDestroyMethod(String clazz) {
		this.preDestroyMethods.remove(clazz);
		fireContainerEvent("removePreDestroyMethod", clazz);
	}

	public String findPostConstructMethod(String clazz) {
		return (String) this.postConstructMethods.get(clazz);
	}

	public String findPreDestroyMethod(String clazz) {
		return (String) this.preDestroyMethods.get(clazz);
	}

	public Map<String, String> findPostConstructMethods() {
		return this.postConstructMethods;
	}

	public Map<String, String> findPreDestroyMethods() {
		return this.preDestroyMethods;
	}

	private void postWorkDirectory() {
		String workDir = getWorkDir();
		if ((workDir == null) || (workDir.length() == 0)) {
			String hostName = null;
			String engineName = null;
			String hostWorkDir = null;
			Container parentHost = getParent();
			if (parentHost != null) {
				hostName = parentHost.getName();
				if ((parentHost instanceof StandardHost)) {
					hostWorkDir = ((StandardHost) parentHost).getWorkDir();
				}
				Container parentEngine = parentHost.getParent();
				if (parentEngine != null) {
					engineName = parentEngine.getName();
				}
			}
			if ((hostName == null) || (hostName.length() < 1)) {
				hostName = "_";
			}
			if ((engineName == null) || (engineName.length() < 1)) {
				engineName = "_";
			}
			String temp = getName();
			if (temp.startsWith("/")) {
				temp = temp.substring(1);
			}
			temp = temp.replace('/', '_');
			temp = temp.replace('\\', '_');
			if (temp.length() < 1) {
				temp = "_";
			}
			if (hostWorkDir != null) {
				workDir = hostWorkDir + File.separator + temp;
			} else {
				workDir = "work" + File.separator + engineName + File.separator + hostName + File.separator + temp;
			}
			setWorkDir(workDir);
		}
		File dir = new File(workDir);
		if (!dir.isAbsolute()) {
			File catalinaHome = engineBase();
			String catalinaHomePath = null;
			try {
				catalinaHomePath = catalinaHome.getCanonicalPath();
				dir = new File(catalinaHomePath, workDir);
			} catch (IOException e) {
				log.warn(sm.getString("standardContext.workCreateException",
						new Object[] { workDir, catalinaHomePath, getName() }), e);
			}
		}
		if ((!dir.mkdirs()) && (!dir.isDirectory())) {
			log.warn(sm.getString("standardContext.workCreateFail", new Object[] { dir, getName() }));
		}
		if (this.context == null) {
			getServletContext();
		}
		this.context.setAttribute("javax.servlet.context.tempdir", dir);
		this.context.setAttributeReadOnly("javax.servlet.context.tempdir");
	}

	private void setPaused(boolean paused) {
		this.paused = paused;
	}

	private boolean validateURLPattern(String urlPattern) {
		if (urlPattern == null) {
			return false;
		}
		if ((urlPattern.indexOf('\n') >= 0) || (urlPattern.indexOf('\r') >= 0)) {
			return false;
		}
		if (urlPattern.equals("")) {
			return true;
		}
		if (urlPattern.startsWith("*.")) {
			if (urlPattern.indexOf('/') < 0) {
				checkUnusualURLPattern(urlPattern);
				return true;
			}
			return false;
		}
		if ((urlPattern.startsWith("/")) && (urlPattern.indexOf("*.") < 0)) {
			checkUnusualURLPattern(urlPattern);
			return true;
		}
		return false;
	}

	private void checkUnusualURLPattern(String urlPattern) {
		if (log.isInfoEnabled()) {
			if (((urlPattern.endsWith("*"))
					&& ((urlPattern.length() < 2) || (urlPattern.charAt(urlPattern.length() - 2) != '/')))
					|| ((urlPattern.startsWith("*.")) && (urlPattern.length() > 2)
							&& (urlPattern.lastIndexOf('.') > 1))) {
				log.info("Suspicious url pattern: \"" + urlPattern + "\"" + " in context [" + getName() + "] - see"
						+ " sections 12.1 and 12.2 of the Servlet specification");
			}
		}
	}

	public String getDeploymentDescriptor() {
		InputStream stream = null;
		ServletContext servletContext = getServletContext();
		if (servletContext != null) {
			stream = servletContext.getResourceAsStream("/WEB-INF/web.xml");
		}
		if (stream == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(stream));
			String strRead = "";
			while (strRead != null) {
				sb.append(strRead);
				strRead = br.readLine();
			}
			return sb.toString();
		} catch (IOException e) {
			return "";
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	public String[] getServlets() {
		String[] result = null;

		Container[] children = findChildren();
		if (children != null) {
			result = new String[children.length];
			for (int i = 0; i < children.length; i++) {
				result[i] = children[i].getObjectName().toString();
			}
		}
		return result;
	}

	protected String getObjectNameKeyProperties() {
		StringBuilder keyProperties = new StringBuilder("j2eeType=WebModule,");

		keyProperties.append(getObjectKeyPropertiesNameOnly());
		keyProperties.append(",J2EEApplication=");
		keyProperties.append(getJ2EEApplication());
		keyProperties.append(",J2EEServer=");
		keyProperties.append(getJ2EEServer());

		return keyProperties.toString();
	}

	private String getObjectKeyPropertiesNameOnly() {
		StringBuilder result = new StringBuilder("name=//");
		String hostname = getParent().getName();
		if (hostname == null) {
			result.append("DEFAULT");
		} else {
			result.append(hostname);
		}
		String contextName = getName();
		if (!contextName.startsWith("/")) {
			result.append('/');
		}
		result.append(contextName);

		return result.toString();
	}

	protected void initInternal() throws LifecycleException {
		super.initInternal();
		if (this.processTlds) {
			addLifecycleListener(new TldConfig());
		}
		if (this.namingResources != null) {
			this.namingResources.init();
		}
		if (getObjectName() != null) {
			Notification notification = new Notification("j2ee.object.created", getObjectName(),
					this.sequenceNumber.getAndIncrement());

			this.broadcaster.sendNotification(notification);
		}
	}

	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object object)
			throws ListenerNotFoundException {
		this.broadcaster.removeNotificationListener(listener, filter, object);
	}

	public MBeanNotificationInfo[] getNotificationInfo() {
		if (this.notificationInfo == null) {
			this.notificationInfo = new MBeanNotificationInfo[] {
					new MBeanNotificationInfo(new String[] { "j2ee.object.created" }, Notification.class.getName(),
							"web application is created"),
					new MBeanNotificationInfo(new String[] { "j2ee.state.starting" }, Notification.class.getName(),
							"change web application is starting"),
					new MBeanNotificationInfo(new String[] { "j2ee.state.running" }, Notification.class.getName(),
							"web application is running"),
					new MBeanNotificationInfo(new String[] { "j2ee.state.stopping" }, Notification.class.getName(),
							"web application start to stopped"),
					new MBeanNotificationInfo(new String[] { "j2ee.object.stopped" }, Notification.class.getName(),
							"web application is stopped"),
					new MBeanNotificationInfo(new String[] { "j2ee.object.deleted" }, Notification.class.getName(),
							"web application is deleted") };
		}
		return this.notificationInfo;
	}

	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object object)
			throws IllegalArgumentException {
		this.broadcaster.addNotificationListener(listener, filter, object);
	}

	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		this.broadcaster.removeNotificationListener(listener);
	}

	@Deprecated
	public DirContext getStaticResources() {
		return getResources();
	}

	@Deprecated
	public DirContext findStaticResources() {
		return getResources();
	}

	public String[] getWelcomeFiles() {
		return findWelcomeFiles();
	}

	public boolean getXmlNamespaceAware() {
		return this.webXmlNamespaceAware;
	}

	public void setXmlNamespaceAware(boolean webXmlNamespaceAware) {
		this.webXmlNamespaceAware = webXmlNamespaceAware;
	}

	public void setXmlValidation(boolean webXmlValidation) {
		this.webXmlValidation = webXmlValidation;
	}

	public boolean getXmlValidation() {
		return this.webXmlValidation;
	}

	public boolean getTldNamespaceAware() {
		return true;
	}

	public void setXmlBlockExternal(boolean xmlBlockExternal) {
		this.xmlBlockExternal = xmlBlockExternal;
	}

	public boolean getXmlBlockExternal() {
		return this.xmlBlockExternal;
	}

	public void setTldValidation(boolean tldValidation) {
		this.tldValidation = tldValidation;
	}

	public boolean getTldValidation() {
		return this.tldValidation;
	}

	public void setProcessTlds(boolean newProcessTlds) {
		this.processTlds = newProcessTlds;
	}

	public boolean getProcessTlds() {
		return this.processTlds;
	}

	public boolean isStateManageable() {
		return true;
	}

	@Deprecated
	public void startRecursive() throws LifecycleException {
		start();
	}

	private String server = null;
	private String[] javaVMs = null;

	public String getServer() {
		return this.server;
	}

	public String setServer(String server) {
		return this.server = server;
	}

	public String[] getJavaVMs() {
		return this.javaVMs;
	}

	public String[] setJavaVMs(String[] javaVMs) {
		return this.javaVMs = javaVMs;
	}

	public long getStartTime() {
		return this.startTime;
	}

	@Deprecated
	public boolean isEventProvider() {
		return false;
	}

	@Deprecated
	public boolean isStatisticsProvider() {
		return false;
	}


	public ApplicationParameter[] findApplicationParameters() {
		  synchronized (applicationParametersLock) {
	            return (applicationParameters);
	        }
	}

	
	public ErrorPage findErrorPage(String exceptionType) {
		   synchronized (exceptionPages) {
	            return (exceptionPages.get(exceptionType));
	        }
	}

	
	public FilterDef findFilterDef(String filterName) {
		   synchronized (filterDefs) {
	            return (filterDefs.get(filterName));
	        }
	}

	
	public String[] findInstanceListeners() {
		   synchronized (instanceListenersLock) {
	            return (instanceListeners);
	        }
	}

	
	public MessageDestination findMessageDestination(String name) {

        synchronized (messageDestinations) {
            return (messageDestinations.get(name));
        }
	}

	
	public String findParameter(String name) {
	    synchronized (parameters) {
            return (parameters.get(name));
        }
	}

	
	public String[] findSecurityRoles() {
	    synchronized (securityRolesLock) {
            return (securityRoles);
        }
	}

	
	public String findServletMapping(String pattern) {
	    synchronized (servletMappingsLock) {
            return (servletMappings.get(pattern));
        }
	}

	
	public String[] findWatchedResources() {
		   synchronized (watchedResourcesLock) {
	            return watchedResources;
	        }
	}

	
	public String[] findWelcomeFiles() {
	    synchronized (welcomeFilesLock) {
            return (welcomeFiles);
        }
	}

	
	public String[] findWrapperLifecycles() {
	     synchronized (wrapperLifecyclesLock) {
	            return (wrapperLifecycles);
	        }

	}

	
	public String[] findWrapperListeners() {
	     synchronized (wrapperListenersLock) {
	            return (wrapperListeners);
	        }
	}

	public void setTldNamespaceAware(boolean tldNamespaceAware) {
	}

	private static class NoPluggabilityServletContext implements ServletContext {
		private final ServletContext sc;

		public NoPluggabilityServletContext(ServletContext sc) {
			this.sc = sc;
		}

		public String getContextPath() {
			return this.sc.getContextPath();
		}

		public ServletContext getContext(String uripath) {
			return this.sc.getContext(uripath);
		}

		public int getMajorVersion() {
			return this.sc.getMajorVersion();
		}

		public int getMinorVersion() {
			return this.sc.getMinorVersion();
		}

		public int getEffectiveMajorVersion() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public int getEffectiveMinorVersion() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public String getMimeType(String file) {
			return this.sc.getMimeType(file);
		}

		public Set<String> getResourcePaths(String path) {
			return this.sc.getResourcePaths(path);
		}

		public URL getResource(String path) throws MalformedURLException {
			return this.sc.getResource(path);
		}

		public InputStream getResourceAsStream(String path) {
			return this.sc.getResourceAsStream(path);
		}

		public RequestDispatcher getRequestDispatcher(String path) {
			return this.sc.getRequestDispatcher(path);
		}

		public RequestDispatcher getNamedDispatcher(String name) {
			return this.sc.getNamedDispatcher(name);
		}

		@Deprecated
		public Servlet getServlet(String name) throws ServletException {
			return this.sc.getServlet(name);
		}

		@Deprecated
		public Enumeration<Servlet> getServlets() {
			return this.sc.getServlets();
		}

		@Deprecated
		public Enumeration<String> getServletNames() {
			return this.sc.getServletNames();
		}

		public void log(String msg) {
			this.sc.log(msg);
		}

		@Deprecated
		public void log(Exception exception, String msg) {
			this.sc.log(exception, msg);
		}

		public void log(String message, Throwable throwable) {
			this.sc.log(message, throwable);
		}

		public String getRealPath(String path) {
			return this.sc.getRealPath(path);
		}

		public String getServerInfo() {
			return this.sc.getServerInfo();
		}

		public String getInitParameter(String name) {
			return this.sc.getInitParameter(name);
		}

		public Enumeration<String> getInitParameterNames() {
			return this.sc.getInitParameterNames();
		}

		public boolean setInitParameter(String name, String value) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public Object getAttribute(String name) {
			return this.sc.getAttribute(name);
		}

		public Enumeration<String> getAttributeNames() {
			return this.sc.getAttributeNames();
		}

		public void setAttribute(String name, Object object) {
			this.sc.setAttribute(name, object);
		}

		public void removeAttribute(String name) {
			this.sc.removeAttribute(name);
		}

		public String getServletContextName() {
			return this.sc.getServletContextName();
		}

		public ServletRegistration.Dynamic addServlet(String servletName, String className) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public ServletRegistration getServletRegistration(String servletName) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public Map<String, ? extends ServletRegistration> getServletRegistrations() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public FilterRegistration.Dynamic addFilter(String filterName, String className) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public FilterRegistration getFilterRegistration(String filterName) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public SessionCookieConfig getSessionCookieConfig() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public void addListener(String className) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public <T extends EventListener> void addListener(T t) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public void addListener(Class<? extends EventListener> listenerClass) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public <T extends EventListener> T createListener(Class<T> c) throws ServletException {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public JspConfigDescriptor getJspConfigDescriptor() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public ClassLoader getClassLoader() {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}

		public void declareRoles(String... roleNames) {
			throw new UnsupportedOperationException(
					ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
		}
	}
}

/*
 * Location:
 * D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\
 * apache\catalina\core\StandardContext.class Java compiler version: 6 (50.0)
 * JD-Core Version: 0.7.1
 */