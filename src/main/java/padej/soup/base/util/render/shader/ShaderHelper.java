package padej.soup.base.util.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;

public class ShaderHelper implements QuickImports {
   private static Shader chromaShader;
   private static Shader solidShader;
   private static Shader balatroShader;
   private static Shader smokeShader;
   private static Shader stripesShader;
   private static Shader glowShader;
   private static Shader glassShader;
   private static Shader snowShader;
   private static Shader invertShader;
   private static Shader blendShader;
   private static SimpleFramebuffer copyFbo;
   private static SimpleFramebuffer shader1Fbo;
   private static SimpleFramebuffer shader2Fbo;
   private static boolean initialized = false;

   public static void initShadersIfNeeded() {
      if (!initialized) {
         try {
            chromaShader = new Shader("hand", "chroma");
            solidShader = new Shader("hand", "solid");
            balatroShader = new Shader("hand", "balatro");
            smokeShader = new Shader("hand", "smoke");
            stripesShader = new Shader("hand", "stripes");
            glowShader = new Shader("hand", "glow");
            glassShader = new Shader("hand", "glass");
            snowShader = new Shader("hand", "snow");
            invertShader = new Shader("hand", "invert");
            blendShader = new Shader("hand", "blend");
            initialized = true;
         } catch (Exception var1) {
            LoggerUtil.error("Failed to initialize hand shaders: " + var1.getMessage());
         }
      }
   }

   public static void checkFramebuffers() {
      if (mc != null && mc.getWindow() != null) {
         int width = mc.getWindow().getFramebufferWidth();
         int height = mc.getWindow().getFramebufferHeight();
         if (copyFbo == null || copyFbo.textureWidth != width || copyFbo.textureHeight != height) {
            if (copyFbo != null) {
               copyFbo.delete();
               if (shader1Fbo != null) {
                  shader1Fbo.delete();
               }

               if (shader2Fbo != null) {
                  shader2Fbo.delete();
               }
            }

            copyFbo = new SimpleFramebuffer(width, height, true);
            shader1Fbo = new SimpleFramebuffer(width, height, true);
            shader2Fbo = new SimpleFramebuffer(width, height, true);
         }
      }
   }

   public static void drawFullScreenQuad() {
      RenderSystem.assertOnRenderThread();
      BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
      BufferRenderer.draw(bufferBuilder.end());
   }

   public static Shader getChromaShader() {
      return chromaShader;
   }

   public static Shader getSolidShader() {
      return solidShader;
   }

   public static Shader getBalatroShader() {
      return balatroShader;
   }

   public static Shader getSmokeShader() {
      return smokeShader;
   }

   public static Shader getStripesShader() {
      return stripesShader;
   }

   public static Shader getGlowShader() {
      return glowShader;
   }

   public static Shader getGlassShader() {
      return glassShader;
   }

   public static Shader getSnowShader() {
      return snowShader;
   }

   public static Shader getInvertShader() {
      return invertShader;
   }

   public static Shader getBlendShader() {
      return blendShader;
   }

   public static SimpleFramebuffer getCopyFbo() {
      return copyFbo;
   }

   public static SimpleFramebuffer getShader1Fbo() {
      return shader1Fbo;
   }

   public static SimpleFramebuffer getShader2Fbo() {
      return shader2Fbo;
   }

   public static boolean isInitialized() {
      return initialized;
   }
}
