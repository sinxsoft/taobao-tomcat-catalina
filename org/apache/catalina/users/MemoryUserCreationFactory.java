package org.apache.catalina.users;

import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.tomcat.util.digester.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

class MemoryUserCreationFactory
  extends AbstractObjectCreationFactory
{
  public MemoryUserCreationFactory(MemoryUserDatabase database)
  {
    this.database = database;
  }
  
  public Object createObject(Attributes attributes)
  {
    String username = attributes.getValue("username");
    if (username == null) {
      username = attributes.getValue("name");
    }
    String password = attributes.getValue("password");
    String fullName = attributes.getValue("fullName");
    if (fullName == null) {
      fullName = attributes.getValue("fullname");
    }
    String groups = attributes.getValue("groups");
    String roles = attributes.getValue("roles");
    User user = this.database.createUser(username, password, fullName);
    if (groups != null) {
      while (groups.length() > 0)
      {
        String groupname = null;
        int comma = groups.indexOf(',');
        if (comma >= 0)
        {
          groupname = groups.substring(0, comma).trim();
          groups = groups.substring(comma + 1);
        }
        else
        {
          groupname = groups.trim();
          groups = "";
        }
        if (groupname.length() > 0)
        {
          Group group = this.database.findGroup(groupname);
          if (group == null) {
            group = this.database.createGroup(groupname, null);
          }
          user.addGroup(group);
        }
      }
    }
    if (roles != null) {
      while (roles.length() > 0)
      {
        String rolename = null;
        int comma = roles.indexOf(',');
        if (comma >= 0)
        {
          rolename = roles.substring(0, comma).trim();
          roles = roles.substring(comma + 1);
        }
        else
        {
          rolename = roles.trim();
          roles = "";
        }
        if (rolename.length() > 0)
        {
          Role role = this.database.findRole(rolename);
          if (role == null) {
            role = this.database.createRole(rolename, null);
          }
          user.addRole(role);
        }
      }
    }
    return user;
  }
  
  private MemoryUserDatabase database = null;
}


