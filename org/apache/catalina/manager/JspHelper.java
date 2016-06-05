package org.apache.catalina.manager;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.catalina.Session;
import org.apache.catalina.manager.util.SessionUtils;

public class JspHelper
{
  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final int HIGHEST_SPECIAL = 62;
  
  public static String guessDisplayLocaleFromSession(Session in_session)
  {
    return localeToString(SessionUtils.guessLocaleFromSession(in_session));
  }
  
  private static String localeToString(Locale locale)
  {
    if (locale != null) {
      return escapeXml(locale.toString());
    }
    return "";
  }
  
  public static String guessDisplayUserFromSession(Session in_session)
  {
    Object user = SessionUtils.guessUserFromSession(in_session);
    return escapeXml(user);
  }
  
  public static String getDisplayCreationTimeForSession(Session in_session)
  {
    try
    {
      if (in_session.getCreationTime() == 0L) {
        return "";
      }
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return formatter.format(new Date(in_session.getCreationTime()));
    }
    catch (IllegalStateException ise) {}
    return "";
  }
  
  public static String getDisplayLastAccessedTimeForSession(Session in_session)
  {
    try
    {
      if (in_session.getLastAccessedTime() == 0L) {
        return "";
      }
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return formatter.format(new Date(in_session.getLastAccessedTime()));
    }
    catch (IllegalStateException ise) {}
    return "";
  }
  
  public static String getDisplayUsedTimeForSession(Session in_session)
  {
    try
    {
      if (in_session.getCreationTime() == 0L) {
        return "";
      }
    }
    catch (IllegalStateException ise)
    {
      return "";
    }
    return secondsToTimeString(SessionUtils.getUsedTimeForSession(in_session) / 1000L);
  }
  
  public static String getDisplayTTLForSession(Session in_session)
  {
    try
    {
      if (in_session.getCreationTime() == 0L) {
        return "";
      }
    }
    catch (IllegalStateException ise)
    {
      return "";
    }
    return secondsToTimeString(SessionUtils.getTTLForSession(in_session) / 1000L);
  }
  
  public static String getDisplayInactiveTimeForSession(Session in_session)
  {
    try
    {
      if (in_session.getCreationTime() == 0L) {
        return "";
      }
    }
    catch (IllegalStateException ise)
    {
      return "";
    }
    return secondsToTimeString(SessionUtils.getInactiveTimeForSession(in_session) / 1000L);
  }
  
  public static String secondsToTimeString(long in_seconds)
  {
    StringBuilder buff = new StringBuilder(9);
    if (in_seconds < 0L)
    {
      buff.append('-');
      in_seconds = -in_seconds;
    }
    long rest = in_seconds;
    long hour = rest / 3600L;
    rest %= 3600L;
    long minute = rest / 60L;
    rest %= 60L;
    long second = rest;
    if (hour < 10L) {
      buff.append('0');
    }
    buff.append(hour);
    buff.append(':');
    if (minute < 10L) {
      buff.append('0');
    }
    buff.append(minute);
    buff.append(':');
    if (second < 10L) {
      buff.append('0');
    }
    buff.append(second);
    return buff.toString();
  }
  
  private static char[][] specialCharactersRepresentation = new char[63][];
  
  static
  {
    specialCharactersRepresentation[38] = "&amp;".toCharArray();
    specialCharactersRepresentation[60] = "&lt;".toCharArray();
    specialCharactersRepresentation[62] = "&gt;".toCharArray();
    specialCharactersRepresentation[34] = "&#034;".toCharArray();
    specialCharactersRepresentation[39] = "&#039;".toCharArray();
  }
  
  public static String escapeXml(Object obj)
  {
    String value = null;
    try
    {
      value = obj == null ? null : obj.toString();
    }
    catch (Exception e) {}
    return escapeXml(value);
  }
  
  public static String escapeXml(String buffer)
  {
    if (buffer == null) {
      return "";
    }
    int start = 0;
    int length = buffer.length();
    char[] arrayBuffer = buffer.toCharArray();
    StringBuilder escapedBuffer = null;
    for (int i = 0; i < length; i++)
    {
      char c = arrayBuffer[i];
      if (c <= '>')
      {
        char[] escaped = specialCharactersRepresentation[c];
        if (escaped != null)
        {
          if (start == 0) {
            escapedBuffer = new StringBuilder(length + 5);
          }
          if (start < i) {
            escapedBuffer.append(arrayBuffer, start, i - start);
          }
          start = i + 1;
          
          escapedBuffer.append(escaped);
        }
      }
    }
    if (start == 0) {
      return buffer;
    }
    if (start < length) {
      escapedBuffer.append(arrayBuffer, start, length - start);
    }
    return escapedBuffer.toString();
  }
  
  public static String formatNumber(long number)
  {
    return NumberFormat.getNumberInstance().format(number);
  }
  
  private JspHelper() {}
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\manager\JspHelper.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */