package padej.soup.implement.menu.components;

import org.joml.Matrix4f;
import padej.soup.base.QuickImports;
import padej.soup.base.trait.ResizableMovable;
import padej.soup.base.util.math.MathUtil;

public abstract class AbstractComponent implements Component, QuickImports, ResizableMovable {
   public float x;
   public float y;
   public float width;
   public float height;
   public double scroll = 0.0;
   public double smoothedScroll = 0.0;
   protected static Matrix4f renderMatrix;

   @Override
   public ResizableMovable position(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
   }

   @Override
   public ResizableMovable size(float width, float height) {
      this.width = width;
      this.height = height;
      return this;
   }

   @Override
   public void tick() {
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return false;
   }

   @Override
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      return false;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return false;
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public static void setRenderMatrix(Matrix4f renderMatrix) {
      AbstractComponent.renderMatrix = renderMatrix;
   }
}
