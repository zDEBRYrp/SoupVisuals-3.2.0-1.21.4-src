package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20C;
import padej.soup.api.accessor.ICapabilityTracker;
import padej.soup.mixins.accessor.BufferRendererAccessor;

public class GL {
   private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
   private static final ICapabilityTracker DEPTH = getTracker("DEPTH");
   private static final ICapabilityTracker BLEND = getTracker("BLEND");
   private static final ICapabilityTracker CULL = getTracker("CULL");
   private static final ICapabilityTracker SCISSOR = getTracker("SCISSOR");
   private static boolean depthSaved;
   private static boolean blendSaved;
   private static boolean cullSaved;
   private static boolean scissorSaved;
   public static int CURRENT_IBO;
   private static int prevIbo;

   private GL() {
   }

   public static int genVertexArray() {
      return GlStateManager._glGenVertexArrays();
   }

   public static int genBuffer() {
      return GlStateManager._glGenBuffers();
   }

   public static void deleteShader(int shader) {
      GlStateManager.glDeleteShader(shader);
   }

   public static void bindVertexArray(int vao) {
      GlStateManager._glBindVertexArray(vao);
      BufferRendererAccessor.setCurrentVertexBuffer(null);
   }

   public static void bindVertexBuffer(int vbo) {
      GlStateManager._glBindBuffer(34962, vbo);
   }

   public static void bindIndexBuffer(int ibo) {
      if (ibo != 0) {
         prevIbo = CURRENT_IBO;
      }

      GlStateManager._glBindBuffer(34963, ibo != 0 ? ibo : prevIbo);
   }

   public static void bufferData(int target, ByteBuffer data, int usage) {
      GlStateManager._glBufferData(target, data, usage);
   }

   public static void drawElements(int mode, int first, int type) {
      GlStateManager._drawElements(mode, first, type, 0L);
   }

   public static void enableVertexAttribute(int i) {
      GlStateManager._enableVertexAttribArray(i);
   }

   public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
      GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
   }

   public static int createShader(int type) {
      return GlStateManager.glCreateShader(type);
   }

   public static void shaderSource(int shader, String source) {
      GlStateManager.glShaderSource(shader, source);
   }

   public static String compileShader(int shader) {
      GlStateManager.glCompileShader(shader);
      return GlStateManager.glGetShaderi(shader, 35713) == 0 ? GlStateManager.glGetShaderInfoLog(shader, 512) : null;
   }

   public static int createProgram() {
      return GlStateManager.glCreateProgram();
   }

   public static String linkProgram(int program, int vertShader, int fragShader) {
      GlStateManager.glAttachShader(program, vertShader);
      GlStateManager.glAttachShader(program, fragShader);
      GlStateManager.glLinkProgram(program);
      return GlStateManager.glGetProgrami(program, 35714) == 0 ? GlStateManager.glGetProgramInfoLog(program, 512) : null;
   }

   public static void useProgram(int program) {
      GlStateManager._glUseProgram(program);
   }

   public static int getUniformLocation(int program, String name) {
      return GlStateManager._glGetUniformLocation(program, name);
   }

   public static void uniformInt(int location, int v) {
      GlStateManager._glUniform1i(location, v);
   }

   public static void uniformFloat(int location, float v) {
      GL20C.glUniform1f(location, v);
   }

   public static void uniformFloat2(int location, float v1, float v2) {
      GL20C.glUniform2f(location, v1, v2);
   }

   public static void uniformMatrix(int location, Matrix4f v) {
      v.get(MAT);
      GlStateManager._glUniformMatrix4(location, false, MAT);
   }

   public static void saveState() {
      depthSaved = DEPTH.soup$get();
      blendSaved = BLEND.soup$get();
      cullSaved = CULL.soup$get();
      scissorSaved = SCISSOR.soup$get();
   }

   public static void restoreState() {
      DEPTH.soup$set(depthSaved);
      BLEND.soup$set(blendSaved);
      CULL.soup$set(cullSaved);
      SCISSOR.soup$set(scissorSaved);
      disableLineSmooth();
   }

   public static void disableDepth() {
      GlStateManager._disableDepthTest();
   }

   public static void enableBlend() {
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
   }

   public static void disableCull() {
      GlStateManager._disableCull();
   }

   public static void disableLineSmooth() {
      GL20C.glDisable(2848);
   }

   public static void bindTexture(int i, int slot) {
      GlStateManager._activeTexture(33984 + slot);
      GlStateManager._bindTexture(i);
   }

   private static ICapabilityTracker getTracker(String fieldName) {
      try {
         Class<?> glStateManager = GlStateManager.class;
         Field field = glStateManager.getDeclaredField(fieldName);
         field.setAccessible(true);
         Object state = field.get(null);
         String trackerName = FabricLoader.getInstance()
            .getMappingResolver()
            .mapClassName("intermediary", "com.mojang.blaze3d.platform.GlStateManager$class_1018");
         Field capStateField = null;

         for (Field f : state.getClass().getDeclaredFields()) {
            if (f.getType().getName().equals(trackerName)) {
               capStateField = f;
               break;
            }
         }

         capStateField.setAccessible(true);
         return (ICapabilityTracker)capStateField.get(state);
      } catch (IllegalAccessException | NoSuchFieldException var10) {
         throw new IllegalStateException("Could not find GL state tracker '" + fieldName + "'", var10);
      }
   }
}
