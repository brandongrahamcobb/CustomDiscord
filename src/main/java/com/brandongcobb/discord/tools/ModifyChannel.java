
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
import com.brandongcobb.discord.domain.input.ModifyChannelInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ModifyChannel implements CustomTool<ModifyChannelInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    

    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ModifyChannel(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Modifies a channel in a target guild.";
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
                        "description": "The unique ID of the Discord guild (server)."
                    },
                    "channelId": {
                        "type": "string",
                        "description": "The unique ID of the Discord channel to modify."
                    },
                    "name": {
                        "type": "string",
                        "description": "New name for the channel."
                    },
                    "topic": {
                        "type": "string",
                        "description": "New topic for the channel (only applicable to text channels)."
                    },
                    "parentId": {
                        "type": "string",
                        "description": "ID of the parent category to move the channel under."
                    },
                    "bitrate": {
                        "type": "integer",
                        "minimum": 8000,
                        "maximum": 96000,
                        "description": "New bitrate for voice channels (in bits per second)."
                    },
                    "userLimit": {
                        "type": "integer",
                        "minimum": 0,
                        "maximum": 99,
                        "description": "Maximum number of users for the voice channel."
                    },
                    "position": {
                        "type": "integer",
                        "minimum": 0,
                        "description": "New position of the channel in the channel list."
                    }
                },
                "required": ["guildId", "channelId"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build modify_channel schema", e);
        }
    }

    @Override
    public Class<ModifyChannelInput> getInputClass() {
        return ModifyChannelInput.class;
    }
    
    @Override
    public String getName() {
        return "modify_channel";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ModifyChannelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();
                var guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                GuildChannel channel = guild.getGuildChannelById(input.getChannelId());
                if (channel == null) {
                    return new ToolStatusWrapper("Channel not found: " + input.getChannelId(), false, toolCall);
                }
                if (input.getName() != null) {
                    channel.getManager().setName(input.getName()).queue();
                }
                if (input.getPosition() != null) {
                    guild.modifyTextChannelPositions()
                             .selectPosition(channel)
                             .moveTo(input.getPosition())
                             .queue();
                }
                if (input.getParentId() != null) {
                    var parent = guild.getCategoryById(input.getParentId());
                    if (channel instanceof TextChannel textChannel && parent != null) {
                        textChannel.getManager().setParent(parent).queue();
                    }
                }
                if (channel instanceof TextChannel textChannel && input.getTopic() != null) {
                    textChannel.getManager().setTopic(input.getTopic()).queue();
                }
                if (channel instanceof VoiceChannel voiceChannel) {
                    if (input.getBitrate() != null) {
                        voiceChannel.getManager().setBitrate(input.getBitrate()).queue();
                    }
                    if (input.getUserLimit() != null) {
                        voiceChannel.getManager().setUserLimit(input.getUserLimit()).queue();
                    }
                }
                return new ToolStatusWrapper("Channel modified successfully", true, toolCall);
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
