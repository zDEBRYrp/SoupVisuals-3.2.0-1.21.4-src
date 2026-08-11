package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.implement.features.draggables.watermark.CPUComponent;
import padej.soup.implement.features.draggables.watermark.ClientIconComponent;
import padej.soup.implement.features.draggables.watermark.ClientNameComponent;
import padej.soup.implement.features.draggables.watermark.FPSComponent;
import padej.soup.implement.features.draggables.watermark.MemoryComponent;
import padej.soup.implement.features.draggables.watermark.PingComponent;
import padej.soup.implement.features.draggables.watermark.RoleComponent;
import padej.soup.implement.features.draggables.watermark.ServerIPComponent;
import padej.soup.implement.features.draggables.watermark.UserComponent;
import padej.soup.implement.features.draggables.watermark.WatermarkComponent;

public class Watermark extends AbstractDraggable {
   private final List<WatermarkComponent> components = new ArrayList<>();
   private List<List<Integer>> cachedMonoLines = null;
   private List<List<Integer>> cachedSplitLines = null;
   private List<Float> cachedWidths = null;
   private String cachedEnabledComponentsHash = "";
   private long lastCacheUpdate = 0L;
   private static final long CACHE_INVALIDATION_TIME = 100L;
   private List<WatermarkComponent> cachedEnabledComponents = new ArrayList<>();

   public Watermark() {
      super("Watermark", 10, 10, 92, 16, true);
      this.initializeComponents();
   }

   private void initializeComponents() {
      this.components.add(new ClientIconComponent());
      this.components.add(new ClientNameComponent());
      this.components.add(new RoleComponent());
      this.components.add(new UserComponent());
      this.components.add(new FPSComponent());
      this.components.add(new ServerIPComponent());
      this.components.add(new PingComponent());
      this.components.add(new MemoryComponent());
      this.components.add(new CPUComponent());
   }

   @Override
   public void tick() {
      this.cachedEnabledComponents.clear();

      for (WatermarkComponent component : this.components) {
         if (this.isComponentEnabled(component.getName())) {
            this.cachedEnabledComponents.add(component);
            component.tick();
         }
      }
   }

   private String generateCacheHash(List<WatermarkComponent> enabledComponents, List<Float> widths) {
      StringBuilder hash = new StringBuilder();

      for (int i = 0; i < enabledComponents.size(); i++) {
         hash.append(enabledComponents.get(i).getName());
         hash.append(":");
         hash.append(Math.round(widths.get(i) * 10.0F) / 10.0F);
         hash.append(";");
      }

      return hash.toString();
   }

   private boolean isCacheValid(String currentHash) {
      long currentTime = System.currentTimeMillis();
      return this.cachedEnabledComponentsHash.equals(currentHash) && currentTime - this.lastCacheUpdate < 100L && this.cachedWidths != null;
   }

   private void updateCache(String hash, List<Float> widths, List<List<Integer>> monoLines, List<List<Integer>> splitLines) {
      this.cachedEnabledComponentsHash = hash;
      this.cachedWidths = new ArrayList<>(widths);
      this.cachedMonoLines = monoLines;
      this.cachedSplitLines = splitLines;
      this.lastCacheUpdate = System.currentTimeMillis();
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      List<WatermarkComponent> enabledComponents = this.cachedEnabledComponents;
      if (!enabledComponents.isEmpty()) {
         boolean isSplitStyle = padej.soup.implement.features.modules.hud.Watermark.getInstance().style.isSelected("Split");
         if (isSplitStyle) {
            this.drawSplitStyle(matrix, font, enabledComponents);
         } else {
            this.drawMonoStyle(matrix, font, enabledComponents);
         }
      }
   }

   private void drawMonoStyle(MatrixStack matrix, FontRenderer font, List<WatermarkComponent> enabledComponents) {
      HudProfiler.begin(HudProfiler.Section.WM_LAYOUT);
      boolean hasIcon = enabledComponents.getFirst().getName().equals("Icon");
      float padding = 5.0F;
      float iconGap = 4.0F;
      float separatorSpacing = 10.0F;
      float lineGap = 4.0F;
      float lineHeight = 16.0F;
      float maxWidth = 300.0F;
      List<Float> componentWidths = new ArrayList<>();

      for (WatermarkComponent comp : enabledComponents) {
         componentWidths.add(comp.getSmoothedWidth(font, lineHeight));
      }

      String currentHash = this.generateCacheHash(enabledComponents, componentWidths);
      float totalWidth = 0.0F;

      for (int i = 0; i < enabledComponents.size(); i++) {
         boolean isIcon = i == 0 && hasIcon;
         float componentWidth = componentWidths.get(i);
         if (isIcon) {
            totalWidth += lineHeight + iconGap;
         } else {
            totalWidth += componentWidth + padding * 2.0F;
            if (i > (hasIcon ? 1 : 0)) {
               totalWidth += separatorSpacing;
            }
         }
      }

      List<List<Integer>> lines;
      if (totalWidth <= maxWidth) {
         lines = new ArrayList<>();
         List<Integer> singleLine = new ArrayList<>();

         for (int ix = 0; ix < enabledComponents.size(); ix++) {
            singleLine.add(ix);
         }

         lines.add(singleLine);
      } else if (this.isCacheValid(currentHash) && this.cachedMonoLines != null) {
         lines = this.cachedMonoLines;
      } else {
         lines = this.balanceLines(enabledComponents, componentWidths, hasIcon, maxWidth, padding, separatorSpacing, iconGap, lineHeight);
         this.updateCache(currentHash, componentWidths, lines, this.cachedSplitLines);
      }

      HudProfiler.end(HudProfiler.Section.WM_LAYOUT);
      HudProfiler.begin(HudProfiler.Section.WM_DRAW_COMPONENTS);

      try {
         float currentY = this.getY();
         float maxLineWidth = 0.0F;

         for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<Integer> line = lines.get(lineIndex);
            boolean isFirstLineFlag = lineIndex == 0;
            boolean hasIconInLine = isFirstLineFlag && hasIcon;
            float lineContentWidth = 0.0F;
            int nonIconCount = 0;

            for (int idx : line) {
               if (idx != 0 || !hasIconInLine) {
                  lineContentWidth += componentWidths.get(idx);
                  nonIconCount++;
               }
            }

            if (nonIconCount > 1) {
               lineContentWidth += separatorSpacing * (nonIconCount - 1);
            }

            float iconX = this.getX();
            float iconWidth = hasIconInLine ? lineHeight : 0.0F;
            float gapWidth = hasIconInLine ? iconGap : 0.0F;
            float backgroundX = iconX + iconWidth + gapWidth;
            if (lineContentWidth > 0.0F) {
               float actualWidth = lineContentWidth + padding * 2.0F;
               if (padej.soup.implement.features.modules.hud.Watermark.getInstance().getShowBackground().isValue()) {
                  blur.render(
                     ShapeProperties.create(matrix, backgroundX, currentY, actualWidth, lineHeight)
                        .round(3.0F)
                        .softness(1.0F)
                        .thickness(2.0F)
                        .outlineColor(ColorUtil.getOutline())
                        .color(ColorUtil.getBlurRect(0.7F))
                        .build()
                  );
               }

               maxLineWidth = Math.max(maxLineWidth, iconWidth + gapWidth + actualWidth);
            }

            float currentX = iconX;

            for (int ix = 0; ix < line.size(); ix++) {
               int componentIndex = line.get(ix);
               WatermarkComponent component = enabledComponents.get(componentIndex);
               if (ix == 1 && hasIconInLine) {
                  currentX = backgroundX + padding;
               } else if (ix == 0 && !hasIconInLine) {
                  currentX = backgroundX + padding;
               }

               long _t0 = HudProfiler.nano();
               component.render(matrix, currentX, currentY, lineHeight, font);
               HudProfiler.recordComponent(component.getName(), _t0);
               currentX += componentWidths.get(componentIndex);
               boolean isIconInList = ix == 0 && hasIconInLine;
               if (!isIconInList && ix < line.size() - 1) {
                  currentX += separatorSpacing;
               }
            }

            this.renderLineSeparators(matrix, line, enabledComponents, componentWidths, hasIconInLine, padding, separatorSpacing, currentY, lineHeight);
            currentY += lineHeight + lineGap;
         }

         int totalHeight = (int)(lines.size() * lineHeight + (lines.size() - 1) * lineGap);
         this.setWidth((int)maxLineWidth);
         this.setHeight(totalHeight);
      } finally {
         HudProfiler.end(HudProfiler.Section.WM_DRAW_COMPONENTS);
      }
   }

   private List<List<Integer>> balanceLines(
      List<WatermarkComponent> components,
      List<Float> widths,
      boolean hasIcon,
      float maxWidth,
      float padding,
      float separatorSpacing,
      float iconGap,
      float lineHeight
   ) {
      List<List<Integer>> fallbackLines = this.createFallbackLines(components, widths, hasIcon, maxWidth, padding, separatorSpacing, iconGap, lineHeight);
      boolean fallbackFits = true;

      for (List<Integer> line : fallbackLines) {
         float lineWidth = this.calculateLineWidth(line, widths, hasIcon, padding, separatorSpacing, iconGap, lineHeight);
         if (lineWidth > maxWidth) {
            fallbackFits = false;
            break;
         }
      }

      if (fallbackFits) {
         return fallbackLines;
      } else {
         int minLines = 2;
         int maxLines = Math.min(5, components.size());

         for (int numLines = minLines; numLines <= maxLines; numLines++) {
            List<List<Integer>> lines = this.tryBalanceWithLines(
               components, widths, hasIcon, maxWidth, padding, separatorSpacing, iconGap, lineHeight, numLines
            );
            if (lines != null) {
               boolean fits = true;

               for (List<Integer> linex : lines) {
                  float lineWidth = this.calculateLineWidth(linex, widths, hasIcon, padding, separatorSpacing, iconGap, lineHeight);
                  if (lineWidth > maxWidth) {
                     fits = false;
                     break;
                  }
               }

               if (fits) {
                  return lines;
               }
            }
         }

         return fallbackLines;
      }
   }

   private List<List<Integer>> tryBalanceWithLines(
      List<WatermarkComponent> components,
      List<Float> widths,
      boolean hasIcon,
      float maxWidth,
      float padding,
      float separatorSpacing,
      float iconGap,
      float lineHeight,
      int targetLines
   ) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;
      boolean isFirstLine = true;
      float totalWidth = 0.0F;

      for (int i = 0; i < components.size(); i++) {
         totalWidth += this.getComponentFullWidth(i, widths.get(i), hasIcon, padding, iconGap, lineHeight, i > (hasIcon ? 1 : 0));
      }

      float targetWidthPerLine = totalWidth / targetLines;

      for (int i = 0; i < components.size(); i++) {
         if (i == 0 && hasIcon) {
            boolean var23 = true;
         } else {
            boolean var10000 = false;
         }

         float componentWidth = widths.get(i);
         boolean needsSeparator = !currentLine.isEmpty() && (currentLine.size() != 1 || !isFirstLine || !hasIcon);
         float widthNeeded = this.getComponentFullWidth(i, componentWidth, hasIcon, padding, iconGap, lineHeight, needsSeparator);
         boolean shouldWrap = !currentLine.isEmpty()
            && lines.size() < targetLines - 1
            && Math.abs(currentLineWidth + widthNeeded - targetWidthPerLine) > Math.abs(currentLineWidth - targetWidthPerLine);
         if (shouldWrap) {
            lines.add(new ArrayList<>(currentLine));
            currentLine.clear();
            currentLineWidth = 0.0F;
            isFirstLine = false;
            widthNeeded = this.getComponentFullWidth(i, componentWidth, hasIcon, padding, iconGap, lineHeight, false);
         }

         currentLine.add(i);
         currentLineWidth += widthNeeded;
      }

      if (!currentLine.isEmpty()) {
         lines.add(currentLine);
      }

      return lines.size() == targetLines ? lines : null;
   }

   private float getComponentFullWidth(int index, float componentWidth, boolean hasIcon, float padding, float iconGap, float lineHeight, boolean needsSeparator) {
      boolean isIcon = index == 0 && hasIcon;
      if (isIcon) {
         return lineHeight + iconGap;
      } else {
         float width = componentWidth + padding * 2.0F;
         if (needsSeparator) {
            width += 10.0F;
         }

         return width;
      }
   }

   private float calculateLineWidth(
      List<Integer> line, List<Float> widths, boolean hasIcon, float padding, float separatorSpacing, float iconGap, float lineHeight
   ) {
      float width = 0.0F;
      boolean isFirstLine = line.contains(0);
      boolean hasIconInLine = isFirstLine && hasIcon;

      for (int i = 0; i < line.size(); i++) {
         int idx = line.get(i);
         boolean isIcon = idx == 0 && hasIconInLine;
         if (isIcon) {
            width += lineHeight + iconGap;
         } else {
            width += widths.get(idx) + padding * 2.0F;
            if (i > (hasIconInLine ? 1 : 0)) {
               width += separatorSpacing;
            }
         }
      }

      return width;
   }

   private List<List<Integer>> createFallbackLines(
      List<WatermarkComponent> components,
      List<Float> widths,
      boolean hasIcon,
      float maxWidth,
      float padding,
      float separatorSpacing,
      float iconGap,
      float lineHeight
   ) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;
      boolean isFirstLine = true;

      for (int i = 0; i < components.size(); i++) {
         if (i == 0 && hasIcon) {
            boolean var18 = true;
         } else {
            boolean var10000 = false;
         }

         float componentWidth = widths.get(i);
         boolean needsSeparator = !currentLine.isEmpty() && (currentLine.size() != 1 || !isFirstLine || !hasIcon);
         float widthNeeded = this.getComponentFullWidth(i, componentWidth, hasIcon, padding, iconGap, lineHeight, needsSeparator);
         if (!currentLine.isEmpty() && currentLineWidth + widthNeeded > maxWidth) {
            lines.add(new ArrayList<>(currentLine));
            currentLine.clear();
            currentLineWidth = 0.0F;
            isFirstLine = false;
            widthNeeded = this.getComponentFullWidth(i, componentWidth, hasIcon, padding, iconGap, lineHeight, false);
         }

         currentLine.add(i);
         currentLineWidth += widthNeeded;
      }

      if (!currentLine.isEmpty()) {
         lines.add(currentLine);
      }

      return lines;
   }

   private void renderLineSeparators(
      MatrixStack matrix,
      List<Integer> line,
      List<WatermarkComponent> allComponents,
      List<Float> componentWidths,
      boolean hasIcon,
      float padding,
      float separatorSpacing,
      float y,
      float lineHeight
   ) {
      float iconX = this.getX();
      float iconGap = 4.0F;
      float iconWidth = hasIcon ? lineHeight : 0.0F;
      float gapWidth = hasIcon ? iconGap : 0.0F;
      float backgroundX = iconX + iconWidth + gapWidth;
      float currentX = iconX;
      padej.soup.implement.features.modules.hud.Watermark watermark = padej.soup.implement.features.modules.hud.Watermark.getInstance();
      int separatorColor;
      if (!watermark.getColoredSeparators().isValue()) {
         separatorColor = ColorUtil.getOutline(0.75F, 0.5F);
      } else if (watermark.getSeparatorGradient().isValue()) {
         separatorColor = ColorUtil.fade(8);
      } else {
         separatorColor = ColorUtil.getClientColor();
      }

      for (int i = 0; i < line.size() - 1; i++) {
         int componentIndex = line.get(i);
         if (i == 0 && hasIcon) {
            currentX += componentWidths.get(componentIndex);
         } else {
            if (i == 1 && hasIcon) {
               currentX = backgroundX + padding;
            } else if (i == 0) {
               currentX = backgroundX + padding;
            }

            float width = componentWidths.get(componentIndex);
            currentX += width;
            float separatorX = currentX + separatorSpacing / 2.0F;
            rectangle.render(ShapeProperties.create(matrix, separatorX, y + 4.0F, 0.5, lineHeight - 8.0F).color(separatorColor).build());
            currentX += separatorSpacing;
         }
      }
   }

   private void drawSplitStyle(MatrixStack matrix, FontRenderer font, List<WatermarkComponent> enabledComponents) {
      HudProfiler.begin(HudProfiler.Section.WM_LAYOUT);
      float padding = 5.0F;
      float componentGap = 4.0F;
      float lineGap = 4.0F;
      float lineHeight = 16.0F;
      float maxWidth = 300.0F;
      List<Float> componentWidths = new ArrayList<>();

      for (WatermarkComponent comp : enabledComponents) {
         componentWidths.add(comp.getSmoothedWidth(font, lineHeight));
      }

      String currentHash = this.generateCacheHash(enabledComponents, componentWidths);
      float totalWidth = 0.0F;

      for (int i = 0; i < enabledComponents.size(); i++) {
         float componentWidth = componentWidths.get(i);
         boolean isIcon = enabledComponents.get(i).getName().equals("Icon");
         float fullWidth = isIcon ? componentWidth : componentWidth + padding * 2.0F;
         totalWidth += fullWidth;
         if (i > 0) {
            totalWidth += componentGap;
         }
      }

      List<List<Integer>> lines;
      if (totalWidth <= maxWidth) {
         lines = new ArrayList<>();
         List<Integer> singleLine = new ArrayList<>();

         for (int ix = 0; ix < enabledComponents.size(); ix++) {
            singleLine.add(ix);
         }

         lines.add(singleLine);
      } else if (this.isCacheValid(currentHash) && this.cachedSplitLines != null) {
         lines = this.cachedSplitLines;
      } else {
         lines = this.balanceSplitLines(enabledComponents, componentWidths, maxWidth, padding, componentGap);
         this.updateCache(currentHash, componentWidths, this.cachedMonoLines, lines);
      }

      HudProfiler.end(HudProfiler.Section.WM_LAYOUT);
      HudProfiler.begin(HudProfiler.Section.WM_DRAW_COMPONENTS);

      try {
         float currentY = this.getY();
         float maxLineWidth = 0.0F;

         for (List<Integer> line : lines) {
            float currentX = this.getX();
            float lineWidth = 0.0F;

            for (int ix = 0; ix < line.size(); ix++) {
               int componentIndex = line.get(ix);
               WatermarkComponent component = enabledComponents.get(componentIndex);
               float componentWidth = componentWidths.get(componentIndex);
               if (!component.getName().equals("Icon") && padej.soup.implement.features.modules.hud.Watermark.getInstance().getShowBackground().isValue()) {
                  blur.render(
                     ShapeProperties.create(matrix, currentX, currentY, componentWidth + padding * 2.0F, lineHeight)
                        .round(3.0F)
                        .softness(1.0F)
                        .thickness(2.0F)
                        .outlineColor(ColorUtil.getOutline())
                        .color(ColorUtil.getBlurRect(0.7F))
                        .build()
                  );
               }

               float renderX = component.getName().equals("Icon") ? currentX : currentX + padding;
               long _t0 = HudProfiler.nano();
               component.render(matrix, renderX, currentY, lineHeight, font);
               HudProfiler.recordComponent(component.getName(), _t0);
               float fullComponentWidth = component.getName().equals("Icon") ? componentWidth : componentWidth + padding * 2.0F;
               currentX += fullComponentWidth;
               lineWidth += fullComponentWidth;
               if (ix < line.size() - 1) {
                  currentX += componentGap;
                  lineWidth += componentGap;
               }
            }

            maxLineWidth = Math.max(maxLineWidth, lineWidth);
            currentY += lineHeight + lineGap;
         }

         int totalHeight = (int)(lines.size() * lineHeight + (lines.size() - 1) * lineGap);
         this.setWidth((int)maxLineWidth);
         this.setHeight(totalHeight);
      } finally {
         HudProfiler.end(HudProfiler.Section.WM_DRAW_COMPONENTS);
      }
   }

   private List<List<Integer>> balanceSplitLines(List<WatermarkComponent> components, List<Float> widths, float maxWidth, float padding, float componentGap) {
      List<List<Integer>> fallbackLines = this.createFallbackSplitLines(components, widths, maxWidth, padding, componentGap);
      boolean fallbackFits = true;

      for (List<Integer> line : fallbackLines) {
         float lineWidth = this.calculateSplitLineWidth(line, widths, components, padding, componentGap);
         if (lineWidth > maxWidth) {
            fallbackFits = false;
            break;
         }
      }

      if (fallbackFits) {
         return fallbackLines;
      } else {
         int minLines = 2;
         int maxLines = Math.min(5, components.size());

         for (int numLines = minLines; numLines <= maxLines; numLines++) {
            List<List<Integer>> lines = this.tryBalanceSplitLines(components, widths, maxWidth, padding, componentGap, numLines);
            if (lines != null) {
               boolean fits = true;

               for (List<Integer> linex : lines) {
                  float lineWidth = this.calculateSplitLineWidth(linex, widths, components, padding, componentGap);
                  if (lineWidth > maxWidth) {
                     fits = false;
                     break;
                  }
               }

               if (fits) {
                  return lines;
               }
            }
         }

         return fallbackLines;
      }
   }

   private List<List<Integer>> tryBalanceSplitLines(
      List<WatermarkComponent> components, List<Float> widths, float maxWidth, float padding, float componentGap, int targetLines
   ) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;
      float totalWidth = 0.0F;

      for (int i = 0; i < components.size(); i++) {
         boolean isIcon = components.get(i).getName().equals("Icon");
         float fullWidth = isIcon ? widths.get(i) : widths.get(i) + padding * 2.0F;
         totalWidth += fullWidth;
         if (i > 0) {
            totalWidth += componentGap;
         }
      }

      float targetWidthPerLine = totalWidth / targetLines;

      for (int ix = 0; ix < components.size(); ix++) {
         boolean isIcon = components.get(ix).getName().equals("Icon");
         float fullWidth = isIcon ? widths.get(ix) : widths.get(ix) + padding * 2.0F;
         float widthToAdd = fullWidth + (currentLine.isEmpty() ? 0.0F : componentGap);
         boolean shouldWrap = !currentLine.isEmpty()
            && lines.size() < targetLines - 1
            && Math.abs(currentLineWidth + widthToAdd - targetWidthPerLine) > Math.abs(currentLineWidth - targetWidthPerLine);
         if (shouldWrap) {
            lines.add(new ArrayList<>(currentLine));
            currentLine.clear();
            currentLineWidth = 0.0F;
            widthToAdd = fullWidth;
         }

         currentLine.add(ix);
         currentLineWidth += widthToAdd;
      }

      if (!currentLine.isEmpty()) {
         lines.add(currentLine);
      }

      return lines.size() == targetLines ? lines : null;
   }

   private float calculateSplitLineWidth(List<Integer> line, List<Float> widths, List<WatermarkComponent> components, float padding, float componentGap) {
      float width = 0.0F;

      for (int i = 0; i < line.size(); i++) {
         int idx = line.get(i);
         boolean isIcon = components.get(idx).getName().equals("Icon");
         float fullWidth = isIcon ? widths.get(idx) : widths.get(idx) + padding * 2.0F;
         width += fullWidth;
         if (i > 0) {
            width += componentGap;
         }
      }

      return width;
   }

   private List<List<Integer>> createFallbackSplitLines(
      List<WatermarkComponent> components, List<Float> widths, float maxWidth, float padding, float componentGap
   ) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;

      for (int i = 0; i < components.size(); i++) {
         boolean isIcon = components.get(i).getName().equals("Icon");
         float fullWidth = isIcon ? widths.get(i) : widths.get(i) + padding * 2.0F;
         float widthToAdd = fullWidth + (currentLine.isEmpty() ? 0.0F : componentGap);
         if (!currentLine.isEmpty() && currentLineWidth + widthToAdd > maxWidth) {
            lines.add(new ArrayList<>(currentLine));
            currentLine.clear();
            currentLineWidth = 0.0F;
            widthToAdd = fullWidth;
         }

         currentLine.add(i);
         currentLineWidth += widthToAdd;
      }

      if (!currentLine.isEmpty()) {
         lines.add(currentLine);
      }

      return lines;
   }

   private boolean isComponentEnabled(String componentName) {
      return padej.soup.implement.features.modules.hud.Watermark.getInstance().watermarkComponents.isSelected(componentName);
   }
}
