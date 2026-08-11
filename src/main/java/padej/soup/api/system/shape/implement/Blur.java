package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import padej.soup.api.system.shape.Shape;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.implement.features.modules.client.Theme;

public class Blur implements Shape, QuickImports {
   private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur"), VertexFormats.POSITION, Defines.EMPTY);
   private final ShaderProgramKey DOWN_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur_down"), VertexFormats.POSITION, Defines.EMPTY);
   private final ShaderProgramKey UP_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur_up"), VertexFormats.POSITION, Defines.EMPTY);
   private Framebuffer blurResult;
   private Framebuffer fboA;
   private Framebuffer fboB;
   private int fullWidth;
   private int fullHeight;
   private int regionX;
   private int regionY;
   private int regionW;
   private int regionH;
   private boolean hasRegion = false;
   private int regionGlX;
   private int regionGlY;
   private static final int BASE_BLUR_PADDING = 16;
   private static final Vector3f SCRATCH_POS = new Vector3f();
   private static final Vector3f SCRATCH_SIZE = new Vector3f();
   private boolean anyBlurLastFrame = false;
   private boolean anyBlurThisFrame = false;
   private static final long SETUP_INTERVAL_MS = 16L;
   private long lastSetupMs = 0L;
   private int lastSetupRegionX = -1;
   private int lastSetupRegionY = -1;
   private int lastSetupRegionW = -1;
   private int lastSetupRegionH = -1;
   private static final int REGION_CHANGE_THRESHOLD_PX = 2;

   public Framebuffer getBlurResult() {
      return this.blurResult;
   }

   public int getRegionGlX() {
      return this.regionGlX;
   }

   public int getRegionGlY() {
      return this.regionGlY;
   }

   public void resetRegion() {
      this.hasRegion = false;
   }

   public void expandRegion(float guiX, float guiY, float guiW, float guiH) {
      float scale = (float)mc.getWindow().getScaleFactor();
      int x = (int)(guiX * scale);
      int y = (int)(guiY * scale);
      int w = (int)Math.ceil(guiW * scale);
      int h = (int)Math.ceil(guiH * scale);
      if (!this.hasRegion) {
         this.regionX = x;
         this.regionY = y;
         this.regionW = w;
         this.regionH = h;
         this.hasRegion = true;
      } else {
         int maxX = Math.max(this.regionX + this.regionW, x + w);
         int maxY = Math.max(this.regionY + this.regionH, y + h);
         this.regionX = Math.min(this.regionX, x);
         this.regionY = Math.min(this.regionY, y);
         this.regionW = maxX - this.regionX;
         this.regionH = maxY - this.regionY;
      }
   }

   @Override
   public void render(ShapeProperties shape) {
      this.anyBlurThisFrame = true;
      if (this.blurResult != null) {
         BlurBatch batch = BlurBatch.active();
         if (batch != null) {
            batch.submit(shape);
         } else {
            HudProfiler.begin(HudProfiler.Section.BLUR_RENDER_IMMEDIATE);
            HudProfiler.countBlurImmediate();

            try {
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.enableDepthTest();
               RenderSystem.enableCull();
               float scale = (float)mc.getWindow().getScaleFactor();
               float alpha = RenderSystem.getShaderColor()[3];
               Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
               Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, SCRATCH_POS).mul(scale);
               Vector3f size = matrix4f.getScale(SCRATCH_SIZE).mul(scale);
               Vector4f round = shape.getRound().mul(size.y);
               float softness = shape.getSoftness();
               float thickness = shape.getThickness();
               float width = shape.getWidth() * size.x;
               float height = shape.getHeight() * size.y;
               float quadPad = Math.max(1.0F, softness + 1.0F);
               BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
               drawEngine.quad(
                  matrix4f, buffer, shape.getX() - quadPad / 2.0F, shape.getY() - quadPad / 2.0F, shape.getWidth() + quadPad, shape.getHeight() + quadPad
               );
               GlStateManager._activeTexture(33984);
               RenderSystem.bindTexture(this.blurResult.getColorAttachment());
               GlStateManager._texParameter(3553, 10241, 9729);
               GlStateManager._texParameter(3553, 10240, 9729);
               ShaderProgram shader = RenderSystem.setShader(this.SHADER_KEY);
               if (shader != null) {
                  shader.getUniformOrDefault("size").set(width, height);
                  shader.getUniformOrDefault("location").set(pos.x, window.getHeight() - height - pos.y);
                  shader.getUniformOrDefault("radius").set(round);
                  shader.getUniformOrDefault("softness").set(softness);
                  shader.getUniformOrDefault("thickness").set(thickness);
                  shader.getUniformOrDefault("color1")
                     .set(
                        ColorUtil.redf(shape.getColor().x),
                        ColorUtil.greenf(shape.getColor().x),
                        ColorUtil.bluef(shape.getColor().x),
                        ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().x, alpha))
                     );
                  shader.getUniformOrDefault("color2")
                     .set(
                        ColorUtil.redf(shape.getColor().y),
                        ColorUtil.greenf(shape.getColor().y),
                        ColorUtil.bluef(shape.getColor().y),
                        ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().y, alpha))
                     );
                  shader.getUniformOrDefault("color3")
                     .set(
                        ColorUtil.redf(shape.getColor().z),
                        ColorUtil.greenf(shape.getColor().z),
                        ColorUtil.bluef(shape.getColor().z),
                        ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().z, alpha))
                     );
                  shader.getUniformOrDefault("color4")
                     .set(
                        ColorUtil.redf(shape.getColor().w),
                        ColorUtil.greenf(shape.getColor().w),
                        ColorUtil.bluef(shape.getColor().w),
                        ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().w, alpha))
                     );
                  shader.getUniformOrDefault("outlineColor")
                     .set(
                        ColorUtil.redf(shape.getOutlineColor()),
                        ColorUtil.greenf(shape.getOutlineColor()),
                        ColorUtil.bluef(shape.getOutlineColor()),
                        ColorUtil.alphaf(ColorUtil.multAlpha(shape.getOutlineColor(), alpha))
                     );
                  shader.getUniformOrDefault("BlurRegionOffset").set((float) this.regionGlX, (float) this.regionGlY);
                  shader.getUniformOrDefault("BlurRegionSize").set((float) this.blurResult.textureWidth, (float) this.blurResult.textureHeight);
                  BufferRenderer.drawWithGlobalProgram(buffer.end());
                  GlStateManager._activeTexture(33984);
                  GlStateManager._bindTexture(0);
                  RenderSystem.disableBlend();
                  return;
               }
            } finally {
               HudProfiler.end(HudProfiler.Section.BLUR_RENDER_IMMEDIATE);
            }
         }
      }
   }

   public void setup() {
      boolean usedLastFrame = this.anyBlurLastFrame;
      this.anyBlurLastFrame = this.anyBlurThisFrame;
      this.anyBlurThisFrame = false;
      if (this.hasRegion) {
         if (usedLastFrame) {
            long now = System.currentTimeMillis();
            boolean regionStable = this.blurResult != null
               && Math.abs(this.regionX - this.lastSetupRegionX) < 2
               && Math.abs(this.regionY - this.lastSetupRegionY) < 2
               && Math.abs(this.regionW - this.lastSetupRegionW) < 2
               && Math.abs(this.regionH - this.lastSetupRegionH) < 2;
            if (!regionStable || now - this.lastSetupMs >= 16L) {
               this.lastSetupMs = now;
               this.lastSetupRegionX = this.regionX;
               this.lastSetupRegionY = this.regionY;
               this.lastSetupRegionW = this.regionW;
               this.lastSetupRegionH = this.regionH;
               float strength = Theme.getInstance().blurStrength.getValue();
               Framebuffer mainFbo = mc.getFramebuffer();
               this.fullWidth = mainFbo.textureWidth;
               this.fullHeight = mainFbo.textureHeight;
               int padding = (int)Math.ceil(16.0F * Math.max(1.0F, strength));
               int px = Math.max(0, this.regionX - padding);
               int py = Math.max(0, this.regionY - padding);
               int pw = Math.min(this.fullWidth - px, this.regionW + padding * 2);
               int ph = Math.min(this.fullHeight - py, this.regionH + padding * 2);
               if (pw > 0 && ph > 0) {
                  this.regionGlX = px;
                  this.regionGlY = this.fullHeight - py - ph;
                  float uvOffX = (float)px / this.fullWidth;
                  float uvOffY = (float)this.regionGlY / this.fullHeight;
                  float uvSclX = (float)pw / this.fullWidth;
                  float uvSclY = (float)ph / this.fullHeight;
                  int halfW = Math.max(1, pw / 2);
                  int halfH = Math.max(1, ph / 2);
                  this.blurResult = this.ensureFbo(this.blurResult, pw, ph);
                  this.fboA = this.ensureFbo(this.fboA, halfW, halfH);
                  Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
                  Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
                  Matrix4f savedModelView = new Matrix4f(modelViewStack);
                  RenderSystem.disableDepthTest();
                  RenderSystem.disableCull();
                  this.downsample(mainFbo, this.fboA, this.fullWidth, this.fullHeight, uvOffX, uvOffY, uvSclX, uvSclY, strength);
                  this.upsample(this.fboA, this.blurResult, strength);
                  RenderSystem.enableDepthTest();
                  RenderSystem.enableCull();
                  RenderSystem.getProjectionMatrix().set(savedProj);
                  modelViewStack.set(savedModelView);
                  mainFbo.beginWrite(false);
                  GlStateManager._viewport(0, 0, this.fullWidth, this.fullHeight);
                  GlStateManager._activeTexture(33984);
                  GlStateManager._bindTexture(0);
               }
            }
         }
      }
   }

   private void downsample(
      Framebuffer input, Framebuffer output, int inputW, int inputH, float uvOffX, float uvOffY, float uvSclX, float uvSclY, float strength
   ) {
      output.beginWrite(false);
      GlStateManager._viewport(0, 0, output.textureWidth, output.textureHeight);
      GlStateManager._activeTexture(33984);
      RenderSystem.bindTexture(input.getColorAttachment());
      GlStateManager._texParameter(3553, 10241, 9729);
      GlStateManager._texParameter(3553, 10240, 9729);
      ShaderProgram shader = RenderSystem.setShader(this.DOWN_KEY);
      if (shader != null) {
         shader.getUniformOrDefault("InputSize").set((float) inputW, (float) inputH);
         shader.getUniformOrDefault("OutputSize").set((float) output.textureWidth, (float) output.textureHeight);
         shader.getUniformOrDefault("UVOffset").set(uvOffX, uvOffY);
         shader.getUniformOrDefault("UVScale").set(uvSclX, uvSclY);
         shader.getUniformOrDefault("Strength").set(strength);
         this.drawFullscreenQuad(output.textureWidth, output.textureHeight);
      }
   }

   private void upsample(Framebuffer input, Framebuffer output, float strength) {
      output.beginWrite(false);
      GlStateManager._viewport(0, 0, output.textureWidth, output.textureHeight);
      GlStateManager._activeTexture(33984);
      RenderSystem.bindTexture(input.getColorAttachment());
      GlStateManager._texParameter(3553, 10241, 9729);
      GlStateManager._texParameter(3553, 10240, 9729);
      ShaderProgram shader = RenderSystem.setShader(this.UP_KEY);
      if (shader != null) {
         shader.getUniformOrDefault("InputSize").set((float) input.textureWidth, (float) input.textureHeight);
         shader.getUniformOrDefault("OutputSize").set((float) output.textureWidth, (float) output.textureHeight);
         shader.getUniformOrDefault("Strength").set(strength);
         this.drawFullscreenQuad(output.textureWidth, output.textureHeight);
      }
   }

   private void drawFullscreenQuad(int width, int height) {
      Matrix4f projMat = new Matrix4f().setOrtho(0.0F, width, 0.0F, height, -1000.0F, 1000.0F);
      Matrix4f modelView = new Matrix4f();
      BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
      buffer.vertex(modelView, 0.0F, 0.0F, 0.0F);
      buffer.vertex(modelView, 0.0F, height, 0.0F);
      buffer.vertex(modelView, width, height, 0.0F);
      buffer.vertex(modelView, width, 0.0F, 0.0F);
      RenderSystem.getModelViewStack().set(modelView);
      RenderSystem.getProjectionMatrix().set(projMat);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private Framebuffer ensureFbo(Framebuffer fbo, int width, int height) {
      if (fbo == null) {
         Framebuffer var4 = new SimpleFramebuffer(width, height, false);
         this.setClampToEdge(var4);
         return var4;
      } else {
         if (fbo.textureWidth != width || fbo.textureHeight != height) {
            fbo.resize(width, height);
            this.setClampToEdge(fbo);
         }

         return fbo;
      }
   }

   private void setClampToEdge(Framebuffer fbo) {
      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(fbo.getColorAttachment());
      GlStateManager._texParameter(3553, 10242, 33071);
      GlStateManager._texParameter(3553, 10243, 33071);
      GlStateManager._bindTexture(0);
   }
}
