package org.apache.catalina.startup;

import java.util.ArrayList;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.digester.CallMethodRule;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.SAXException;

final class CallMethodMultiRule
  extends CallMethodRule
{
  protected int multiParamIndex = 0;
  
  public CallMethodMultiRule(String methodName, int paramCount, int multiParamIndex)
  {
    super(methodName, paramCount);
    this.multiParamIndex = multiParamIndex;
  }
  
  public void end(String namespace, String name)
    throws Exception
  {
    Object[] parameters = null;
    if (this.paramCount > 0)
    {
      parameters = (Object[])this.digester.popParams();
    }
    else
    {
      parameters = new Object[0];
      super.end(namespace, name);
    }
    ArrayList<?> multiParams = (ArrayList)parameters[this.multiParamIndex];
    
    Object[] paramValues = new Object[this.paramTypes.length];
    for (int i = 0; i < this.paramTypes.length; i++) {
      if (i != this.multiParamIndex) {
        if ((parameters[i] == null) || (((parameters[i] instanceof String)) && (!String.class.isAssignableFrom(this.paramTypes[i])))) {
          paramValues[i] = IntrospectionUtils.convert((String)parameters[i], this.paramTypes[i]);
        } else {
          paramValues[i] = parameters[i];
        }
      }
    }
    Object target;

    if (this.targetOffset >= 0) {
      target = this.digester.peek(this.targetOffset);
    } else {
      target = this.digester.peek(this.digester.getCount() + this.targetOffset);
    }
    if (target == null)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("[CallMethodRule]{");
      sb.append("");
      sb.append("} Call target is null (");
      sb.append("targetOffset=");
      sb.append(this.targetOffset);
      sb.append(",stackdepth=");
      sb.append(this.digester.getCount());
      sb.append(")");
      throw new SAXException(sb.toString());
    }
    if (multiParams == null)
    {
      paramValues[this.multiParamIndex] = null;
      IntrospectionUtils.callMethodN(target, this.methodName, paramValues, this.paramTypes);
      
      return;
    }
    for (int j = 0; j < multiParams.size(); j++)
    {
      Object param = multiParams.get(j);
      if ((param == null) || (((param instanceof String)) && (!String.class.isAssignableFrom(this.paramTypes[this.multiParamIndex])))) {
        paramValues[this.multiParamIndex] = IntrospectionUtils.convert((String)param, this.paramTypes[this.multiParamIndex]);
      } else {
        paramValues[this.multiParamIndex] = param;
      }
      IntrospectionUtils.callMethodN(target, this.methodName, paramValues, this.paramTypes);
    }
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\startup\CallMethodMultiRule.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */