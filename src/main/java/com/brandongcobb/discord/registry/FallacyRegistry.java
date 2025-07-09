//
//  FallacyCorrectionStore.swift
//  
//
//  Created by Brandon Cobb on 7/9/25.
//
package com.brandongcobb.discord.registry;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FallacyRegistry {
    private static final FallacyRegistry INSTANCE = new FallacyRegistry();

    private final Map<Long, Pair<String, String>> messageIdToPair = new ConcurrentHashMap<>();

    private FallacyRegistry() {}

    public static FallacyRegistry getInstance() {
        return INSTANCE;
    }

    public Map<Long, Pair<String, String>> getMessageIdToPair() {
        return messageIdToPair;
    }
}
