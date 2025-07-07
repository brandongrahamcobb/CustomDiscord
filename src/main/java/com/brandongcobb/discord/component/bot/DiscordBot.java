/*  DiscordBot.java The purpose of this class is to manage the
 *  JDA discord api.
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
package com.brandongcobb.discord.component.bot;

import com.brandongcobb.discord.Application;
import com.brandongcobb.discord.cogs.Cog;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class DiscordBot {

    private JDA api;
    private Map<String, Cog> cogs;
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private final ReentrantLock lock = new ReentrantLock();
    private final Set<ListenerAdapter> activeListeners = new HashSet<>();

    @Autowired
    public DiscordBot(ApplicationContext context, JDA api) {
        Map<String, Cog> cogs = context.getBeansOfType(Cog.class);
        for (Cog cog : cogs.values()) {
            cog.register(api, this);
        }
        LOGGER.finer("Discord bot successfully initialized.");
    }

    @Bean
    public CompletableFuture<DiscordBot> completeGetBot() {
        return CompletableFuture.supplyAsync(() -> {
            return this;
        });
    }

    @Bean
    public CompletableFuture<JDA> completeGetJDA() {
        return CompletableFuture.supplyAsync(() -> {
            return this.api;
        });
    }

}
