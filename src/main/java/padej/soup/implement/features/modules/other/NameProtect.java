package padej.soup.implement.features.modules.other;

import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.render.TextFactoryEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.TextSetting;
import padej.soup.api.repository.friend.FriendUtils;

public class NameProtect extends Module {
   private final TextSetting nameSetting = new TextSetting("setting.nameprotect.name.name", "setting.nameprotect.name.desc").setText("Protect").setMax(16);
   private final BooleanSetting friendsSetting = new BooleanSetting("setting.nameprotect.friends.name", "setting.nameprotect.friends.desc").setValue(true);

   public NameProtect() {
      super("module.nameprotect.name", ModuleCategory.OTHER);
      GroupSetting protectionGroup = new GroupSetting("group.nameprotect.protection.name", "group.nameprotect.protection.desc", false)
         .settings(this.nameSetting, this.friendsSetting);
      this.setup(new Setting[]{protectionGroup});
   }

   @EventHandler
   public void onTextFactory(TextFactoryEvent e) {
      e.replaceText(mc.getSession().getUsername(), this.nameSetting.getText());
      if (this.friendsSetting.isValue()) {
         FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), this.nameSetting.getText()));
      }
   }

   public TextSetting getNameSetting() {
      return this.nameSetting;
   }

   public BooleanSetting getFriendsSetting() {
      return this.friendsSetting;
   }
}
