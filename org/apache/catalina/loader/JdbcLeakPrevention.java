package org.apache.catalina.loader;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

public class JdbcLeakPrevention
{
  public JdbcLeakPrevention() {}
  
  public List<String> clearJdbcDriverRegistrations()
    throws SQLException
  {
    List<String> driverNames = new ArrayList();
    
    HashSet<Driver> originalDrivers = new HashSet();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      originalDrivers.add(drivers.nextElement());
    }
    drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements())
    {
      Driver driver = (Driver)drivers.nextElement();
      if (driver.getClass().getClassLoader() == getClass().getClassLoader())
      {
        if (originalDrivers.contains(driver)) {
          driverNames.add(driver.getClass().getCanonicalName());
        }
        DriverManager.deregisterDriver(driver);
      }
    }
    return driverNames;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\loader\JdbcLeakPrevention.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */