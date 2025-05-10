/*  Maps.java
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
package com.brandongcobb.vyrtuous.utils.inc;

import com.brandongcobb.vyrtuous.utils.inc.*;
import com.brandongcobb.vyrtuous.records.ModelInfo;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FileObject {
    ACTIVITY_OBJECT(Helpers.FILE_ACTIVITY_OBJECT),
    AI_MANAGER(Helpers.FILE_AI_MANAGER),
    CONFIG_MANAGER(Helpers.FILE_CONFIG_MANAGER),
    COST_OBJECT(Helpers.FILE_COST_OBJECT),
    DISCORD_BOT(Helpers.FILE_DISCORD_BOT),
    EVENT_LISTENERS(Helpers.FILE_EVENT_LISTENERS),
    HELPERS(Helpers.FILE_HELPERS),
    HYBIRD_COMMANDS(Helpers.FILE_HYBIRD_COMMANDS),
    METADATA_CONTAINER(Helpers.FILE_METADATA_CONTAINER),
    METADATA_HOLDER(Helpers.FILE_METADATA_HOLDER),
    METADATA_KEY(Helpers.FILE_METADATA_KEY),
    METADATA_TYPE(Helpers.FILE_METADATA_TYPE),
    MESSAGE_MANAGER(Helpers.FILE_MESSAGE_MANAGER),
    MODEL_INFO(Helpers.FILE_MODEL_INFO),
    MODEL_REGISTRY(Helpers.FILE_MODEL_REGISTRY),
    MODERATION_MANAGER(Helpers.FILE_MODERATION_MANAGER),
    PREDICATOR(Helpers.FILE_PREDICATOR),
    REQUEST_OBJECT(Helpers.FILE_REQUEST_OBJECT),
    RESPONSE_OBJECT(Helpers.FILE_RESPONSE_OBJECT),
    VYRTUOUS(Helpers.FILE_VYRTUOUS);

    private final String fileContent;

    FileObject(String fileContent) {
        this.fileContent = fileContent;
    }
}
