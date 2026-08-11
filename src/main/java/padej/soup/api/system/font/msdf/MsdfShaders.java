package padej.soup.api.system.font.msdf;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class MsdfShaders {
   public static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(
      Identifier.of("minecraft", "core/msdf_font/data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );

   private MsdfShaders() {
   }
}
