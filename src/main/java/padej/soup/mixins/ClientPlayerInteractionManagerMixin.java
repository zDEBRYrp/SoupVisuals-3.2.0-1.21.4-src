package padej.soup.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.block.BlockInteractEvent;
import padej.soup.api.event.events.player.EventAttack;

@ProtIgnore
@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Shadow
   @Final
   private MinecraftClient field_3712;

   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAttackEntityPre(PlayerEntity player, Entity target, CallbackInfo ci) {
      Vec3d hitPos = this.getHitPosition(target);
      EventAttack event = new EventAttack(target, true, hitPos);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At("RETURN")}
   )
   private void onAttackEntityPost(PlayerEntity player, Entity target, CallbackInfo ci) {
      Vec3d hitPos = this.getHitPosition(target);
      EventAttack event = new EventAttack(target, false, hitPos);
      EventManager.callEvent(event);
   }

   @Unique
   private Vec3d getHitPosition(Entity target) {
      HitResult hitResult = this.field_3712.crosshairTarget;
      if (hitResult != null && hitResult.getType() == Type.ENTITY) {
         EntityHitResult entityHit = (EntityHitResult)hitResult;
         if (entityHit.getEntity() == target) {
            return entityHit.getPos();
         }
      }

      return target.getPos().add(0.0, target.getHeight() / 2.0, 0.0);
   }

   @Inject(
      method = {"interactBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      BlockInteractEvent event = new BlockInteractEvent(hitResult.getBlockPos(), hitResult.getSide(), hand, hitResult);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         cir.setReturnValue(ActionResult.FAIL);
      }
   }
}
