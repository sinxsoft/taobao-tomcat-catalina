package org.apache.catalina.realm;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.catalina.Container;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

public class JAASCallbackHandler
  implements CallbackHandler
{
  public JAASCallbackHandler(JAASRealm realm, String username, String password)
  {
    this.realm = realm;
    this.username = username;
    if (realm.hasMessageDigest()) {
      this.password = realm.digest(password);
    } else {
      this.password = password;
    }
  }
  
  public JAASCallbackHandler(JAASRealm realm, String username, String password, String nonce, String nc, String cnonce, String qop, String realmName, String md5a2, String authMethod)
  {
    this(realm, username, password);
    this.nonce = nonce;
    this.nc = nc;
    this.cnonce = cnonce;
    this.qop = qop;
    this.realmName = realmName;
    this.md5a2 = md5a2;
    this.authMethod = authMethod;
  }
  
  protected static final StringManager sm = StringManager.getManager("org.apache.catalina.realm");
  protected String password = null;
  protected JAASRealm realm = null;
  protected String username = null;
  protected String nonce = null;
  protected String nc = null;
  protected String cnonce = null;
  protected String qop;
  protected String realmName;
  protected String md5a2;
  protected String authMethod;
  
  public void handle(Callback[] callbacks)
    throws IOException, UnsupportedCallbackException
  {
    for (int i = 0; i < callbacks.length; i++) {
      if ((callbacks[i] instanceof NameCallback))
      {
        if (this.realm.getContainer().getLogger().isTraceEnabled()) {
          this.realm.getContainer().getLogger().trace(sm.getString("jaasCallback.username", new Object[] { this.username }));
        }
        ((NameCallback)callbacks[i]).setName(this.username);
      }
      else if ((callbacks[i] instanceof PasswordCallback))
      {
        char[] passwordcontents;
        
        if (this.password != null) {
          passwordcontents = this.password.toCharArray();
        } else {
          passwordcontents = new char[0];
        }
        ((PasswordCallback)callbacks[i]).setPassword(passwordcontents);
      }
      else if ((callbacks[i] instanceof TextInputCallback))
      {
        TextInputCallback cb = (TextInputCallback)callbacks[i];
        if (cb.getPrompt().equals("nonce")) {
          cb.setText(this.nonce);
        } else if (cb.getPrompt().equals("nc")) {
          cb.setText(this.nc);
        } else if (cb.getPrompt().equals("cnonce")) {
          cb.setText(this.cnonce);
        } else if (cb.getPrompt().equals("qop")) {
          cb.setText(this.qop);
        } else if (cb.getPrompt().equals("realmName")) {
          cb.setText(this.realmName);
        } else if (cb.getPrompt().equals("md5a2")) {
          cb.setText(this.md5a2);
        } else if (cb.getPrompt().equals("authMethod")) {
          cb.setText(this.authMethod);
        } else {
          throw new UnsupportedCallbackException(callbacks[i]);
        }
      }
      else
      {
        throw new UnsupportedCallbackException(callbacks[i]);
      }
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\realm\JAASCallbackHandler.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */