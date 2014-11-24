package imbusy;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentHashMap;
import static java.lang.System.currentTimeMillis;

import imbusy.ExpireMap;

public class ConcurrentExpireHashMap<K,V> implements ExpireMap<K,V> {
  
  // store values together with their expiration time in milliseconds
  public static class ValueTimestamp<V> {
    public ValueTimestamp(V value, long timeoutMs) {
      this.value = value;
      this.timeout = currentTimeMillis() + timeoutMs;
    }
    public V value;
    public long timeout;
  }
   
  // the underlying hash map
  private ConcurrentHashMap<K, ValueTimestamp<V>> hashMap;
   
  public ConcurrentExpireHashMap() {
    hashMap = new ConcurrentHashMap<K, ValueTimestamp<V>>();
  }
   
  public void put(K key, V value, long timeoutMs) {
    if(timeoutMs > 0) {
      hashMap.put(key, new ValueTimestamp<V>(value, timeoutMs));
    }
  }
   
  public V get(K key) {
    ValueTimestamp value = hashMap.get(key);
    if(value != null) {
      if(value.timeout <= currentTimeMillis()) {
        return null;
      } else {
        return (V)value.value;
      }
    }
    return null;
  }
   
  public void remove(K key) {
    hashMap.remove(key);
  }
}
