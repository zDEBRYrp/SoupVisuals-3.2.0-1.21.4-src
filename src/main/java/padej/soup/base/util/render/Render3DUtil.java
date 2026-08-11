package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public final class Render3DUtil implements QuickImports {
   private static final Vector3f SCRATCH_START = new Vector3f();
   private static final Vector3f SCRATCH_END = new Vector3f();
   private static final Vector3f SCRATCH_NORMAL = new Vector3f();
   private static final Vector3f SCRATCH_V1 = new Vector3f();
   private static final Vector3f SCRATCH_V2 = new Vector3f();
   private static final Vector3f SCRATCH_V3 = new Vector3f();
   private static final Vector3f SCRATCH_V4 = new Vector3f();
   private static final int POOL_CAP = 1024;
   private static final ArrayDeque<Render3DUtil.Line> LINE_POOL = new ArrayDeque<>(256);
   private static final ArrayDeque<Render3DUtil.Quad> QUAD_POOL = new ArrayDeque<>(256);
   private static final ArrayDeque<Render3DUtil.Texture> TEXTURE_POOL = new ArrayDeque<>(64);
   private static final ArrayDeque<Render3DUtil.Triangle> TRI_POOL = new ArrayDeque<>(256);
   private static final ArrayDeque<Render3DUtil.GhostTexture> GHOST_POOL = new ArrayDeque<>(64);
   private static final Map<VoxelShape, Pair<List<Box>, List<Render3DUtil.Line>>> SHAPE_OUTLINES = new HashMap<>();
   public static final List<Render3DUtil.Texture> TEXTURE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Texture> TEXTURE = new ArrayList<>();
   public static final List<Render3DUtil.Line> LINE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Line> LINE = new ArrayList<>();
   public static final List<Render3DUtil.Quad> QUAD_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Quad> QUAD = new ArrayList<>();
   public static final List<Render3DUtil.Triangle> TRI_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Triangle> TRI = new ArrayList<>();
   public static final List<Render3DUtil.GhostTexture> GHOST_TEXTURE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.GhostTexture> GHOST_TEXTURE = new ArrayList<>();
   private static final Map<Identifier, List<Render3DUtil.Texture>> TEXTURE_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, List<Render3DUtil.Texture>> TEXTURE_DEPTH_BATCHES = new LinkedHashMap<>();
   private static final Map<Float, List<Render3DUtil.Line>> LINE_BATCHES = new LinkedHashMap<>();
   private static final Map<Float, List<Render3DUtil.Line>> LINE_DEPTH_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> GHOST_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> GHOST_DEPTH_BATCHES = new LinkedHashMap<>();
   public static Matrix4f lastProjMat = new Matrix4f();
   public static net.minecraft.client.util.math.MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();
   private static final Matrix4f lastWorldRotationMatrix = new Matrix4f();
   private static Vec3d lastCameraPos = Vec3d.ZERO;

   public static void onWorldRender() {
      prepareWorldPrecisionState();
      ParticleBatchRenderer.renderBatches();
      if (!TEXTURE.isEmpty()) {
         groupTexturesById(TEXTURE, TEXTURE_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

         for (Entry<Identifier, List<Render3DUtil.Texture>> batch : TEXTURE_BATCHES.entrySet()) {
            if (!batch.getValue().isEmpty()) {
               RenderSystem.setShaderTexture(0, batch.getKey());
               RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (Render3DUtil.Texture tex : batch.getValue()) {
                  quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.disableBlend();
         TEXTURE_BATCHES.values().forEach(List::clear);
         returnAll(TEXTURE, TEXTURE_POOL);
         TEXTURE.clear();
      }

      if (!TEXTURE_DEPTH.isEmpty()) {
         groupTexturesById(TEXTURE_DEPTH, TEXTURE_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);

         for (Entry<Identifier, List<Render3DUtil.Texture>> batchx : TEXTURE_DEPTH_BATCHES.entrySet()) {
            if (!batchx.getValue().isEmpty()) {
               RenderSystem.setShaderTexture(0, batchx.getKey());
               RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (Render3DUtil.Texture tex : batchx.getValue()) {
                  quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.disableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         TEXTURE_DEPTH_BATCHES.values().forEach(List::clear);
         returnAll(TEXTURE_DEPTH, TEXTURE_POOL);
         TEXTURE_DEPTH.clear();
      }

      if (!LINE.isEmpty()) {
         GL11.glEnable(2848);
         groupLinesByWidth(LINE, LINE_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

         for (Entry<Float, List<Render3DUtil.Line>> batchxx : LINE_BATCHES.entrySet()) {
            if (!batchxx.getValue().isEmpty()) {
               RenderSystem.lineWidth(batchxx.getKey());
               BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);

               for (Render3DUtil.Line line : batchxx.getValue()) {
                  vertexLine(line.entry, buffer, line.start, line.end, line.colorStart, line.colorEnd);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         GL11.glDisable(2848);
         LINE_BATCHES.values().forEach(List::clear);
         returnAll(LINE, LINE_POOL);
         LINE.clear();
      }

      if (!QUAD.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         returnAll(QUAD, QUAD_POOL);
         QUAD.clear();
      }

      if (!TRI.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

         for (Render3DUtil.Triangle tri : TRI) {
            vertexTriangle(tri.entry, buffer, tri.a, tri.b, tri.c, tri.color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         returnAll(TRI, TRI_POOL);
         TRI.clear();
      }

      if (!LINE_DEPTH.isEmpty()) {
         GL11.glEnable(2848);
         groupLinesByWidth(LINE_DEPTH, LINE_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

         for (Entry<Float, List<Render3DUtil.Line>> batchxxx : LINE_DEPTH_BATCHES.entrySet()) {
            if (!batchxxx.getValue().isEmpty()) {
               RenderSystem.lineWidth(batchxxx.getKey());
               BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);

               for (Render3DUtil.Line line : batchxxx.getValue()) {
                  vertexLine(line.entry, buffer, line.start, line.end, line.colorStart, line.colorEnd);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         GL11.glDisable(2848);
         LINE_DEPTH_BATCHES.values().forEach(List::clear);
         returnAll(LINE_DEPTH, LINE_POOL);
         LINE_DEPTH.clear();
      }

      if (!QUAD_DEPTH.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD_DEPTH.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         returnAll(QUAD_DEPTH, QUAD_POOL);
         QUAD_DEPTH.clear();
      }

      if (!TRI_DEPTH.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

         for (Render3DUtil.Triangle tri : TRI_DEPTH) {
            vertexTriangle(tri.entry, buffer, tri.a, tri.b, tri.c, tri.color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         returnAll(TRI_DEPTH, TRI_POOL);
         TRI_DEPTH.clear();
      }

      if (!GHOST_TEXTURE.isEmpty()) {
         groupGhostTextures(GHOST_TEXTURE, GHOST_BATCHES);
         RenderSystem.enableBlend();

         for (Entry<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> texBatch : GHOST_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, texBatch.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            for (Entry<Runnable, List<Render3DUtil.GhostTexture>> blendBatch : texBatch.getValue().entrySet()) {
               if (!blendBatch.getValue().isEmpty()) {
                  blendBatch.getKey().run();
                  BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                  for (Render3DUtil.GhostTexture tex : blendBatch.getValue()) {
                     quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
                  }

                  BufferRenderer.drawWithGlobalProgram(buffer.end());
               }
            }
         }

         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         clearGhostBatches(GHOST_BATCHES);
         returnAll(GHOST_TEXTURE, GHOST_POOL);
         GHOST_TEXTURE.clear();
      }

      if (!GHOST_TEXTURE_DEPTH.isEmpty()) {
         groupGhostTextures(GHOST_TEXTURE_DEPTH, GHOST_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);

         for (Entry<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> texBatch : GHOST_DEPTH_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, texBatch.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            for (Entry<Runnable, List<Render3DUtil.GhostTexture>> blendBatchx : texBatch.getValue().entrySet()) {
               if (!blendBatchx.getValue().isEmpty()) {
                  blendBatchx.getKey().run();
                  BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                  for (Render3DUtil.GhostTexture tex : blendBatchx.getValue()) {
                     quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
                  }

                  BufferRenderer.drawWithGlobalProgram(buffer.end());
               }
            }
         }

         RenderSystem.disableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         clearGhostBatches(GHOST_DEPTH_BATCHES);
         returnAll(GHOST_TEXTURE_DEPTH, GHOST_POOL);
         GHOST_TEXTURE_DEPTH.clear();
      }
   }

   private static <T> void returnAll(List<T> list, ArrayDeque<T> pool) {
      if (pool.size() < 1024) {
         for (T obj : list) {
            if (pool.size() >= 1024) {
               break;
            }

            pool.offerLast(obj);
         }
      }
   }

   private static void clearGhostBatches(Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> batches) {
      for (Map<Runnable, List<Render3DUtil.GhostTexture>> inner : batches.values()) {
         inner.values().forEach(List::clear);
      }
   }

   public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      Vec3d vec3d = Vec3d.of(blockPos);
      if (ProjectionUtil.canSee(new Box(blockPos))) {
         if (SHAPE_OUTLINES.containsKey(voxelShape)) {
            Pair<List<Box>, List<Render3DUtil.Line>> pair = SHAPE_OUTLINES.get(voxelShape);
            if (fill) {
               for (Box box : pair.getLeft()) {
                  drawBox(box.offset(vec3d), color, width, false, true, depth);
               }
            }

            for (Render3DUtil.Line line : pair.getRight()) {
               drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth);
            }

            return;
         }

         List<Render3DUtil.Line> lines = new ArrayList<>();
         voxelShape.forEachEdge(
            (minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Render3DUtil.Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0F))
         );
         SHAPE_OUTLINES.put(voxelShape, new Pair(voxelShape.getBoundingBoxes(), lines));
      }
   }

   public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      drawBox(null, box, color, width, line, fill, depth);
   }

   public static void drawBox(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth
   ) {
      box = box.expand(0.001);
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      if (fill) {
         int fillColor = ColorUtil.multAlpha(color, 0.1F);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
      }

      if (line) {
         drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
         drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
         drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
      }
   }

   public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
      vertexLine(matrices != null ? matrices.peek() : null, buffer, start, end, startColor, endColor);
   }

   public static void vertexLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      boolean useWorldRebase = usesLegacyWorldSpace(entry);
      Matrix4f positionMatrix = useWorldRebase ? lastWorldRotationMatrix : entry.getPositionMatrix();
      if (useWorldRebase) {
         toCameraRelative(start, SCRATCH_START);
         toCameraRelative(end, SCRATCH_END);
      } else {
         SCRATCH_START.set((float)start.x, (float)start.y, (float)start.z);
         SCRATCH_END.set((float)end.x, (float)end.y, (float)end.z);
      }

      computeNormal(SCRATCH_START, SCRATCH_END, SCRATCH_NORMAL);
      buffer.vertex(positionMatrix, SCRATCH_START.x, SCRATCH_START.y, SCRATCH_START.z).color(startColor).normal(entry, SCRATCH_NORMAL);
      buffer.vertex(positionMatrix, SCRATCH_END.x, SCRATCH_END.y, SCRATCH_END.z).color(endColor).normal(entry, SCRATCH_NORMAL);
   }

   public static void vertexLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      computeNormal(start, end, SCRATCH_NORMAL);
      Matrix4f positionMatrix = entry.getPositionMatrix();
      buffer.vertex(positionMatrix, start.x, start.y, start.z).color(startColor).normal(entry, SCRATCH_NORMAL);
      buffer.vertex(positionMatrix, end.x, end.y, end.z).color(endColor).normal(entry, SCRATCH_NORMAL);
   }

   public static void vertexQuad(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      boolean useWorldRebase = usesLegacyWorldSpace(entry);
      Matrix4f positionMatrix = useWorldRebase ? lastWorldRotationMatrix : entry.getPositionMatrix();
      if (useWorldRebase) {
         toCameraRelative(vec1, SCRATCH_V1);
         toCameraRelative(vec2, SCRATCH_V2);
         toCameraRelative(vec3, SCRATCH_V3);
         toCameraRelative(vec4, SCRATCH_V4);
      } else {
         SCRATCH_V1.set((float)vec1.x, (float)vec1.y, (float)vec1.z);
         SCRATCH_V2.set((float)vec2.x, (float)vec2.y, (float)vec2.z);
         SCRATCH_V3.set((float)vec3.x, (float)vec3.y, (float)vec3.z);
         SCRATCH_V4.set((float)vec4.x, (float)vec4.y, (float)vec4.z);
      }

      vertexQuad(positionMatrix, buffer, SCRATCH_V1, SCRATCH_V2, SCRATCH_V3, SCRATCH_V4, color);
   }

   public static void vertexQuad(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      vertexQuad(entry.getPositionMatrix(), buffer, vec1, vec2, vec3, vec4, color);
   }

   public static void vertexTriangle(net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vec3d a, Vec3d b, Vec3d c, int color) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      boolean useWorldRebase = usesLegacyWorldSpace(entry);
      Matrix4f positionMatrix = useWorldRebase ? lastWorldRotationMatrix : entry.getPositionMatrix();
      Vector3f va = new Vector3f();
      Vector3f vb = new Vector3f();
      Vector3f vc = new Vector3f();
      if (useWorldRebase) {
         toCameraRelative(a, va);
         toCameraRelative(b, vb);
         toCameraRelative(c, vc);
      } else {
         va.set((float)a.x, (float)a.y, (float)a.z);
         vb.set((float)b.x, (float)b.y, (float)b.z);
         vc.set((float)c.x, (float)c.y, (float)c.z);
      }

      buffer.vertex(positionMatrix, va.x, va.y, va.z).color(color);
      buffer.vertex(positionMatrix, vb.x, vb.y, vb.z).color(color);
      buffer.vertex(positionMatrix, vc.x, vc.y, vc.z).color(color);
   }

   public static void quadTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color
   ) {
      buffer.vertex(entry, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color.x);
      buffer.vertex(entry, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color.y);
      buffer.vertex(entry, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color.w);
      buffer.vertex(entry, x, y, 0.0F).texture(1.0F, 0.0F).color(color.z);
   }

   @NotNull
   public static Vector3f getNormal(Vector3f start, Vector3f end) {
      Vector3f normal = new Vector3f(start).sub(end);
      float lengthSq = normal.lengthSquared();
      return lengthSq <= 1.0E-8F ? new Vector3f(0.0F, 1.0F, 0.0F) : normal.div(MathHelper.sqrt(lengthSq));
   }

   public static void drawLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      int color,
      float width,
      boolean depth
   ) {
      drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
   }

   public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
      drawLine(null, start, end, color, color, width, depth);
   }

   public static void drawLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth
   ) {
      Render3DUtil.Line line = LINE_POOL.pollFirst();
      if (line == null) {
         line = new Render3DUtil.Line();
      }

      line.set(entry, start, end, colorStart, colorEnd, width);
      if (depth) {
         LINE_DEPTH.add(line);
      } else {
         LINE.add(line);
      }
   }

   public static void drawQuad(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      Render3DUtil.Quad quad = QUAD_POOL.pollFirst();
      if (quad == null) {
         quad = new Render3DUtil.Quad();
      }

      quad.set(entry, x, y, w, z, color);
      if (depth) {
         QUAD_DEPTH.add(quad);
      } else {
         QUAD.add(quad);
      }
   }

   public static void drawTriangle(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d a, Vec3d b, Vec3d c, int color, boolean depth) {
      Render3DUtil.Triangle tri = TRI_POOL.pollFirst();
      if (tri == null) {
         tri = new Render3DUtil.Triangle();
      }

      tri.set(entry, a, b, c, color);
      if (depth) {
         TRI_DEPTH.add(tri);
      } else {
         TRI.add(tri);
      }
   }

   public static void drawDebugSphere(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      Vec3d center,
      double radius,
      int color,
      int segments,
      boolean depth,
      boolean fill,
      boolean outline,
      float lineWidth
   ) {
      double cx = center.x;
      double cy = center.y;
      double cz = center.z;

      for (int lat = 0; lat < segments; lat++) {
         double theta1 = Math.PI * lat / segments;
         double theta2 = Math.PI * (lat + 1) / segments;
         double sinT1 = Math.sin(theta1);
         double cosT1 = Math.cos(theta1);
         double sinT2 = Math.sin(theta2);
         double cosT2 = Math.cos(theta2);

         for (int lon = 0; lon < segments; lon++) {
            double phi1 = (Math.PI * 2) * lon / segments;
            double phi2 = (Math.PI * 2) * (lon + 1) / segments;
            double sinP1 = Math.sin(phi1);
            double cosP1 = Math.cos(phi1);
            double sinP2 = Math.sin(phi2);
            double cosP2 = Math.cos(phi2);
            Vec3d v00 = new Vec3d(cx + radius * sinT1 * cosP1, cy + radius * cosT1, cz + radius * sinT1 * sinP1);
            Vec3d v10 = new Vec3d(cx + radius * sinT2 * cosP1, cy + radius * cosT2, cz + radius * sinT2 * sinP1);
            Vec3d v11 = new Vec3d(cx + radius * sinT2 * cosP2, cy + radius * cosT2, cz + radius * sinT2 * sinP2);
            Vec3d v01 = new Vec3d(cx + radius * sinT1 * cosP2, cy + radius * cosT1, cz + radius * sinT1 * sinP2);
            if (fill) {
               if (lat == 0) {
                  drawTriangle(entry, v00, v10, v11, color, depth);
               } else if (lat == segments - 1) {
                  drawTriangle(entry, v00, v10, v01, color, depth);
               } else {
                  drawTriangle(entry, v00, v10, v11, color, depth);
                  drawTriangle(entry, v00, v11, v01, color, depth);
               }
            }

            if (outline) {
               drawLine(entry, v10, v11, color, color, lineWidth, depth);
               drawLine(entry, v00, v10, color, color, lineWidth, depth);
            }
         }
      }
   }

   public static void drawTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth
   ) {
      Render3DUtil.Texture texture = TEXTURE_POOL.pollFirst();
      if (texture == null) {
         texture = new Render3DUtil.Texture();
      }

      texture.set(entry, id, x, y, width, height, color);
      if (depth) {
         TEXTURE_DEPTH.add(texture);
      } else {
         TEXTURE.add(texture);
      }
   }

   public static void drawGhostTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      Identifier id,
      float x,
      float y,
      float width,
      float height,
      Vector4i color,
      boolean depth,
      Runnable setupBlendFunc
   ) {
      Render3DUtil.GhostTexture texture = GHOST_POOL.pollFirst();
      if (texture == null) {
         texture = new Render3DUtil.GhostTexture();
      }

      texture.set(entry, id, x, y, width, height, color, setupBlendFunc);
      if (depth) {
         GHOST_TEXTURE_DEPTH.add(texture);
      } else {
         GHOST_TEXTURE.add(texture);
      }
   }

   private static void groupTexturesById(List<Render3DUtil.Texture> textures, Map<Identifier, List<Render3DUtil.Texture>> batches) {
      batches.values().forEach(List::clear);

      for (Render3DUtil.Texture texture : textures) {
         batches.computeIfAbsent(texture.id, k -> new ArrayList<>()).add(texture);
      }
   }

   private static void groupLinesByWidth(List<Render3DUtil.Line> lines, Map<Float, List<Render3DUtil.Line>> batches) {
      batches.values().forEach(List::clear);

      for (Render3DUtil.Line line : lines) {
         batches.computeIfAbsent(line.width, k -> new ArrayList<>()).add(line);
      }
   }

   private static void groupGhostTextures(List<Render3DUtil.GhostTexture> textures, Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> batches) {
      clearGhostBatches(batches);

      for (Render3DUtil.GhostTexture texture : textures) {
         Map<Runnable, List<Render3DUtil.GhostTexture>> byBlend = batches.computeIfAbsent(texture.id, k -> new LinkedHashMap<>());
         byBlend.computeIfAbsent(texture.setupBlendFunc, k -> new ArrayList<>()).add(texture);
      }
   }

   private static void prepareWorldPrecisionState() {
      if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
         lastCameraPos = mc.gameRenderer.getCamera().getPos();
      } else {
         lastCameraPos = Vec3d.ZERO;
      }

      if (lastWorldSpaceMatrix != null) {
         lastWorldRotationMatrix.set(lastWorldSpaceMatrix.getPositionMatrix());
         lastWorldRotationMatrix.setTranslation(0.0F, 0.0F, 0.0F);
      } else {
         lastWorldRotationMatrix.identity();
      }
   }

   private static boolean usesLegacyWorldSpace(net.minecraft.client.util.math.MatrixStack.Entry entry) {
      if (entry == null || lastWorldSpaceMatrix == null) {
         return false;
      } else {
         return entry == lastWorldSpaceMatrix
            ? true
            : entry.getPositionMatrix().equals(lastWorldSpaceMatrix.getPositionMatrix())
               && entry.getNormalMatrix().equals(lastWorldSpaceMatrix.getNormalMatrix());
      }
   }

   private static void toCameraRelative(Vec3d worldPos, Vector3f out) {
      out.set((float)(worldPos.x - lastCameraPos.x), (float)(worldPos.y - lastCameraPos.y), (float)(worldPos.z - lastCameraPos.z));
   }

   private static void computeNormal(Vector3f start, Vector3f end, Vector3f out) {
      out.set(start).sub(end);
      float lenSq = out.lengthSquared();
      if (lenSq <= 1.0E-8F) {
         out.set(0.0F, 1.0F, 0.0F);
      } else {
         out.div(MathHelper.sqrt(lenSq));
      }
   }

   private static void vertexQuad(Matrix4f matrix, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
      buffer.vertex(matrix, vec1.x, vec1.y, vec1.z).color(color);
      buffer.vertex(matrix, vec2.x, vec2.y, vec2.z).color(color);
      buffer.vertex(matrix, vec3.x, vec3.y, vec3.z).color(color);
      buffer.vertex(matrix, vec4.x, vec4.y, vec4.z).color(color);
   }

   private Render3DUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void setLastProjMat(Matrix4f lastProjMat) {
      Render3DUtil.lastProjMat = lastProjMat;
   }

   public static void setLastWorldSpaceMatrix(net.minecraft.client.util.math.MatrixStack.Entry lastWorldSpaceMatrix) {
      Render3DUtil.lastWorldSpaceMatrix = lastWorldSpaceMatrix;
   }

   public static final class GhostTexture {
      public net.minecraft.client.util.math.MatrixStack.Entry entry;
      public Identifier id;
      public float x;
      public float y;
      public float width;
      public float height;
      public Vector4i color;
      public Runnable setupBlendFunc;

      GhostTexture() {
      }

      void set(
         net.minecraft.client.util.math.MatrixStack.Entry entry,
         Identifier id,
         float x,
         float y,
         float width,
         float height,
         Vector4i color,
         Runnable setupBlendFunc
      ) {
         this.entry = entry;
         this.id = id;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.color = color;
         this.setupBlendFunc = setupBlendFunc;
      }
   }

   public static final class Line {
      public net.minecraft.client.util.math.MatrixStack.Entry entry;
      public Vec3d start;
      public Vec3d end;
      public int colorStart;
      public int colorEnd;
      public float width;

      Line() {
      }

      public Line(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
         this.set(entry, start, end, colorStart, colorEnd, width);
      }

      void set(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
         this.entry = entry;
         this.start = start;
         this.end = end;
         this.colorStart = colorStart;
         this.colorEnd = colorEnd;
         this.width = width;
      }
   }

   public static final class Quad {
      public net.minecraft.client.util.math.MatrixStack.Entry entry;
      public Vec3d x;
      public Vec3d y;
      public Vec3d w;
      public Vec3d z;
      public int color;

      Quad() {
      }

      void set(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
         this.entry = entry;
         this.x = x;
         this.y = y;
         this.w = w;
         this.z = z;
         this.color = color;
      }
   }

   public static final class Texture {
      public net.minecraft.client.util.math.MatrixStack.Entry entry;
      public Identifier id;
      public float x;
      public float y;
      public float width;
      public float height;
      public Vector4i color;

      Texture() {
      }

      void set(net.minecraft.client.util.math.MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
         this.entry = entry;
         this.id = id;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.color = color;
      }
   }

   public static final class Triangle {
      public net.minecraft.client.util.math.MatrixStack.Entry entry;
      public Vec3d a;
      public Vec3d b;
      public Vec3d c;
      public int color;

      Triangle() {
      }

      void set(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d a, Vec3d b, Vec3d c, int color) {
         this.entry = entry;
         this.a = a;
         this.b = b;
         this.c = c;
         this.color = color;
      }
   }
}
