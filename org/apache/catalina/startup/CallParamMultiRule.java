package org.apache.catalina.startup;

import java.util.ArrayList;
import org.apache.tomcat.util.digester.ArrayStack;
import org.apache.tomcat.util.digester.CallParamRule;
import org.apache.tomcat.util.digester.Digester;

final class CallParamMultiRule
  extends CallParamRule
{
  public CallParamMultiRule(int paramIndex)
  {
    super(paramIndex);
  }
  
  public void end(String namespace, String name)
  {
    if ((this.bodyTextStack != null) && (!this.bodyTextStack.empty()))
    {
      Object[] parameters = (Object[])this.digester.peekParams();
      
      ArrayList<String> params = (ArrayList)parameters[this.paramIndex];
      if (params == null)
      {
        params = new ArrayList();
        parameters[this.paramIndex] = params;
      }
      params.add(this.bodyTextStack.pop());
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\CallParamMultiRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */