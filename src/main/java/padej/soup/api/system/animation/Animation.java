package padej.soup.api.system.animation;

import padej.soup.base.util.math.Counter;

public abstract class Animation implements AnimationCalculation {
   private final Counter counter = new Counter();
   protected int ms;
   protected double value;
   protected Direction direction = Direction.FORWARDS;

   public int getMs() {
      return this.ms;
   }

   public void reset() {
      this.counter.resetCounter();
   }

   public boolean isDone() {
      return this.counter.isReached(this.ms);
   }

   public boolean isFinished(Direction direction) {
      return this.direction == direction && this.isDone();
   }

   public void setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
         this.adjustTimer();
      }
   }

   public boolean isDirection(Direction direction) {
      return this.direction == direction;
   }

   private void adjustTimer() {
      this.counter.setTime(System.currentTimeMillis() - (this.ms - Math.min((long)this.ms, this.counter.getTime())));
   }

   public Double getOutput() {
      return this.getOutputDouble();
   }

   public double getOutputDouble() {
      if (this.direction == Direction.FORWARDS) {
         return this.isDone() ? this.value : this.calculation(this.counter.getTime()) * this.value;
      } else {
         return this.isDone() ? 0.0 : (1.0 - this.calculation(this.counter.getTime())) * this.value;
      }
   }

   public float getOutputFloat() {
      return (float)this.getOutputDouble();
   }

   public Animation setMs(int ms) {
      this.ms = ms;
      return this;
   }

   public Animation setValue(double value) {
      this.value = value;
      return this;
   }
}
