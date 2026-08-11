package padej.soup.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.base.util.render.shader.ShaderHelper;
import padej.soup.implement.features.modules.hud.TotemPop;
import padej.soup.implement.features.modules.other.AspectRatio;
import padej.soup.implement.features.modules.visuals.HandsShader;

@ProtIgnore
@Mixin(
   value = {GameRenderer.class},
   priority = 1001
)
public abstract class GameRendererMixin {
   @Shadow
   private float field_4005;
   @Shadow
   private float field_3988;
   @Shadow
   private float field_4004;
   @Shadow
   private float field_4025;

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"
      )}
   )
   public void beforeRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!ShaderHelper.isInitialized()) {
         ShaderHelper.initShadersIfNeeded();
      }

      HandsShader hands = HandsShader.getInstance();
      if (hands != null && hands.isEnabled()) {
         if (hands.getMainShader().isSelected("Glass") && hands.getGlassHideHand().isValue()) {
            RenderSystem.colorMask(false, false, false, false);
         } else if (hands.getMainShader().isSelected("Invert") && hands.getInvertHideHand().isValue()) {
            RenderSystem.colorMask(false, false, false, false);
         }
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )}
   )
   public void afterRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      HandsShader hands = HandsShader.getInstance();
      if (hands != null && hands.isEnabled()) {
         if (hands.getMainShader().isSelected("Glass") && hands.getGlassHideHand().isValue()
            || hands.getMainShader().isSelected("Invert") && hands.getInvertHideHand().isValue()) {
            RenderSystem.colorMask(true, true, true, true);
         }

         ShaderHelper.checkFramebuffers();
         hands.render();
      }
   }

   @Inject(
      method = {"renderFloatingItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void afterTranslateInject(DrawContext context, float tickDelta, CallbackInfo ci) {
      MatrixStack matrices = context.getMatrices();
      TotemPop instance = TotemPop.getInstance();
      if (instance != null && instance.isEnabled()) {
         float scale = instance.totemSize.getValue();
         matrices.scale(scale, scale, scale);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, instance.totemAlpha.getValue());
      }
   }

   @Inject(
      method = {"renderFloatingItem"},
      at = {@At("TAIL")}
   )
   private void afterRenderFloatingItem(DrawContext context, float tickDelta, CallbackInfo ci) {
      TotemPop instance = TotemPop.getInstance();
      if (instance != null && instance.isEnabled()) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      AspectRatio aspectRatio = AspectRatio.getInstance();
      if (aspectRatio != null && aspectRatio.isEnabled()) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.peek().getPositionMatrix().identity();
         float factor = aspectRatio.getRatio();
         if (this.field_4005 != 1.0F) {
            matrixStack.translate(this.field_3988, -this.field_4004, 0.0F);
            matrixStack.scale(this.field_4005, this.field_4005, 1.0F);
         }

         matrixStack.peek()
            .getPositionMatrix()
            .mul(new Matrix4f().setPerspective((float)(fovDegrees * (Math.PI / 180.0)), factor, 0.05F, this.field_4025 * 4.0F));
         cir.setReturnValue(matrixStack.peek().getPositionMatrix());
      }
   }
}
