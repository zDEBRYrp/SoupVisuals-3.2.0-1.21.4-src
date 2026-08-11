package padej.soup.base.util.math;

public class Counter {
   public long lastMS = System.currentTimeMillis();

   public Counter() {
      this.resetCounter();
   }

   public static Counter create() {
      return new Counter();
   }

   public void resetCounter() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean isReached(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public long getLastMS() {
      return this.lastMS;
   }
}
