package imbusy;

import java.lang.Thread;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

public class ExpireMapTest {
  @Test
  public void canAddElements() {
    ConcurrentExpireHashMap<String, String> map = new ConcurrentExpireHashMap<String, String>();
    map.put("hello", "world", 5000);
    assertEquals(map.get("hello"),"world");
  }
   
  @Test
  public void canRemoveElements() {
    ConcurrentExpireHashMap<String, String> map = new ConcurrentExpireHashMap<String, String>();
    map.put("hello", "world", 5000);
    assertEquals(map.get("hello"),"world");
    map.remove("hello");
    assertEquals(map.get("hello"),null);
  }
   
  @Test
  public void elementsTimeout() throws InterruptedException {
    ConcurrentExpireHashMap<String, String> map = new ConcurrentExpireHashMap<String, String>();
    map.put("hello", "world", 500);
    assertEquals(map.get("hello"),"world");
    Thread.sleep(500);
    assertEquals(map.get("hello"),null);
  }
   
  @Test
  public void negativeTimeout() {
    ConcurrentExpireHashMap<String, String> map = new ConcurrentExpireHashMap<String, String>();
    map.put("hello", "world", -100);
    assertEquals(map.get("hello"),null);
  }
}
