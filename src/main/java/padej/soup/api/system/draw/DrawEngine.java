package padej.soup.api.system.draw;

import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public interface DrawEngine {
   void quad(Matrix4f var1, BufferBuilder var2, float var3, float var4, float var5, float var6);

   void quad(Matrix4f var1, BufferBuilder var2, float var3, float var4, float var5, float var6, int var7);

   void quad(Matrix4f var1, float var2, float var3, float var4, float var5, int var6);
}
