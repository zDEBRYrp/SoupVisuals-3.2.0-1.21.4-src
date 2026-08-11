package padej.soup.base.util.animation;

public class Interpolations {
   public static final Interpolation LINEAR = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return progress;
      }

      @Override
      public String getName() {
         return "Linear";
      }
   };
   public static final Interpolation EASE_IN = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return progress * progress;
      }

      @Override
      public String getName() {
         return "Smooth";
      }
   };
   public static final Interpolation EASE_OUT = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return 1.0 - Math.pow(1.0 - progress, 2.0);
      }

      @Override
      public String getName() {
         return "Fast";
      }
   };
   public static final Interpolation EASE_IN_OUT = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
      }

      @Override
      public String getName() {
         return "Balanced";
      }
   };
   public static final Interpolation EASE_OUT_BACK = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return 1.0 + 2.70158 * Math.pow(progress - 1.0, 3.0) + 1.70158 * Math.pow(progress - 1.0, 2.0);
      }

      @Override
      public String getName() {
         return "Back";
      }
   };
   public static final Interpolation EASE_IN_BACK = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         double c1 = 1.70158;
         double c3 = c1 + 1.0;
         return c3 * progress * progress * progress - c1 * progress * progress;
      }

      @Override
      public String getName() {
         return "Overshoot";
      }
   };
   public static final Interpolation ELASTIC = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         if (progress == 0.0) {
            return 0.0;
         } else if (progress == 1.0) {
            return 1.0;
         } else {
            double c4 = Math.PI * 2.0 / 3.0;
            return Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * c4) + 1.0;
         }
      }

      @Override
      public String getName() {
         return "Elastic";
      }
   };
   public static final Interpolation BOUNCE = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         double n1 = 7.5625;
         double d1 = 2.75;
         if (progress < 1.0 / d1) {
            return n1 * progress * progress;
         } else if (progress < 2.0 / d1) {
            double var9;
            return n1 * (var9 = progress - 1.5 / d1) * var9 + 0.75;
         } else {
            double var7;
            double var8;
            return progress < 2.5 / d1 ? n1 * (var7 = progress - 2.25 / d1) * var7 + 0.9375 : n1 * (var8 = progress - 2.625 / d1) * var8 + 0.984375;
         }
      }

      @Override
      public String getName() {
         return "Bounce";
      }
   };
   public static final Interpolation IOS_EASE_OUT = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return 1.0 - Math.pow(1.0 - progress, 3.0);
      }

      @Override
      public String getName() {
         return "Ease Out";
      }
   };
   public static final Interpolation IOS_SPRING = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         double damping = 0.7;
         double frequency = 1.5;
         double decay = Math.exp(-damping * frequency * progress);
         double oscillation = Math.cos(frequency * progress * Math.PI * 2.0);
         return 1.0 - (decay * oscillation * 0.1 + decay * (1.0 - progress));
      }

      @Override
      public String getName() {
         return "Spring";
      }
   };
   public static final Interpolation IOS_DECELERATE = new Interpolation() {
      @Override
      public double interpolate(double progress) {
         return progress * (2.0 - progress);
      }

      @Override
      public String getName() {
         return "Decelerate";
      }
   };

   public static Interpolation getByName(String name) {
      return switch (name) {
         case "Linear" -> LINEAR;
         case "Smooth" -> EASE_IN;
         case "Fast" -> EASE_OUT;
         case "Balanced" -> EASE_IN_OUT;
         case "Back" -> EASE_OUT_BACK;
         case "Overshoot" -> EASE_IN_BACK;
         case "Elastic" -> ELASTIC;
         case "Bounce" -> BOUNCE;
         case "Ease Out" -> IOS_EASE_OUT;
         case "Spring" -> IOS_SPRING;
         case "Decelerate" -> IOS_DECELERATE;
         default -> LINEAR;
      };
   }

   public static String[] getAllNames() {
      return new String[]{"Linear", "Smooth", "Fast", "Balanced", "Back", "Overshoot", "Elastic", "Bounce", "Ease Out", "Spring", "Decelerate"};
   }
}
