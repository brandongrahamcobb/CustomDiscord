package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager.MessageContent;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.brandongcobb.vyrtuous.utils.inc.ModelRegistry;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.yaml.snakeyaml.Yaml;

public class AIManager {

    private boolean addCompletionToHistory;
    private Vyrtuous app;
    private static ConfigManager configManager;
    private static Map<String, List<Map<String, String>>> conversations;
    private static String openAIAPIKey;
    private static Helpers helpers = new Helpers();
    private int i;
    private CompletableFuture<List<Map<String, Object>>> inputArray;
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";

    public AIManager(Vyrtuous application) throws IOException {
        Vyrtuous.aiManager = this;
        this.app = application;
        this.configManager = app.configManager;
        this.openAIAPIKey = configManager.getNestedConfigValue("api_keys", "OpenAI").getStringValue("api_key");
        this.conversations = new HashMap<>();
        this.helpers = helpers;
    }


    public CompletableFuture<String> getChatCompletion(
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
                    if (store) {
                        LocalDateTime now = LocalDateTime.now();
                        Map<String, Object> metadataMap = new HashMap<>();
                        metadataMap.put("user", customId);
                        metadataMap.put("timestamp", now);
                        requestBody.put("metadata", Collections.singletonList(metadataMap));
                    }
                    trimConversationHistory(model, String.valueOf(customId));
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    post.setEntity(new StringEntity(jsonBody));
                    try (CloseableHttpResponse response = httpClient.execute(post)) {
                        HttpEntity entity = response.getEntity();
                        String result = EntityUtils.toString(entity);
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

    public CompletableFuture<String> getCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) {

        try {
            ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(configManager.getStringValue("openai_chat_model"));
            return getChatCompletion(
                app.openAIDefaultChatCompletionNumber,
                customId,
                inputArray,
                contextInfo.upperLimit(),
                configManager.getStringValue("openai_chat_model"),
                app.openAIDefaultChatCompletionResponseFormat,
                configManager.getStringValue("openai_chat_stop"), // Handle stop as boolean
                configManager.getBooleanValue("openai_chat_stream"), // Handle stream as boolean
                app.openAIDefaultChatCompletionSysInput,
                (float) Float.parseFloat(String.valueOf(configManager.getConfigValue("openai_chat_temperature"))),
                (float) Float.parseFloat(String.valueOf(configManager.getConfigValue("openai_chat_top_p"))),
                app.openAIDefaultChatCompletionAddToHistory,
                app.openAIDefaultChatCompletionUseHistory
            );
        } catch (IOException ioe) {}
        return null;
    }

    public CompletableFuture<String> getChatModerationCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) throws IOException {
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

    private String extractCompletion(String jsonResponse) throws IOException {
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
//    private String extractCompletion(String jsonResponse) throws IOException {
//        System.out.println("API Response: " + jsonResponse);  // Log full response for debugging
//        ObjectMapper objectMapper = new ObjectMapper();
//        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
//    
//        // Check for API error
//        if (responseMap.containsKey("error")) {
//            Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error");
//            String message = (String) errorMap.getOrDefault("message", "Unknown error");
//            System.err.println("API Error: " + message);
//            return ""; // or handle as needed
//        }
//    
//        // Check for choices key and validity
//        Object choicesObj = responseMap.get("choices");
//        if (choicesObj == null || !(choicesObj instanceof List)) {
//            System.err.println("No 'choices' key or not a list in response");
//            return "";
//        }
//    
//        List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
//        if (choices.isEmpty()) {
//            System.err.println("Empty 'choices' array in response");
//            return "";
//        }
//    
//        Map<String, Object> firstChoice = choices.get(0);
//        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
//        if (message == null) {
//            System.err.println("Missing 'message' in first choice");
//            return "";
//        }
//    
//        Object contentObj = message.get("content");
//        if (contentObj == null) {
//            System.err.println("Missing 'content' in message");
//            return "";
//        }
//    
//        return (String) contentObj;
//    }
//    private String extractCompletion(String jsonResponse) throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
//        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
//        Map<String, Object> firstChoice = choices.get(0);
//        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
//        return (String) message.get("content"); // Cast to String
//    }
//
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

    public void trimConversationHistory(String model, String customId) {
        ModelInfo contextInfo = ModelRegistry.OPENAI_CHAT_COMPLETION_MODEL_OUTPUT_LIMITS.get(model);
        List<Map<String, String>> history = conversations.get(customId);
        if (history == null) {
            System.out.println("No conversation history found for customId: " + customId);
            return; // or handle it as appropriate
        }
        long totalTokens = history.stream()
            .mapToInt(msg -> msg.get("content").length())
            .sum();
        while (totalTokens > contextInfo.upperLimit() && !history.isEmpty()) {
            Map<String, String> removedMessage = history.remove(0);
            totalTokens -= removedMessage.get("content").length();
        }
        conversations.put(customId, history);
    }
}



