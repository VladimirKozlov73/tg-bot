package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTaskService {

    private final NotificationTaskRepository repository;
    private final TelegramBot telegramBot;


    public NotificationTaskService(NotificationTaskRepository repository, TelegramBot telegramBot) {
        this.repository = repository;
        this.telegramBot = telegramBot;
    }

    public void processMessage(Long chatId, String message) {
        String pattern = "(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(message);
        if (m.find()) {
            String dateTimeStr = m.group(1);
            String notificationText = m.group(3);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

            NotificationTask task = new NotificationTask(chatId, notificationText, dateTime);
            repository.save(task);
        } else {
            throw new IllegalArgumentException("Неверный формат сообщения: " + message);
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void checkDueNotifications() {
        LocalDateTime nowMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> dueTasks = repository.findAllByNotificationTime(nowMinute);

        for (NotificationTask task : dueTasks) {
            SendMessage sendMessage = new SendMessage(task.getChatId(), task.getNotificationText());
            telegramBot.execute(sendMessage);

        }
    }
}
