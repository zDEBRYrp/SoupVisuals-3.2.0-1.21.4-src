package padej.soup.implement.menu.components.implement.window.implement.settings.color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.window.AbstractWindow;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.component.AlphaComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.component.ColorEditorComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.component.ColorPresetComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.component.HueComponent;
import padej.soup.implement.menu.components.implement.window.implement.settings.color.component.SaturationComponent;

public class ColorWindow extends AbstractWindow {
   private final List<AbstractComponent> components = new ArrayList<>();
   private final HueComponent hueComponent;
   private final SaturationComponent saturationComponent;
   private final AlphaComponent alphaComponent;
   private final ColorEditorComponent colorEditorComponent;
   private final ColorPresetComponent colorPresetComponent;

   public ColorWindow(ColorSetting setting) {
      this.components
         .addAll(
            Arrays.asList(
               this.hueComponent = new HueComponent(setting),
               this.saturationComponent = new SaturationComponent(setting),
               this.alphaComponent = new AlphaComponent(setting),
               this.colorEditorComponent = new ColorEditorComponent(setting),
               this.colorPresetComponent = new ColorPresetComponent(setting)
            )
         );
   }

   @Override
   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, this.height)
            .round(6.0F)
            .thickness(2.0F)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getMainGuiColor())
            .build()
      );
      this.alphaComponent.position(this.x, this.y);
      this.hueComponent.position(this.x, this.y);
      this.saturationComponent.position(this.x, this.y);
      this.colorEditorComponent.position(this.x, this.y);
      this.height = ((ColorPresetComponent)this.colorPresetComponent.position(this.x, this.y)).getWindowHeight() - 20.0F;
      this.components.forEach(component -> component.render(context, mouseX, mouseY, delta));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.draggable(MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, 17.0));
      this.components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      this.components.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      this.components.forEach(component -> component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(component -> component.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public List<AbstractComponent> getComponents() {
      return this.components;
   }

   public HueComponent getHueComponent() {
      return this.hueComponent;
   }

   public SaturationComponent getSaturationComponent() {
      return this.saturationComponent;
   }

   public AlphaComponent getAlphaComponent() {
      return this.alphaComponent;
   }

   public ColorEditorComponent getColorEditorComponent() {
      return this.colorEditorComponent;
   }

   public ColorPresetComponent getColorPresetComponent() {
      return this.colorPresetComponent;
   }
}
