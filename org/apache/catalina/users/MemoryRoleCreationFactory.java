package org.apache.catalina.users;

import org.apache.catalina.Role;
import org.apache.tomcat.util.digester.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

class MemoryRoleCreationFactory
  extends AbstractObjectCreationFactory
{
  public MemoryRoleCreationFactory(MemoryUserDatabase database)
  {
    this.database = database;
  }
  
  public Object createObject(Attributes attributes)
  {
    String rolename = attributes.getValue("rolename");
    if (rolename == null) {
      rolename = attributes.getValue("name");
    }
    String description = attributes.getValue("description");
    Role role = this.database.createRole(rolename, description);
    return role;
  }
  
  private MemoryUserDatabase database = null;
}

