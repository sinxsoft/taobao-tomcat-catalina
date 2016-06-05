package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class StandardManager extends ManagerBase {
	private final Log log = LogFactory.getLog(StandardManager.class);
	protected static final String info = "StandardManager/1.0";
	protected static final String name = "StandardManager";

	public StandardManager() {
	}

	private class PrivilegedDoLoad implements PrivilegedExceptionAction<Void> {
		PrivilegedDoLoad() {
		}

		public Void run() throws Exception {
			StandardManager.this.doLoad();
			return null;
		}
	}

	private class PrivilegedDoUnload implements PrivilegedExceptionAction<Void> {
		PrivilegedDoUnload() {
		}

		public Void run() throws Exception {
			StandardManager.this.doUnload();
			return null;
		}
	}

	protected String pathname = "SESSIONS.ser";

	public String getInfo() {
		return "StandardManager/1.0";
	}

	public String getName() {
		return "StandardManager";
	}

	public String getPathname() {
		return this.pathname;
	}

	public void setPathname(String pathname) {
		String oldPathname = this.pathname;
		this.pathname = pathname;
		this.support.firePropertyChange("pathname", oldPathname, this.pathname);
	}

	public void load() throws ClassNotFoundException, IOException {
		if (SecurityUtil.isPackageProtectionEnabled()) {
			try {
				AccessController.doPrivileged(new PrivilegedDoLoad());
			} catch (PrivilegedActionException ex) {
				Exception exception = ex.getException();
				if ((exception instanceof ClassNotFoundException)) {
					throw ((ClassNotFoundException) exception);
				}
				if ((exception instanceof IOException)) {
					throw ((IOException) exception);
				}
				if (this.log.isDebugEnabled()) {
					this.log.debug("Unreported exception in load() " + exception);
				}
			}
		} else {
			doLoad();
		}
	}

	protected void doLoad() throws ClassNotFoundException, IOException {
		if (this.log.isDebugEnabled()) {
			this.log.debug("Start: Loading persisted sessions");
		}
		this.sessions.clear();

		File file = file();
		if (file == null) {
			return;
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug(sm.getString("standardManager.loading", new Object[] { this.pathname }));
		}
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ObjectInputStream ois = null;
		Loader loader = null;
		ClassLoader classLoader = null;
		try {
			fis = new FileInputStream(file.getAbsolutePath());
			bis = new BufferedInputStream(fis);
			if (this.container != null) {
				loader = this.container.getLoader();
			}
			if (loader != null) {
				classLoader = loader.getClassLoader();
			}
			if (classLoader != null) {
				if (this.log.isDebugEnabled()) {
					this.log.debug("Creating custom object input stream for class loader ");
				}
				ois = new CustomObjectInputStream(bis, classLoader);
			} else {
				if (this.log.isDebugEnabled()) {
					this.log.debug("Creating standard object input stream");
				}
				ois = new ObjectInputStream(bis);
			}
		} catch (FileNotFoundException e) {
			if (this.log.isDebugEnabled()) {
				this.log.debug("No persisted data file found");
			}
			return;
		} catch (IOException e) {
			this.log.error(sm.getString("standardManager.loading.ioe", new Object[] { e }), e);
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException f) {
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException f) {
				}
			}
			throw e;
		}
		synchronized (this.sessions) {
			try {
				Integer count = (Integer) ois.readObject();
				int n = count.intValue();
				if (this.log.isDebugEnabled()) {
					this.log.debug("Loading " + n + " persisted sessions");
				}
				for (int i = 0; i < n; i++) {
					StandardSession session = getNewSession();
					session.readObjectData(ois);
					session.setManager(this);
					this.sessions.put(session.getIdInternal(), session);
					session.activate();
					if (!session.isValidInternal()) {
						session.setValid(true);
						session.expire();
					}
					this.sessionCounter += 1L;
				}
			} catch (ClassNotFoundException e) {
				this.log.error(sm.getString("standardManager.loading.cnfe", new Object[] { e }), e);
				try {
					ois.close();
				} catch (IOException f) {
				}
				throw e;
			} catch (IOException e) {
				this.log.error(sm.getString("standardManager.loading.ioe", new Object[] { e }), e);
				try {
					ois.close();
				} catch (IOException f) {
				}
				throw e;
			} finally {
				try {
					ois.close();
				} catch (IOException f) {
				}
				if (file.exists()) {
					file.delete();
				}
			}
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug("Finish: Loading persisted sessions");
		}
	}

	public void unload() throws IOException {
		if (SecurityUtil.isPackageProtectionEnabled()) {
			try {
				AccessController.doPrivileged(new PrivilegedDoUnload());
			} catch (PrivilegedActionException ex) {
				Exception exception = ex.getException();
				if ((exception instanceof IOException)) {
					throw ((IOException) exception);
				}
				if (this.log.isDebugEnabled()) {
					this.log.debug("Unreported exception in unLoad() " + exception);
				}
			}
		} else {
			doUnload();
		}
	}

	protected void doUnload() throws IOException {
		if (log.isDebugEnabled())
			log.debug(sm.getString("standardManager.unloading.debug"));

		if (sessions.isEmpty()) {
			log.debug(sm.getString("standardManager.unloading.nosessions"));
			return; // nothing to do
		}

		// Open an output stream to the specified pathname, if any
		File file = file();
		if (file == null) {
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug(sm.getString("standardManager.unloading", pathname));
		}
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ObjectOutputStream oos = null;
		boolean error = false;
		try {
			fos = new FileOutputStream(file.getAbsolutePath());
			bos = new BufferedOutputStream(fos);
			oos = new ObjectOutputStream(bos);
		} catch (IOException e) {
			error = true;
			log.error(sm.getString("standardManager.unloading.ioe", e), e);
			throw e;
		} finally {
			if (error) {
				if (oos != null) {
					try {
						oos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
			}
		}

		// Write the number of active sessions, followed by the details
		ArrayList<StandardSession> list = new ArrayList<StandardSession>();
		synchronized (sessions) {
			if (log.isDebugEnabled()) {
				log.debug("Unloading " + sessions.size() + " sessions");
			}
			try {
				oos.writeObject(Integer.valueOf(sessions.size()));
				Iterator<Session> elements = sessions.values().iterator();
				while (elements.hasNext()) {
					StandardSession session = (StandardSession) elements.next();
					list.add(session);
					session.passivate();
					session.writeObjectData(oos);
				}
			} catch (IOException e) {
				log.error(sm.getString("standardManager.unloading.ioe", e), e);
				try {
					oos.close();
				} catch (IOException f) {
					// Ignore
				}
				throw e;
			}
		}

		// Flush and close the output stream
		try {
			oos.flush();
		} finally {
			try {
				oos.close();
			} catch (IOException f) {
				// Ignore
			}
		}

		// Expire all the sessions we just wrote
		if (log.isDebugEnabled()) {
			log.debug("Expiring " + list.size() + " persisted sessions");
		}
		Iterator<StandardSession> expires = list.iterator();
		while (expires.hasNext()) {
			StandardSession session = expires.next();
			try {
				session.expire(false);
			} catch (Throwable t) {
				ExceptionUtils.handleThrowable(t);
			} finally {
				session.recycle();
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Unloading complete");
		}
	}

	protected synchronized void startInternal() throws LifecycleException {
		super.startInternal();
		try {
			load();
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			this.log.error(sm.getString("standardManager.managerLoad"), t);
		}
		setState(LifecycleState.STARTING);
	}

	protected synchronized void stopInternal() throws LifecycleException {
		if (this.log.isDebugEnabled()) {
			this.log.debug("Stopping");
		}
		setState(LifecycleState.STOPPING);
		try {
			unload();
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			this.log.error(sm.getString("standardManager.managerUnload"), t);
		}
		Session[] sessions = findSessions();
		for (int i = 0; i < sessions.length; i++) {
			Session session = sessions[i];
			try {
				if (session.isValid()) {
					session.expire();
				}
			} catch (Throwable t) {
				ExceptionUtils.handleThrowable(t);
			} finally {
				session.recycle();
			}
		}
		super.stopInternal();
	}

	protected File file() {
		if ((this.pathname == null) || (this.pathname.length() == 0)) {
			return null;
		}
		File file = new File(this.pathname);
		if ((!file.isAbsolute()) && ((this.container instanceof Context))) {
			ServletContext servletContext = ((Context) this.container).getServletContext();

			File tempdir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			if (tempdir != null) {
				file = new File(tempdir, this.pathname);
			}
		}
		return file;
	}
}

/*
 * Location:
 * D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\
 * apache\catalina\session\StandardManager.class Java compiler version: 6 (50.0)
 * JD-Core Version: 0.7.1
 */