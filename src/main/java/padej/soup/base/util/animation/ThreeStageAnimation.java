package padej.soup.base.util.animation;

public class ThreeStageAnimation {
   private final double appearDuration;
   private final double existDuration;
   private final double disappearDuration;
   private final double totalDuration;
   private final Interpolation appearInterpolation;
   private final Interpolation disappearInterpolation;

   public ThreeStageAnimation(
      double appearDuration, double existDuration, double disappearDuration, Interpolation appearInterpolation, Interpolation disappearInterpolation
   ) {
      this.appearDuration = appearDuration;
      this.existDuration = existDuration;
      this.disappearDuration = disappearDuration;
      this.totalDuration = appearDuration + Math.max(0.0, existDuration) + disappearDuration;
      this.appearInterpolation = appearInterpolation;
      this.disappearInterpolation = disappearInterpolation;
   }

   public double getValue(double elapsedTime) {
      if (elapsedTime >= this.totalDuration) {
         return 0.0;
      } else if (this.existDuration < 0.0) {
         double overlapStart = this.appearDuration + this.existDuration;
         if (elapsedTime <= overlapStart) {
            double progress = elapsedTime / this.appearDuration;
            return this.appearInterpolation.interpolate(progress);
         } else if (elapsedTime <= this.appearDuration) {
            double appearProgress = elapsedTime / this.appearDuration;
            double appearValue = this.appearInterpolation.interpolate(appearProgress);
            double disappearProgress = (elapsedTime - overlapStart) / this.disappearDuration;
            double disappearValue = 1.0 - this.disappearInterpolation.interpolate(disappearProgress);
            return Math.min(appearValue, disappearValue);
         } else {
            double disappearProgress = (elapsedTime - overlapStart) / this.disappearDuration;
            return 1.0 - this.disappearInterpolation.interpolate(disappearProgress);
         }
      } else if (elapsedTime <= this.appearDuration) {
         double progress = elapsedTime / this.appearDuration;
         return this.appearInterpolation.interpolate(progress);
      } else if (elapsedTime <= this.appearDuration + this.existDuration) {
         return 1.0;
      } else {
         double progress = (elapsedTime - this.appearDuration - this.existDuration) / this.disappearDuration;
         return 1.0 - this.disappearInterpolation.interpolate(progress);
      }
   }

   public ThreeStageAnimation.AnimationStage getStage(double elapsedTime) {
      if (this.existDuration < 0.0) {
         double overlapStart = this.appearDuration + this.existDuration;
         if (elapsedTime <= overlapStart) {
            return ThreeStageAnimation.AnimationStage.APPEAR;
         } else if (elapsedTime <= this.appearDuration) {
            return ThreeStageAnimation.AnimationStage.APPEAR;
         } else {
            return elapsedTime <= this.totalDuration ? ThreeStageAnimation.AnimationStage.DISAPPEAR : ThreeStageAnimation.AnimationStage.FINISHED;
         }
      } else if (elapsedTime <= this.appearDuration) {
         return ThreeStageAnimation.AnimationStage.APPEAR;
      } else if (elapsedTime <= this.appearDuration + this.existDuration) {
         return ThreeStageAnimation.AnimationStage.EXIST;
      } else {
         return elapsedTime <= this.totalDuration ? ThreeStageAnimation.AnimationStage.DISAPPEAR : ThreeStageAnimation.AnimationStage.FINISHED;
      }
   }

   public boolean isFinished(double elapsedTime) {
      return elapsedTime >= this.totalDuration;
   }

   public double getAppearDuration() {
      return this.appearDuration;
   }

   public double getExistDuration() {
      return this.existDuration;
   }

   public double getDisappearDuration() {
      return this.disappearDuration;
   }

   public static enum AnimationStage {
      APPEAR,
      EXIST,
      DISAPPEAR,
      FINISHED;
   }
}
