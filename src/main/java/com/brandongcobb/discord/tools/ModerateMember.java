
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
import com.brandongcobb.discord.domain.input.ModerateMemberInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ModerateMember implements CustomTool<ModerateMemberInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    

    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ModerateMember(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Modifies a member in a target guild.";
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
                        "description": "The ID of the guild where moderation is being performed"
                    },
                    "userId": {
                        "type": "string",
                        "description": "The ID of the user to be moderated"
                    },
                    "kick": {
                        "type": "boolean",
                        "description": "Whether to kick the member from the guild"
                    },
                    "ban": {
                        "type": "boolean",
                        "description": "Whether to ban the member from the guild"
                    },
                    "unban": {
                        "type": "boolean",
                        "description": "Whether to unban the user from the guild"
                    },
                    "deleteMessageDays": {
                        "type": "integer",
                        "minimum": 0,
                        "maximum": 7,
                        "description": "Number of days of messages to delete when banning"
                    },
                    "timeoutMinutes": {
                        "type": "integer",
                        "minimum": 1,
                        "description": "Number of minutes to time out the user (mute from messaging)"
                    },
                    "addRoleIds": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "description": "List of role IDs to assign to the user"
                    },
                    "removeRoleIds": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "description": "List of role IDs to remove from the user"
                    },
                    "newNickname": {
                        "type": "string",
                        "description": "New nickname to assign to the user"
                    },
                    "muteVoice": {
                        "type": "boolean",
                        "description": "Whether to server-mute the user in voice channels"
                    },
                    "deafenVoice": {
                        "type": "boolean",
                        "description": "Whether to server-deafen the user in voice channels"
                    },
                    "dmMessage": {
                        "type": "string",
                        "description": "A message to send to the user via DM before moderation action"
                    }
                },
                "required": ["guildId", "userId"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build moderate_channel schema", e);
        }
    }

    @Override
    public Class<ModerateMemberInput> getInputClass() {
        return ModerateMemberInput.class;
    }
    
    @Override
    public String getName() {
        return "moderate_member";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ModerateMemberInput input) {
        return CompletableFuture.supplyAsync(() -> {
            String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson() + "}";

            try {
                JDA api = ctx.getBean(DiscordBot.class).completeGetJDA().join();
                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) return new ToolStatusWrapper("Guild not found", false, toolCall);

                Member member = guild.retrieveMemberById(input.getUserId()).complete();
                if (member == null) return new ToolStatusWrapper("Member not found", false, toolCall);

                if (input.getKick() != null && input.getKick()) {
                    guild.kick(member).queue();
                }

                if (input.getBan() != null && input.getBan()) {
                    int deleteDays = input.getDeleteMessageDays() != null ? input.getDeleteMessageDays() : 0;
                    guild.ban(member, deleteDays, TimeUnit.DAYS).queue();
                }

                if (input.getUnban() != null && input.getUnban()) {
                    guild.unban(member).queue();
                }

                if (input.getTimeoutMinutes() != null) {
                    Duration duration = Duration.ofMinutes(input.getTimeoutMinutes());
                    guild.timeoutFor(member, duration).queue();
                }

                if (input.getAddRoleIds() != null) {
                    for (String roleId : input.getAddRoleIds()) {
                        Role role = guild.getRoleById(roleId);
                        if (role != null) guild.addRoleToMember(member, role).queue();
                    }
                }

                if (input.getRemoveRoleIds() != null) {
                    for (String roleId : input.getRemoveRoleIds()) {
                        Role role = guild.getRoleById(roleId);
                        if (role != null) guild.removeRoleFromMember(member, role).queue();
                    }
                }

                if (input.getNewNickname() != null) {
                    guild.modifyNickname(member, input.getNewNickname()).queue();
                }

                if (Boolean.TRUE.equals(input.getMuteVoice()) || Boolean.TRUE.equals(input.getDeafenVoice())) {
                    guild.mute(member, Boolean.TRUE.equals(input.getMuteVoice())).queue();
                    guild.deafen(member, Boolean.TRUE.equals(input.getDeafenVoice())).queue();
                }

                if (input.getDmMessage() != null) {
                    member.getUser().openPrivateChannel().flatMap(ch -> ch.sendMessage(input.getDmMessage())).queue();
                }

                return new ToolStatusWrapper("Moderation actions executed", true, toolCall);
            } catch (Exception e) {
                return new ToolStatusWrapper("Moderation failed: " + e.getMessage(), false, toolCall);
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
