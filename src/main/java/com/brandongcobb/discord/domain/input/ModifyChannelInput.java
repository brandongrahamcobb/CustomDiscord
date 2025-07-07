/*  ModifyChannelInput.java 
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
package com.brandongcobb.discord.domain.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.brandongcobb.discord.domain.input.ToolInput;

public class ModifyChannelInput implements ToolInput {

    private transient JsonNode originalJson;

    private String guildId;
    private String channelId;
    private String name;
    private String topic;
    private String parentId;
    private Integer bitrate;
    private Integer userLimit;
    private Integer position;

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

    public String getChannelId() {
        return channelId;
    }

    public String getName() {
        return name;
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

    public Integer getPosition() {
        return position;
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

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setPosition(Integer position) {
        this.position = position;
    }
}
