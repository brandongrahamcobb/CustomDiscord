//
//  RoleInfo.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//


package com.brandongcobb.discord.domain.output;

import java.util.Set;

public class RoleInfo {
    private String id;
    private String name;
    private long permissions;         // bitmask of permissions
    private int position;             // role position (hierarchy)
    private boolean mentionable;
    private int color;                // RGB integer
    private Integer memberCount;      // nullable: included only if requested

    // Getters and setters

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
    public long getPermissions() {
        return permissions;
    }
    public void setPermissions(long permissions) {
        this.permissions = permissions;
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
        this.position = position;
    }
    public boolean isMentionable() {
        return mentionable;
    }
    public void setMentionable(boolean mentionable) {
        this.mentionable = mentionable;
    }
    public int getColor() {
        return color;
    }
    public void setColor(int color) {
        this.color = color;
    }
    public Integer getMemberCount() {
        return memberCount;
    }
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
