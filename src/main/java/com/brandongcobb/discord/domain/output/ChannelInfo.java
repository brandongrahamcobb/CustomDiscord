//
//  ChannelInfo.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//


package com.brandongcobb.discord.domain.output;

import java.util.List;

public class ChannelInfo {
    private String id;
    private String name;
    private String type; // TEXT, VOICE, CATEGORY, etc.
    private String parentId; // category ID if applicable, else null
    private List<ChannelInfo> children; // for categories, their child channels

    // Getters and setters below

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<ChannelInfo> getChildren() {
        return children;
    }

    public void setChildren(List<ChannelInfo> children) {
        this.children = children;
    }
}
