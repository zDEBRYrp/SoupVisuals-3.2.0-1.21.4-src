package padej.soup.implement.features.modules.client;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.UIManager;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.color.ColorPalette;
import padej.soup.base.util.color.CustomPalette;
import padej.soup.base.util.color.DarkPalette;
import padej.soup.base.util.color.LightPalette;
import padej.soup.base.util.color.SoftDarkPalette;
import padej.soup.base.util.color.SoftLightPalette;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;

public class Theme extends Module {
   private ColorPalette currentPalette = new DarkPalette();
   private int savedMainGuiColor = -15066592;
   private int savedGuiRectColor = -14803420;
   private int savedGuiRectColor2 = -14540246;
   private int savedRectColor = -14935006;
   private int savedRectDarkerColor = -15198176;
   private int savedBlurRectColor = -14935006;
   private int savedTextColor = -2829096;
   private int savedDescriptionColor = -7894892;
   private boolean lastSystemDarkMode = false;
   private long lastThemeCheckTime = 0L;
   static final long THEME_CHECK_INTERVAL = 5000L;
   private String lastThemeMode = "Dark";
   public final SelectSetting themeMode = new SelectSetting("setting.theme.thememode.name", "setting.theme.thememode.desc")
      .value("System", "Dark", "Light", "Custom")
      .selected("Dark")
      .onChange(mode -> this.applyTheme());
   public final BooleanSetting softMode = new BooleanSetting("setting.theme.soft.name", "setting.theme.soft.desc")
      .setValue(false)
      .visible(() -> !this.themeMode.isSelected("Custom"))
      .onChange(enabled -> this.applyTheme());
   public final SelectSetting colorMode = new SelectSetting("setting.theme.colormode.name", "setting.theme.colormode.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo");
   public final MultiColorSetting colorSetting = new MultiColorSetting("setting.theme.clientcolor.name", "setting.theme.clientcolor.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301);
   public final ValueSetting fadeSpeed = new ValueSetting("setting.theme.fadespeed.name", "setting.theme.fadespeed.desc")
      .setValue(1.0F)
      .range(0.1F, 5.0F)
      .visible(() -> !this.colorMode.isSelected("Solo"));
   public final ValueSetting blurStrength = new ValueSetting("setting.theme.blurstrength.name", "setting.theme.blurstrength.desc")
      .setValue(1.0F)
      .range(0.5F, 4.0F);
   public final ColorSetting mainGuiColor = new ColorSetting("setting.theme.mainguicolor.name", "setting.theme.mainguicolor.desc")
      .setColor(-15066592)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting guiRectColor = new ColorSetting("setting.theme.guirectcolor.name", "setting.theme.guirectcolor.desc")
      .setColor(-14803420)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting guiRectColor2 = new ColorSetting("setting.theme.guirectcolor2.name", "setting.theme.guirectcolor2.desc")
      .setColor(-14540246)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting rectColor = new ColorSetting("setting.theme.rectcolor.name", "setting.theme.rectcolor.desc")
      .setColor(-14935006)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting rectDarkerColor = new ColorSetting("setting.theme.rectdarkercolor.name", "setting.theme.rectdarkercolor.desc")
      .setColor(-15198176)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting blurRectColor = new ColorSetting("setting.theme.blurrectcolor.name", "setting.theme.blurrectcolor.desc")
      .setColor(-14935006)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting textColor = new ColorSetting("setting.theme.textcolor.name", "setting.theme.textcolor.desc")
      .setColor(-2829096)
      .visible(() -> this.themeMode.isSelected("Custom"));
   public final ColorSetting descriptionColor = new ColorSetting("setting.theme.descriptioncolor.name", "setting.theme.descriptioncolor.desc")
      .setColor(-7894892)
      .visible(() -> this.themeMode.isSelected("Custom"));

   public static Theme getInstance() {
      return Instance.get(Theme.class);
   }

   public Theme() {
      super("module.theme.name", ModuleCategory.CLIENT, false);
      GroupSetting appearanceGroup = new GroupSetting("group.theme.appearance.name", "group.theme.appearance.desc", false)
         .settings(this.themeMode, this.softMode, this.blurStrength);
      GroupSetting clientColorsGroup = new GroupSetting("group.theme.clientcolors.name", "group.theme.clientcolors.desc", false)
         .settings(this.colorMode, this.colorSetting, this.fadeSpeed);
      GroupSetting customThemeGroup = new GroupSetting("group.theme.customtheme.name", "group.theme.customtheme.desc", false)
         .settings(
            this.mainGuiColor,
            this.guiRectColor,
            this.guiRectColor2,
            this.rectColor,
            this.rectDarkerColor,
            this.blurRectColor,
            this.textColor,
            this.descriptionColor
         )
         .visible(() -> this.themeMode.isSelected("Custom"));
      this.setup(new Setting[]{appearanceGroup, clientColorsGroup, customThemeGroup});
      this.lastSystemDarkMode = this.isSystemDarkMode();
      this.applyTheme();
   }

   public float getFadeSpeed() {
      return this.fadeSpeed.getValue();
   }

   public int[] getClientColors() {
      String var1 = this.colorMode.getSelected();

      return switch (var1) {
         case "Solo" -> new int[]{this.colorSetting.getColor1().getColor()};
         case "Duo" -> new int[]{this.colorSetting.getColor1().getColor(), this.colorSetting.getColor2().getColor()};
         case "Triple" -> new int[]{
            this.colorSetting.getColor1().getColor(), this.colorSetting.getColor2().getColor(), this.colorSetting.getColor3().getColor()
         };
         case "Quartet" -> this.colorSetting.getColorValues();
         default -> new int[]{this.colorSetting.getColor1().getColor()};
      };
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.themeMode.isSelected("System")) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastThemeCheckTime >= 5000L) {
            this.lastThemeCheckTime = currentTime;
            boolean currentSystemDarkMode = this.isSystemDarkMode();
            if (currentSystemDarkMode != this.lastSystemDarkMode) {
               this.lastSystemDarkMode = currentSystemDarkMode;
               this.applyTheme();
            }
         }
      }
   }

   private boolean isSystemDarkMode() {
      String osName = System.getProperty("os.name").toLowerCase();
      if (osName.contains("win")) {
         try {
            Process process = Runtime.getRuntime()
               .exec("reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
               if (line.contains("AppsUseLightTheme")) {
                  return line.contains("0x0");
               }
            }
         } catch (Exception var7) {
            LoggerUtil.error("Failed get win theme: " + var7.getMessage());
         }
      }

      if (osName.contains("mac")) {
         try {
            Process process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            return result != null && result.trim().equalsIgnoreCase("Dark");
         } catch (Exception var6) {
            LoggerUtil.error("Failed get mac theme: " + var6.getMessage());
         }
      }

      try {
         Color bgColor = UIManager.getColor("Panel.background");
         if (bgColor != null) {
            int brightness = (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue()) / 3;
            return brightness < 128;
         }
      } catch (Exception var5) {
         LoggerUtil.error("Failed get linux theme: " + var5.getMessage());
      }

      return false;
   }

   public void applyTheme() {
      String mode = this.themeMode.getSelected();
      boolean isSoft = this.softMode.isValue();
      if (this.lastThemeMode.equals("Custom") && !mode.equals("Custom")) {
         this.savedMainGuiColor = this.mainGuiColor.getColor();
         this.savedGuiRectColor = this.guiRectColor.getColor();
         this.savedGuiRectColor2 = this.guiRectColor2.getColor();
         this.savedRectColor = this.rectColor.getColor();
         this.savedRectDarkerColor = this.rectDarkerColor.getColor();
         this.savedBlurRectColor = this.blurRectColor.getColor();
         this.savedTextColor = this.textColor.getColor();
         this.savedDescriptionColor = this.descriptionColor.getColor();
      }

      switch (mode) {
         case "System":
            boolean isDark = this.isSystemDarkMode();
            if (isSoft) {
               this.applyPalette((ColorPalette)(isDark ? new SoftDarkPalette() : new SoftLightPalette()));
            } else {
               this.applyPalette((ColorPalette)(isDark ? new DarkPalette() : new LightPalette()));
            }
            break;
         case "Dark":
            this.applyPalette((ColorPalette)(isSoft ? new SoftDarkPalette() : new DarkPalette()));
            break;
         case "Light":
            this.applyPalette((ColorPalette)(isSoft ? new SoftLightPalette() : new LightPalette()));
            break;
         case "Custom":
            this.mainGuiColor.setColor(this.savedMainGuiColor);
            this.guiRectColor.setColor(this.savedGuiRectColor);
            this.guiRectColor2.setColor(this.savedGuiRectColor2);
            this.rectColor.setColor(this.savedRectColor);
            this.rectDarkerColor.setColor(this.savedRectDarkerColor);
            this.blurRectColor.setColor(this.savedBlurRectColor);
            this.textColor.setColor(this.savedTextColor);
            this.descriptionColor.setColor(this.savedDescriptionColor);
            this.currentPalette = new CustomPalette(
               this.savedMainGuiColor,
               this.savedGuiRectColor,
               this.savedGuiRectColor2,
               this.savedRectColor,
               this.savedRectDarkerColor,
               this.savedBlurRectColor,
               this.savedTextColor,
               this.savedDescriptionColor
            );
      }

      this.lastThemeMode = mode;
   }

   private void applyPalette(ColorPalette palette) {
      this.currentPalette = palette;
      this.mainGuiColor.setColor(palette.mainGuiColor());
      this.guiRectColor.setColor(palette.guiRectColor());
      this.guiRectColor2.setColor(palette.guiRectColor2());
      this.rectColor.setColor(palette.rectColor());
      this.rectDarkerColor.setColor(palette.rectDarkerColor());
      this.blurRectColor.setColor(palette.blurRectColor());
      this.textColor.setColor(palette.textColor());
      this.descriptionColor.setColor(palette.descriptionColor());
   }

   public ColorPalette getCurrentPalette() {
      return this.currentPalette;
   }
}
