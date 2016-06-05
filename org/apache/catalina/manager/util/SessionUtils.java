package org.apache.catalina.manager.util;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import javax.security.auth.Subject;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Session;
import org.apache.tomcat.util.ExceptionUtils;

public class SessionUtils
{
  private static final String STRUTS_LOCALE_KEY = "org.apache.struts.action.LOCALE";
  private static final String JSTL_LOCALE_KEY = "javax.servlet.jsp.jstl.fmt.locale";
  private static final String SPRING_LOCALE_KEY = "org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE";
  private static final String[] LOCALE_TEST_ATTRIBUTES = { "org.apache.struts.action.LOCALE", "org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE", "javax.servlet.jsp.jstl.fmt.locale", "Locale", "java.util.Locale" };
  private static final String[] USER_TEST_ATTRIBUTES = { "Login", "User", "userName", "UserName", "Utilisateur", "SPRING_SECURITY_LAST_USERNAME" };
  
  private SessionUtils() {}
  
  public static Locale guessLocaleFromSession(Session in_session)
  {
    return guessLocaleFromSession(in_session.getSession());
  }
  
  public static Locale guessLocaleFromSession(HttpSession in_session)
  {
    if (null == in_session) {
      return null;
    }
    try
    {
      Locale locale = null;
      for (int i = 0; i < LOCALE_TEST_ATTRIBUTES.length; i++)
      {
        Object obj = in_session.getAttribute(LOCALE_TEST_ATTRIBUTES[i]);
        if ((null != obj) && ((obj instanceof Locale)))
        {
          locale = (Locale)obj;
          break;
        }
        obj = in_session.getAttribute(LOCALE_TEST_ATTRIBUTES[i].toLowerCase(Locale.ENGLISH));
        if ((null != obj) && ((obj instanceof Locale)))
        {
          locale = (Locale)obj;
          break;
        }
        obj = in_session.getAttribute(LOCALE_TEST_ATTRIBUTES[i].toUpperCase(Locale.ENGLISH));
        if ((null != obj) && ((obj instanceof Locale)))
        {
          locale = (Locale)obj;
          break;
        }
      }
      if (null != locale) {
        return locale;
      }
      List<Object> tapestryArray = new ArrayList();
      for (Enumeration<String> enumeration = in_session.getAttributeNames(); enumeration.hasMoreElements();)
      {
        String name = (String)enumeration.nextElement();
        if ((name.indexOf("tapestry") > -1) && (name.indexOf("engine") > -1) && (null != in_session.getAttribute(name))) {
          tapestryArray.add(in_session.getAttribute(name));
        }
      }
      if (tapestryArray.size() == 1)
      {
        Object probableEngine = tapestryArray.get(0);
        if (null != probableEngine) {
          try
          {
            Method readMethod = probableEngine.getClass().getMethod("getLocale", (Class[])null);
            if (null != readMethod)
            {
              Object possibleLocale = readMethod.invoke(probableEngine, (Object[])null);
              if ((null != possibleLocale) && ((possibleLocale instanceof Locale))) {
                locale = (Locale)possibleLocale;
              }
            }
          }
          catch (Exception e)
          {
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            
            ExceptionUtils.handleThrowable(t);
          }
        }
      }
      if (null != locale) {
        return locale;
      }
      List<Object> localeArray = new ArrayList();
      for (Enumeration<String> enumeration = in_session.getAttributeNames(); enumeration.hasMoreElements();)
      {
        String name = (String)enumeration.nextElement();
        Object obj = in_session.getAttribute(name);
        if ((null != obj) && ((obj instanceof Locale))) {
          localeArray.add(obj);
        }
      }
      if (localeArray.size() == 1) {}
      return (Locale)localeArray.get(0);
    }
    catch (IllegalStateException ise) {}
    return null;
  }
  
  public static Object guessUserFromSession(Session in_session)
  {
    if (null == in_session) {
      return null;
    }
    if (in_session.getPrincipal() != null) {
      return in_session.getPrincipal().getName();
    }
    HttpSession httpSession = in_session.getSession();
    if (httpSession == null) {
      return null;
    }
    try
    {
      Object user = null;
      for (int i = 0; i < USER_TEST_ATTRIBUTES.length; i++)
      {
        Object obj = httpSession.getAttribute(USER_TEST_ATTRIBUTES[i]);
        if (null != obj)
        {
          user = obj;
          break;
        }
        obj = httpSession.getAttribute(USER_TEST_ATTRIBUTES[i].toLowerCase(Locale.ENGLISH));
        if (null != obj)
        {
          user = obj;
          break;
        }
        obj = httpSession.getAttribute(USER_TEST_ATTRIBUTES[i].toUpperCase(Locale.ENGLISH));
        if (null != obj)
        {
          user = obj;
          break;
        }
      }
      if (null != user) {
        return user;
      }
      List<Object> principalArray = new ArrayList();
      for (Enumeration<String> enumeration = httpSession.getAttributeNames(); enumeration.hasMoreElements();)
      {
        String name = (String)enumeration.nextElement();
        Object obj = httpSession.getAttribute(name);
        if ((null != obj) && (((obj instanceof Principal)) || ((obj instanceof Subject)))) {
          principalArray.add(obj);
        }
      }
      if (principalArray.size() == 1) {
        user = principalArray.get(0);
      }
      if (null != user) {
        return user;
      }
      return user;
    }
    catch (IllegalStateException ise) {}
    return null;
  }
  
  public static long getUsedTimeForSession(Session in_session)
  {
    try
    {
      return in_session.getThisAccessedTime() - in_session.getCreationTime();
    }
    catch (IllegalStateException ise) {}
    return -1L;
  }
  
  public static long getTTLForSession(Session in_session)
  {
    try
    {
      return 1000 * in_session.getMaxInactiveInterval() - (System.currentTimeMillis() - in_session.getThisAccessedTime());
    }
    catch (IllegalStateException ise) {}
    return -1L;
  }
  
  public static long getInactiveTimeForSession(Session in_session)
  {
    try
    {
      return System.currentTimeMillis() - in_session.getThisAccessedTime();
    }
    catch (IllegalStateException ise) {}
    return -1L;
  }
}

