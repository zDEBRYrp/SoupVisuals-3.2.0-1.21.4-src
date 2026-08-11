package padej.soup.base.util.render.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import padej.soup.api.accessor.IWorldRenderer;
import padej.soup.base.QuickImports;
import padej.soup.base.util.render.GL;
import padej.soup.implement.features.modules.visuals.BetterGlow;

public class EntityOutline implements QuickImports {
   private static EntityOutline instance;
   public static boolean rendering;
   public final OutlineVertexConsumerProvider vertexConsumerProvider;
   public final Framebuffer framebuffer;
   private final int shaderId;
   private final int vao;
   private final Object2IntMap<String> uniformLocations = new Object2IntOpenHashMap();
   private final int indicesCount;

   private EntityOutline() {
      this.vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
      this.framebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
      int vert = GL.createShader(35633);
      GL.shaderSource(vert, this.readShader("post-process/base.vert"));
      if (GL.compileShader(vert) != null) {
         throw new RuntimeException("Vertex shader compilation failed");
      } else {
         int frag = GL.createShader(35632);
         GL.shaderSource(frag, this.readShader("post-process/outline.frag"));
         if (GL.compileShader(frag) != null) {
            throw new RuntimeException("Fragment shader compilation failed");
         } else {
            this.shaderId = GL.createProgram();
            if (GL.linkProgram(this.shaderId, vert, frag) != null) {
               throw new RuntimeException("Shader linking failed");
            } else {
               GL.deleteShader(vert);
               GL.deleteShader(frag);
               ByteBuffer vertices = BufferUtils.createByteBuffer(64);
               long vp = MemoryUtil.memAddress0(vertices);
               MemoryUtil.memPutFloat(vp, -1.0F);
               MemoryUtil.memPutFloat(vp + 4L, -1.0F);
               MemoryUtil.memPutFloat(vp + 8L, -1.0F);
               MemoryUtil.memPutFloat(vp + 12L, 1.0F);
               MemoryUtil.memPutFloat(vp + 16L, 1.0F);
               MemoryUtil.memPutFloat(vp + 20L, 1.0F);
               MemoryUtil.memPutFloat(vp + 24L, 1.0F);
               MemoryUtil.memPutFloat(vp + 28L, -1.0F);
               ByteBuffer indices = BufferUtils.createByteBuffer(24);
               long ip = MemoryUtil.memAddress0(indices);
               MemoryUtil.memPutInt(ip, 0);
               MemoryUtil.memPutInt(ip + 4L, 1);
               MemoryUtil.memPutInt(ip + 8L, 2);
               MemoryUtil.memPutInt(ip + 12L, 2);
               MemoryUtil.memPutInt(ip + 16L, 3);
               MemoryUtil.memPutInt(ip + 20L, 0);
               this.indicesCount = 6;
               this.vao = GL.genVertexArray();
               GL.bindVertexArray(this.vao);
               int vbo = GL.genBuffer();
               GL.bindVertexBuffer(vbo);
               GL.bufferData(34962, vertices.limit(32), 35044);
               int ibo = GL.genBuffer();
               GL.bindIndexBuffer(ibo);
               GL.bufferData(34963, indices.limit(24), 35044);
               GL.enableVertexAttribute(0);
               GL.vertexAttribute(0, 2, 5126, false, 8, 0L);
               GL.bindVertexArray(0);
               GL.bindVertexBuffer(0);
               GL.bindIndexBuffer(0);
            }
         }
      }
   }

   private String readShader(String path) {
      try {
         return IOUtils.toString(
            ((Resource)mc.getResourceManager()
                  .getResource(Identifier.of("soupapi", "shaders/" + path))
                  .orElseThrow(() -> new RuntimeException("Shader not found: " + path)))
               .getInputStream(),
            StandardCharsets.UTF_8
         );
      } catch (IOException var3) {
         throw new RuntimeException("Failed to read shader: " + path, var3);
      }
   }

   public static EntityOutline get() {
      if (instance == null) {
         instance = new EntityOutline();
      }

      return instance;
   }

   public static boolean isInitialized() {
      return instance != null;
   }

   public void beginRender() {
      this.framebuffer.clear();
      mc.getFramebuffer().beginWrite(false);
   }

   public void endRender() {
      ((IWorldRenderer)mc.worldRenderer).soup$pushFramebuffer(this.framebuffer);
      this.vertexConsumerProvider.draw();
      ((IWorldRenderer)mc.worldRenderer).soup$popFramebuffer();
      mc.getFramebuffer().beginWrite(false);
      GL.bindTexture(this.framebuffer.getColorAttachment(), 0);
      GL.useProgram(this.shaderId);
      BetterGlow betterGlow = BetterGlow.getInstance();
      this.setUniform(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
      this.setUniform("u_Texture", 0);
      this.setUniform("u_Time", (float)GLFW.glfwGetTime());
      this.setUniform("u_Width", betterGlow != null ? (int)betterGlow.getOutlineWidth().getValue() : 2);
      this.setUniform("u_FillOpacity", 0.3F);
      this.setUniform("u_ShapeMode", betterGlow != null ? betterGlow.getShapeModeValue() : 2);
      this.setUniform("u_GlowMultiplier", betterGlow != null ? betterGlow.getGlowMultiplier().getValue() : 3.5F);
      this.setUniform(RenderSystem.getProjectionMatrix());
      this.setUniform(RenderSystem.getModelViewStack());
      GL.saveState();
      GL.disableDepth();
      GL.enableBlend();
      GL.disableCull();
      Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
      matrixStack.pushMatrix();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      matrixStack.translate(0.0F, (float)(-cameraPos.y), 0.0F);
      GL.bindVertexArray(this.vao);
      GL.drawElements(4, this.indicesCount, 5125);
      GL.bindVertexArray(0);
      matrixStack.popMatrix();
      GL.restoreState();
   }

   public void onResized(int width, int height) {
      this.framebuffer.resize(width, height);
   }

   private int getUniformLocation(String name) {
      if (this.uniformLocations.containsKey(name)) {
         return this.uniformLocations.getInt(name);
      } else {
         int location = GL.getUniformLocation(this.shaderId, name);
         this.uniformLocations.put(name, location);
         return location;
      }
   }

   private void setUniform(String name, int v) {
      GL.uniformInt(this.getUniformLocation(name), v);
   }

   private void setUniform(String name, float v) {
      GL.uniformFloat(this.getUniformLocation(name), v);
   }

   private void setUniform(int v1, int v2) {
      GL.uniformFloat2(this.getUniformLocation("u_Size"), v1, v2);
   }

   private void setUniform(Matrix4f mat) {
      GL.uniformMatrix(this.getUniformLocation("u_Proj"), mat);
   }

   private void setUniform(Matrix4fStack stack) {
      GL.uniformMatrix(this.getUniformLocation("u_ModelView"), stack);
   }
}
