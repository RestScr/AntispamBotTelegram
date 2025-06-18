package net.gamedoctor.hakaton.data;

import kotlin.Pair;
import lombok.Getter;
import net.gamedoctor.hakaton.TGBot;
import net.gamedoctor.hakaton.cache.ChatAdmin;
import net.gamedoctor.hakaton.cache.ChatData;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final TGBot tgBot;
    private final String userDataTable = "userdata";
    private final String chatsTable = "chats";
    private final String chatAdminsTable = "chat_admins";
    private final String whitelistTable = "whitelist";
    private final String filtersTable = "filters";
    private final String punishLogsTable = "punish_logs";
    private final String authTokensTable = "auth_tokens";
    @Getter
    private final HashMap<Long, ChatData> registeredChatsCache = new HashMap<>();
    @Getter
    private final HashMap<Long, List<Long>> whitelistCache = new HashMap<>();
    @Getter
    private final HashMap<Long, List<Pair<CheckType, String>>> activeChecksType = new HashMap<>();
    private Connection connection;

    public DatabaseManager(TGBot tgBot) {
        this.tgBot = tgBot;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + tgBot.getCfg().getDatabase_host() + "/" + tgBot.getCfg().getDatabase_database() + tgBot.getCfg().getDatabase_arguments(), tgBot.getCfg().getDatabase_user(), tgBot.getCfg().getDatabase_password());

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + chatsTable + " WHERE status = ?");
            preparedStatement.setString(1, "ACTIVE");
            ResultSet set = preparedStatement.executeQuery();

            while (set.next()) {
                registeredChatsCache.put(set.getLong("tg_id"), getChatData(set.getLong("tg_id")));
            }

            System.out.println("В кеш загружено " + registeredChatsCache.size() + " чатов");
        } catch (Exception e) {
            e.printStackTrace();
        }

        keepAlive();
        cacheUpdater();
        whitelistUpdater();
        activeChecksTypeUpdater();
    }

    public boolean isCheckContains(long chatID, CheckType checkType) {
        for (Pair<CheckType, String> pair : activeChecksType.getOrDefault(chatID, new ArrayList<>())) {
            if (pair.getFirst().equals(checkType)) {
                return true;
            }
        }

        return false;
    }

    private void whitelistUpdater() {
        new Thread(() -> {
            while (true) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + whitelistTable);
                    ResultSet set = preparedStatement.executeQuery();

                    while (set.next()) {
                        long chatID = set.getLong("chatID");
                        long userID = set.getLong("tg_id");

                        List<Long> whitelist = whitelistCache.getOrDefault(chatID, new ArrayList<>());
                        whitelist.add(userID);

                        whitelistCache.put(chatID, whitelist);
                    }
                } catch (Exception ignored) {

                }

                try {
                    Thread.sleep(1000L * 5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void activeChecksTypeUpdater() {
        new Thread(() -> {
            while (true) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + filtersTable);
                    ResultSet set = preparedStatement.executeQuery();

                    activeChecksType.clear();
                    while (set.next()) {
                        long chatID = set.getLong("chatID");
                        CheckType checkType = CheckType.valueOf(set.getString("type"));

                        List<Pair<CheckType, String>> checksList = activeChecksType.getOrDefault(chatID, new ArrayList<>());


                        checksList.add(new Pair<>(checkType, set.getString("data")));
                        activeChecksType.put(chatID, checksList);
                    }

                } catch (Exception sd) {
                    sd.printStackTrace();
                }

                try {
                    Thread.sleep(1000L * 5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void updateChatsCache() {
        try {
            for (long chatID : registeredChatsCache.keySet()) {

                ChatData oldChatData = getChatData(chatID);
                Chat chat = tgBot.getChat(chatID);
                if (chat != null) {
                    ChatData newChatData = new ChatData(oldChatData.getChatID(), chat.getTitle(), oldChatData.getRegDate(), tgBot.getChatMembersCount(chat.getId()), tgBot.getChatAdmins(chatID));
                    updateChatData(newChatData);
                    registeredChatsCache.put(chatID, newChatData);
                } else {
                    registeredChatsCache.remove(chatID);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public ChatData getChatData(long chatId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + chatsTable + " WHERE tg_id = ?");
            preparedStatement.setLong(1, chatId);

            ResultSet set = preparedStatement.executeQuery();

            if (set.next()) {
                return new ChatData(chatId, set.getString("name"), set.getLong("addDate"), set.getInt("usersCount"), getChatAdmins(set.getInt("id")));
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public void updateChatData(ChatData chatData) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + chatsTable + " SET name = ?, usersCount = ? WHERE tg_id = ?");
            preparedStatement.setString(1, chatData.getName());
            preparedStatement.setLong(2, chatData.getUsersCount());
            preparedStatement.setLong(3, chatData.getChatID());
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement("DELETE FROM " + chatAdminsTable + " WHERE chat_id = ?");
            preparedStatement.setLong(1, chatData.getChatID());
            preparedStatement.executeUpdate();

            for (ChatAdmin chatAdmin : chatData.getAdmins()) {
                System.out.println(chatAdmin.getUserID() + "_" + chatAdmin.getPrefix() + "_" + chatData.getChatID());
                preparedStatement = connection.prepareStatement("INSERT INTO " + chatAdminsTable + " (`chat_id`, `tg_id`, `owner`) VALUES (?, ?, ?)");
                preparedStatement.setLong(1, chatData.getChatID());
                preparedStatement.setLong(2, chatAdmin.getUserID());
                preparedStatement.setBoolean(3, chatAdmin.isOwner());
                preparedStatement.executeUpdate();
            }
        } catch (Exception ed) {
            ed.printStackTrace();
        }
    }

    public List<ChatAdmin> getChatAdmins(int chatID) {
        List<ChatAdmin> chatAdmins = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + chatAdminsTable + " WHERE chat_id = ?");
            preparedStatement.setLong(1, chatID);

            ResultSet set = preparedStatement.executeQuery();
            while (set.next()) {
                chatAdmins.add(new ChatAdmin(set.getLong("tg_id"), set.getString("prefix"), set.getBoolean("owner")));
            }
        } catch (Exception ignored) {
        }

        return chatAdmins;
    }

    public ChatData getNewChatData(long tg_id) {
        Chat chat = tgBot.getChat(tg_id);
        return new ChatData(tg_id, chat.getTitle(), System.currentTimeMillis(), tgBot.getChatMembersCount(chat.getId()), tgBot.getChatAdmins(tg_id));
    }

    public boolean isUserExists(long tgID) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + userDataTable + " WHERE tg_id=?");
            preparedStatement.setLong(1, tgID);
            ResultSet set = preparedStatement.executeQuery();

            return set.next() && set.getInt("COUNT(*)") != 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isChatRegistered(long tgID) {
        if (registeredChatsCache.containsKey(tgID))
            return true;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + chatsTable + " WHERE tg_id=?");
            preparedStatement.setLong(1, tgID);
            ResultSet set = preparedStatement.executeQuery();

            return set.next() && set.getInt("COUNT(*)") != 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void registerAdmin(long tgID) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + userDataTable + " (`tg_id`) VALUES (?)");
            preparedStatement.setLong(1, tgID);
            preparedStatement.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public void addPunishToLogs(Message message, CheckType checkType) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + punishLogsTable + " (`chatID`, `targetID`, `type`, `reason`, `date`) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setLong(1, message.getChatId());
            preparedStatement.setLong(2, message.getFrom().getId());
            preparedStatement.setString(3, checkType.toString());
            preparedStatement.setString(4, checkType.getName());
            preparedStatement.setLong(5, System.currentTimeMillis());
            preparedStatement.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public String getNewAuthToken(long userID) {
        try {
            String token = UUID.randomUUID().toString();
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + authTokensTable + " (`tg_id`, `token`, `date`) VALUES (?, ?, ?)");
            preparedStatement.setLong(1, userID);
            preparedStatement.setString(2, token);
            preparedStatement.setLong(3, System.currentTimeMillis());
            preparedStatement.executeUpdate();

            return token;
        } catch (Exception ignored) {
        }

        return "-";
    }

    public void markNewStatus(long tgID, String status) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + chatsTable + " SET status = ? WHERE tg_id = ?");
            preparedStatement.setString(1, status);
            preparedStatement.setLong(2, tgID);
            preparedStatement.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public void registerChat(long tgID) {
        try {
            ChatData chatData = getNewChatData(tgID);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + chatsTable + " (`tg_id`, `name`, `addDate`, `status`, `usersCount`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, chatData.getChatID());
            preparedStatement.setString(2, chatData.getName());
            preparedStatement.setLong(3, chatData.getRegDate());
            preparedStatement.setString(4, "ACTIVE");
            preparedStatement.setInt(5, chatData.getUsersCount());
            preparedStatement.executeUpdate();

            ResultSet set = preparedStatement.getGeneratedKeys();
            if (set.next()) {
                int chatID = set.getInt(1);
                for (ChatAdmin chatAdmin : tgBot.getChatAdmins(tgID)) {
                    preparedStatement = connection.prepareStatement("INSERT INTO " + chatAdminsTable + " (`chat_id`, `tg_id`, `owner`) VALUES (?, ?, ?)");
                    preparedStatement.setLong(1, chatID);
                    preparedStatement.setLong(2, chatAdmin.getUserID());
                    preparedStatement.setBoolean(3, chatAdmin.isOwner());
                    preparedStatement.executeUpdate();
                }
            }

            registeredChatsCache.put(chatData.getChatID(), chatData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void keepAlive() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    connection.prepareStatement("SET NAMES utf8").execute();
                } catch (SQLException ignored) {
                }
            }
        }).start();
    }

    private void cacheUpdater() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                updateChatsCache();
            }
        }).start();
    }
}