package padej.soup.implement.menu.components.implement.other;

import com.mojang.blaze3d.systems.RenderSystem;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.activity.ActivityData;
import padej.soup.api.system.activity.ActivityManager;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class ActivityCalendarComponent extends AbstractComponent {
   private final ActivityManager activityManager;
   private final float CELL_SIZE = 4.0F;
   private final float CELL_SPACING = 1.15F;
   private int weeksToShow;
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
   private static final DateTimeFormatter TOOLTIP_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
   private static final float[] INTENSITY_LEVELS = new float[]{0.0F, 0.25F, 0.4F, 0.6F, 0.85F};
   private static final String[] DAY_LABEL_KEYS = new String[]{"", "calendar.day.mon", "", "calendar.day.wed", "", "calendar.day.fri", ""};
   private static final ActivityData EMPTY_ACTIVITY = new ActivityData();
   private long cachedMaxActivity = -1L;
   private LocalDate cachedStatsDate = null;
   private LocalDate cachedCalendarDate = null;
   private LocalDate cachedStartDate = null;
   private LocalDate cachedEndDate = null;
   private String[] cachedDateStrings = null;
   private LocalDate[] cachedDates = null;
   private ActivityData[] cachedActivities = null;
   private final ActivityData currentDayActivityBuffer = new ActivityData();
   private final String[] cachedDayLabels = new String[7];
   private String cachedLanguage = null;
   private String cachedLessText = "";
   private String cachedMoreText = "";
   private String cachedNoActivityText = "";
   private String cachedHoursMinutesPattern = "";
   private String cachedMinutesPattern = "";
   private String cachedTotalPlaytimeLine = "";
   private long cachedTotalPlaytimeUpdateMs = 0L;
   public String hoveredDate = null;
   public ActivityData hoveredActivity = null;

   public ActivityCalendarComponent(ActivityManager activityManager) {
      this.activityManager = activityManager;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.updateTextCaches();
      this.renderCalendarGrid(context, mouseX, mouseY);
      Fonts.getSize(12).drawString(matrix, this.cachedTotalPlaytimeLine, this.x + 4.0F, this.y + 55.0F, ColorUtil.getDescription());
   }

   private void renderCalendarGrid(DrawContext context, int mouseX, int mouseY) {
      MatrixStack matrix = context.getMatrices();
      LocalDate today = LocalDate.now();
      this.weeksToShow = 52;
      LocalDate startDate = today.minusWeeks(this.weeksToShow - 1);

      while (startDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
         startDate = startDate.minusDays(1L);
      }

      if (this.cachedStatsDate == null
         || !this.cachedStatsDate.equals(today)
         || this.cachedCalendarDate == null
         || !this.cachedCalendarDate.equals(today)
         || this.cachedStartDate == null
         || !this.cachedStartDate.equals(startDate)) {
         this.cachedMaxActivity = this.calculateYearlyMaxActivity(today);
         this.cachedStatsDate = today;
         this.cachedCalendarDate = today;
         this.cachedStartDate = startDate;
         this.cachedEndDate = today;
         int totalDays = this.weeksToShow * 7;
         this.cachedDates = new LocalDate[totalDays];
         this.cachedDateStrings = new String[totalDays];
         this.cachedActivities = new ActivityData[totalDays];
         LocalDate date = startDate;

         for (int index = 0; !date.isAfter(today) && index < totalDays; index++) {
            this.cachedDates[index] = date;
            this.cachedDateStrings[index] = date.format(DATE_FORMATTER);
            this.cachedActivities[index] = this.activityManager.getActivityForDate(this.cachedDateStrings[index]);
            date = date.plusDays(1L);
         }
      }

      float calendarX = this.x + 15.0F;
      float calendarY = this.y + 15.0F;

      for (int day = 0; day < 7; day++) {
         String dayLabel = this.cachedDayLabels[day];
         if (!dayLabel.isEmpty()) {
            Fonts.getSize(8).drawString(matrix, dayLabel, calendarX - 12.0F, calendarY + day * 5.15F + 2.0F, ColorUtil.getDescription());
         }
      }

      this.renderMonthLabels(matrix, startDate, calendarX, calendarY - 10.0F);
      this.hoveredDate = null;
      this.hoveredActivity = null;
      float cellSpacingTotal = 5.15F;
      boolean hasActiveSession = this.activityManager.getSessionStartTime() > 0L;
      int baseRectColor = ColorUtil.getRectDarker(1.0F);
      int accentColor = ColorUtil.getClientColor();
      int validDays = this.cachedDates != null ? this.cachedDates.length : 0;

      for (int i = 0; i < validDays; i++) {
         LocalDate currentDate = this.cachedDates[i];
         if (currentDate == null || currentDate.isAfter(today)) {
            break;
         }

         int week = i / 7;
         int dayx = i % 7;
         float cellX = calendarX + week * cellSpacingTotal;
         float cellY = calendarY + dayx * cellSpacingTotal;
         String dateString = this.cachedDateStrings[i];
         ActivityData activity = this.cachedActivities != null ? this.cachedActivities[i] : null;
         if (activity == null) {
            activity = EMPTY_ACTIVITY;
         }

         if (currentDate.equals(today)) {
            ActivityData todayActivity = this.activityManager.getActivities().get(dateString);
            activity = todayActivity != null ? todayActivity : EMPTY_ACTIVITY;
         }

         if (currentDate.equals(today) && hasActiveSession) {
            activity = this.getCurrentDayActivityWithSession(dateString, activity);
         }

         boolean isHovered = MathUtil.isHovered(mouseX, mouseY, cellX, cellY, 4.0, 4.0);
         if (isHovered) {
            this.hoveredDate = dateString;
            this.hoveredActivity = activity;
         }

         int color = this.getIntensityColorOptimized(activity, isHovered, baseRectColor, accentColor);
         this.drawCellFast(context, cellX, cellY, 4.0F, color);
      }

      this.renderLegend(context, calendarX + 103.5F, calendarY + 36.05F + 5.0F);
   }

   private void renderLegend(DrawContext context, float legendX, float legendY) {
      MatrixStack matrix = context.getMatrices();
      String lessText = this.cachedLessText;
      String moreText = this.cachedMoreText;
      float lessTextWidth = Fonts.getSize(8).getStringWidth(lessText);
      float moreTextWidth = Fonts.getSize(8).getStringWidth(moreText);
      float squaresWidth = 24.0F;
      float totalWidth = lessTextWidth + 5.0F + squaresWidth + 5.0F + moreTextWidth;
      float startX = legendX + (this.weeksToShow * 5.15F - totalWidth) / 2.0F;
      Fonts.getSize(8).drawString(matrix, lessText, startX, legendY, ColorUtil.getDescription());
      float squareStartX = startX + lessTextWidth + 5.0F;

      for (int i = 0; i <= 4; i++) {
         float cellX = squareStartX + i * 5.0F;
         int baseRectColor = ColorUtil.getRectDarker(1.0F);
         int accentColor = ColorUtil.getClientColor();
         int color;
         if (i == 0) {
            color = baseRectColor;
         } else {
            float intensity = INTENSITY_LEVELS[i];
            color = ColorUtil.overCol(baseRectColor, accentColor, intensity);
         }

         this.drawCellFast(context, cellX, legendY - 2.0F, 4.0F, color);
      }

      float moreX = squareStartX + squaresWidth + 5.0F;
      Fonts.getSize(8).drawString(matrix, moreText, moreX, legendY, ColorUtil.getDescription());
   }

   public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
      String dateText = this.formatDateForTooltip(this.hoveredDate);
      String timeText = this.formatPlayTimeForTooltip(this.hoveredActivity.getTotalPlayTime());
      int textWidth = (int)Math.max(Fonts.getSize(12).getStringWidth(dateText), Fonts.getSize(11).getStringWidth(timeText));
      int tooltipWidth = textWidth + 12;
      int tooltipHeight = 26;
      int tooltipX = mouseX + 10;
      int tooltipY = mouseY - tooltipHeight - 10;
      if (tooltipX + tooltipWidth > window.getScaledWidth()) {
         tooltipX = mouseX - tooltipWidth - 10;
      }

      if (tooltipY < 0) {
         tooltipY = mouseY + 20;
      }

      MatrixStack matrix = context.getMatrices();
      matrix.push();
      matrix.translate(0.0F, 0.0F, 400.0F);
      rectangle.render(
         ShapeProperties.create(matrix, tooltipX, tooltipY, tooltipWidth, tooltipHeight)
            .round(4.0F)
            .color(ColorUtil.getGuiRectColor(0.9F))
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .build()
      );
      int dateCenterX = tooltipX + (tooltipWidth - (int)Fonts.getSize(12).getStringWidth(dateText)) / 2;
      int timeCenterX = tooltipX + (tooltipWidth - (int)Fonts.getSize(11).getStringWidth(timeText)) / 2;
      Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawString(matrix, dateText, dateCenterX, tooltipY + 6, ColorUtil.getDescription());
      Fonts.getSize(11).drawString(matrix, timeText, timeCenterX, tooltipY + 18, ColorUtil.getText());
      matrix.pop();
   }

   private int getIntensityColorOptimized(ActivityData activity, boolean hovered, int baseRectColor, int accentColor) {
      int level = this.getRelativeIntensityLevel(activity);
      int baseColor;
      if (level == 0) {
         baseColor = baseRectColor;
      } else {
         float intensity = INTENSITY_LEVELS[Math.max(0, Math.min(4, level))];
         baseColor = ColorUtil.overCol(baseRectColor, accentColor, intensity);
      }

      if (hovered) {
         baseColor = ColorUtil.lighter(baseColor, 2);
      }

      return baseColor;
   }

   private int getIntensityColor(ActivityData activity, boolean hovered) {
      return this.getIntensityColorOptimized(activity, hovered, ColorUtil.getRectDarker(1.0F), ColorUtil.getClientColor());
   }

   private int getRelativeIntensityLevel(ActivityData activity) {
      if (activity.getTotalPlayTime() == 0L) {
         return 0;
      } else if (this.cachedMaxActivity == 0L) {
         return 0;
      } else {
         long currentScore = activity.getTotalPlayTime() / 60000L + activity.getSessionsCount() * 5L;
         double intensity = (double)currentScore / this.cachedMaxActivity;
         if (intensity <= 0.0) {
            return 0;
         } else if (intensity <= 0.25) {
            return 1;
         } else if (intensity <= 0.5) {
            return 2;
         } else {
            return intensity <= 0.75 ? 3 : 4;
         }
      }
   }

   private long calculateYearlyMaxActivity(LocalDate today) {
      long maxActivity = 0L;
      LocalDate startDate = today.minusYears(1L);

      for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1L)) {
         String dateString = date.format(DATE_FORMATTER);
         ActivityData dayActivity = this.activityManager.getActivities().get(dateString);
         if (dayActivity != null && dayActivity.getTotalPlayTime() > 0L) {
            long dayScore = dayActivity.getTotalPlayTime() / 60000L + dayActivity.getSessionsCount() * 5L;
            maxActivity = Math.max(maxActivity, dayScore);
         }
      }

      return maxActivity;
   }

   private String formatDateForTooltip(String dateString) {
      LocalDate date = LocalDate.parse(dateString);
      return date.format(TOOLTIP_DATE_FORMATTER);
   }

   private String formatPlayTimeForTooltip(long playTimeMs) {
      if (playTimeMs == 0L) {
         return this.cachedNoActivityText;
      } else {
         long hours = playTimeMs / 3600000L;
         long minutes = playTimeMs % 3600000L / 60000L;
         return hours > 0L ? String.format(this.cachedHoursMinutesPattern, hours, minutes) : String.format(this.cachedMinutesPattern, minutes);
      }
   }

   private void renderMonthLabels(MatrixStack matrix, LocalDate startDate, float calendarX, float monthLabelY) {
      LocalDate currentMonth = null;

      for (int week = 0; week < this.weeksToShow; week++) {
         LocalDate weekStart = startDate.plusWeeks(week);
         if (currentMonth == null || !weekStart.getMonth().equals(currentMonth.getMonth()) || weekStart.getYear() != currentMonth.getYear()) {
            currentMonth = weekStart;
            String monthKey = "calendar.month." + weekStart.getMonth().name().substring(0, 3).toLowerCase();
            String monthName = LocalizationManager.getInstance().get(monthKey);
            Fonts.getSize(8).drawString(matrix, monthName, calendarX + week * 5.15F, monthLabelY + 5.0F, ColorUtil.getDescription());
         }
      }
   }

   private ActivityData getCurrentDayActivityWithSession(String dateString, ActivityData activity) {
      long sessionStart = this.activityManager.getSessionStartTime();
      if (sessionStart > 0L) {
         long currentTime = System.currentTimeMillis();
         long currentSessionTime = currentTime - sessionStart;
         this.currentDayActivityBuffer.setDate(dateString);
         this.currentDayActivityBuffer.setTotalPlayTime(activity.getTotalPlayTime() + currentSessionTime);
         this.currentDayActivityBuffer.setSessionsCount(activity.getSessionsCount() + 1);
         this.currentDayActivityBuffer.setLoginTime(activity.getLoginTime() != 0L ? activity.getLoginTime() : sessionStart);
         this.currentDayActivityBuffer.setLogoutTime(currentTime);
         return this.currentDayActivityBuffer;
      } else {
         return activity;
      }
   }

   private void drawCellFast(DrawContext context, float x, float y, float size, int color) {
      int x1 = (int)x;
      int y1 = (int)y;
      int x2 = Math.max(x1 + 1, (int)(x + size));
      int y2 = Math.max(y1 + 1, (int)(y + size));
      int finalColor = ColorUtil.multAlpha(color, RenderSystem.getShaderColor()[3]);
      context.fill(x1, y1, x2, y2, finalColor);
   }

   private void updateTextCaches() {
      LocalizationManager localization = LocalizationManager.getInstance();
      String currentLanguage = localization.getCurrentLanguage();
      if (!currentLanguage.equals(this.cachedLanguage)) {
         this.cachedLanguage = currentLanguage;
         this.cachedLessText = localization.get("ui.less");
         this.cachedMoreText = localization.get("ui.more");
         this.cachedNoActivityText = localization.get("calendar.no_activity");
         this.cachedHoursMinutesPattern = localization.get("calendar.hours_minutes_played");
         this.cachedMinutesPattern = localization.get("calendar.minutes_played");

         for (int i = 0; i < DAY_LABEL_KEYS.length; i++) {
            this.cachedDayLabels[i] = DAY_LABEL_KEYS[i].isEmpty() ? "" : localization.get(DAY_LABEL_KEYS[i]);
         }

         this.cachedTotalPlaytimeUpdateMs = 0L;
      }

      long now = System.currentTimeMillis();
      if (this.cachedTotalPlaytimeUpdateMs == 0L || now - this.cachedTotalPlaytimeUpdateMs >= 1000L) {
         String totalTime = this.activityManager.getFormattedTotalPlayTime();
         this.cachedTotalPlaytimeLine = localization.get("ui.total_playtime").replace("%s", totalTime);
         this.cachedTotalPlaytimeUpdateMs = now;
      }
   }

   public ActivityCalendarComponent setWeeksToShow(int weeksToShow) {
      this.weeksToShow = weeksToShow;
      return this;
   }

   public ActivityCalendarComponent setCachedMaxActivity(long cachedMaxActivity) {
      this.cachedMaxActivity = cachedMaxActivity;
      return this;
   }

   public ActivityCalendarComponent setCachedStatsDate(LocalDate cachedStatsDate) {
      this.cachedStatsDate = cachedStatsDate;
      return this;
   }

   public ActivityCalendarComponent setCachedCalendarDate(LocalDate cachedCalendarDate) {
      this.cachedCalendarDate = cachedCalendarDate;
      return this;
   }

   public ActivityCalendarComponent setCachedStartDate(LocalDate cachedStartDate) {
      this.cachedStartDate = cachedStartDate;
      return this;
   }

   public ActivityCalendarComponent setCachedEndDate(LocalDate cachedEndDate) {
      this.cachedEndDate = cachedEndDate;
      return this;
   }

   public ActivityCalendarComponent setCachedDateStrings(String[] cachedDateStrings) {
      this.cachedDateStrings = cachedDateStrings;
      return this;
   }

   public ActivityCalendarComponent setCachedDates(LocalDate[] cachedDates) {
      this.cachedDates = cachedDates;
      return this;
   }

   public ActivityCalendarComponent setCachedActivities(ActivityData[] cachedActivities) {
      this.cachedActivities = cachedActivities;
      return this;
   }

   public ActivityCalendarComponent setCachedLanguage(String cachedLanguage) {
      this.cachedLanguage = cachedLanguage;
      return this;
   }

   public ActivityCalendarComponent setCachedLessText(String cachedLessText) {
      this.cachedLessText = cachedLessText;
      return this;
   }

   public ActivityCalendarComponent setCachedMoreText(String cachedMoreText) {
      this.cachedMoreText = cachedMoreText;
      return this;
   }

   public ActivityCalendarComponent setCachedNoActivityText(String cachedNoActivityText) {
      this.cachedNoActivityText = cachedNoActivityText;
      return this;
   }

   public ActivityCalendarComponent setCachedHoursMinutesPattern(String cachedHoursMinutesPattern) {
      this.cachedHoursMinutesPattern = cachedHoursMinutesPattern;
      return this;
   }

   public ActivityCalendarComponent setCachedMinutesPattern(String cachedMinutesPattern) {
      this.cachedMinutesPattern = cachedMinutesPattern;
      return this;
   }

   public ActivityCalendarComponent setCachedTotalPlaytimeLine(String cachedTotalPlaytimeLine) {
      this.cachedTotalPlaytimeLine = cachedTotalPlaytimeLine;
      return this;
   }

   public ActivityCalendarComponent setCachedTotalPlaytimeUpdateMs(long cachedTotalPlaytimeUpdateMs) {
      this.cachedTotalPlaytimeUpdateMs = cachedTotalPlaytimeUpdateMs;
      return this;
   }

   public ActivityCalendarComponent setHoveredDate(String hoveredDate) {
      this.hoveredDate = hoveredDate;
      return this;
   }

   public ActivityCalendarComponent setHoveredActivity(ActivityData hoveredActivity) {
      this.hoveredActivity = hoveredActivity;
      return this;
   }
}
