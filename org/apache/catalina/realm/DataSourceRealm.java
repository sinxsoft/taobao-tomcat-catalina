package org.apache.catalina.realm;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.naming.Context;
import javax.sql.DataSource;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.juli.logging.Log;
import org.apache.naming.ContextBindings;
import org.apache.tomcat.util.res.StringManager;

public class DataSourceRealm
  extends RealmBase
{
  private String preparedRoles = null;
  private String preparedCredentials = null;
  protected String dataSourceName = null;
  protected static final String info = "org.apache.catalina.realm.DataSourceRealm/1.0";
  protected boolean localDataSource = false;
  protected static final String name = "DataSourceRealm";
  protected String roleNameCol = null;
  protected String userCredCol = null;
  protected String userNameCol = null;
  protected String userRoleTable = null;
  protected String userTable = null;
  
  public DataSourceRealm() {}
  
  public String getDataSourceName()
  {
    return this.dataSourceName;
  }
  
  public void setDataSourceName(String dataSourceName)
  {
    this.dataSourceName = dataSourceName;
  }
  
  public boolean getLocalDataSource()
  {
    return this.localDataSource;
  }
  
  public void setLocalDataSource(boolean localDataSource)
  {
    this.localDataSource = localDataSource;
  }
  
  public String getRoleNameCol()
  {
    return this.roleNameCol;
  }
  
  public void setRoleNameCol(String roleNameCol)
  {
    this.roleNameCol = roleNameCol;
  }
  
  public String getUserCredCol()
  {
    return this.userCredCol;
  }
  
  public void setUserCredCol(String userCredCol)
  {
    this.userCredCol = userCredCol;
  }
  
  public String getUserNameCol()
  {
    return this.userNameCol;
  }
  
  public void setUserNameCol(String userNameCol)
  {
    this.userNameCol = userNameCol;
  }
  
  public String getUserRoleTable()
  {
    return this.userRoleTable;
  }
  
  public void setUserRoleTable(String userRoleTable)
  {
    this.userRoleTable = userRoleTable;
  }
  
  public String getUserTable()
  {
    return this.userTable;
  }
  
  public void setUserTable(String userTable)
  {
    this.userTable = userTable;
  }
  
  public String getInfo()
  {
    return "org.apache.catalina.realm.DataSourceRealm/1.0";
  }
  
  public Principal authenticate(String username, String credentials)
  {
    if ((username == null) || (credentials == null)) {
      return null;
    }
    Connection dbConnection = null;
    
    dbConnection = open();
    if (dbConnection == null) {
      return null;
    }
    Principal principal = authenticate(dbConnection, username, credentials);
    
    close(dbConnection);
    
    return principal;
  }
  
  protected Principal authenticate(Connection dbConnection, String username, String credentials)
  {
    String dbCredentials = getPassword(dbConnection, username);
    
    boolean validated = compareCredentials(credentials, dbCredentials);
    if (validated)
    {
      if (this.containerLog.isTraceEnabled()) {
        this.containerLog.trace(sm.getString("dataSourceRealm.authenticateSuccess", new Object[] { username }));
      }
    }
    else
    {
      if (this.containerLog.isTraceEnabled()) {
        this.containerLog.trace(sm.getString("dataSourceRealm.authenticateFailure", new Object[] { username }));
      }
      return null;
    }
    ArrayList<String> list = getRoles(dbConnection, username);
    
    return new GenericPrincipal(username, credentials, list);
  }
  
  protected void close(Connection dbConnection)
  {
    if (dbConnection == null) {
      return;
    }
    try
    {
      if (!dbConnection.getAutoCommit()) {
        dbConnection.commit();
      }
    }
    catch (SQLException e)
    {
      this.containerLog.error("Exception committing connection before closing:", e);
    }
    try
    {
      dbConnection.close();
    }
    catch (SQLException e)
    {
      this.containerLog.error(sm.getString("dataSourceRealm.close"), e);
    }
  }
  
  protected Connection open()
  {
    try
    {
      Context context = null;
      if (this.localDataSource)
      {
        context = ContextBindings.getClassLoader();
        context = (Context)context.lookup("comp/env");
      }
      else
      {
        context = getServer().getGlobalNamingContext();
      }
      DataSource dataSource = (DataSource)context.lookup(this.dataSourceName);
      return dataSource.getConnection();
    }
    catch (Exception e)
    {
      this.containerLog.error(sm.getString("dataSourceRealm.exception"), e);
    }
    return null;
  }
  
  protected String getName()
  {
    return "DataSourceRealm";
  }
  
  protected String getPassword(String username)
  {
    Connection dbConnection = null;
    
    dbConnection = open();
    if (dbConnection == null) {
      return null;
    }
    try
    {
      return getPassword(dbConnection, username);
    }
    finally
    {
      close(dbConnection);
    }
  }
  
  protected String getPassword(Connection dbConnection, String username)
  {
    ResultSet rs = null;
    PreparedStatement stmt = null;
    String dbCredentials = null;
    try
    {
      stmt = credentials(dbConnection, username);
      rs = stmt.executeQuery();
      if (rs.next()) {
        dbCredentials = rs.getString(1);
      }
      return dbCredentials != null ? dbCredentials.trim() : null;
    }
    catch (SQLException e)
    {
      this.containerLog.error(sm.getString("dataSourceRealm.getPassword.exception", new Object[] { username }), e);
    }
    finally
    {
      try
      {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      }
      catch (SQLException e)
      {
        this.containerLog.error(sm.getString("dataSourceRealm.getPassword.exception", new Object[] { username }), e);
      }
    }
    return null;
  }
  
  protected Principal getPrincipal(String username)
  {
    Connection dbConnection = open();
    if (dbConnection == null) {
      return new GenericPrincipal(username, null, null);
    }
    try
    {
      return new GenericPrincipal(username, getPassword(dbConnection, username), getRoles(dbConnection, username));
    }
    finally
    {
      close(dbConnection);
    }
  }
  
  protected ArrayList<String> getRoles(String username)
  {
    Connection dbConnection = null;
    
    dbConnection = open();
    if (dbConnection == null) {
      return null;
    }
    try
    {
      return getRoles(dbConnection, username);
    }
    finally
    {
      close(dbConnection);
    }
  }
  
  protected ArrayList<String> getRoles(Connection dbConnection, String username)
  {
    if ((this.allRolesMode != RealmBase.AllRolesMode.STRICT_MODE) && (!isRoleStoreDefined())) {
      return null;
    }
    ResultSet rs = null;
    PreparedStatement stmt = null;
    ArrayList<String> list = null;
    try
    {
      stmt = roles(dbConnection, username);
      rs = stmt.executeQuery();
      list = new ArrayList();
      String role;
      while (rs.next())
      {
        role = rs.getString(1);
        if (role != null) {
          list.add(role.trim());
        }
      }
      return list;
    }
    catch (SQLException e)
    {
      this.containerLog.error(sm.getString("dataSourceRealm.getRoles.exception", new Object[] { username }), e);
    }
    finally
    {
      try
      {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      }
      catch (SQLException e)
      {
        this.containerLog.error(sm.getString("dataSourceRealm.getRoles.exception", new Object[] { username }), e);
      }
    }
    return null;
  }
  
  private PreparedStatement credentials(Connection dbConnection, String username)
    throws SQLException
  {
    PreparedStatement credentials = dbConnection.prepareStatement(this.preparedCredentials);
    
    credentials.setString(1, username);
    return credentials;
  }
  
  private PreparedStatement roles(Connection dbConnection, String username)
    throws SQLException
  {
    PreparedStatement roles = dbConnection.prepareStatement(this.preparedRoles);
    
    roles.setString(1, username);
    return roles;
  }
  
  private boolean isRoleStoreDefined()
  {
    return (this.userRoleTable != null) || (this.roleNameCol != null);
  }
  
  protected void startInternal()
    throws LifecycleException
  {
    StringBuilder temp = new StringBuilder("SELECT ");
    temp.append(this.roleNameCol);
    temp.append(" FROM ");
    temp.append(this.userRoleTable);
    temp.append(" WHERE ");
    temp.append(this.userNameCol);
    temp.append(" = ?");
    this.preparedRoles = temp.toString();
    
    temp = new StringBuilder("SELECT ");
    temp.append(this.userCredCol);
    temp.append(" FROM ");
    temp.append(this.userTable);
    temp.append(" WHERE ");
    temp.append(this.userNameCol);
    temp.append(" = ?");
    this.preparedCredentials = temp.toString();
    
    super.startInternal();
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\DataSourceRealm.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */