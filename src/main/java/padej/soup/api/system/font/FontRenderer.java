package padej.soup.api.system.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.render.TextFactoryEvent;
import padej.soup.api.system.font.msdf.MsdfFont;
import padej.soup.api.system.font.msdf.MsdfGlyph;
import padej.soup.api.system.font.msdf.MsdfShaders;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.perftest.HudProfiler;

public class FontRenderer implements QuickImports {
   private static final float DEFAULT_THICKNESS = 0.0F;
   private static final float DEFAULT_SMOOTHNESS = 0.5F;
   private static final float DEFAULT_SPACING = 0.0F;
   private static final float Y_CORRECT = 2.25F;
   private static boolean msdfFailed = false;
   public static double ANIMATION_TIME = 0.0;
   private static final int STRING_BUILDER_POOL_SIZE = 16;
   private static final Queue<StringBuilder> STRING_BUILDER_POOL = new ArrayDeque<>(16);
   private static final Map<FontRenderer.ColorInterpKey, Integer> COLOR_INTERP_CACHE;
   private static final int BAKED_CACHE_SIZE = 256;
   private static final int BAKED_MAX_TEXT_LEN = 2000;
   private static final LinkedHashMap<FontRenderer.BakedKey, FontRenderer.BakedString> BAKED_CACHE;
   private final MsdfFont msdfFont;
   private final float pixelSize;
   private static FontRenderer.FontBatch ACTIVE_BATCH;
   private static final Matrix4f IDENTITY_MAT;
   private static final Vector3f BATCH_SCRATCH;
   private static final char EJECT_DELIM = '⏏';
   private static final float AA_PADDING_EM = 0.15F;
   private static int lastShaderId;
   private static float u_Range;
   private static float u_Thickness;
   private static float u_Smoothness;
   private static int u_Outline;
   private static int u_EnableFadeout;
   private static float u_FadeoutStart;
   private static float u_FadeoutEnd;
   private static float u_MaxWidth;
   private static float u_TextPosX;

   public FontRenderer(MsdfFont msdfFont, float pixelSize) {
      this.msdfFont = msdfFont;
      this.pixelSize = pixelSize;
   }

   private FontRenderer.BakedString bakeText(String text, int defaultColor, boolean legacyApply) {
      int n = 0;
      boolean expect = false;
      int[] ejectOut = new int[1];

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (expect) {
            expect = false;
         } else if (c == 167) {
            expect = true;
         } else {
            if (c == 9167) {
               int close = tryParseEjectColor(text, i, ejectOut);
               if (close >= 0) {
                  i = close;
                  continue;
               }
            }

            if (this.msdfFont.getGlyph(c) != null) {
               n++;
            }
         }
      }

      FontRenderer.BakedString baked = new FontRenderer.BakedString(n);
      if (n == 0) {
         return baked;
      } else {
         float cursor = 0.0F;
         float baselineOffset = 0.0F;
         float lineHeight = this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
         int activeColor = defaultColor;
         int prevChar = -1;
         expect = false;
         int idx = 0;
         float perCharExtra = 0.0F * this.pixelSize + 0.0F;
         float legacyShift = legacyApply ? -1.0F : 0.0F;

         for (int ix = 0; ix < text.length(); ix++) {
            char c = text.charAt(ix);
            if (expect) {
               expect = false;
               char up = Character.toUpperCase(c);
               if (ColorUtil.colorCodes.containsKey(up)) {
                  activeColor = 0xFF000000 | ColorUtil.colorCodes.get(up);
               } else if (up == 'R') {
                  activeColor = defaultColor;
               }
            } else if (c == 167) {
               expect = true;
            } else {
               if (c == 9167) {
                  int close = tryParseEjectColor(text, ix, ejectOut);
                  if (close >= 0) {
                     activeColor = ejectOut[0];
                     ix = close;
                     continue;
                  }
               }

               if (c == '\n') {
                  baselineOffset += lineHeight;
                  cursor = 0.0F;
                  prevChar = -1;
               } else {
                  MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                  if (glyph != null) {
                     if (prevChar != -1) {
                        cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                     }

                     baked.qx[idx] = cursor + glyph.getLeftBearing() * this.pixelSize;
                     baked.qy[idx] = baselineOffset + -glyph.getTopPosition() * this.pixelSize + legacyShift;
                     baked.qw[idx] = glyph.getPlaneWidth() * this.pixelSize;
                     baked.qh[idx] = glyph.getPlaneHeight() * this.pixelSize;
                     baked.umin[idx] = glyph.getMinU();
                     baked.umax[idx] = glyph.getMaxU();
                     baked.vmin[idx] = glyph.getMinV();
                     baked.vmax[idx] = glyph.getMaxV();
                     baked.col[idx] = activeColor;
                     idx++;
                     cursor += glyph.getWidth(this.pixelSize) + perCharExtra;
                     prevChar = c;
                  }
               }
            }
         }

         return baked;
      }
   }

   private void emitBaked(Matrix4f mat, BufferBuilder builder, FontRenderer.BakedString baked, float x, float baseline) {
      int n = baked.n;
      float[] qx = baked.qx;
      float[] qy = baked.qy;
      float[] qw = baked.qw;
      float[] qh = baked.qh;
      float[] umin = baked.umin;
      float[] umax = baked.umax;
      float[] vmin = baked.vmin;
      float[] vmax = baked.vmax;
      int[] col = baked.col;

      for (int i = 0; i < n; i++) {
         float ax = x + qx[i];
         float ay = baseline + qy[i];
         float aw = qw[i];
         float ah = qh[i];
         float u0 = umin[i];
         float u1 = umax[i];
         float v0 = vmin[i];
         float v1 = vmax[i];
         int color = col[i];
         builder.vertex(mat, ax, ay, 0.0F).texture(u0, v0).color(color);
         builder.vertex(mat, ax, ay + ah, 0.0F).texture(u0, v1).color(color);
         builder.vertex(mat, ax + aw, ay + ah, 0.0F).texture(u1, v1).color(color);
         builder.vertex(mat, ax + aw, ay, 0.0F).texture(u1, v0).color(color);
      }
   }

   private void emitBakedToBatch(FontRenderer.FontBatch batch, Matrix4f mat, FontRenderer.BakedString baked, float x, float baseline) {
      int n = baked.n;
      float[] qx = baked.qx;
      float[] qy = baked.qy;
      float[] qw = baked.qw;
      float[] qh = baked.qh;
      float[] umin = baked.umin;
      float[] umax = baked.umax;
      float[] vmin = baked.vmin;
      float[] vmax = baked.vmax;
      int[] col = baked.col;
      Vector3f v = BATCH_SCRATCH;
      float alphaMul = RenderSystem.getShaderColor()[3];

      for (int i = 0; i < n; i++) {
         float ax = x + qx[i];
         float ay = baseline + qy[i];
         float aw = qw[i];
         float ah = qh[i];
         float u0 = umin[i];
         float u1 = umax[i];
         float v0c = vmin[i];
         float v1c = vmax[i];
         int color = applyAlphaMul(col[i], alphaMul);
         mat.transformPosition(ax, ay, 0.0F, v);
         float x0 = v.x;
         float y0 = v.y;
         mat.transformPosition(ax, ay + ah, 0.0F, v);
         float x1 = v.x;
         float y1 = v.y;
         mat.transformPosition(ax + aw, ay + ah, 0.0F, v);
         float x2 = v.x;
         float y2 = v.y;
         mat.transformPosition(ax + aw, ay, 0.0F, v);
         float x3 = v.x;
         float y3 = v.y;
         batch.submitQuad(this, x0, y0, u0, v0c, x1, y1, u0, v1c, x2, y2, u1, v1c, x3, y3, u1, v0c, color);
      }
   }

   private static int applyAlphaMul(int color, float mul) {
      if (mul >= 0.999F) {
         return color;
      } else {
         int a = color >>> 24 & 0xFF;
         int newA = Math.max(0, Math.min(255, Math.round(a * mul)));
         return newA << 24 | color & 16777215;
      }
   }

   private FontRenderer.BakedString getOrBakeText(String text, int defaultColor, boolean legacyApply) {
      if (text.length() > 2000) {
         return null;
      } else {
         int pxQ = Math.round(this.pixelSize * 100.0F);
         FontRenderer.BakedKey key = new FontRenderer.BakedKey(text, pxQ, System.identityHashCode(this.msdfFont), defaultColor, legacyApply);
         FontRenderer.BakedString cached = BAKED_CACHE.get(key);
         if (cached != null) {
            return cached;
         } else {
            FontRenderer.BakedString baked = this.bakeText(text, defaultColor, legacyApply);
            BAKED_CACHE.put(key, baked);
            return baked;
         }
      }
   }

   public static void invalidateBakedCache() {
      BAKED_CACHE.clear();
   }

   public static FontRenderer.FontBatch activeBatch() {
      return ACTIVE_BATCH;
   }

   private static StringBuilder acquireStringBuilder() {
      StringBuilder sb = STRING_BUILDER_POOL.poll();
      if (sb == null) {
         sb = new StringBuilder(256);
      } else {
         sb.setLength(0);
      }

      return sb;
   }

   private static void releaseStringBuilder(StringBuilder sb) {
      if (STRING_BUILDER_POOL.size() < 16) {
         sb.setLength(0);
         STRING_BUILDER_POOL.offer(sb);
      }
   }

   private static int tryParseEjectColor(String text, int startIdx, int[] out) {
      int closeIdx = text.indexOf(9167, startIdx + 1);
      if (closeIdx < 0) {
         return -1;
      } else {
         try {
            int rgb = Integer.parseInt(text.substring(startIdx + 1, closeIdx));
            out[0] = 0xFF000000 | rgb;
            return closeIdx;
         } catch (NumberFormatException var5) {
            return -1;
         }
      }
   }

   public void drawText(MatrixStack matrix, Text text, double x, double y) {
      StringBuilder sb = acquireStringBuilder();

      try {
         this.findStyle(sb, text);
         this.drawString(matrix, sb.toString(), x, y, ColorUtil.getText());
      } finally {
         releaseStringBuilder(sb);
      }
   }

   public void findStyle(StringBuilder sb, Text component) {
      Style style = component.getStyle();
      if (component.getSiblings().isEmpty()) {
         if (style.getColor() != null) {
            sb.append(ColorUtil.formatting(style.getColor().getRgb()));
         }

         sb.append(component.getString()).append(Formatting.RESET);
      } else {
         component.getWithStyle(style).forEach(t -> this.findStyle(sb, t));
      }
   }

   public void drawStringWithScroll(MatrixStack matrix, String text, double x, double y, float width, int color) {
      String separation = "  |  ";
      float textWidth = this.getStringWidth(text + separation);
      if (textWidth - width < 10.0F) {
         this.drawString(matrix, text, x, y, color);
      } else {
         this.drawString(matrix, text + separation + text, x - MathUtil.textScrolling(textWidth), y, color);
      }
   }

   public void drawStringWithFadeout(MatrixStack matrix, String text, double x, double y, float maxWidth, float fadeStart, int color) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      float startX = (float)x;
      this.renderTextPerCharWithColorCodes(matrix, text, startX, (float)y, color, (idx, ch, cursor, activeColor) -> {
         float relX = cursor - startX;
         if (relX >= maxWidth) {
            return 0;
         } else if (relX > fadeStart) {
            float fadeProgress = (relX - fadeStart) / (maxWidth - fadeStart);
            int origAlpha = activeColor >>> 24 & 0xFF;
            int newAlpha = (int)(origAlpha * (1.0F - fadeProgress));
            return newAlpha << 24 | activeColor & 16777215;
         } else {
            return activeColor;
         }
      });
   }

   private void renderTextPerCharWithColorCodes(
      MatrixStack matrix, String text, float x, float y, int defaultColor, FontRenderer.ColorPerCharWithCursor resolver
   ) {
      if (text != null && !text.isEmpty()) {
         if (msdfFailed) {
            this.fallbackRender(matrix, text, x, y, defaultColor);
         } else {
            Matrix4f mat = matrix.peek().getPositionMatrix();
            float baseline = this.getBaselineY(y);
            int[] ejectOut = new int[1];
            if (ACTIVE_BATCH != null) {
               float cursor = x;
               int activeColor = defaultColor;
               int prevChar = -1;
               boolean expectColorCode = false;
               Vector3f v = BATCH_SCRATCH;
               float alphaMul = RenderSystem.getShaderColor()[3];

               for (int i = 0; i < text.length(); i++) {
                  char c = text.charAt(i);
                  if (expectColorCode) {
                     expectColorCode = false;
                     char up = Character.toUpperCase(c);
                     if (ColorUtil.colorCodes.containsKey(up)) {
                        activeColor = 0xFF000000 | ColorUtil.colorCodes.get(up);
                     } else if (up == 'R') {
                        activeColor = defaultColor;
                     }
                  } else if (c == 167) {
                     expectColorCode = true;
                  } else {
                     if (c == 9167) {
                        int close = tryParseEjectColor(text, i, ejectOut);
                        if (close >= 0) {
                           activeColor = ejectOut[0];
                           i = close;
                           continue;
                        }
                     }

                     if (c == '\n') {
                        baseline += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                        cursor = x;
                        prevChar = -1;
                     } else {
                        MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                        if (glyph != null) {
                           if (prevChar != -1) {
                              cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                           }

                           int glyphColor = resolver.colorAt(i, c, cursor, activeColor);
                           if (glyphColor == 0) {
                              cursor += glyph.getWidth(this.pixelSize) + 0.0F * this.pixelSize + 0.0F;
                              prevChar = c;
                           } else {
                              glyphColor = applyAlphaMul(glyphColor, alphaMul);
                              float qx = cursor + glyph.getLeftBearing() * this.pixelSize;
                              float qy = baseline - glyph.getTopPosition() * this.pixelSize - 1.0F;
                              float qw = glyph.getPlaneWidth() * this.pixelSize;
                              float qh = glyph.getPlaneHeight() * this.pixelSize;
                              mat.transformPosition(qx, qy, 0.0F, v);
                              float x0 = v.x;
                              float y0 = v.y;
                              mat.transformPosition(qx, qy + qh, 0.0F, v);
                              float x1 = v.x;
                              float y1 = v.y;
                              mat.transformPosition(qx + qw, qy + qh, 0.0F, v);
                              float x2 = v.x;
                              float y2 = v.y;
                              mat.transformPosition(qx + qw, qy, 0.0F, v);
                              float x3 = v.x;
                              float y3 = v.y;
                              ACTIVE_BATCH.submitQuad(
                                 this,
                                 x0,
                                 y0,
                                 glyph.getMinU(),
                                 glyph.getMinV(),
                                 x1,
                                 y1,
                                 glyph.getMinU(),
                                 glyph.getMaxV(),
                                 x2,
                                 y2,
                                 glyph.getMaxU(),
                                 glyph.getMaxV(),
                                 x3,
                                 y3,
                                 glyph.getMaxU(),
                                 glyph.getMinV(),
                                 glyphColor
                              );
                              cursor += glyph.getWidth(this.pixelSize) + 0.0F * this.pixelSize + 0.0F;
                              prevChar = c;
                           }
                        }
                     }
                  }
               }
            } else {
               try {
                  this.beginMsdfDraw(x);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  float cursor = x;
                  int activeColor = defaultColor;
                  int prevChar = -1;
                  boolean expectColorCode = false;

                  for (int ix = 0; ix < text.length(); ix++) {
                     char c = text.charAt(ix);
                     if (expectColorCode) {
                        expectColorCode = false;
                        char up = Character.toUpperCase(c);
                        if (ColorUtil.colorCodes.containsKey(up)) {
                           activeColor = 0xFF000000 | ColorUtil.colorCodes.get(up);
                        } else if (up == 'R') {
                           activeColor = defaultColor;
                        }
                     } else if (c == 167) {
                        expectColorCode = true;
                     } else {
                        if (c == 9167) {
                           int close = tryParseEjectColor(text, ix, ejectOut);
                           if (close >= 0) {
                              activeColor = ejectOut[0];
                              ix = close;
                              continue;
                           }
                        }

                        if (c == '\n') {
                           baseline += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                           cursor = x;
                           prevChar = -1;
                        } else {
                           MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                           if (glyph != null) {
                              if (prevChar != -1) {
                                 cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                              }

                              int glyphColor = resolver.colorAt(ix, c, cursor, activeColor);
                              if (glyphColor == 0) {
                                 cursor += glyph.getWidth(this.pixelSize) + 0.0F * this.pixelSize + 0.0F;
                                 prevChar = c;
                              } else {
                                 float advance = glyph.apply(mat, builder, this.pixelSize, cursor, baseline, 0.0F, glyphColor);
                                 cursor += advance + 0.0F * this.pixelSize + 0.0F;
                                 prevChar = c;
                              }
                           }
                        }
                     }
                  }

                  this.endMsdfDraw(builder);
               } catch (Exception var32) {
                  msdfFailed = true;
                  this.fallbackRender(matrix, text, x, y, defaultColor);
               }
            }
         }
      }
   }

   public void drawItalicString(MatrixStack matrix, String text, double x, double y, int color) {
      matrix.push();
      float shear = -0.15F;
      Matrix4f mat = matrix.peek().getPositionMatrix();
      mat.m10(mat.m10() + shear);
      mat.m30(mat.m30() - shear * (float)y);
      this.drawString(matrix, text, x, y, color);
      matrix.pop();
   }

   public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
      this.drawString(stack, s, x - this.getStringWidth(s) / 2.0F, y, color);
   }

   public void drawString(MatrixStack matrix, String text, double x, double y, int color) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      this.renderTextWithColorCodes(matrix, text, (float)x, (float)y, color);
   }

   public void drawGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      int totalChars = Math.max(1, text.length() - 1);
      this.renderTextPerChar(matrix, text, (float)x, (float)y, (idx, ch) -> this.interpolateColor(colorStart, colorEnd, (float)idx / totalChars));
   }

   @Deprecated
   public void drawAnimatedGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, waveLength, speed, 0.3F);
   }

   @Deprecated
   public void drawAnimatedGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd);
   }

   public void drawWaveGradientString(
      MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed, float minBrightness
   ) {
      double safeWaveLength = waveLength <= 0.0 ? Math.PI * 2 : waveLength;
      double durationSec = speed <= 0.0 ? 2.4 : 7.2 / speed;
      int symbolCount = this.countWaveSymbols(text);
      double totalCycleSpread = safeWaveLength / (Math.PI * 2);
      double delayPerCharSec = symbolCount <= 1 ? 0.0 : durationSec * totalCycleSpread / (symbolCount - 1);
      this.drawWaveGradientString(matrix, text, x, y, new int[]{colorStart, colorEnd, colorStart}, durationSec, delayPerCharSec, minBrightness);
   }

   public void drawWaveGradientString(
      MatrixStack matrix, String text, double x, double y, int[] keyframeColors, double durationSec, double delayPerCharSec, float minBrightness
   ) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      ANIMATION_TIME = System.currentTimeMillis() / 1000.0;
      int[] palette = this.sanitizeKeyframeColors(keyframeColors);
      int[] normalizedPalette = this.normalizePaletteBrightness(palette, this.clamp01(minBrightness));
      double safeDuration = Math.max(0.05, durationSec);
      double safeDelay = Math.max(0.0, delayPerCharSec);
      this.renderTextPerChar(matrix, text, (float)x, (float)y, (idx, ch) -> {
         double progress = this.computeWaveProgress(ANIMATION_TIME, safeDuration, safeDelay, idx);
         return this.sampleKeyframeColor(normalizedPalette, progress);
      });
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int[] keyframeColors, double durationSec, double delayPerCharSec) {
      this.drawWaveGradientString(matrix, text, x, y, keyframeColors, durationSec, delayPerCharSec, 0.3F);
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, waveLength, speed, 0.3F);
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, Math.PI * 2, 3.0, 0.3F);
   }

   public float getStringWidth(Text text) {
      return text != null ? this.getStringWidth(text.getString()) : 0.0F;
   }

   public float getStringWidth(String text) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      float perCharExtra = 0.0F * this.pixelSize + 0.0F;
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;
      boolean skipNext = false;
      int prevChar = -1;
      int[] ejectOut = new int[1];

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            if (c == 9167) {
               int close = tryParseEjectColor(text, i, ejectOut);
               if (close >= 0) {
                  i = close;
                  continue;
               }
            }

            if (c == '\n') {
               maxPreviousLines = Math.max(currentLine, maxPreviousLines);
               currentLine = 0.0F;
               prevChar = -1;
            } else {
               MsdfGlyph glyph = this.msdfFont.getGlyph(c);
               if (glyph != null) {
                  if (prevChar != -1) {
                     currentLine += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                  }

                  currentLine += glyph.getWidth(this.pixelSize) + perCharExtra;
                  prevChar = c;
               }
            }
         }
      }

      return Math.max(currentLine, maxPreviousLines);
   }

   public float getStringHeight(Text text) {
      return this.getStringHeight(text.getString());
   }

   public float getStringHeight(String text) {
      if (text != null && !text.isEmpty()) {
         int lines = 1;

         for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
               lines++;
            }
         }

         return lines * this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
      } else {
         return this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
      }
   }

   public float getActualTextHeight(String text) {
      return (this.msdfFont.getMetrics().ascender() - this.msdfFont.getMetrics().descender()) * this.pixelSize;
   }

   public float getLowerCaseHeight(String text) {
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase();
         float maxTop = Float.NEGATIVE_INFINITY;
         float minBottom = Float.POSITIVE_INFINITY;
         boolean any = false;
         boolean skipNext = false;
         int[] ejectOut = new int[1];

         for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (skipNext) {
               skipNext = false;
            } else if (c == 167) {
               skipNext = true;
            } else {
               if (c == 9167) {
                  int close = tryParseEjectColor(lower, i, ejectOut);
                  if (close >= 0) {
                     i = close;
                     continue;
                  }
               }

               MsdfGlyph g = this.msdfFont.getGlyph(c);
               if (g != null) {
                  if (g.getTopPosition() > maxTop) {
                     maxTop = g.getTopPosition();
                  }

                  if (g.getBottomPosition() < minBottom) {
                     minBottom = g.getBottomPosition();
                  }

                  any = true;
               }
            }
         }

         return !any ? 0.0F : (maxTop - minBottom) * this.pixelSize;
      } else {
         return 0.0F;
      }
   }

   public void drawCenteredInRow(MatrixStack matrix, String text, double rowLeft, double rowTop, double rowWidth, double rowHeight, int color) {
      if (text != null && !text.isEmpty()) {
         float maxTop = Float.NEGATIVE_INFINITY;
         float minBottom = Float.POSITIVE_INFINITY;
         MsdfGlyph firstG = null;
         boolean skipNext = false;
         int[] ejectOut = new int[1];

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipNext) {
               skipNext = false;
            } else if (c == 167) {
               skipNext = true;
            } else {
               if (c == 9167) {
                  int close = tryParseEjectColor(text, i, ejectOut);
                  if (close >= 0) {
                     i = close;
                     continue;
                  }
               }

               MsdfGlyph g = this.msdfFont.getGlyph(c);
               if (g != null) {
                  float gTop = g.getTopPosition();
                  float gBottom = g.getBottomPosition();
                  if (gBottom > -0.15F) {
                     gBottom = 0.0F;
                  }

                  if (gTop > maxTop) {
                     maxTop = gTop;
                  }

                  if (gBottom < minBottom) {
                     minBottom = gBottom;
                  }

                  if (firstG == null) {
                     firstG = g;
                  }
               }
            }
         }

         if (firstG != null) {
            if (minBottom > 0.0F) {
               minBottom = 0.0F;
            }

            float cursorWidth = this.getStringWidth(text);
            float drawX = (float)rowLeft + ((float)rowWidth - cursorWidth) * 0.5F;
            float visibleHeight = (maxTop - minBottom) * this.pixelSize;
            float visualTop = (float)rowTop + ((float)rowHeight - visibleHeight) * 0.5F;
            float baseline = visualTop + maxTop * this.pixelSize - 1.0F;
            this.renderAtBaselineWithColorCodes(matrix, text, drawX, baseline, color);
         }
      }
   }

   public void drawCenteredInRow(MatrixStack matrix, String text, double x, double rowTop, double rowHeight, int color) {
      this.drawCenteredInRow(matrix, text, x, rowTop, 0.0, rowHeight, color);
   }

   public void drawIcon(MatrixStack matrix, int codepoint, float rectX, float rectY, float size, int color) {
      if (!msdfFailed) {
         MsdfGlyph glyph = this.msdfFont.getGlyph(codepoint);
         if (glyph != null) {
            Matrix4f mat = matrix.peek().getPositionMatrix();
            float cx = rectX + size * 0.5F;
            float cy = rectY + size * 0.5F;
            if (ACTIVE_BATCH != null) {
               float screenW = glyph.getPlaneHeight() * size;
               float screenH = glyph.getPlaneWidth() * size;
               float qx = cx - screenW * 0.5F;
               float qy = cy - screenH * 0.5F;
               Vector3f v = BATCH_SCRATCH;
               mat.transformPosition(qx, qy, 0.0F, v);
               float x0 = v.x;
               float y0 = v.y;
               mat.transformPosition(qx, qy + screenH, 0.0F, v);
               float x1 = v.x;
               float y1 = v.y;
               mat.transformPosition(qx + screenW, qy + screenH, 0.0F, v);
               float x2 = v.x;
               float y2 = v.y;
               mat.transformPosition(qx + screenW, qy, 0.0F, v);
               float x3 = v.x;
               float y3 = v.y;
               float minU = glyph.getMinU();
               float maxU = glyph.getMaxU();
               float minV = glyph.getMinV();
               float maxV = glyph.getMaxV();
               int bakedColor = applyAlphaMul(color, RenderSystem.getShaderColor()[3]);
               ACTIVE_BATCH.submitQuad(this, x0, y0, maxU, minV, x1, y1, minU, minV, x2, y2, minU, maxV, x3, y3, maxU, maxV, bakedColor);
            } else {
               try {
                  this.beginMsdfDraw(rectX);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  glyph.emitRot90CCW(mat, builder, size, cx, cy, 0.0F, color);
                  this.endMsdfDraw(builder);
               } catch (Exception var29) {
                  msdfFailed = true;
               }
            }
         }
      }
   }

   private void renderAtBaselineWithColorCodes(MatrixStack matrix, String text, float x, float baseline, int defaultColor) {
      if (msdfFailed) {
         this.fallbackRender(matrix, text, x, baseline, defaultColor);
      } else {
         Matrix4f mat = matrix.peek().getPositionMatrix();
         FontRenderer.BakedString baked = this.getOrBakeText(text, defaultColor, false);
         if (baked != null) {
            if (ACTIVE_BATCH != null) {
               this.emitBakedToBatch(ACTIVE_BATCH, mat, baked, x, baseline);
            } else {
               try {
                  this.beginMsdfDraw(x);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  this.emitBaked(mat, builder, baked, x, baseline);
                  this.endMsdfDraw(builder);
               } catch (Exception var19) {
                  msdfFailed = true;
                  this.fallbackRender(matrix, text, x, baseline, defaultColor);
               }
            }
         } else {
            try {
               this.beginMsdfDraw(x);
               BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               float cursor = x;
               float bl = baseline;
               int activeColor = defaultColor;
               int prevChar = -1;
               boolean expectColorCode = false;
               int[] ejectOut = new int[1];

               for (int i = 0; i < text.length(); i++) {
                  char c = text.charAt(i);
                  if (expectColorCode) {
                     expectColorCode = false;
                     char up = Character.toUpperCase(c);
                     if (ColorUtil.colorCodes.containsKey(up)) {
                        activeColor = 0xFF000000 | ColorUtil.colorCodes.get(up);
                     } else if (up == 'R') {
                        activeColor = defaultColor;
                     }
                  } else if (c == 167) {
                     expectColorCode = true;
                  } else {
                     if (c == 9167) {
                        int close = tryParseEjectColor(text, i, ejectOut);
                        if (close >= 0) {
                           activeColor = ejectOut[0];
                           i = close;
                           continue;
                        }
                     }

                     if (c == '\n') {
                        bl += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                        cursor = x;
                        prevChar = -1;
                     } else {
                        MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                        if (glyph != null) {
                           if (prevChar != -1) {
                              cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                           }

                           float advance = glyph.emit(mat, builder, this.pixelSize, cursor, bl, 0.0F, activeColor);
                           cursor += advance + 0.0F * this.pixelSize + 0.0F;
                           prevChar = c;
                        }
                     }
                  }
               }

               this.endMsdfDraw(builder);
            } catch (Exception var20) {
               msdfFailed = true;
               this.fallbackRender(matrix, text, x, baseline, defaultColor);
            }
         }
      }
   }

   private void renderTextWithColorCodes(MatrixStack matrix, String text, float x, float y, int defaultColor) {
      if (text != null && !text.isEmpty()) {
         if (msdfFailed) {
            this.fallbackRender(matrix, text, x, y, defaultColor);
         } else {
            Matrix4f mat = matrix.peek().getPositionMatrix();
            float baseline = this.getBaselineY(y);
            FontRenderer.BakedString baked = this.getOrBakeText(text, defaultColor, true);
            if (baked == null) {
               try {
                  this.beginMsdfDraw(x);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  float cursor = x;
                  int activeColor = defaultColor;
                  int prevChar = -1;
                  boolean expectColorCode = false;
                  int[] ejectOut = new int[1];

                  for (int i = 0; i < text.length(); i++) {
                     char c = text.charAt(i);
                     if (expectColorCode) {
                        expectColorCode = false;
                        char up = Character.toUpperCase(c);
                        if (ColorUtil.colorCodes.containsKey(up)) {
                           activeColor = 0xFF000000 | ColorUtil.colorCodes.get(up);
                        } else if (up == 'R') {
                           activeColor = defaultColor;
                        }
                     } else if (c == 167) {
                        expectColorCode = true;
                     } else {
                        if (c == 9167) {
                           int close = tryParseEjectColor(text, i, ejectOut);
                           if (close >= 0) {
                              activeColor = ejectOut[0];
                              i = close;
                              continue;
                           }
                        }

                        if (c == '\n') {
                           baseline += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                           cursor = x;
                           prevChar = -1;
                        } else {
                           MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                           if (glyph != null) {
                              if (prevChar != -1) {
                                 cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                              }

                              float advance = glyph.apply(mat, builder, this.pixelSize, cursor, baseline, 0.0F, activeColor);
                              cursor += advance + 0.0F * this.pixelSize + 0.0F;
                              prevChar = c;
                           }
                        }
                     }
                  }

                  this.endMsdfDraw(builder);
               } catch (Exception var20) {
                  msdfFailed = true;
                  this.fallbackRender(matrix, text, x, y, defaultColor);
               }
            } else if (ACTIVE_BATCH != null) {
               this.emitBakedToBatch(ACTIVE_BATCH, mat, baked, x, baseline);
            } else {
               try {
                  this.beginMsdfDraw(x);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  this.emitBaked(mat, builder, baked, x, baseline);
                  this.endMsdfDraw(builder);
               } catch (Exception var19) {
                  msdfFailed = true;
                  this.fallbackRender(matrix, text, x, y, defaultColor);
               }
            }
         }
      }
   }

   private void renderTextPerChar(MatrixStack matrix, String text, float x, float y, FontRenderer.ColorPerChar resolver) {
      if (text != null && !text.isEmpty()) {
         if (msdfFailed) {
            this.fallbackRender(matrix, text, x, y, -1);
         } else {
            Matrix4f mat = matrix.peek().getPositionMatrix();
            if (ACTIVE_BATCH != null) {
               float cursor = x;
               float baseline = this.getBaselineY(y);
               int prevChar = -1;
               Vector3f v = BATCH_SCRATCH;
               float alphaMul = RenderSystem.getShaderColor()[3];

               for (int i = 0; i < text.length(); i++) {
                  char c = text.charAt(i);
                  if (c == '\n') {
                     baseline += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                     cursor = x;
                     prevChar = -1;
                  } else {
                     MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                     if (glyph != null) {
                        if (prevChar != -1) {
                           cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                        }

                        int color = applyAlphaMul(resolver.colorAt(i, c), alphaMul);
                        float qx = cursor + glyph.getLeftBearing() * this.pixelSize;
                        float qy = baseline - glyph.getTopPosition() * this.pixelSize - 1.0F;
                        float qw = glyph.getPlaneWidth() * this.pixelSize;
                        float qh = glyph.getPlaneHeight() * this.pixelSize;
                        mat.transformPosition(qx, qy, 0.0F, v);
                        float x0 = v.x;
                        float y0 = v.y;
                        mat.transformPosition(qx, qy + qh, 0.0F, v);
                        float x1 = v.x;
                        float y1 = v.y;
                        mat.transformPosition(qx + qw, qy + qh, 0.0F, v);
                        float x2 = v.x;
                        float y2 = v.y;
                        mat.transformPosition(qx + qw, qy, 0.0F, v);
                        float x3 = v.x;
                        float y3 = v.y;
                        ACTIVE_BATCH.submitQuad(
                           this,
                           x0,
                           y0,
                           glyph.getMinU(),
                           glyph.getMinV(),
                           x1,
                           y1,
                           glyph.getMinU(),
                           glyph.getMaxV(),
                           x2,
                           y2,
                           glyph.getMaxU(),
                           glyph.getMaxV(),
                           x3,
                           y3,
                           glyph.getMaxU(),
                           glyph.getMinV(),
                           color
                        );
                        cursor += glyph.getWidth(this.pixelSize) + 0.0F * this.pixelSize + 0.0F;
                        prevChar = c;
                     }
                  }
               }
            } else {
               try {
                  this.beginMsdfDraw(x);
                  BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  float cursor = x;
                  float baseline = this.getBaselineY(y);
                  int prevChar = -1;

                  for (int ix = 0; ix < text.length(); ix++) {
                     char c = text.charAt(ix);
                     if (c == '\n') {
                        baseline += this.msdfFont.getMetrics().lineHeight() * this.pixelSize;
                        cursor = x;
                        prevChar = -1;
                     } else {
                        MsdfGlyph glyph = this.msdfFont.getGlyph(c);
                        if (glyph != null) {
                           if (prevChar != -1) {
                              cursor += this.msdfFont.getKerning(prevChar, c) * this.pixelSize;
                           }

                           int color = resolver.colorAt(ix, c);
                           float advance = glyph.apply(mat, builder, this.pixelSize, cursor, baseline, 0.0F, color);
                           cursor += advance + 0.0F * this.pixelSize + 0.0F;
                           prevChar = c;
                        }
                     }
                  }

                  this.endMsdfDraw(builder);
               } catch (Exception var28) {
                  msdfFailed = true;
                  this.fallbackRender(matrix, text, x, y, -1);
               }
            }
         }
      }
   }

   private static void resetUniformCacheIfShaderChanged(ShaderProgram shader) {
      int id = shader != null ? System.identityHashCode(shader) : -1;
      if (id != lastShaderId) {
         lastShaderId = id;
         u_Smoothness = Float.NaN;
         u_Thickness = Float.NaN;
         u_Range = Float.NaN;
         u_EnableFadeout = -1;
         u_Outline = -1;
         u_TextPosX = Float.NaN;
         u_MaxWidth = Float.NaN;
         u_FadeoutEnd = Float.NaN;
         u_FadeoutStart = Float.NaN;
      }
   }

   private void beginMsdfDraw(float textOriginX) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderTexture(0, this.msdfFont.getTextureId());
      ShaderProgram shader = RenderSystem.setShader(MsdfShaders.MSDF_FONT_SHADER_KEY);
      if (shader == null) {
         throw new IllegalStateException("MSDF font shader not available");
      } else {
         resetUniformCacheIfShaderChanged(shader);
         float range = this.msdfFont.getAtlas().range();
         if (range != u_Range) {
            shader.getUniform("Range").set(range);
            u_Range = range;
         }

         if (0.0F != u_Thickness) {
            shader.getUniform("Thickness").set(0.0F);
            u_Thickness = 0.0F;
         }

         if (0.5F != u_Smoothness) {
            shader.getUniform("Smoothness").set(0.5F);
            u_Smoothness = 0.5F;
         }

         if (u_Outline != 0) {
            shader.getUniform("Outline").set(0);
            u_Outline = 0;
         }

         if (u_EnableFadeout != 0) {
            shader.getUniform("EnableFadeout").set(0);
            u_EnableFadeout = 0;
         }
      }
   }

   private float getBaselineY(float y) {
      return y + this.msdfFont.getMetrics().baselineHeight() * this.pixelSize - 2.25F;
   }

   private void endMsdfDraw(BufferBuilder builder) {
      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
   }

   private void fallbackRender(MatrixStack matrix, String text, float x, float y, int color) {
      MinecraftClient client = MinecraftClient.getInstance();
      TextRenderer tr = client.textRenderer;
      if (tr != null) {
         tr.draw(
            text,
            x,
            y,
            color,
            false,
            matrix.peek().getPositionMatrix(),
            client.getBufferBuilders().getEntityVertexConsumers(),
            TextLayerType.NORMAL,
            0,
            15728880
         );
      }
   }

   private int interpolateColor(int colorStart, int colorEnd, float t) {
      int quantizedT = (int)(Math.max(0.0F, Math.min(1.0F, t)) * 512.0F);
      FontRenderer.ColorInterpKey key = new FontRenderer.ColorInterpKey(colorStart, colorEnd, quantizedT);
      Integer cached = COLOR_INTERP_CACHE.get(key);
      if (cached != null) {
         return cached;
      } else {
         float startAlpha = (colorStart >> 24 & 0xFF) / 255.0F;
         float startRed = (colorStart >> 16 & 0xFF) / 255.0F;
         float startGreen = (colorStart >> 8 & 0xFF) / 255.0F;
         float startBlue = (colorStart & 0xFF) / 255.0F;
         float endAlpha = (colorEnd >> 24 & 0xFF) / 255.0F;
         float endRed = (colorEnd >> 16 & 0xFF) / 255.0F;
         float endGreen = (colorEnd >> 8 & 0xFF) / 255.0F;
         float endBlue = (colorEnd & 0xFF) / 255.0F;
         float alpha = startAlpha + t * (endAlpha - startAlpha);
         float red = startRed + t * (endRed - startRed);
         float green = startGreen + t * (endGreen - startGreen);
         float blue = startBlue + t * (endBlue - startBlue);
         int result = (int)(alpha * 255.0F) << 24 | (int)(red * 255.0F) << 16 | (int)(green * 255.0F) << 8 | (int)(blue * 255.0F);
         if (COLOR_INTERP_CACHE.size() < 10000) {
            COLOR_INTERP_CACHE.put(key, result);
         }

         return result;
      }
   }

   private int normalizeBrightness(int color, float targetBrightness) {
      int alpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;
      float currentBrightness = (0.299F * red + 0.587F * green + 0.114F * blue) / 255.0F;
      if (currentBrightness < targetBrightness) {
         float scale = targetBrightness * 1.01F / Math.max(currentBrightness, 0.01F);
         red = Math.min(255, (int)Math.ceil(red * scale));
         green = Math.min(255, (int)Math.ceil(green * scale));
         blue = Math.min(255, (int)Math.ceil(blue * scale));
      }

      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private int[] sanitizeKeyframeColors(int[] keyframeColors) {
      return keyframeColors != null && keyframeColors.length != 0 ? keyframeColors : new int[]{-1};
   }

   private int[] normalizePaletteBrightness(int[] keyframeColors, float minBrightness) {
      int[] result = new int[keyframeColors.length];

      for (int i = 0; i < keyframeColors.length; i++) {
         result[i] = this.normalizeBrightness(keyframeColors[i], minBrightness);
      }

      return result;
   }

   private double computeWaveProgress(double currentTimeSec, double durationSec, double delayPerCharSec, int charIndex) {
      double shiftedTime = currentTimeSec - charIndex * delayPerCharSec;
      double cyclePos = shiftedTime / durationSec;
      double progress = cyclePos - Math.floor(cyclePos);
      return progress < 0.0 ? progress + 1.0 : progress;
   }

   private int sampleKeyframeColor(int[] keyframeColors, double progress) {
      if (keyframeColors.length == 1) {
         return keyframeColors[0];
      } else {
         double clamped = Math.max(0.0, Math.min(1.0, progress));
         int segmentCount = keyframeColors.length - 1;
         double scaled = clamped * segmentCount;
         int segment = (int)Math.floor(scaled);
         if (segment >= segmentCount) {
            return keyframeColors[segmentCount];
         } else {
            float localT = (float)(scaled - segment);
            float easedT = this.easeInOut(localT);
            return this.interpolateColor(keyframeColors[segment], keyframeColors[segment + 1], easedT);
         }
      }
   }

   private float easeInOut(float t) {
      float clamped = Math.max(0.0F, Math.min(1.0F, t));
      return (float)(0.5 - 0.5 * Math.cos(Math.PI * clamped));
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private int countWaveSymbols(String text) {
      if (text != null && !text.isEmpty()) {
         int count = 0;

         for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') {
               count++;
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   public MsdfFont getMsdfFont() {
      return this.msdfFont;
   }

   public float getPixelSize() {
      return this.pixelSize;
   }

   static {
      for (int i = 0; i < 16; i++) {
         STRING_BUILDER_POOL.offer(new StringBuilder(256));
      }

      COLOR_INTERP_CACHE = new HashMap<>();
      BAKED_CACHE = new LinkedHashMap<FontRenderer.BakedKey, FontRenderer.BakedString>(341, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<FontRenderer.BakedKey, FontRenderer.BakedString> eldest) {
            return this.size() > 256;
         }
      };
      ACTIVE_BATCH = null;
      IDENTITY_MAT = new Matrix4f();
      BATCH_SCRATCH = new Vector3f();
      lastShaderId = -1;
      u_Range = Float.NaN;
      u_Thickness = Float.NaN;
      u_Smoothness = Float.NaN;
      u_Outline = -1;
      u_EnableFadeout = -1;
      u_FadeoutStart = Float.NaN;
      u_FadeoutEnd = Float.NaN;
      u_MaxWidth = Float.NaN;
      u_TextPosX = Float.NaN;
   }

   private record BakedKey(String text, int pixelSizeQ, int fontId, int defaultColor, boolean legacyApply) {
   }

   private static final class BakedString {
      final float[] qx;
      final float[] qy;
      final float[] qw;
      final float[] qh;
      final float[] umin;
      final float[] umax;
      final float[] vmin;
      final float[] vmax;
      final int[] col;
      final int n;

      BakedString(int n) {
         this.n = n;
         this.qx = new float[n];
         this.qy = new float[n];
         this.qw = new float[n];
         this.qh = new float[n];
         this.umin = new float[n];
         this.umax = new float[n];
         this.vmin = new float[n];
         this.vmax = new float[n];
         this.col = new int[n];
      }
   }

   private record ColorInterpKey(int colorStart, int colorEnd, int quantizedT) {
   }

   @FunctionalInterface
   private interface ColorPerChar {
      int colorAt(int var1, char var2);
   }

   @FunctionalInterface
   private interface ColorPerCharWithCursor {
      int colorAt(int var1, char var2, float var3, int var4);
   }

   public static final class FontBatch implements AutoCloseable {
      private static final int MAX_QUADS = 2048;
      private static final int MAX_FONTS = 16;
      private static final FontRenderer.FontBatch INSTANCE = new FontRenderer.FontBatch();
      private final float[] positions = new float[16384];
      private final float[] uvs = new float[16384];
      private final int[] colors = new int[2048];
      private int quadCount = 0;
      private final FontRenderer[] uniqueFonts = new FontRenderer[16];
      private int uniqueFontCount = 0;
      private final int[][] fontBuckets = new int[16][2048];
      private final int[] fontBucketCounts = new int[16];
      private boolean closed = false;

      private FontBatch() {
      }

      public static FontRenderer.FontBatch begin(FontRenderer font) {
         if (FontRenderer.ACTIVE_BATCH != null) {
            FontRenderer.ACTIVE_BATCH.end();
         }

         INSTANCE.quadCount = 0;
         INSTANCE.uniqueFontCount = 0;

         for (int i = 0; i < 16; i++) {
            INSTANCE.fontBucketCounts[i] = 0;
         }

         INSTANCE.closed = false;
         INSTANCE.uniqueFonts[0] = font;
         INSTANCE.uniqueFontCount = 1;
         FontRenderer.ACTIVE_BATCH = INSTANCE;
         return INSTANCE;
      }

      private int fontIndex(FontRenderer font) {
         for (int i = 0; i < this.uniqueFontCount; i++) {
            if (this.uniqueFonts[i] == font) {
               return i;
            }
         }

         if (this.uniqueFontCount >= 16) {
            return -1;
         } else {
            this.uniqueFonts[this.uniqueFontCount] = font;
            return this.uniqueFontCount++;
         }
      }

      void submitQuad(
         FontRenderer font,
         float x0,
         float y0,
         float u0,
         float v0,
         float x1,
         float y1,
         float u1,
         float v1,
         float x2,
         float y2,
         float u2,
         float v2,
         float x3,
         float y3,
         float u3,
         float v3,
         int color
      ) {
         if (this.quadCount >= 2048) {
            this.flush();
         }

         int fontIdx = this.fontIndex(font);
         if (fontIdx >= 0) {
            int p = this.quadCount * 8;
            this.positions[p + 0] = x0;
            this.positions[p + 1] = y0;
            this.positions[p + 2] = x1;
            this.positions[p + 3] = y1;
            this.positions[p + 4] = x2;
            this.positions[p + 5] = y2;
            this.positions[p + 6] = x3;
            this.positions[p + 7] = y3;
            this.uvs[p + 0] = u0;
            this.uvs[p + 1] = v0;
            this.uvs[p + 2] = u1;
            this.uvs[p + 3] = v1;
            this.uvs[p + 4] = u2;
            this.uvs[p + 5] = v2;
            this.uvs[p + 6] = u3;
            this.uvs[p + 7] = v3;
            this.colors[this.quadCount] = color;
            this.fontBuckets[fontIdx][this.fontBucketCounts[fontIdx]++] = this.quadCount++;
         }
      }

      public void flush() {
         if (this.quadCount != 0) {
            HudProfiler.begin(HudProfiler.Section.FONT_BATCH_FLUSH);

            try {
               RenderSystem.disableDepthTest();
               HudProfiler.countFontFlush(this.uniqueFontCount, this.quadCount);

               for (int fIdx = 0; fIdx < this.uniqueFontCount; fIdx++) {
                  int count = this.fontBucketCounts[fIdx];
                  if (count != 0) {
                     FontRenderer font = this.uniqueFonts[fIdx];
                     int[] bucket = this.fontBuckets[fIdx];
                     HudProfiler.begin(HudProfiler.Section.FONT_FLUSH_BEGIN);

                     try {
                        font.beginMsdfDraw(0.0F);
                     } catch (Exception var13) {
                        FontRenderer.msdfFailed = true;
                        HudProfiler.end(HudProfiler.Section.FONT_FLUSH_BEGIN);
                        break;
                     }

                     HudProfiler.end(HudProfiler.Section.FONT_FLUSH_BEGIN);
                     HudProfiler.begin(HudProfiler.Section.FONT_FLUSH_EMIT);
                     BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                     for (int n = 0; n < count; n++) {
                        int qIdx = bucket[n];
                        int p = qIdx * 8;
                        int color = this.colors[qIdx];
                        builder.vertex(FontRenderer.IDENTITY_MAT, this.positions[p + 0], this.positions[p + 1], 0.0F)
                           .texture(this.uvs[p + 0], this.uvs[p + 1])
                           .color(color);
                        builder.vertex(FontRenderer.IDENTITY_MAT, this.positions[p + 2], this.positions[p + 3], 0.0F)
                           .texture(this.uvs[p + 2], this.uvs[p + 3])
                           .color(color);
                        builder.vertex(FontRenderer.IDENTITY_MAT, this.positions[p + 4], this.positions[p + 5], 0.0F)
                           .texture(this.uvs[p + 4], this.uvs[p + 5])
                           .color(color);
                        builder.vertex(FontRenderer.IDENTITY_MAT, this.positions[p + 6], this.positions[p + 7], 0.0F)
                           .texture(this.uvs[p + 6], this.uvs[p + 7])
                           .color(color);
                     }

                     HudProfiler.end(HudProfiler.Section.FONT_FLUSH_EMIT);
                     HudProfiler.begin(HudProfiler.Section.FONT_FLUSH_DRAW);
                     BuiltBuffer built = builder.endNullable();
                     if (built != null) {
                        BufferRenderer.drawWithGlobalProgram(built);
                     }

                     HudProfiler.end(HudProfiler.Section.FONT_FLUSH_DRAW);
                  }
               }

               RenderSystem.setShaderTexture(0, 0);
               RenderSystem.enableCull();
               RenderSystem.disableBlend();
               RenderSystem.enableDepthTest();
               this.quadCount = 0;
               this.uniqueFontCount = 0;

               for (int i = 0; i < 16; i++) {
                  this.fontBucketCounts[i] = 0;
               }
            } finally {
               HudProfiler.end(HudProfiler.Section.FONT_BATCH_FLUSH);
            }
         }
      }

      @Override
      public void close() {
         this.end();
      }

      public void end() {
         if (!this.closed) {
            this.closed = true;
            if (FontRenderer.ACTIVE_BATCH == this) {
               FontRenderer.ACTIVE_BATCH = null;
            }

            this.flush();
         }
      }
   }
}
