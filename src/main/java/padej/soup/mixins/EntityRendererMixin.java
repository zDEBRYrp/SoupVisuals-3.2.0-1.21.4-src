package padej.soup.mixins;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.repository.client.OnlinePlayersManager;
import padej.soup.base.util.render.BadgeRenderer;

@ProtIgnore
@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<S extends EntityRenderState> {
   @Unique
   private static final int ICON_SIZE = 10;
   @Unique
   private static final int SPACING = 2;
   @Unique
   private static final int ICON_WIDTH = 12;
   @Unique
   private Text soupapi$modifiedText = null;
   @Unique
   private String soupapi$originalUsername = null;

   @Shadow
   public abstract TextRenderer method_3932();

   @Unique
   private boolean soupapi$shouldRenderBadge(EntityRenderState state) {
      if (state instanceof PlayerEntityRenderState) {
         return true;
      } else {
         return state instanceof ArmorStandEntityRenderState ? state.invisible : false;
      }
   }

   @ModifyVariable(
      method = {"renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Text modifyText(Text text, EntityRenderState state) {
      if (!this.soupapi$shouldRenderBadge(state)) {
         this.soupapi$modifiedText = null;
         this.soupapi$originalUsername = null;
         return text;
      } else {
         String displayText = text.getString();
         String realUsername = null;
         if (state instanceof PlayerEntityRenderState playerState) {
            String playerName = playerState.name;
            if (playerName != null && OnlinePlayersManager.isPlayerOnline(playerName)) {
               realUsername = playerName;
            }
         } else {
            for (String username : OnlinePlayersManager.getOnlinePlayers()) {
               if (displayText.equals(username)) {
                  realUsername = username;
                  break;
               }
            }
         }

         if (realUsername != null) {
            this.soupapi$originalUsername = realUsername;
            TextRenderer textRenderer = this.method_3932();
            int spaceWidth = textRenderer.getWidth(" ");
            int spacesNeeded = (12 + spaceWidth - 1) / spaceWidth;
            this.soupapi$modifiedText = Text.literal(" ".repeat(Math.max(0, spacesNeeded)) + displayText);
            return this.soupapi$modifiedText;
         } else {
            this.soupapi$modifiedText = null;
            this.soupapi$originalUsername = null;
            return text;
         }
      }
   }

   @Inject(
      method = {"renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
         ordinal = 0
      )}
   )
   private void onRenderLabel(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      if (this.soupapi$modifiedText != null && this.soupapi$originalUsername != null) {
         Vec3d vec3d = state.nameLabelPos;
         if (vec3d != null) {
            TextRenderer textRenderer = this.method_3932();
            float textWidth = -textRenderer.getWidth(this.soupapi$modifiedText) / 2;
            float badgeX = textWidth - 2.0F + 1.0F;
            float badgeY = -1.0F;
            boolean seeThrough = !state.sneaking;
            BadgeRenderer.render3D(matrices, vertexConsumers, badgeX, badgeY, seeThrough, light, this.soupapi$originalUsername);
         }
      }
   }
}
