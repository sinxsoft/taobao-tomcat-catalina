package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

public class JDBCStore
  extends StoreBase
{
  protected static final String info = "JDBCStore/1.0";
  private String name = null;
  protected static String storeName = "JDBCStore";
  protected String threadName = "JDBCStore";
  protected String connectionName = null;
  protected String connectionPassword = null;
  protected String connectionURL = null;
  private Connection dbConnection = null;
  protected Driver driver = null;
  protected String driverName = null;
  protected String dataSourceName = null;
  protected DataSource dataSource = null;
  protected String sessionTable = "tomcat$sessions";
  protected String sessionAppCol = "app";
  protected String sessionIdCol = "id";
  protected String sessionDataCol = "data";
  protected String sessionValidCol = "valid";
  protected String sessionMaxInactiveCol = "maxinactive";
  protected String sessionLastAccessedCol = "lastaccess";
  protected PreparedStatement preparedSizeSql = null;
  protected PreparedStatement preparedKeysSql = null;
  protected PreparedStatement preparedSaveSql = null;
  protected PreparedStatement preparedClearSql = null;
  protected PreparedStatement preparedRemoveSql = null;
  protected PreparedStatement preparedLoadSql = null;
  
  public JDBCStore() {}
  
  public String getInfo()
  {
    return "JDBCStore/1.0";
  }
  
  public String getName()
  {
    if (this.name == null)
    {
      Container container = this.manager.getContainer();
      String contextName = container.getName();
      if (!contextName.startsWith("/")) {
        contextName = "/" + contextName;
      }
      String hostName = "";
      String engineName = "";
      if (container.getParent() != null)
      {
        Container host = container.getParent();
        hostName = host.getName();
        if (host.getParent() != null) {
          engineName = host.getParent().getName();
        }
      }
      this.name = ("/" + engineName + "/" + hostName + contextName);
    }
    return this.name;
  }
  
  public String getThreadName()
  {
    return this.threadName;
  }
  
  public String getStoreName()
  {
    return storeName;
  }
  
  public void setDriverName(String driverName)
  {
    String oldDriverName = this.driverName;
    this.driverName = driverName;
    this.support.firePropertyChange("driverName", oldDriverName, this.driverName);
    
    this.driverName = driverName;
  }
  
  public String getDriverName()
  {
    return this.driverName;
  }
  
  public String getConnectionName()
  {
    return this.connectionName;
  }
  
  public void setConnectionName(String connectionName)
  {
    this.connectionName = connectionName;
  }
  
  public String getConnectionPassword()
  {
    return this.connectionPassword;
  }
  
  public void setConnectionPassword(String connectionPassword)
  {
    this.connectionPassword = connectionPassword;
  }
  
  public void setConnectionURL(String connectionURL)
  {
    String oldConnString = this.connectionURL;
    this.connectionURL = connectionURL;
    this.support.firePropertyChange("connectionURL", oldConnString, this.connectionURL);
  }
  
  public String getConnectionURL()
  {
    return this.connectionURL;
  }
  
  public void setSessionTable(String sessionTable)
  {
    String oldSessionTable = this.sessionTable;
    this.sessionTable = sessionTable;
    this.support.firePropertyChange("sessionTable", oldSessionTable, this.sessionTable);
  }
  
  public String getSessionTable()
  {
    return this.sessionTable;
  }
  
  public void setSessionAppCol(String sessionAppCol)
  {
    String oldSessionAppCol = this.sessionAppCol;
    this.sessionAppCol = sessionAppCol;
    this.support.firePropertyChange("sessionAppCol", oldSessionAppCol, this.sessionAppCol);
  }
  
  public String getSessionAppCol()
  {
    return this.sessionAppCol;
  }
  
  public void setSessionIdCol(String sessionIdCol)
  {
    String oldSessionIdCol = this.sessionIdCol;
    this.sessionIdCol = sessionIdCol;
    this.support.firePropertyChange("sessionIdCol", oldSessionIdCol, this.sessionIdCol);
  }
  
  public String getSessionIdCol()
  {
    return this.sessionIdCol;
  }
  
  public void setSessionDataCol(String sessionDataCol)
  {
    String oldSessionDataCol = this.sessionDataCol;
    this.sessionDataCol = sessionDataCol;
    this.support.firePropertyChange("sessionDataCol", oldSessionDataCol, this.sessionDataCol);
  }
  
  public String getSessionDataCol()
  {
    return this.sessionDataCol;
  }
  
  public void setSessionValidCol(String sessionValidCol)
  {
    String oldSessionValidCol = this.sessionValidCol;
    this.sessionValidCol = sessionValidCol;
    this.support.firePropertyChange("sessionValidCol", oldSessionValidCol, this.sessionValidCol);
  }
  
  public String getSessionValidCol()
  {
    return this.sessionValidCol;
  }
  
  public void setSessionMaxInactiveCol(String sessionMaxInactiveCol)
  {
    String oldSessionMaxInactiveCol = this.sessionMaxInactiveCol;
    this.sessionMaxInactiveCol = sessionMaxInactiveCol;
    this.support.firePropertyChange("sessionMaxInactiveCol", oldSessionMaxInactiveCol, this.sessionMaxInactiveCol);
  }
  
  public String getSessionMaxInactiveCol()
  {
    return this.sessionMaxInactiveCol;
  }
  
  public void setSessionLastAccessedCol(String sessionLastAccessedCol)
  {
    String oldSessionLastAccessedCol = this.sessionLastAccessedCol;
    this.sessionLastAccessedCol = sessionLastAccessedCol;
    this.support.firePropertyChange("sessionLastAccessedCol", oldSessionLastAccessedCol, this.sessionLastAccessedCol);
  }
  
  public String getSessionLastAccessedCol()
  {
    return this.sessionLastAccessedCol;
  }
  
  public void setDataSourceName(String dataSourceName)
  {
    if ((dataSourceName == null) || ("".equals(dataSourceName.trim())))
    {
      this.manager.getContainer().getLogger().warn(sm.getString(getStoreName() + ".missingDataSourceName"));
      
      return;
    }
    this.dataSourceName = dataSourceName;
  }
  
  public String getDataSourceName()
  {
    return this.dataSourceName;
  }
  
  public String[] keys()
    throws IOException
  {
    ResultSet rst = null;
    String[] keys = null;
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return new String[0];
        }
        try
        {
          if (this.preparedKeysSql == null)
          {
            String keysSql = "SELECT " + this.sessionIdCol + " FROM " + this.sessionTable + " WHERE " + this.sessionAppCol + " = ?";
            
            this.preparedKeysSql = _conn.prepareStatement(keysSql);
          }
          this.preparedKeysSql.setString(1, getName());
          rst = this.preparedKeysSql.executeQuery();
          ArrayList<String> tmpkeys = new ArrayList();
          if (rst != null) {
            while (rst.next()) {
              tmpkeys.add(rst.getString(1));
            }
          }
          keys = (String[])tmpkeys.toArray(new String[tmpkeys.size()]);
          
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          keys = new String[0];
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        finally
        {
          try
          {
            if (rst != null) {
              rst.close();
            }
          }
          catch (SQLException e) {}
          release(_conn);
        }
        numberOfTries--;
      }
    }
    return keys;
  }
  
  public int getSize()
    throws IOException
  {
    int size = 0;
    ResultSet rst = null;
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return size;
        }
        try
        {
          if (this.preparedSizeSql == null)
          {
            String sizeSql = "SELECT COUNT(" + this.sessionIdCol + ") FROM " + this.sessionTable + " WHERE " + this.sessionAppCol + " = ?";
            
            this.preparedSizeSql = _conn.prepareStatement(sizeSql);
          }
          this.preparedSizeSql.setString(1, getName());
          rst = this.preparedSizeSql.executeQuery();
          if (rst.next()) {
            size = rst.getInt(1);
          }
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        finally
        {
          try
          {
            if (rst != null) {
              rst.close();
            }
          }
          catch (SQLException e) {}
          release(_conn);
        }
        numberOfTries--;
      }
    }
    return size;
  }
  
  public Session load(String id)
    throws ClassNotFoundException, IOException
  {
    ResultSet rst = null;
    StandardSession _session = null;
    Loader loader = null;
    ClassLoader classLoader = null;
    ObjectInputStream ois = null;
    BufferedInputStream bis = null;
    Container container = this.manager.getContainer();
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return null;
        }
        ClassLoader oldThreadContextCL = Thread.currentThread().getContextClassLoader();
        try
        {
          if (this.preparedLoadSql == null)
          {
            String loadSql = "SELECT " + this.sessionIdCol + ", " + this.sessionDataCol + " FROM " + this.sessionTable + " WHERE " + this.sessionIdCol + " = ? AND " + this.sessionAppCol + " = ?";
            
            this.preparedLoadSql = _conn.prepareStatement(loadSql);
          }
          this.preparedLoadSql.setString(1, id);
          this.preparedLoadSql.setString(2, getName());
          rst = this.preparedLoadSql.executeQuery();
          if (rst.next())
          {
            bis = new BufferedInputStream(rst.getBinaryStream(2));
            if (container != null) {
              loader = container.getLoader();
            }
            if (loader != null) {
              classLoader = loader.getClassLoader();
            }
            if (classLoader != null)
            {
              Thread.currentThread().setContextClassLoader(classLoader);
              ois = new CustomObjectInputStream(bis, classLoader);
            }
            else
            {
              ois = new ObjectInputStream(bis);
            }
            if (this.manager.getContainer().getLogger().isDebugEnabled()) {
              this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".loading", new Object[] { id, this.sessionTable }));
            }
            _session = (StandardSession)this.manager.createEmptySession();
            _session.readObjectData(ois);
            _session.setManager(this.manager);
          }
          else if (this.manager.getContainer().getLogger().isDebugEnabled())
          {
            this.manager.getContainer().getLogger().debug(getStoreName() + ": No persisted data object found");
          }
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        finally
        {
          try
          {
            if (rst != null) {
              rst.close();
            }
          }
          catch (SQLException e) {}
          if (ois != null) {
            try
            {
              ois.close();
            }
            catch (IOException e) {}
          }
          Thread.currentThread().setContextClassLoader(oldThreadContextCL);
          release(_conn);
        }
        numberOfTries--;
      }
    }
    return _session;
  }
  
  public void remove(String id)
    throws IOException
  {
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return;
        }
        try
        {
          remove(id, _conn);
          
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        finally
        {
          release(_conn);
        }
        numberOfTries--;
      }
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".removing", new Object[] { id, this.sessionTable }));
    }
  }
  
  private void remove(String id, Connection _conn)
    throws SQLException
  {
    if (this.preparedRemoveSql == null)
    {
      String removeSql = "DELETE FROM " + this.sessionTable + " WHERE " + this.sessionIdCol + " = ?  AND " + this.sessionAppCol + " = ?";
      
      this.preparedRemoveSql = _conn.prepareStatement(removeSql);
    }
    this.preparedRemoveSql.setString(1, id);
    this.preparedRemoveSql.setString(2, getName());
    this.preparedRemoveSql.execute();
  }
  
  public void clear()
    throws IOException
  {
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return;
        }
        try
        {
          if (this.preparedClearSql == null)
          {
            String clearSql = "DELETE FROM " + this.sessionTable + " WHERE " + this.sessionAppCol + " = ?";
            
            this.preparedClearSql = _conn.prepareStatement(clearSql);
          }
          this.preparedClearSql.setString(1, getName());
          this.preparedClearSql.execute();
          
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        finally
        {
          release(_conn);
        }
        numberOfTries--;
      }
    }
  }
  
  public void save(Session session)
    throws IOException
  {
    ObjectOutputStream oos = null;
    ByteArrayOutputStream bos = null;
    ByteArrayInputStream bis = null;
    InputStream in = null;
    synchronized (this)
    {
      int numberOfTries = 2;
      while (numberOfTries > 0)
      {
        Connection _conn = getConnection();
        if (_conn == null) {
          return;
        }
        try
        {
          remove(session.getIdInternal(), _conn);
          
          bos = new ByteArrayOutputStream();
          oos = new ObjectOutputStream(new BufferedOutputStream(bos));
          
          ((StandardSession)session).writeObjectData(oos);
          oos.close();
          oos = null;
          byte[] obs = bos.toByteArray();
          int size = obs.length;
          bis = new ByteArrayInputStream(obs, 0, size);
          in = new BufferedInputStream(bis, size);
          if (this.preparedSaveSql == null)
          {
            String saveSql = "INSERT INTO " + this.sessionTable + " (" + this.sessionIdCol + ", " + this.sessionAppCol + ", " + this.sessionDataCol + ", " + this.sessionValidCol + ", " + this.sessionMaxInactiveCol + ", " + this.sessionLastAccessedCol + ") VALUES (?, ?, ?, ?, ?, ?)";
            
            this.preparedSaveSql = _conn.prepareStatement(saveSql);
          }
          this.preparedSaveSql.setString(1, session.getIdInternal());
          this.preparedSaveSql.setString(2, getName());
          this.preparedSaveSql.setBinaryStream(3, in, size);
          this.preparedSaveSql.setString(4, session.isValid() ? "1" : "0");
          this.preparedSaveSql.setInt(5, session.getMaxInactiveInterval());
          this.preparedSaveSql.setLong(6, session.getLastAccessedTime());
          this.preparedSaveSql.execute();
          
          numberOfTries = 0;
        }
        catch (SQLException e)
        {
          this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".SQLException", new Object[] { e }));
          if (this.dbConnection != null) {
            close(this.dbConnection);
          }
        }
        catch (IOException e) {}finally
        {
          if (oos != null) {
            oos.close();
          }
          if (bis != null) {
            bis.close();
          }
          if (in != null) {
            in.close();
          }
          release(_conn);
        }
        numberOfTries--;
      }
    }
    if (this.manager.getContainer().getLogger().isDebugEnabled()) {
      this.manager.getContainer().getLogger().debug(sm.getString(getStoreName() + ".saving", new Object[] { session.getIdInternal(), this.sessionTable }));
    }
  }
  
  protected Connection getConnection()
  {
    Connection conn = null;
    try
    {
      conn = open();
      if ((conn == null) || (conn.isClosed()))
      {
        this.manager.getContainer().getLogger().info(sm.getString(getStoreName() + ".checkConnectionDBClosed"));
        conn = open();
        if ((conn == null) || (conn.isClosed())) {
          this.manager.getContainer().getLogger().info(sm.getString(getStoreName() + ".checkConnectionDBReOpenFail"));
        }
      }
    }
    catch (SQLException ex)
    {
      this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".checkConnectionSQLException", new Object[] { ex.toString() }));
    }
    return conn;
  }
  
  protected Connection open()
    throws SQLException
  {
    if (this.dbConnection != null) {
      return this.dbConnection;
    }
    if ((this.dataSourceName != null) && (this.dataSource == null)) {
      try
      {
        Context initCtx = new InitialContext();
        Context envCtx = (Context)initCtx.lookup("java:comp/env");
        this.dataSource = ((DataSource)envCtx.lookup(this.dataSourceName));
      }
      catch (NamingException e)
      {
        this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".wrongDataSource", new Object[] { this.dataSourceName }), e);
      }
    }
    if (this.dataSource != null) {
      return this.dataSource.getConnection();
    }
    if (this.driver == null) {
      try
      {
        Class<?> clazz = Class.forName(this.driverName);
        this.driver = ((Driver)clazz.newInstance());
      }
      catch (ClassNotFoundException ex)
      {
        this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException", new Object[] { ex.toString() }));
      }
      catch (InstantiationException ex)
      {
        this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException", new Object[] { ex.toString() }));
      }
      catch (IllegalAccessException ex)
      {
        this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException", new Object[] { ex.toString() }));
      }
    }
    Properties props = new Properties();
    if (this.connectionName != null) {
      props.put("user", this.connectionName);
    }
    if (this.connectionPassword != null) {
      props.put("password", this.connectionPassword);
    }
    this.dbConnection = this.driver.connect(this.connectionURL, props);
    this.dbConnection.setAutoCommit(true);
    return this.dbConnection;
  }
  
  protected void close(Connection dbConnection)
  {
    if (dbConnection == null) {
      return;
    }
    try
    {
      this.preparedSizeSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    this.preparedSizeSql = null;
    try
    {
      this.preparedKeysSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    this.preparedKeysSql = null;
    try
    {
      this.preparedSaveSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    this.preparedSaveSql = null;
    try
    {
      this.preparedClearSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    try
    {
      this.preparedRemoveSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    this.preparedRemoveSql = null;
    try
    {
      this.preparedLoadSql.close();
    }
    catch (Throwable f)
    {
      ExceptionUtils.handleThrowable(f);
    }
    this.preparedLoadSql = null;
    try
    {
      if (!dbConnection.getAutoCommit()) {
        dbConnection.commit();
      }
    }
    catch (SQLException e)
    {
      this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".commitSQLException"), e);
    }
    try
    {
      dbConnection.close();
    }
    catch (SQLException e)
    {
      this.manager.getContainer().getLogger().error(sm.getString(getStoreName() + ".close", new Object[] { e.toString() }));
    }
    finally
    {
      this.dbConnection = null;
    }
  }
  
  protected void release(Connection conn)
  {
    if (this.dataSource != null) {
      close(conn);
    }
  }
  
  protected synchronized void startInternal()
    throws LifecycleException
  {
    if (this.dataSourceName == null) {
      this.dbConnection = getConnection();
    }
    super.startInternal();
  }
  
  protected synchronized void stopInternal()
    throws LifecycleException
  {
    super.stopInternal();
    if (this.dbConnection != null)
    {
      try
      {
        this.dbConnection.commit();
      }
      catch (SQLException e) {}
      close(this.dbConnection);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\session\JDBCStore.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */