package padej.soup.base.util.render.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;

public class Shader {
   private final int programId;
   private final String vertexPath;
   private final String fragmentPath;

   public Shader(String folder, String name) {
      this.vertexPath = "/assets/soupapi/shaders/" + folder + "/" + name + ".vsh";
      this.fragmentPath = "/assets/soupapi/shaders/" + folder + "/" + name + ".fsh";
      int vertexId = this.createShader(this.vertexPath, 35633);
      int fragmentId = this.createShader(this.fragmentPath, 35632);
      this.programId = GL20.glCreateProgram();
      GL20.glAttachShader(this.programId, vertexId);
      GL20.glAttachShader(this.programId, fragmentId);
      GL20.glLinkProgram(this.programId);
      if (GL20.glGetProgrami(this.programId, 35714) == 0) {
         throw new RuntimeException("Failed to link shader program: " + GL20.glGetProgramInfoLog(this.programId));
      } else {
         GL20.glDeleteShader(vertexId);
         GL20.glDeleteShader(fragmentId);
      }
   }

   private int createShader(String path, int type) {
      try {
         String source = this.readShaderFile(path);
         int shaderId = GL20.glCreateShader(type);
         GL20.glShaderSource(shaderId, source);
         GL20.glCompileShader(shaderId);
         if (GL20.glGetShaderi(shaderId, 35713) == 0) {
            throw new RuntimeException("Failed to compile shader: " + path + "\n" + GL20.glGetShaderInfoLog(shaderId));
         } else {
            return shaderId;
         }
      } catch (IOException var5) {
         throw new RuntimeException("Failed to load shader: " + path, var5);
      }
   }

   private String readShaderFile(String path) throws IOException {
      InputStream stream = Shader.class.getResourceAsStream(path);
      if (stream == null) {
         throw new IOException("Shader file not found: " + path);
      } else {
         StringBuilder source = new StringBuilder();

         String line;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while ((line = reader.readLine()) != null) {
               source.append(line).append("\n");
            }
         }

         return source.toString();
      }
   }

   public void bind() {
      GL20.glUseProgram(this.programId);
   }

   public void unbind() {
      GL20.glUseProgram(0);
   }

   public void setUniform1i(String name, int value) {
      int location = GL20.glGetUniformLocation(this.programId, name);
      if (location != -1) {
         GL20.glUniform1i(location, value);
      }
   }

   public void setUniform1f(String name, float value) {
      int location = GL20.glGetUniformLocation(this.programId, name);
      if (location != -1) {
         GL20.glUniform1f(location, value);
      }
   }

   public void setUniform2f(String name, float x, float y) {
      int location = GL20.glGetUniformLocation(this.programId, name);
      if (location != -1) {
         GL20.glUniform2f(location, x, y);
      }
   }

   public void setUniform2f(String name, Vector2f vec) {
      this.setUniform2f(name, vec.x, vec.y);
   }

   public void setUniform3f(String name, float x, float y, float z) {
      int location = GL20.glGetUniformLocation(this.programId, name);
      if (location != -1) {
         GL20.glUniform3f(location, x, y, z);
      }
   }

   public void setUniform3f(String name, Vector3f vec) {
      this.setUniform3f(name, vec.x, vec.y, vec.z);
   }

   public void setUniformBool(String name, boolean value) {
      this.setUniform1i(name, value ? 1 : 0);
   }

   public void delete() {
      GL20.glDeleteProgram(this.programId);
   }
}
