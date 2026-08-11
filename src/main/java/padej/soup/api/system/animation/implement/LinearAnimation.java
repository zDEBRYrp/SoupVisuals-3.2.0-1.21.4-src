package padej.soup.api.system.animation.implement;

import padej.soup.api.system.animation.Animation;

public class LinearAnimation extends Animation {
   @Override
   public double calculation(double value) {
      return value / this.ms;
   }
}
