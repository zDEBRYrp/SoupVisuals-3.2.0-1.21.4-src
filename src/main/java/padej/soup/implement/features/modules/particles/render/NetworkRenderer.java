package padej.soup.implement.features.modules.particles.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.particle.ParticleColorUtil;
import padej.soup.base.util.spatial.SpatialGrid3D;
import padej.soup.implement.features.modules.particles.types.WorldParticle;

public class NetworkRenderer {
   public static void renderNetworkLinks(
      MatrixStack matrices,
      List<WorldParticle> networkParticles,
      SpatialGrid3D<WorldParticle> spatialGrid,
      long currentTime,
      float maxLinkDistance,
      IParticleModule module
   ) {
      if (networkParticles.size() >= 2 && spatialGrid != null) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         Matrix4f matrix = new Matrix4f(matrices.peek().getPositionMatrix()).setTranslation(0.0F, 0.0F, 0.0F);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         RenderSystem.enableDepthTest();
         int particleCount = networkParticles.size();
         IdentityHashMap<WorldParticle, Integer> indexByParticle = new IdentityHashMap<>(particleCount * 2);

         for (int i = 0; i < particleCount; i++) {
            indexByParticle.put(networkParticles.get(i), i);
         }

         double maxLinkDistanceSq = maxLinkDistance * maxLinkDistance;
         int[] lineCount = new int[]{0};
         Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
         double cameraX = cameraPos.x;
         double cameraY = cameraPos.y;
         double cameraZ = cameraPos.z;

         for (int i = 0; i < particleCount; i++) {
            WorldParticle p1 = networkParticles.get(i);
            double posX1 = MathUtil.interpolate(p1.getPx(), p1.getX());
            double posY1 = MathUtil.interpolate(p1.getPy(), p1.getY()) + 0.1;
            double posZ1 = MathUtil.interpolate(p1.getPz(), p1.getZ());
            int currentIndex = i;
            spatialGrid.forEachInRadiusWithPositions(
               (float)posX1,
               (float)posY1,
               (float)posZ1,
               maxLinkDistance,
               entry -> {
                  WorldParticle p2 = entry.getObject();
                  if (p1 != p2) {
                     Integer p2Index = indexByParticle.get(p2);
                     if (p2Index != null && p2Index > currentIndex) {
                        double posX2 = MathUtil.interpolate(p2.getPx(), p2.getX());
                        double posY2 = MathUtil.interpolate(p2.getPy(), p2.getY()) + 0.1;
                        double posZ2 = MathUtil.interpolate(p2.getPz(), p2.getZ());
                        double dx = posX2 - posX1;
                        double dy = posY2 - posY1;
                        double dz = posZ2 - posZ1;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < maxLinkDistanceSq && distSq > 1.0E-4) {
                           double dist = Math.sqrt(distSq);
                           float lineAlpha = (float)(1.0 - dist / maxLinkDistance);
                           float particleAlpha1 = p1.getAlpha();
                           float particleAlpha2 = p2.getAlpha();
                           float alpha = Math.min(lineAlpha, Math.min(particleAlpha1, particleAlpha2));
                           int color1;
                           int color2;
                           if (module.getColorMode().isSelected("Sync")) {
                              int baseColor = ColorUtil.getClientColor();
                              color1 = ColorUtil.multAlpha(baseColor, alpha);
                              color2 = color1;
                           } else if (module.getColorMode().isSelected("Vanilla")) {
                              color1 = ColorUtil.multAlpha(p1.getColor().getRGB(), alpha);
                              color2 = ColorUtil.multAlpha(p2.getColor().getRGB(), alpha);
                           } else {
                              int[] colors = module.getCustomColors();
                              if (colors != null && colors.length > 0) {
                                 if (module.getColorAnimation().isSelected("Vertex")) {
                                    int offset1 = System.identityHashCode(p1) % 360;
                                    int offset2 = System.identityHashCode(p2) % 360;
                                    color1 = ParticleColorUtil.getVertexGradientColor(offset1, colors, alpha);
                                    color2 = ParticleColorUtil.getVertexGradientColor(offset2, colors, alpha);
                                 } else {
                                    color1 = ParticleColorUtil.getWaveColor(colors, alpha, p1.getColorOffset());
                                    color2 = ParticleColorUtil.getWaveColor(colors, alpha, p2.getColorOffset());
                                 }
                              } else {
                                 color1 = ColorUtil.multAlpha(-1, alpha);
                                 color2 = color1;
                              }
                           }

                           float localX1 = (float)(posX1 - cameraX);
                           float localY1 = (float)(posY1 - cameraY);
                           float localZ1 = (float)(posZ1 - cameraZ);
                           float localX2 = (float)(posX2 - cameraX);
                           float localY2 = (float)(posY2 - cameraY);
                           float localZ2 = (float)(posZ2 - cameraZ);
                           bufferBuilder.vertex(matrix, localX1, localY1, localZ1)
                              .color((color1 >> 16 & 0xFF) / 255.0F, (color1 >> 8 & 0xFF) / 255.0F, (color1 & 0xFF) / 255.0F, (color1 >> 24 & 0xFF) / 255.0F);
                           bufferBuilder.vertex(matrix, localX2, localY2, localZ2)
                              .color((color2 >> 16 & 0xFF) / 255.0F, (color2 >> 8 & 0xFF) / 255.0F, (color2 & 0xFF) / 255.0F, (color2 >> 24 & 0xFF) / 255.0F);
                           lineCount[0]++;
                        }
                     }
                  }
               }
            );
         }

         if (lineCount[0] > 0) {
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
      }
   }
}
