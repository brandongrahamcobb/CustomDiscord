//
//  AppStartup.swift
//  
//
//  Created by Brandon Cobb on 7/6/25.
//

package com.brandongcobb.discord.component.startup;

import com.brandongcobb.discord.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Startup {

    private final ConfigurableApplicationContext ctx;

    @Autowired
    public Startup(ConfigurableApplicationContext ctx) {
        this.ctx = ctx;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(String args[]) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    }
}
