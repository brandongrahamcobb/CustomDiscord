//
//  CreateChannelInput.swift
//  
//
//  Created by Brandon Cobb on 7/7/25.
//

package com.brandongcobb.discord.domain.input;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;

public class CreateChannelInput implements ToolInput {

    private transient JsonNode originalJson;

    private String guildId;
    private String name;
    private String type;       // "TEXT", "VOICE", "CATEGORY"
    private String topic;
    private String parentId;
    private Integer bitrate;
    private Integer userLimit;

    /*
     *  Getters
     */
    @Override
    public JsonNode getOriginalJson() {
        return originalJson;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTopic() {
        return topic;
    }

    public String getParentId() {
        return parentId;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public Integer getUserLimit() {
        return userLimit;
    }

    /*
     *  Setters
     */
    @Override
    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public void setUserLimit(Integer userLimit) {
        this.userLimit = userLimit;
    }
}
