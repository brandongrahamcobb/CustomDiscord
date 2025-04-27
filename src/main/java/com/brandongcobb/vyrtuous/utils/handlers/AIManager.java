package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager.MessageContent;
import com.brandongcobb.vyrtuous.utils.inc.ModelRegistry;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.message.MessageAttachment;
import java.util.AbstractMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class AIManager {

    private boolean addCompletionToHistory;
    private static Vyrtuous app;
    private static Map<Long, List<Map<String, Object>>> conversations;
    private static String openAIAPIKey;
    private int i;
    private CompletableFuture<List<Map<String, Object>>> inputArray;

    public AIManager(Vyrtuous application) throws IOException {
        this.app = application;
        this.openAIAPIKey = ConfigManager.getNestedConfigValue("api_keys", "OpenAI").getStringValue("api_key");
        this.conversations = new HashMap<>();
    }

    public static CompletableFuture<String> getChatCompletion(
            long n,
            long customId,
            CompletableFuture<List<MessageContent>> inputArray,
            long maxTokens,
            String model,
            Map<String, Object> responseFormat,
            String stop,
            boolean stream,
            String sysInput,
            float temperature,
            float top_p,
            boolean store,
            boolean addCompletionToHistory) throws IOException {
        return inputArray.thenCompose(messages ->
            CompletableFuture.supplyAsync(() -> {
                String apiUrl = "https://api.openai.com/v1/chat/completions";
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpPost post = new HttpPost(apiUrl);
                    post.setHeader("Authorization", "Bearer " + openAIAPIKey);
                    post.setHeader("Content-Type", "application/json");
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("n", n);
                    ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
                    boolean status = contextInfo.status();
                    if (status) {
                        requestBody.put("max_completion_tokens", contextInfo.upperLimit());
                    } else {
                        requestBody.put("max_tokens", contextInfo.upperLimit());
                    }
                    requestBody.put("temperature", temperature);
                    requestBody.put("model", model);
                    if (responseFormat != null && !responseFormat.isEmpty()) {
                       requestBody.put("response_format", responseFormat);
                    }
                    requestBody.put("stop", stop);
                    requestBody.put("store", store);
                    requestBody.put("top_p", top_p);
                    List<Map<String, Object>> messagesList = new ArrayList<>();
                    for (MessageContent messageContent : messages) {
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("role", messageContent.getType()); // Assuming getType() returns the role ("user" or "assistant")
                        messageMap.put("content", messageContent.getText()); // Assuming getText() returns the message content
                        messagesList.add(messageMap);
                    }
                    requestBody.put("messages", messagesList); // Add the constructed messages list to the request body
                    conversations.put(customId, messagesList);
                    if (store) {
                        LocalDateTime now = LocalDateTime.now();
                        Map<String, Object> metadataMap = new HashMap<>();
                        metadataMap.put("user", customId);
                        metadataMap.put("timestamp", now);
                        requestBody.put("metadata", Collections.singletonList(metadataMap));
                    }
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    post.setEntity(new StringEntity(jsonBody));
                    try (CloseableHttpResponse response = httpClient.execute(post)) {
                        HttpEntity entity = response.getEntity();
                        String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                        String completionResult = extractCompletion(result);
                        return completionResult;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get chat completion", e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to get chat completion", e);
                }
            })
        );
    }

    public static CompletableFuture<String> getCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) {

        try {
            ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(ConfigManager.getStringValue("openai_chat_model"));
            return getChatCompletion(
                app.openAIDefaultChatCompletionNumber,
                customId,
                inputArray,
                contextInfo.upperLimit(),
                ConfigManager.getStringValue("openai_chat_model"),
                app.openAIDefaultChatCompletionResponseFormat,
                ConfigManager.getStringValue("openai_chat_stop"),
                ConfigManager.getBooleanValue("openai_chat_stream"),
                app.openAIDefaultChatCompletionSysInput,
                (float) Float.parseFloat(String.valueOf(ConfigManager.getConfigValue("openai_chat_temperature"))),                                         // I really want to change this, but it causes errors.
                (float) Float.parseFloat(String.valueOf(ConfigManager.getConfigValue("openai_chat_top_p"))),                                               // this too
                app.openAIDefaultChatCompletionAddToHistory,
                app.openAIDefaultChatCompletionUseHistory
            );
        } catch (IOException ioe) {}
        return null;
    }

    public static CompletableFuture<String> getChatModerationCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) throws IOException {
        try {
            ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(app.openAIDefaultChatModerationModel);
            return getChatCompletion(
                app.openAIDefaultChatModerationNumber,
                customId,
                inputArray,
                contextInfo.upperLimit(),
                app.openAIDefaultChatModerationModel,
                app.openAIDefaultChatModerationResponseFormat,
                app.openAIDefaultChatModerationStop,
                app.openAIDefaultChatModerationStream,
                app.openAIDefaultChatModerationSysInput,
                app.openAIDefaultChatModerationTopP,
                app.openAIDefaultChatModerationTemperature,
                app.openAIDefaultChatModerationAddToHistory,
                app.openAIDefaultChatModerationUseHistory
           );
        } catch (IOException ioe) {}
        return null;
    }

    private static String extractCompletion(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
        if (responseMap.containsKey("error")) {
            Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error");
            String message = (String) errorMap.getOrDefault("message", "Unknown error");
            System.err.println("API Error: " + message);
            return ""; // or handle as needed
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content"); // Cast to String
    }


    public static CompletableFuture<Map.Entry<String, Boolean>> handleConversation(
            long senderId,
            String message,
            List<MessageAttachment> attachments
    ) {
        return CompletableFuture.completedFuture(MessageManager.processArray(message, attachments))
            .thenCompose(inputArray -> {
                try {
                    if (ConfigManager.getBooleanValue("openai_chat_moderation")) {
                        return getChatModerationCompletion(senderId, inputArray)
                            .thenCompose(response -> {
                                String reasons = "";
                                boolean flagged = false;
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    Map<String, Object> responseMap = mapper.readValue(response, new TypeReference<>() {});
                                    List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                                    if (results != null && !results.isEmpty()) {
                                        Map<String, Object> result = results.get(0);
                                        flagged = (Boolean) result.get("flagged");
                                        Map<String, Boolean> categories = (Map<String, Boolean>) result.get("categories");
                                        StringBuilder reasonsBuilder = new StringBuilder();
                                        if (categories != null) {
                                            for (Map.Entry<String, Boolean> entry : categories.entrySet()) {
                                                if (Boolean.TRUE.equals(entry.getValue())) {
                                                    reasonsBuilder
                                                        .append(entry.getKey().replace("/", " → ").replace("-", " "))
                                                        .append("; ");
                                                }
                                            }
                                        }
                                        reasons = reasonsBuilder.toString();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    reasons = "Error parsing moderation data.";
                                }
    
                                if (flagged) {
                                    String finalResponse = "Moderation flagged for: " + reasons;
                                    return CompletableFuture.completedFuture(Map.entry(finalResponse, true));
                                } else {
                                    // Not flagged, handle chat response
                                    return getCompletion(senderId, inputArray)
                                        .thenApply(chatResponse -> {
                                            String reply = (chatResponse.length() > 2000)
                                                    ? String.join("\n---\n", splitLongResponse(chatResponse, 1950))
                                                    : chatResponse;
                                            return Map.entry(reply, false);
                                        });
                                }
                            });
                    } else {
                        // Moderation skipped, directly get chat
                        return getCompletion(senderId, inputArray)
                            .thenApply(chatResponse -> {
                                String reply = (chatResponse.length() > 2000)
                                        ? String.join("\n---\n", splitLongResponse(chatResponse, 1950))
                                        : chatResponse;
                                return Map.entry(reply, false);
                            });
                    }
                } catch (IOException e) {
                    // Handle exceptions from processing inputArray
                    e.printStackTrace();
                    return CompletableFuture.completedFuture(Map.entry("Error processing request", false));
                }
            });
    }


    public static List<String> splitLongResponse(String response, int limit) {
        List<String> outputChunks = new ArrayList<>();
        String[] parts = response.split("(?<=```)|(?=```)");
        boolean inCode = false;
        for (String part : parts) { if (part.equals("```")) {
                inCode = !inCode; // toggle code mode
                continue;
            }
            if (inCode) {
                outputChunks.add("```" + part + "```");
                inCode = false; // code block closed
            } else {
                // Split plain text into chunks
                for (int i = 0; i < part.length(); i += limit) {
                    int end = Math.min(i + limit, part.length());
                    outputChunks.add(part.substring(i, end));
                }
            }
        }
        return outputChunks;
    }

    public void trimConversationHistory(String model, long customId) {
        ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
        List<Map<String, Object>> history = conversations.get(customId);
        if (history == null) {
            System.out.println("No conversation history found for customId: " + customId);
            return;
        }
        long totalTokens = history.stream()
            .mapToInt(msg -> String.valueOf(msg.get("content")).length())
            .sum();
        while (totalTokens > contextInfo.upperLimit() && !history.isEmpty()) {
            Map<String, Object> removedMessage = history.remove(0);
            totalTokens -= String.valueOf(removedMessage.get("content")).length();
        }
        conversations.put(customId, history);
    }


}
