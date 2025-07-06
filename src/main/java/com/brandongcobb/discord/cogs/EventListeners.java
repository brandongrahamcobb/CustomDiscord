/* EventListeners.java The purpose of this program is to listen for Discord
 * events and handle them.
 *
 * Copyright (C) 2025  github.com/brandongrahamcobb
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.discord.cogs;

import com.brandongcobb.discord.Application;
import com.brandongcobb.discord.component.bot.DiscordBot;
import com.brandongcobb.discord.registry.ModelRegistry;
import com.brandongcobb.discord.service.AIService;
import com.brandongcobb.discord.service.DiscordService;
import com.brandongcobb.discord.service.MessageService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Component
public class EventListeners extends ListenerAdapter implements Cog {
    
    public AIService ais;
    private JDA api;
    private Application app;
    private DiscordBot bot;
    private DiscordService dis;
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private MessageService mess;
    private ModelRegistry registry = new ModelRegistry();
    
    @Autowired
    public EventListeners(AIService ais, Application app, DiscordService dis, MessageService mess) {
        this.ais = ais;
        this.app = app;
        this.dis = dis;
        this.mess = mess;
    }
    
    @Override
    public void register(JDA api, DiscordBot bot) {
        this.api = api;
        this.bot = bot.completeGetBot().join();
        api.addEventListener(this);
        api.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                LOGGER.finer("I've always wanted to do this.");
            }
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) {
            LOGGER.finer("Skipped: Message author is a bot");
            return;
        }
        String prefix = System.getenv("DISCORD_COMMAND_PREFIX");
        if (prefix != null && message.getContentRaw().startsWith(prefix)) {
            LOGGER.finer("Skipped: Message starts with command prefix");
            return;
        }
        if (message.getReferencedMessage() != null) {
            if (!message.getReferencedMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
                LOGGER.finer("Skipped: Reply not to this bot");
                return;
            }
        }
        long senderId = event.getAuthor().getIdLong();
        List<Attachment> attachments = message.getAttachments();
        final boolean[] multimodal = new boolean[] { false };
        CompletableFuture<String> contentFuture = (attachments != null && !attachments.isEmpty())
            ? mess.completeProcessAttachments(attachments).thenApply(list -> {
                multimodal[0] = true;
                return String.join("\n", list) + "\n" + message.getContentDisplay().replace("@Application", "");
            })
            : CompletableFuture.completedFuture(message.getContentDisplay().replace("@Application", ""));
        contentFuture.thenCompose(prompt ->  {
            return dis.startSequence(prompt, senderId, message.getChannel().asTextChannel());
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
            
