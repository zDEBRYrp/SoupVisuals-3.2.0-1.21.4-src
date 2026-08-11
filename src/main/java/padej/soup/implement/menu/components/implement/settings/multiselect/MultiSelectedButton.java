package padej.soup.implement.menu.components.implement.settings.multiselect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
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
import padej.soup.implement.menu.components.AbstractComponent;

public class MultiSelectedButton extends AbstractComponent {
   private final MultiSelectSetting setting;
   private final String text;
   private float alpha;
   private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(0.5);

   public MultiSelectedButton(MultiSelectSetting setting, String text) {
      this.setting = setting;
      this.text = text;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.alphaAnimation.setDirection(this.setting.getSelected().contains(this.text) ? Direction.FORWARDS : Direction.BACKWARDS);
      float opacity = this.alphaAnimation.getOutput().floatValue();
      int selectedOpacity = ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity * this.alpha);
      if (!this.alphaAnimation.isFinished(Direction.BACKWARDS)) {
         rectangle.render(
            ShapeProperties.create(matrix, this.x, this.y, this.width, this.height + 0.15F)
               .round(getRound(this.setting.getList(), this.text))
               .color(selectedOpacity)
               .build()
         );
      }

      Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawString(matrix, this.text, this.x + 4.0F, this.y + 5.0F, ColorUtil.getText(this.alpha));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && button == 0) {
         List<String> selected = new ArrayList<>(this.setting.getSelected());
         if (selected.contains(this.text)) {
            SoundManager.playSound(SoundManager.TURN_OFF, 1.0F, 1.5F);
            selected.remove(this.text);
         } else {
            SoundManager.playSound(SoundManager.TURN_ON, 1.0F, 1.5F);
            selected.add(this.text);
            this.sortSelectedAccordingToList(selected, this.setting.getList());
         }

         this.setting.setSelected(selected);
         ConfigManager cm = ConfigUtils.getConfigManager();
         if (cm != null) {
            cm.scheduleSave();
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void sortSelectedAccordingToList(List<String> selected, List<String> list) {
      selected.sort(Comparator.comparingInt(list::indexOf));
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

   public MultiSelectedButton setAlpha(float alpha) {
      this.alpha = alpha;
      return this;
   }
}
