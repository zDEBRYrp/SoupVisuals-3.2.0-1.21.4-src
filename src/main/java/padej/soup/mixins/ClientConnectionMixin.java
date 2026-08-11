package padej.soup.mixins;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.packet.PacketEvent;

@ProtIgnore
@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void handlePacketPre(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      PacketEvent packetEvent = PacketEvent.RECEIVE_INSTANCE.set(packet, PacketEvent.Type.RECEIVE);
      EventManager.callEvent(packetEvent);
      if (packetEvent.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendPre(Packet<?> packet, CallbackInfo info) {
      PacketEvent packetEvent = PacketEvent.SEND_INSTANCE.set(packet, PacketEvent.Type.SEND);
      EventManager.callEvent(packetEvent);
      if (packetEvent.isCancelled()) {
         info.cancel();
      }
   }
}
