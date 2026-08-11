package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.features.draggables.playerinfo.BPSComponent;
import padej.soup.implement.features.draggables.playerinfo.PlayerInfoComponent;
import padej.soup.implement.features.draggables.playerinfo.TPSComponent;
import padej.soup.implement.features.draggables.playerinfo.XYZComponent;

public class PlayerInfo extends AbstractDraggable {
   private final List<PlayerInfoComponent> components = new ArrayList<>();
   private List<PlayerInfoComponent> cachedEnabledComponents = new ArrayList<>();
   private boolean cachedIsVertical = false;
   private boolean cachedIsSplitStyle = false;
   private boolean cachedShowBackground = false;
   private final Animation chatHideAnimation = new DecelerateAnimation().setMs(250).setValue(1.0);

   public PlayerInfo() {
      super("PlayerInfo", 3, 0, 92, 16, false);
      this.initializeComponents();
   }

   @Override
   protected float getInteractionScale() {
      return Math.min(super.getInteractionScale(), 1.0F);
   }

   private void initializeComponents() {
      this.components.add(new BPSComponent());
      this.components.add(new TPSComponent());
      this.components.add(new XYZComponent());
   }

   @Override
   public void tick() {
      this.cachedEnabledComponents.clear();

      for (PlayerInfoComponent component : this.components) {
         if (this.isComponentEnabled(component.getName())) {
            this.cachedEnabledComponents.add(component);
         }

         component.tick();
      }

      padej.soup.implement.features.modules.hud.PlayerInfo playerInfo = padej.soup.implement.features.modules.hud.PlayerInfo.getInstance();
      this.cachedIsVertical = playerInfo.position.isSelected("Vertical");
      this.cachedIsSplitStyle = playerInfo.style.isSelected("Split");
      this.cachedShowBackground = playerInfo.getShowBackground().isValue();
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      List<PlayerInfoComponent> enabledComponents = this.cachedEnabledComponents;
      if (!enabledComponents.isEmpty()) {
         boolean isVertical = this.cachedIsVertical;
         boolean isChatOpen = PlayerIntersectionUtil.isChat(mc.currentScreen);
         int offset = isChatOpen ? -13 : 0;
         if (isVertical) {
            this.chatHideAnimation.setDirection(isChatOpen ? Direction.BACKWARDS : Direction.FORWARDS);
            float alpha = this.chatHideAnimation.getOutput().floatValue();
            if (alpha < 0.01F) {
               return;
            }

            float lineHeight = 16.0F;
            boolean isSplitStyle = this.cachedIsSplitStyle;
            boolean hasBackground = this.cachedShowBackground;
            float lineGap = hasBackground && !isSplitStyle ? -6.0F : 2.0F;
            int totalHeight = (int)(enabledComponents.size() * lineHeight + (enabledComponents.size() - 1) * lineGap);
            float scaleV = this.getInteractionScale();
            this.setY((int)(window.getScaledHeight() + offset - 5 - totalHeight / 2.0F * (1.0F + scaleV)));
            MathUtil.setAlpha(alpha, () -> this.drawVerticalStyle(matrix, font, enabledComponents));
         } else {
            float scaleH = this.getInteractionScale();
            int prevH = Math.max(this.getHeight(), 16);
            this.setY((int)(window.getScaledHeight() + offset - 5 - prevH / 2.0F * (1.0F + scaleH)));
            if (this.cachedIsSplitStyle) {
               this.drawSplitStyle(matrix, font, enabledComponents);
            } else {
               this.drawMonoStyle(matrix, font, enabledComponents);
            }
         }
      }
   }

   private void drawMonoStyle(MatrixStack matrix, FontRenderer font, List<PlayerInfoComponent> enabledComponents) {
      float padding = 5.0F;
      float separatorSpacing = 10.0F;
      float lineGap = 4.0F;
      float lineHeight = 16.0F;
      float maxWidth = 300.0F;
      List<Float> componentWidths = new ArrayList<>();

      for (PlayerInfoComponent comp : enabledComponents) {
         componentWidths.add(comp.getSmoothedWidth(font, lineHeight));
      }

      float totalWidth = 0.0F;

      for (int i = 0; i < enabledComponents.size(); i++) {
         float componentWidth = componentWidths.get(i);
         totalWidth += componentWidth + padding * 2.0F;
         if (i > 0) {
            totalWidth += separatorSpacing;
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
      } else {
         lines = this.balanceLines(enabledComponents, componentWidths, maxWidth, padding, separatorSpacing, lineHeight);
      }

      float currentY = this.getY();
      float maxLineWidth = 0.0F;

      for (List<Integer> line : lines) {
         float lineContentWidth = 0.0F;

         for (int idx : line) {
            lineContentWidth += componentWidths.get(idx);
         }

         if (line.size() > 1) {
            lineContentWidth += separatorSpacing * (line.size() - 1);
         }

         float backgroundX = this.getX();
         if (lineContentWidth > 0.0F) {
            float actualWidth = lineContentWidth + padding * 2.0F;
            if (this.cachedShowBackground) {
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

            maxLineWidth = Math.max(maxLineWidth, actualWidth);
         }

         float currentX = backgroundX + padding;

         for (int ix = 0; ix < line.size(); ix++) {
            int componentIndex = line.get(ix);
            PlayerInfoComponent component = enabledComponents.get(componentIndex);
            component.render(matrix, currentX, currentY, lineHeight, font);
            currentX += componentWidths.get(componentIndex);
            if (ix < line.size() - 1) {
               currentX += separatorSpacing;
            }
         }

         this.renderLineSeparators(matrix, line, enabledComponents, componentWidths, padding, separatorSpacing, currentY, lineHeight);
         currentY += lineHeight + lineGap;
      }

      int totalHeight = (int)(lines.size() * lineHeight + (lines.size() - 1) * lineGap);
      this.setWidth((int)maxLineWidth);
      this.setHeight(totalHeight);
   }

   private List<List<Integer>> balanceLines(
      List<PlayerInfoComponent> components, List<Float> widths, float maxWidth, float padding, float separatorSpacing, float lineHeight
   ) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;

      for (int i = 0; i < components.size(); i++) {
         float componentWidth = widths.get(i);
         boolean needsSeparator = !currentLine.isEmpty();
         float widthNeeded = componentWidth + padding * 2.0F;
         if (needsSeparator) {
            widthNeeded += separatorSpacing;
         }

         if (!currentLine.isEmpty() && currentLineWidth + widthNeeded > maxWidth) {
            lines.add(new ArrayList<>(currentLine));
            currentLine.clear();
            currentLineWidth = 0.0F;
            widthNeeded = componentWidth + padding * 2.0F;
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
      List<PlayerInfoComponent> allComponents,
      List<Float> componentWidths,
      float padding,
      float separatorSpacing,
      float y,
      float lineHeight
   ) {
      float backgroundX = this.getX();
      float currentX = backgroundX + padding;
      padej.soup.implement.features.modules.hud.PlayerInfo playerInfo = padej.soup.implement.features.modules.hud.PlayerInfo.getInstance();
      int separatorColor;
      if (!playerInfo.getColoredSeparators().isValue()) {
         separatorColor = ColorUtil.getOutline(0.75F, 0.5F);
      } else if (playerInfo.getSeparatorGradient().isValue()) {
         separatorColor = ColorUtil.fade(8);
      } else {
         separatorColor = ColorUtil.getClientColor();
      }

      for (int i = 0; i < line.size() - 1; i++) {
         int componentIndex = line.get(i);
         float width = componentWidths.get(componentIndex);
         currentX += width;
         float separatorX = currentX + separatorSpacing / 2.0F;
         rectangle.render(ShapeProperties.create(matrix, separatorX, y + 4.0F, 0.5, lineHeight - 8.0F).color(separatorColor).build());
         currentX += separatorSpacing;
      }
   }

   private void drawSplitStyle(MatrixStack matrix, FontRenderer font, List<PlayerInfoComponent> enabledComponents) {
      float padding = 5.0F;
      float componentGap = 4.0F;
      float lineGap = 4.0F;
      float lineHeight = 16.0F;
      float maxWidth = 300.0F;
      List<Float> componentWidths = new ArrayList<>();

      for (PlayerInfoComponent comp : enabledComponents) {
         componentWidths.add(comp.getSmoothedWidth(font, lineHeight));
      }

      float totalWidth = 0.0F;

      for (int i = 0; i < enabledComponents.size(); i++) {
         float componentWidth = componentWidths.get(i);
         float fullWidth = componentWidth + padding * 2.0F;
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
      } else {
         lines = this.balanceSplitLines(enabledComponents, componentWidths, maxWidth, padding, componentGap);
      }

      float currentY = this.getY();
      float maxLineWidth = 0.0F;

      for (List<Integer> line : lines) {
         float currentX = this.getX();
         float lineWidth = 0.0F;

         for (int ix = 0; ix < line.size(); ix++) {
            int componentIndex = line.get(ix);
            PlayerInfoComponent component = enabledComponents.get(componentIndex);
            float componentWidth = componentWidths.get(componentIndex);
            if (this.cachedShowBackground) {
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

            float renderX = currentX + padding;
            component.render(matrix, renderX, currentY, lineHeight, font);
            float fullComponentWidth = componentWidth + padding * 2.0F;
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
   }

   private List<List<Integer>> balanceSplitLines(List<PlayerInfoComponent> components, List<Float> widths, float maxWidth, float padding, float componentGap) {
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> currentLine = new ArrayList<>();
      float currentLineWidth = 0.0F;

      for (int i = 0; i < components.size(); i++) {
         float fullWidth = widths.get(i) + padding * 2.0F;
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

   private List<PlayerInfoComponent> getEnabledComponents() {
      List<PlayerInfoComponent> enabled = new ArrayList<>();

      for (PlayerInfoComponent component : this.components) {
         if (this.isComponentEnabled(component.getName())) {
            enabled.add(component);
         }
      }

      return enabled;
   }

   private boolean isComponentEnabled(String componentName) {
      return padej.soup.implement.features.modules.hud.PlayerInfo.getInstance().playerInfoComponents.isSelected(componentName);
   }

   private void drawVerticalStyle(MatrixStack matrix, FontRenderer font, List<PlayerInfoComponent> enabledComponents) {
      float padding = 5.0F;
      float lineHeight = 16.0F;
      boolean isSplitStyle = this.cachedIsSplitStyle;
      boolean hasBackground = this.cachedShowBackground;
      float lineGap = hasBackground && !isSplitStyle ? -6.0F : 2.0F;
      List<Float> componentWidths = new ArrayList<>();

      for (PlayerInfoComponent comp : enabledComponents) {
         componentWidths.add(comp.getSmoothedWidth(font, lineHeight));
      }

      float maxComponentWidth = 0.0F;

      for (float width : componentWidths) {
         maxComponentWidth = Math.max(maxComponentWidth, width);
      }

      float currentY = this.getY();
      float maxWidth = 0.0F;

      for (int i = 0; i < enabledComponents.size(); i++) {
         PlayerInfoComponent component = enabledComponents.get(i);
         float componentWidth = componentWidths.get(i);
         float backgroundX = this.getX();
         if (isSplitStyle) {
            if (hasBackground) {
               blur.render(
                  ShapeProperties.create(matrix, backgroundX, currentY, componentWidth + padding * 2.0F, lineHeight)
                     .round(3.0F)
                     .softness(1.0F)
                     .thickness(2.0F)
                     .outlineColor(ColorUtil.getOutline())
                     .color(ColorUtil.getBlurRect(0.7F))
                     .build()
               );
            }

            maxWidth = Math.max(maxWidth, componentWidth + padding * 2.0F);
         } else {
            if (i == 0 && hasBackground) {
               float totalHeight = enabledComponents.size() * lineHeight + (enabledComponents.size() - 1) * lineGap;
               blur.render(
                  ShapeProperties.create(matrix, backgroundX, currentY, maxComponentWidth + padding * 2.0F, totalHeight)
                     .round(3.0F)
                     .softness(1.0F)
                     .thickness(2.0F)
                     .outlineColor(ColorUtil.getOutline())
                     .color(ColorUtil.getBlurRect(0.7F))
                     .build()
               );
            }

            maxWidth = maxComponentWidth + padding * 2.0F;
         }

         float renderX = backgroundX + padding;
         component.render(matrix, renderX, currentY, lineHeight, font);
         currentY += lineHeight + lineGap;
      }

      int totalHeight = (int)(enabledComponents.size() * lineHeight + (enabledComponents.size() - 1) * lineGap);
      this.setWidth((int)maxWidth);
      this.setHeight(totalHeight);
   }
}
