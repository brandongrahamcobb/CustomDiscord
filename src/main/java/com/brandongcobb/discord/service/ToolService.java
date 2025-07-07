/*  ToolService.java The primary purpose of this class is to serve as
 *  the Model Context Protocol server for the Application spring boot application.
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
package com.brandongcobb.discord.service;

import com.brandongcobb.discord.Application;
import com.brandongcobb.discord.domain.ToolStatus;
import com.brandongcobb.discord.domain.input.*;
import com.brandongcobb.discord.tools.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class ToolService {

    private CreateChannel createChannel;
    private GetGuildInfo getGuildInfo;
    private GetMemberInfo getMemberInfo;
    private ListChannels listChannels;
    private ListMembers listMembers;
    private ListRoles listRoles;
    private ModerateMember moderateMember;
    private ModifyChannel modifyChannel;
    private ModifyGuild modifyGuild;
    private SearchWeb searchWeb;
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private final ChatMemory chatMemory;
    private final Map<String, CustomTool<?, ?>> tools = new HashMap<>();

    @Autowired
    public ToolService(ChatMemory chatMemory,
                       CreateChannel createChannel,
                       GetGuildInfo getGuildInfo,
                       GetMemberInfo getMemberInfo,
                       ListChannels listChannels,
                       ListMembers listMembers,
                       ListRoles listRoles,
                       ModerateMember moderateMember,
                       ModifyChannel modifyChannel,
                       ModifyGuild modifyGuild,
                       SearchWeb searchWeb) {
        this.chatMemory = chatMemory;
        this.createChannel = createChannel;
        this.getGuildInfo = getGuildInfo;
        this.getMemberInfo = getMemberInfo;
        this.listChannels = listChannels;
        this.listMembers = listMembers;
        this.listRoles = listRoles;
        this.moderateMember  = moderateMember;
        this.modifyChannel = modifyChannel;
        this.modifyGuild = modifyGuild;
        this.searchWeb = searchWeb;
    }
    
    public CompletableFuture<JsonNode> callTool(String name, JsonNode arguments) {
        CustomTool<?, ?> customTool = tools.get(name);
        if (customTool == null) {
            CompletableFuture<JsonNode> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Tool not found: " + name));
            return failed;
        }
        try {
            Object inputObj = mapper.treeToValue(arguments, customTool.getInputClass());
            if (inputObj instanceof ToolInput toolInput) {
                toolInput.setOriginalJson(arguments);
            }
            CustomTool<Object, ?> typedTool = (CustomTool<Object, ?>) customTool;
            return typedTool.run(inputObj)
                .thenApply(result -> {
                    if (result instanceof ToolResult tr) {
                        return tr.getOutput();
                    } else {
                        return mapper.valueToTree(result);
                    }
                });
        } catch (Exception e) {
            CompletableFuture<JsonNode> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    public Collection<CustomTool<?, ?>> getTools() {
        return tools.values();
    }
    
    public void registerTool(CustomTool<?, ?> tool) {
        tools.put(tool.getName(), tool);
    }

    /*
     *  Tools
     */
    @Tool(name = "create_channel", description = "Create a channel in a guild")
    public CompletableFuture<ToolStatus> createChannel(CreateChannelInput input) {
        return createChannel.run(input);
    }
    
    @Tool(name = "get_guild_info", description = "Get information about a guild")
    public CompletableFuture<ToolStatus> getGuildInfo(GetGuildInfoInput input) {
        return getGuildInfo.run(input);
    }
    
    @Tool(name = "get_member_info", description = "Get information about a guild member")
    public CompletableFuture<ToolStatus> getMemberInfo(GetMemberInfoInput input) {
        return getMemberInfo.run(input);
    }
    
    @Tool(name = "list_channels", description = "List channels in a guild")
    public CompletableFuture<ToolStatus> listChannels(ListChannelsInput input) {
        return listChannels.run(input);
    }
    
    @Tool(name = "list_members", description = "List members in a guild")
    public CompletableFuture<ToolStatus> listMembers(ListMembersInput input) {
        return listMembers.run(input);
    }
    
    @Tool(name = "list_roles", description = "List roles in a guild")
    public CompletableFuture<ToolStatus> getListRoles(ListRolesInput input) {
        return listRoles.run(input);
    }

    @Tool(name = "moderate_member", description = "Moderates a member in a guild")
    public CompletableFuture<ToolStatus> moderateMember(ModerateMemberInput input) {
        return moderateMember.run(input);
    }

    @Tool(name = "modify_channel", description = "Modifies a channel in a guild")
    public CompletableFuture<ToolStatus> modifyChannel(ModifyChannelInput input) {
        return modifyChannel.run(input);
    }

    @Tool(name = "modify_guild", description = "Modifies a guild")
    public CompletableFuture<ToolStatus> modifyGuild(ModifyGuildInput input) {
        return modifyGuild.run(input);
    }
    
    @Tool(name = "search_web", description = "Search the web for matching criteria")
    public CompletableFuture<ToolStatus> searchWeb(SearchWebInput input) {
        return searchWeb.run(input);
    }
}

