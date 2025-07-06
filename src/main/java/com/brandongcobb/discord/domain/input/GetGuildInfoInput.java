//
//  GetServerInfoInput.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//


package com.brandongcobb.discord.domain.input;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;

public class GetGuildInfoInput implements ToolInput {

    private transient JsonNode originalJson;
    private Boolean includeAll;
    private List<String> fields;
    private transient String guildId;

    /*
     *  Getters
     */
    @Override
    public JsonNode getOriginalJson() {
        return originalJson;
    }

    public Boolean getIncludeAll() {
        return includeAll;
    }

    public List<String> getFields() {
        return fields;
    }
    
    public String getGuildId() {
        return guildId;
    }

    /*
     *  Setters
     */
    @Override
    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }

    public void setIncludeAll(Boolean includeAll) {
        this.includeAll = includeAll;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
    
    public void setGuild(String guildId) {
        this.guildId = guildId;
    }
}
