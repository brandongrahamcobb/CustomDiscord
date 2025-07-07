
/*  CreateChannel.java The primary purpose of this class is to act as a tool
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
import com.brandongcobb.discord.domain.input.CreateChannelInput;
import com.brandongcobb.discord.domain.input.GetGuildInfoInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CreateChannel implements CustomTool<CreateChannelInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    

    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public CreateChannel(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Creates a channel in a target guild.";
    }

    @Override
    public JsonNode getJsonSchema() {
        try {
            return mapper.readTree("""
            {
                "type": "object",
                "required": ["guildId", "name", "type"],
                "properties": {
                "guildId": {
                    "type": "string",
                    "description": "The ID of the Discord guild (server) to create the channel in."
                },
                "name": {
                    "type": "string",
                    "description": "The name of the new channel. Must be unique within the guild."
                },
                "type": {
                    "type": "string",
                    "enum": ["TEXT", "VOICE", "CATEGORY"],
                    "description": "The type of channel to create: TEXT, VOICE, or CATEGORY."
                },
                "topic": {
                    "type": "string",
                    "description": "The topic of the text channel. Only applicable for TEXT channels."
                },
                "parentId": {
                    "type": "string",
                    "description": "The ID of the category under which the new channel should be nested."
                },
                "bitrate": {
                    "type": "integer",
                    "minimum": 8000,
                    "maximum": 96000,
                    "description": "Bitrate in bits per second for voice channels. Only applicable for VOICE channels."
                },
                "userLimit": {
                    "type": "integer",
                    "minimum": 0,
                    "maximum": 99,
                    "description": "Maximum number of users allowed in the voice channel. Only applicable for VOICE channels."
                }
                },
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build count_file_lines schema", e);
        }
    }

    @Override
    public Class<CreateChannelInput> getInputClass() {
        return CreateChannelInput.class;
    }
    
    @Override
    public String getName() {
        return "create_channel";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(CreateChannelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();
                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                ChannelType channelType = ChannelType.valueOf(input.getType().toLowerCase());

                ChannelAction<?> action = switch (channelType) {
                    case TEXT -> guild.createTextChannel(input.getName());
                    case VOICE -> guild.createVoiceChannel(input.getName());
                    case CATEGORY -> guild.createCategory(input.getName());
                    default -> null;
                };

                if (action == null) {
                    return new ToolStatusWrapper("Unsupported or null channel type: " + input.getType(), false, toolCall);
                }

                if (input.getParentId() != null) {
                    action.setParent(guild.getCategoryById(input.getParentId()));
                }

                if (input.getTopic() != null && channelType == ChannelType.TEXT) {
                    action = ((ChannelAction<?>) action).setTopic(input.getTopic());
                }

                if (channelType == ChannelType.VOICE) {
                    if (input.getBitrate() != null) {
                        action = ((ChannelAction<?>) action).setBitrate(input.getBitrate());
                    }
                    if (input.getUserLimit() != null) {
                        action = ((ChannelAction<?>) action).setUserlimit(input.getUserLimit());
                    }
                }

                var createdChannel = action.complete();

                return new ToolStatusWrapper("Created channel: " + createdChannel.getName() + " (" + createdChannel.getId() + ")", true, toolCall);
            } catch (Exception e) {
                
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                return new ToolStatusWrapper("Unexpected error: " + e.getMessage(), false, toolCall);
            }
        });
    }
}
//🛠️ 1. GetServerInfo
//Purpose: Get basic metadata and identity
//Returns: name, ID, owner ID, boost tier, features, locale, creation time
//
//🛠️ 2. ListChannels
//Purpose: List all text, voice, category, and announcement channels
//Returns: channel names, types, IDs, category hierarchy
//Options: optionally filtered by type (text, voice, etc.)
//
//🛠️ 3. ListRoles
//Purpose: Return all roles with permissions and hierarchy
//Returns: role name, ID, permissions, position, mentionable, color
//Options: include member counts?
//
//🛠️ 4. ListEmojisAndStickers
//Purpose: List all custom emojis and stickers
//Returns: name, ID, animated?, usable?
//
//🛠️ 5. GetGuildSettings
//Purpose: Return settings like AFK channel, AFK timeout, system channel, rules channel, locale, etc.
//
//🛠️ 6. ListScheduledEvents
//Purpose: Return community events
//Returns: event name, time, channel, type, privacy level
//
//🛠️ 7. GetAuditLog
//Purpose: Return recent moderation actions
//Options: filter by action (e.g. ban, kick, role_update)
//Returns: user who performed action, target, timestamp
//
//🛠️ 8. ListInvites
//Purpose: Show active invites and vanity URL (if available)
//
//🛠️ 9. ListMembers (optional & expensive)
//Purpose: Return paginated or limited list of guild members
//Warning: Requires GUILD_MEMBERS intent and can be heavy
//
