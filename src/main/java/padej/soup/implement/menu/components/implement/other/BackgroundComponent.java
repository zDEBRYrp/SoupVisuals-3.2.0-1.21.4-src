package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;

public class BackgroundComponent extends AbstractComponent {
   private SearchComponent searchComponent;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      blur.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(4.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.multAlpha(ColorUtil.getMainGuiColor(), 0.8F))
            .build()
      );
      int separatorColor = 872415231;
      rectangle.render(ShapeProperties.create(context.getMatrices(), this.x + 32.0F, this.y + 28.0F, 1.0, 207.0).color(separatorColor).build());
      rectangle.render(ShapeProperties.create(context.getMatrices(), this.x + 1.0F, this.y + 27.0F, 321.0, 1.0).color(separatorColor).build());
      float categoryOffsetX = 0.0F;
      if (this.searchComponent != null) {
         float searchProgress = this.searchComponent.getAnimationProgress();
         categoryOffsetX = searchProgress * 61.0F;
      }

      String title = MenuScreen.INSTANCE.getCategory().getLocalizedName();
      float titleWidth = Fonts.getSize(14).getStringWidth(title);
      float backgroundWidth = titleWidth + 8.0F;
      float backgroundX = this.x + 60.0F + categoryOffsetX;
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), backgroundX, this.y + 5.0F, backgroundWidth, 17.0)
            .round(3.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(0.5F))
            .build()
      );
      float centeredX = backgroundX + 4.0F;
      Fonts.getSize(14).drawString(matrix, title, centeredX, this.y + 12.0F, ColorUtil.getDescription());
   }

   public BackgroundComponent setSearchComponent(SearchComponent searchComponent) {
      this.searchComponent = searchComponent;
      return this;
   }
}
