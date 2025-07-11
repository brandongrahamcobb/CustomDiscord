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
import com.brandongcobb.discord.registry.FallacyRegistry;
import com.brandongcobb.discord.registry.ModelRegistry;
import com.brandongcobb.discord.service.AIService;
import com.brandongcobb.discord.service.DiscordService;
import com.brandongcobb.discord.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class EventListeners extends ListenerAdapter implements Cog {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private final AIService ais;
    private final DiscordService dis;
    private final MessageService mess;
    private final ModelRegistry registry = new ModelRegistry();

    private JDA api;
    private DiscordBot bot;
    @Autowired
    public EventListeners(AIService ais, DiscordService dis, MessageService mess) {
        this.ais = ais;
        this.dis = dis;
        this.mess = mess;
    }

    @Override
    public void register(JDA api, DiscordBot bot) {
        this.api = api;
        this.bot = bot.completeGetBot().join();
        api.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        long senderId = event.getAuthor().getIdLong();
        if (message.getAuthor().isBot() || !String.valueOf(senderId).equals(System.getenv("DISCORD_OWNER_ID")) ) return;
        String prefix = System.getenv("DISCORD_COMMAND_PREFIX");
        if (prefix != null && message.getContentRaw().startsWith(prefix)) return;
        if (message.getReferencedMessage() != null &&
            !message.getReferencedMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }
        if (!message.getMentions().isMentioned(event.getJDA().getSelfUser()) &&
            message.getReferencedMessage() == null) {
            return;
        }
        List<Attachment> attachments = message.getAttachments();
        final boolean[] multimodal = new boolean[]{false};
        CompletableFuture<String> contentFuture = (attachments != null && !attachments.isEmpty())
            ? mess.completeProcessAttachmentsBase64(attachments).thenApply(list -> {
                multimodal[0] = true;
                return String.join("\n", list) + "\n" + message.getContentDisplay().replace("@Application", "");
            })
            : CompletableFuture.completedFuture(message.getContentDisplay().replace("@Application", ""));
        contentFuture.thenCompose(prompt ->
            dis.startSequence(prompt, senderId, message.getChannel().asTextChannel())
        ).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
