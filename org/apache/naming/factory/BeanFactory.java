package org.apache.naming.factory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.ResourceRef;

public class BeanFactory
  implements ObjectFactory
{
  public BeanFactory() {}
  
  public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
    throws NamingException
  {
    if ((obj instanceof ResourceRef)) {
      try
      {
        Reference ref = (Reference)obj;
        String beanClassName = ref.getClassName();
        Class<?> beanClass = null;
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        if (tcl != null) {
          try
          {
            beanClass = tcl.loadClass(beanClassName);
          }
          catch (ClassNotFoundException e) {}
        } else {
          try
          {
            beanClass = Class.forName(beanClassName);
          }
          catch (ClassNotFoundException e)
          {
            e.printStackTrace();
          }
        }
        if (beanClass == null) {
          throw new NamingException("Class not found: " + beanClassName);
        }
        BeanInfo bi = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] pda = bi.getPropertyDescriptors();
        
        Object bean = beanClass.newInstance();
        
        Enumeration<RefAddr> e = ref.getAll();
        while (e.hasMoreElements())
        {
          RefAddr ra = (RefAddr)e.nextElement();
          String propName = ra.getType();
          if ((!propName.equals("factory")) && (!propName.equals("scope")) && (!propName.equals("auth")) && (!propName.equals("singleton")))
          {
            String value = (String)ra.getContent();
            
            Object[] valueArray = new Object[1];
            
            int i = 0;
            for (i = 0; i < pda.length; i++) {
              if (pda[i].getName().equals(propName))
              {
                Class<?> propType = pda[i].getPropertyType();
                if (propType.equals(String.class)) {
                  valueArray[0] = value;
                } else if ((propType.equals(Character.class)) || (propType.equals(Character.TYPE))) {
                  valueArray[0] = Character.valueOf(value.charAt(0));
                } else if ((propType.equals(Byte.class)) || (propType.equals(Byte.TYPE))) {
                  valueArray[0] = new Byte(value);
                } else if ((propType.equals(Short.class)) || (propType.equals(Short.TYPE))) {
                  valueArray[0] = new Short(value);
                } else if ((propType.equals(Integer.class)) || (propType.equals(Integer.TYPE))) {
                  valueArray[0] = new Integer(value);
                } else if ((propType.equals(Long.class)) || (propType.equals(Long.TYPE))) {
                  valueArray[0] = new Long(value);
                } else if ((propType.equals(Float.class)) || (propType.equals(Float.TYPE))) {
                  valueArray[0] = new Float(value);
                } else if ((propType.equals(Double.class)) || (propType.equals(Double.TYPE))) {
                  valueArray[0] = new Double(value);
                } else if ((propType.equals(Boolean.class)) || (propType.equals(Boolean.TYPE))) {
                  valueArray[0] = Boolean.valueOf(value);
                } else {
                  throw new NamingException("String conversion for property type '" + propType.getName() + "' not available");
                }
                Method setProp = pda[i].getWriteMethod();
                if (setProp != null)
                {
                  setProp.invoke(bean, valueArray); break;
                }
                throw new NamingException("Write not allowed for property: " + propName);
              }
            }
            if (i == pda.length) {
              throw new NamingException("No set method found for property: " + propName);
            }
          }
        }
        return bean;
      }
      catch (IntrospectionException ie)
      {
        NamingException ne = new NamingException(ie.getMessage());
        ne.setRootCause(ie);
        throw ne;
      }
      catch (IllegalAccessException iae)
      {
        NamingException ne = new NamingException(iae.getMessage());
        ne.setRootCause(iae);
        throw ne;
      }
      catch (InstantiationException ie2)
      {
        NamingException ne = new NamingException(ie2.getMessage());
        ne.setRootCause(ie2);
        throw ne;
      }
      catch (InvocationTargetException ite)
      {
        Throwable cause = ite.getCause();
        if ((cause instanceof ThreadDeath)) {
          throw ((ThreadDeath)cause);
        }
        if ((cause instanceof VirtualMachineError)) {
          throw ((VirtualMachineError)cause);
        }
        NamingException ne = new NamingException(ite.getMessage());
        ne.setRootCause(ite);
        throw ne;
      }
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\factory\BeanFactory.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */