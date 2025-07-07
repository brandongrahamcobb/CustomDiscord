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
import com.brandongcobb.discord.domain.input.GetMemberInfoInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class GetMemberInfo implements CustomTool<GetMemberInfoInput, ToolStatus> {
    
    private JDA api;
    private ApplicationContext ctx;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public GetMemberInfo(ApplicationContext ctx, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.ctx = ctx;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Returns information about a single guild member.";
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
                    "description": "The ID of the guild (server) where the member belongs."
                    },
                    "userId": {
                    "type": "string",
                    "description": "The ID of the user/member to fetch information for."
                    },
                    "originalJson": {
                    "type": "object",
                    "description": "The original unprocessed JSON input (optional)."
                    }
                },
                "required": ["guildId", "userId"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build list_channels schema", e);
        }
    }

    @Override
    public Class<GetMemberInfoInput> getInputClass() {
        return GetMemberInfoInput.class;
    }
    
    @Override
    public String getName() {
        return "get_member_info";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(GetMemberInfoInput input) {
        return CompletableFuture.supplyAsync(() -> {
            String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson() + "}";
            try {
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();

                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                Member member = guild.getMemberById(input.getUserId());
                if (member == null) {
                    return new ToolStatusWrapper("Member not found: " + input.getUserId(), false, toolCall);
                }

                ObjectNode memberJson = mapper.createObjectNode();
                memberJson.put("username", member.getUser().getName());
                memberJson.put("discriminator", member.getUser().getDiscriminator());
                memberJson.put("globalName", member.getUser().getGlobalName());
                memberJson.put("nickname", member.getNickname());
                memberJson.put("id", member.getId());
                memberJson.put("isBot", member.getUser().isBot());
                memberJson.put("isOwner", guild.getOwnerId().equals(member.getId()));
                memberJson.put("joinedAt", member.getTimeJoined().toString());
                memberJson.put("avatarUrl", member.getUser().getEffectiveAvatarUrl());

                ArrayNode rolesArray = memberJson.putArray("roles");
                member.getRoles().forEach(role -> {
                    ObjectNode roleJson = mapper.createObjectNode();
                    roleJson.put("id", role.getId());
                    roleJson.put("name", role.getName());
                    roleJson.put("color", role.getColorRaw());
                    roleJson.put("position", role.getPosition());
                    rolesArray.add(roleJson);
                });

                return new ToolStatusWrapper(memberJson.toString(), true, toolCall);

            } catch (Exception e) {
                return new ToolStatusWrapper("Error fetching member info: " + e.getMessage(), false, toolCall);
            }
        });
    }
}
