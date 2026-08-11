package padej.soup.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.implement.menu.MenuScreen;

@ProtIgnore
@Mixin({Screen.class})
public class ScreenMixin {
   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void settingsForMenuScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof MenuScreen) {
         ci.cancel();
      }
   }
}
