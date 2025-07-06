//
//  ListRolesInput.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//


package com.brandongcobb.discord.domain.input;

import com.fasterxml.jackson.databind.JsonNode;

public class ListRolesInput implements ToolInput {

    private transient JsonNode originalJson;
    private String guildId;
    private boolean includeMemberCounts = false;

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

    public boolean isIncludeMemberCounts() {
        return includeMemberCounts;
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

    public void setIncludeMemberCounts(boolean includeMemberCounts) {
        this.includeMemberCounts = includeMemberCounts;
    }
}
