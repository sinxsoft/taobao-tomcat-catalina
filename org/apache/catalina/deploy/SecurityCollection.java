package org.apache.catalina.deploy;

import java.io.Serializable;
import org.apache.catalina.util.RequestUtil;

public class SecurityCollection
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  public SecurityCollection()
  {
    this(null, null);
  }
  
  public SecurityCollection(String name)
  {
    this(name, null);
  }
  
  public SecurityCollection(String name, String description)
  {
    setName(name);
    setDescription(description);
  }
  
  private String description = null;
  private String[] methods = new String[0];
  private String[] omittedMethods = new String[0];
  private String name = null;
  private String[] patterns = new String[0];
  private boolean isFromDescriptor = true;
  
  public String getDescription()
  {
    return this.description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public boolean isFromDescriptor()
  {
    return this.isFromDescriptor;
  }
  
  public void setFromDescriptor(boolean isFromDescriptor)
  {
    this.isFromDescriptor = isFromDescriptor;
  }
  
  public void addMethod(String method)
  {
    if (method == null) {
      return;
    }
    String[] results = new String[this.methods.length + 1];
    for (int i = 0; i < this.methods.length; i++) {
      results[i] = this.methods[i];
    }
    results[this.methods.length] = method;
    this.methods = results;
  }
  
  public void addOmittedMethod(String method)
  {
    if (method == null) {
      return;
    }
    String[] results = new String[this.omittedMethods.length + 1];
    for (int i = 0; i < this.omittedMethods.length; i++) {
      results[i] = this.omittedMethods[i];
    }
    results[this.omittedMethods.length] = method;
    this.omittedMethods = results;
  }
  
  public void addPattern(String pattern)
  {
    if (pattern == null) {
      return;
    }
    String decodedPattern = RequestUtil.URLDecode(pattern);
    String[] results = new String[this.patterns.length + 1];
    for (int i = 0; i < this.patterns.length; i++) {
      results[i] = this.patterns[i];
    }
    results[this.patterns.length] = decodedPattern;
    this.patterns = results;
  }
  
  public boolean findMethod(String method)
  {
    if ((this.methods.length == 0) && (this.omittedMethods.length == 0)) {
      return true;
    }
    if (this.methods.length > 0)
    {
      for (int i = 0; i < this.methods.length; i++) {
        if (this.methods[i].equals(method)) {
          return true;
        }
      }
      return false;
    }
    if (this.omittedMethods.length > 0) {
      for (int i = 0; i < this.omittedMethods.length; i++) {
        if (this.omittedMethods[i].equals(method)) {
          return false;
        }
      }
    }
    return true;
  }
  
  public String[] findMethods()
  {
    return this.methods;
  }
  
  public String[] findOmittedMethods()
  {
    return this.omittedMethods;
  }
  
  public boolean findPattern(String pattern)
  {
    for (int i = 0; i < this.patterns.length; i++) {
      if (this.patterns[i].equals(pattern)) {
        return true;
      }
    }
    return false;
  }
  
  public String[] findPatterns()
  {
    return this.patterns;
  }
  
  public void removeMethod(String method)
  {
    if (method == null) {
      return;
    }
    int n = -1;
    for (int i = 0; i < this.methods.length; i++) {
      if (this.methods[i].equals(method))
      {
        n = i;
        break;
      }
    }
    if (n >= 0)
    {
      int j = 0;
      String[] results = new String[this.methods.length - 1];
      for (int i = 0; i < this.methods.length; i++) {
        if (i != n) {
          results[(j++)] = this.methods[i];
        }
      }
      this.methods = results;
    }
  }
  
  public void removeOmittedMethod(String method)
  {
    if (method == null) {
      return;
    }
    int n = -1;
    for (int i = 0; i < this.omittedMethods.length; i++) {
      if (this.omittedMethods[i].equals(method))
      {
        n = i;
        break;
      }
    }
    if (n >= 0)
    {
      int j = 0;
      String[] results = new String[this.omittedMethods.length - 1];
      for (int i = 0; i < this.omittedMethods.length; i++) {
        if (i != n) {
          results[(j++)] = this.omittedMethods[i];
        }
      }
      this.omittedMethods = results;
    }
  }
  
  public void removePattern(String pattern)
  {
    if (pattern == null) {
      return;
    }
    int n = -1;
    for (int i = 0; i < this.patterns.length; i++) {
      if (this.patterns[i].equals(pattern))
      {
        n = i;
        break;
      }
    }
    if (n >= 0)
    {
      int j = 0;
      String[] results = new String[this.patterns.length - 1];
      for (int i = 0; i < this.patterns.length; i++) {
        if (i != n) {
          results[(j++)] = this.patterns[i];
        }
      }
      this.patterns = results;
    }
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("SecurityCollection[");
    sb.append(this.name);
    if (this.description != null)
    {
      sb.append(", ");
      sb.append(this.description);
    }
    sb.append("]");
    return sb.toString();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\deploy\SecurityCollection.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */