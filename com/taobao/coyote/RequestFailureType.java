package com.taobao.coyote;

public enum RequestFailureType
{
  PARAMETERS_PARSE_FAILURE,  REQUEST_POST_TOO_LARGE_FAILURE,  REQUEST_CHUNKED_POST_TOO_LARGE_FAILURE,  FILE_UPLOAD_MAX_SIZE_EXCEEDED_FAILURE,  INVALID_CONTENT_TYPE_FAILURE,  FILE_UPLOAD_EXCEPTION,  MAX_POST_SIZE_EXCEEDED_FAILURE;
  
  private RequestFailureType() {}
  
  public static int set(int source, RequestFailureType type)
  {
    if (type.ordinal() > 31) {
      throw new IndexOutOfBoundsException("Only support up to 32 RequestFailureType for an integer source");
    }
    return source | 1 << type.ordinal();
  }
  
  public static boolean get(int source, RequestFailureType type)
  {
    if (type.ordinal() > 31) {
      throw new IndexOutOfBoundsException("Only support up to 32 RequestFailureType for an integer source");
    }
    return (1 << type.ordinal() & source) != 0;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\com\taobao\coyote\RequestFailureType.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */