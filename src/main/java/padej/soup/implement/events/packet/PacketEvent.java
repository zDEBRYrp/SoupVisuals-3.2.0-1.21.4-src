package padej.soup.implement.events.packet;

import net.minecraft.network.packet.Packet;

@Deprecated
public class PacketEvent extends padej.soup.api.event.events.packet.PacketEvent {
   public PacketEvent(Packet<?> packet, padej.soup.api.event.events.packet.PacketEvent.Type type) {
      this.set(packet, type);
   }
}
