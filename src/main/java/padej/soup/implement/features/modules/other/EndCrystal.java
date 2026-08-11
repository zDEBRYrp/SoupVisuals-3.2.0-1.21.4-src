package padej.soup.implement.features.modules.other;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.EndCrystalRenderer;

public class EndCrystal extends Module {
   private final List<EndCrystal.PhantomCrystal> phantomCrystals = new ArrayList<>();
   private final Map<Vec3d, EndCrystal.CrystalModelState> activeCrystals = new HashMap<>();
   private EndCrystalEntityModel crystalModel;
   private final SelectSetting colorMode = new SelectSetting("setting.endcrystal.colormode.name", "setting.endcrystal.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.endcrystal.colorcount.name", "setting.endcrystal.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.endcrystal.gradientcolors.name", "setting.endcrystal.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting colorAnimation = new SelectSetting("setting.endcrystal.coloranimation.name", "setting.endcrystal.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiSelectSetting modelParts = new MultiSelectSetting("setting.endcrystal.modelparts.name", "setting.endcrystal.modelparts.desc")
      .value("Outer Glass", "Inner Glass", "Cube")
      .selected("Outer Glass", "Inner Glass", "Cube");
   private final ValueSetting yOffset = new ValueSetting("setting.endcrystal.yoffset.name", "setting.endcrystal.yoffset.desc")
      .setValue(0.0F)
      .range(-2.0F, 2.0F);
   private final ValueSetting size = new ValueSetting("setting.endcrystal.size.name", "setting.endcrystal.size.desc").setValue(2.0F).range(0.5F, 3.0F);
   private final ValueSetting animationSpeed = new ValueSetting("setting.endcrystal.animationspeed.name", "setting.endcrystal.animationspeed.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F);
   private final ValueSetting breakDuration = new ValueSetting("setting.endcrystal.breakduration.name", "setting.endcrystal.breakduration.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F);
   private final ValueSetting breakScaleMultiplier = new ValueSetting(
         "setting.endcrystal.breakscalemultiplier.name", "setting.endcrystal.breakscalemultiplier.desc"
      )
      .setValue(0.5F)
      .range(0.0F, 2.0F);

   public static EndCrystal getInstance() {
      return Instance.get(EndCrystal.class);
   }

   public EndCrystal() {
      super("module.endcrystal.name", ModuleCategory.OTHER);
      GroupSetting colorGroup = new GroupSetting("group.endcrystal.colors.name", "group.endcrystal.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation);
      GroupSetting modelGroup = new GroupSetting("group.endcrystal.model.name", "group.endcrystal.model.desc", false)
         .settings(this.modelParts, this.yOffset, this.size);
      GroupSetting animationGroup = new GroupSetting("group.endcrystal.animations.name", "group.endcrystal.animations.desc", false)
         .settings(this.animationSpeed, this.breakDuration, this.breakScaleMultiplier);
      this.setup(new Setting[]{colorGroup, modelGroup, animationGroup});
   }

   public void drawCrystals(WorldRenderContext context) {
      EndCrystalRenderer.drawCrystals(context, this);
   }

   public void renderCrystal(
      EndCrystalEntityRenderState state, MatrixStack matrixStack, int light, EndCrystalEntityModel model, VertexConsumerProvider vertexConsumerProvider
   ) {
      EndCrystalRenderer.renderCrystal(state, matrixStack, light, model, vertexConsumerProvider, this);
   }

   public float getSize() {
      return this.size.getValue();
   }

   public float getYOffset() {
      return this.yOffset.getValue();
   }

   public boolean isVertexMode() {
      return this.colorAnimation.isSelected("Vertex") && this.colorMode.isSelected("Custom");
   }

   public boolean isWaveMode() {
      return this.colorAnimation.isSelected("Wave");
   }

   public boolean isSyncMode() {
      return this.colorMode.isSelected("Sync");
   }

   public boolean isOuterGlassVisible() {
      return this.modelParts.isSelected("Outer Glass");
   }

   public boolean isInnerGlassVisible() {
      return this.modelParts.isSelected("Inner Glass");
   }

   public boolean isCubeVisible() {
      return this.modelParts.isSelected("Cube");
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

   public void trackCrystal(Vec3d pos, float outerGlassYaw, float outerGlassPivotY) {
      this.activeCrystals.put(pos, new EndCrystal.CrystalModelState(outerGlassYaw, outerGlassPivotY));
   }

   public void clearTracking() {
      this.activeCrystals.clear();
   }

   public void checkRemovedCrystal(Vec3d crystalPos) {
      Vec3d closestPos = null;
      EndCrystal.CrystalModelState closestState = null;
      double minDistance = 0.1;

      for (Entry<Vec3d, EndCrystal.CrystalModelState> entry : this.activeCrystals.entrySet()) {
         double dist = entry.getKey().distanceTo(crystalPos);
         if (dist < minDistance) {
            closestPos = entry.getKey();
            closestState = entry.getValue();
            minDistance = dist;
         }
      }

      if (closestPos != null && closestState != null) {
         this.addPhantomCrystal(closestPos, closestState);
         this.activeCrystals.remove(closestPos);
      }
   }

   private void addPhantomCrystal(Vec3d pos, EndCrystal.CrystalModelState state) {
      int durationMs = (int)(600.0F / this.breakDuration.getValue());
      this.phantomCrystals.add(new EndCrystal.PhantomCrystal(pos, state.outerGlassYaw, state.outerGlassPivotY, durationMs));
   }

   public float getBreakScaleMultiplier() {
      return this.breakScaleMultiplier.getValue();
   }

   public float getAnimationSpeed() {
      return this.animationSpeed.getValue();
   }

   public List<EndCrystal.PhantomCrystal> getPhantomCrystals() {
      return this.phantomCrystals;
   }

   public EndCrystalEntityModel getCrystalModel() {
      return this.crystalModel;
   }

   public void setCrystalModel(EndCrystalEntityModel crystalModel) {
      this.crystalModel = crystalModel;
   }

   public record CrystalModelState(float outerGlassYaw, float outerGlassPivotY) {
   }

   public static class PhantomCrystal {
      public final Vec3d pos;
      public final DecelerateAnimation animation;
      public final float outerGlassYaw;
      public final float outerGlassPivotY;

      public PhantomCrystal(Vec3d pos, float outerGlassYaw, float outerGlassPivotY, int duration) {
         this.pos = pos;
         this.animation = new DecelerateAnimation();
         this.animation.setMs(duration);
         this.animation.setValue(1.0);
         this.animation.setDirection(Direction.FORWARDS);
         this.outerGlassYaw = outerGlassYaw;
         this.outerGlassPivotY = outerGlassPivotY;
      }

      public boolean isFinished() {
         return this.animation.isDone();
      }
   }
}
