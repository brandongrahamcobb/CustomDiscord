//
//  ListChannelsResult.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//

package com.brandongcobb.discord.domain.output;

import java.util.List;

public class ListChannelsResult {
    private List<ChannelInfo> channels;

    public List<ChannelInfo> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelInfo> channels) {
        this.channels = channels;
    }
}
