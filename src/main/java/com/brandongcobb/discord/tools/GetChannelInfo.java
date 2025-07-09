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
import com.brandongcobb.discord.domain.input.GetChannelInfoInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.templates.TemplateChannel;
import net.dv8tion.jda.api.entities.templates.TemplateChannel.PermissionOverride;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class GetChannelInfo implements CustomTool<GetChannelInfoInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    
    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public GetChannelInfo(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Returns information about a single channel.";
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
                        "description": "The ID of the Discord server (guild) to retrieve channel permissions for."
                    },
                    "channelIds": {
                        "type": "array",
                        "description": "A list of channel IDs to include. If omitted and includeAll is true, returns all channels.",
                        "items": {
                            "type": "string"
                        }
                    },
                    "includeAll": {
                        "type": "boolean",
                        "description": "If true, includes all channels in the guild. Overrides channelIds if both are present."
                    }
                },
                "required": ["guildId"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build get_guild_info schema", e);
        }
    }

    @Override
    public Class<GetChannelInfoInput> getInputClass() {
        return GetChannelInfoInput.class;
    }
    
    @Override
    public String getName() {
        return "get_channel_info";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(GetChannelInfoInput input) {
        return CompletableFuture.supplyAsync(() -> {
            String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input + "}";

            try {
                if (input == null || input.getGuildId() == null) {
                    return new ToolStatusWrapper("Missing required 'guildId' field.", false, toolCall);
                }
                String guildId = input.getGuildId();
                boolean includeAll = input.getIncludeAll() != null && input.getIncludeAll();

                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA jda = bot.completeGetJDA().join();
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + guildId, false, toolCall);
                }

                List<GuildChannel> channels = new ArrayList<>();
                if (includeAll) {
                    channels.addAll(guild.getChannels());
                } else {
                    ArrayNode channelIdsNode = mapper.createArrayNode();
                    for (String item : input.getChannelIds()) {
                        channelIdsNode.add(item);
                    }
                    if (channelIdsNode != null && channelIdsNode.isArray()) {
                        for (JsonNode idNode : channelIdsNode) {
                            String channelId = idNode.asText();
                            GuildChannel channel = guild.getGuildChannelById(channelId);
                            if (channel != null) {
                                channels.add(channel);
                            }
                        }
                    } else {
                        return new ToolStatusWrapper("No channels specified and includeAll is false.", false, toolCall);
                    }
                }

                JsonObject result = new JsonObject();

                for (GuildChannel channel : channels) {
                    JsonObject channelJson = new JsonObject();
                    channelJson.addProperty("channelId", channel.getId());
                    channelJson.addProperty("channelName", channel.getName());
                    channelJson.addProperty("channelType", channel.getType().name());

                    JsonArray overridesArray = new JsonArray();

                    if (channel instanceof TemplateChannel template) {
                        for (PermissionOverride override : template.getPermissionOverrides()) {
                            JsonObject overrideJson = new JsonObject();

                            String id = Long.toUnsignedString(override.getIdLong());

                            String type;
                            String name;

                            Role role = guild.getRoleById(id);
                            if (role != null) {
                                type = "role";
                                name = role.getName();
                            } else {
                                Member member = guild.getMemberById(id);
                                if (member != null) {
                                    type = "member";
                                    name = member.getEffectiveName();
                                } else {
                                    type = "unknown";
                                    name = "Unknown";
                                }
                            }

                            overrideJson.addProperty("type", type);
                            overrideJson.addProperty("id", id);
                            overrideJson.addProperty("name", name);

                            long allowedRaw = override.getAllowedRaw();
                            long deniedRaw = override.getDeniedRaw();

                            EnumSet<Permission> allowed = Permission.getPermissions(allowedRaw);
                            EnumSet<Permission> denied = Permission.getPermissions(deniedRaw);

                            JsonArray allowArray = new JsonArray();
                            for (Permission p : allowed) {
                                allowArray.add(p.getName());
                            }

                            JsonArray denyArray = new JsonArray();
                            for (Permission p : denied) {
                                denyArray.add(p.getName());
                            }

                            overrideJson.add("allow", allowArray);
                            overrideJson.add("deny", denyArray);

                            overridesArray.add(overrideJson);
                        }
                    }

                    channelJson.add("overrides", overridesArray);
                    result.add(channel.getId(), channelJson);
                }

                return new ToolStatusWrapper("Fetched " + channels.size() + " channel(s) with overrides: " + result.toString(), true, toolCall);

            } catch (Exception e) {
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
