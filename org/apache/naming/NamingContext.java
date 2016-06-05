package org.apache.naming;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class NamingContext
  implements Context
{
  protected static final NameParser nameParser = new NameParserImpl();
  private static final Log log = LogFactory.getLog(NamingContext.class);
  protected Hashtable<String, Object> env;
  
  public NamingContext(Hashtable<String, Object> env, String name)
    throws NamingException
  {
    this.bindings = new HashMap();
    this.env = new Hashtable();
    
    this.name = name;
    if (env != null)
    {
      Enumeration<String> envEntries = env.keys();
      while (envEntries.hasMoreElements())
      {
        String entryName = (String)envEntries.nextElement();
        addToEnvironment(entryName, env.get(entryName));
      }
    }
  }
  
  public NamingContext(Hashtable<String, Object> env, String name, HashMap<String, NamingEntry> bindings)
    throws NamingException
  {
    this(env, name);
    this.bindings = bindings;
  }
  
  protected static final StringManager sm = StringManager.getManager("org.apache.naming");
  protected HashMap<String, NamingEntry> bindings;
  protected String name;
  private boolean exceptionOnFailedWrite = true;
  
  public boolean getExceptionOnFailedWrite()
  {
    return this.exceptionOnFailedWrite;
  }
  
  public void setExceptionOnFailedWrite(boolean exceptionOnFailedWrite)
  {
    this.exceptionOnFailedWrite = exceptionOnFailedWrite;
  }
  
  public Object lookup(Name name)
    throws NamingException
  {
    return lookup(name, true);
  }
  
  public Object lookup(String name)
    throws NamingException
  {
    return lookup(new CompositeName(name), true);
  }
  
  public void bind(Name name, Object obj)
    throws NamingException
  {
    bind(name, obj, false);
  }
  
  public void bind(String name, Object obj)
    throws NamingException
  {
    bind(new CompositeName(name), obj);
  }
  
  public void rebind(Name name, Object obj)
    throws NamingException
  {
    bind(name, obj, true);
  }
  
  public void rebind(String name, Object obj)
    throws NamingException
  {
    rebind(new CompositeName(name), obj);
  }
  
  public void unbind(Name name)
    throws NamingException
  {
    if (!checkWritable()) {
      return;
    }
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      throw new NamingException(sm.getString("namingContext.invalidName"));
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (entry == null) {
      throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
    }
    if (name.size() > 1)
    {
      if (entry.type == 10) {
        ((Context)entry.value).unbind(name.getSuffix(1));
      } else {
        throw new NamingException(sm.getString("namingContext.contextExpected"));
      }
    }
    else {
      this.bindings.remove(name.get(0));
    }
  }
  
  public void unbind(String name)
    throws NamingException
  {
    unbind(new CompositeName(name));
  }
  
  public void rename(Name oldName, Name newName)
    throws NamingException
  {
    Object value = lookup(oldName);
    bind(newName, value);
    unbind(oldName);
  }
  
  public void rename(String oldName, String newName)
    throws NamingException
  {
    rename(new CompositeName(oldName), new CompositeName(newName));
  }
  
  public NamingEnumeration<NameClassPair> list(Name name)
    throws NamingException
  {
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      return new NamingContextEnumeration(this.bindings.values().iterator());
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (entry == null) {
      throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
    }
    if (entry.type != 10) {
      throw new NamingException(sm.getString("namingContext.contextExpected"));
    }
    return ((Context)entry.value).list(name.getSuffix(1));
  }
  
  public NamingEnumeration<NameClassPair> list(String name)
    throws NamingException
  {
    return list(new CompositeName(name));
  }
  
  public NamingEnumeration<Binding> listBindings(Name name)
    throws NamingException
  {
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      return new NamingContextBindingsEnumeration(this.bindings.values().iterator(), this);
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (entry == null) {
      throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
    }
    if (entry.type != 10) {
      throw new NamingException(sm.getString("namingContext.contextExpected"));
    }
    return ((Context)entry.value).listBindings(name.getSuffix(1));
  }
  
  public NamingEnumeration<Binding> listBindings(String name)
    throws NamingException
  {
    return listBindings(new CompositeName(name));
  }
  
  public void destroySubcontext(Name name)
    throws NamingException
  {
    if (!checkWritable()) {
      return;
    }
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      throw new NamingException(sm.getString("namingContext.invalidName"));
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (entry == null) {
      throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
    }
    if (name.size() > 1)
    {
      if (entry.type == 10) {
        ((Context)entry.value).destroySubcontext(name.getSuffix(1));
      } else {
        throw new NamingException(sm.getString("namingContext.contextExpected"));
      }
    }
    else if (entry.type == 10)
    {
      ((Context)entry.value).close();
      this.bindings.remove(name.get(0));
    }
    else
    {
      throw new NotContextException(sm.getString("namingContext.contextExpected"));
    }
  }
  
  public void destroySubcontext(String name)
    throws NamingException
  {
    destroySubcontext(new CompositeName(name));
  }
  
  public Context createSubcontext(Name name)
    throws NamingException
  {
    if (!checkWritable()) {
      return null;
    }
    NamingContext newContext = new NamingContext(this.env, this.name);
    bind(name, newContext);
    
    newContext.setExceptionOnFailedWrite(getExceptionOnFailedWrite());
    
    return newContext;
  }
  
  public Context createSubcontext(String name)
    throws NamingException
  {
    return createSubcontext(new CompositeName(name));
  }
  
  public Object lookupLink(Name name)
    throws NamingException
  {
    return lookup(name, false);
  }
  
  public Object lookupLink(String name)
    throws NamingException
  {
    return lookup(new CompositeName(name), false);
  }
  
  public NameParser getNameParser(Name name)
    throws NamingException
  {
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      return nameParser;
    }
    if (name.size() > 1)
    {
      Object obj = this.bindings.get(name.get(0));
      if ((obj instanceof Context)) {
        return ((Context)obj).getNameParser(name.getSuffix(1));
      }
      throw new NotContextException(sm.getString("namingContext.contextExpected"));
    }
    return nameParser;
  }
  
  public NameParser getNameParser(String name)
    throws NamingException
  {
    return getNameParser(new CompositeName(name));
  }
  
  public Name composeName(Name name, Name prefix)
    throws NamingException
  {
    prefix = (Name)prefix.clone();
    return prefix.addAll(name);
  }
  
  public String composeName(String name, String prefix)
    throws NamingException
  {
    return prefix + "/" + name;
  }
  
  public Object addToEnvironment(String propName, Object propVal)
    throws NamingException
  {
    return this.env.put(propName, propVal);
  }
  
  public Object removeFromEnvironment(String propName)
    throws NamingException
  {
    return this.env.remove(propName);
  }
  
  public Hashtable<?, ?> getEnvironment()
    throws NamingException
  {
    return this.env;
  }
  
  public void close()
    throws NamingException
  {
    if (!checkWritable()) {
      return;
    }
    this.env.clear();
  }
  
  public String getNameInNamespace()
    throws NamingException
  {
    throw new OperationNotSupportedException(sm.getString("namingContext.noAbsoluteName"));
  }
  
  protected Object lookup(Name name, boolean resolveLinks)
    throws NamingException
  {
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      return new NamingContext(this.env, this.name, this.bindings);
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (entry == null) {
      throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
    }
    if (name.size() > 1)
    {
      if (entry.type != 10) {
        throw new NamingException(sm.getString("namingContext.contextExpected"));
      }
      return ((Context)entry.value).lookup(name.getSuffix(1));
    }
    if ((resolveLinks) && (entry.type == 1))
    {
      String link = ((LinkRef)entry.value).getLinkName();
      if (link.startsWith(".")) {
        return lookup(link.substring(1));
      }
      return new InitialContext(this.env).lookup(link);
    }
    if (entry.type == 2) {
      try
      {
        Object obj = NamingManager.getObjectInstance(entry.value, name, this, this.env);
        if ((entry.value instanceof ResourceRef))
        {
          boolean singleton = Boolean.parseBoolean((String)((ResourceRef)entry.value).get("singleton").getContent());
          if (singleton)
          {
            entry.type = 0;
            entry.value = obj;
          }
        }
        return obj;
      }
      catch (NamingException e)
      {
        throw e;
      }
      catch (Exception e)
      {
        log.warn(sm.getString("namingContext.failResolvingReference"), e);
        
        throw new NamingException(e.getMessage());
      }
    }
    return entry.value;
  }
  
  protected void bind(Name name, Object obj, boolean rebind)
    throws NamingException
  {
    if (!checkWritable()) {
      return;
    }
    while ((!name.isEmpty()) && (name.get(0).length() == 0)) {
      name = name.getSuffix(1);
    }
    if (name.isEmpty()) {
      throw new NamingException(sm.getString("namingContext.invalidName"));
    }
    NamingEntry entry = (NamingEntry)this.bindings.get(name.get(0));
    if (name.size() > 1)
    {
      if (entry == null) {
        throw new NameNotFoundException(sm.getString("namingContext.nameNotBound", new Object[] { name, name.get(0) }));
      }
      if (entry.type == 10)
      {
        if (rebind) {
          ((Context)entry.value).rebind(name.getSuffix(1), obj);
        } else {
          ((Context)entry.value).bind(name.getSuffix(1), obj);
        }
      }
      else {
        throw new NamingException(sm.getString("namingContext.contextExpected"));
      }
    }
    else
    {
      if ((!rebind) && (entry != null)) {
        throw new NameAlreadyBoundException(sm.getString("namingContext.alreadyBound", new Object[] { name.get(0) }));
      }
      Object toBind = NamingManager.getStateToBind(obj, name, this, this.env);
      if ((toBind instanceof Context))
      {
        entry = new NamingEntry(name.get(0), toBind, 10);
      }
      else if ((toBind instanceof LinkRef))
      {
        entry = new NamingEntry(name.get(0), toBind, 1);
      }
      else if ((toBind instanceof Reference))
      {
        entry = new NamingEntry(name.get(0), toBind, 2);
      }
      else if ((toBind instanceof Referenceable))
      {
        toBind = ((Referenceable)toBind).getReference();
        entry = new NamingEntry(name.get(0), toBind, 2);
      }
      else
      {
        entry = new NamingEntry(name.get(0), toBind, 0);
      }
      this.bindings.put(name.get(0), entry);
    }
  }
  
  protected boolean isWritable()
  {
    return ContextAccessController.isWritable(this.name);
  }
  
  protected boolean checkWritable()
    throws NamingException
  {
    if (isWritable()) {
      return true;
    }
    if (this.exceptionOnFailedWrite) {
      throw new OperationNotSupportedException(sm.getString("namingContext.readOnly"));
    }
    return false;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\NamingContext.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */