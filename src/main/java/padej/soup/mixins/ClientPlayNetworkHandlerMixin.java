package padej.soup.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.chat.ReceiveChatMessageEvent;
import padej.soup.api.event.events.entity.EntityDamageEvent;
import padej.soup.api.event.events.world.PlayerJoinWorldEvent;
import padej.soup.core.Main;
import padej.soup.core.server.ServerConfigManager;
import padej.soup.implement.features.modules.particles.TotemParticles;

@ProtIgnore
@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {
   @Shadow
   private ClientWorld field_3699;

   @Shadow
   private static ItemStack method_19691(PlayerEntity player) {
      return null;
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At("RETURN")}
   )
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      MinecraftClient client = Main.mc;
      if (client.getCurrentServerEntry() != null) {
         String serverAddress = client.getCurrentServerEntry().address;
         ServerConfigManager.onServerJoin(serverAddress);
      }

      if (this.field_3699 != null) {
         EventManager.callEvent(new PlayerJoinWorldEvent(this.field_3699));
      }
   }

   @Inject(
      method = {"onGameMessage"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
      ReceiveChatMessageEvent event = new ReceiveChatMessageEvent(packet.content());
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onChatMessage"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
      ReceiveChatMessageEvent event = new ReceiveChatMessageEvent(Text.literal(packet.body().content()));
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntityDamage"},
      at = {@At("HEAD")}
   )
   private void onEntityDamagePacket(EntityDamageS2CPacket packet, CallbackInfo ci) {
      EventManager.callEvent(new EntityDamageEvent(packet.entityId(), packet.sourceCauseId(), packet.sourceDirectId()));
   }

   @Inject(
      method = {"onEntityStatus"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTotemPopCustomParticles(EntityStatusS2CPacket packet, CallbackInfo ci) {
      MinecraftClient client = Main.mc;
      TotemParticles module = TotemParticles.getInstance();
      if (module != null && module.isEnabled()) {
         if (packet.getStatus() == 35) {
            Entity entity = packet.getEntity(this.field_3699);
            if (entity != null) {
               this.field_3699.playSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_TOTEM_USE, entity.getSoundCategory(), 1.0F, 1.0F, false);
               if (entity == client.player) {
                  client.gameRenderer.showFloatingItem(method_19691(client.player));
               }

               client.execute(() -> module.onTotemPop(entity));
               ci.cancel();
            }
         }
      }
   }
}
