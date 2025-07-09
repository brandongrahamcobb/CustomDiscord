//
//  FallacyCorrectionInput.swift
//  
//
//  Created by Brandon Cobb on 7/9/25.
//


package com.brandongcobb.discord.domain.input;

public class CorrectFallacyInput {

    private String guildId;  // The ID of the Discord guild (server)
    private String channelId;  // The channel ID where the correction is to be sent
    private String correction;  // The correction of the fallacy
    private String fallacy;  // The string detected which is incorrect
    private String messageId;  // The snowflake ID for the referenced message (optional)

    // Getters and setters
    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getCorrection() {
        return correction;
    }

    public void setCorrection(String correction) {
        this.correction = correction;
    }

    public String getFallacy() {
        return fallacy;
    }

    public void setFallacy(String fallacy) {
        this.fallacy = fallacy;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
