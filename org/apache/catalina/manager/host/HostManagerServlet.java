package org.apache.catalina.manager.host;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.management.MBeanServer;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.HostConfig;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

public class HostManagerServlet extends HttpServlet implements ContainerServlet {
	private static final long serialVersionUID = 1L;
	protected transient Context context = null;
	protected int debug = 1;
	protected transient Host installedHost = null;
	protected transient Engine engine = null;
	protected transient MBeanServer mBeanServer = null;
	protected static final StringManager sm = StringManager.getManager("org.apache.catalina.manager.host");
	protected transient Wrapper wrapper = null;

	public HostManagerServlet() {
	}

	public Wrapper getWrapper() {
		return this.wrapper;
	}

	public void setWrapper(Wrapper wrapper) {
		this.wrapper = wrapper;
		if (wrapper == null) {
			this.context = null;
			this.installedHost = null;
			this.engine = null;
		} else {
			this.context = ((Context) wrapper.getParent());
			this.installedHost = ((Host) this.context.getParent());
			this.engine = ((Engine) this.installedHost.getParent());
		}
		this.mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
	}

	public void destroy() {
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		StringManager smClient = StringManager.getManager("org.apache.catalina.manager.host", request.getLocales());

		String command = request.getPathInfo();
		if (command == null) {
			command = request.getServletPath();
		}
		String name = request.getParameter("name");

		response.setContentType("text/plain; charset=utf-8");
		PrintWriter writer = response.getWriter();
		if (command == null) {
			writer.println(sm.getString("hostManagerServlet.noCommand"));
		} else if (command.equals("/add")) {
			add(request, writer, name, false, smClient);
		} else if (command.equals("/remove")) {
			remove(writer, name, smClient);
		} else if (command.equals("/list")) {
			list(writer, smClient);
		} else if (command.equals("/start")) {
			start(writer, name, smClient);
		} else if (command.equals("/stop")) {
			stop(writer, name, smClient);
		} else {
			writer.println(sm.getString("hostManagerServlet.unknownCommand", new Object[] { command }));
		}
		writer.flush();
		writer.close();
	}

	protected void add(HttpServletRequest request, PrintWriter writer, String name, boolean htmlMode,
			StringManager smClient) {
		String aliases = request.getParameter("aliases");
		String appBase = request.getParameter("appBase");
		boolean manager = booleanParameter(request, "manager", false, htmlMode);
		boolean autoDeploy = booleanParameter(request, "autoDeploy", true, htmlMode);
		boolean deployOnStartup = booleanParameter(request, "deployOnStartup", true, htmlMode);
		boolean deployXML = booleanParameter(request, "deployXML", true, htmlMode);
		boolean unpackWARs = booleanParameter(request, "unpackWARs", true, htmlMode);
		boolean copyXML = booleanParameter(request, "copyXML", false, htmlMode);
		add(writer, name, aliases, appBase, manager, autoDeploy, deployOnStartup, deployXML, unpackWARs, copyXML,
				smClient);
	}

	protected boolean booleanParameter(HttpServletRequest request, String parameter, boolean theDefault,
			boolean htmlMode) {
		String value = request.getParameter(parameter);
		boolean booleanValue = theDefault;
		if (value != null) {
			if (htmlMode) {
				if (value.equals("on")) {
					booleanValue = true;
				}
			} else if (theDefault) {
				if (value.equals("false")) {
					booleanValue = false;
				}
			} else if (value.equals("true")) {
				booleanValue = true;
			}
		} else if (htmlMode) {
			booleanValue = false;
		}
		return booleanValue;
	}

	public void init() throws ServletException {
		if ((this.wrapper == null) || (this.context == null)) {
			throw new UnavailableException(sm.getString("hostManagerServlet.noWrapper"));
		}
		String value = null;
		try {
			value = getServletConfig().getInitParameter("debug");
			this.debug = Integer.parseInt(value);
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
		}
	}

	protected synchronized void add(PrintWriter writer, String name, String aliases, String appBase, boolean manager,
			boolean autoDeploy, boolean deployOnStartup, boolean deployXML, boolean unpackWARs, boolean copyXML,
			StringManager smClient) {
		StandardHost host = null;
		if (this.debug >= 1) {
			log(sm.getString("hostManagerServlet.add", new Object[] { name }));
		}
		if ((name == null) || (name.length() == 0)) {
			writer.println(smClient.getString("hostManagerServlet.invalidHostName", new Object[] { name }));

			return;
		}
		if (this.engine.findChild(name) != null) {
			writer.println(smClient.getString("hostManagerServlet.alreadyHost", new Object[] { name }));

			return;
		}
		File appBaseFile = null;
		File file = null;
		String applicationBase = appBase;
		if ((applicationBase == null) || (applicationBase.length() == 0)) {
			applicationBase = name;
		}
		file = new File(applicationBase);
		if (!file.isAbsolute()) {
			file = new File(System.getProperty("catalina.base"), file.getPath());
		}
		try {
			appBaseFile = file.getCanonicalFile();
		} catch (IOException e) {
			appBaseFile = file;
		}
		if ((!appBaseFile.mkdirs()) && (!appBaseFile.isDirectory())) {
			writer.println(smClient.getString("hostManagerServlet.appBaseCreateFail",
					new Object[] { appBaseFile.toString(), name }));

			return;
		}
		File configBaseFile = getConfigBase(name);
		if (manager) {
			if (configBaseFile == null) {
				writer.println(smClient.getString("hostManagerServlet.configBaseCreateFail", new Object[] { name }));

				return;
			}
			InputStream is = null;
			OutputStream os = null;
			try {
				is = getServletContext().getResourceAsStream("/manager.xml");
				os = new FileOutputStream(new File(configBaseFile, "manager.xml"));
				byte[] buffer = new byte['Ȁ'];
				int len = buffer.length;
				for (;;) {
					len = is.read(buffer);
					if (len == -1) {
						break;
					}
					os.write(buffer, 0, len);
				}
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
					}
				}
				host = new StandardHost();
			} catch (IOException e) {
				writer.println(smClient.getString("hostManagerServlet.managerXml"));
				return;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
					}
				}
			}
		}
		host.setAppBase(applicationBase);
		host.setName(name);

		host.addLifecycleListener(new HostConfig());
		if ((aliases != null) && (!"".equals(aliases))) {
			StringTokenizer tok = new StringTokenizer(aliases, ", ");
			while (tok.hasMoreTokens()) {
				host.addAlias(tok.nextToken());
			}
		}
		host.setAutoDeploy(autoDeploy);
		host.setDeployOnStartup(deployOnStartup);
		host.setDeployXML(deployXML);
		host.setUnpackWARs(unpackWARs);
		host.setCopyXML(copyXML);
		try {
			this.engine.addChild(host);
		} catch (Exception e) {
			writer.println(smClient.getString("hostManagerServlet.exception", new Object[] { e.toString() }));

			return;
		}
		host = (StandardHost) this.engine.findChild(name);
		if (host != null) {
			writer.println(smClient.getString("hostManagerServlet.add", new Object[] { name }));
		} else {
			writer.println(smClient.getString("hostManagerServlet.addFailed", new Object[] { name }));
		}
	}

	protected synchronized void remove(PrintWriter writer, String name, StringManager smClient) {
		if (this.debug >= 1) {
			log(sm.getString("hostManagerServlet.remove", new Object[] { name }));
		}
		if ((name == null) || (name.length() == 0)) {
			writer.println(smClient.getString("hostManagerServlet.invalidHostName", new Object[] { name }));

			return;
		}
		if (this.engine.findChild(name) == null) {
			writer.println(smClient.getString("hostManagerServlet.noHost", new Object[] { name }));

			return;
		}
		if (this.engine.findChild(name) == this.installedHost) {
			writer.println(smClient.getString("hostManagerServlet.cannotRemoveOwnHost", new Object[] { name }));

			return;
		}
		try {
			Container child = this.engine.findChild(name);
			this.engine.removeChild(child);
			if ((child instanceof ContainerBase)) {
				((ContainerBase) child).destroy();
			}
		} catch (Exception e) {
			writer.println(smClient.getString("hostManagerServlet.exception", new Object[] { e.toString() }));

			return;
		}
		Host host = (StandardHost) this.engine.findChild(name);
		if (host == null) {
			writer.println(smClient.getString("hostManagerServlet.remove", new Object[] { name }));
		} else {
			writer.println(smClient.getString("hostManagerServlet.removeFailed", new Object[] { name }));
		}
	}

	protected void list(PrintWriter writer, StringManager smClient) {
		if (this.debug >= 1) {
			log(sm.getString("hostManagerServlet.list", new Object[] { this.engine.getName() }));
		}
		writer.println(smClient.getString("hostManagerServlet.listed", new Object[] { this.engine.getName() }));

		Container[] hosts = this.engine.findChildren();
		for (int i = 0; i < hosts.length; i++) {
			Host host = (Host) hosts[i];
			String name = host.getName();
			String[] aliases = host.findAliases();
			StringBuilder buf = new StringBuilder();
			if (aliases.length > 0) {
				buf.append(aliases[0]);
				for (int j = 1; j < aliases.length; j++) {
					buf.append(',').append(aliases[j]);
				}
			}
			writer.println(smClient.getString("hostManagerServlet.listitem", new Object[] { name, buf.toString() }));
		}
	}

	protected void start(PrintWriter writer, String name, StringManager smClient) {
		if (this.debug >= 1) {
			log(sm.getString("hostManagerServlet.start", new Object[] { name }));
		}
		if ((name == null) || (name.length() == 0)) {
			writer.println(smClient.getString("hostManagerServlet.invalidHostName", new Object[] { name }));

			return;
		}
		Container host = this.engine.findChild(name);
		if (host == null) {
			writer.println(smClient.getString("hostManagerServlet.noHost", new Object[] { name }));

			return;
		}
		if (host == this.installedHost) {
			writer.println(smClient.getString("hostManagerServlet.cannotStartOwnHost", new Object[] { name }));

			return;
		}
		if (host.getState().isAvailable()) {
			writer.println(smClient.getString("hostManagerServlet.alreadyStarted", new Object[] { name }));

			return;
		}
		try {
			host.start();
			writer.println(smClient.getString("hostManagerServlet.started", new Object[] { name }));
		} catch (Exception e) {
			getServletContext().log(sm.getString("hostManagerServlet.startFailed", new Object[] { name }), e);

			writer.println(smClient.getString("hostManagerServlet.startFailed", new Object[] { name }));

			writer.println(smClient.getString("hostManagerServlet.exception", new Object[] { e.toString() }));

			return;
		}
	}

	protected void stop(PrintWriter writer, String name, StringManager smClient) {
		if (this.debug >= 1) {
			log(sm.getString("hostManagerServlet.stop", new Object[] { name }));
		}
		if ((name == null) || (name.length() == 0)) {
			writer.println(smClient.getString("hostManagerServlet.invalidHostName", new Object[] { name }));

			return;
		}
		Container host = this.engine.findChild(name);
		if (host == null) {
			writer.println(smClient.getString("hostManagerServlet.noHost", new Object[] { name }));

			return;
		}
		if (host == this.installedHost) {
			writer.println(smClient.getString("hostManagerServlet.cannotStopOwnHost", new Object[] { name }));

			return;
		}
		if (!host.getState().isAvailable()) {
			writer.println(smClient.getString("hostManagerServlet.alreadyStopped", new Object[] { name }));

			return;
		}
		try {
			host.stop();
			writer.println(smClient.getString("hostManagerServlet.stopped", new Object[] { name }));
		} catch (Exception e) {
			getServletContext().log(sm.getString("hostManagerServlet.stopFailed", new Object[] { name }), e);

			writer.println(smClient.getString("hostManagerServlet.stopFailed", new Object[] { name }));

			writer.println(smClient.getString("hostManagerServlet.exception", new Object[] { e.toString() }));

			return;
		}
	}

	protected File getConfigBase(String hostName) {
		File configBase = new File(System.getProperty("catalina.base"), "conf");
		if (!configBase.exists()) {
			return null;
		}
		if (this.engine != null) {
			configBase = new File(configBase, this.engine.getName());
		}
		if (this.installedHost != null) {
			configBase = new File(configBase, hostName);
		}
		if ((!configBase.mkdirs()) && (!configBase.isDirectory())) {
			return null;
		}
		return configBase;
	}

	@Deprecated
	protected StringManager getStringManager(HttpServletRequest req) {
		Enumeration<Locale> requestedLocales = req.getLocales();
		while (requestedLocales.hasMoreElements()) {
			Locale locale = (Locale) requestedLocales.nextElement();
			StringManager result = StringManager.getManager("org.apache.catalina.manager.host", locale);
			if (result.getLocale().equals(locale)) {
				return result;
			}
		}
		return sm;
	}
}

/*
 * Location:
 * D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\
 * apache\catalina\manager\host\HostManagerServlet.class Java compiler version:
 * 6 (50.0) JD-Core Version: 0.7.1
 */