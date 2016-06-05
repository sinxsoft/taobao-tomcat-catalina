package com.taobao.tomcat.util;

public abstract interface Constants
{
  public static final String VERSION_NUMBER = "com.taobao.tomcat.version";
  public static final String VERSION_INFO = "com.taobao.tomcat.info";
  public static final String JBOSS_WEB_XML = "WEB-INF/jboss-web.xml";
  public static final String DEFAULT_PANDORA_NAME = "Pandora";
  public static final String DEFAULT_PANDORA_BASE = "deploy";
  public static final String DEFAULT_PANDORA_TARGET = "taobao-hsf.sar";
  public static final String[] PANDORA_TARGET_OPTIONS = { "taobao-hsf.sar", "pandora.sar" };
  public static final String PANDORA_CONTAINER = "com.taobao.pandora.delegator.PandoraDelegator";
  public static final String HSF_CONTAINER = "com.taobao.hsf.container.HSFContainer";
  public static final String ALI_WEBAPP_CLASS_LOADER = "com.taobao.tomcat.container.context.loader.AliWebappClassLoader";
  public static final String CATALINA_LOGS_PROP = "catalina.logs";
  public static final String PANDORA_SKIP_PROP = "pandora.skip";
  public static final String PANDORA_LOCATION = "pandora.location";
  public static final String JBOSS_GETRESOURCE = "com.tomcat.catalina.loader.jbossGetResource";
}

