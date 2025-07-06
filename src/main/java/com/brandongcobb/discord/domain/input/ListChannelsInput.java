//
//  ListChannelsInput.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//


package com.brandongcobb.discord.domain.input;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class ListChannelsInput implements ToolInput {

    private transient JsonNode originalJson;
    private String guildId;
    private List<String> channelTypes;

    /*
     * Getters
     */
    @Override
    public JsonNode getOriginalJson() {
        return originalJson;
    }

    public String getGuildId() {
        return guildId;
    }

    public List<String> getChannelTypes() {
        return channelTypes;
    }

    /*
     * Setters
     */
    @Override
    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setChannelTypes(List<String> channelTypes) {
        this.channelTypes = channelTypes;
    }
}
