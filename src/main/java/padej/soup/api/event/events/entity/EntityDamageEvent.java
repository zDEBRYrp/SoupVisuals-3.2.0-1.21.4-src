package padej.soup.api.event.events.entity;

import padej.soup.api.event.events.Event;

public record EntityDamageEvent(int entityId, int sourceCauseId, int sourceDirectId) implements Event {
}
