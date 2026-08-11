package padej.soup.base.util.render;

import net.minecraft.entity.LivingEntity;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;

public class RenderSlot {
   private LivingEntity entity;
   private final Animation animation;
   private long lastActiveTime;
   private long deathTimestamp;

   public RenderSlot(LivingEntity entity, int animationMs) {
      this.entity = entity;
      this.animation = new DecelerateAnimation().setMs(animationMs).setValue(1.0);
   }

   public void setDirection(Direction direction) {
      this.animation.setDirection(direction);
   }

   public float getAnimationDelta() {
      return this.animation.getOutput().floatValue();
   }

   public boolean isFinishedBackwards() {
      return this.animation.isFinished(Direction.BACKWARDS);
   }

   public boolean isActive() {
      return this.animation.isDirection(Direction.FORWARDS);
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public long getLastActiveTime() {
      return this.lastActiveTime;
   }

   public long getDeathTimestamp() {
      return this.deathTimestamp;
   }

   public void setEntity(LivingEntity entity) {
      this.entity = entity;
   }

   public void setLastActiveTime(long lastActiveTime) {
      this.lastActiveTime = lastActiveTime;
   }

   public void setDeathTimestamp(long deathTimestamp) {
      this.deathTimestamp = deathTimestamp;
   }
}
