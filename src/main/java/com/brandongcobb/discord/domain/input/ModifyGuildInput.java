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

    private String afkChannelId;

    private Integer afkTimeoutSeconds;

    private String iconBase64; // guild icon image bytes encoded as Base64

    private String splashBase64;

    private String bannerBase64;

    private String systemChannelId;

    private String rulesChannelId;

    private String publicUpdatesChannelId;

    private String preferredLocale;

    private String description;

    private String verificationLevel; // e.g., NONE, LOW, MEDIUM, HIGH, VERY_HIGH

    private String defaultMessageNotifications; // e.g., ALL_MESSAGES, ONLY_MENTIONS

    private String explicitContentFilter; // e.g., DISABLED, MEMBERS_WITHOUT_ROLES, ALL_MEMBERS

    private Boolean widgetEnabled;

    private Integer mfaLevel; // e.g., 0 (NONE), 1 (ELEVATED)

    private String ownerId; // for ownership transfer (requires permissions)

    /*
     * Getters and setters
     */

    public JsonNode getOriginalJson() {
        return originalJson;
    }

    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAfkChannelId() {
        return afkChannelId;
    }

    public void setAfkChannelId(String afkChannelId) {
        this.afkChannelId = afkChannelId;
    }

    public Integer getAfkTimeout() {
        return afkTimeoutSeconds;
    }

    public void setAfkTimeout(Integer afkTimeoutSeconds) {
        this.afkTimeoutSeconds = afkTimeoutSeconds;
    }

    public String getIconBase64() {
        return iconBase64;
    }

    public void setIconBase64(String iconBase64) {
        this.iconBase64 = iconBase64;
    }

    public String getSplashBase64() {
        return splashBase64;
    }

    public void setSplashBase64(String splashBase64) {
        this.splashBase64 = splashBase64;
    }

    public String getBannerBase64() {
        return bannerBase64;
    }

    public void setBannerBase64(String bannerBase64) {
        this.bannerBase64 = bannerBase64;
    }

    public String getSystemChannelId() {
        return systemChannelId;
    }

    public void setSystemChannelId(String systemChannelId) {
        this.systemChannelId = systemChannelId;
    }

    public String getRulesChannelId() {
        return rulesChannelId;
    }

    public void setRulesChannelId(String rulesChannelId) {
        this.rulesChannelId = rulesChannelId;
    }

    public String getPublicUpdatesChannelId() {
        return publicUpdatesChannelId;
    }

    public void setPublicUpdatesChannelId(String publicUpdatesChannelId) {
        this.publicUpdatesChannelId = publicUpdatesChannelId;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVerificationLevel() {
        return verificationLevel;
    }

    public void setVerificationLevel(String verificationLevel) {
        this.verificationLevel = verificationLevel;
    }

    public String getDefaultMessageNotifications() {
        return defaultMessageNotifications;
    }

    public void setDefaultMessageNotifications(String defaultMessageNotifications) {
        this.defaultMessageNotifications = defaultMessageNotifications;
    }

    public String getExplicitContentFilter() {
        return explicitContentFilter;
    }

    public void setExplicitContentFilter(String explicitContentFilter) {
        this.explicitContentFilter = explicitContentFilter;
    }

    public Boolean getWidgetEnabled() {
        return widgetEnabled;
    }

    public void setWidgetEnabled(Boolean widgetEnabled) {
        this.widgetEnabled = widgetEnabled;
    }

    public Integer getMfaLevel() {
        return mfaLevel;
    }

    public void setMfaLevel(Integer mfaLevel) {
        this.mfaLevel = mfaLevel;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
