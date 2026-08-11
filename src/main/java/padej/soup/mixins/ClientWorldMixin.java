package padej.soup.mixins;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.implement.features.modules.hud.TotemPop;
import padej.soup.implement.features.modules.other.EndCrystal;
import padej.soup.implement.features.modules.other.HitSounds;

@ProtIgnore
@Mixin({ClientWorld.class})
public class ClientWorldMixin {
   @ModifyVariable(
      method = {"playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private float modifySoundVolume(float volume, double x, double y, double z, SoundEvent event) {
      TotemPop totemPop = TotemPop.getInstance();
      if (totemPop != null && totemPop.isEnabled() && event == SoundEvents.ITEM_TOTEM_USE) {
         return volume * totemPop.totemVolume.getValue();
      } else {
         HitSounds hitSounds = HitSounds.getInstance();
         if (hitSounds != null && hitSounds.isEnabled()) {
            if (event == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP) {
               return volume * hitSounds.getSweepVolume().getValue();
            }

            if (event == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT) {
               return volume * hitSounds.getCritVolume().getValue();
            }

            if (event == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK) {
               return volume * hitSounds.getKnockbackVolume().getValue();
            }
         }

         return volume;
      }
   }

   @Inject(
      method = {"removeEntity"},
      at = {@At("HEAD")}
   )
   private void onEntityRemoved(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      ClientWorld world = (ClientWorld)(Object)this;
      Entity entity = world.getEntityById(entityId);
      if (entity instanceof EndCrystalEntity && EndCrystal.getInstance().isEnabled()) {
         Vec3d crystalPos = entity.getPos();
         EndCrystal.getInstance().checkRemovedCrystal(crystalPos);
      }
   }

   @Inject(
      method = {"disconnect"},
      at = {@At("HEAD")}
   )
   private void onWorldDisconnect(CallbackInfo ci) {
      EndCrystal instance = EndCrystal.getInstance();
      if (instance != null) {
         instance.clearTracking();
      }
   }
}
