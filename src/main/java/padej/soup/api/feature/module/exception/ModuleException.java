package padej.soup.api.feature.module.exception;

public final class ModuleException extends RuntimeException {
   private final String message;
   private final String moduleName;

   public ModuleException(String message, String moduleName) {
      this.message = message;
      this.moduleName = moduleName;
   }

   @Override
   public String getMessage() {
      return this.message;
   }

   public String getModuleName() {
      return this.moduleName;
   }

   @Override
   public String toString() {
      return "ModuleException(message=" + this.getMessage() + ", moduleName=" + this.getModuleName() + ")";
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ModuleException other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else {
         Object this$message = this.getMessage();
         Object other$message = other.getMessage();
         if (this$message == null ? other$message == null : this$message.equals(other$message)) {
            Object this$moduleName = this.getModuleName();
            Object other$moduleName = other.getModuleName();
            return this$moduleName == null ? other$moduleName == null : this$moduleName.equals(other$moduleName);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ModuleException;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      Object $message = this.getMessage();
      result = result * 59 + ($message == null ? 43 : $message.hashCode());
      Object $moduleName = this.getModuleName();
      return result * 59 + ($moduleName == null ? 43 : $moduleName.hashCode());
   }
}
