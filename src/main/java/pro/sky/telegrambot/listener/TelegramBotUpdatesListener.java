package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final NotificationTaskService notificationTaskService;

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskService notificationTaskService) {
        this.notificationTaskService = notificationTaskService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
            if (update.message() != null) {
                long chatId = update.message().chat().id();
                String text = update.message().text();

                if ("/start".equals(text)) {
                    telegramBot.execute(new SendMessage(chatId, "Привет! Добро пожаловать в моего Telegram бота."));
                } else {
                    try {
                        notificationTaskService.processMessage(chatId, text);
                        telegramBot.execute(new SendMessage(chatId, "Напоминание сохранено!"));
                    } catch (IllegalArgumentException e) {
                        logger.error("Ошибка при обработке сообщения: {}", text, e);
                        telegramBot.execute(new SendMessage(chatId, "Ошибка: неверный формат сообщения."));
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
