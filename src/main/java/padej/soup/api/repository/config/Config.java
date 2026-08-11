package padej.soup.api.repository.config;

public class Config {
   private String name;
   private String description;
   private String fileName;
   private long lastModified;

   public Config(String name, String description, String fileName) {
      this.name = name;
      this.description = description;
      this.fileName = fileName;
      this.lastModified = System.currentTimeMillis();
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         Config config = (Config)obj;
         return this.name.equals(config.name) && this.fileName.equals(config.fileName);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.name.hashCode() + this.fileName.hashCode();
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public String getFileName() {
      return this.fileName;
   }

   public long getLastModified() {
      return this.lastModified;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public void setLastModified(long lastModified) {
      this.lastModified = lastModified;
   }

   public Config(String name, String description, String fileName, long lastModified) {
      this.name = name;
      this.description = description;
      this.fileName = fileName;
      this.lastModified = lastModified;
   }

   public Config() {
   }
}
