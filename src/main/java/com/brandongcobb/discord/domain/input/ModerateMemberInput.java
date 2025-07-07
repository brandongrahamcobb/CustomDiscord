/*  ModerateMemberInput.java
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

import java.util.List;

public class ModerateMemberInput implements ToolInput {

    private transient JsonNode originalJson;

    private String guildId;
    private String userId;

    private Boolean kick;
    private Boolean ban;
    private Boolean unban;
    private Integer deleteMessageDays;

    private Integer timeoutMinutes;

    private List<String> addRoleIds;
    private List<String> removeRoleIds;

    private String newNickname;

    private Boolean muteVoice;
    private Boolean deafenVoice;

    private String dmMessage;

    @Override
    public JsonNode getOriginalJson() {
        return originalJson;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getUserId() {
        return userId;
    }

    public Boolean getKick() {
        return kick;
    }

    public Boolean getBan() {
        return ban;
    }

    public Boolean getUnban() {
        return unban;
    }

    public Integer getDeleteMessageDays() {
        return deleteMessageDays;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public List<String> getAddRoleIds() {
        return addRoleIds;
    }

    public List<String> getRemoveRoleIds() {
        return removeRoleIds;
    }

    public String getNewNickname() {
        return newNickname;
    }

    public Boolean getMuteVoice() {
        return muteVoice;
    }

    public Boolean getDeafenVoice() {
        return deafenVoice;
    }

    public String getDmMessage() {
        return dmMessage;
    }

    @Override
    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setKick(Boolean kick) {
        this.kick = kick;
    }

    public void setBan(Boolean ban) {
        this.ban = ban;
    }

    public void setUnban(Boolean unban) {
        this.unban = unban;
    }

    public void setDeleteMessageDays(Integer deleteMessageDays) {
        this.deleteMessageDays = deleteMessageDays;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public void setAddRoleIds(List<String> addRoleIds) {
        this.addRoleIds = addRoleIds;
    }

    public void setRemoveRoleIds(List<String> removeRoleIds) {
        this.removeRoleIds = removeRoleIds;
    }

    public void setNewNickname(String newNickname) {
        this.newNickname = newNickname;
    }

    public void setMuteVoice(Boolean muteVoice) {
        this.muteVoice = muteVoice;
    }

    public void setDeafenVoice(Boolean deafenVoice) {
        this.deafenVoice = deafenVoice;
    }

    public void setDmMessage(String dmMessage) {
        this.dmMessage = dmMessage;
    }
}
