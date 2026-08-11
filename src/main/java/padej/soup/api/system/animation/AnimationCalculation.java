package padej.soup.api.system.animation;

public interface AnimationCalculation {
   default double calculation(double value) {
      return 0.0;
   }
}
