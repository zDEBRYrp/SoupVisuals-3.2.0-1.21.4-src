package padej.soup.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.item.HandAnimationEvent;
import padej.soup.api.event.events.item.HandOffsetEvent;
import padej.soup.api.event.events.item.HandScaleEvent;

@ProtIgnore
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      HandOffsetEvent offsetEvent = new HandOffsetEvent(matrices, stack, hand);
      EventManager.callEvent(offsetEvent);
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )}
   )
   private void onRenderItem(
      HeldItemRenderer instance,
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode mode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      Operation<Void> original,
      @Local(ordinal = 0,argsOnly = true) Hand hand
   ) {
      HandScaleEvent scaleEvent = new HandScaleEvent(matrices, stack, hand);
      EventManager.callEvent(scaleEvent);
      if (scaleEvent.getScale() != 1.0F) {
         matrices.push();
         float scale = scaleEvent.getScale();
         matrices.scale(scale, scale, scale);
         original.call(new Object[]{instance, entity, stack, mode, leftHanded, matrices, vertexConsumers, light});
         matrices.pop();
      } else {
         original.call(new Object[]{instance, entity, stack, mode, leftHanded, matrices, vertexConsumers, light});
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
         ordinal = 2
      )}
   )
   private void onSwingArmMainHand(
      HeldItemRenderer instance,
      float swingProgress,
      float equipProgress,
      MatrixStack matrices,
      int armX,
      Arm arm,
      Operation<Void> original,
      @Local(ordinal = 0,argsOnly = true) AbstractClientPlayerEntity player,
      @Local(ordinal = 0,argsOnly = true) Hand hand
   ) {
      HandAnimationEvent event = new HandAnimationEvent(matrices, hand, swingProgress);
      EventManager.callEvent(event);
      if (!event.isCancelled()) {
         original.call(new Object[]{instance, swingProgress, equipProgress, matrices, armX, arm});
      }
   }
}
