package net.gamedoctor.hakaton.data;

import lombok.Getter;

import java.io.*;

@Getter
public class Config {
    private final String database_arguments;
    private String botToken;
    private long botID;
    private String database_host;
    private String database_user;
    private String database_database;
    private String database_password;

    public Config() {
        database_arguments = "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false";
        File file = new File("config.txt");
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("database-host: localhost");
                writer.newLine();
                writer.write("database-user: root");
                writer.newLine();
                writer.write("database-database: root");
                writer.newLine();
                writer.write("database-password: root");
                writer.newLine();
                writer.write("bot-token: asd");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                database_host = reader.readLine().split(" ")[1];
                database_user = reader.readLine().split(" ")[1];
                database_database = reader.readLine().split(" ")[1];
                database_password = reader.readLine().split(" ")[1];
                botToken = reader.readLine().split(" ")[1];
                botID = Long.parseLong(botToken.split(":")[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}