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
import padej.soup.base.util.other.Instance;

public class ItemOverlay extends Module {
   private final BooleanSetting enableItemScale = new BooleanSetting("setting.itemoverlay.enableitemscale.name", "setting.itemoverlay.enableitemscale.desc")
      .setValue(false);
   private final ValueSetting itemScale = new ValueSetting("setting.itemoverlay.itemscale.name", "setting.itemoverlay.itemscale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(this.enableItemScale::isValue);
   private final BooleanSetting customDurabilityBar = new BooleanSetting(
         "setting.itemoverlay.customdurabilitybar.name", "setting.itemoverlay.customdurabilitybar.desc"
      )
      .setValue(true);
   private final BooleanSetting durabilityBarShadow = new BooleanSetting(
         "setting.itemoverlay.durabilitybarshadow.name", "setting.itemoverlay.durabilitybarshadow.desc"
      )
      .setValue(true)
      .visible(this.customDurabilityBar::isValue);
   private final SelectSetting barColorMode = new SelectSetting("setting.itemoverlay.barcolormode.name", "setting.itemoverlay.barcolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(this.customDurabilityBar::isValue);
   private final SelectSetting customBarColorsCount = new SelectSetting("setting.itemoverlay.barcolorcount.name", "setting.itemoverlay.barcolorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.customDurabilityBar.isValue() && this.barColorMode.isSelected("Custom"));
   private final MultiColorSetting customBarColors = new MultiColorSetting(
         "setting.itemoverlay.bargradientcolors.name", "setting.itemoverlay.bargradientcolors.desc"
      )
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.customDurabilityBar.isValue() && this.barColorMode.isSelected("Custom"));
   private final SelectSetting barColorAnimation = new SelectSetting("setting.itemoverlay.barcoloranimation.name", "setting.itemoverlay.barcoloranimation.desc")
      .value("Wave", "Static")
      .selected("Wave")
      .visible(() -> this.customDurabilityBar.isValue() && this.barColorMode.isSelected("Custom"));
   private final BooleanSetting customStackCount = new BooleanSetting("setting.itemoverlay.customstackcount.name", "setting.itemoverlay.customstackcount.desc")
      .setValue(true);
   private final BooleanSetting useCustomFont = new BooleanSetting("setting.itemoverlay.usecustomfont.name", "setting.itemoverlay.usecustomfont.desc")
      .setValue(true)
      .visible(this.customStackCount::isValue);
   private final BooleanSetting addPrefix = new BooleanSetting("setting.itemoverlay.addprefix.name", "setting.itemoverlay.addprefix.desc")
      .setValue(true)
      .visible(this.customStackCount::isValue);
   private final BooleanSetting enableCustomColor = new BooleanSetting(
         "setting.itemoverlay.enablecustomcolor.name", "setting.itemoverlay.enablecustomcolor.desc"
      )
      .setValue(false)
      .visible(this.customStackCount::isValue);
   private final ColorSetting stackCountColor = new ColorSetting("setting.itemoverlay.stackcountcolor.name", "setting.itemoverlay.stackcountcolor.desc")
      .value(-1)
      .visible(() -> this.customStackCount.isValue() && this.enableCustomColor.isValue());
   private final BooleanSetting customCooldown = new BooleanSetting("setting.itemoverlay.customcooldown.name", "setting.itemoverlay.customcooldown.desc")
      .setValue(true);
   private final SelectSetting cooldownStyle = new SelectSetting("setting.itemoverlay.cooldownstyle.name", "setting.itemoverlay.cooldownstyle.desc")
      .value("Bar", "Ring")
      .selected("Bar")
      .visible(this.customCooldown::isValue);
   private final BooleanSetting showCooldownNumber = new BooleanSetting(
         "setting.itemoverlay.showcooldownnumber.name", "setting.itemoverlay.showcooldownnumber.desc"
      )
      .setValue(false)
      .visible(this.customCooldown::isValue);
   private final SelectSetting cooldownColorMode = new SelectSetting("setting.itemoverlay.cooldowncolormode.name", "setting.itemoverlay.cooldowncolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(this.customCooldown::isValue);
   private final ColorSetting cooldownColor = new ColorSetting("setting.itemoverlay.cooldowncolor.name", "setting.itemoverlay.cooldowncolor.desc")
      .value(-1)
      .visible(() -> this.customCooldown.isValue() && this.cooldownColorMode.isSelected("Custom"));
   private final ValueSetting cooldownAlpha = new ValueSetting("setting.itemoverlay.cooldownalpha.name", "setting.itemoverlay.cooldownalpha.desc")
      .setValue(0.5F)
      .range(0.0F, 1.0F)
      .visible(this.customCooldown::isValue);

   public static ItemOverlay getInstance() {
      return Instance.get(ItemOverlay.class);
   }

   public ItemOverlay() {
      super("module.itemoverlay.name", ModuleCategory.HUD);
      GroupSetting generalGroup = new GroupSetting("group.itemoverlay.general.name", "group.itemoverlay.general.desc", false)
         .settings(this.enableItemScale, this.itemScale);
      GroupSetting durabilityGroup = new GroupSetting("group.itemoverlay.durability.name", "group.itemoverlay.durability.desc", false)
         .settings(this.customDurabilityBar, this.durabilityBarShadow);
      GroupSetting durabilityColorGroup = new GroupSetting("group.itemoverlay.durabilitycolor.name", "group.itemoverlay.durabilitycolor.desc", false)
         .settings(this.barColorMode, this.customBarColorsCount, this.customBarColors, this.barColorAnimation)
         .visible(this.customDurabilityBar::isValue);
      GroupSetting stackCountGroup = new GroupSetting("group.itemoverlay.stackcount.name", "group.itemoverlay.stackcount.desc", false)
         .settings(this.customStackCount, this.useCustomFont, this.addPrefix, this.enableCustomColor, this.stackCountColor);
      GroupSetting cooldownGroup = new GroupSetting("group.itemoverlay.cooldown.name", "group.itemoverlay.cooldown.desc", false)
         .settings(this.customCooldown, this.cooldownStyle, this.showCooldownNumber);
      GroupSetting cooldownColorGroup = new GroupSetting("group.itemoverlay.cooldowncolor.name", "group.itemoverlay.cooldowncolor.desc", false)
         .settings(this.cooldownColorMode, this.cooldownColor, this.cooldownAlpha)
         .visible(this.customCooldown::isValue);
      this.setup(new Setting[]{generalGroup, durabilityGroup, durabilityColorGroup, stackCountGroup, cooldownGroup, cooldownColorGroup});
   }

   public int[] getCustomBarColors() {
      if (!this.barColorMode.isSelected("Custom")) {
         return null;
      } else {
         String var1 = this.customBarColorsCount.getSelected();

         return switch (var1) {
            case "Solo" -> new int[]{this.customBarColors.getColor1().getColor()};
            case "Duo" -> new int[]{this.customBarColors.getColor1().getColor(), this.customBarColors.getColor2().getColor()};
            case "Triple" -> new int[]{
               this.customBarColors.getColor1().getColor(), this.customBarColors.getColor2().getColor(), this.customBarColors.getColor3().getColor()
            };
            case "Quartet" -> this.customBarColors.getColorValues();
            default -> null;
         };
      }
   }

   public BooleanSetting getEnableItemScale() {
      return this.enableItemScale;
   }

   public ValueSetting getItemScale() {
      return this.itemScale;
   }

   public BooleanSetting getCustomDurabilityBar() {
      return this.customDurabilityBar;
   }

   public BooleanSetting getDurabilityBarShadow() {
      return this.durabilityBarShadow;
   }

   public SelectSetting getBarColorMode() {
      return this.barColorMode;
   }

   public SelectSetting getCustomBarColorsCount() {
      return this.customBarColorsCount;
   }

   public SelectSetting getBarColorAnimation() {
      return this.barColorAnimation;
   }

   public BooleanSetting getCustomStackCount() {
      return this.customStackCount;
   }

   public BooleanSetting getUseCustomFont() {
      return this.useCustomFont;
   }

   public BooleanSetting getAddPrefix() {
      return this.addPrefix;
   }

   public BooleanSetting getEnableCustomColor() {
      return this.enableCustomColor;
   }

   public ColorSetting getStackCountColor() {
      return this.stackCountColor;
   }

   public BooleanSetting getCustomCooldown() {
      return this.customCooldown;
   }

   public SelectSetting getCooldownStyle() {
      return this.cooldownStyle;
   }

   public BooleanSetting getShowCooldownNumber() {
      return this.showCooldownNumber;
   }

   public SelectSetting getCooldownColorMode() {
      return this.cooldownColorMode;
   }

   public ColorSetting getCooldownColor() {
      return this.cooldownColor;
   }

   public ValueSetting getCooldownAlpha() {
      return this.cooldownAlpha;
   }
}
