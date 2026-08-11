package padej.soup.api.system.blur;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.render.shader.Shader;
import padej.soup.base.util.render.shader.ShaderHelper;

public class GlobalBlurProcessor implements QuickImports {
   public static final GlobalBlurProcessor INSTANCE = new GlobalBlurProcessor();
   private Shader blurShader;
   private SimpleFramebuffer blurBuffer;
   private boolean initialized = false;
   private boolean initFailed = false;

   private void init() {
      try {
         this.blurShader = new Shader("blur", "prepass");
         this.initialized = true;
      } catch (Exception var2) {
         this.initFailed = true;
         LoggerUtil.error("GlobalBlurProcessor: failed to init shader: " + var2.getMessage());
      }
   }

   public void prepare() {
      if (!this.initFailed) {
         if (!this.initialized) {
            this.init();
         }

         if (this.initialized) {
            Framebuffer mcBuffer = mc.getFramebuffer();
            int fw = mcBuffer.textureWidth;
            int fh = mcBuffer.textureHeight;
            if (this.blurBuffer == null || this.blurBuffer.textureWidth != fw || this.blurBuffer.textureHeight != fh) {
               if (this.blurBuffer != null) {
                  this.blurBuffer.delete();
               }

               this.blurBuffer = new SimpleFramebuffer(fw, fh, false);
            }

            this.blurBuffer.beginWrite(false);
            GlStateManager._viewport(0, 0, fw, fh);
            GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager._clear(16384);
            GlStateManager._activeTexture(33984);
            RenderSystem.bindTexture(mcBuffer.getColorAttachment());
            this.blurShader.bind();
            this.blurShader.setUniform2f("Resolution", fw, fh);
            this.blurShader.setUniform1f("Radius", 6.0F);
            this.blurShader.setUniform1i("Sampler0", 0);
            ShaderHelper.drawFullScreenQuad();
            this.blurShader.unbind();
            mcBuffer.beginWrite(true);
         }
      }
   }

   public int getBlurredTexture() {
      return this.blurBuffer != null ? this.blurBuffer.getColorAttachment() : mc.getFramebuffer().getColorAttachment();
   }

   public boolean isReady() {
      return this.blurBuffer != null;
   }
}
