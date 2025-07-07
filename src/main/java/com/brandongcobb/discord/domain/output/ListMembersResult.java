//
//  ListMembersResult.swift
//  
//
//  Created by Brandon Cobb on 7/7/25.
//


package com.brandongcobb.discord.domain.output;

import java.util.List;

public class ListMembersResult {
    private List<MemberInfo> members;

    public List<MemberInfo> getMembers() {
        return members;
    }

    public void setMembers(List<MemberInfo> members) {
        this.members = members;
    }

    public static class MemberInfo {
        private String id;
        private String username;
        private String nickname;
        private String status;
        private List<String> roles;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
