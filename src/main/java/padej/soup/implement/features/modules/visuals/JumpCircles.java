package padej.soup.implement.features.modules.visuals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.JumpEvent;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.QuickImports;
import padej.soup.base.util.animation.Interpolations;
import padej.soup.base.util.animation.ThreeStageAnimation;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.other.StopWatch;
import padej.soup.base.util.render.JumpCircleRenderer;

public class JumpCircles extends Module {
   private final List<JumpCircles.Circle> circles = new ArrayList<>();
   public final SelectSetting colorMode = new SelectSetting("setting.jumpcircles.colormode.name", "setting.jumpcircles.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   public final SelectSetting customColorsCount = new SelectSetting("setting.jumpcircles.colorcount.name", "setting.jumpcircles.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final MultiColorSetting customColors = new MultiColorSetting("setting.jumpcircles.gradientcolors.name", "setting.jumpcircles.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final SelectSetting colorAnimation = new SelectSetting("setting.jumpcircles.coloranimation.name", "setting.jumpcircles.coloranimation.desc")
      .value("Wave", "Vertexes")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting circleTexture = new SelectSetting("setting.jumpcircles.circlestyle.name", "setting.jumpcircles.circlestyle.desc")
      .value("Default", "Bold", "Portal", "Soup")
      .selected("Default");
   public final SelectSetting appearAnimationType = new SelectSetting(
         "setting.jumpcircles.appearanimationtype.name", "setting.jumpcircles.appearanimationtype.desc"
      )
      .value("Fade", "Scale", "Both")
      .selected("Both");
   public final SelectSetting disappearAnimationType = new SelectSetting(
         "setting.jumpcircles.disappearanimationtype.name", "setting.jumpcircles.disappearanimationtype.desc"
      )
      .value("Fade", "Scale", "Both")
      .selected("Both");
   public final ValueSetting appearDuration = new ValueSetting("setting.jumpcircles.appearduration.name", "setting.jumpcircles.appearduration.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F);
   public final ValueSetting existDuration = new ValueSetting("setting.jumpcircles.existduration.name", "setting.jumpcircles.existduration.desc")
      .setValue(1.5F)
      .range(-3.0F, 3.0F);
   public final ValueSetting disappearDuration = new ValueSetting("setting.jumpcircles.disappearduration.name", "setting.jumpcircles.disappearduration.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F);
   public final SelectSetting appearInterpolation = new SelectSetting(
         "setting.jumpcircles.appearinterpolation.name", "setting.jumpcircles.appearinterpolation.desc"
      )
      .value(Interpolations.getAllNames())
      .selected("Bounce");
   public final SelectSetting disappearInterpolation = new SelectSetting(
         "setting.jumpcircles.disappearinterpolation.name", "setting.jumpcircles.disappearinterpolation.desc"
      )
      .value(Interpolations.getAllNames())
      .selected("Smooth");
   public final ValueSetting rotateSpeed = new ValueSetting("setting.jumpcircles.rotatespeed.name", "setting.jumpcircles.rotatespeed.desc")
      .setValue(2.0F)
      .range(0.5F, 5.0F);
   public final ValueSetting circleScale = new ValueSetting("setting.jumpcircles.circlescale.name", "setting.jumpcircles.circlescale.desc")
      .setValue(1.0F)
      .range(0.5F, 3.0F);
   public final ValueSetting bright = new ValueSetting("setting.jumpcircles.bright.name", "setting.jumpcircles.bright.desc").setValue(1.0F).range(1, 5);

   public static JumpCircles getInstance() {
      return Instance.get(JumpCircles.class);
   }

   public JumpCircles() {
      super("module.jumpcircles.name", ModuleCategory.VISUALS);
      GroupSetting colorGroup = new GroupSetting("group.jumpcircles.colors.name", "group.jumpcircles.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting appearanceGroup = new GroupSetting("group.jumpcircles.appearance.name", "group.jumpcircles.appearance.desc", false)
         .settings(this.circleTexture, this.appearAnimationType, this.disappearAnimationType);
      GroupSetting durationGroup = new GroupSetting("group.jumpcircles.duration.name", "group.jumpcircles.duration.desc", false)
         .settings(this.appearDuration, this.existDuration, this.disappearDuration);
      GroupSetting animationGroup = new GroupSetting("group.jumpcircles.animation.name", "group.jumpcircles.animation.desc", false)
         .settings(this.appearInterpolation, this.disappearInterpolation, this.rotateSpeed, this.circleScale, this.bright);
      this.setup(new Setting[]{colorGroup, appearanceGroup, durationGroup, animationGroup});
   }

   @Override
   public void deactivate() {
      this.circles.clear();
   }

   @EventHandler
   public void onJump(JumpEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null) {
         Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
         int circleCount = (int)this.bright.getValue();
         float sharedRotationAngle = (float)(QuickImports.random().nextDouble() * 360.0);
         float sharedAngularVelocity = (float)(QuickImports.random().nextDouble() * 2.0 + 1.0);

         for (int i = 0; i < circleCount; i++) {
            this.circles.add(new JumpCircles.Circle(pos, new StopWatch(), player, sharedRotationAngle, sharedAngularVelocity));
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.world != null && mc.player != null) {
         this.circles.removeIf(circlex -> {
            double elapsedSeconds = circlex.timer.elapsedTime() / 1000.0;
            double totalDuration = this.getTotalAnimationDuration();
            return elapsedSeconds > totalDuration;
         });
         if (!this.circles.isEmpty()) {
            for (JumpCircles.Circle circle : this.circles) {
               circle.updateRotation(this);
               circle.updateAnimation(this);
               JumpCircleRenderer.renderCircle(circle.pos, circle.timer, circle.rotationAngle, this, circle.animation);
            }
         }
      }
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

   public Identifier getCircleTexture() {
      String var1 = this.circleTexture.getSelected();

      return switch (var1) {
         case "Default" -> Identifier.of("textures/circles/circle.png");
         case "Bold" -> Identifier.of("textures/circles/circle_bold.png");
         case "Portal" -> Identifier.of("textures/circles/portal.png");
         case "Soup" -> Identifier.of("textures/circles/soup.png");
         default -> null;
      };
   }

   public List<JumpCircles.Circle> getCircles() {
      return this.circles;
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

   public SelectSetting getAppearAnimationType() {
      return this.appearAnimationType;
   }

   public SelectSetting getDisappearAnimationType() {
      return this.disappearAnimationType;
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

   public ValueSetting getCircleScale() {
      return this.circleScale;
   }

   public ValueSetting getBright() {
      return this.bright;
   }

   private static class Circle {
      final Vec3d pos;
      final StopWatch timer;
      final PlayerEntity player;
      float rotationAngle;
      final float angularVelocity;
      long lastUpdateTime;
      ThreeStageAnimation animation;

      Circle(Vec3d pos, StopWatch timer, PlayerEntity player, float initialRotation, float angularVelocity) {
         this.pos = pos;
         this.timer = timer;
         this.player = player;
         this.rotationAngle = initialRotation;
         this.angularVelocity = angularVelocity;
         this.lastUpdateTime = System.currentTimeMillis();
      }

      void updateAnimation(JumpCircles module) {
         if (this.animation == null) {
            this.animation = module.createAnimation();
         }
      }

      void updateRotation(JumpCircles module) {
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
