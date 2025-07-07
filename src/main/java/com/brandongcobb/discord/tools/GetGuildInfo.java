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
import com.brandongcobb.discord.domain.input.GetGuildInfoInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component

public class GetGuildInfo implements CustomTool<GetGuildInfoInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    
    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public GetGuildInfo(ChatMemory chatMemory) {
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
                "required": ["guildId"],
                "properties": {
                  "guildId": {
                  "type": "string",
                  "description": "The ID of the Discord server (guild) to retrieve metadata for."
                },
                "includeAll": {
                  "type": "boolean",
                  "description": "If true, returns all available server metadata fields."
                },
                "fields": {
                  "type": "array",
                  "description": "A list of specific server metadata fields to return instead of all.",
                  "items": {
                    "type": "string",
                    "enum": [
                      "name",
                      "id",
                      "ownerId",
                      "boostTier",
                      "boostCount",
                      "features",
                      "preferredLocale",
                      "createdAt",
                      "systemChannelId",
                      "afkChannelId",
                      "afkTimeoutSeconds",
                      "rulesChannelId",
                      "publicUpdatesChannelId",
                      "description",
                      "vanityUrl",
                      "iconUrl"
                    ]
                  }
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
    public Class<GetGuildInfoInput> getInputClass() {
        return GetGuildInfoInput.class;
    }
    
    @Override
    public String getName() {
        return "get_guild_info";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(GetGuildInfoInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();
                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found or unavailable", false, "Guild not found or unavailable");
                }
                ObjectNode result = mapper.createObjectNode();
                boolean includeAll = Boolean.TRUE.equals(input.getIncludeAll());
                List<String> fields = input.getFields();
                if (includeAll || (fields != null && fields.contains("name"))) {
                    result.put("name", guild.getName());
                }
                if (includeAll || (fields != null && fields.contains("id"))) {
                    result.put("id", guild.getId());
                }
                if (includeAll || (fields != null && fields.contains("ownerId"))) {
                    result.put("ownerId", guild.getOwnerId());
                }
                if (includeAll || (fields != null && fields.contains("boostTier"))) {
                    result.put("boostTier", guild.getBoostTier().name());
                }
                if (includeAll || (fields != null && fields.contains("boostCount"))) {
                    result.put("boostCount", guild.getBoostCount());
                }
//                if (includeAll || (fields != null && fields.contains("features"))) {
//                    ArrayNode featureArray = result.putArray("features");
//                    guild.getFeatures().forEach(feature -> featureArray.add(feature.name()));
//                }
                if (includeAll || (fields != null && fields.contains("preferredLocale"))) {
                    result.put("preferredLocale", guild.getLocale().getLocale());
                }
                if (includeAll || (fields != null && fields.contains("createdAt"))) {
                    result.put("createdAt", guild.getTimeCreated().toString());
                }
                if (includeAll || (fields != null && fields.contains("systemChannelId")) && guild.getSystemChannel() != null) {
                    result.put("systemChannelId", guild.getSystemChannel().getId());
                }
                if (includeAll || (fields != null && fields.contains("afkChannelId")) && guild.getAfkChannel() != null) {
                    result.put("afkChannelId", guild.getAfkChannel().getId());
                }
                if (includeAll || (fields != null && fields.contains("afkTimeoutSeconds"))) {
                    result.put("afkTimeoutSeconds", guild.getAfkTimeout().getSeconds());
                }
                if (includeAll || (fields != null && fields.contains("rulesChannelId")) && guild.getRulesChannel() != null) {
                    result.put("rulesChannelId", guild.getRulesChannel().getId());
                }
//                if (includeAll || (fields != null && fields.contains("publicUpdatesChannelId")) && guild.getPublicUpdatesChannel() != null) {
//                    result.put("publicUpdatesChannelId", guild.getPublicUpdatesChannel().getId());
//                }
                if (includeAll || (fields != null && fields.contains("description")) && guild.getDescription() != null) {
                    result.put("description", guild.getDescription());
                }
                if (includeAll || (fields != null && fields.contains("vanityUrl"))) {
                    result.put("vanityUrl", guild.getVanityUrl() != null ? guild.getVanityUrl() : "");
                }
                if (includeAll || (fields != null && fields.contains("iconUrl"))) {
                    result.put("iconUrl", guild.getIconUrl() != null ? guild.getIconUrl() : "");
                }
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                return new ToolStatusWrapper(result.toPrettyString(), true, toolCall);
            } catch (Exception e) {
                return new ToolStatusWrapper("Unexpected error: " + e.getMessage(), false, "IO error: " + e.getMessage());
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
