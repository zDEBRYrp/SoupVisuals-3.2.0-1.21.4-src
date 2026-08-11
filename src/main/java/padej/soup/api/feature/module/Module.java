package padej.soup.api.feature.module;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import padej.soup.api.event.EventManager;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.SettingRepository;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.notification.NotificationService;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.QuickImports;

public class Module extends SettingRepository implements QuickImports {
   private final String name;
   private final String visibleName;
   private final Category category;
   private final boolean showEnable;
   private final boolean canBind;
   private final Animation animation = new DecelerateAnimation().setMs(150).setValue(1.0);
   private int key = -1;
   private int type = 1;
   public boolean state;
   private boolean hidden = false;
   public boolean expanded = false;
   private static Consumer<Module> globalStateChangeListener;

   public Module(String name, Category category) {
      this.name = name;
      this.category = category;
      this.visibleName = name;
      this.showEnable = true;
      this.canBind = true;
      this.initializeModule();
   }

   public Module(String name, Category category, boolean showEnable, boolean canBind) {
      this.name = name;
      this.category = category;
      this.visibleName = name;
      this.showEnable = showEnable;
      this.canBind = canBind;
      this.initializeModule();
   }

   @Deprecated(
      forRemoval = true
   )
   public Module(String name, Category category, boolean showEnable) {
      this(name, category, showEnable, true);
   }

   @Deprecated(
      forRemoval = true
   )
   public Module(String name, String visibleName, Category category) {
      this.name = name;
      this.visibleName = visibleName;
      this.category = category;
      this.showEnable = true;
      this.canBind = true;
      this.initializeModule();
   }

   @Deprecated(
      forRemoval = true
   )
   public Module(String name, String visibleName, Category category, boolean showEnable) {
      this.name = name;
      this.visibleName = visibleName;
      this.category = category;
      this.showEnable = showEnable;
      this.canBind = true;
      this.initializeModule();
   }

   @Deprecated(
      forRemoval = true
   )
   public Module(String name, String visibleName, Category category, boolean showEnable, boolean canBind) {
      this.name = name;
      this.visibleName = visibleName;
      this.category = category;
      this.showEnable = showEnable;
      this.canBind = canBind;
      this.initializeModule();
   }

   public void switchState() {
      this.setState(!this.state);
   }

   public void setState(boolean state) {
      this.animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
      if (state != this.state) {
         this.state = state;
         this.handleStateChange();
         this.notifyStateChange();
      }
   }

   private void notifyStateChange() {
      if (globalStateChangeListener != null) {
         globalStateChangeListener.accept(this);
      }
   }

   public void setKey(int key) {
      if (this.key != key) {
         this.key = key;
         this.notifyStateChange();
      }
   }

   public void setStateSilent(boolean state) {
      this.animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
      if (state != this.state) {
         this.state = state;
         this.toggleSilent(this.isEnabled());
      }
   }

   private void handleStateChange() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         String localizedName = this.getLocalizedName();
         NotificationService notifications = NotificationService.getInstance();
         if (this.state) {
            String message = LocalizationManager.getInstance().getFormatted("notification.module.enabled", Formatting.GREEN + localizedName + Formatting.RESET);
            if (notifications != null) {
               notifications.show(message, 2000L, SoundManager.ENABLE_MODULE);
            }

            this.activate();
         } else {
            String message = LocalizationManager.getInstance().getFormatted("notification.module.disabled", Formatting.RED + localizedName + Formatting.RESET);
            if (notifications != null) {
               notifications.show(message, 2000L, SoundManager.DISABLE_MODULE);
            }

            this.deactivate();
         }
      }

      this.toggleSilent(this.isEnabled());
   }

   private void toggleSilent(boolean activate) {
      EventManager eventManager = EventManager.getInstance();
      if (eventManager != null) {
         if (activate) {
            eventManager.register(this);
         } else {
            eventManager.unregister(this);
         }
      }
   }

   private void initializeModule() {
      if (!this.showEnable) {
         this.toggleSilent(true);
      } else {
         this.animation.setDirection(Direction.BACKWARDS);
      }
   }

   public boolean isEnabled() {
      return !this.showEnable || this.state;
   }

   public boolean isVisible() {
      return !this.hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public void activate() {
   }

   public void deactivate() {
   }

   @Override
   public final void setup(Setting... settings) {
      String moduleContext = this.extractModuleContext(this.name);

      for (Setting setting : settings) {
         this.setModuleContextRecursive(setting, moduleContext);
      }

      super.setup(settings);
   }

   private String extractModuleContext(String moduleName) {
      if (moduleName != null && moduleName.startsWith("module.") && moduleName.endsWith(".name")) {
         String withoutPrefix = moduleName.substring(7);
         return withoutPrefix.substring(0, withoutPrefix.length() - 5);
      } else {
         return moduleName;
      }
   }

   private void setModuleContextRecursive(Setting setting, String moduleName) {
      setting.setModuleContext(moduleName);
      if (setting instanceof GroupSetting group) {
         for (Setting subSetting : group.getSubSettings()) {
            this.setModuleContextRecursive(subSetting, moduleName);
         }
      }
   }

   public String getVisibleName() {
      String translated = LocalizationManager.getInstance().get(this.visibleName);
      return translated.equals(this.visibleName) ? this.visibleName : translated;
   }

   @Deprecated
   public String getLocalizedName() {
      return this.getVisibleName();
   }

   public String getDescription() {
      return ModuleDescription.getDescription(this);
   }

   public String getVisibleNameKey() {
      return this.visibleName;
   }

   public String getIdentifier() {
      return this.extractModuleContext(this.name);
   }

   public String getName() {
      return this.name;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isShowEnable() {
      return this.showEnable;
   }

   public boolean isCanBind() {
      return this.canBind;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public boolean isState() {
      return this.state;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public static void setGlobalStateChangeListener(Consumer<Module> globalStateChangeListener) {
      Module.globalStateChangeListener = globalStateChangeListener;
   }
}
