package padej.soup.mixins;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.accessor.IWorldRenderer;
import padej.soup.base.util.render.outline.EntityOutline;
import padej.soup.implement.features.modules.visuals.BetterGlow;
import padej.soup.implement.features.modules.visuals.BlockHighLight;

@ProtIgnore
@Mixin({WorldRenderer.class})
public abstract class WorldRendererMixin implements IWorldRenderer {
   @Shadow
   @Final
   private MinecraftClient field_4088;
   @Shadow
   private Framebuffer field_53080;
   @Shadow
   @Final
   private DefaultFramebufferSet field_53081;
   @Unique
   private Stack<Framebuffer> framebufferStack;
   @Unique
   private Stack<Handle<Framebuffer>> framebufferHandleStack;
   @Unique
   private boolean isRenderingCustomOutline = false;

   @Shadow
   protected abstract void method_62202(
      FrameGraphBuilder var1,
      Frustum var2,
      Camera var3,
      Matrix4f var4,
      Matrix4f var5,
      Fog var6,
      boolean var7,
      boolean var8,
      RenderTickCounter var9,
      Profiler var10
   );

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/render/Fog;ZZLnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V"
      )
   )
   private void onRender(
      WorldRenderer instance,
      FrameGraphBuilder frameGraphBuilder,
      Frustum frustum,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Fog fog,
      boolean renderBlockOutline,
      boolean hasEntitiesToRender,
      RenderTickCounter renderTickCounter,
      Profiler profiler
   ) {
      BlockHighLight blockHighLight = BlockHighLight.getInstance();
      boolean shouldRenderBlockOutline = blockHighLight == null || !blockHighLight.isState();
      this.method_62202(
         frameGraphBuilder, frustum, camera, positionMatrix, projectionMatrix, fog, shouldRenderBlockOutline, hasEntitiesToRender, renderTickCounter, profiler
      );
   }

   @Shadow
   protected abstract void method_22977(Entity var1, double var2, double var4, double var6, float var8, MatrixStack var9, VertexConsumerProvider var10);

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo info) {
      this.framebufferStack = new ObjectArrayList();
      this.framebufferHandleStack = new ObjectArrayList();
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRenderHead(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      CallbackInfo ci
   ) {
      if (!EntityOutline.isInitialized()) {
         EntityOutline.get();
      }

      if (BetterGlow.getInstance().isEnabled()) {
         EntityOutline.get().beginRender();
      }
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderEntity(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo info
   ) {
      if (BetterGlow.getInstance().isEnabled() && EntityOutline.isInitialized() && !this.isRenderingCustomOutline) {
         if (vertexConsumers instanceof OutlineVertexConsumerProvider && this.field_4088.hasOutline(entity)) {
            EntityOutline outline = EntityOutline.get();
            int color = entity.getTeamColorValue();
            this.soup$pushFramebuffer(outline.framebuffer);
            EntityOutline.rendering = true;
            this.isRenderingCustomOutline = true;
            outline.vertexConsumerProvider.setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
            this.method_22977(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, outline.vertexConsumerProvider);
            this.isRenderingCustomOutline = false;
            EntityOutline.rendering = false;
            this.soup$popFramebuffer();
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"method_62214"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"
      )}
   )
   private void onRenderEnd(CallbackInfo ci) {
      if (BetterGlow.getInstance().isEnabled() && EntityOutline.isInitialized()) {
         EntityOutline.get().endRender();
      }
   }

   @Inject(
      method = {"onResized"},
      at = {@At("HEAD")}
   )
   private void onResize(int width, int height, CallbackInfo info) {
      if (EntityOutline.isInitialized()) {
         EntityOutline.get().onResized(width, height);
      }
   }

   @Override
   public void soup$pushFramebuffer(Framebuffer framebuffer) {
      this.framebufferStack.push(this.field_53080);
      this.field_53080 = framebuffer;
      this.framebufferHandleStack.push(this.field_53081.entityOutlineFramebuffer);
      this.field_53081.entityOutlineFramebuffer = () -> framebuffer;
   }

   @Override
   public void soup$popFramebuffer() {
      this.field_53080 = (Framebuffer)this.framebufferStack.pop();
      this.field_53081.entityOutlineFramebuffer = (Handle)this.framebufferHandleStack.pop();
   }
}
