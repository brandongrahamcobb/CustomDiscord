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

import com.brandongcobb.discord.domain.ToolStatus;
import com.brandongcobb.discord.domain.ToolStatusWrapper;
import com.brandongcobb.discord.domain.input.CorrectFallacyInput;
import com.brandongcobb.discord.service.AIService;
import com.brandongcobb.discord.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CorrectFallacy implements CustomTool<CorrectFallacyInput, ToolStatus> {

    private JDA api;
    private ApplicationContext ctx;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;
    private MessageService mess;

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
        return "Sends a message in the relevant channel if a fallacy is detected.";
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
                        "type": "boolean",
                        "description": "The channel ID where the correction is to be sent."
                    },
                    "correction": {
                       "type": "boolean",
                       "description": "The correction of the fallacy."
                    },
                    "fallacy": {
                       "type": "string",
                       "description": "The string detected which is incorrect."
                    },
                    "messageId": {
                        "type": "string".
                        "description": "The snowflake ID for the referenced message."
                    }
                },
                "required": ["guildId", "fallacy"],
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

        Guild guild = api.getGuildById(input.getGuildId());
        if (guild == null) {
            return CompletableFuture.completedFuture(
                new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, null)
            );
        }

        GuildChannel channel = guild.getGuildChannelById(input.getChannelId());
        if (!(channel instanceof TextChannel textChannel)) {
            return CompletableFuture.completedFuture(
                new ToolStatusWrapper("Channel not found or not a TextChannel: " + input.getChannelId(), false, null)
            );
        }

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("🧠 **Detected Fallacy:**\n").append(input.getFallacy());
        if (input.getCorrection() != null && !input.getCorrection().isBlank()) {
            messageBuilder.append("\n✅ **Suggested Correction:**\n").append(input.getCorrection());
        }

        String content = messageBuilder.toString();

        String jsonCall = """
            {
              "guildId": "%s",
              "fallacy": "%s"%s
            }
            """.formatted(
            input.getGuildId(),
            input.getFallacy(),
            input.getCorrection() != null
                ? ",\n  \"correction\": \"%s\"".formatted(input.getCorrection())
                : ""
        );

        if (input.getMessageId() != null && !input.getMessageId().isBlank()) {
            textChannel.retrieveMessageById(input.getMessageId()).queue(
                original -> original.reply(content).submit().whenComplete((msg, err) -> {
                    if (err != null) {
                        future.complete(new ToolStatusWrapper("Failed to send reply: " + err.getMessage(), false, null));
                    } else {
                        future.complete(new ToolStatusWrapper("Replied with fallacy and correction.", true, jsonCall));
                    }
                }),
                fetchError -> future.complete(new ToolStatusWrapper("Could not retrieve original message: " + fetchError.getMessage(), false, null))
            );
        } else {
            mess.completeSendResponse(textChannel, content).whenComplete((msg, err) -> {
                if (err != null) {
                    future.complete(new ToolStatusWrapper("Failed to send message: " + err.getMessage(), false, null));
                } else {
                    future.complete(new ToolStatusWrapper("Sent fallacy and correction.", true, jsonCall));
                }
            });
        }

        return future;
    }
}
