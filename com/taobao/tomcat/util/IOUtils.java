package com.taobao.tomcat.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IOUtils
{
  public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");
  private static final int DEFAULT_BUFFER_SIZE = 4096;
  private static final int EOF = -1;
  private static final char WINDOWS_SEPARATOR = '\\';
  private static final char SYSTEM_SEPARATOR = File.separatorChar;
  
  public IOUtils() {}
  
  static boolean isSystemWindows()
  {
    return SYSTEM_SEPARATOR == '\\';
  }
  
  public static void close(Closeable stream)
  {
    if (stream == null) {
      return;
    }
    try
    {
      stream.close();
    }
    catch (IOException ioe) {}
  }
  
  public static void close(Closeable... streams)
  {
    if ((streams == null) || (streams.length == 0)) {
      return;
    }
    for (Closeable c : streams) {
      try
      {
        if (c != null) {
          c.close();
        }
      }
      catch (IOException ioe) {}
    }
  }
  
  public static void writeToFile(byte[] data, File file)
    throws IOException
  {
    if ((data == null) || (file == null)) {
      return;
    }
    BufferedOutputStream bos = null;
    try
    {
      bos = new BufferedOutputStream(new FileOutputStream(file));
      bos.write(data);
    }
    finally
    {
      close(bos);
    }
  }
  
  public static void writeToFile(List<String> strList, File file)
    throws IOException
  {
    if ((strList == null) || (file == null)) {
      return;
    }
    BufferedWriter bw = null;
    try
    {
      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
      for (String line : strList)
      {
        bw.write(line);
        bw.newLine();
      }
    }
    finally
    {
      close(bw);
    }
  }
  
  public static void appendToFile(List<String> strList, File file)
    throws IOException
  {
    if ((strList == null) || (strList.isEmpty()) || (file == null)) {
      return;
    }
    BufferedWriter bw = null;
    try
    {
      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
      for (String str : strList)
      {
        bw.write(str);
        bw.newLine();
      }
    }
    finally
    {
      close(bw);
    }
  }
  
  public static byte[] readFromFile(File file)
    throws IOException
  {
    if (file == null) {
      return null;
    }
    BufferedInputStream bis = null;
    try
    {
      bis = new BufferedInputStream(new FileInputStream(file));
      ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
      byte[] buf = new byte['Ѐ'];
      int len = 0;
      while ((len = bis.read(buf)) > 0) {
        output.write(buf, 0, len);
      }
      return output.toByteArray();
    }
    finally
    {
      close(bis);
    }
  }
  
  public static List<String> readAsStringList(File file)
    throws IOException
  {
    if (file == null) {
      return null;
    }
    BufferedReader br = null;
    List<String> strList = null;
    try
    {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      strList = new ArrayList();
      String line = null;
      while ((line = br.readLine()) != null) {
        strList.add(line);
      }
      return strList;
    }
    finally
    {
      close(br);
    }
  }
  
  /* Error */
  public static void copyFile(File src, File dest)
    throws IOException
  {
    // Byte code:
    //   0: aload_0
    //   1: ifnull +14 -> 15
    //   4: aload_1
    //   5: ifnull +10 -> 15
    //   8: aload_0
    //   9: invokevirtual 40	java/io/File:exists	()Z
    //   12: ifne +4 -> 16
    //   15: return
    //   16: aconst_null
    //   17: astore_2
    //   18: aconst_null
    //   19: astore_3
    //   20: sipush 2048
    //   23: newarray <illegal type>
    //   25: astore 4
    //   27: new 23	java/io/BufferedInputStream
    //   30: dup
    //   31: new 24	java/io/FileInputStream
    //   34: dup
    //   35: aload_0
    //   36: invokespecial 25	java/io/FileInputStream:<init>	(Ljava/io/File;)V
    //   39: invokespecial 26	java/io/BufferedInputStream:<init>	(Ljava/io/InputStream;)V
    //   42: astore_2
    //   43: new 5	java/io/BufferedOutputStream
    //   46: dup
    //   47: new 6	java/io/FileOutputStream
    //   50: dup
    //   51: aload_1
    //   52: invokespecial 7	java/io/FileOutputStream:<init>	(Ljava/io/File;)V
    //   55: invokespecial 8	java/io/BufferedOutputStream:<init>	(Ljava/io/OutputStream;)V
    //   58: astore_3
    //   59: iconst_0
    //   60: istore 5
    //   62: aload_2
    //   63: aload 4
    //   65: invokevirtual 29	java/io/BufferedInputStream:read	([B)I
    //   68: dup
    //   69: istore 5
    //   71: iconst_m1
    //   72: if_icmpeq +15 -> 87
    //   75: aload_3
    //   76: aload 4
    //   78: iconst_0
    //   79: iload 5
    //   81: invokevirtual 41	java/io/BufferedOutputStream:write	([BII)V
    //   84: goto -22 -> 62
    //   87: iconst_2
    //   88: anewarray 42	java/io/Closeable
    //   91: dup
    //   92: iconst_0
    //   93: aload_2
    //   94: aastore
    //   95: dup
    //   96: iconst_1
    //   97: aload_3
    //   98: aastore
    //   99: invokestatic 43	com/taobao/tomcat/util/IOUtils:close	([Ljava/io/Closeable;)V
    //   102: goto +23 -> 125
    //   105: astore 6
    //   107: iconst_2
    //   108: anewarray 42	java/io/Closeable
    //   111: dup
    //   112: iconst_0
    //   113: aload_2
    //   114: aastore
    //   115: dup
    //   116: iconst_1
    //   117: aload_3
    //   118: aastore
    //   119: invokestatic 43	com/taobao/tomcat/util/IOUtils:close	([Ljava/io/Closeable;)V
    //   122: aload 6
    //   124: athrow
    //   125: return
    // Line number table:
    //   Java source line #228	-> byte code offset #0
    //   Java source line #229	-> byte code offset #15
    //   Java source line #231	-> byte code offset #16
    //   Java source line #232	-> byte code offset #18
    //   Java source line #234	-> byte code offset #20
    //   Java source line #235	-> byte code offset #27
    //   Java source line #236	-> byte code offset #43
    //   Java source line #237	-> byte code offset #59
    //   Java source line #238	-> byte code offset #62
    //   Java source line #239	-> byte code offset #75
    //   Java source line #242	-> byte code offset #87
    //   Java source line #243	-> byte code offset #102
    //   Java source line #242	-> byte code offset #105
    //   Java source line #244	-> byte code offset #125
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	126	0	src	File
    //   0	126	1	dest	File
    //   17	97	2	input	BufferedInputStream
    //   19	99	3	output	BufferedOutputStream
    //   25	52	4	buffer	byte[]
    //   60	20	5	len	int
    //   105	18	6	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   20	87	105	finally
    //   105	107	105	finally
  }
  
  public static int copy(InputStream input, OutputStream output)
    throws IOException
  {
    long count = copyLarge(input, output);
    if (count > 2147483647L) {
      return -1;
    }
    return (int)count;
  }
  
  public static long copyLarge(InputStream input, OutputStream output)
    throws IOException
  {
    return copyLarge(input, output, new byte['က']);
  }
  
  public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
    throws IOException
  {
    long count = 0L;
    int n = 0;
    while (-1 != (n = input.read(buffer)))
    {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
  
  public static byte[] toByteArray(InputStream input)
    throws IOException
  {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output);
    return output.toByteArray();
  }
  
  public static String parseString(InputStream is, String charset)
    throws IOException
  {
    if ((is == null) || (charset == null)) {
      throw new IllegalArgumentException("param error");
    }
    return tryParseString(is, new String[] { charset });
  }
  
  /* Error */
  public static String tryParseString(InputStream is, String... charsets)
    throws IOException
  {
	  return "";
    // Byte code:
    //   0: aload_0
    //   1: ifnull +12 -> 13
    //   4: aload_1
    //   5: ifnull +8 -> 13
    //   8: aload_1
    //   9: arraylength
    //   10: ifne +13 -> 23
    //   13: new 52	java/lang/IllegalArgumentException
    //   16: dup
    //   17: ldc 53
    //   19: invokespecial 54	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   22: athrow
    //   23: aconst_null
    //   24: astore_2
    //   25: aconst_null
    //   26: astore_3
    //   27: aconst_null
    //   28: astore 4
    //   30: sipush 128
    //   33: newarray <illegal type>
    //   35: astore 4
    //   37: new 23	java/io/BufferedInputStream
    //   40: dup
    //   41: aload_0
    //   42: invokespecial 26	java/io/BufferedInputStream:<init>	(Ljava/io/InputStream;)V
    //   45: astore_2
    //   46: new 27	java/io/ByteArrayOutputStream
    //   49: dup
    //   50: sipush 128
    //   53: invokespecial 28	java/io/ByteArrayOutputStream:<init>	(I)V
    //   56: astore 5
    //   58: new 5	java/io/BufferedOutputStream
    //   61: dup
    //   62: aload 5
    //   64: invokespecial 8	java/io/BufferedOutputStream:<init>	(Ljava/io/OutputStream;)V
    //   67: astore_3
    //   68: iconst_0
    //   69: istore 6
    //   71: aload_2
    //   72: aload 4
    //   74: invokevirtual 29	java/io/BufferedInputStream:read	([B)I
    //   77: dup
    //   78: istore 6
    //   80: iconst_m1
    //   81: if_icmpeq +15 -> 96
    //   84: aload_3
    //   85: aload 4
    //   87: iconst_0
    //   88: iload 6
    //   90: invokevirtual 41	java/io/BufferedOutputStream:write	([BII)V
    //   93: goto -22 -> 71
    //   96: aload_3
    //   97: invokevirtual 56	java/io/BufferedOutputStream:flush	()V
    //   100: aload 5
    //   102: invokevirtual 31	java/io/ByteArrayOutputStream:toByteArray	()[B
    //   105: astore 4
    //   107: iconst_2
    //   108: anewarray 42	java/io/Closeable
    //   111: dup
    //   112: iconst_0
    //   113: aload_2
    //   114: aastore
    //   115: dup
    //   116: iconst_1
    //   117: aload_3
    //   118: aastore
    //   119: invokestatic 43	com/taobao/tomcat/util/IOUtils:close	([Ljava/io/Closeable;)V
    //   122: goto +23 -> 145
    //   125: astore 7
    //   127: iconst_2
    //   128: anewarray 42	java/io/Closeable
    //   131: dup
    //   132: iconst_0
    //   133: aload_2
    //   134: aastore
    //   135: dup
    //   136: iconst_1
    //   137: aload_3
    //   138: aastore
    //   139: invokestatic 43	com/taobao/tomcat/util/IOUtils:close	([Ljava/io/Closeable;)V
    //   142: aload 7
    //   144: athrow
    //   145: aconst_null
    //   146: astore 5
    //   148: aload_1
    //   149: astore 6
    //   151: aload 6
    //   153: arraylength
    //   154: istore 7
    //   156: iconst_0
    //   157: istore 8
    //   159: iload 8
    //   161: iload 7
    //   163: if_icmpge +46 -> 209
    //   166: aload 6
    //   168: iload 8
    //   170: aaload
    //   171: astore 9
    //   173: aload 9
    //   175: invokestatic 57	java/nio/charset/Charset:forName	(Ljava/lang/String;)Ljava/nio/charset/Charset;
    //   178: invokevirtual 58	java/nio/charset/Charset:newDecoder	()Ljava/nio/charset/CharsetDecoder;
    //   181: astore 10
    //   183: aload 10
    //   185: aload 4
    //   187: invokestatic 59	java/nio/ByteBuffer:wrap	([B)Ljava/nio/ByteBuffer;
    //   190: invokevirtual 60	java/nio/charset/CharsetDecoder:decode	(Ljava/nio/ByteBuffer;)Ljava/nio/CharBuffer;
    //   193: invokevirtual 61	java/nio/CharBuffer:toString	()Ljava/lang/String;
    //   196: areturn
    //   197: astore 10
    //   199: aload 10
    //   201: astore 5
    //   203: iinc 8 1
    //   206: goto -47 -> 159
    //   209: aload 5
    //   211: athrow
    // Line number table:
    //   Java source line #328	-> byte code offset #0
    //   Java source line #329	-> byte code offset #13
    //   Java source line #332	-> byte code offset #23
    //   Java source line #333	-> byte code offset #25
    //   Java source line #334	-> byte code offset #27
    //   Java source line #336	-> byte code offset #30
    //   Java source line #337	-> byte code offset #37
    //   Java source line #338	-> byte code offset #46
    //   Java source line #339	-> byte code offset #58
    //   Java source line #340	-> byte code offset #68
    //   Java source line #341	-> byte code offset #71
    //   Java source line #342	-> byte code offset #84
    //   Java source line #344	-> byte code offset #96
    //   Java source line #346	-> byte code offset #100
    //   Java source line #348	-> byte code offset #107
    //   Java source line #349	-> byte code offset #122
    //   Java source line #348	-> byte code offset #125
    //   Java source line #351	-> byte code offset #145
    //   Java source line #352	-> byte code offset #148
    //   Java source line #355	-> byte code offset #173
    //   Java source line #358	-> byte code offset #183
    //   Java source line #359	-> byte code offset #197
    //   Java source line #360	-> byte code offset #199
    //   Java source line #352	-> byte code offset #203
    //   Java source line #363	-> byte code offset #209
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	212	0	is	InputStream
    //   0	212	1	charsets	String[]
    //   24	110	2	input	BufferedInputStream
    //   26	112	3	output	BufferedOutputStream
    //   28	158	4	buf	byte[]
    //   56	45	5	outBuf	ByteArrayOutputStream
    //   146	64	5	e	IOException
    //   69	20	6	len	int
    //   149	18	6	arr$	String[]
    //   125	18	7	localObject	Object
    //   154	10	7	len$	int
    //   157	47	8	i$	int
    //   171	3	9	charset	String
    //   181	3	10	decoder	java.nio.charset.CharsetDecoder
    //   197	3	10	e1	java.nio.charset.CharacterCodingException
    // Exception table:
    //   from	to	target	type
    //   30	107	125	finally
    //   125	127	125	finally
    //   173	196	197	java/nio/charset/CharacterCodingException
  }
  
  public static File[] listAll(File dir)
  {
    File[] files = null;
    if (dir.exists()) {
      files = dir.listFiles();
    }
    if (files == null) {
      return new File[0];
    }
    return files;
  }
  
  public static File[] listAll(File dir, boolean recursive)
  {
    File[] files = null;
    if (dir.exists()) {
      files = dir.listFiles();
    }
    if (files == null) {
      return new File[0];
    }
    return files;
  }
  
  public static File[] listFiles(File dir, String suffix)
  {
    File[] files = null;
    if (dir.exists()) {
      files = dir.listFiles(new FileFilter()
      {
        public boolean accept(File pathName)
        {
          if ((pathName.isFile()) && (pathName.getName().toLowerCase(Locale.ENGLISH).endsWith(suffix))) {
            return true;
          }
          return false;
        }
      });
    }
    if (files == null) {
      return new File[0];
    }
    return files;
  }
  
  public static File[] listDirs(File dir, String suffix)
  {
    File[] files = null;
    if (dir.exists()) {
      files = dir.listFiles(new FileFilter()
      {
        public boolean accept(File pathName)
        {
          if ((pathName.isDirectory()) && (pathName.getName().toLowerCase(Locale.ENGLISH).endsWith(suffix))) {
            return true;
          }
          return false;
        }
      });
    }
    if (files == null) {
      return new File[0];
    }
    return files;
  }
  
  private static void cleanDirectoryOnExit(File directory)
    throws IOException
  {
    if (!directory.exists())
    {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }
    if (!directory.isDirectory())
    {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }
    File[] files = directory.listFiles();
    if (files == null) {
      throw new IOException("Failed to list contents of " + directory);
    }
    IOException exception = null;
    for (File file : files) {
      try
      {
        forceDeleteOnExit(file);
      }
      catch (IOException ioe)
      {
        exception = ioe;
      }
    }
    if (null != exception) {
      throw exception;
    }
  }
  
  public static void cleanDirectory(File directory)
    throws IOException
  {
    if (!directory.exists())
    {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }
    if (!directory.isDirectory())
    {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }
    File[] files = directory.listFiles();
    if (files == null) {
      throw new IOException("Failed to list contents of " + directory);
    }
    IOException exception = null;
    for (File file : files) {
      try
      {
        forceDelete(file);
      }
      catch (IOException ioe)
      {
        exception = ioe;
      }
    }
    if (null != exception) {
      throw exception;
    }
  }
  
  public static void deleteDirectory(File directory)
    throws IOException
  {
    if (!directory.exists()) {
      return;
    }
    if (!isSymlink(directory)) {
      cleanDirectory(directory);
    }
    if (!directory.delete())
    {
      String message = "Unable to delete directory " + directory + ".";
      throw new IOException(message);
    }
  }
  
  public static boolean isSymlink(File file)
    throws IOException
  {
    if (file == null) {
      throw new NullPointerException("File must not be null");
    }
    if (isSystemWindows()) {
      return false;
    }
    File fileInCanonicalDir = null;
    if (file.getParent() == null)
    {
      fileInCanonicalDir = file;
    }
    else
    {
      File canonicalDir = file.getParentFile().getCanonicalFile();
      fileInCanonicalDir = new File(canonicalDir, file.getName());
    }
    if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
      return false;
    }
    return true;
  }
  
  public static void forceDeleteOnExit(File file)
    throws IOException
  {
    if (file.isDirectory()) {
      deleteDirectoryOnExit(file);
    } else {
      file.deleteOnExit();
    }
  }
  
  public static void forceDelete(File file)
    throws IOException
  {
    if (file.isDirectory())
    {
      deleteDirectory(file);
    }
    else
    {
      boolean filePresent = file.exists();
      if (!file.delete())
      {
        if (!filePresent) {
          throw new FileNotFoundException("File does not exist: " + file);
        }
        String message = "Unable to delete file: " + file;
        throw new IOException(message);
      }
    }
  }
  
  private static void deleteDirectoryOnExit(File directory)
    throws IOException
  {
    if (!directory.exists()) {
      return;
    }
    directory.deleteOnExit();
    if (!isSymlink(directory)) {
      cleanDirectoryOnExit(directory);
    }
  }
}

