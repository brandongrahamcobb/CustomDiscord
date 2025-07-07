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
import com.brandongcobb.discord.domain.input.ListChannelsInput;
import com.brandongcobb.discord.domain.output.ChannelInfo;
import com.brandongcobb.discord.domain.output.ListChannelsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.templates.TemplateChannel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component

public class ListChannels implements CustomTool<ListChannelsInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    
    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ListChannels(ChatMemory chatMemory) {
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
                  "description": "The ID of the Discord server (guild) to list channels for."
                },
                "channelTypes": {
                  "type": "array",
                  "description": "Optional list of channel types to include. If omitted, all channel types are returned.",
                  "items": {
                    "type": "string",
                    "enum": [
                      "TEXT",
                      "VOICE",
                      "CATEGORY",
                      "ANNOUNCEMENT",
                      "STAGE",
                      "FORUM",
                      "NEWS"
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
    public Class<ListChannelsInput> getInputClass() {
        return ListChannelsInput.class;
    }
    
    @Override
    public String getName() {
        return "list_channels";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ListChannelsInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();
                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, "Guild not found: " + input.getGuildId());
                }

                List<String> filterTypes = input.getChannelTypes();
                List<GuildChannel> allChannels = guild.getChannels();

                Set<ChannelType> filterChannelTypes = null;
                if (filterTypes != null && !filterTypes.isEmpty()) {
                    filterChannelTypes = filterTypes.stream()
                        .map(String::toUpperCase)
                        .map(typeStr -> {
                            try {
                                return ChannelType.valueOf(typeStr);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                }

                final Set<ChannelType> channelTypesFilter = filterChannelTypes;

                List<GuildChannel> filteredChannels = channelTypesFilter == null ? allChannels :
                    allChannels.stream()
                        .filter(ch -> channelTypesFilter.contains(ch.getType()))
                        .collect(Collectors.toList());


                Map<String, List<ChannelInfo>> childrenByCategory = new HashMap<>();
                List<ChannelInfo> topLevelChannels = new ArrayList<>();

                for (GuildChannel ch : filteredChannels) {
                    ChannelInfo info = new ChannelInfo();
                    info.setId(ch.getId());
                    info.setName(ch.getName());
                    info.setType(ch.getType().name());

                    String parentId = null;
                    if (ch instanceof TemplateChannel stdChannel) {
                        if (String.valueOf(stdChannel.getParentId()) != null) {
                            parentId = String.valueOf(stdChannel.getParentId());
                        }
                    }
                    info.setParentId(parentId);

                    if (ch.getType() == ChannelType.CATEGORY) {
                        info.setChildren(new ArrayList<>());
                        topLevelChannels.add(info);
                    } else {
                        if (parentId != null) {
                            childrenByCategory.computeIfAbsent(parentId, k -> new ArrayList<>()).add(info);
                        } else {
                            topLevelChannels.add(info);
                        }
                    }
                }

                for (ChannelInfo cat : topLevelChannels) {
                    if (cat.getType().equals(ChannelType.CATEGORY.name())) {
                        List<ChannelInfo> children = childrenByCategory.get(cat.getId());
                        if (children != null) {
                            cat.setChildren(children);
                        }
                    }
                }

                ListChannelsResult result = new ListChannelsResult();
                result.setChannels(topLevelChannels);

                String jsonResult;
                try {
                    jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                } catch (Exception e) {
                    return new ToolStatusWrapper("Error serializing result: " + e.getMessage(), false, "Error serializing result: " + e.getMessage());
                }

                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                return new ToolStatusWrapper(jsonResult, true, toolCall);

            } catch (Exception e) {
                return new ToolStatusWrapper("Unexpected error: " + e.getMessage(), false, "IO error: " + e.getMessage());
            }
        });
    }
}
