package padej.soup.api.system.font.msdf;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
   private static final Gson GSON = new Gson();
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private ResourceProvider() {
   }

   public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
      return (T)GSON.fromJson(toString(identifier), clazz);
   }

   public static String toString(Identifier identifier) {
      ResourceManager rm = mc.getResourceManager();

      try {
         Optional<Resource> resource = rm.getResource(identifier);
         if (resource.isPresent()) {
            String var6;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
               StringBuilder builder = new StringBuilder();

               String line;
               while ((line = reader.readLine()) != null) {
                  builder.append(line).append('\n');
               }

               var6 = builder.toString();
            }

            return var6;
         }
      } catch (Exception var9) {
         throw new RuntimeException("Failed to read resource: " + identifier, var9);
      }

      throw new RuntimeException("Resource not found: " + identifier);
   }
}
