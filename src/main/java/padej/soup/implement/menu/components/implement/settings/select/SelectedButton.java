package padej.soup.implement.menu.components.implement.settings.select;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.repository.config.ConfigManager;
import padej.soup.api.repository.config.ConfigUtils;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.TargetHudRenderer;
import padej.soup.implement.features.modules.hud.TargetHud;
import padej.soup.implement.menu.components.AbstractComponent;

public class SelectedButton extends AbstractComponent {
   private final SelectSetting setting;
   private final String text;
   private float alpha;
   private boolean isHovered = false;
   private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(0.5);

   public SelectedButton(SelectSetting setting, String text) {
      this.setting = setting;
      this.text = text;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      this.isHovered = MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      this.alphaAnimation.setDirection(this.setting.getSelected().contains(this.text) ? Direction.FORWARDS : Direction.BACKWARDS);
      float opacity = this.alphaAnimation.getOutput().floatValue();
      int selectedOpacity = ColorUtil.multAlpha(ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity), this.alpha);
      if (!this.alphaAnimation.isFinished(Direction.BACKWARDS)) {
         rectangle.render(
            ShapeProperties.create(matrices, this.x, this.y, this.width, this.height + 0.15F)
               .round(getRound(this.setting.getList(), this.text))
               .color(selectedOpacity)
               .build()
         );
      }

      Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawString(matrices, this.text, this.x + 4.0F, this.y + 5.0F, ColorUtil.getText(this.alpha));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && button == 0) {
         SoundManager.playSound(SoundManager.CLICK);
         this.setting.setSelected(this.text);
         ConfigManager cm = ConfigUtils.getConfigManager();
         if (cm != null) {
            cm.scheduleSave();
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void renderTargetHudTooltip(DrawContext context) {
      if (this.isTargetHudStyleSetting() && this.isHovered) {
         int screenWidth = window.getScaledWidth();
         int screenHeight = window.getScaledHeight();
         if (mc.player != null) {
            TargetHud hudModule = TargetHud.getInstance();
            float scale = hudModule.scale.getValue();
            float baseWidth = this.text.equals("Default") ? 100.0F : 120.0F;
            float baseHeight = this.text.equals("Default") ? 36.0F : 46.0F;
            float scaledWidth = baseWidth * scale;
            float scaledHeight = baseHeight * scale;
            float xScaleOffset = scaledWidth - baseWidth;
            float yScaleOffset = scaledHeight - baseHeight;
            float tooltipX = screenWidth / 2.0F - 205.0F;
            float tooltipY = screenHeight / 2.0F + 120.0F;
            tooltipX += xScaleOffset / 2.0F;
            tooltipY += yScaleOffset / 2.0F;
            context.getMatrices().push();
            context.getMatrices().translate(tooltipX + 10.0F + baseWidth / 2.0F, tooltipY + 5.0F + baseHeight / 2.0F, 0.0F);
            context.getMatrices().scale(scale, scale, 1.0F);
            context.getMatrices().translate(-baseWidth / 2.0F, -baseHeight / 2.0F, 0.0F);
            String var14 = this.text;
            switch (var14) {
               case "Default":
                  TargetHudRenderer.renderStyleZenith(context, mc.player, 0.0F, 0.0F, 100.0F, 36.0F, 50.0F);
                  break;
               case "Round":
                  TargetHudRenderer.renderStyleAres(context, mc.player, 0.0F, 0.0F, 120.0F, 46.0F, 50.0F);
            }

            context.getMatrices().pop();
         }
      }
   }

   private boolean isTargetHudStyleSetting() {
      return this.setting.getNameKey().equals("setting.targethud.style.name");
   }

   public static Vector4f getRound(List<String> list, String text) {
      if (list.size() == 1) {
         return new Vector4f(4.0F);
      } else if (list.getLast().contains(text)) {
         return new Vector4f(0.0F, 4.0F, 0.0F, 4.0F);
      } else {
         return list.getFirst().contains(text) ? new Vector4f(4.0F, 0.0F, 4.0F, 0.0F) : new Vector4f(0.0F);
      }
   }

   public String getText() {
      return this.text;
   }

   public SelectedButton setAlpha(float alpha) {
      this.alpha = alpha;
      return this;
   }

   public boolean isHovered() {
      return this.isHovered;
   }
}
