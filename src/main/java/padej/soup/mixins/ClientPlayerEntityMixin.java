package padej.soup.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.message.MessageSignatureData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.chat.SendChatMessageEvent;
import padej.soup.api.event.events.container.CloseScreenEvent;
import padej.soup.api.event.events.player.TickEvent;

@ProtIgnore
@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Final
   @Shadow
   protected MinecraftClient field_3937;

   @Shadow
   public abstract float getPitch(float tickDelta);

   @Shadow
   public abstract float getYaw(float tickDelta);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo info) {
      if (this.field_3937.player != null && this.field_3937.world != null) {
         EventManager.callEvent(TickEvent.INSTANCE);
      }
   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo info) {
      CloseScreenEvent event = new CloseScreenEvent(this.field_3937.currentScreen);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"sendChatMessage"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void onSendChatMessage(String message, MessageSignatureData signature, CallbackInfo ci) {
      SendChatMessageEvent event = new SendChatMessageEvent(message);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
