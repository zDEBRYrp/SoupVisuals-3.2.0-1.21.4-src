package padej.soup.api.event.events.world;

import net.minecraft.client.world.ClientWorld;
import padej.soup.api.event.events.Event;

public record PlayerJoinWorldEvent(ClientWorld world) implements Event {
}
