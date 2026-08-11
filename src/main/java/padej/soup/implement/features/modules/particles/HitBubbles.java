package padej.soup.implement.features.modules.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.EventAttack;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.animation.Interpolations;
import padej.soup.base.util.animation.ThreeStageAnimation;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.other.StopWatch;
import padej.soup.base.util.render.HitBubblesRenderer;

public class HitBubbles extends Module {
   private final List<HitBubbles.Bubble> bubbles = new ArrayList<>();
   private boolean wasLastAttackCrit = false;
   public final SelectSetting colorMode = new SelectSetting("setting.hitbubbles.colormode.name", "setting.hitbubbles.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   public final SelectSetting customColorsCount = new SelectSetting("setting.hitbubbles.colorcount.name", "setting.hitbubbles.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final MultiColorSetting customColors = new MultiColorSetting("setting.hitbubbles.gradientcolors.name", "setting.hitbubbles.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final SelectSetting colorAnimation = new SelectSetting("setting.hitbubbles.coloranimation.name", "setting.hitbubbles.coloranimation.desc")
      .value("Wave", "Vertexes")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting bubbleTexture = new SelectSetting("setting.hitbubbles.bubblestyle.name", "setting.hitbubbles.bubblestyle.desc")
      .value("Default", "Bold", "Portal", "Soup")
      .selected("Portal");
   public final SelectSetting animationType = new SelectSetting("setting.hitbubbles.animationtype.name", "setting.hitbubbles.animationtype.desc")
      .value("Fade", "Scale", "Both")
      .selected("Both");
   public final ValueSetting appearDuration = new ValueSetting("setting.hitbubbles.appearduration.name", "setting.hitbubbles.appearduration.desc")
      .setValue(0.3F)
      .range(0.1F, 3.0F);
   public final ValueSetting existDuration = new ValueSetting("setting.hitbubbles.existduration.name", "setting.hitbubbles.existduration.desc")
      .setValue(0.5F)
      .range(0.1F, 3.0F);
   public final ValueSetting disappearDuration = new ValueSetting("setting.hitbubbles.disappearduration.name", "setting.hitbubbles.disappearduration.desc")
      .setValue(0.5F)
      .range(0.1F, 3.0F);
   public final SelectSetting appearInterpolation = new SelectSetting(
         "setting.hitbubbles.appearinterpolation.name", "setting.hitbubbles.appearinterpolation.desc"
      )
      .value(Interpolations.getAllNames())
      .selected("Bounce");
   public final SelectSetting disappearInterpolation = new SelectSetting(
         "setting.hitbubbles.disappearinterpolation.name", "setting.hitbubbles.disappearinterpolation.desc"
      )
      .value(Interpolations.getAllNames())
      .selected("Smooth");
   public final ValueSetting rotateSpeed = new ValueSetting("setting.hitbubbles.rotatespeed.name", "setting.hitbubbles.rotatespeed.desc")
      .setValue(2.0F)
      .range(0.5F, 5.0F);
   public final ValueSetting scale = new ValueSetting("setting.hitbubbles.scale.name", "setting.hitbubbles.scale.desc").setValue(0.75F).range(0.5F, 2.0F);
   public final BooleanSetting onlyCrits = new BooleanSetting("setting.hitbubbles.onlycrits.name", "setting.hitbubbles.onlycrits.desc").setValue(false);

   public static HitBubbles getInstance() {
      return Instance.get(HitBubbles.class);
   }

   public HitBubbles() {
      super("module.hitbubbles.name", ModuleCategory.PARTICLES);
      GroupSetting colorGroup = new GroupSetting("group.hitbubbles.colors.name", "group.hitbubbles.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting durationGroup = new GroupSetting("group.hitbubbles.duration.name", "group.hitbubbles.duration.desc", false)
         .settings(this.appearDuration, this.existDuration, this.disappearDuration);
      GroupSetting animationGroup = new GroupSetting("group.hitbubbles.animation.name", "group.hitbubbles.animation.desc", false)
         .settings(this.appearInterpolation, this.disappearInterpolation, this.rotateSpeed, this.scale);
      this.setup(new Setting[]{colorGroup, this.bubbleTexture, this.animationType, durationGroup, animationGroup, this.onlyCrits});
   }

   private boolean isCrit() {
      PlayerEntity player = mc.player;
      if (player == null) {
         return false;
      } else {
         boolean bl = player.getAttackCooldownProgress(0.5F) > 0.9F;
         return bl
            && player.fallDistance > 0.0F
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !player.hasVehicle()
            && !player.isSprinting();
      }
   }

   @EventHandler
   public void onAttack(EventAttack event) {
      if (mc.player != null && mc.world != null) {
         if (event.isPre()) {
            if (mc.player.getAttackCooldownProgress(0.5F) > 0.9F) {
               this.wasLastAttackCrit = this.isCrit();
            }
         } else if (event.getTarget() != null) {
            if (!this.onlyCrits.isValue() || this.wasLastAttackCrit) {
               Vec3d point = event.getHitPos();
               if (point != null) {
                  Vec3d fromPos = mc.player.getEyePos();
                  Vec3d direction = point.subtract(fromPos).normalize();
                  float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
                  float pitch = (float)(-Math.toDegrees(Math.asin(direction.y)));
                  this.bubbles.add(new HitBubbles.Bubble(point, -yaw, pitch, new StopWatch(), mc.player));
               }
            }
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.world != null && mc.player != null) {
         this.bubbles.removeIf(bubblex -> {
            double elapsedSeconds = bubblex.timer.elapsedTime() / 1000.0;
            double totalDuration = this.getTotalAnimationDuration();
            return elapsedSeconds > totalDuration;
         });
         if (!this.bubbles.isEmpty()) {
            for (HitBubbles.Bubble bubble : this.bubbles) {
               bubble.updateRotation(this);
               bubble.updateAnimation(this);
               HitBubblesRenderer.renderBubble(bubble.pos, bubble.yaw, bubble.pitch, bubble.timer, bubble.rotationAngle, this, bubble.animation);
            }
         }
      }
   }

   @Override
   public void deactivate() {
      this.bubbles.clear();
   }

   public int[] getCustomColors() {
      if (!this.colorMode.isSelected("Custom")) {
         return null;
      } else {
         String var1 = this.customColorsCount.getSelected();

         return switch (var1) {
            case "Solo" -> new int[]{this.customColors.getColor1().getColor()};
            case "Duo" -> new int[]{this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor()};
            case "Triple" -> new int[]{
               this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor(), this.customColors.getColor3().getColor()
            };
            case "Quartet" -> this.customColors.getColorValues();
            default -> null;
         };
      }
   }

   public double getTotalAnimationDuration() {
      return this.appearDuration.getValue() + this.existDuration.getValue() + this.disappearDuration.getValue();
   }

   public ThreeStageAnimation createAnimation() {
      return new ThreeStageAnimation(
         this.appearDuration.getValue(),
         this.existDuration.getValue(),
         this.disappearDuration.getValue(),
         Interpolations.getByName(this.appearInterpolation.getSelected()),
         Interpolations.getByName(this.disappearInterpolation.getSelected())
      );
   }

   public Identifier getBubbleTexture() {
      String var1 = this.bubbleTexture.getSelected();

      return switch (var1) {
         case "Default" -> Identifier.of("textures/circles/circle.png");
         case "Bold" -> Identifier.of("textures/circles/circle_bold.png");
         case "Portal" -> Identifier.of("textures/circles/portal.png");
         case "Soup" -> Identifier.of("textures/circles/soup.png");
         default -> null;
      };
   }

   public List<HitBubbles.Bubble> getBubbles() {
      return this.bubbles;
   }

   public boolean isWasLastAttackCrit() {
      return this.wasLastAttackCrit;
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public SelectSetting getCustomColorsCount() {
      return this.customColorsCount;
   }

   public SelectSetting getColorAnimation() {
      return this.colorAnimation;
   }

   public SelectSetting getAnimationType() {
      return this.animationType;
   }

   public ValueSetting getAppearDuration() {
      return this.appearDuration;
   }

   public ValueSetting getExistDuration() {
      return this.existDuration;
   }

   public ValueSetting getDisappearDuration() {
      return this.disappearDuration;
   }

   public SelectSetting getAppearInterpolation() {
      return this.appearInterpolation;
   }

   public SelectSetting getDisappearInterpolation() {
      return this.disappearInterpolation;
   }

   public ValueSetting getRotateSpeed() {
      return this.rotateSpeed;
   }

   public ValueSetting getScale() {
      return this.scale;
   }

   public BooleanSetting getOnlyCrits() {
      return this.onlyCrits;
   }

   private static class Bubble {
      final Vec3d pos;
      final float yaw;
      final float pitch;
      final StopWatch timer;
      final PlayerEntity player;
      float rotationAngle;
      final float angularVelocity;
      long lastUpdateTime;
      ThreeStageAnimation animation;

      Bubble(Vec3d pos, float yaw, float pitch, StopWatch timer, PlayerEntity player) {
         this.pos = pos;
         this.yaw = yaw;
         this.pitch = pitch;
         this.timer = timer;
         this.player = player;
         this.rotationAngle = 0.0F;
         this.angularVelocity = (float)ThreadLocalRandom.current().nextDouble(1.0, 3.0);
         this.lastUpdateTime = System.currentTimeMillis();
      }

      void updateAnimation(HitBubbles module) {
         if (this.animation == null) {
            this.animation = module.createAnimation();
         }
      }

      void updateRotation(HitBubbles module) {
         long currentTime = System.currentTimeMillis();
         float deltaTime = (float)(currentTime - this.lastUpdateTime) / 1000.0F;
         this.lastUpdateTime = currentTime;
         deltaTime = Math.min(deltaTime, 0.1F);
         double elapsedSeconds = this.timer.elapsedTime() / 1000.0;
         if (this.animation != null) {
            ThreeStageAnimation.AnimationStage stage = this.animation.getStage(elapsedSeconds);
            float frameTime = 0.016666668F;
            float normalizedDelta = deltaTime / frameTime;
            switch (stage) {
               case APPEAR:
                  this.rotationAngle = this.rotationAngle - this.angularVelocity * normalizedDelta * module.rotateSpeed.getValue();
                  break;
               case EXIST:
                  this.rotationAngle = this.rotationAngle - this.angularVelocity * normalizedDelta * module.rotateSpeed.getValue();
                  break;
               case DISAPPEAR:
                  double disappearProgress = (elapsedSeconds - this.animation.getAppearDuration() - this.animation.getExistDuration())
                     / this.animation.getDisappearDuration();
                  float fadeFactor = (float)(1.0 - disappearProgress);
                  this.rotationAngle = this.rotationAngle - this.angularVelocity * normalizedDelta * fadeFactor * module.rotateSpeed.getValue();
               case FINISHED:
            }
         }
      }
   }
}
