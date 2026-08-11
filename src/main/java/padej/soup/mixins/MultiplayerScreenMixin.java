package padej.soup.mixins;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.core.server.ServerConfigManager;

@ProtIgnore
@Mixin({MultiplayerScreen.class})
public class MultiplayerScreenMixin {
   @Inject(
      method = {"connect(Lnet/minecraft/client/network/ServerInfo;)V"},
      at = {@At("HEAD")}
   )
   private void connectHook(CallbackInfo ci) {
      ServerConfigManager.refreshAll();
   }
}
