package org.apache.catalina.core;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class StandardHost extends ContainerBase implements Host {
	private static final Log log = LogFactory.getLog(StandardHost.class);

	public StandardHost() {
		this.pipeline.setBasic(new StandardHostValve());
	}

	private String[] aliases = new String[0];
	private final Object aliasesLock = new Object();
	private String appBase = "webapps";
	private String xmlBase = null;
	private boolean autoDeploy = true;
	private String configClass = "org.apache.catalina.startup.ContextConfig";
	private String contextClass = "org.apache.catalina.core.StandardContext";
	private boolean deployOnStartup = true;
	private boolean deployXML = !Globals.IS_SECURITY_ENABLED;
	private boolean copyXML = false;
	private String errorReportValveClass = "org.apache.catalina.valves.ErrorReportValve";
	private static final String info = "org.apache.catalina.core.StandardHost/1.0";
	private boolean unpackWARs = true;
	private String workDir = null;
	private boolean createDirs = true;
	private Map<ClassLoader, String> childClassLoaders = new WeakHashMap();
	private Pattern deployIgnore = null;
	private boolean undeployOldVersions = false;
	private boolean failCtxIfServletStartFails = false;

	public boolean getUndeployOldVersions() {
		return this.undeployOldVersions;
	}

	public void setUndeployOldVersions(boolean undeployOldVersions) {
		this.undeployOldVersions = undeployOldVersions;
	}

	public ExecutorService getStartStopExecutor() {
		return this.startStopExecutor;
	}

	public String getAppBase() {
		return this.appBase;
	}

	public void setAppBase(String appBase) {
		String oldAppBase = this.appBase;
		this.appBase = appBase;
		this.support.firePropertyChange("appBase", oldAppBase, this.appBase);
	}

	public String getXmlBase() {
		return this.xmlBase;
	}

	public void setXmlBase(String xmlBase) {
		String oldXmlBase = this.xmlBase;
		this.xmlBase = xmlBase;
		this.support.firePropertyChange("xmlBase", oldXmlBase, this.xmlBase);
	}

	public boolean getCreateDirs() {
		return this.createDirs;
	}

	public void setCreateDirs(boolean createDirs) {
		this.createDirs = createDirs;
	}

	public boolean getAutoDeploy() {
		return this.autoDeploy;
	}

	public void setAutoDeploy(boolean autoDeploy) {
		boolean oldAutoDeploy = this.autoDeploy;
		this.autoDeploy = autoDeploy;
		this.support.firePropertyChange("autoDeploy", oldAutoDeploy, this.autoDeploy);
	}

	public String getConfigClass() {
		return this.configClass;
	}

	public void setConfigClass(String configClass) {
		String oldConfigClass = this.configClass;
		this.configClass = configClass;
		this.support.firePropertyChange("configClass", oldConfigClass, this.configClass);
	}

	public String getContextClass() {
		return this.contextClass;
	}

	public void setContextClass(String contextClass) {
		String oldContextClass = this.contextClass;
		this.contextClass = contextClass;
		this.support.firePropertyChange("contextClass", oldContextClass, this.contextClass);
	}

	public boolean getDeployOnStartup() {
		return this.deployOnStartup;
	}

	public void setDeployOnStartup(boolean deployOnStartup) {
		boolean oldDeployOnStartup = this.deployOnStartup;
		this.deployOnStartup = deployOnStartup;
		this.support.firePropertyChange("deployOnStartup", oldDeployOnStartup, this.deployOnStartup);
	}

	public boolean isDeployXML() {
		return this.deployXML;
	}

	public void setDeployXML(boolean deployXML) {
		this.deployXML = deployXML;
	}

	public boolean isCopyXML() {
		return this.copyXML;
	}

	public void setCopyXML(boolean copyXML) {
		this.copyXML = copyXML;
	}

	public String getErrorReportValveClass() {
		return this.errorReportValveClass;
	}

	public void setErrorReportValveClass(String errorReportValveClass) {
		String oldErrorReportValveClassClass = this.errorReportValveClass;
		this.errorReportValveClass = errorReportValveClass;
		this.support.firePropertyChange("errorReportValveClass", oldErrorReportValveClassClass,
				this.errorReportValveClass);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (name == null) {
			throw new IllegalArgumentException(sm.getString("standardHost.nullName"));
		}
		name = name.toLowerCase(Locale.ENGLISH);

		String oldName = this.name;
		this.name = name;
		this.support.firePropertyChange("name", oldName, this.name);
	}

	public boolean isUnpackWARs() {
		return this.unpackWARs;
	}

	public void setUnpackWARs(boolean unpackWARs) {
		this.unpackWARs = unpackWARs;
	}

	public String getWorkDir() {
		return this.workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public String getDeployIgnore() {
		if (this.deployIgnore == null) {
			return null;
		}
		return this.deployIgnore.toString();
	}

	public Pattern getDeployIgnorePattern() {
		return this.deployIgnore;
	}

	public void setDeployIgnore(String deployIgnore) {
		String oldDeployIgnore;

		if (this.deployIgnore == null) {
			oldDeployIgnore = null;
		} else {
			oldDeployIgnore = this.deployIgnore.toString();
		}
		if (deployIgnore == null) {
			this.deployIgnore = null;
		} else {
			this.deployIgnore = Pattern.compile(deployIgnore);
		}
		this.support.firePropertyChange("deployIgnore", oldDeployIgnore, deployIgnore);
	}

	public boolean isFailCtxIfServletStartFails() {
		return this.failCtxIfServletStartFails;
	}

	public void setFailCtxIfServletStartFails(boolean failCtxIfServletStartFails) {
		boolean oldFailCtxIfServletStartFails = this.failCtxIfServletStartFails;
		this.failCtxIfServletStartFails = failCtxIfServletStartFails;
		this.support.firePropertyChange("failCtxIfServletStartFails", oldFailCtxIfServletStartFails,
				failCtxIfServletStartFails);
	}

	public void addAlias(String alias) {
		alias = alias.toLowerCase(Locale.ENGLISH);
		synchronized (this.aliasesLock) {
			for (int i = 0; i < this.aliases.length; i++) {
				if (this.aliases[i].equals(alias)) {
					return;
				}
			}
			String[] newAliases = new String[this.aliases.length + 1];
			for (int i = 0; i < this.aliases.length; i++) {
				newAliases[i] = this.aliases[i];
			}
			newAliases[this.aliases.length] = alias;
			this.aliases = newAliases;
		}
		fireContainerEvent("addAlias", alias);
	}

	public void addChild(Container child) {
		child.addLifecycleListener(new MemoryLeakTrackingListener());
		if (!(child instanceof Context)) {
			throw new IllegalArgumentException(sm.getString("standardHost.notContext"));
		}
		super.addChild(child);
	}

	private class MemoryLeakTrackingListener implements LifecycleListener {
		private MemoryLeakTrackingListener() {
		}

		public void lifecycleEvent(LifecycleEvent event) {
			if ((event.getType().equals("after_start")) && ((event.getSource() instanceof Context))) {
				Context context = (Context) event.getSource();
				StandardHost.this.childClassLoaders.put(context.getLoader().getClassLoader(),
						context.getServletContext().getContextPath());
			}
		}
	}

	public String[] findReloadedContextMemoryLeaks() {
		System.gc();

		List<String> result = new ArrayList();
		for (Map.Entry<ClassLoader, String> entry : this.childClassLoaders.entrySet()) {
			ClassLoader cl = (ClassLoader) entry.getKey();
			if (((cl instanceof WebappClassLoader)) && (!((WebappClassLoader) cl).isStarted())) {
				result.add(entry.getValue());
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public String[] findAliases() {
		synchronized (aliasesLock) {
			return (this.aliases);
		}
	}

	public String getInfo() {
		return "org.apache.catalina.core.StandardHost/1.0";
	}

	public void removeAlias(String alias) {
		alias = alias.toLowerCase(Locale.ENGLISH);
		synchronized (this.aliasesLock) {
			int n = -1;
			for (int i = 0; i < this.aliases.length; i++) {
				if (this.aliases[i].equals(alias)) {
					n = i;
					break;
				}
			}
			if (n < 0) {
				return;
			}
			int j = 0;
			String[] results = new String[this.aliases.length - 1];
			for (int i = 0; i < this.aliases.length; i++) {
				if (i != n) {
					results[(j++)] = this.aliases[i];
				}
			}
			this.aliases = results;
		}
		fireContainerEvent("removeAlias", alias);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (getParent() != null) {
			sb.append(getParent().toString());
			sb.append(".");
		}
		sb.append("StandardHost[");
		sb.append(getName());
		sb.append("]");
		return sb.toString();
	}

	protected synchronized void startInternal() throws LifecycleException {
		String errorValve = getErrorReportValveClass();
		if ((errorValve != null) && (!errorValve.equals(""))) {
			try {
				boolean found = false;
				Valve[] valves = getPipeline().getValves();
				for (Valve valve : valves) {
					if (errorValve.equals(valve.getClass().getName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					Valve valve = (Valve) Class.forName(errorValve).newInstance();

					getPipeline().addValve(valve);
				}
			} catch (Throwable t) {
				ExceptionUtils.handleThrowable(t);
				log.error(sm.getString("standardHost.invalidErrorReportValveClass", new Object[] { errorValve }), t);
			}
		}
		super.startInternal();
	}

	public String[] getValveNames() throws Exception {
		Valve[] valves = getPipeline().getValves();
		String[] mbeanNames = new String[valves.length];
		for (int i = 0; i < valves.length; i++) {
			if ((valves[i] != null) && (((ValveBase) valves[i]).getObjectName() != null)) {
				mbeanNames[i] = ((ValveBase) valves[i]).getObjectName().toString();
			}
		}
		return mbeanNames;
	}

	public String[] getAliases() {
		synchronized (aliasesLock) {
			return aliases;
		}
	}

	protected String getObjectNameKeyProperties() {
		StringBuilder keyProperties = new StringBuilder("type=Host");
		keyProperties.append(MBeanUtils.getContainerKeyProperties(this));

		return keyProperties.toString();
	}
}

/*
 * Location:
 * D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\
 * apache\catalina\core\StandardHost.class Java compiler version: 6 (50.0)
 * JD-Core Version: 0.7.1
 */