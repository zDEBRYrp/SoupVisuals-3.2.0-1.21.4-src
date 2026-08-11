package padej.soup.api.system.animation.implement;

import padej.soup.api.system.animation.Animation;

public class EaseInOutAnimation extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      if (x < 0.5) {
         return 4.0 * x * x * x;
      } else {
         double f = 2.0 * x - 2.0;
         return 1.0 - f * f * f / 2.0;
      }
   }
}
