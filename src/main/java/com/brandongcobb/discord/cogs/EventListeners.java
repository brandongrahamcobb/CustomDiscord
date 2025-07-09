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
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
public class EventListeners extends ListenerAdapter implements Cog, Runnable {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    private final AIService ais;
    private final DiscordService dis;
    private final MessageService mess;
    private final ModelRegistry registry = new ModelRegistry();

    private JDA api;
    private DiscordBot bot;

    // File watch components
    private ScheduledExecutorService scheduler;
    private long lastPointer = 0L;
    private File file;
    private TextChannel targetChannel;

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
    public void onReady(ReadyEvent event) {
        LOGGER.info("Bot is ready");

        String filePath = System.getenv().getOrDefault("LOG_WATCH_FILE", "/Users/spawd/git/CustomDiscord/subtitles/obs_output.txt");
        file = new File(filePath);
        if (!file.exists()) {
            LOGGER.warning("Log file does not exist: " + file.getAbsolutePath());
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 0, 60, TimeUnit.SECONDS);
        LOGGER.info("Started file watcher for: " + file.getAbsolutePath());
    }

    @Override
    public void run() {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(lastPointer);

            StringBuilder newLines = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                newLines.append(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)).append("\n");
            }

            lastPointer = raf.getFilePointer();
            String content = newLines.toString().trim();

            if (!content.isEmpty()) {
                dis.startSequence("Here is plaintext to be parsed for a fallacy:" + content, Long.valueOf("154749533429956608"), targetChannel)
                .exceptionally(ex -> {
                    LOGGER.warning("Error sending log content to startSequence: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
            }
        } catch (Exception e) {
            LOGGER.severe("Error reading log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) return;

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

        long senderId = event.getAuthor().getIdLong();
        List<Attachment> attachments = message.getAttachments();
        final boolean[] multimodal = new boolean[]{false};

        CompletableFuture<String> contentFuture = (attachments != null && !attachments.isEmpty())
            ? mess.completeProcessAttachments(attachments).thenApply(list -> {
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
