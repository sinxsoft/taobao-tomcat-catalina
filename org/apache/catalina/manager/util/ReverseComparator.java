package org.apache.catalina.manager.util;

import java.util.Comparator;
import org.apache.catalina.Session;

public class ReverseComparator
  implements Comparator<Session>
{
  protected Comparator<Session> comparator;
  
  public ReverseComparator(Comparator<Session> comparator)
  {
    this.comparator = comparator;
  }
  
  public int compare(Session o1, Session o2)
  {
    int returnValue = this.comparator.compare(o1, o2);
    return -returnValue;
  }
}

