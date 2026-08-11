package padej.soup.api.accessor;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
   void soup$pushFramebuffer(Framebuffer var1);

   void soup$popFramebuffer();
}
