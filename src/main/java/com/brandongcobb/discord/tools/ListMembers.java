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
import com.brandongcobb.discord.domain.input.ListMembersInput;
import com.brandongcobb.discord.domain.output.ListMembersResult;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ListMembers implements CustomTool<ListMembersInput, ToolStatus> {
    
    private JDA api;
    private ApplicationContext ctx;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;

    @Autowired
    public ListMembers(ApplicationContext ctx, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.ctx = ctx;
    }

    /*
     *  Getters
     */
    @Override
    public String getDescription() {
        return "Returns a list of members in a guild.";
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
                        "description": "The ID of the Discord guild (server) to retrieve members from."
                    },
                    "includeStatus": {
                        "type": "boolean",
                        "description": "Whether to include each member's online status (e.g., ONLINE, OFFLINE)."
                    },
                    "includeRoles": {
                       "type": "boolean",
                       "description": "Whether to include the list of role names for each member."
                    }
                },
                "required": ["guildId"],
                "additionalProperties": false
            }
            """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build list_channels schema", e);
        }
    }

    @Override
    public Class<ListMembersInput> getInputClass() {
        return ListMembersInput.class;
    }
    
    @Override
    public String getName() {
        return "list_members";
    }
    
    /*
     * Tool
     */
    @Override
    public CompletableFuture<ToolStatus> run(ListMembersInput input) {
        return CompletableFuture.supplyAsync(() -> {

            String toolCall = "{\"tool\":\"" + getName() + "\",\"arguments\":" + input.getOriginalJson().toString() + "}";

            try {
                DiscordBot bot = ctx.getBean(DiscordBot.class);
                JDA api = bot.completeGetJDA().join();

                Guild guild = api.getGuildById(input.getGuildId());
                if (guild == null) {
                    return new ToolStatusWrapper("Guild not found: " + input.getGuildId(), false, toolCall);
                }

                List<Member> members = guild.loadMembers().get(); // blocking fetch for all members
                List<ListMembersResult.MemberInfo> memberInfoList = new ArrayList<>();

                for (Member member : members) {
                    ListMembersResult.MemberInfo info = new ListMembersResult.MemberInfo();
                    info.setId(member.getId());
                    info.setUsername(member.getUser().getName());
                    info.setNickname(member.getNickname());

                    if (Boolean.TRUE.equals(input.getIncludeStatus())) {
                        info.setStatus(member.getOnlineStatus().name());
                    }

                    if (Boolean.TRUE.equals(input.getIncludeRoles())) {
                        info.setRoles(member.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList()));
                    }

                    memberInfoList.add(info);
                }

                ListMembersResult result = new ListMembersResult();
                result.setMembers(memberInfoList);

                String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                return new ToolStatusWrapper(jsonResult, true, toolCall);

            } catch (Exception e) {
                return new ToolStatusWrapper("Error listing members: " + e.getMessage(), false, toolCall);
            }
        });
    }
}
