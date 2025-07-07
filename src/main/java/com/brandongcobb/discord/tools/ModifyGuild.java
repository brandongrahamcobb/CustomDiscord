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
import com.brandongcobb.discord.domain.input.ModifyGuildInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.GuildManager;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import net.dv8tion.jda.api.entities.Guild.VerificationLevel;
import net.dv8tion.jda.api.entities.Guild.ExplicitContentLevel;
import net.dv8tion.jda.api.entities.Guild.NotificationLevel;

import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Component
public class ModifyGuild implements CustomTool<ModifyGuildInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    
    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ModifyGuild(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Returns information about a single guild.";
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
                            "description": "The ID of the guild to modify"
                        },
                        "name": {
                            "type": "string",
                            "description": "New name for the guild"
                        },
                        "afkTimeout": {
                            "type": "integer",
                            "minimum": 60,
                            "maximum": 3600,
                            "description": "AFK timeout in seconds"
                        },
                        "afkChannelId": {
                            "type": ["string", "null"],
                            "description": "ID of the AFK channel"
                        },
                        "iconBase64": {
                            "type": ["string", "null"],
                            "description": "Base64-encoded guild icon image"
                        },
                        "bannerBase64": {
                            "type": ["string", "null"],
                            "description": "Base64-encoded guild banner image"
                        },
                        "verificationLevel": {
                            "type": "string",
                            "enum": ["NONE", "LOW", "MEDIUM", "HIGH", "VERY_HIGH"],
                            "description": "Verification level for the guild"
                        },
                        "explicitContentFilter": {
                            "type": "string",
                            "enum": ["DISABLED", "MEMBERS_WITHOUT_ROLES", "ALL"],
                            "description": "Explicit content filter level"
                        },
                        "systemChannelId": {
                            "type": ["string", "null"],
                            "description": "ID of the system channel"
                        },
                        "features": {
                            "type": "array",
                            "items": {
                            "type": "string"
                            },
                            "description": "List of guild features"
                        }
                    },
                    "required": ["guildId"],
                    "additionalProperties": false
                }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build modify_guild schema", e);
        }
    }

    @Override
    public Class<ModifyGuildInput> getInputClass() {
        return ModifyGuildInput.class;
    }
    
    @Override
    public String getName() {
        return "modify_guild";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ModifyGuildInput input) {
        return CompletableFuture.supplyAsync(() -> {
            String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson() + "}";
            try {
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();

                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                GuildManager manager = guild.getManager();

                if (input.getName() != null) {
                    manager.setName(input.getName());
                }

                if (input.getAfkChannelId() != null) {
                    VoiceChannel afkChannel = api.getVoiceChannelById(input.getAfkChannelId());
                    if (afkChannel != null) {
                        manager.setAfkChannel(afkChannel);
                    }
                }

                if (input.getAfkTimeout() != null) {
                    try {
                        Guild.Timeout timeout = Guild.Timeout.fromKey(input.getAfkTimeout());
                        manager.setAfkTimeout(timeout);
                    } catch (IllegalArgumentException ex) {
                        return new ToolStatusWrapper("Invalid AFK timeout value: " + input.getAfkTimeout(), false, toolCall);
                    }
                }

                if (input.getIconBase64() != null) {
                    byte[] iconBytes = Base64.getDecoder().decode(input.getIconBase64());
                    manager.setIcon(Icon.from(iconBytes));
                }

                if (input.getSplashBase64() != null) {
                    byte[] splashBytes = Base64.getDecoder().decode(input.getSplashBase64());
                    manager.setSplash(Icon.from(splashBytes));
                }

                if (input.getBannerBase64() != null) {
                    byte[] bannerBytes = Base64.getDecoder().decode(input.getBannerBase64());
                    manager.setBanner(Icon.from(bannerBytes));
                }

                if (input.getSystemChannelId() != null) {
                    TextChannel systemChannel = api.getTextChannelById(input.getSystemChannelId());
                    if (systemChannel != null) {
                        manager.setSystemChannel(systemChannel);
                    }
                }

                if (input.getRulesChannelId() != null) {
                    TextChannel rulesChannel = api.getTextChannelById(input.getRulesChannelId());
                    if (rulesChannel != null) {
                        manager.setRulesChannel(rulesChannel);
                    }
                }

                if (input.getPublicUpdatesChannelId() != null) {
                    TextChannel updatesChannel = api.getTextChannelById(input.getPublicUpdatesChannelId());
                    if (updatesChannel != null) {
                        manager.setCommunityUpdatesChannel(updatesChannel);
                    }
                }

//                if (input.getPreferredLocale() != null) {
//                    manager.setPreferredLocale(input.getPreferredLocale());
//                }

                if (input.getDescription() != null) {
                    manager.setDescription(input.getDescription());
                }

                if (input.getVerificationLevel() != null) {
                    try {
                        VerificationLevel level = VerificationLevel.valueOf(input.getVerificationLevel().toUpperCase());
                        manager.setVerificationLevel(level);
                    } catch (IllegalArgumentException ex) {
                        return new ToolStatusWrapper("Invalid verification level: " + input.getVerificationLevel(), false, toolCall);
                    }
                }

                if (input.getDefaultMessageNotifications() != null) {
                    try {
                        NotificationLevel notifLevel =
                            NotificationLevel.valueOf(input.getDefaultMessageNotifications().toUpperCase());
                        manager.setDefaultNotificationLevel(notifLevel);
                    } catch (IllegalArgumentException ex) {
                        return new ToolStatusWrapper("Invalid default message notifications: " + input.getDefaultMessageNotifications(), false, toolCall);
                    }
                }

                if (input.getExplicitContentFilter() != null) {
                    try {
                        ExplicitContentLevel filter = ExplicitContentLevel.valueOf(input.getExplicitContentFilter().toUpperCase());
                        manager.setExplicitContentLevel(filter);
                    } catch (IllegalArgumentException ex) {
                        return new ToolStatusWrapper("Invalid explicit content filter: " + input.getExplicitContentFilter(), false, toolCall);
                    }
                }

//                if (input.getWidgetEnabled() != null) {
//                    manager.setWidgetEnabled(input.getWidgetEnabled());
//                }

                if (input.getMfaLevel() != null) {
                    try {
                        Guild.MFALevel mfaLevel = Guild.MFALevel.fromKey(input.getMfaLevel());
                        manager.setRequiredMFALevel(mfaLevel);
                    } catch (IllegalArgumentException ex) {
                        return new ToolStatusWrapper("Invalid MFA level: " + input.getMfaLevel(), false, toolCall);
                    }
                }

                if (input.getOwnerId() != null) {
                    try {
                        Member newOwner = guild.getMemberById(input.getOwnerId());
                        if (newOwner != null) {
                            guild.transferOwnership(newOwner);
                        } else {
                            return new ToolStatusWrapper("Owner user not found: " + input.getOwnerId(), false, toolCall);
                        }
                    } catch (Exception ex) {
                        return new ToolStatusWrapper("Failed to transfer ownership: " + ex.getMessage(), false, toolCall);
                    }
                }

                manager.queue();

                return new ToolStatusWrapper("Guild updated successfully", true, toolCall);

            } catch (Exception e) {
                return new ToolStatusWrapper("Error modifying guild: " + e.getMessage(), false, toolCall);
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
