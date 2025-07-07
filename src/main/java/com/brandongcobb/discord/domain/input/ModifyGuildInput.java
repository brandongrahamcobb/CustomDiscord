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

import java.util.List;

public class ModifyGuildInput implements ToolInput {

    private transient JsonNode originalJson;

    private String guildId;
    private String name;
    private Integer afkTimeout; // seconds
    private String afkChannelId;
    private String iconBase64;  // base64 encoded image
    private String bannerBase64; // base64 encoded image
    private String verificationLevel; // NONE, LOW, MEDIUM, HIGH, VERY_HIGH
    private String explicitContentFilter; // DISABLED, MEMBERS_WITHOUT_ROLES, ALL
    private String systemChannelId;
    private List<String> features;

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

    public String getName() {
        return name;
    }

    public Integer getAfkTimeout() {
        return afkTimeout;
    }

    public String getAfkChannelId() {
        return afkChannelId;
    }

    public String getIconBase64() {
        return iconBase64;
    }

    public String getBannerBase64() {
        return bannerBase64;
    }

    public String getVerificationLevel() {
        return verificationLevel;
    }

    public String getExplicitContentFilter() {
        return explicitContentFilter;
    }

    public String getSystemChannelId() {
        return systemChannelId;
    }

    public List<String> getFeatures() {
        return features;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setAfkTimeout(Integer afkTimeout) {
        this.afkTimeout = afkTimeout;
    }

    public void setAfkChannelId(String afkChannelId) {
        this.afkChannelId = afkChannelId;
    }

    public void setIconBase64(String iconBase64) {
        this.iconBase64 = iconBase64;
    }

    public void setBannerBase64(String bannerBase64) {
        this.bannerBase64 = bannerBase64;
    }

    public void setVerificationLevel(String verificationLevel) {
        this.verificationLevel = verificationLevel;
    }

    public void setExplicitContentFilter(String explicitContentFilter) {
        this.explicitContentFilter = explicitContentFilter;
    }

    public void setSystemChannelId(String systemChannelId) {
        this.systemChannelId = systemChannelId;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }
}
