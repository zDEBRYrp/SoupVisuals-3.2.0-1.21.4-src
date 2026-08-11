package padej.soup.core.perftest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class HudProfiler {
   public static volatile boolean ENABLED = false;
   private static final int N = HudProfiler.Section.values().length;
   private static final long[] activeStart = new long[N];
   private static final long[] frameAccumNs = new long[N];
   private static final long[] totalNs = new long[N];
   private static final long[] maxFrameNs = new long[N];
   private static final long[] sumFrameNs = new long[N];
   private static final long[] frameCount = new long[N];
   private static final Map<String, HudProfiler.DraggableStats> drStats = new HashMap<>();
   private static final Map<String, HudProfiler.DraggableStats> compStats = new HashMap<>();
   private static long framesSeen = 0L;
   private static long totalFontFlushes = 0L;
   private static long totalFontSegments = 0L;
   private static long totalFontQuads = 0L;
   private static long totalRectFlushes = 0L;
   private static long totalRectQuads = 0L;
   private static long totalBlurImmediate = 0L;

   public static void countFontFlush(int segments, int quads) {
      if (ENABLED) {
         totalFontFlushes++;
         totalFontSegments += segments;
         totalFontQuads += quads;
      }
   }

   public static void countRectFlush(int quads) {
      if (ENABLED) {
         totalRectFlushes++;
         totalRectQuads += quads;
      }
   }

   public static void countBlurImmediate() {
      if (ENABLED) {
         totalBlurImmediate++;
      }
   }

   private HudProfiler() {
   }

   public static void begin(HudProfiler.Section s) {
      if (ENABLED) {
         activeStart[s.ordinal()] = System.nanoTime();
      }
   }

   public static void end(HudProfiler.Section s) {
      if (ENABLED) {
         long dur = System.nanoTime() - activeStart[s.ordinal()];
         frameAccumNs[s.ordinal()] += dur;
         totalNs[s.ordinal()] += dur;
      }
   }

   public static long nano() {
      return ENABLED ? System.nanoTime() : 0L;
   }

   public static void recordDraggable(String name, long startNano) {
      if (ENABLED && startNano != 0L) {
         long dur = System.nanoTime() - startNano;
         HudProfiler.DraggableStats st = drStats.get(name);
         if (st == null) {
            st = new HudProfiler.DraggableStats();
            drStats.put(name, st);
         }

         st.totalNs += dur;
         st.calls++;
         if (dur > st.maxNs) {
            st.maxNs = dur;
         }
      }
   }

   public static void recordComponent(String name, long startNano) {
      if (ENABLED && startNano != 0L) {
         long dur = System.nanoTime() - startNano;
         HudProfiler.DraggableStats st = compStats.get(name);
         if (st == null) {
            st = new HudProfiler.DraggableStats();
            compStats.put(name, st);
         }

         st.totalNs += dur;
         st.calls++;
         if (dur > st.maxNs) {
            st.maxNs = dur;
         }
      }
   }

   public static void frameStart() {
      if (ENABLED) {
         Arrays.fill(frameAccumNs, 0L);
      }
   }

   public static void frameEnd() {
      if (ENABLED) {
         framesSeen++;

         for (int i = 0; i < N; i++) {
            long v = frameAccumNs[i];
            if (v > 0L) {
               sumFrameNs[i] = sumFrameNs[i] + v;
               frameCount[i]++;
               if (v > maxFrameNs[i]) {
                  maxFrameNs[i] = v;
               }
            }
         }
      }
   }

   public static void reset() {
      Arrays.fill(activeStart, 0L);
      Arrays.fill(frameAccumNs, 0L);
      Arrays.fill(totalNs, 0L);
      Arrays.fill(maxFrameNs, 0L);
      Arrays.fill(sumFrameNs, 0L);
      Arrays.fill(frameCount, 0L);
      drStats.clear();
      compStats.clear();
      framesSeen = 0L;
      totalFontFlushes = 0L;
      totalFontSegments = 0L;
      totalFontQuads = 0L;
      totalRectFlushes = 0L;
      totalRectQuads = 0L;
      totalBlurImmediate = 0L;
   }

   public static HudProfiler.Snapshot snapshot() {
      HudProfiler.Snapshot snap = new HudProfiler.Snapshot();
      snap.framesSeen = framesSeen;
      snap.totalFontFlushes = totalFontFlushes;
      snap.totalFontSegments = totalFontSegments;
      snap.totalFontQuads = totalFontQuads;
      snap.totalRectFlushes = totalRectFlushes;
      snap.totalRectQuads = totalRectQuads;
      snap.totalBlurImmediate = totalBlurImmediate;
      snap.sections = new ArrayList<>(N);

      for (HudProfiler.Section s : HudProfiler.Section.values()) {
         int i = s.ordinal();
         HudProfiler.SectionRow row = new HudProfiler.SectionRow();
         row.name = s.name();
         row.totalMs = totalNs[i] / 1000000.0;
         row.maxFrameMs = maxFrameNs[i] / 1000000.0;
         long fc = frameCount[i];
         row.avgFrameMs = fc > 0L ? (double)sumFrameNs[i] / fc / 1000000.0 : 0.0;
         row.frameCount = fc;
         snap.sections.add(row);
      }

      snap.draggables = new ArrayList<>(drStats.size());

      for (Entry<String, HudProfiler.DraggableStats> e : drStats.entrySet()) {
         HudProfiler.DraggableStats st = e.getValue();
         HudProfiler.DraggableRow row = new HudProfiler.DraggableRow();
         row.name = e.getKey();
         row.totalMs = st.totalNs / 1000000.0;
         row.calls = st.calls;
         row.avgMs = st.calls > 0L ? (double)st.totalNs / st.calls / 1000000.0 : 0.0;
         row.maxMs = st.maxNs / 1000000.0;
         snap.draggables.add(row);
      }

      snap.draggables.sort(Comparator.<HudProfiler.DraggableRow>comparingDouble(r -> r.totalMs).reversed());
      snap.components = new ArrayList<>(compStats.size());

      for (Entry<String, HudProfiler.DraggableStats> e : compStats.entrySet()) {
         HudProfiler.DraggableStats st = e.getValue();
         HudProfiler.DraggableRow row = new HudProfiler.DraggableRow();
         row.name = e.getKey();
         row.totalMs = st.totalNs / 1000000.0;
         row.calls = st.calls;
         row.avgMs = st.calls > 0L ? (double)st.totalNs / st.calls / 1000000.0 : 0.0;
         row.maxMs = st.maxNs / 1000000.0;
         snap.components.add(row);
      }

      snap.components.sort(Comparator.<HudProfiler.DraggableRow>comparingDouble(r -> r.totalMs).reversed());
      return snap;
   }

   public static String formatReport(HudProfiler.Snapshot snap) {
      StringBuilder sb = new StringBuilder(1024);
      sb.append("=== HUD Profiler ===\n");
      sb.append("frames sampled: ").append(snap.framesSeen).append('\n');
      sb.append("\n[Sections] avg/frame · max/frame · total\n");

      for (HudProfiler.SectionRow row : snap.sections) {
         sb.append(
            String.format("  %-26s  avg=%.3fms  max=%.3fms  total=%.1fms  frames=%d\n", row.name, row.avgFrameMs, row.maxFrameMs, row.totalMs, row.frameCount)
         );
      }

      long frames = snap.framesSeen > 0L ? snap.framesSeen : 1L;
      sb.append("\n[Counters] per-frame avg / total\n");
      sb.append(String.format("  FontFlushes      avg=%.2f/frame  total=%d\n", (double)snap.totalFontFlushes / frames, snap.totalFontFlushes));
      sb.append(
         String.format(
            "  FontSegments     avg=%.2f/frame  total=%d  (avg %.2f segs/flush)\n",
            (double)snap.totalFontSegments / frames,
            snap.totalFontSegments,
            snap.totalFontFlushes > 0L ? (double)snap.totalFontSegments / snap.totalFontFlushes : 0.0
         )
      );
      sb.append(
         String.format(
            "  FontQuads        avg=%.1f/frame  total=%d  (avg %.1f quads/flush)\n",
            (double)snap.totalFontQuads / frames,
            snap.totalFontQuads,
            snap.totalFontFlushes > 0L ? (double)snap.totalFontQuads / snap.totalFontFlushes : 0.0
         )
      );
      sb.append(String.format("  RectFlushes      avg=%.2f/frame  total=%d\n", (double)snap.totalRectFlushes / frames, snap.totalRectFlushes));
      sb.append(String.format("  RectQuads        avg=%.1f/frame  total=%d\n", (double)snap.totalRectQuads / frames, snap.totalRectQuads));
      sb.append(String.format("  BlurImmediate    avg=%.2f/frame  total=%d\n", (double)snap.totalBlurImmediate / frames, snap.totalBlurImmediate));
      sb.append("\n[Draggables] sorted by total\n");

      for (HudProfiler.DraggableRow row : snap.draggables) {
         sb.append(String.format("  %-22s  total=%.1fms  avg=%.3fms  max=%.3fms  calls=%d\n", row.name, row.totalMs, row.avgMs, row.maxMs, row.calls));
      }

      if (snap.components != null && !snap.components.isEmpty()) {
         sb.append("\n[Components] sorted by total\n");

         for (HudProfiler.DraggableRow row : snap.components) {
            sb.append(String.format("  %-22s  total=%.1fms  avg=%.3fms  max=%.3fms  calls=%d\n", row.name, row.totalMs, row.avgMs, row.maxMs, row.calls));
         }
      }

      return sb.toString();
   }

   public static final class DraggableRow {
      public String name;
      public double totalMs;
      public double avgMs;
      public double maxMs;
      public long calls;
   }

   private static final class DraggableStats {
      long totalNs;
      long maxNs;
      long calls;
   }

   public static enum Section {
      FRAME_TOTAL,
      PHASE1_COLLECT_REGIONS,
      PHASE2_BLUR_SETUP,
      PHASE3A_BLUR_BACKGROUNDS,
      PHASE3B_DRAGGABLES,
      BLUR_RENDER_IMMEDIATE,
      BLUR_BATCH_FLUSH,
      FONT_BATCH_FLUSH,
      RECT_BATCH_FLUSH,
      FONT_FLUSH_BEGIN,
      FONT_FLUSH_EMIT,
      FONT_FLUSH_DRAW,
      WM_LAYOUT,
      WM_DRAW_COMPONENTS;
   }

   public static final class SectionRow {
      public String name;
      public double totalMs;
      public double avgFrameMs;
      public double maxFrameMs;
      public long frameCount;
   }

   public static final class Snapshot {
      public long framesSeen;
      public long totalFontFlushes;
      public long totalFontSegments;
      public long totalFontQuads;
      public long totalRectFlushes;
      public long totalRectQuads;
      public long totalBlurImmediate;
      public List<HudProfiler.SectionRow> sections;
      public List<HudProfiler.DraggableRow> draggables;
      public List<HudProfiler.DraggableRow> components;
   }
}
