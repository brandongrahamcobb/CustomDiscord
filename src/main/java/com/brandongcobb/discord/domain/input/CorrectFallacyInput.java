/*  CorrectFallacyInput.java
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

public class CorrectFallacyInput implements ToolInput {

    private transient JsonNode originalJson;
    private String guildId;              // The ID of the Discord guild (server)
    private String channelId;            // The channel ID where the correction is to be sent
    private String messageId;            // The snowflake ID for the referenced message (optional)
    private List<FallacyCorrection> corrections;
    // A list of fallacy-correction pairs

    // Getters
    @Override
    public JsonNode getOriginalJson() {
        return originalJson;
    }
    
    @Override
    public void setOriginalJson(JsonNode originalJson) {
        this.originalJson = originalJson;
    }
    
    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    public List<FallacyCorrection> getCorrections() {
        return corrections;
    }

    // Setters
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setCorrections(List<FallacyCorrection> corrections) {
        this.corrections = corrections;
    }

    // Inner class representing each fallacy-correction pair
    public static class FallacyCorrection {
        private String fallacy;
        private String fallacyName;   // The incorrect string
        private String correction;
        private String timestamp;// The suggested correction (optional)

        public String getFallacy() {
            return fallacy;
        }

        public void setFallacy(String fallacy) {
            this.fallacy = fallacy;
        }
        
        public String getFallacyName() {
            return fallacyName;
        }

        public void setFallacyName(String fallacyName) {
            this.fallacyName = fallacyName;
        }

        public String getCorrection() {
            return correction;
        }

        public void setCorrection(String correction) {
            this.correction = correction;
        }


        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

    }
}
