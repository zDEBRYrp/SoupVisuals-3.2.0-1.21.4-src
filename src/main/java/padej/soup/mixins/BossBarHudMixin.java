package padej.soup.mixins;

import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.implement.features.modules.hud.BossBars;

@ProtIgnore
@Mixin({BossBarHud.class})
public class BossBarHudMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void render(CallbackInfo ci) {
      if (BossBars.getInstance().isEnabled()) {
         ci.cancel();
      }
   }
}
