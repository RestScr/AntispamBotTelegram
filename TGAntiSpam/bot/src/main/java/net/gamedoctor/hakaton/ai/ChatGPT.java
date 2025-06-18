package net.gamedoctor.hakaton.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;

public class ChatGPT {

    public static String makeGPTRequest(String prompt) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            String proxy_login = "ZBrD5E";
            String proxy_password = "MSfzGp";
            String proxy_host = "181.177.112.251";
            int proxy_port = 8000;
            URL obj = new URL(url);
            HttpURLConnection connection;
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "false");
            System.setProperty("jdk.http.auth.proxying.disabledSchemes", "false");

            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(proxy_login,
                            proxy_password.toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);

            Proxy pr = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy_host, proxy_port));
            connection = (HttpURLConnection) obj.openConnection(pr);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + "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");
            connection.setRequestProperty("Content-Type", "application/json");

            String model = "chatgpt-4o-latest";

            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("model", model);

            JsonArray messagesArray = new JsonArray();
            JsonObject messageObject = new JsonObject();
            messageObject.addProperty("role", "user");
            messageObject.addProperty("content", prompt);

            messagesArray.add(messageObject);
            jsonBody.add("messages", messagesArray);

            /*
            String body = "{\n" +
                    "        \"model\": \"" + model + "\",\n" +
                    "        \"messages\": [\n" +
                    "            {\n" +
                    "                \"role\": \"user\",\n" +
                    "                \"content\": \"" + prompt + "\"\n" +
                    "            }\n" +
                    "        ]\n" +
                    "    }";

             */
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(jsonBody.toString());
            writer.flush();
            writer.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return extractMessageFromJSONResponse(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка";
        }
    }

    private static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content") + 11;

        int end = response.indexOf("\"", start);

        return response.substring(start, end);

    }
}