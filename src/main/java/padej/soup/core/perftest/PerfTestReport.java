package padej.soup.core.perftest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.MinecraftClient;

public final class PerfTestReport {
   private PerfTestReport() {
   }

   public static File write(List<PerfTestSession.Sample> samples, long durationMs, String testName) throws IOException {
      return write(samples, durationMs, testName, null);
   }

   public static File write(List<PerfTestSession.Sample> samples, long durationMs, String testName, HudProfiler.Snapshot hudSnap) throws IOException {
      File runDir = MinecraftClient.getInstance().runDirectory;
      File dir = new File(runDir, "soupapi/temp");
      if (!dir.exists() && !dir.mkdirs()) {
         throw new IOException("Не удалось создать директорию " + dir.getAbsolutePath());
      } else {
         String filename = testName != null && !testName.isBlank()
            ? testName + ".html"
            : "perf_test_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";
         File out = new File(dir, filename);

         try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
            w.write(buildHtml(samples, durationMs, hudSnap));
         }

         return out;
      }
   }

   public static File write(List<PerfTestSession.Sample> samples, long durationMs) throws IOException {
      return write(samples, durationMs, null, null);
   }

   private static String buildHtml(List<PerfTestSession.Sample> samples, long durationMs) {
      return buildHtml(samples, durationMs, null);
   }

   private static String buildHtml(List<PerfTestSession.Sample> samples, long durationMs, HudProfiler.Snapshot hudSnap) {
      double avgFps = average(samples, s -> s.fps());
      double medFps = median(samples, s -> s.fps());
      double minFps = min(samples, s -> s.fps());
      double maxFps = max(samples, s -> s.fps());
      double avgMs = average(samples, PerfTestSession.Sample::frameMs);
      double medMs = median(samples, PerfTestSession.Sample::frameMs);
      double avgRam = average(samples, PerfTestSession.Sample::ramMB);
      double medRam = median(samples, PerfTestSession.Sample::ramMB);
      double minRam = min(samples, PerfTestSession.Sample::ramMB);
      double maxRam = max(samples, PerfTestSession.Sample::ramMB);
      double avgCpu = average(samples, PerfTestSession.Sample::cpu);
      double medCpu = median(samples, PerfTestSession.Sample::cpu);
      String labels = joinDouble(samples, s -> s.tMs() / 1000.0);
      String fpsArr = joinDouble(samples, s -> s.fps());
      String ramArr = joinDouble(samples, PerfTestSession.Sample::ramMB);
      String cpuArr = joinDouble(samples, PerfTestSession.Sample::cpu);
      String msArr = joinDouble(samples, PerfTestSession.Sample::frameMs);
      String llmSummary = buildLlmSummary(samples, durationMs);
      String llmSummaryEscaped = htmlEscape(llmSummary);
      String hudReport = hudSnap != null ? HudProfiler.formatReport(hudSnap) : "";
      String hudReportEscaped = htmlEscape(hudReport);
      return "<!doctype html>\n<html lang=\"ru\"><head><meta charset=\"utf-8\">\n<title>SoupAPI · perf-test</title>\n<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n<style>\n:root { --bg:#0f1115; --card:#1a1d24; --line:#2a2f3a; --fg:#e5e7eb; --mute:#9ca3af; }\n* { box-sizing: border-box; }\nbody { font-family: -apple-system, Segoe UI, system-ui, sans-serif; margin: 24px; background: var(--bg); color: var(--fg); }\nh1 { margin: 0 0 4px; font-size: 22px; }\n.meta { color: var(--mute); margin: 0 0 20px; font-size: 13px; }\n.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }\n.stat { background: var(--card); border: 1px solid var(--line); border-radius: 8px; padding: 14px 16px; }\n.stat .label { font-size: 11px; color: var(--mute); text-transform: uppercase; letter-spacing: 0.5px; }\n.stat .val { font-size: 22px; font-weight: 600; margin: 6px 0 2px; }\n.stat .sub { font-size: 12px; color: var(--mute); }\n.chart { background: var(--card); border: 1px solid var(--line); border-radius: 8px; padding: 14px; margin-bottom: 14px; height: 240px; }\n.llm { background: var(--card); border: 1px solid var(--line); border-radius: 8px; padding: 14px; margin: 24px 0 14px; }\n.llm-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }\n.llm-head h2 { margin:0; font-size:13px; font-weight:600; color: var(--mute); text-transform:uppercase; letter-spacing:0.5px; }\n.llm-head button { background:#2a2f3a; color:#e5e7eb; border:1px solid #3a4150; border-radius:5px; padding:6px 12px; cursor:pointer; font-size:12px; }\n.llm-head button:hover { background:#3a4150; }\n.llm pre { margin:0; padding:0; color:#cbd5e1; font: 12px/1.5 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; white-space:pre; overflow-x:auto; }\n</style></head><body>\n<h1>SoupAPI · perf-test</h1>\n<p class=\"meta\">Длительность "
         + fmt(durationMs / 1000.0, 1)
         + " сек · "
         + samples.size()
         + " семплов · "
         + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
         + "</p>\n<div class=\"stats\">\n"
         + statCard("FPS", fmt(avgFps, 1), "median " + fmt(medFps, 1) + " · min " + fmt(minFps, 0) + " · max " + fmt(maxFps, 0))
         + statCard("Frame", fmt(avgMs, 2) + " ms", "median " + fmt(medMs, 2) + " ms")
         + statCard("RAM (heap)", fmt(avgRam, 1) + " MB", "median " + fmt(medRam, 1) + " · min " + fmt(minRam, 0) + " · max " + fmt(maxRam, 0))
         + statCard("CPU (proc)", fmt(avgCpu, 1) + "%", "median " + fmt(medCpu, 1) + "%")
         + "</div>\n<div class=\"chart\"><canvas id=\"fpsCh\"></canvas></div>\n<div class=\"chart\"><canvas id=\"msCh\"></canvas></div>\n<div class=\"chart\"><canvas id=\"ramCh\"></canvas></div>\n<div class=\"chart\"><canvas id=\"cpuCh\"></canvas></div>\n"
         + (
            hudReport.isEmpty()
               ? ""
               : "<div class=\"llm\">\n  <div class=\"llm-head\">\n    <h2>HUD profiler · per-section + per-draggable</h2>\n    <button onclick=\"navigator.clipboard.writeText(document.getElementById('hudProf').textContent); this.textContent='Copied'; setTimeout(()=>this.textContent='Copy',1500);\">Copy</button>\n  </div>\n  <pre id=\"hudProf\">"
                  + hudReportEscaped
                  + "</pre>\n</div>\n"
         )
         + "<div class=\"llm\">\n  <div class=\"llm-head\">\n    <h2>LLM-summary · копировать целиком и скормить агенту</h2>\n    <button onclick=\"navigator.clipboard.writeText(document.getElementById('llmSum').textContent); this.textContent='Copied'; setTimeout(()=>this.textContent='Copy',1500);\">Copy</button>\n  </div>\n  <pre id=\"llmSum\">"
         + llmSummaryEscaped
         + "</pre>\n</div>\n<script>\nconst labels = "
         + labels
         + ";\nconst mkOpts = (title) => ({\n  responsive: true, maintainAspectRatio: false,\n  plugins: { legend: { display: false },\n             title:  { display: true, text: title, color: '#e5e7eb', font: { size: 13, weight: '500' } } },\n  scales: {\n    x: { ticks: { color: '#9ca3af', maxTicksLimit: 10 }, grid: { color: '#2a2f3a' },\n         title: { display: true, text: 'сек', color: '#9ca3af' } },\n    y: { ticks: { color: '#9ca3af' }, grid: { color: '#2a2f3a' }, beginAtZero: true }\n  }\n});\nconst mkDs = (label, data, color) => ({ label, data,\n  borderColor: color, backgroundColor: color + '22', borderWidth: 2,\n  tension: 0.25, pointRadius: 0, fill: true });\nnew Chart(document.getElementById('fpsCh'), { type: 'line', data: { labels, datasets: [mkDs('FPS',       "
         + fpsArr
         + ", '#34d399')] }, options: mkOpts('FPS') });\nnew Chart(document.getElementById('msCh'),  { type: 'line', data: { labels, datasets: [mkDs('Frame ms',  "
         + msArr
         + ", '#fbbf24')] }, options: mkOpts('Frame time (ms)') });\nnew Chart(document.getElementById('ramCh'), { type: 'line', data: { labels, datasets: [mkDs('RAM (MB)',  "
         + ramArr
         + ", '#60a5fa')] }, options: mkOpts('RAM heap (MB)') });\nnew Chart(document.getElementById('cpuCh'), { type: 'line', data: { labels, datasets: [mkDs('CPU (%)',   "
         + cpuArr
         + ", '#f87171')] }, options: mkOpts('CPU process (%)') });\n</script>\n</body></html>\n";
   }

   private static String statCard(String label, String value, String sub) {
      return "<div class=\"stat\"><div class=\"label\">" + label + "</div><div class=\"val\">" + value + "</div><div class=\"sub\">" + sub + "</div></div>\n";
   }

   private static String fmt(double v, int frac) {
      return String.format(Locale.ROOT, "%." + frac + "f", v);
   }

   private static String joinDouble(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> getter) {
      StringBuilder sb = new StringBuilder("[");

      for (int i = 0; i < samples.size(); i++) {
         if (i > 0) {
            sb.append(',');
         }

         sb.append(fmt(getter.applyAsDouble(samples.get(i)), 3));
      }

      sb.append(']');
      return sb.toString();
   }

   private static double average(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g) {
      return samples.stream().mapToDouble(g).average().orElse(0.0);
   }

   private static double median(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g) {
      double[] v = samples.stream().mapToDouble(g).sorted().toArray();
      if (v.length == 0) {
         return 0.0;
      } else {
         return v.length % 2 == 1 ? v[v.length / 2] : (v[v.length / 2 - 1] + v[v.length / 2]) / 2.0;
      }
   }

   private static double min(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g) {
      return samples.stream().mapToDouble(g).min().orElse(0.0);
   }

   private static double max(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g) {
      return samples.stream().mapToDouble(g).max().orElse(0.0);
   }

   private static double percentile(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g, double p) {
      double[] v = samples.stream().mapToDouble(g).sorted().toArray();
      if (v.length == 0) {
         return 0.0;
      } else {
         int idx = (int)Math.max(0L, Math.min((long)(v.length - 1), Math.round(p * (v.length - 1))));
         return v[idx];
      }
   }

   private static String buildLlmSummary(List<PerfTestSession.Sample> samples, long durationMs) {
      if (samples.isEmpty()) {
         return "perf-test: 0 samples";
      } else {
         StringBuilder sb = new StringBuilder();
         double durSec = durationMs / 1000.0;
         sb.append("perf-test mc=1.21.4 ");
         sb.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
         sb.append(" dur=").append(fmt(durSec, 0)).append("s");
         sb.append(" n=").append(samples.size()).append("\n");
         sb.append(metricLine("fps", samples, sx -> sx.fps(), 0));
         sb.append(metricLine("frame_ms", samples, PerfTestSession.Sample::frameMs, 2));
         sb.append(metricLine("ram_mb", samples, PerfTestSession.Sample::ramMB, 0));
         sb.append(metricLine("cpu_pct", samples, PerfTestSession.Sample::cpu, 1));
         double ramFirst = quartileAverage(samples, PerfTestSession.Sample::ramMB, 0);
         double ramLast = quartileAverage(samples, PerfTestSession.Sample::ramMB, 3);
         double ramDelta = ramLast - ramFirst;
         String ramTrend = Math.abs(ramDelta) < 5.0 ? "stable" : (ramDelta > 0.0 ? "growing" : "shrinking");
         sb.append("ram_trend=").append(ramTrend).append(" delta=").append(fmt(ramDelta, 1)).append("MB\n");
         int bins = Math.min(8, Math.max(4, samples.size() / 10));
         long binMs = durationMs / bins;
         sb.append("bins=").append(bins).append("x").append(fmt(binMs / 1000.0, 1)).append("s [fps,ram_mb,cpu_pct]:\n");

         for (int b = 0; b < bins; b++) {
            long from = b * binMs;
            long to = b == bins - 1 ? durationMs + 1L : (b + 1) * binMs;
            List<PerfTestSession.Sample> bucket = new ArrayList<>();

            for (PerfTestSession.Sample s : samples) {
               if (s.tMs() >= from && s.tMs() < to) {
                  bucket.add(s);
               }
            }

            if (!bucket.isEmpty()) {
               sb.append("  ").append(String.format(Locale.ROOT, "%4ds", from / 1000L));
               sb.append(": ");
               sb.append(fmt(average(bucket, sx -> sx.fps()), 0)).append(",");
               sb.append(fmt(average(bucket, PerfTestSession.Sample::ramMB), 0)).append(",");
               sb.append(fmt(average(bucket, PerfTestSession.Sample::cpu), 1));
               sb.append("\n");
            }
         }

         return sb.toString();
      }
   }

   private static String metricLine(String name, List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g, int frac) {
      return name
         + ": avg="
         + fmt(average(samples, g), frac)
         + " med="
         + fmt(median(samples, g), frac)
         + " min="
         + fmt(min(samples, g), frac)
         + " max="
         + fmt(max(samples, g), frac)
         + " p95="
         + fmt(percentile(samples, g, 0.95), frac)
         + " p99="
         + fmt(percentile(samples, g, 0.99), frac)
         + "\n";
   }

   private static double quartileAverage(List<PerfTestSession.Sample> samples, ToDoubleFunction<PerfTestSession.Sample> g, int quartile) {
      int n = samples.size();
      int from = quartile * n / 4;
      int to = (quartile + 1) * n / 4;
      if (to <= from) {
         return 0.0;
      } else {
         double sum = 0.0;

         for (int i = from; i < to; i++) {
            sum += g.applyAsDouble(samples.get(i));
         }

         return sum / (to - from);
      }
   }

   private static String htmlEscape(String s) {
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }
}
