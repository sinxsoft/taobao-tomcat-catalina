package org.apache.naming.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import org.apache.naming.NamingEntry;

public class VirtualDirContext extends FileDirContext {
	private String extraResourcePaths = "";
	private Map<String, List<String>> mappedResourcePaths;

	public VirtualDirContext() {
	}

	public void setExtraResourcePaths(String path) {
		this.extraResourcePaths = path;
	}

	public void allocate() {
		super.allocate();

		this.mappedResourcePaths = new HashMap();
		StringTokenizer tkn = new StringTokenizer(this.extraResourcePaths, ",");
		while (tkn.hasMoreTokens()) {
			String resSpec = tkn.nextToken();
			if (resSpec.length() > 0) {
				int idx = resSpec.indexOf('=');
				String path;

				if (idx <= 0) {
					path = "";
				} else {
					if (resSpec.startsWith("/=")) {
						resSpec = resSpec.substring(1);
						idx--;
					}
					path = resSpec.substring(0, idx);
				}
				String dir = resSpec.substring(idx + 1);
				List<String> resourcePaths = (List) this.mappedResourcePaths.get(path);
				if (resourcePaths == null) {
					resourcePaths = new ArrayList();
					this.mappedResourcePaths.put(path, resourcePaths);
				}
				resourcePaths.add(dir);
			}
		}
		if (this.mappedResourcePaths.isEmpty()) {
			this.mappedResourcePaths = null;
		}
	}

	public void release() {
		this.mappedResourcePaths = null;

		super.release();
	}

	public Attributes getAttributes(String name) throws NamingException {
		try {
			return super.getAttributes(name);
		} catch (NamingException exc) {
			NamingException initialException = exc;
			if (this.mappedResourcePaths != null) {
				for (Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
					String path = (String) mapping.getKey();
					List<String> dirList = (List) mapping.getValue();
					String resourcesDir = (String) dirList.get(0);
					if (name.equals(path)) {
						File f = new File(resourcesDir);
						if ((f.exists()) && (f.canRead())) {
							return new FileDirContext.FileResourceAttributes(f);
						}
					}
					path = path + "/";
					if (name.startsWith(path)) {
						String res = name.substring(path.length());
						File f = new File(resourcesDir + "/" + res);
						if ((f.exists()) && (f.canRead())) {
							return new FileDirContext.FileResourceAttributes(f);
						}
					}
				}
			}
			throw initialException;
		}
	}

	protected File file(String name) {
		File file = super.file(name);
		if ((file != null) || (this.mappedResourcePaths == null)) {
			return file;
		}
		if ((name.length() > 0) && (name.charAt(0) != '/')) {
			name = "/" + name;
		}
		for (Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
			String path = (String) mapping.getKey();
			List<String> dirList = (List) mapping.getValue();
			if (name.equals(path)) {
				for (String resourcesDir : dirList) {
					file = new File(resourcesDir);
					if ((file.exists()) && (file.canRead())) {
						return file;
					}
				}
			}
			if (name.startsWith(path + "/")) {
				String res;
				res = name.substring(path.length());
				for (String resourcesDir : dirList) {
					file = new File(resourcesDir, res);
					if ((file.exists()) && (file.canRead())) {
						return file;
					}
				}
			}
		}

		return null;
	}

	protected List<NamingEntry> list(File file) {
		List<NamingEntry> entries = super.list(file);
		Set<String> entryNames;
		String relPath;
		String fsRelPath;
		String res;
		if ((this.mappedResourcePaths != null) && (!this.mappedResourcePaths.isEmpty())) {
			entryNames = new HashSet(entries.size());
			for (NamingEntry entry : entries) {
				entryNames.add(entry.name);
			}
			String absPath = file.getAbsolutePath();
			if (absPath.startsWith(getDocBase() + File.separator)) {
				relPath = absPath.substring(getDocBase().length());
				fsRelPath = relPath.replace(File.separatorChar, '/');
				for (Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
					String path = (String) mapping.getKey();
					List<String> dirList = (List) mapping.getValue();
					res = null;
					if (fsRelPath.equals(path)) {
						res = "";
					} else if (fsRelPath.startsWith(path + "/")) {
						res = relPath.substring(path.length());
					}
					if (res != null) {
						for (String resourcesDir : dirList) {
							File f = new File(resourcesDir, res);
							if ((f.exists()) && (f.canRead()) && (f.isDirectory())) {
								List<NamingEntry> virtEntries = super.list(f);
								for (NamingEntry entry : virtEntries) {
									if (!entryNames.contains(entry.name)) {
										entryNames.add(entry.name);
										entries.add(entry);
									}
								}
							}
						}
					}
				}
			}
		}

		return entries;
	}

	protected Object doLookup(String name) {
		String res;
		Object retSuper = super.doLookup(name);
		if ((retSuper != null) || (this.mappedResourcePaths == null)) {
			return retSuper;
		}
		for (Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
			String path = (String) mapping.getKey();
			List<String> dirList = (List) mapping.getValue();
			if (name.equals(path)) {
				for (String resourcesDir : dirList) {
					File f = new File(resourcesDir);
					if ((f.exists()) && (f.canRead()) && (f.isFile())) {
						return new FileDirContext.FileResource(f);
					}
				}
			}
			path = path + "/";
			if (name.startsWith(path)) {
				res = name.substring(path.length());
				for (String resourcesDir : dirList) {
					File f = new File(resourcesDir + "/" + res);
					if ((f.exists()) && (f.canRead()) && (f.isFile())) {
						return new FileDirContext.FileResource(f);
					}
				}
			}
		}

		return retSuper;
	}

	protected String doGetRealPath(String path) {
		File file = file(path);
		if (null != file) {
			return file.getAbsolutePath();
		}
		return null;
	}
}

/*
 * Location:
 * D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\
 * apache\naming\resources\VirtualDirContext.class Java compiler version: 6
 * (50.0) JD-Core Version: 0.7.1
 */