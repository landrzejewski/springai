package pl.training.ai.chat;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public class DateTimeTools {

    private final Logger logger = Logger.getLogger(DateTimeTools.class.getName());

    @Tool(description = "Get the current date and time in the user's timezone", returnDirect = true)
    public String getCurrentTime(ToolContext toolContext) {
        logger.info("User id: " + toolContext.getContext().get("userId"));
        logger.info("Get the current date and time in the user's timezone");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "Get the current date and time in the specified timezone", name = "getCurrentTimeWithTimeZone")
    public String getCurrentTime(String timeZone) {
        logger.info("Get the current date and time in the specified timezone");
        try {
            var zoneId = ZoneId.of(timeZone);
            var dateTime = ZonedDateTime.now(zoneId);
            return dateTime.toString();
        } catch (Exception e) {
            return "Invalid time zone "  + timeZone;
        }
    }

}
