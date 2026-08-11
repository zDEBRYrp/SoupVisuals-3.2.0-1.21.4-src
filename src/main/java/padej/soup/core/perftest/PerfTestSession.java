package padej.soup.core.perftest;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.listener.Listener;

public class PerfTestSession implements Listener {
   private static final long SAMPLE_INTERVAL_MS = 100L;
   private static final AtomicInteger REPORT_THREAD_COUNTER = new AtomicInteger();
   private static PerfTestSession active;
   private final long durationMs;
   private final long startedAtMs;
   private final String testName;
   private final List<PerfTestSession.Sample> samples;
   private long lastSampleMs = 0L;

   public static boolean isActive() {
      return active != null;
   }

   public static void start(long durationMs, String testName) {
      if (active == null) {
         active = new PerfTestSession(durationMs, testName);
         EventManager mgr = EventManager.getInstance();
         if (mgr != null) {
            mgr.register(active);
         }

         HudProfiler.reset();
         HudProfiler.ENABLED = true;
      }
   }

   public static void start(long durationMs) {
      start(durationMs, null);
   }

   private PerfTestSession(long durationMs, String testName) {
      this.durationMs = durationMs;
      this.startedAtMs = System.currentTimeMillis();
      this.testName = testName;
      int expected = (int)(durationMs / 100L) + 16;
      this.samples = new ArrayList<>(expected);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      long now = System.currentTimeMillis();
      long elapsed = now - this.startedAtMs;
      if (elapsed >= this.durationMs) {
         this.complete();
      } else if (now - this.lastSampleMs >= 100L) {
         this.lastSampleMs = now;
         this.sample(elapsed);
      }
   }

   private void sample(long elapsedMs) {
      MinecraftClient mc = MinecraftClient.getInstance();
      long heapUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      double ramMB = heapUsed / 1024.0 / 1024.0;
      double cpu = 0.0;

      try {
         if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean os) {
            double v = os.getProcessCpuLoad();
            if (v >= 0.0) {
               cpu = v * 100.0;
            }
         }
      } catch (Throwable var14) {
      }

      int fps = mc.getCurrentFps();
      double frameMs = fps > 0 ? 1000.0 / fps : 0.0;
      this.samples.add(new PerfTestSession.Sample(elapsedMs, fps, ramMB, cpu, frameMs));
   }

   private void complete() {
      EventManager mgr = EventManager.getInstance();
      if (mgr != null) {
         mgr.unregister(this);
      }

      active = null;
      List<PerfTestSession.Sample> snapshot = this.samples;
      long dur = this.durationMs;
      String name = this.testName;
      int sampleCount = snapshot.size();
      HudProfiler.ENABLED = false;
      HudProfiler.Snapshot hudSnap = HudProfiler.snapshot();
      postToChat("§e[perf] HUD profile:");

      for (String line : HudProfiler.formatReport(hudSnap).split("\n")) {
         if (!line.isBlank()) {
            postToChat("§7" + line);
         }
      }

      Thread t = new Thread(() -> {
         try {
            File out = PerfTestReport.write(snapshot, dur, name, hudSnap);
            postToChat("§a[perf] Готово · §e" + sampleCount + "§a семплов · §f" + out.getAbsolutePath());
         } catch (Throwable var7x) {
            LoggerUtil.error("PerfTest: не удалось записать отчёт", var7x);
            postToChat("§c[perf] Ошибка записи отчёта: " + var7x.getMessage());
         }
      }, "soup-perf-report-" + REPORT_THREAD_COUNTER.incrementAndGet());
      t.setDaemon(true);
      t.setPriority(4);
      t.start();
   }

   private static void postToChat(String text) {
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.execute(() -> {
         if (mc.player != null) {
            mc.player.sendMessage(Text.literal(text), false);
         }
      });
   }

   public record Sample(long tMs, int fps, double ramMB, double cpu, double frameMs) {
   }
}
