package imbusy;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentHashMap;
import static java.lang.System.currentTimeMillis;
import java.util.Comparator;
import java.util.Iterator;

import imbusy.ExpireMap;

public class ConcurrentExpireHashMap<K,V> implements ExpireMap<K,V> {
  
  // store values together with their expiration time in milliseconds
  // in the underlying hash map.
  private static class ValueTimestamp<V> {
    public ValueTimestamp(V value, long timeoutMs) {
      this.value = value;
      this.timeout = currentTimeMillis() + timeoutMs;
    }
    public V value;
    public long timeout;
  }
   
  // the underlying hash map
  private ConcurrentHashMap<K, ValueTimestamp<V>> hashMap;
  
  // used in RemovalLoop to store the exact key, value, timestamp
  // triples to be removed
  private static class KeyValueTimestamp<K, V> {
    public KeyValueTimestamp(K key, ValueTimestamp<V> valueTimestamp) {
      this.key = key;
      this.valueTimestamp = valueTimestamp;
    }
    public K key;
    public ValueTimestamp<V> valueTimestamp;
    
    @Override
    public boolean equals(Object o) {
      KeyValueTimestamp<K, V> other = (KeyValueTimestamp<K, V>)o;
      if(other == null) return false;
      if(this.key == other.key &&
         this.valueTimestamp.value == other.valueTimestamp.value &&
         this.valueTimestamp.timeout == other.valueTimestamp.timeout) {
        return true;
      }
      return false;
    }
  }
    
  // a task that removes elements from the hash map once they expire
  private class RemovalLoop implements Runnable {
    
    // an ordered set where we store element expiration times
    ConcurrentSkipListSet<KeyValueTimestamp<K,V>> expirationSet;
    
    public RemovalLoop() {
      expirationSet = new ConcurrentSkipListSet<KeyValueTimestamp<K,V>>(
        new Comparator<KeyValueTimestamp<K, V>>() { // order by timestamp
          public int compare(KeyValueTimestamp<K, V> a, KeyValueTimestamp<K, V> b) {
            return a.valueTimestamp.timeout > b.valueTimestamp.timeout ? 1 
                    : a.valueTimestamp.timeout < b.valueTimestamp.timeout ? -1 : 0;
          }
        }
      );
    }
    
    public void run() {
      while(true) {
        long nextTimestamp = -1;
        long curTimestamp = currentTimeMillis();
        
        // remove elements that timed out and figure out the length
        // of time to sleep in milliseconds till next expiration
        for(KeyValueTimestamp<K, V> element : expirationSet) {
          if(element.valueTimestamp.timeout <= curTimestamp) {
            hashMap.remove(element.key, element.valueTimestamp);
            expirationSet.remove(element);
          } else {
            nextTimestamp = element.valueTimestamp.timeout;
            break;
          }
        }
        
        try {
          synchronized(this) {
            if(nextTimestamp > 0) {
              wait(nextTimestamp - curTimestamp);
            } else {
              wait(1000);
            }
          }
        } catch(InterruptedException e) {
        }
      }
    }
    
    public void newValue(K key, ValueTimestamp vt) {
      expirationSet.add(new KeyValueTimestamp(key, vt));
      synchronized(this) {
        notify();
      }
    }
  }
  
  RemovalLoop removalLoop;
  
  public ConcurrentExpireHashMap() {
    hashMap = new ConcurrentHashMap<K, ValueTimestamp<V>>();
    
    removalLoop = new RemovalLoop();
    Thread t = new Thread(removalLoop);
    t.start();
  }
   
  public void put(K key, V value, long timeoutMs) {
    if(timeoutMs > 0) {
      ValueTimestamp<V> vt = new ValueTimestamp<V>(value, timeoutMs);
      hashMap.put(key, vt);
      removalLoop.newValue(key, vt);
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
