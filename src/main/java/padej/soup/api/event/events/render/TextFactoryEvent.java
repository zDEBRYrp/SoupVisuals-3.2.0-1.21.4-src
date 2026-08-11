package padej.soup.api.event.events.render;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import padej.soup.api.event.events.Event;

public class TextFactoryEvent implements Event {
   private static final Map<String, Pattern> PATTERN_CACHE = new HashMap<>();
   private String text;

   public void replaceText(String protect, String replaced) {
      if (this.text != null && !this.text.isEmpty() && protect != null && replaced != null) {
         if (this.text.equalsIgnoreCase(protect)) {
            this.text = replaced;
         } else {
            Pattern compiledPattern = PATTERN_CACHE.computeIfAbsent(protect, p -> Pattern.compile("(?i)\\b" + Pattern.quote(p) + "\\b"));
            this.text = compiledPattern.matcher(this.text).replaceAll(Matcher.quoteReplacement(replaced));
            this.text = this.text.replace("⏏" + protect, "⏏" + replaced);
            this.text = this.text.replace(protect + "§", replaced + "§");
         }
      }
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public TextFactoryEvent(String text) {
      this.text = text;
   }
}
