package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;

public class ScoreBoard extends AbstractDraggable {
   private static final Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR = Comparator.comparing(ScoreboardEntry::value)
      .reversed()
      .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
   private final List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
   private ScoreboardObjective objective;
   private MutableText cachedText = Text.empty().copy();
   private Text cachedMainText = Text.empty();
   private boolean cachedShowHeader = false;
   private boolean cachedDarkenHeader = false;

   public ScoreBoard() {
      super("ScoreBoard", 10, 100, 100, 120, true);
   }

   @Override
   public boolean visible() {
      return !this.scoreboardEntries.isEmpty();
   }

   @Override
   public void tick() {
      if (mc.world == null) {
         this.objective = null;
         this.scoreboardEntries.clear();
      } else {
         this.objective = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (this.objective == null) {
            this.scoreboardEntries.clear();
         } else {
            this.scoreboardEntries.clear();
            this.scoreboardEntries.addAll(mc.world.getScoreboard().getScoreboardEntries(this.objective));
            this.scoreboardEntries.sort(SCOREBOARD_ENTRY_COMPARATOR);
            this.cachedText = Text.empty().copy();

            for (ScoreboardEntry entry : this.scoreboardEntries) {
               this.cachedText.append(Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).append("\n");
            }

            this.cachedMainText = this.objective.getDisplayName();
            padej.soup.implement.features.modules.hud.ScoreBoard scoreboardModule = padej.soup.implement.features.modules.hud.ScoreBoard.getInstance();
            this.cachedShowHeader = scoreboardModule.getShowHeader().isValue();
            this.cachedDarkenHeader = scoreboardModule.getDarkenHeader().isValue();
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(16);
      MutableText text = this.cachedText;
      Text mainText = this.cachedMainText;
      float headerHeight = 14.0F;
      int padding = 3;
      boolean showHeader = this.cachedShowHeader;
      boolean darkenHeader = this.cachedDarkenHeader;
      int width = (int)Math.max(font.getStringWidth(text) + padding * 2 + 1.0F, 100.0F);
      float contentHeight = font.getStringHeight(text) + padding;
      float totalHeight = showHeader ? headerHeight + contentHeight : contentHeight;
      blur.render(
         ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), totalHeight)
            .round(4.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(0.7F))
            .build()
      );
      if (showHeader) {
         if (darkenHeader) {
            rectangle.render(
               ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
                  .round(4.0F, 0.0F, 4.0F, 0.0F)
                  .softness(-0.5F)
                  .thickness(0.0F)
                  .color(ColorUtil.getRectDarker(0.9F))
                  .build()
            );
         }

         font.drawText(matrix, mainText, (int)(this.getX() + (this.getWidth() - font.getStringWidth(mainText)) / 2.0F), this.getY() + padding + 1.5F);
      }

      int offsetText = showHeader ? (int)(headerHeight + padding) : padding;
      font.drawText(matrix, text, this.getX() + padding, this.getY() + offsetText);
      if (this.getX() > mc.getWindow().getScaledWidth() / 2) {
         this.setX(this.getX() + this.getWidth() - width);
      }

      this.setWidth(width);
      this.setHeight((int)totalHeight);
   }
}
