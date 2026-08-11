package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class TargetHud extends Module {
   public final SelectSetting style = new SelectSetting("setting.targethud.style.name", "setting.targethud.style.desc")
      .value("Default", "Round")
      .selected("Default");
   public final SelectSetting displayMode = new SelectSetting("setting.targethud.displaymode.name", "setting.targethud.displaymode.desc")
      .value("2D", "3D")
      .selected("2D");
   public final ValueSetting liveTime = new ValueSetting("setting.targethud.livetime.name", "setting.targethud.livetime.desc").setValue(1.0F).range(0, 10);
   public final ValueSetting scale = new ValueSetting("setting.targethud.scale.name", "setting.targethud.scale.desc").setValue(1.0F).range(0.5F, 2.0F);
   public final ValueSetting backAlpha = new ValueSetting("setting.targethud.backalpha.name", "setting.targethud.backalpha.desc")
      .setValue(0.7F)
      .range(0.0F, 1.0F);
   public final SelectSetting animationType = new SelectSetting("setting.targethud.animationtype.name", "setting.targethud.animationtype.desc")
      .value("Decelerate", "Linear", "EaseInOut")
      .selected("Decelerate");
   public final SelectSetting animationMode = new SelectSetting("setting.targethud.animationmode.name", "setting.targethud.animationmode.desc")
      .value("Scale", "Fade", "Both")
      .selected("Both");
   public final ValueSetting animationSpeed = new ValueSetting("setting.targethud.animationspeed.name", "setting.targethud.animationspeed.desc")
      .setValue(200.0F)
      .range(50, 500);
   public final SelectSetting anchor = new SelectSetting("setting.targethud.anchor3d.name", "setting.targethud.anchor3d.desc")
      .value("HEAD", "BODY", "FEET")
      .selected("HEAD");
   public final ValueSetting xOffset = new ValueSetting("setting.targethud.xoffset3d.name", "setting.targethud.xoffset3d.desc").setValue(0.0F).range(-100, 100);
   public final BooleanSetting particles = new BooleanSetting("setting.targethud.particles.name", "setting.targethud.particles.desc").setValue(true);
   public final SelectSetting particleSpawnLoc = new SelectSetting("setting.targethud.particlespawnloc.name", "setting.targethud.particlespawnloc.desc")
      .value("Face", "HP Bar")
      .selected("Face");
   public final MultiSelectSetting particleMode = new MultiSelectSetting("setting.targethud.particlemode.name", "setting.targethud.particlemode.desc")
      .value(
         "Stars",
         "Hearts",
         "Bloom",
         "Glyph",
         "Things",
         "Blink",
         "Coron",
         "Dollar",
         "Flame",
         "Geometric",
         "Snowflake",
         "Logo",
         "Virus",
         "SoupAPI Old",
         "Sword",
         "Cube",
         "Pyramid",
         "Network2D"
      );
   public final ValueSetting particleCount = new ValueSetting("setting.targethud.particlecount.name", "setting.targethud.particlecount.desc")
      .setValue(7.0F)
      .range(1, 20);
   public final ValueSetting particleSize = new ValueSetting("setting.targethud.particlesize.name", "setting.targethud.particlesize.desc")
      .setValue(6.0F)
      .range(4, 10);
   public final ValueSetting particleSpeed = new ValueSetting("setting.targethud.particlespeed.name", "setting.targethud.particlespeed.desc")
      .setValue(3.0F)
      .range(1.0F, 10.0F);
   public final ValueSetting particleMaxRadius = new ValueSetting("setting.targethud.particlemaxradius.name", "setting.targethud.particlemaxradius.desc")
      .setValue(50.0F)
      .range(20.0F, 100.0F);
   public final ValueSetting particleLifetime = new ValueSetting("setting.targethud.particlelifetime.name", "setting.targethud.particlelifetime.desc")
      .setValue(2.0F)
      .range(0.5F, 5.0F);
   public final SelectSetting particleColorMode = new SelectSetting("setting.targethud.particlecolormode.name", "setting.targethud.particlecolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   public final SelectSetting particleColorCount = new SelectSetting("setting.targethud.particlecolorcount.name", "setting.targethud.particlecolorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   public final MultiColorSetting particleColors = new MultiColorSetting("setting.targethud.particlecolors.name", "setting.targethud.particlecolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   public final SelectSetting particleColorAnimation = new SelectSetting(
         "setting.targethud.particlecoloranimation.name", "setting.targethud.particlecoloranimation.desc"
      )
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   public final ValueSetting linkDistance = new ValueSetting("setting.targethud.linkdistance.name", "setting.targethud.linkdistance.desc")
      .setValue(50.0F)
      .range(10.0F, 150.0F);
   public final ValueSetting maxLinks = new ValueSetting("setting.targethud.maxlinks.name", "setting.targethud.maxlinks.desc").setValue(3.0F).range(1, 10);
   private transient int[] cachedParticleCustomColors;
   private transient String lastParticleColorsCount = "";
   private transient int lastPColor1;
   private transient int lastPColor2;
   private transient int lastPColor3;
   private transient boolean lastParticleCustomMode = false;

   public static TargetHud getInstance() {
      return Instance.get(TargetHud.class);
   }

   public int[] getCustomColors() {
      boolean isCustom = this.particleColorMode.isSelected("Custom");
      if (!isCustom) {
         if (this.lastParticleCustomMode) {
            this.cachedParticleCustomColors = null;
            this.lastParticleCustomMode = false;
         }

         return null;
      } else {
         String count = this.particleColorCount.getSelected();
         int c1 = this.particleColors.getColor1().getColor();
         int c2 = this.particleColors.getColor2().getColor();
         int c3 = this.particleColors.getColor3().getColor();
         if (this.lastParticleCustomMode != isCustom
            || !count.equals(this.lastParticleColorsCount)
            || c1 != this.lastPColor1
            || c2 != this.lastPColor2
            || c3 != this.lastPColor3) {
            this.lastParticleCustomMode = true;
            this.lastParticleColorsCount = count;
            this.lastPColor1 = c1;
            this.lastPColor2 = c2;
            this.lastPColor3 = c3;

            this.cachedParticleCustomColors = switch (count) {
               case "Solo" -> new int[]{c1};
               case "Duo" -> new int[]{c1, c2};
               case "Triple" -> new int[]{c1, c2, c3};
               case "Quartet" -> this.particleColors.getColorValues();
               default -> null;
            };
         }

         return this.cachedParticleCustomColors;
      }
   }

   public TargetHud() {
      super("module.targethud.name", ModuleCategory.HUD);
      GroupSetting appearanceGroup = new GroupSetting("group.targethud.appearance.name", "group.targethud.appearance.desc", false)
         .settings(this.style, this.displayMode);
      GroupSetting displayGroup = new GroupSetting("group.targethud.display.name", "group.targethud.display.desc", false)
         .settings(this.scale, this.backAlpha, this.animationType, this.animationMode, this.animationSpeed);
      GroupSetting positionGroup = new GroupSetting("group.targethud.position3d.name", "group.targethud.position3d.desc", false)
         .settings(this.anchor, this.xOffset)
         .visible(() -> this.displayMode.getSelected().equals("3D"));
      GroupSetting particleColorGroup = new GroupSetting("group.targethud.particlecolor.name", "group.targethud.particlecolor.desc", false)
         .settings(this.particleColorMode, this.particleColorCount, this.particleColors, this.particleColorAnimation)
         .visible(this.particles::isValue);
      GroupSetting networkGroup = new GroupSetting("group.targethud.network.name", "group.targethud.network.desc", false)
         .settings(this.linkDistance, this.maxLinks)
         .visible(() -> this.particles.isValue() && this.particleMode.getSelected().contains("Network2D"));
      GroupSetting particlesGroup = new GroupSetting("group.targethud.particles.name", "group.targethud.particles.desc", false)
         .settings(this.particles, this.particleSpawnLoc, this.particleMode, this.particleCount, this.particleSize, this.particleSpeed, this.particleLifetime);
      this.setup(new Setting[]{appearanceGroup, this.liveTime, displayGroup, positionGroup, particlesGroup, particleColorGroup, networkGroup});
   }

   public SelectSetting getStyle() {
      return this.style;
   }

   public SelectSetting getDisplayMode() {
      return this.displayMode;
   }

   public ValueSetting getLiveTime() {
      return this.liveTime;
   }

   public ValueSetting getScale() {
      return this.scale;
   }

   public ValueSetting getBackAlpha() {
      return this.backAlpha;
   }

   public SelectSetting getAnimationType() {
      return this.animationType;
   }

   public SelectSetting getAnimationMode() {
      return this.animationMode;
   }

   public ValueSetting getAnimationSpeed() {
      return this.animationSpeed;
   }

   public SelectSetting getAnchor() {
      return this.anchor;
   }

   public ValueSetting getXOffset() {
      return this.xOffset;
   }

   public BooleanSetting getParticles() {
      return this.particles;
   }

   public SelectSetting getParticleSpawnLoc() {
      return this.particleSpawnLoc;
   }

   public MultiSelectSetting getParticleMode() {
      return this.particleMode;
   }

   public ValueSetting getParticleCount() {
      return this.particleCount;
   }

   public ValueSetting getParticleSize() {
      return this.particleSize;
   }

   public ValueSetting getParticleSpeed() {
      return this.particleSpeed;
   }

   public ValueSetting getParticleMaxRadius() {
      return this.particleMaxRadius;
   }

   public ValueSetting getParticleLifetime() {
      return this.particleLifetime;
   }

   public SelectSetting getParticleColorMode() {
      return this.particleColorMode;
   }

   public SelectSetting getParticleColorCount() {
      return this.particleColorCount;
   }

   public MultiColorSetting getParticleColors() {
      return this.particleColors;
   }

   public SelectSetting getParticleColorAnimation() {
      return this.particleColorAnimation;
   }

   public ValueSetting getLinkDistance() {
      return this.linkDistance;
   }

   public ValueSetting getMaxLinks() {
      return this.maxLinks;
   }

   public int[] getCachedParticleCustomColors() {
      return this.cachedParticleCustomColors;
   }

   public String getLastParticleColorsCount() {
      return this.lastParticleColorsCount;
   }

   public int getLastPColor1() {
      return this.lastPColor1;
   }

   public int getLastPColor2() {
      return this.lastPColor2;
   }

   public int getLastPColor3() {
      return this.lastPColor3;
   }

   public boolean isLastParticleCustomMode() {
      return this.lastParticleCustomMode;
   }
}
