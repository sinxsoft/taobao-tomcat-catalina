package org.apache.catalina.manager.host;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.manager.Constants;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.tomcat.util.res.StringManager;

public final class HTMLHostManagerServlet
  extends HostManagerServlet
{
  private static final long serialVersionUID = 1L;
  private static final String HOSTS_HEADER_SECTION = "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"5\" class=\"title\">{0}</td>\n</tr>\n<tr>\n <td class=\"header-left\"><small>{0}</small></td>\n <td class=\"header-center\"><small>{1}</small></td>\n <td class=\"header-center\"><small>{2}</small></td>\n</tr>\n";
  private static final String HOSTS_ROW_DETAILS_SECTION = "<tr>\n <td class=\"row-left\"><small><a href=\"http://{0}\">{0}</a></small></td>\n <td class=\"row-center\"><small>{1}</small></td>\n";
  
  public HTMLHostManagerServlet() {}
  
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    StringManager smClient = StringManager.getManager("org.apache.catalina.manager.host", request.getLocales());
    
    String command = request.getPathInfo();
    
    response.setContentType("text/html; charset=utf-8");
    
    String message = "";
    if (command != null) {
      if (!command.equals("/list")) {
        if ((command.equals("/add")) || (command.equals("/remove")) || (command.equals("/start")) || (command.equals("/stop"))) {
          message = smClient.getString("hostManagerServlet.postCommand", new Object[] { command });
        } else {
          message = smClient.getString("hostManagerServlet.unknownCommand", new Object[] { command });
        }
      }
    }
    list(request, response, message, smClient);
  }
  
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    StringManager smClient = StringManager.getManager("org.apache.catalina.manager.host", request.getLocales());
    
    String command = request.getPathInfo();
    
    String name = request.getParameter("name");
    
    response.setContentType("text/html; charset=utf-8");
    
    String message = "";
    if (command != null) {
      if (command.equals("/add")) {
        message = add(request, name, smClient);
      } else if (command.equals("/remove")) {
        message = remove(name, smClient);
      } else if (command.equals("/start")) {
        message = start(name, smClient);
      } else if (command.equals("/stop")) {
        message = stop(name, smClient);
      } else {
        doGet(request, response);
      }
    }
    list(request, response, message, smClient);
  }
  
  protected String add(HttpServletRequest request, String name, StringManager smClient)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    
    super.add(request, printWriter, name, true, smClient);
    
    return stringWriter.toString();
  }
  
  protected String remove(String name, StringManager smClient)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    
    super.remove(printWriter, name, smClient);
    
    return stringWriter.toString();
  }
  
  protected String start(String name, StringManager smClient)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    
    super.start(printWriter, name, smClient);
    
    return stringWriter.toString();
  }
  
  protected String stop(String name, StringManager smClient)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    
    super.stop(printWriter, name, smClient);
    
    return stringWriter.toString();
  }
  
  public void list(HttpServletRequest request, HttpServletResponse response, String message, StringManager smClient)
    throws IOException
  {
    if (this.debug >= 1) {
      log(sm.getString("hostManagerServlet.list", new Object[] { this.engine.getName() }));
    }
    PrintWriter writer = response.getWriter();
    
    writer.print(Constants.HTML_HEADER_SECTION);
    
    Object[] args = new Object[2];
    args[0] = request.getContextPath();
    args[1] = smClient.getString("htmlHostManagerServlet.title");
    writer.print(MessageFormat.format("<title>{0}</title>\n</head>\n\n<body bgcolor=\"#FFFFFF\">\n\n<table cellspacing=\"4\" border=\"0\">\n <tr>\n  <td colspan=\"2\">\n   <a href=\"http://www.apache.org/\">\n    <img border=\"0\" alt=\"The Apache Software Foundation\" align=\"left\"\n         src=\"{0}/images/asf-logo.gif\">\n   </a>\n   <a href=\"http://tomcat.apache.org/\">\n    <img border=\"0\" alt=\"The Tomcat Servlet/JSP Container\"\n         align=\"right\" src=\"{0}/images/tomcat.gif\">\n   </a>\n  </td>\n </tr>\n</table>\n<hr size=\"1\" noshade=\"noshade\">\n<table cellspacing=\"4\" border=\"0\">\n <tr>\n  <td class=\"page-title\" bordercolor=\"#000000\" align=\"left\" nowrap>\n   <font size=\"+2\">{1}</font>\n  </td>\n </tr>\n</table>\n<br>\n\n", args));
    
    args = new Object[3];
    args[0] = smClient.getString("htmlHostManagerServlet.messageLabel");
    if ((message == null) || (message.length() == 0)) {
      args[1] = "OK";
    } else {
      args[1] = RequestUtil.filter(message);
    }
    writer.print(MessageFormat.format("<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n <tr>\n  <td class=\"row-left\" width=\"10%\"><small><strong>{0}</strong></small>&nbsp;</td>\n  <td class=\"row-left\"><pre>{1}</pre></td>\n </tr>\n</table>\n<br>\n\n", args));
    
    args = new Object[9];
    args[0] = smClient.getString("htmlHostManagerServlet.manager");
    args[1] = response.encodeURL(request.getContextPath() + "/html/list");
    args[2] = smClient.getString("htmlHostManagerServlet.list");
    args[3] = response.encodeURL(request.getContextPath() + "/" + smClient.getString("htmlHostManagerServlet.helpHtmlManagerFile"));
    
    args[4] = smClient.getString("htmlHostManagerServlet.helpHtmlManager");
    args[5] = response.encodeURL(request.getContextPath() + "/" + smClient.getString("htmlHostManagerServlet.helpManagerFile"));
    
    args[6] = smClient.getString("htmlHostManagerServlet.helpManager");
    args[7] = response.encodeURL("/manager/status");
    args[8] = smClient.getString("statusServlet.title");
    writer.print(MessageFormat.format("<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"4\" class=\"title\">{0}</td>\n</tr>\n <tr>\n  <td class=\"row-left\"><a href=\"{1}\">{2}</a></td>\n  <td class=\"row-center\"><a href=\"{3}\">{4}</a></td>\n  <td class=\"row-center\"><a href=\"{5}\">{6}</a></td>\n  <td class=\"row-right\"><a href=\"{7}\">{8}</a></td>\n </tr>\n</table>\n<br>\n\n", args));
    
    args = new Object[3];
    args[0] = smClient.getString("htmlHostManagerServlet.hostName");
    args[1] = smClient.getString("htmlHostManagerServlet.hostAliases");
    args[2] = smClient.getString("htmlHostManagerServlet.hostTasks");
    writer.print(MessageFormat.format("<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"5\" class=\"title\">{0}</td>\n</tr>\n<tr>\n <td class=\"header-left\"><small>{0}</small></td>\n <td class=\"header-center\"><small>{1}</small></td>\n <td class=\"header-center\"><small>{2}</small></td>\n</tr>\n", args));
    
    Container[] children = this.engine.findChildren();
    String[] hostNames = new String[children.length];
    for (int i = 0; i < children.length; i++) {
      hostNames[i] = children[i].getName();
    }
    TreeMap<String, String> sortedHostNamesMap = new TreeMap();
    for (int i = 0; i < hostNames.length; i++)
    {
      String displayPath = hostNames[i];
      sortedHostNamesMap.put(displayPath, hostNames[i]);
    }
    String hostsStart = smClient.getString("htmlHostManagerServlet.hostsStart");
    
    String hostsStop = smClient.getString("htmlHostManagerServlet.hostsStop");
    
    String hostsRemove = smClient.getString("htmlHostManagerServlet.hostsRemove");
    
    Iterator<Map.Entry<String, String>> iterator = sortedHostNamesMap.entrySet().iterator();
    while (iterator.hasNext())
    {
      Map.Entry<String, String> entry = (Map.Entry)iterator.next();
      String hostName = (String)entry.getKey();
      Host host = (Host)this.engine.findChild(hostName);
      if (host != null)
      {
        args = new Object[2];
        args[0] = RequestUtil.filter(hostName);
        String[] aliases = host.findAliases();
        StringBuilder buf = new StringBuilder();
        if (aliases.length > 0)
        {
          buf.append(aliases[0]);
          for (int j = 1; j < aliases.length; j++) {
            buf.append(", ").append(aliases[j]);
          }
        }
        if (buf.length() == 0)
        {
          buf.append("&nbsp;");
          args[1] = buf.toString();
        }
        else
        {
          args[1] = RequestUtil.filter(buf.toString());
        }
        writer.print(MessageFormat.format("<tr>\n <td class=\"row-left\"><small><a href=\"http://{0}\">{0}</a></small></td>\n <td class=\"row-center\"><small>{1}</small></td>\n", args));
        
        args = new Object[4];
        if (host.getState().isAvailable())
        {
          args[0] = response.encodeURL(request.getContextPath() + "/html/stop?name=" + URLEncoder.encode(hostName, "UTF-8"));
          
          args[1] = hostsStop;
        }
        else
        {
          args[0] = response.encodeURL(request.getContextPath() + "/html/start?name=" + URLEncoder.encode(hostName, "UTF-8"));
          
          args[1] = hostsStart;
        }
        args[2] = response.encodeURL(request.getContextPath() + "/html/remove?name=" + URLEncoder.encode(hostName, "UTF-8"));
        
        args[3] = hostsRemove;
        if (host == this.installedHost) {
          writer.print(MessageFormat.format(MANAGER_HOST_ROW_BUTTON_SECTION, args));
        } else {
          writer.print(MessageFormat.format(" <td class=\"row-left\" NOWRAP>\n  <form class=\"inline\" method=\"POST\" action=\"{0}\">   <small><input type=\"submit\" value=\"{1}\"></small>  </form>\n  <form class=\"inline\" method=\"POST\" action=\"{2}\">   <small><input type=\"submit\" value=\"{3}\"></small>  </form>\n </td>\n</tr>\n", args));
        }
      }
    }
    args = new Object[6];
    args[0] = smClient.getString("htmlHostManagerServlet.addTitle");
    args[1] = smClient.getString("htmlHostManagerServlet.addHost");
    args[2] = response.encodeURL(request.getContextPath() + "/html/add");
    args[3] = smClient.getString("htmlHostManagerServlet.addName");
    args[4] = smClient.getString("htmlHostManagerServlet.addAliases");
    args[5] = smClient.getString("htmlHostManagerServlet.addAppBase");
    writer.print(MessageFormat.format("</table>\n<br>\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"2\" class=\"title\">{0}</td>\n</tr>\n<tr>\n <td colspan=\"2\" class=\"header-left\"><small>{1}</small></td>\n</tr>\n<tr>\n <td colspan=\"2\">\n<form method=\"post\" action=\"{2}\">\n<table cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td class=\"row-right\">\n  <small>{3}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"name\" size=\"20\">\n </td>\n</tr>\n<tr>\n <td class=\"row-right\">\n  <small>{4}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"aliases\" size=\"64\">\n </td>\n</tr>\n<tr>\n <td class=\"row-right\">\n  <small>{5}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"appBase\" size=\"64\">\n </td>\n</tr>\n", args));
    
    args = new Object[3];
    args[0] = smClient.getString("htmlHostManagerServlet.addAutoDeploy");
    args[1] = "autoDeploy";
    args[2] = "checked";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    args[0] = smClient.getString("htmlHostManagerServlet.addDeployOnStartup");
    
    args[1] = "deployOnStartup";
    args[2] = "checked";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    args[0] = smClient.getString("htmlHostManagerServlet.addDeployXML");
    args[1] = "deployXML";
    args[2] = "checked";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    args[0] = smClient.getString("htmlHostManagerServlet.addUnpackWARs");
    args[1] = "unpackWARs";
    args[2] = "checked";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    
    args[0] = smClient.getString("htmlHostManagerServlet.addManager");
    args[1] = "manager";
    args[2] = "checked";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    
    args[0] = smClient.getString("htmlHostManagerServlet.addCopyXML");
    args[1] = "copyXML";
    args[2] = "";
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n", args));
    
    args = new Object[1];
    args[0] = smClient.getString("htmlHostManagerServlet.addButton");
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-right\">\n  &nbsp;\n </td>\n <td class=\"row-left\">\n  <input type=\"submit\" value=\"{0}\">\n </td>\n</tr>\n</table>\n</form>\n</td>\n</tr>\n</table>\n<br>\n\n", args));
    
    args = new Object[7];
    args[0] = smClient.getString("htmlHostManagerServlet.serverTitle");
    args[1] = smClient.getString("htmlHostManagerServlet.serverVersion");
    args[2] = smClient.getString("htmlHostManagerServlet.serverJVMVersion");
    args[3] = smClient.getString("htmlHostManagerServlet.serverJVMVendor");
    args[4] = smClient.getString("htmlHostManagerServlet.serverOSName");
    args[5] = smClient.getString("htmlHostManagerServlet.serverOSVersion");
    args[6] = smClient.getString("htmlHostManagerServlet.serverOSArch");
    writer.print(MessageFormat.format("<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"6\" class=\"title\">{0}</td>\n</tr>\n<tr>\n <td class=\"header-center\"><small>{1}</small></td>\n <td class=\"header-center\"><small>{2}</small></td>\n <td class=\"header-center\"><small>{3}</small></td>\n <td class=\"header-center\"><small>{4}</small></td>\n <td class=\"header-center\"><small>{5}</small></td>\n <td class=\"header-center\"><small>{6}</small></td>\n</tr>\n", args));
    
    args = new Object[6];
    args[0] = ServerInfo.getServerInfo();
    args[1] = System.getProperty("java.runtime.version");
    args[2] = System.getProperty("java.vm.vendor");
    args[3] = System.getProperty("os.name");
    args[4] = System.getProperty("os.version");
    args[5] = System.getProperty("os.arch");
    writer.print(MessageFormat.format("<tr>\n <td class=\"row-center\"><small>{0}</small></td>\n <td class=\"row-center\"><small>{1}</small></td>\n <td class=\"row-center\"><small>{2}</small></td>\n <td class=\"row-center\"><small>{3}</small></td>\n <td class=\"row-center\"><small>{4}</small></td>\n <td class=\"row-center\"><small>{5}</small></td>\n</tr>\n</table>\n<br>\n\n", args));
    
    writer.print("<hr size=\"1\" noshade=\"noshade\">\n<center><font size=\"-1\" color=\"#525D76\">\n <em>Copyright &copy; 1999-2015, Apache Software Foundation</em></font></center>\n\n</body>\n</html>");
    
    writer.flush();
    writer.close();
  }
  
  private static final String MANAGER_HOST_ROW_BUTTON_SECTION = " <td class=\"row-left\">\n  <small>\n" + sm.getString("htmlHostManagerServlet.hostThis") + "  </small>\n" + " </td>\n" + "</tr>\n";
  private static final String HOSTS_ROW_BUTTON_SECTION = " <td class=\"row-left\" NOWRAP>\n  <form class=\"inline\" method=\"POST\" action=\"{0}\">   <small><input type=\"submit\" value=\"{1}\"></small>  </form>\n  <form class=\"inline\" method=\"POST\" action=\"{2}\">   <small><input type=\"submit\" value=\"{3}\"></small>  </form>\n </td>\n</tr>\n";
  private static final String ADD_SECTION_START = "</table>\n<br>\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td colspan=\"2\" class=\"title\">{0}</td>\n</tr>\n<tr>\n <td colspan=\"2\" class=\"header-left\"><small>{1}</small></td>\n</tr>\n<tr>\n <td colspan=\"2\">\n<form method=\"post\" action=\"{2}\">\n<table cellspacing=\"0\" cellpadding=\"3\">\n<tr>\n <td class=\"row-right\">\n  <small>{3}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"name\" size=\"20\">\n </td>\n</tr>\n<tr>\n <td class=\"row-right\">\n  <small>{4}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"aliases\" size=\"64\">\n </td>\n</tr>\n<tr>\n <td class=\"row-right\">\n  <small>{5}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"text\" name=\"appBase\" size=\"64\">\n </td>\n</tr>\n";
  private static final String ADD_SECTION_BOOLEAN = "<tr>\n <td class=\"row-right\">\n  <small>{0}</small>\n </td>\n <td class=\"row-left\">\n  <input type=\"checkbox\" name=\"{1}\" {2}>\n </td>\n</tr>\n";
  private static final String ADD_SECTION_END = "<tr>\n <td class=\"row-right\">\n  &nbsp;\n </td>\n <td class=\"row-left\">\n  <input type=\"submit\" value=\"{0}\">\n </td>\n</tr>\n</table>\n</form>\n</td>\n</tr>\n</table>\n<br>\n\n";
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\manager\host\HTMLHostManagerServlet.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */