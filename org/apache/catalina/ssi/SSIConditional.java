package org.apache.catalina.ssi;

import java.io.PrintWriter;
import java.text.ParseException;

public class SSIConditional
  implements SSICommand
{
  public SSIConditional() {}
  
  public long process(SSIMediator ssiMediator, String commandName, String[] paramNames, String[] paramValues, PrintWriter writer)
    throws SSIStopProcessingException
  {
    long lastModified = System.currentTimeMillis();
    
    SSIConditionalState state = ssiMediator.getConditionalState();
    if ("if".equalsIgnoreCase(commandName))
    {
      if (state.processConditionalCommandsOnly)
      {
        state.nestingCount += 1;
        return lastModified;
      }
      state.nestingCount = 0;
      if (evaluateArguments(paramNames, paramValues, ssiMediator))
      {
        state.branchTaken = true;
      }
      else
      {
        state.processConditionalCommandsOnly = true;
        state.branchTaken = false;
      }
    }
    else if ("elif".equalsIgnoreCase(commandName))
    {
      if (state.nestingCount > 0) {
        return lastModified;
      }
      if (state.branchTaken)
      {
        state.processConditionalCommandsOnly = true;
        return lastModified;
      }
      if (evaluateArguments(paramNames, paramValues, ssiMediator))
      {
        state.processConditionalCommandsOnly = false;
        state.branchTaken = true;
      }
      else
      {
        state.processConditionalCommandsOnly = true;
        state.branchTaken = false;
      }
    }
    else if ("else".equalsIgnoreCase(commandName))
    {
      if (state.nestingCount > 0) {
        return lastModified;
      }
      state.processConditionalCommandsOnly = state.branchTaken;
      
      state.branchTaken = true;
    }
    else if ("endif".equalsIgnoreCase(commandName))
    {
      if (state.nestingCount > 0)
      {
        state.nestingCount -= 1;
        return lastModified;
      }
      state.processConditionalCommandsOnly = false;
      
      state.branchTaken = true;
    }
    else
    {
      throw new SSIStopProcessingException();
    }
    return lastModified;
  }
  
  private boolean evaluateArguments(String[] names, String[] values, SSIMediator ssiMediator)
    throws SSIStopProcessingException
  {
    String expr = getExpression(names, values);
    if (expr == null) {
      throw new SSIStopProcessingException();
    }
    try
    {
      ExpressionParseTree tree = new ExpressionParseTree(expr, ssiMediator);
      
      return tree.evaluateTree();
    }
    catch (ParseException e)
    {
      throw new SSIStopProcessingException();
    }
  }
  
  private String getExpression(String[] paramNames, String[] paramValues)
  {
    if ("expr".equalsIgnoreCase(paramNames[0])) {
      return paramValues[0];
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\catalina\ssi\SSIConditional.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */