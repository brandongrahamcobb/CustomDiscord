/*  CustomGuild.java The primary purpose of this class is to act as a tool
 *  for counting lines in a file.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.discord.tools;

import com.brandongcobb.discord.component.bot.DiscordBot;
import com.brandongcobb.discord.domain.ToolStatus;
import com.brandongcobb.discord.domain.ToolStatusWrapper;
import com.brandongcobb.discord.domain.input.CorrectFallacyInput;
import com.brandongcobb.discord.domain.input.CorrectFallacyInput.FallacyCorrection;
import com.brandongcobb.discord.registry.FallacyRegistry;
import com.brandongcobb.discord.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class CorrectFallacy implements CustomTool<CorrectFallacyInput, ToolStatus> {

    private JDA api;
    private ApplicationContext ctx;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;
    private MessageService mess;
    private static Map<Long, Pair<String, String>> messageIdToPair = FallacyRegistry.getInstance().getMessageIdToPair();



    @Autowired
    public CorrectFallacy(ApplicationContext ctx, ChatMemory chatMemory, MessageService mess) {
        this.mess = mess;
        this.chatMemory = chatMemory;
        this.ctx = ctx;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Sends a message in the relevant channel if a fallacy is detected along with a correction.";
    }

    @Override
    public JsonNode getJsonSchema() {
        try {
            return mapper.readTree("""
            {
                "type": "object",
                "properties": {
                    "guildId": {
                        "type": "string",
                        "description": "The ID of the Discord guild (server)."
                    },
                    "channelId": {
                        "type": "string",
                        "description": "The ID of the channel where the message should be sent."
                    },
                    "messageId": {
                        "type": "string",
                        "description": "The snowflake ID for the referenced message (to reply to)."
                    },
                    "corrections": {
                        "type": "array",
                        "description": "An array of detected fallacies and their suggested corrections.",
                        "items": {
                            "type": "object",
                            "properties": {
                                "fallacyName": {
                                    "type": "string",
                                    "description": "The latin name of the fallacy which is incorrect."
                                },
                                "fallacy": {
                                    "type": "string",
                                    "description": "The text which contains the fallacy."
                                },
                                "correction": {
                                    "type": "string",
                                    "description": "The suggested correction of the fallacy."
                                },
                                "timestamp": {
                                    "type": "string",
                                    "description": "The timestamp of the fallacy."
                                }
                            },
                            "required": ["fallacy"],
                            "additionalProperties": false
                        }
                    }
                },
                "required": ["guildId", "corrections"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build correct_fallacy schema", e);
        }
    }

    @Override
    public Class<CorrectFallacyInput> getInputClass() {
        return CorrectFallacyInput.class;
    }
    
    @Override
    public String getName() {
        return "correct_fallacy";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(CorrectFallacyInput input) {
        CompletableFuture<ToolStatus> future = new CompletableFuture<>();

        String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
        DiscordBot bot = ctx.getBean(DiscordBot.class);
        JDA api = bot.completeGetJDA().join();
        Guild guild = api.getGuildById(input.getGuildId());
        if (guild == null) {
            return CompletableFuture.completedFuture(
                new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall)
            );
        }

        final TextChannel resolvedTextChannel;
        if (input.getChannelId() != null) {
            GuildChannel channel = guild.getGuildChannelById(input.getChannelId());
            if (!(channel instanceof TextChannel textChannel)) {
                return CompletableFuture.completedFuture(
                    new ToolStatusWrapper("Channel not found or not a TextChannel: " + input.getChannelId(), false, toolCall)
                );
            }
            resolvedTextChannel = (TextChannel) channel;
        } else {
            resolvedTextChannel = guild.getChannelById(TextChannel.class, Long.parseLong(System.getenv("DEV_DISCORD_CHANNEL")));
        }

        if (resolvedTextChannel == null) {
            return CompletableFuture.completedFuture(
                new ToolStatusWrapper("Channel ID was null or invalid; TextChannel not available", false, toolCall)
            );
        }

        List<FallacyCorrection> corrections = input.getCorrections();
        if (corrections == null || corrections.isEmpty()) {
            return CompletableFuture.completedFuture(
                new ToolStatusWrapper("No fallacy/correction pairs provided", false, toolCall)
            );
        }

        CompletableFuture<ToolStatus> result = new CompletableFuture<>();

        Consumer<Integer> sendPage = new Consumer<>() {
            int sent = 0;

            @Override
            public void accept(Integer index) {
                if (index >= corrections.size()) {
                    result.complete(new ToolStatusWrapper(
                        "Sent " + sent + " fallacy/correction messages.",
                        true,
                        generateJson(input)
                    ));
                    return;
                }

                // 1) Grab the fallacy/correction pair
                FallacyCorrection fc = corrections.get(index);

                // 2) Extract fields
                String correction   = fc.getCorrection();
                String fallacyName  = fc.getFallacyName();
                String fallacyText  = fc.getFallacy();
                String timestampRaw = fc.getTimestamp();

                // 3) Prepare EST timestamp
                // --- STEP 0) isolate the start time from the range
                String timestampFull  = fc.getTimestamp();  // e.g. "00:00:00,644 --> 00:00:06,632"
                String timestampRange = (timestampFull != null)
                    ? timestampFull.split("-->")[0].trim()
                    : "";

                // --- STEP 1) normalize to dot‐separator
                String normalized     = timestampRange.replace(',', '.');

                // --- STEP 2) manual split into h:m:s and millis
                String[] parts        = normalized.split("\\.");
                String   hms          = (parts.length > 0) ? parts[0] : "00:00:00";
                int      milli        = (parts.length > 1)
                                       ? Integer.parseInt(parts[1])
                                       : 0;

                // --- STEP 3) split h:m:s into components
                String[] split        = hms.split(":");
                LocalTime lt          = LocalTime.of(
                    Integer.parseInt(split[0]),  // hour
                    Integer.parseInt(split[1]),  // minute
                    Integer.parseInt(split[2]),  // second
                    milli * 1_000_000            // nanosecond
                );

                // --- STEP 4) interpret as UTC, then convert to EST
                ZoneId        estZone    = ZoneId.of("America/New_York");
                ZonedDateTime nowEst     = ZonedDateTime.now(estZone);
                String        timestampEst
                    = nowEst.format(DateTimeFormatter.ofPattern("h:mm a z"));

                // 4) Build the embed
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("🧠 Fallacy " + (index + 1))
                    .addField("Name",            fallacyName,                          false)
                    .addField("Fallacy",         fallacyText,                          false)
                    .addField("Correction",      correction   != null && !correction.isBlank()
                                                ? correction
                                                : "—",                                false)
                    .addField("Timestamp (EST)", timestampEst,                          false);

                // 5) Define the callback
                Consumer<Message> afterSend = sentMessage -> {
                    messageIdToPair.put(
                        sentMessage.getIdLong(),
                        Pair.of(fallacyText, correction)
                    );
                    sentMessage.addReaction(Emoji.fromUnicode("✅")).queue();
                    sentMessage.addReaction(Emoji.fromUnicode("❌")).queue();
                    sent++;
                    accept(index + 1);
                };

                // 6) Send or reply
                if (input.getMessageId() != null
                    && !input.getMessageId().isBlank()
                    && index == 0)
                {
                    resolvedTextChannel
                        .retrieveMessageById(input.getMessageId())
                        .queue(
                            msg -> msg.replyEmbeds(embed.build()).queue(
                                afterSend,
                                e -> result.complete(new ToolStatusWrapper(
                                    "Failed to reply: " + e.getMessage(),
                                    false,
                                    toolCall
                                ))
                            ),
                            e -> result.complete(new ToolStatusWrapper(
                                "Could not find original message: " + e.getMessage(),
                                false,
                                toolCall
                            ))
                        );
                } else {
                    resolvedTextChannel
                        .sendMessageEmbeds(embed.build())
                        .queue(
                            afterSend,
                            e -> result.complete(new ToolStatusWrapper(
                                "Failed to send embed: " + e.getMessage(),
                                false,
                                toolCall
                            ))
                        );
                }
            }
        };

        sendPage.accept(0);
        return result;
    }

    
    private String generateJson(CorrectFallacyInput input) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"guildId\": \"").append(input.getGuildId()).append("\",\n");
        json.append("  \"corrections\": [\n");
        java.util.List<String> entries = input.getCorrections().stream().map(fc -> {
            String part = "    { \"fallacy\": \"%s\"".formatted(fc.getFallacy());
            if (fc.getCorrection() != null && !fc.getCorrection().isBlank()) {
                part += ", \"correction\": \"%s\"".formatted(fc.getCorrection());
            }
            return part + " }";
        }).toList();
        json.append(String.join(",\n", entries));
        json.append("\n  ]\n}");
        return json.toString();
    }

}
