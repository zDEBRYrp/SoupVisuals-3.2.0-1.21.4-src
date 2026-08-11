package padej.soup.api.system.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.implement.ArcBatch;
import padej.soup.api.system.shape.implement.BlurBatch;
import padej.soup.api.system.shape.implement.RectangleBatch;
import padej.soup.base.util.math.MathUtil;

public final class HudRenderPipeline {
   private static final int LAYER_COUNT = HudRenderPipeline.VanillaLayer.values().length;
   private static final HudRenderPipeline INSTANCE = new HudRenderPipeline();
   private final List<HudRenderPipeline.VanillaEntry>[] vanillaSlots = new List[LAYER_COUNT];
   private final List<HudRenderPipeline.VanillaEntry> outlineSlot = new ArrayList<>(4);
   private boolean active = false;
   private FontRenderer hintFont = null;

   public static HudRenderPipeline getInstance() {
      return INSTANCE;
   }

   private HudRenderPipeline() {
      for (int i = 0; i < LAYER_COUNT; i++) {
         this.vanillaSlots[i] = new ArrayList<>(16);
      }
   }

   public void beginFrame(FontRenderer hintFont) {
      this.hintFont = hintFont;
      this.active = true;

      for (int i = 0; i < LAYER_COUNT; i++) {
         this.vanillaSlots[i].clear();
      }

      this.outlineSlot.clear();
      BlurBatch.begin();
      RectangleBatch.begin();
      ArcBatch.begin();
      if (hintFont != null) {
         FontRenderer.FontBatch.begin(hintFont);
      }
   }

   public void endFrame() {
      if (this.active) {
         this.active = false;
         RenderSystem.disableDepthTest();

         try {
            BlurBatch batch0 = BlurBatch.active();
            if (batch0 != null) {
               batch0.end();
            }

            this.flushVanilla(HudRenderPipeline.VanillaLayer.AFTER_BLUR);
            RectangleBatch batch1 = RectangleBatch.active();
            if (batch1 != null) {
               batch1.end();
            }

            this.flushVanilla(HudRenderPipeline.VanillaLayer.AFTER_RECT);
            this.flushOutlines();
            this.flushVanilla(HudRenderPipeline.VanillaLayer.AFTER_OUTLINE);
            FontRenderer.FontBatch batch3 = FontRenderer.activeBatch();
            if (batch3 != null) {
               batch3.end();
            }

            this.flushVanilla(HudRenderPipeline.VanillaLayer.AFTER_FONT);
            ArcBatch batch4 = ArcBatch.active();
            if (batch4 != null) {
               batch4.end();
            }

            this.flushVanilla(HudRenderPipeline.VanillaLayer.AFTER_ARC);
         } finally {
            RenderSystem.enableDepthTest();
         }
      }
   }

   public void recordVanilla(Runnable action, HudRenderPipeline.VanillaLayer layer) {
      if (!this.active) {
         action.run();
      } else {
         float alpha = RenderSystem.getShaderColor()[3];
         this.vanillaSlots[layer.ordinal()].add(new HudRenderPipeline.VanillaEntry(action, alpha));
      }
   }

   public void recordOutline(Runnable action) {
      if (!this.active) {
         action.run();
      } else {
         float alpha = RenderSystem.getShaderColor()[3];
         this.outlineSlot.add(new HudRenderPipeline.VanillaEntry(action, alpha));
      }
   }

   public boolean isActive() {
      return this.active;
   }

   private void flushVanilla(HudRenderPipeline.VanillaLayer layer) {
      List<HudRenderPipeline.VanillaEntry> entries = this.vanillaSlots[layer.ordinal()];
      if (!entries.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();

         for (HudRenderPipeline.VanillaEntry entry : entries) {
            MathUtil.setAlpha(entry.alpha, entry.action);
         }

         RenderSystem.disableDepthTest();
      }
   }

   private void flushOutlines() {
      if (!this.outlineSlot.isEmpty()) {
         for (HudRenderPipeline.VanillaEntry entry : this.outlineSlot) {
            MathUtil.setAlpha(entry.alpha, entry.action);
         }
      }
   }

   private static final class VanillaEntry {
      final Runnable action;
      final float alpha;

      VanillaEntry(Runnable action, float alpha) {
         this.action = action;
         this.alpha = alpha;
      }
   }

   public static enum VanillaLayer {
      AFTER_BLUR,
      AFTER_RECT,
      AFTER_OUTLINE,
      AFTER_FONT,
      AFTER_ARC;
   }
}
