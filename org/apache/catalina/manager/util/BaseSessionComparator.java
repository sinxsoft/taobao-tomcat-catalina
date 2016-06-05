package org.apache.catalina.manager.util;

import java.util.Comparator;
import org.apache.catalina.Session;

public abstract class BaseSessionComparator<T>
  implements Comparator<Session>
{
  public BaseSessionComparator() {}
  
  public abstract Comparable<T> getComparableObject(Session paramSession);
  
  public final int compare(Session s1, Session s2)
  {
    Comparable<T> c1 = getComparableObject(s1);
    Comparable<T> c2 = getComparableObject(s2);
    return c2 == null ? 1 : c1 == null ? -1 : c2 == null ? 0 : c1.compareTo((T) c2);
  }
}
