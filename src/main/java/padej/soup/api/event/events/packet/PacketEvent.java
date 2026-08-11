package padej.soup.api.event.events.packet;

import net.minecraft.network.packet.Packet;
import padej.soup.api.event.events.callables.EventCancellable;

public class PacketEvent extends EventCancellable {
   public static final PacketEvent RECEIVE_INSTANCE = new PacketEvent();
   public static final PacketEvent SEND_INSTANCE = new PacketEvent();
   private Packet<?> packet;
   private PacketEvent.Type type;

   public PacketEvent set(Packet<?> packet, PacketEvent.Type type) {
      this.packet = packet;
      this.type = type;
      this.setCancelled(false);
      return this;
   }

   public boolean isSend() {
      return this.type == PacketEvent.Type.SEND;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public PacketEvent.Type getType() {
      return this.type;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public void setType(PacketEvent.Type type) {
      this.type = type;
   }

   public static enum Type {
      SEND,
      RECEIVE;
   }
}
