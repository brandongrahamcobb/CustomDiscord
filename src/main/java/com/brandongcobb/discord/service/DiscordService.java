/* REPLService.java The purpose of this class is to serve as the local
 * CLI interface for a variety of AI endpoints.
 *
 * Copyright (C) 2025  github.com/brandongrahamcobb
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * aInteger with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.brandongcobb.discord.service;

import com.brandongcobb.discord.Application;
import com.brandongcobb.discord.component.bot.DiscordBot;
import com.brandongcobb.discord.component.server.CustomMCPServer;
import com.brandongcobb.discord.registry.ModelRegistry;
import com.brandongcobb.discord.utils.handlers.MetadataUtils;
import com.brandongcobb.discord.utils.handlers.OpenAIUtils;
import com.brandongcobb.metadata.Metadata;
import com.brandongcobb.metadata.MetadataContainer;
import com.brandongcobb.metadata.MetadataKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DiscordService {

    private AIService ais;
    private JDA api;
    private DiscordBot bot;
    private static final AtomicLong counter = new AtomicLong();
    private volatile boolean firstRun = true;
    private MetadataContainer lastAIResponseContainer = null;
    private List<JsonNode> lastResults;
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private ObjectMapper mapper = new ObjectMapper();
    private CustomMCPServer mcpServer;
    private MessageService mess;
    private ModelRegistry modelRegistry = new ModelRegistry();
    private CompletableFuture<String> nextInputFuture = null;
    private String originalDirective;
    private ToolService toolService;
    private static ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    private final ExecutorService replExecutor = Executors.newFixedThreadPool(2);
    private volatile boolean waitingForInput = false;
    
    @Autowired
    public DiscordService(CustomMCPServer server, MessageService mess, ToolService toolService) {
        this.ais = new AIService(chatMemory, toolService);
        this.mess = mess;
        this.toolService = toolService;
        this.mcpServer = server;
    }

    /*
     *  Helper
     */
    private void addToolOutput(String content, ChatMemory chatMemory, long senderId) {
        String uuid = String.valueOf(counter.getAndIncrement());
        ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(uuid, "tool", content);
        ToolResponseMessage toolMsg = new ToolResponseMessage(List.of(response));
        ToolResponseMessage.ToolResponse otherResponse = new ToolResponseMessage.ToolResponse(uuid, "tool", content.length() <= 500 ? content : content.substring(0, 500));
        ToolResponseMessage otherToolMsg = new ToolResponseMessage(List.of(response));
        chatMemory.add(String.valueOf(senderId), toolMsg);
        chatMemory.add(String.valueOf(senderId), otherToolMsg);
    }
    /*
     *  Helper
     */
    public String buildContext(long senderId) {
        List<org.springframework.ai.chat.messages.Message> messages = chatMemory.get(String.valueOf(senderId));
        if (messages.isEmpty()) {
            return "No conversation context available.";
        }
        return messages.stream()
            .map(msg -> {
                if (msg instanceof ToolResponseMessage toolMsg) {
                    var responses = toolMsg.getResponses();
                    if (!responses.isEmpty()) {
                        return msg.getMessageType() + ": " + responses.get(0).responseData();
                    } else {
                        return msg.getMessageType() + ": [no tool response data]";
                    }
                } else {
                    String text = msg.getText();
                    return msg.getMessageType() + ": " + (text != null ? text : "[no text]");
                }
            })
            .collect(Collectors.joining("\n"));
    }
    /*
     *  E-Step
     */
    private CompletableFuture<Void> completeESubStep(JsonNode toolCallNode, long senderId) {
        LOGGER.finer("Starting E-substep for tool calls...");
        return CompletableFuture.runAsync(() -> {
            String toolName = toolCallNode.get("tool").asText();
            try {
                JsonNode argsNode = toolCallNode.get("arguments");
                if (toolName == null || toolName.isBlank() || argsNode == null || argsNode.isEmpty()) {
                    LOGGER.finer("Skipping tool call with missing or empty name/arguments.");
                    return;
                }
                ObjectNode rpcRequest = mapper.createObjectNode();
                rpcRequest.put("jsonrpc", "2.0");
                rpcRequest.put("method", "tools/call");
                ObjectNode params = rpcRequest.putObject("params");
                params.put("name", toolName);
                params.set("arguments", argsNode);
                String rpcText = rpcRequest.toString();
                LOGGER.finer("[JSON-RPC →] " + rpcText);
                StringWriter buffer = new StringWriter();
                CountDownLatch latch = new CountDownLatch(1);
                PrintWriter out = new PrintWriter(new Writer() {
                    @Override public void write(char[] cbuf, int off, int len)  {
                        buffer.write(cbuf, off, len);
                    }
                    @Override public void flush() {
                        latch.countDown();
                    }
                    @Override public void close() {}
                }, true);
                String responseStr = mcpServer.handleRequest(rpcText).join();
                LOGGER.finer("[JSON-RPC ←] " + responseStr);
                if (responseStr.isEmpty()) {
                    String emptyMsg = "TOOL: [" + toolName + "] Error: Empty tool response";
                    LOGGER.severe(emptyMsg);
                    addToolOutput(emptyMsg, chatMemory, senderId);
                    return;
                }
                JsonNode root = mapper.readTree(responseStr);
                JsonNode result = root.path("result");
                String message = result.path("message").asText("No message");
                String toolCall = result.path("toolCall").asText("No tool call");
                chatMemory.add(String.valueOf(senderId), new AssistantMessage(toolCall));
                boolean success = result.path("success").asBoolean(false);
                if (success) {
                    addToolOutput("[" + toolName + "] " + message, chatMemory, senderId);
                    LOGGER.finer("[" + toolName + "] succeeded: " + message);
                } else {
                    addToolOutput("TOOL: [" + toolName + "] Error: " + message, chatMemory, senderId);
                    LOGGER.severe(toolName + " failed: " + message);
                }
            } catch (Exception e) {
                String err = "TOOL: [" + toolName + "] Error: Exception executing tool: " + e.getMessage();
                LOGGER.severe(err);
                addToolOutput(err, chatMemory, senderId);
            }
        }).exceptionally(ex -> {
            LOGGER.severe("completeESubStep (exec) failed: " + ex.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> completeESubStep(boolean firstRun) {
        LOGGER.finer("Starting E-substep for first run...");
        if (!firstRun) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            try {
                ObjectNode initRequest = mapper.createObjectNode();
                initRequest.put("jsonrpc", "2.0");
                initRequest.put("method", "initialize");
                initRequest.set("params", mapper.createObjectNode());
                initRequest.put("id", "init-001");
                String responseStr = mcpServer.handleRequest(initRequest.toString()).join();
                if (responseStr.isEmpty()) throw new IOException("Empty initialization response");
                JsonNode responseJson = mapper.readTree(responseStr);
                LOGGER.finer("Initialization completed: " + responseJson.toString());
            } catch (Exception e) {
                LOGGER.severe("Initialization error: " + e.getMessage());
            }
        }).exceptionally(ex -> {
            LOGGER.severe("completeESubStep (init) failed: " + ex.getMessage());
            return null;
        });
    }
    
    private CompletableFuture<Void> completeEStep(MetadataContainer response, boolean firstRun, GuildChannel channel, long senderId) {
        LOGGER.fine("Starting E-step...");
//        if (System.getenv("DISCORD_PROVIDER").equals("google")) {
//            try {
//                Thread.sleep(60000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                e.printStackTrace();
//            }
//        }
        return new MetadataUtils(response).completeGetContent().thenCompose(contentStr -> {
            String finishReason = new OpenAIUtils(response).completeGetFinishReason().join();
            if (finishReason != null) {
                if (lastResults != null && !lastResults.isEmpty() && !finishReason.contains("MALFORMED_FUNCTION_CALL")) {
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    return completeESubStep(firstRun).thenCompose(v -> {
                        for (JsonNode toolCallNode : lastResults) {
                            futures.add(completeESubStep(toolCallNode, senderId));
                        }
                        lastResults = null;
                        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    }).exceptionally(ex -> {
                        LOGGER.severe("One or more tool executions failed: " + ex.getMessage());
                        return null;
                    });
                } else {
                    LOGGER.finer("No tools to run, falling back to user input.");
                    return null;
                }
            }
            return CompletableFuture.completedFuture(null);
        }).exceptionally(ex -> {
            LOGGER.severe("completeEStep failed: " + ex.getMessage());
            return null;
        });
    }
    
    /*
     * P-Step
     */
    private CompletableFuture<Void> completePStep(GuildChannel channel, long senderId) {
        LOGGER.fine("Starting P-step");
        printIt(channel, senderId);
        return CompletableFuture.completedFuture(null);
    }
    /*
     *  R-Step
     */
    private CompletableFuture<MetadataContainer> completeRStepWithTimeout(boolean firstRun, GuildChannel channel, Long senderId) {
        final int maxRetries = 2;
        final long timeout = 3600_000;
        CompletableFuture<MetadataContainer> result = new CompletableFuture<>();
        Runnable attempt = new Runnable() {
            int retries = 0;
            @Override
            public void run() {
                retries++;
                completeRStep(firstRun, channel, senderId)
                    .orTimeout(timeout, TimeUnit.SECONDS)
                    .whenComplete((resp, err) -> {
                        if (err != null || resp == null) {
                            LOGGER.finer(err.toString());
                            chatMemory.clear(String.valueOf(senderId));
                            addToolOutput("The previous output was greater than the token limit (32768 tokens) or errored and as a result the request failed. The context has been removed.", chatMemory, senderId);
                            boolean shouldRetry = false;
                            if (resp != null) {
                                String content = resp.get(new MetadataKey<>("response", Metadata.STRING));
                                String finishReason = new OpenAIUtils(resp).completeGetFinishReason().join();
                                if (finishReason != null) {
                                    if (finishReason.contains("MALFORMED_FUNCTION_CALL")) {
                                        LOGGER.warning("Detected MALFORMED_FUNCTION_CALL, retrying...");
                                        result.complete(resp);
                                    }
                                }
                            }
                            if (retries < maxRetries) {
                                retries++;
                                replExecutor.submit(this);
                            } else {
                                LOGGER.severe("completeRStepWithTimeoutfailed: " + retries + " attempts.");
                                result.completeExceptionally(err != null ? err : new IllegalStateException("completeRStepWithTimeoutfailed: " + retries + " attempts."));
                            }
                        } else {
                            result.complete(resp);
                        }
                    });
            }
        };
        replExecutor.submit(attempt);
        return result;
    }
    
    private CompletableFuture<MetadataContainer> completeRStep(boolean firstRun, GuildChannel channel, long senderId) {
        LOGGER.fine("Starting R-step, firstRun=" + firstRun);
        String prompt = firstRun ? originalDirective : buildContext(senderId);
        String model = System.getenv("DISCORD_MODEL");
        String provider = System.getenv("DISCORD_PROVIDER");
        String requestType = System.getenv("DISCORD_REQUEST_TYPE");
        CompletableFuture<String> endpointFuture = modelRegistry.completeGetAIEndpoint(false, provider, "discord", requestType);
        CompletableFuture<String> instructionsFuture = modelRegistry.completeGetInstructions(false, provider, "discord");
        return endpointFuture.thenCombine(instructionsFuture, AbstractMap.SimpleEntry::new).thenCompose(pair -> {
            String endpoint = pair.getKey();
            String instructions = pair.getValue();
            String prevId = null;
            if (!firstRun) {
                MetadataKey<String> previousResponseIdKey = new MetadataKey<>("id", Metadata.STRING);
                prevId = (String) lastAIResponseContainer.get(previousResponseIdKey);
            }
            try {
                return ais.completeRequest(instructions, prompt, prevId, model, requestType, endpoint,
                        Boolean.parseBoolean(System.getenv("DISCORD_STREAM")), null, provider)
                    .thenApply(resp -> {
                        if (resp == null) {
                            throw new CompletionException(new IllegalStateException("AI returned null"));
                        }
                        lastAIResponseContainer = resp;
                        OpenAIUtils utils = new OpenAIUtils(resp);
                        String finishReason = utils.completeGetFinishReason().join();
                        String content = utils.completeGetContent().join();
                        this.lastResults = new ArrayList<>();
                        String toolName = utils.completeGetFunctionName().join();
                        Map<String, Object> toolArgs = utils.completeGetArguments().join();
                        if (content == null && content.isBlank() && toolName == null && toolArgs == null) {
                            LOGGER.warning("No content in model response.");
                        } else {
                            if (toolName != null && toolArgs != null) {
                                ObjectNode toolCallNode = mapper.createObjectNode();
                                toolCallNode.put("tool", toolName);
                                toolCallNode.set("arguments", mapper.valueToTree(toolArgs));
                                lastResults.add(toolCallNode);
                            } else {
                                Pattern jsonBlock = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```", Pattern.DOTALL);
                                Matcher matcher = jsonBlock.matcher(content);
                                while (matcher.find()) {
                                    String jsonText = matcher.group(1).trim();
                                    try {
                                        JsonNode toolCallNode = mapper.readTree(jsonText);
                                        if (toolCallNode.has("tool") && toolCallNode.has("arguments")) {
                                            lastResults.add(toolCallNode);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.severe("Failed to parse inline tool JSON: " + e.getMessage());
                                    }
                                }
                                if (lastResults.isEmpty()) {
                                    chatMemory.add(String.valueOf(senderId), new AssistantMessage(content));
                                }
                            }
                        }

                        MetadataContainer metadata = new MetadataContainer();
                        metadata.put(new MetadataKey<>("finish_reason", Metadata.STRING), finishReason);
                        return metadata;
                    });
            } catch (Exception e) {
                e.printStackTrace();
                return CompletableFuture.completedFuture(new MetadataContainer());
            }
        });
    }
    
    /*
     *  Helper
     */
    public void printIt(GuildChannel channel, long senderId) {
        List<org.springframework.ai.chat.messages.Message> messages = chatMemory.get(String.valueOf(senderId));
        org.springframework.ai.chat.messages.Message lastMessage = messages.get(messages.size() - 1);
        String content  = "";
        if (lastMessage instanceof UserMessage userMsg) {
            content = userMsg.getText();
        } else if (lastMessage instanceof AssistantMessage assistantMsg) {
            content = assistantMsg.getText();
        } else if (lastMessage instanceof SystemMessage systemMsg) {
            content = systemMsg.getText();
        } else if (lastMessage instanceof ToolResponseMessage toolResponseMsg) {
            var responses = toolResponseMsg.getResponses();
            if (!responses.isEmpty()) {
                content = responses.get(0).responseData();
            }
        }
        mess.completeSendDiscordMessage(channel, content).join();
    }
    
    

    /*
     * Full-REPL
     */
    public CompletableFuture<Void> startSequence(String userInput, long senderId, GuildChannel channel) {
        if (senderId != Long.valueOf(System.getenv("DISCORD_OWNER_ID"))) { return null; }
        mess.completeSendDiscordMessage(channel, "Thinking...").join();
        if (userInput == null || userInput.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        originalDirective = "Guild ID:" + channel.getGuild() + "Channel ID: " + channel.getId() + userInput;
        chatMemory.add(String.valueOf(senderId), new AssistantMessage("Guild ID:" + channel.getGuild() + "Channel ID: " + channel.getId() + userInput));
        userInput = null;
        return completeRStepWithTimeout(firstRun, channel, senderId)
            .thenCompose(resp ->
                completeEStep(resp, firstRun, channel, senderId)
                    .thenCompose(eDone ->
                        completePStep(channel, senderId)
                    )
            );
    }

}
