package net.gamedoctor.hakaton.ai;

import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import net.gamedoctor.hakaton.TGBot;
import net.gamedoctor.hakaton.cache.ChatAdmin;
import net.gamedoctor.hakaton.data.CheckType;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@RequiredArgsConstructor
public class PunishmentsManager {
    private final TGBot tgBot;
    private final BlockingDeque<Pair<Message, Boolean>> pendingToCheck = new LinkedBlockingDeque<>();
    private final HashMap<Long, Message> waitingForPreModeation = new HashMap<>();

    private CheckType checkMessageAndUser(Message message, boolean onlyUser) {
        User author = message.getFrom();
        long chatID = message.getChatId();
        String text = "-";
        if (!onlyUser)
            text = message.getText();
        if (!isAdmin(chatID, author.getId()) && !tgBot.getDatabaseManager().getWhitelistCache().getOrDefault(chatID, new ArrayList<>()).contains(author.getId()) && !tgBot.getDatabaseManager().getActiveChecksType().getOrDefault(chatID, new ArrayList<>()).isEmpty()) {
            int counter = 0;
            String chatTitle = tgBot.getDatabaseManager().getRegisteredChatsCache().get(chatID).getName();
            StringBuilder reasons = new StringBuilder(counter + " - Нарушений нет");
            HashMap<Integer, CheckType> idToReason = new HashMap<>();

            for (Pair<CheckType, String> checkType : tgBot.getDatabaseManager().getActiveChecksType().get(chatID)) {
                if(checkType.getFirst().equals(CheckType.PRE_MODERATION))
                    continue;
                counter++;
                reasons.append(", ").append(counter).append(" - ").append(checkType.getFirst().getName());
                idToReason.put(counter, checkType.getFirst());
            }

            int result = Integer.parseInt(ChatGPT.makeGPTRequest("Ты - бот для обнаружения спама. Не блокируй за маты и шуточные оскорбления, только за открытую ненависть. В ответ верни только цифру в зависимости от результата обнаружения. " + reasons + ".\n"
                    + "Название чата: " + chatTitle + "\n" +
                    "Данные об авторе сообщения: имя:" + author.getFirstName() + ", фамилия:" + author.getLastName() + ", юзернейм:" + author.getUserName() + "\n" +
                    "Данные о сообщении: '" + text + "'"));

            return idToReason.getOrDefault(result, null);
        } else {
            return null;
        }
    }

    public void addToQueue(Pair<Message, Boolean> pair, boolean preModeation) {
        if (preModeation)
            waitingForPreModeation.put(pair.getFirst().getChatId(), pair.getFirst());

        pendingToCheck.add(pair);
    }

    public boolean isAdmin(long chatID, long userID) {
        for (ChatAdmin chatAdmin : tgBot.getChatAdmins(chatID)) {
            if (chatAdmin.getUserID() == userID) {
                return true;
            }
        }

        return false;
    }

    public void ban(Message message, CheckType checkType) {
        if (checkType != null) {
            try {
                try {
                    tgBot.getTelegramClient().execute(DeleteMessage.builder().chatId(message.getChatId()).messageId(message.getMessageId()).build());
                } catch (Exception ignored) {
                }
                tgBot.getTelegramClient().execute(BanChatMember.builder().chatId(message.getChatId()).userId(message.getFrom().getId()).revokeMessages(false).build());
                tgBot.sendMessage(message.getChatId(), "Пользователь " + tgBot.getUsernameByID(message.getFrom().getId()) + " наказан по причине: " + checkType.getName());
                tgBot.getDatabaseManager().addPunishToLogs(message, checkType);
            } catch (Exception ignored) {
                tgBot.sendMessage(message.getChatId(), "Не удалось исключить пользователя " + tgBot.getUsernameByID(message.getFrom().getId()) + " за " + checkType.getName());
            }
        }
    }

    public void runQueue() {
        new Thread(() -> {
            while (true) {
                if (!pendingToCheck.isEmpty()) {
                    try {
                        Pair<Message, Boolean> pair = pendingToCheck.take();
                        Message message = pair.getFirst();
                        CheckType checkType = checkMessageAndUser(message, pair.getSecond());
                        System.out.println(checkType);
                        if (checkType == null && waitingForPreModeation.containsKey(pair.getFirst().getChatId())) {
                            Message msg = waitingForPreModeation.remove(pair.getFirst().getChatId());
                            tgBot.sendMessage(msg.getChatId(), "Сообщение от " + tgBot.getUsernameByID(msg.getFrom().getId()) + ":\n\n" + msg.getText());
                        } else if (checkType != null) {
                            ban(message, checkType);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
