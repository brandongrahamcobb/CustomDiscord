/*  GetGuildInfoInput.java
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
