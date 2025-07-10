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
public class EventListeners extends ListenerAdapter implements Cog, Runnable {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    private static Map<Long, Pair<String, String>> messageIdToPair = FallacyRegistry.getInstance().getMessageIdToPair();
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
    public void run() {
        LOGGER.info("Running file watcher...");

        if (file.length() < lastPointer) {
            LOGGER.warning("File was truncated or replaced. Resetting pointer.");
            lastPointer = 0L;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(lastPointer);

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                buffer.append(line).append("\n");
            }

            lastPointer = raf.getFilePointer();
            LOGGER.info("Updated lastPointer to: " + lastPointer);

            String[] blocks = buffer.toString().split("\r?\n\r?\n");
            List<String> recentBlocks = new ArrayList<>();

            for (String block : blocks) {
                if (!block.isBlank()) {
                    recentBlocks.add(block.trim());
                }
            }

            if (!recentBlocks.isEmpty()) {
                String combined = "Here is plaintext to be parsed for a fallacy:\n\n"
                                + String.join("\n\n", recentBlocks);

                dis.startAlternateSequence(combined, Long.valueOf("154749533429956608"), targetChannel)
                   .exceptionally(ex -> {
                       LOGGER.warning("Error sending recent blocks to tool: " + ex.getMessage());
                       ex.printStackTrace();
                       return null;
                   });
            } else {
                LOGGER.info("No new recent blocks found.");
            }

        } catch (Exception e) {
            LOGGER.severe("Error reading SRT file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Instant parseSRTTimeToInstant(String timeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS")
                                                           .withZone(ZoneOffset.UTC);
            LocalTime localTime = LocalTime.parse(timeStr, formatter);
            return Instant.now()
                          .truncatedTo(ChronoUnit.DAYS)
                          .plusSeconds(localTime.toSecondOfDay())
                          .plusNanos(localTime.getNano());
        } catch (Exception e) {
            LOGGER.warning("Failed to parse timestamp: " + timeStr);
            return null;
        }
    }


    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("Bot is ready");

        // Define allowed time window
        LocalTime startTime = LocalTime.of(0, 0);  // 08:00 AM
        LocalTime endTime = LocalTime.of(23, 59);   // 10:00 PM
        LocalTime now = LocalTime.now();

        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            LOGGER.info("Current time is outside active window (" + startTime + " to " + endTime + "). File watcher not started.");
            return;
        }

        String filePath = System.getenv().getOrDefault("LOG_WATCH_FILE", "/Users/spawd/git/CustomDiscord/subtitles/obs_output.txt");
        file = new File(filePath);
        if (!file.exists()) {
            LOGGER.warning("Log file does not exist: " + file.getAbsolutePath());
            return;
        }

        String channelId = "1390814952285012133";
        targetChannel = api.getTextChannelById(channelId);
        if (targetChannel == null) {
            LOGGER.warning("Target text channel not found for ID: " + channelId);
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 0, 60, TimeUnit.SECONDS);
        LOGGER.info("Started file watcher for: " + file.getAbsolutePath());
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        LOGGER.finer("test");
        if (event.getUser().isBot()) return;
        
        long messageId = event.getMessageIdLong();
        String emoji = event.getReaction().getEmoji().asUnicode().getFormatted();

        if (!messageIdToPair.containsKey(messageId)) {
            return;
        }

        Pair<String, String> pair = messageIdToPair.get(messageId);

        Path path = Paths.get("fallacy_corrections.json");

        try {
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();

            ObjectMapper mapper = new ObjectMapper();
            String jsonLine = mapper.writeValueAsString(Map.of(
                "role", "user",
                "content", pair.getLeft()
            )) + "\n" + mapper.writeValueAsString(Map.of(
                "role", "assistant",
                "content", pair.getRight()
            ));

            if (emoji.equals("✅")) {
                lines.add(jsonLine);
                Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } else if (emoji.equals("❌")) {
                boolean removed = lines.removeIf(line -> line.contains(pair.getLeft()) && line.contains(pair.getRight()));
                if (removed) {
                    Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // optionally log or handle errors here
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
