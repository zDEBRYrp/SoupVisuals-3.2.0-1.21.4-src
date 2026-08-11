package padej.soup.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.keyboard.HotBarScrollEvent;
import padej.soup.api.event.events.keyboard.KeyEvent;
import padej.soup.api.event.events.keyboard.MouseRotationEvent;
import padej.soup.implement.features.modules.client.KeyBind;
import padej.soup.implement.menu.MenuScreen;

@ProtIgnore
@Mixin({Mouse.class})
public class MouseMixin {
   @Final
   @Shadow
   private MinecraftClient field_1779;

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   public void onMouseButtonHook(long window, int button, int action, int mods, CallbackInfo ci) {
      if (button != -1 && window == this.field_1779.getWindow().getHandle()) {
         KeyBind keyBind = KeyBind.getInstance();
         if (action == 1 && keyBind != null && button == keyBind.getMenuKey() && this.field_1779.currentScreen == null) {
            MenuScreen.INSTANCE.openGui();
         }

         EventManager.callEvent(new KeyEvent(this.field_1779.currentScreen, Type.MOUSE, button, action));
      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
      )},
      cancellable = true
   )
   public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      HotBarScrollEvent event = new HotBarScrollEvent(horizontal, vertical);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @WrapWithCondition(
      method = {"updateMouse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      )},
      require = 1,
      allow = 1
   )
   private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      MouseRotationEvent event = new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         return false;
      } else {
         instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
         return false;
      }
   }
}
