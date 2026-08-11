package padej.soup.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.entity.EntityDeathEvent;
import padej.soup.api.event.events.item.SwingDurationEvent;
import padej.soup.api.event.events.player.JumpEvent;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.client.FakePlayerEntity;

@ProtIgnore
@Mixin(
   value = {LivingEntity.class},
   priority = 999
)
public abstract class LivingEntityMixin {
   @Unique
   private final MinecraftClient client = Main.mc;

   @Shadow
   public abstract boolean method_6059(RegistryEntry<StatusEffect> var1);

   @Shadow
   @Nullable
   public abstract StatusEffectInstance method_6112(RegistryEntry<StatusEffect> var1);

   @Inject(
      method = {"tickRiptide"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void skipRiptideOnFakePlayer(Box a, Box b, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      Box box = a.union(b);

      for (Entity entity : self.getWorld().getOtherEntities(self, box)) {
         if (entity instanceof FakePlayerEntity) {
            ci.cancel();
            return;
         }
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void jump(CallbackInfo info) {
      if ((Object)this instanceof ClientPlayerEntity player) {
         JumpEvent event = new JumpEvent(player);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == this.client.player) {
         SwingDurationEvent event = new SwingDurationEvent();
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(this.client.player)) {
               animation *= 6 - (1 + StatusEffectUtil.getHasteAmplifier(this.client.player));
            } else if (this.method_6059(StatusEffects.MINING_FATIGUE)) {
               StatusEffectInstance effect = this.method_6112(StatusEffects.MINING_FATIGUE);
               animation *= 6 + (1 + effect.getAmplifier()) * 2;
            } else {
               animation *= 6.0F;
            }

            cir.setReturnValue((int)animation);
         }
      }
   }

   @Inject(
      method = {"onDeath"},
      at = {@At("HEAD")},
      require = 0
   )
   private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      EventManager.callEvent(new EntityDeathEvent(self, damageSource));
   }
}
