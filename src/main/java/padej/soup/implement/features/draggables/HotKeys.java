package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.module.Module;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.BindBadgeRenderer;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.hud.Icons;

public class HotKeys extends AbstractDraggable {
   private final List<Module> keysList = new ArrayList<>();
   private final List<String> cachedBindNames = new ArrayList<>();
   private boolean cachedShowHeader = false;
   private boolean cachedDarkenHeader = false;
   private String cachedMode = "Active";

   public HotKeys() {
      super("HotKeys", 300, 10, 80, 23, true);
   }

   @Override
   public boolean visible() {
      return !this.keysList.isEmpty() || PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      padej.soup.implement.features.modules.hud.HotKeys hotKeysModule = padej.soup.implement.features.modules.hud.HotKeys.getInstance();
      this.cachedMode = hotKeysModule.mode.getSelected();
      this.cachedShowHeader = hotKeysModule.getShowHeader().isValue();
      this.cachedDarkenHeader = hotKeysModule.getDarkenHeader().isValue();
      boolean onlyActive = this.cachedMode.equals("Active");
      this.keysList.clear();
      this.cachedBindNames.clear();

      for (Module module : Main.getInstance().getModuleProvider().getModules()) {
         if (module.getKey() != -1 && module.isCanBind() && (!onlyActive || !(module.getAnimation().getOutput().floatValue() <= 0.0F))) {
            this.keysList.add(module);
            this.cachedBindNames.add(StringUtil.getBindName(module.getKey()));
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      float centerX = this.getX() + this.getWidth() / 2.0F;
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontModuleBold = Fonts.getSize(13, Fonts.Type.INTER_BOLD);
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = this.cachedShowHeader;
      boolean darkenHeader = this.cachedDarkenHeader;
      float animatedHeight = this.getAnimatedHeight();
      int maxWidth = 80;
      blur.render(
         ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), animatedHeight)
            .round(4.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(0.7F))
            .build()
      );
      if (showHeader && darkenHeader) {
         rectangle.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
               .round(4.0F, 0.0F, 4.0F, 0.0F)
               .softness(-0.5F)
               .thickness(0.0F)
               .color(ColorUtil.getRectDarker(0.9F))
               .build()
         );
      }

      if (showHeader) {
         font.drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
         float iconSize = 8.0F;
         float iconPadding = 4.0F;
         Icons iconsModule = Icons.getInstance();
         int iconColor;
         if (!iconsModule.getColoredIcons().isValue()) {
            iconColor = ColorUtil.getText();
         } else if (iconsModule.getIconGradient().isValue()) {
            iconColor = ColorUtil.fade(8);
         } else {
            iconColor = ColorUtil.getClientColor();
         }

         image.setIcon(61450)
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(iconColor)
                  .build()
            );
      }

      int offset = showHeader ? (int)(headerHeight + 7.0F) : 7;
      String mode = this.cachedMode;

      for (int idx = 0; idx < this.keysList.size(); idx++) {
         Module module = this.keysList.get(idx);
         String bind = idx < this.cachedBindNames.size() ? this.cachedBindNames.get(idx) : "";
         float centerY = this.getY() + offset;
         boolean isActive = module.getAnimation().getOutput().floatValue() > 0.0F;
         float animation = module.getAnimation().getOutput().floatValue();
         FontRenderer currentFont;
         if (mode.equals("All") && isActive) {
            currentFont = fontModuleBold;
         } else {
            currentFont = fontModule;
         }

         float bindBadgeWidth = BindBadgeRenderer.getBadgeWidth(bind);
         float moduleNameWidth = currentFont.getStringWidth(module.getVisibleName());
         float totalWidth = moduleNameWidth + bindBadgeWidth + 24.0F;
         if (mode.equals("Active")) {
            MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
               currentFont.drawString(matrix, module.getVisibleName(), this.getX() + 6, centerY + 1.0F, ColorUtil.getText());
               this.drawBindBadge(matrix, bind, centerY + 1.0F);
            });
            offset += (int)(animation * 11.0F);
         } else {
            currentFont.drawString(matrix, module.getVisibleName(), this.getX() + 6, centerY + 1.0F, ColorUtil.getText());
            this.drawBindBadge(matrix, bind, centerY + 1.0F);
            offset += 11;
         }

         maxWidth = (int)Math.max(totalWidth, (float)maxWidth);
      }

      this.setWidth(maxWidth);
      this.setHeight(mode.equals("Active") ? (int)this.getAnimatedHeight() : offset);
   }

   private void drawBindBadge(MatrixStack matrix, String bind, float textY) {
      BindBadgeRenderer.draw(matrix, bind, this.getX() + this.getWidth(), textY);
   }

   private float getAnimatedHeight() {
      if (!this.cachedMode.equals("Active")) {
         return this.getHeight();
      } else {
         float headerHeight = 16.0F;
         boolean showHeader = this.cachedShowHeader;
         float animatedOffset = showHeader ? headerHeight + 7.0F : 7.0F;

         for (Module module : this.keysList) {
            float animation = module.getAnimation().getOutput().floatValue();
            animatedOffset += animation * 11.0F;
         }

         return animatedOffset;
      }
   }
}
