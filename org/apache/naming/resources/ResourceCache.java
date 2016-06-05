package org.apache.naming.resources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class ResourceCache
{
  protected Random random = new Random();
  protected CacheEntry[] cache = new CacheEntry[0];
  protected HashMap<String, CacheEntry> notFoundCache = new HashMap();
  protected int cacheMaxSize = 10240;
  protected int maxAllocateIterations = 20;
  protected long desiredEntryAccessRatio = 3L;
  protected int spareNotFoundEntries = 500;
  protected int cacheSize = 0;
  protected long accessCount = 0L;
  protected long hitsCount = 0L;
  
  public ResourceCache() {}
  
  public long getAccessCount()
  {
    return this.accessCount;
  }
  
  public int getCacheMaxSize()
  {
    return this.cacheMaxSize;
  }
  
  public void setCacheMaxSize(int cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize;
  }
  
  public int getCacheSize()
  {
    return this.cacheSize;
  }
  
  @Deprecated
  public long getDesiredEntryAccessRatio()
  {
    return this.desiredEntryAccessRatio;
  }
  
  @Deprecated
  public void setDesiredEntryAccessRatio(long desiredEntryAccessRatio)
  {
    this.desiredEntryAccessRatio = desiredEntryAccessRatio;
  }
  
  public long getHitsCount()
  {
    return this.hitsCount;
  }
  
  @Deprecated
  public int getMaxAllocateIterations()
  {
    return this.maxAllocateIterations;
  }
  
  @Deprecated
  public void setMaxAllocateIterations(int maxAllocateIterations)
  {
    this.maxAllocateIterations = maxAllocateIterations;
  }
  
  @Deprecated
  public int getSpareNotFoundEntries()
  {
    return this.spareNotFoundEntries;
  }
  
  @Deprecated
  public void setSpareNotFoundEntries(int spareNotFoundEntries)
  {
    this.spareNotFoundEntries = spareNotFoundEntries;
  }
  
  public boolean allocate(int space)
  {
    int toFree = space - (this.cacheMaxSize - this.cacheSize);
    if (toFree <= 0) {
      return true;
    }
    toFree += this.cacheMaxSize / 20;
    
    int size = this.notFoundCache.size();
    if (size > this.spareNotFoundEntries)
    {
      this.notFoundCache.clear();
      this.cacheSize -= size;
      toFree -= size;
    }
    if (toFree <= 0) {
      return true;
    }
    int attempts = 0;
    int entriesFound = 0;
    long totalSpace = 0L;
    int[] toRemove = new int[this.maxAllocateIterations];
    while (toFree > 0)
    {
      if (attempts == this.maxAllocateIterations) {
        return false;
      }
      if (toFree > 0)
      {
        int entryPos = -1;
        boolean unique = false;
        while (!unique)
        {
          unique = true;
          entryPos = this.random.nextInt(this.cache.length);
          for (int i = 0; i < entriesFound; i++) {
            if (toRemove[i] == entryPos) {
              unique = false;
            }
          }
        }
        long entryAccessRatio = this.cache[entryPos].accessCount * 100L / this.accessCount;
        if (entryAccessRatio < this.desiredEntryAccessRatio)
        {
          toRemove[entriesFound] = entryPos;
          totalSpace += this.cache[entryPos].size;
          toFree -= this.cache[entryPos].size;
          entriesFound++;
        }
      }
      attempts++;
    }
    Arrays.sort(toRemove, 0, entriesFound);
    CacheEntry[] newCache = new CacheEntry[this.cache.length - entriesFound];
    int pos = 0;
    int n = -1;
    if (entriesFound > 0)
    {
      n = toRemove[0];
      for (int i = 0; i < this.cache.length; i++) {
        if (i == n)
        {
          if (pos + 1 < entriesFound)
          {
            n = toRemove[(pos + 1)];
            pos++;
          }
          else
          {
            pos++;
            n = -1;
          }
        }
        else {
          newCache[(i - pos)] = this.cache[i];
        }
      }
    }
    this.cache = newCache;
    this.cacheSize = ((int)(this.cacheSize - totalSpace));
    
    return true;
  }
  
  public CacheEntry lookup(String name)
  {
    CacheEntry cacheEntry = null;
    CacheEntry[] currentCache = this.cache;
    this.accessCount += 1L;
    int pos = find(currentCache, name);
    if ((pos != -1) && (name.equals(currentCache[pos].name))) {
      cacheEntry = currentCache[pos];
    }
    if (cacheEntry == null) {
      try
      {
        cacheEntry = (CacheEntry)this.notFoundCache.get(name);
      }
      catch (Exception e) {}
    }
    if (cacheEntry != null) {
      this.hitsCount += 1L;
    }
    return cacheEntry;
  }
  
  public void load(CacheEntry entry)
  {
    if (entry.exists)
    {
      if (insertCache(entry)) {
        this.cacheSize += entry.size;
      }
    }
    else
    {
      int sizeIncrement = this.notFoundCache.get(entry.name) == null ? 1 : 0;
      this.notFoundCache.put(entry.name, entry);
      this.cacheSize += sizeIncrement;
    }
  }
  
  public boolean unload(String name)
  {
    CacheEntry removedEntry = removeCache(name);
    if (removedEntry != null)
    {
      this.cacheSize -= removedEntry.size;
      return true;
    }
    if (this.notFoundCache.remove(name) != null)
    {
      this.cacheSize -= 1;
      return true;
    }
    return false;
  }
  
  private static final int find(CacheEntry[] map, String name)
  {
    int a = 0;
    int b = map.length - 1;
    if (b == -1) {
      return -1;
    }
    if (name.compareTo(map[0].name) < 0) {
      return -1;
    }
    if (b == 0) {
      return 0;
    }
    int i = 0;
    for (;;)
    {
      i = b + a >>> 1;
      int result = name.compareTo(map[i].name);
      if (result > 0)
      {
        a = i;
      }
      else
      {
        if (result == 0) {
          return i;
        }
        b = i;
      }
      if (b - a == 1)
      {
        int result2 = name.compareTo(map[b].name);
        if (result2 < 0) {
          return a;
        }
        return b;
      }
    }
  }
  
  private final boolean insertCache(CacheEntry newElement)
  {
    CacheEntry[] oldCache = this.cache;
    int pos = find(oldCache, newElement.name);
    if ((pos != -1) && (newElement.name.equals(oldCache[pos].name))) {
      return false;
    }
    CacheEntry[] newCache = new CacheEntry[this.cache.length + 1];
    System.arraycopy(oldCache, 0, newCache, 0, pos + 1);
    newCache[(pos + 1)] = newElement;
    System.arraycopy(oldCache, pos + 1, newCache, pos + 2, oldCache.length - pos - 1);
    
    this.cache = newCache;
    return true;
  }
  
  private final CacheEntry removeCache(String name)
  {
    CacheEntry[] oldCache = this.cache;
    int pos = find(oldCache, name);
    if ((pos != -1) && (name.equals(oldCache[pos].name)))
    {
      CacheEntry[] newCache = new CacheEntry[this.cache.length - 1];
      System.arraycopy(oldCache, 0, newCache, 0, pos);
      System.arraycopy(oldCache, pos + 1, newCache, pos, oldCache.length - pos - 1);
      
      this.cache = newCache;
      return oldCache[pos];
    }
    return null;
  }
}


/* Location:              D:\F\阿里云架构开发\taobao-tomcat-7.0.59\taobao-tomcat-7.0.59\lib\catalina.jar!\org\apache\naming\resources\ResourceCache.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */