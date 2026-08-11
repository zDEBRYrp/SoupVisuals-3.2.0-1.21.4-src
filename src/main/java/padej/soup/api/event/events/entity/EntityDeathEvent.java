package padej.soup.api.event.events.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import padej.soup.api.event.events.Event;

public record EntityDeathEvent(LivingEntity entity, DamageSource damageSource) implements Event {
}
