package org.apache.catalina.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@Deprecated
final class StandardSessionContext
  implements HttpSessionContext
{
  private static final List<String> emptyString = null;
  
  StandardSessionContext() {}
  
  @Deprecated
  public Enumeration<String> getIds()
  {
    return Collections.enumeration(emptyString);
  }
  
  @Deprecated
  public HttpSession getSession(String id)
  {
    return null;
  }
}

