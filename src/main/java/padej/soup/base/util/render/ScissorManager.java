package padej.soup.base.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Stack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import padej.soup.base.QuickImports;
import padej.soup.base.util.other.Pool;

public class ScissorManager implements QuickImports {
   private final Pool<ScissorManager.Scissor> scissorPool = new Pool<>(ScissorManager.Scissor::new);
   private final Stack<ScissorManager.Scissor> scissorStack = new Stack<>();

   public void push(Matrix4f matrix4f, float x, float y, float width, float height) {
      Vector3f pos = matrix4f.transformPosition(x, y, 0.0F, new Vector3f());
      Vector3f size = matrix4f.getScale(new Vector3f()).mul(width, height, 0.0F);
      ScissorManager.Scissor newScissor = this.scissorPool.get();
      if (!this.scissorStack.isEmpty()) {
         ScissorManager.Scissor prevScissor = this.scissorStack.peek();
         double intersectX = Math.max((float)prevScissor.x, pos.x);
         double intersectY = Math.max((float)prevScissor.y, pos.y);
         double intersectWidth = Math.min((float)(prevScissor.x + prevScissor.width), pos.x + size.x) - intersectX;
         double intersectHeight = Math.min((float)(prevScissor.y + prevScissor.height), pos.y + size.y) - intersectY;
         newScissor.set(intersectX, intersectY, intersectWidth, intersectHeight);
      } else {
         newScissor.set(pos.x, pos.y, size.x, size.y);
      }

      this.scissorStack.push(newScissor);
      this.setScissor(newScissor);
   }

   public void pop() {
      if (!this.scissorStack.isEmpty()) {
         this.scissorPool.free(this.scissorStack.pop());
         if (this.scissorStack.isEmpty()) {
            RenderSystem.disableScissor();
         } else {
            this.setScissor(this.scissorStack.peek());
         }
      }
   }

   private void setScissor(ScissorManager.Scissor scissor) {
      int scaleFactor = (int)window.getScaleFactor();
      int x = scissor.x * scaleFactor;
      int y = window.getHeight() - (scissor.y * scaleFactor + scissor.height * scaleFactor);
      int width = scissor.width * scaleFactor;
      int height = scissor.height * scaleFactor;
      RenderSystem.enableScissor(x, y, width, height);
   }

   private static class Scissor {
      public int x;
      public int y;
      public int width;
      public int height;

      public void set(double x, double y, double width, double height) {
         this.x = Math.max(0, (int)Math.round(x));
         this.y = Math.max(0, (int)Math.round(y));
         this.width = Math.max(0, (int)Math.round(width));
         this.height = Math.max(0, (int)Math.round(height));
      }

      ScissorManager.Scissor copy() {
         ScissorManager.Scissor newScissor = new ScissorManager.Scissor();
         newScissor.set(this.x, this.y, this.width, this.height);
         return newScissor;
      }
   }
}
