//
//  ListRolesResult.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//

package com.brandongcobb.discord.domain.output;

import java.util.List;

public class ListRolesResult {
    private List<RoleInfo> roles;

    public List<RoleInfo> getRoles() {
        return roles;
    }
    public void setRoles(List<RoleInfo> roles) {
        this.roles = roles;
    }
}
