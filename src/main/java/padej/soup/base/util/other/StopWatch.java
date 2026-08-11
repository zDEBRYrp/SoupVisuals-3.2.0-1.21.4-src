package padej.soup.base.util.other;

public class StopWatch {
   private long startTime;

   public StopWatch() {
      this.reset();
   }

   public boolean finished(double delay) {
      return System.currentTimeMillis() - delay >= this.startTime;
   }

   public void reset() {
      this.startTime = System.currentTimeMillis();
   }

   public int elapsedTime() {
      return Math.toIntExact(System.currentTimeMillis() - this.startTime);
   }

   public StopWatch setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
      return this;
   }

   public long getStartTime() {
      return this.startTime;
   }
}
