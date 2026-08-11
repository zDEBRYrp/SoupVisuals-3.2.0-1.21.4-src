package padej.soup.implement.features.modules.other;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Trinket extends Module {
   private final ValueSetting gravity = new ValueSetting("setting.trinket.gravity.name", "setting.trinket.gravity.desc").setValue(0.5F).range(0.0F, 2.0F);
   private final ValueSetting weight = new ValueSetting("setting.trinket.weight.name", "setting.trinket.weight.desc").setValue(1.0F).range(0.0F, 1.5F);
   private final ValueSetting friction = new ValueSetting("setting.trinket.friction.name", "setting.trinket.friction.desc").setValue(0.98F).range(0.8F, 1.0F);
   private final ValueSetting bounce = new ValueSetting("setting.trinket.bounce.name", "setting.trinket.bounce.desc").setValue(0.7F).range(0.0F, 1.0F);
   private final ValueSetting groundFriction = new ValueSetting("setting.trinket.groundfriction.name", "setting.trinket.groundfriction.desc")
      .setValue(0.9F)
      .range(0.7F, 1.0F);
   private final BooleanSetting enableTrail = new BooleanSetting("setting.trinket.enabletrail.name", "setting.trinket.enabletrail.desc").setValue(true);
   private final SelectSetting trailColorMode = new SelectSetting("setting.trinket.trailcolormode.name", "setting.trinket.trailcolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(this.enableTrail::isValue);
   private final ColorSetting trailColor = new ColorSetting("setting.trinket.trailcolor.name", "setting.trinket.trailcolor.desc")
      .value(-1)
      .visible(() -> this.enableTrail.isValue() && this.trailColorMode.isSelected("Custom"));
   private final ValueSetting minSpeed = new ValueSetting("setting.trinket.minspeed.name", "setting.trinket.minspeed.desc")
      .setValue(0.5F)
      .range(0.1F, 5.0F)
      .visible(this.enableTrail::isValue);
   private final ValueSetting trailLength = new ValueSetting("setting.trinket.traillength.name", "setting.trinket.traillength.desc")
      .setValue(15.0F)
      .range(5, 30)
      .visible(this.enableTrail::isValue);
   private final ValueSetting trailSizeModifier = new ValueSetting("setting.trinket.trailsizemodifier.name", "setting.trinket.trailsizemodifier.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(this.enableTrail::isValue);
   private final SelectSetting iconType = new SelectSetting("setting.trinket.icontype.name", "setting.trinket.icontype.desc")
      .value("Atom", "Ball", "Banana", "Cat", "Donut", "Flame", "Flower", "Gem", "Ghost", "MCTiers")
      .selected("Atom");
   private final SelectSetting iconColorMode = new SelectSetting("setting.trinket.iconcolormode.name", "setting.trinket.iconcolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final ColorSetting iconColor = new ColorSetting("setting.trinket.iconcolor.name", "setting.trinket.iconcolor.desc")
      .value(-1)
      .visible(() -> this.iconColorMode.isSelected("Custom"));
   private final ValueSetting iconSize = new ValueSetting("setting.trinket.iconsize.name", "setting.trinket.iconsize.desc").setValue(16.0F).range(8, 32);
   private final SelectSetting soundType = new SelectSetting("setting.trinket.soundtype.name", "setting.trinket.soundtype.desc")
      .value("Default", "8Bit", "Glass", "DrumBass", "AimBooster", "Plastic", "Blip", "Pop")
      .selected("Default");
   private final ValueSetting soundVolume = new ValueSetting("setting.trinket.soundvolume.name", "setting.trinket.soundvolume.desc")
      .setValue(1.0F)
      .range(0.0F, 2.0F);
   private final BooleanSetting enableParticles = new BooleanSetting("setting.trinket.enableparticles.name", "setting.trinket.enableparticles.desc")
      .setValue(true);
   private final MultiSelectSetting particleMode = new MultiSelectSetting("setting.trinket.particlemode.name", "setting.trinket.particlemode.desc")
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
      )
      .selected("Bloom", "Stars");
   private final ValueSetting particleCount = new ValueSetting("setting.trinket.particlecount.name", "setting.trinket.particlecount.desc")
      .setValue(7.0F)
      .range(1, 20);
   private final ValueSetting particleSize = new ValueSetting("setting.trinket.particlesize.name", "setting.trinket.particlesize.desc")
      .setValue(6.0F)
      .range(4, 10);
   private final ValueSetting particleSpeed = new ValueSetting("setting.trinket.particlespeed.name", "setting.trinket.particlespeed.desc")
      .setValue(3.0F)
      .range(1.0F, 10.0F);
   private final ValueSetting particleMaxRadius = new ValueSetting("setting.trinket.particlemaxradius.name", "setting.trinket.particlemaxradius.desc")
      .setValue(50.0F)
      .range(20.0F, 100.0F);
   private final ValueSetting particleLifetime = new ValueSetting("setting.trinket.particlelifetime.name", "setting.trinket.particlelifetime.desc")
      .setValue(2.0F)
      .range(0.5F, 5.0F);
   private final SelectSetting particleColorMode = new SelectSetting("setting.trinket.particlecolormode.name", "setting.trinket.particlecolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting particleColorCount = new SelectSetting("setting.trinket.particlecolorcount.name", "setting.trinket.particlecolorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   private final MultiColorSetting particleColors = new MultiColorSetting("setting.trinket.particlecolors.name", "setting.trinket.particlecolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   private final SelectSetting particleColorAnimation = new SelectSetting(
         "setting.trinket.particlecoloranimation.name", "setting.trinket.particlecoloranimation.desc"
      )
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.particleColorMode.isSelected("Custom"));
   private final ValueSetting linkDistance = new ValueSetting("setting.trinket.linkdistance.name", "setting.trinket.linkdistance.desc")
      .setValue(50.0F)
      .range(10.0F, 150.0F);
   private final ValueSetting maxLinks = new ValueSetting("setting.trinket.maxlinks.name", "setting.trinket.maxlinks.desc").setValue(3.0F).range(1, 10);

   public static Trinket getInstance() {
      return Instance.get(Trinket.class);
   }

   public int[] getCustomColors() {
      if (!this.particleColorMode.isSelected("Custom")) {
         return null;
      } else {
         String var1 = this.particleColorCount.getSelected();

         return switch (var1) {
            case "Solo" -> new int[]{this.particleColors.getColor1().getColor()};
            case "Duo" -> new int[]{this.particleColors.getColor1().getColor(), this.particleColors.getColor2().getColor()};
            case "Triple" -> new int[]{
               this.particleColors.getColor1().getColor(), this.particleColors.getColor2().getColor(), this.particleColors.getColor3().getColor()
            };
            case "Quartet" -> this.particleColors.getColorValues();
            default -> null;
         };
      }
   }

   public Trinket() {
      super("module.trinket.name", ModuleCategory.OTHER);
      GroupSetting physicsGroup = new GroupSetting("group.trinket.physics.name", "group.trinket.physics.desc", false)
         .settings(this.gravity, this.weight, this.friction, this.bounce, this.groundFriction);
      GroupSetting trailGroup = new GroupSetting("group.trinket.trail.name", "group.trinket.trail.desc", false)
         .settings(this.enableTrail, this.trailColorMode, this.trailColor, this.minSpeed, this.trailLength, this.trailSizeModifier);
      GroupSetting appearanceGroup = new GroupSetting("group.trinket.appearance.name", "group.trinket.appearance.desc", false)
         .settings(this.iconType, this.iconColorMode, this.iconColor, this.iconSize);
      GroupSetting soundGroup = new GroupSetting("group.trinket.sound.name", "group.trinket.sound.desc", false).settings(this.soundType, this.soundVolume);
      GroupSetting particleColorGroup = new GroupSetting("group.trinket.particlecolor.name", "group.trinket.particlecolor.desc", false)
         .settings(this.particleColorMode, this.particleColorCount, this.particleColors, this.particleColorAnimation)
         .visible(() -> this.enableParticles.isValue());
      GroupSetting networkGroup = new GroupSetting("group.trinket.network.name", "group.trinket.network.desc", false)
         .settings(this.linkDistance, this.maxLinks)
         .visible(() -> this.enableParticles.isValue() && this.particleMode.getSelected().contains("Network2D"));
      GroupSetting particleGroup = new GroupSetting("group.trinket.particles.name", "group.trinket.particles.desc", false)
         .settings(
            this.enableParticles, this.particleMode, this.particleCount, this.particleSize, this.particleSpeed, this.particleMaxRadius, this.particleLifetime
         )
         .visible(() -> this.enableParticles.isValue());
      this.setup(new Setting[]{physicsGroup, trailGroup, appearanceGroup, soundGroup, particleGroup, particleColorGroup, networkGroup});
   }

   public ValueSetting getGravity() {
      return this.gravity;
   }

   public ValueSetting getWeight() {
      return this.weight;
   }

   public ValueSetting getFriction() {
      return this.friction;
   }

   public ValueSetting getBounce() {
      return this.bounce;
   }

   public ValueSetting getGroundFriction() {
      return this.groundFriction;
   }

   public BooleanSetting getEnableTrail() {
      return this.enableTrail;
   }

   public SelectSetting getTrailColorMode() {
      return this.trailColorMode;
   }

   public ColorSetting getTrailColor() {
      return this.trailColor;
   }

   public ValueSetting getMinSpeed() {
      return this.minSpeed;
   }

   public ValueSetting getTrailLength() {
      return this.trailLength;
   }

   public ValueSetting getTrailSizeModifier() {
      return this.trailSizeModifier;
   }

   public SelectSetting getIconType() {
      return this.iconType;
   }

   public SelectSetting getIconColorMode() {
      return this.iconColorMode;
   }

   public ColorSetting getIconColor() {
      return this.iconColor;
   }

   public ValueSetting getIconSize() {
      return this.iconSize;
   }

   public SelectSetting getSoundType() {
      return this.soundType;
   }

   public ValueSetting getSoundVolume() {
      return this.soundVolume;
   }

   public BooleanSetting getEnableParticles() {
      return this.enableParticles;
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
}
