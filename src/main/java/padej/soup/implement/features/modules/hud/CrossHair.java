package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.CrosshairRenderer;

public class CrossHair extends Module {
   public float prevYaw;
   public float prevPitch;
   public float xAnim;
   public float yAnim;
   private int[] cachedColors;
   private String lastColorsCount = "";
   private int lastColor1;
   private int lastColor2;
   private int lastColor3;
   private boolean lastCustomMode = false;
   private final SelectSetting styleSetting = new SelectSetting("setting.crosshair.style.name", "setting.crosshair.style.desc")
      .value("Cross", "Blob", "Ring")
      .selected("Cross");
   private final SelectSetting animationTypeSetting = new SelectSetting("setting.crosshair.animationtype.name", "setting.crosshair.animationtype.desc")
      .value("Ease Out", "Spring", "Decelerate", "Fast", "Smooth", "Balanced", "Elastic", "Bounce")
      .selected("Ease Out")
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting size1Setting = new ValueSetting("setting.crosshair.width.name", "setting.crosshair.width.desc")
      .setValue(4.0F)
      .range(2, 10)
      .visible(() -> this.styleSetting.isSelected("Cross"));
   private final ValueSetting size2Setting = new ValueSetting("setting.crosshair.height.name", "setting.crosshair.height.desc")
      .setValue(1.0F)
      .range(1, 4)
      .visible(() -> this.styleSetting.isSelected("Cross"));
   private final ValueSetting blobSizeSetting = new ValueSetting("setting.crosshair.blobsize.name", "setting.crosshair.blobsize.desc")
      .setValue(6.0F)
      .range(4, 16)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting blobThicknessSetting = new ValueSetting("setting.crosshair.blobthickness.name", "setting.crosshair.blobthickness.desc")
      .setValue(2.0F)
      .range(1, 5)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting attackSizeMultiplierSetting = new ValueSetting(
         "setting.crosshair.attacksizemultiplier.name", "setting.crosshair.attacksizemultiplier.desc"
      )
      .setValue(0.9F)
      .range(0.0F, 2.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting entityWidthMultiplierSetting = new ValueSetting(
         "setting.crosshair.entitywidthmultiplier.name", "setting.crosshair.entitywidthmultiplier.desc"
      )
      .setValue(0.7F)
      .range(0.0F, 2.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting entityHeightMultiplierSetting = new ValueSetting(
         "setting.crosshair.entityheightmultiplier.name", "setting.crosshair.entityheightmultiplier.desc"
      )
      .setValue(0.2F)
      .range(0.0F, 1.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting hitStretchMultiplierSetting = new ValueSetting(
         "setting.crosshair.hitstretchmultiplier.name", "setting.crosshair.hitstretchmultiplier.desc"
      )
      .setValue(0.0F)
      .range(0.0F, 5.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final BooleanSetting enableFillSetting = new BooleanSetting("setting.crosshair.enablefill.name", "setting.crosshair.enablefill.desc")
      .setValue(true)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting fillMaxAlphaSetting = new ValueSetting("setting.crosshair.fillmaxalpha.name", "setting.crosshair.fillmaxalpha.desc")
      .setValue(0.5F)
      .range(0.0F, 1.0F)
      .visible(() -> this.styleSetting.isSelected("Blob") && this.enableFillSetting.isValue());
   private final ValueSetting outlineThicknessSetting = new ValueSetting("setting.crosshair.outlinethickness.name", "setting.crosshair.outlinethickness.desc")
      .setValue(3.5F)
      .range(0.0F, 10.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting colorAnimationSpeedSetting = new ValueSetting(
         "setting.crosshair.coloranimationspeed.name", "setting.crosshair.coloranimationspeed.desc"
      )
      .setValue(2.0F)
      .range(1.0F, 10.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting hitAnimationSpeedSetting = new ValueSetting(
         "setting.crosshair.hitanimationspeed.name", "setting.crosshair.hitanimationspeed.desc"
      )
      .setValue(5.0F)
      .range(1.0F, 10.0F)
      .visible(() -> this.styleSetting.isSelected("Blob"));
   private final ValueSetting ringSizeSetting = new ValueSetting("setting.crosshair.ringsize.name", "setting.crosshair.ringsize.desc")
      .setValue(18.0F)
      .range(15, 30)
      .visible(() -> this.styleSetting.isSelected("Ring"));
   private final ValueSetting ringThicknessSetting = new ValueSetting("setting.crosshair.ringthickness.name", "setting.crosshair.ringthickness.desc")
      .setValue(0.3F)
      .range(0.1F, 0.5F)
      .visible(() -> this.styleSetting.isSelected("Ring"));
   private final ValueSetting ringRoundnessSetting = new ValueSetting("setting.crosshair.ringroundness.name", "setting.crosshair.ringroundness.desc")
      .setValue(0.15F)
      .range(0.0F, 0.3F)
      .visible(() -> this.styleSetting.isSelected("Ring"));
   private final ValueSetting attackSetting = new ValueSetting("setting.crosshair.attackindent.name", "setting.crosshair.attackindent.desc")
      .setValue(10.0F)
      .range(0, 20);
   private final ValueSetting indentSetting = new ValueSetting("setting.crosshair.indent.name", "setting.crosshair.indent.desc").setValue(0.0F).range(0, 5);
   private final BooleanSetting dynamicSetting = new BooleanSetting("setting.crosshair.dynamic.name", "setting.crosshair.dynamic.desc").setValue(false);
   private final BooleanSetting dynamicInvertSetting = new BooleanSetting("setting.crosshair.dynamicinvert.name", "setting.crosshair.dynamicinvert.desc")
      .setValue(true)
      .visible(this.dynamicSetting::isValue);
   private final ValueSetting dynamicRangeSetting = new ValueSetting("setting.crosshair.dynamicrange.name", "setting.crosshair.dynamicrange.desc")
      .setValue(15.0F)
      .range(5, 30)
      .visible(this.dynamicSetting::isValue);
   private final ValueSetting dynamicSpeedSetting = new ValueSetting("setting.crosshair.dynamicspeed.name", "setting.crosshair.dynamicspeed.desc")
      .setValue(3.0F)
      .range(1.0F, 10.0F)
      .visible(this.dynamicSetting::isValue);
   private final ValueSetting dynamicBackSpeedSetting = new ValueSetting("setting.crosshair.dynamicbackspeed.name", "setting.crosshair.dynamicbackspeed.desc")
      .setValue(5.0F)
      .range(1.0F, 10.0F)
      .visible(this.dynamicSetting::isValue);
   private final BooleanSetting invertColorSetting = new BooleanSetting("setting.crosshair.invertcolor.name", "setting.crosshair.invertcolor.desc")
      .setValue(false)
      .visible(() -> this.styleSetting.getSelected().equals("Cross"));
   private final SelectSetting colorMode = new SelectSetting("setting.crosshair.colormode.name", "setting.crosshair.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(() -> !this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross"));
   private final SelectSetting customColorsCount = new SelectSetting("setting.crosshair.colorcount.name", "setting.crosshair.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom") && (!this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross")));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.crosshair.gradientcolors.name", "setting.crosshair.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom") && (!this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross")));
   private final SelectSetting colorAnimation = new SelectSetting("setting.crosshair.coloranimation.name", "setting.crosshair.coloranimation.desc")
      .value("Wave", "Static")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom") && (!this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross")));
   private final ColorSetting targetColorSetting = new ColorSetting("setting.crosshair.targetcolor.name", "setting.crosshair.targetcolor.desc")
      .setColor(ColorUtil.RED)
      .visible(() -> !this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross"));

   public static CrossHair getInstance() {
      return Instance.get(CrossHair.class);
   }

   public CrossHair() {
      super("module.crosshair.name", ModuleCategory.HUD);
      GroupSetting appearanceGroup = new GroupSetting("group.crosshair.appearance.name", "group.crosshair.appearance.desc", false)
         .settings(this.styleSetting, this.animationTypeSetting);
      GroupSetting crossSizeGroup = new GroupSetting("group.crosshair.crosssize.name", "group.crosshair.crosssize.desc", false)
         .settings(this.size1Setting, this.size2Setting)
         .visible(() -> this.styleSetting.isSelected("Cross"));
      GroupSetting blobSizeGroup = new GroupSetting("group.crosshair.blobsize.name", "group.crosshair.blobsize.desc", false)
         .settings(this.blobSizeSetting, this.blobThicknessSetting, this.outlineThicknessSetting)
         .visible(() -> this.styleSetting.isSelected("Blob"));
      GroupSetting ringSizeGroup = new GroupSetting("group.crosshair.ringsize.name", "group.crosshair.ringsize.desc", false)
         .settings(this.ringSizeSetting, this.ringThicknessSetting, this.ringRoundnessSetting)
         .visible(() -> this.styleSetting.isSelected("Ring"));
      GroupSetting positionGroup = new GroupSetting("group.crosshair.position.name", "group.crosshair.position.desc", false)
         .settings(this.attackSetting, this.indentSetting)
         .visible(() -> this.styleSetting.isSelected("Cross"));
      GroupSetting dynamicGroup = new GroupSetting("group.crosshair.dynamic.name", "group.crosshair.dynamic.desc", false)
         .settings(this.dynamicSetting, this.dynamicInvertSetting, this.dynamicRangeSetting, this.dynamicSpeedSetting, this.dynamicBackSpeedSetting);
      GroupSetting basicColorsGroup = new GroupSetting("group.crosshair.basiccolors.name", "group.crosshair.basiccolors.desc", false)
         .settings(this.invertColorSetting, this.colorMode, this.targetColorSetting);
      GroupSetting customColorsGroup = new GroupSetting("group.crosshair.customcolors.name", "group.crosshair.customcolors.desc", false)
         .settings(this.customColorsCount, this.customColors, this.colorAnimation)
         .visible(() -> this.colorMode.isSelected("Custom") && (!this.invertColorSetting.isValue() || !this.styleSetting.getSelected().equals("Cross")));
      GroupSetting blobMultipliersGroup = new GroupSetting("group.crosshair.blobmultipliers.name", "group.crosshair.blobmultipliers.desc", false)
         .settings(this.attackSizeMultiplierSetting, this.entityWidthMultiplierSetting, this.entityHeightMultiplierSetting, this.hitStretchMultiplierSetting)
         .visible(() -> this.styleSetting.isSelected("Blob"));
      GroupSetting blobSpeedsGroup = new GroupSetting("group.crosshair.blobspeeds.name", "group.crosshair.blobspeeds.desc", false)
         .settings(this.colorAnimationSpeedSetting, this.hitAnimationSpeedSetting)
         .visible(() -> this.styleSetting.isSelected("Blob"));
      GroupSetting fillGroup = new GroupSetting("group.crosshair.fill.name", "group.crosshair.fill.desc", false)
         .settings(this.enableFillSetting, this.fillMaxAlphaSetting)
         .visible(() -> this.styleSetting.isSelected("Blob"));
      this.setup(
         new Setting[]{
            appearanceGroup,
            crossSizeGroup,
            blobSizeGroup,
            ringSizeGroup,
            positionGroup,
            dynamicGroup,
            basicColorsGroup,
            customColorsGroup,
            blobMultipliersGroup,
            blobSpeedsGroup,
            fillGroup
         }
      );
   }

   public void onRenderCrossHair() {
      CrosshairRenderer.render(this);
   }

   public int[] getCustomColors() {
      boolean isCustom = this.colorMode.isSelected("Custom");
      if (!isCustom) {
         if (this.lastCustomMode) {
            this.cachedColors = null;
            this.lastCustomMode = false;
         }

         return null;
      } else {
         String count = this.customColorsCount.getSelected();
         int c1 = this.customColors.getColor1().getColor();
         int c2 = this.customColors.getColor2().getColor();
         int c3 = this.customColors.getColor3().getColor();
         if (this.lastCustomMode != isCustom || !count.equals(this.lastColorsCount) || c1 != this.lastColor1 || c2 != this.lastColor2 || c3 != this.lastColor3) {
            this.lastCustomMode = true;
            this.lastColorsCount = count;
            this.lastColor1 = c1;
            this.lastColor2 = c2;
            this.lastColor3 = c3;

            this.cachedColors = switch (count) {
               case "Solo" -> new int[]{c1};
               case "Duo" -> new int[]{c1, c2};
               case "Triple" -> new int[]{c1, c2, c3};
               case "Quartet" -> this.customColors.getColorValues();
               default -> null;
            };
         }

         return this.cachedColors;
      }
   }

   public float getPrevYaw() {
      return this.prevYaw;
   }

   public float getPrevPitch() {
      return this.prevPitch;
   }

   public float getXAnim() {
      return this.xAnim;
   }

   public float getYAnim() {
      return this.yAnim;
   }

   public int[] getCachedColors() {
      return this.cachedColors;
   }

   public String getLastColorsCount() {
      return this.lastColorsCount;
   }

   public int getLastColor1() {
      return this.lastColor1;
   }

   public int getLastColor2() {
      return this.lastColor2;
   }

   public int getLastColor3() {
      return this.lastColor3;
   }

   public boolean isLastCustomMode() {
      return this.lastCustomMode;
   }

   public SelectSetting getStyleSetting() {
      return this.styleSetting;
   }

   public SelectSetting getAnimationTypeSetting() {
      return this.animationTypeSetting;
   }

   public ValueSetting getSize1Setting() {
      return this.size1Setting;
   }

   public ValueSetting getSize2Setting() {
      return this.size2Setting;
   }

   public ValueSetting getBlobSizeSetting() {
      return this.blobSizeSetting;
   }

   public ValueSetting getBlobThicknessSetting() {
      return this.blobThicknessSetting;
   }

   public ValueSetting getAttackSizeMultiplierSetting() {
      return this.attackSizeMultiplierSetting;
   }

   public ValueSetting getEntityWidthMultiplierSetting() {
      return this.entityWidthMultiplierSetting;
   }

   public ValueSetting getEntityHeightMultiplierSetting() {
      return this.entityHeightMultiplierSetting;
   }

   public ValueSetting getHitStretchMultiplierSetting() {
      return this.hitStretchMultiplierSetting;
   }

   public BooleanSetting getEnableFillSetting() {
      return this.enableFillSetting;
   }

   public ValueSetting getFillMaxAlphaSetting() {
      return this.fillMaxAlphaSetting;
   }

   public ValueSetting getOutlineThicknessSetting() {
      return this.outlineThicknessSetting;
   }

   public ValueSetting getColorAnimationSpeedSetting() {
      return this.colorAnimationSpeedSetting;
   }

   public ValueSetting getHitAnimationSpeedSetting() {
      return this.hitAnimationSpeedSetting;
   }

   public ValueSetting getRingSizeSetting() {
      return this.ringSizeSetting;
   }

   public ValueSetting getRingThicknessSetting() {
      return this.ringThicknessSetting;
   }

   public ValueSetting getRingRoundnessSetting() {
      return this.ringRoundnessSetting;
   }

   public ValueSetting getAttackSetting() {
      return this.attackSetting;
   }

   public ValueSetting getIndentSetting() {
      return this.indentSetting;
   }

   public BooleanSetting getDynamicSetting() {
      return this.dynamicSetting;
   }

   public BooleanSetting getDynamicInvertSetting() {
      return this.dynamicInvertSetting;
   }

   public ValueSetting getDynamicRangeSetting() {
      return this.dynamicRangeSetting;
   }

   public ValueSetting getDynamicSpeedSetting() {
      return this.dynamicSpeedSetting;
   }

   public ValueSetting getDynamicBackSpeedSetting() {
      return this.dynamicBackSpeedSetting;
   }

   public BooleanSetting getInvertColorSetting() {
      return this.invertColorSetting;
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

   public ColorSetting getTargetColorSetting() {
      return this.targetColorSetting;
   }
}
