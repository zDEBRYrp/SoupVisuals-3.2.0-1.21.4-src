package padej.soup.implement.features.modules.visuals;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.base.util.other.Instance;

public class Hitboxes extends Module {
   private final SelectSetting renderMode = new SelectSetting("setting.hitboxes.rendermode.name", "setting.hitboxes.rendermode.desc")
      .value("Fill", "Outline", "Both")
      .selected("Both");
   private final BooleanSetting eyesLine = new BooleanSetting("setting.hitboxes.eyesline.name", "setting.hitboxes.eyesline.desc").setValue(false);
   private final SelectSetting colorMode = new SelectSetting("setting.hitboxes.colormode.name", "setting.hitboxes.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final ColorSetting hitboxColor = new ColorSetting("setting.hitboxes.color.name", "setting.hitboxes.color.desc")
      .setColor(-1)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final ColorSetting friendHitboxColor = new ColorSetting("setting.hitboxes.friendcolor.name", "setting.hitboxes.friendcolor.desc")
      .setColor(-16711936)
      .visible(() -> this.colorMode.isSelected("Custom"));

   public static Hitboxes getInstance() {
      return Instance.get(Hitboxes.class);
   }

   public Hitboxes() {
      super("module.hitboxes.name", ModuleCategory.VISUALS);
      GroupSetting renderGroup = new GroupSetting("group.hitboxes.render.name", "group.hitboxes.render.desc", false).settings(this.renderMode, this.eyesLine);
      GroupSetting colorGroup = new GroupSetting("group.hitboxes.colors.name", "group.hitboxes.colors.desc", false)
         .settings(this.colorMode, this.hitboxColor, this.friendHitboxColor);
      this.setup(new Setting[]{renderGroup, colorGroup});
   }

   public SelectSetting getRenderMode() {
      return this.renderMode;
   }

   public BooleanSetting getEyesLine() {
      return this.eyesLine;
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public ColorSetting getHitboxColor() {
      return this.hitboxColor;
   }

   public ColorSetting getFriendHitboxColor() {
      return this.friendHitboxColor;
   }
}
