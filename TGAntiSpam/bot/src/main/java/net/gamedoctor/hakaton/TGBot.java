package net.gamedoctor.hakaton;

import kotlin.Pair;
import lombok.Getter;
import net.gamedoctor.hakaton.ai.PunishmentsManager;
import net.gamedoctor.hakaton.cache.ChatAdmin;
import net.gamedoctor.hakaton.data.CheckType;
import net.gamedoctor.hakaton.data.Config;
import net.gamedoctor.hakaton.data.DatabaseManager;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class TGBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final Config cfg;
    private final DatabaseManager databaseManager;
    private final PunishmentsManager punishmentsManager;
    private final HashMap<Long, Long> lastMessagesDate = new HashMap<>();
    private final HashMap<Long, Long> lastTokenGetDate = new HashMap<>();

    public TGBot() {
        cfg = new Config();
        telegramClient = new OkHttpTelegramClient(cfg.getBotToken());
        databaseManager = new DatabaseManager(this);
        punishmentsManager = new PunishmentsManager(this);
        punishmentsManager.runQueue();
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(cfg.getBotToken(), this);
            System.out.println("Бот успешно запущен!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        //message.setReplyMarkup(new ReplyKeyboardRemove(true));

        try {
            Message m = telegramClient.execute(message);
            return m.getMessageId();
        } catch (Exception ignored) {
        }

        return -1;
    }

    public int getChatMembersCount(long chatID) {
        try {
            return telegramClient.execute(GetChatMemberCount.builder().chatId(chatID).build());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public Chat getChat(long chatID) {
        try {
            return telegramClient.execute(GetChat.builder().chatId(chatID).build());
        } catch (Exception e) {
            return null;
        }
    }

    public List<ChatAdmin> getChatAdmins(long chatID) {
        try {
            List<ChatAdmin> admins = new ArrayList<>();

            for (ChatMember member : telegramClient.execute(GetChatAdministrators.builder().chatId(chatID).build())) {
                admins.add(new ChatAdmin(member.getUser().getId(), member.getStatus(), member instanceof ChatMemberOwner));
            }

            return admins;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void pinAlert(long chatId) {
        try {
            telegramClient.execute(PinChatMessage.builder().chatId(chatId).messageId(sendMessage(chatId, "Обратите внимение - данный бот ведёт сбор таких данных, как:\n\n1) Айди пользователя\n2) Юзернейм пользователя\n3) Имя пользователя")).build());
        } catch (Exception ignored) {

        }
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            long chatID = update.getMessage().getChatId();
            long userID = update.getMessage().getFrom().getId();
            Message message = update.getMessage();
            System.out.println(update.getMessage().toString());

            if (!message.getChat().isUserChat()) {
                if (!message.getNewChatMembers().isEmpty()) {
                    for (User user : message.getNewChatMembers()) {
                        if (user.getId() == cfg.getBotID()) {
                            if (!databaseManager.isChatRegistered(chatID)) {
                                databaseManager.registerChat(chatID);
                                sendMessage(chatID, "Я здесь впервые. Чат успешно зарегистрирован");
                                pinAlert(chatID);
                            } else {
                                databaseManager.markNewStatus(chatID, "ACTIVE");
                                //databaseManager.getRegisteredChatsCache().put(chatID, databaseManager.getChatData(chatID));
                                sendMessage(chatID, "Я здесь уже был, привет");
                                pinAlert(chatID);
                            }
                        } else {
                            // логика проверки новых пользователей
                            punishmentsManager.addToQueue(new Pair<>(message, true), false);
                        }
                    }
                }

                if (update.getMessage().getLeftChatMember() != null) {
                    if (update.getMessage().getLeftChatMember().getId() == cfg.getBotID()) {
                        databaseManager.markNewStatus(chatID, "INACTIVE");
                    }
                }

                // логика проверки сообщений
                if (update.getMessage().hasText()) {
                    if (databaseManager.isCheckContains(chatID, CheckType.FLOOD) && System.currentTimeMillis() - lastMessagesDate.getOrDefault(userID, 0L) <= 200L) {
                        punishmentsManager.ban(message, CheckType.FLOOD);
                    } else {
                        lastMessagesDate.put(userID, System.currentTimeMillis());
                        boolean preModeration = false;
                        if (databaseManager.isCheckContains(chatID, CheckType.PRE_MODERATION)) {
                            preModeration = true;
                            try {
                                telegramClient.execute(DeleteMessage.builder().chatId(chatID).messageId(message.getMessageId()).build());
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        punishmentsManager.addToQueue(new Pair<>(message, false), preModeration);
                    }
                }
            } else {
                if (message.getText().equalsIgnoreCase("/auth")) {
                    if (System.currentTimeMillis() - lastTokenGetDate.getOrDefault(userID, 0L) <= 1000L * 20) {
                        sendMessage(chatID, "Вы не можете запрашивать токен авторизации так часто");
                    } else {
                        lastTokenGetDate.put(userID, System.currentTimeMillis());
                        sendMessage(chatID, "Ваш токен авторизации: " + databaseManager.getNewAuthToken(userID) + "\nНикому его не показывайте!\n\nДля использования токена перейдите на сайт hakaton.kosfarix.ru");
                    }
                } else {
                    sendMessage(chatID, "Добавьте меня в чат, чтобы начать работу. Для доступа к Панели Администратора введите команду /auth");
                }
            }
        }
    }

    public String getUsernameByID(Long userId) {
        try {
            GetChat getChat = new GetChat(userId.toString());
            Chat chat = telegramClient.execute(getChat);
            if (chat.getUserName() == null || chat.getUserName().equalsIgnoreCase("null")) {
                return "СКРЫТО";
            } else {
                return "@" + chat.getUserName();
            }
        } catch (TelegramApiException e) {
            return userId.toString();
        }
    }
}