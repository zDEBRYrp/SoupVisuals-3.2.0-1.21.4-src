package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.core.server.ServerConfigManager;
import padej.soup.implement.features.modules.visuals.ChinaHat;

@Environment(EnvType.CLIENT)
public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> implements QuickImports {
   private static final Map<Integer, PlayerEntity> PLAYER_CACHE = new HashMap<>();
   private static long lastCacheUpdate = 0L;
   private static final long CACHE_REFRESH_MS = 1000L;

   public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   public void render(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance
   ) {
      if (ChinaHat.getInstance().isEnabled()) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - lastCacheUpdate > 1000L) {
            updatePlayerCache();
            lastCacheUpdate = currentTime;
         }

         PlayerEntity player = null;
         if (state.id == mc.player.getId()) {
            player = mc.player;
         } else {
            if (!ChinaHat.getInstance().getRenderOnFriends().isValue()) {
               return;
            }

            player = PLAYER_CACHE.get(state.id);
            if (player == null) {
               return;
            }
         }

         if (player != null) {
            VisibleUtils.VisibilityLevel level = VisibleUtils.getVisibilityLevel(player);
            if (ServerConfigManager.showChinaHat(level.name())) {
               matrices.push();
               PlayerEntityModel playerModel = (PlayerEntityModel)this.getContextModel();
               ModelPart head = playerModel.getHead();
               head.rotate(matrices);
               ItemStack headItem = player.getEquippedStack(EquipmentSlot.HEAD);
               float offset = headItem.isEmpty() ? -0.72F : -0.78F;
               matrices.translate(0.0F, offset, 0.0F);
               float rotationSpeed = ChinaHat.getInstance().getRotationSpeed().getValue();
               if (rotationSpeed != 0.0F) {
                  float rotationAngle = (float)(System.currentTimeMillis() % 360000L) / 1000.0F * rotationSpeed * 360.0F;
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
               }

               this.renderHatGeometry(matrices, vertexConsumers);
               matrices.pop();
            }
         }
      }
   }

   private void renderHatGeometry(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      ChinaHat module = ChinaHat.getInstance();
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(ChinaHatBatchRenderer.HAT_LAYER);
      int step = 9;
      float centerY = 0.0F;
      float brimY = 0.3F;
      float radius = 0.65F;

      for (int i = 0; i < 360; i += step) {
         float angle1 = i * (float) (Math.PI / 180.0);
         float angle2 = (i + step) * (float) (Math.PI / 180.0);
         float x1 = MathHelper.sin(angle1) * radius;
         float z1 = -MathHelper.cos(angle1) * radius;
         float x2 = MathHelper.sin(angle2) * radius;
         float z2 = -MathHelper.cos(angle2) * radius;
         int color1 = this.getHatGradientColor(i, module);
         int color2 = this.getHatGradientColor(i + step, module);
         int centerColor = this.getHatGradientColor(i, module);
         int alphaColor1 = color1 & 16777215 | (int)(module.getEdgeAlpha().getValue() * 255.0F) << 24;
         int alphaColor2 = color2 & 16777215 | (int)(module.getEdgeAlpha().getValue() * 255.0F) << 24;
         int alphaCenterColor = centerColor & 16777215 | (int)(module.getCenterAlpha().getValue() * 255.0F) << 24;
         vertexConsumer.vertex(matrix, 0.0F, centerY, 0.0F).color(alphaCenterColor);
         vertexConsumer.vertex(matrix, x1, brimY, z1).color(alphaColor1);
         vertexConsumer.vertex(matrix, x2, brimY, z2).color(alphaColor2);
      }

      for (int i = 0; i < 360; i += step) {
         float angle1 = i * (float) (Math.PI / 180.0);
         float angle2 = (i + step) * (float) (Math.PI / 180.0);
         float x1 = MathHelper.sin(angle1) * radius;
         float z1 = -MathHelper.cos(angle1) * radius;
         float x2 = MathHelper.sin(angle2) * radius;
         float z2 = -MathHelper.cos(angle2) * radius;
         int color1 = ColorUtil.darker(this.getHatGradientColor(i, module), 4);
         int color2 = ColorUtil.darker(this.getHatGradientColor(i + step, module), 4);
         int centerColor = ColorUtil.darker(this.getHatGradientColor(i, module));
         int alphaColor1 = color1 & 16777215 | (int)(module.getEdgeAlpha().getValue() * 255.0F) << 24;
         int alphaColor2 = color2 & 16777215 | (int)(module.getEdgeAlpha().getValue() * 255.0F) << 24;
         int alphaCenterColor = centerColor & 16777215 | (int)(module.getCenterAlpha().getValue() * 255.0F) << 24;
         vertexConsumer.vertex(matrix, x2, brimY, z2).color(alphaColor2);
         vertexConsumer.vertex(matrix, x1, brimY, z1).color(alphaColor1);
         vertexConsumer.vertex(matrix, 0.0F, centerY, 0.0F).color(alphaCenterColor);
      }

      this.renderHatOutline(matrices, module, step, radius, brimY);
   }

   private void renderHatOutline(MatrixStack matrices, ChinaHat module, int step, float radius, float brimY) {
      if (!(mc.currentScreen instanceof CreativeInventoryScreen) && !(mc.currentScreen instanceof InventoryScreen)) {
         GL11.glHint(3154, 4354);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         RenderSystem.lineWidth(4.0F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
         Entry entry = matrices.peek();
         int i = 0;

         while (i < 360) {
            float x1 = MathHelper.sin(i * (float) (Math.PI / 180.0)) * radius;
            float z1 = -MathHelper.cos(i * (float) (Math.PI / 180.0)) * radius;
            float x2 = MathHelper.sin((i + step) * (float) (Math.PI / 180.0)) * radius;
            float z2 = -MathHelper.cos((i + step) * (float) (Math.PI / 180.0)) * radius;
            Vector3f start = new Vector3f(x1, brimY, z1);
            Vector3f end = new Vector3f(x2, brimY, z2);
            int color1 = this.getHatGradientColor(i, module);
            int color2 = this.getHatGradientColor(i + step, module);
            int darkerColor1 = ColorUtil.darker(color1) | 0xFF000000;
            int darkerColor2 = ColorUtil.darker(color2) | 0xFF000000;
            Render3DUtil.vertexLine(entry, buffer, start, end, darkerColor1, darkerColor2);
            i += step;
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private int getHatGradientColor(float angle, ChinaHat module) {
      int[] customColors = module.getCustomColors();
      if (customColors != null && customColors.length > 0) {
         float normalizedAngle = angle % 360.0F;
         if (normalizedAngle < 0.0F) {
            normalizedAngle += 360.0F;
         }

         float degreesPerColor = 360.0F / customColors.length;
         int colorIndex1 = (int)(normalizedAngle / degreesPerColor) % customColors.length;
         int colorIndex2 = (colorIndex1 + 1) % customColors.length;
         float factor = normalizedAngle % degreesPerColor / degreesPerColor;
         int color1 = customColors[colorIndex1];
         int color2 = customColors[colorIndex2];
         return this.interpolateColor(color1, color2, factor);
      } else {
         return ColorUtil.getClientColor();
      }
   }

   private int interpolateColor(int color1, int color2, float factor) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * factor);
      int r = (int)(r1 + (r2 - r1) * factor);
      int g = (int)(g1 + (g2 - g1) * factor);
      int b = (int)(b1 + (b2 - b1) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static void updatePlayerCache() {
      PLAYER_CACHE.clear();
      if (mc.world != null) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (FriendUtils.isFriend(player)) {
               PLAYER_CACHE.put(player.getId(), player);
            }
         }
      }
   }
}
