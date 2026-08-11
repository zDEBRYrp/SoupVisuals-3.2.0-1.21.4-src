package padej.soup.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.keyboard.KeyEvent;
import padej.soup.implement.features.modules.client.KeyBind;
import padej.soup.implement.menu.MenuScreen;

@ProtIgnore
@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Final
   @Shadow
   private MinecraftClient field_1678;

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")}
   )
   private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      if (key != -1 && window == this.field_1678.getWindow().getHandle()) {
         KeyBind keyBind = KeyBind.getInstance();
         if (action == 1 && keyBind != null && key == keyBind.getMenuKey() && this.field_1678.currentScreen == null) {
            MenuScreen.INSTANCE.openGui();
         }

         EventManager.callEvent(new KeyEvent(this.field_1678.currentScreen, Type.KEYSYM, key, action));
      }
   }
}
