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
import com.brandongcobb.discord.domain.input.ListRolesInput;
import com.brandongcobb.discord.domain.output.ListRolesResult;
import com.brandongcobb.discord.domain.output.RoleInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class ListRoles implements CustomTool<ListRolesInput, ToolStatus> {
    
    @Autowired
    private ApplicationContext ctx;
    
    private JDA api;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ListRoles(ChatMemory chatMemory) {
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
                  "description": "The Discord server (guild) ID to fetch roles from."
                },
                "includeMemberCounts": {
                  "type": "boolean",
                  "description": "Whether to include member counts for each role.",
                  "default": false
                }
              },
              "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build list_roles schema", e);
        }
    }

    @Override
    public Class<ListRolesInput> getInputClass() {
        return ListRolesInput.class;
    }
    
    @Override
    public String getName() {
        return "list_roles";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ListRolesInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();
                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                List<Role> roles = guild.getRoles();

                List<RoleInfo> roleInfos = new ArrayList<>();
                boolean includeCounts = input.isIncludeMemberCounts();

                for (Role role : roles) {
                    RoleInfo ri = new RoleInfo();
                    ri.setId(role.getId());
                    ri.setName(role.getName());
                    ri.setPermissions(role.getPermissionsRaw());
                    ri.setPosition(role.getPosition());
                    ri.setMentionable(role.isMentionable());
                    ri.setColor(role.getColor() != null ? role.getColor().getRGB() : 0);
                    if (includeCounts) {
                        int count = (int) guild.getMembers().stream()
                            .filter(m -> m.getRoles().contains(role))
                            .count();
                        ri.setMemberCount(count);
                    }
                    roleInfos.add(ri);
                }

                ListRolesResult result = new ListRolesResult();
                result.setRoles(roleInfos);

                String jsonResult;
                try {
                    jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                } catch (Exception e) {
                    return new ToolStatusWrapper("Error serializing result: " + e.getMessage(), false, toolCall);
                }

                return new ToolStatusWrapper(jsonResult, true, toolCall);
                
            } catch (Exception e) {
                String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";
                return new ToolStatusWrapper("Unexpected error: " + e.getMessage(), false, toolCall);
            }
        });
    }
}
