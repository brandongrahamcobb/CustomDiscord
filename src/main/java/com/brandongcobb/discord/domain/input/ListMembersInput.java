/*  ListChannelsInput.java
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


public class ListMembersInput implements ToolInput {

    private transient JsonNode originalJson;
    private String guildId;
    private Boolean includeStatus;
    private Boolean includeRoles;

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

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public Boolean getIncludeStatus() {
        return includeStatus;
    }

    public void setIncludeStatus(Boolean includeStatus) {
        this.includeStatus = includeStatus;
    }

    public Boolean getIncludeRoles() {
        return includeRoles;
    }

    public void setIncludeRoles(Boolean includeRoles) {
        this.includeRoles = includeRoles;
    }
}
