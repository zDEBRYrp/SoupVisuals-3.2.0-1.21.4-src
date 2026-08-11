package padej.soup.implement.features.draggables.watermark;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;

public class CPUComponent implements WatermarkComponent, QuickImports {
   private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
   private static final Runtime RUNTIME = Runtime.getRuntime();
   private double cpuLoad = 0.0;
   private int tickCounter = 0;
   private long lastCpuTime = 0L;
   private long lastSystemTime = 0L;
   private float smoothedWidth = 0.0F;
   private String cachedText = "0.0%";
   private String cachedWidestPattern = "8.8%";
   private double cachedForLoad = -1.0;

   private void refreshTextIfStale() {
      if (this.cpuLoad != this.cachedForLoad) {
         this.cachedText = String.format("%.1f%%", this.cpuLoad);
         int wholeDigits = (int)this.cpuLoad == 0 ? 1 : (int)Math.log10(Math.max(1, (int)this.cpuLoad)) + 1;
         this.cachedWidestPattern = "8".repeat(wholeDigits) + ".8%";
         this.cachedForLoad = this.cpuLoad;
      }
   }

   @Override
   public void tick() {
      this.tickCounter++;
      if (this.tickCounter >= 40) {
         this.tickCounter = 0;
         this.updateCpuLoad();
      }
   }

   private void updateCpuLoad() {
      try {
         long currentCpuTime = 0L;
         long[] threadIds = THREAD_BEAN.getAllThreadIds();
         int threadLimit = Math.min(threadIds.length, 20);

         for (int i = 0; i < threadLimit; i++) {
            long cpuTime = THREAD_BEAN.getThreadCpuTime(threadIds[i]);
            if (cpuTime > 0L) {
               currentCpuTime += cpuTime;
            }
         }

         long currentSystemTime = System.nanoTime();
         if (this.lastCpuTime == 0L || this.lastSystemTime == 0L) {
            this.lastCpuTime = currentCpuTime;
            this.lastSystemTime = currentSystemTime;
            return;
         }

         long cpuTimeDiff = currentCpuTime - this.lastCpuTime;
         long systemTimeDiff = currentSystemTime - this.lastSystemTime;
         if (systemTimeDiff > 0L) {
            int availableProcessors = RUNTIME.availableProcessors();
            double rawLoad = 100.0 * cpuTimeDiff / systemTimeDiff / availableProcessors;
            this.cpuLoad = this.cpuLoad * 0.7 + rawLoad * 0.3;
            this.cpuLoad = Math.max(0.0, Math.min(100.0, this.cpuLoad));
         }

         this.lastCpuTime = currentCpuTime;
         this.lastSystemTime = currentSystemTime;
      } catch (Exception var14) {
         LoggerUtil.error("Failed to updateCpuLoad: " + var14.getMessage());
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      this.refreshTextIfStale();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61444).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, this.cachedText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(this.cachedText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      this.refreshTextIfStale();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(this.cachedWidestPattern);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   @Override
   public String getName() {
      return "CPU";
   }

   public void setSmoothedWidth(float smoothedWidth) {
      this.smoothedWidth = smoothedWidth;
   }

   public float getSmoothedWidth() {
      return this.smoothedWidth;
   }
}
