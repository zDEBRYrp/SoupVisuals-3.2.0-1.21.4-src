package padej.soup.api.system.activity;

public class ActivityData {
   private String date;
   private long totalPlayTime;
   private long loginTime;
   private long logoutTime;
   private int sessionsCount;

   public ActivityData(String date) {
      this.date = date;
      this.totalPlayTime = 0L;
      this.loginTime = 0L;
      this.logoutTime = 0L;
      this.sessionsCount = 0;
   }

   public void addSession(long startTime, long endTime) {
      this.totalPlayTime += endTime - startTime;
      this.sessionsCount++;
      this.logoutTime = endTime;
      if (this.loginTime == 0L) {
         this.loginTime = startTime;
      }
   }

   public String getDate() {
      return this.date;
   }

   public long getTotalPlayTime() {
      return this.totalPlayTime;
   }

   public long getLoginTime() {
      return this.loginTime;
   }

   public long getLogoutTime() {
      return this.logoutTime;
   }

   public int getSessionsCount() {
      return this.sessionsCount;
   }

   public void setDate(String date) {
      this.date = date;
   }

   public void setTotalPlayTime(long totalPlayTime) {
      this.totalPlayTime = totalPlayTime;
   }

   public void setLoginTime(long loginTime) {
      this.loginTime = loginTime;
   }

   public void setLogoutTime(long logoutTime) {
      this.logoutTime = logoutTime;
   }

   public void setSessionsCount(int sessionsCount) {
      this.sessionsCount = sessionsCount;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ActivityData other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (this.getTotalPlayTime() != other.getTotalPlayTime()) {
         return false;
      } else if (this.getLoginTime() != other.getLoginTime()) {
         return false;
      } else if (this.getLogoutTime() != other.getLogoutTime()) {
         return false;
      } else if (this.getSessionsCount() != other.getSessionsCount()) {
         return false;
      } else {
         Object this$date = this.getDate();
         Object other$date = other.getDate();
         return this$date == null ? other$date == null : this$date.equals(other$date);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ActivityData;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      long $totalPlayTime = this.getTotalPlayTime();
      result = result * 59 + (int)($totalPlayTime >>> 32 ^ $totalPlayTime);
      long $loginTime = this.getLoginTime();
      result = result * 59 + (int)($loginTime >>> 32 ^ $loginTime);
      long $logoutTime = this.getLogoutTime();
      result = result * 59 + (int)($logoutTime >>> 32 ^ $logoutTime);
      result = result * 59 + this.getSessionsCount();
      Object $date = this.getDate();
      return result * 59 + ($date == null ? 43 : $date.hashCode());
   }

   @Override
   public String toString() {
      return "ActivityData(date="
         + this.getDate()
         + ", totalPlayTime="
         + this.getTotalPlayTime()
         + ", loginTime="
         + this.getLoginTime()
         + ", logoutTime="
         + this.getLogoutTime()
         + ", sessionsCount="
         + this.getSessionsCount()
         + ")";
   }

   public ActivityData() {
   }

   public ActivityData(String date, long totalPlayTime, long loginTime, long logoutTime, int sessionsCount) {
      this.date = date;
      this.totalPlayTime = totalPlayTime;
      this.loginTime = loginTime;
      this.logoutTime = logoutTime;
      this.sessionsCount = sessionsCount;
   }
}
