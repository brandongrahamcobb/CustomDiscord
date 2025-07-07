/*  Application.java The primary purpose of this class is to integrate
 *  local and remote AI tools.
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
package com.brandongcobb.discord;

import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
public class Application {
   
    
    private JDA api;
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());
    public static final String BLURPLE = "\033[38;5;61m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String CYAN = "\u001B[36m";
    public static final String DODGER_BLUE = "\u001B[38;5;33m";
    public static final String FUCHSIA = "\033[38;5;201m";
    public static final String GOLD = "\033[38;5;220m";
    public static final String GREEN = "\u001B[32m";
    public static final String LIME = "\033[38;5;154m";
    public static final String NAVY = "\u001B[38;5;18m";
    public static final String ORANGE = "\033[38;5;208m";
    public static final String PINK = "\033[38;5;205m";
    public static final String PURPLE = "\u001B[35m";
    public static final String RED = "\u001B[31m";
    public static final String RESET = "\u001B[0m";
    public static final String SKY_BLUE = "\u001B[38;5;117m";
    public static final String TEAL = "\u001B[38;5;30m";
    public static final String VIOLET = "\033[38;5;93m";
    public static final String WHITE = "\u001B[37m";
    public static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) {
        LOGGER.setLevel(Level.FINER);
        for (Handler h : LOGGER.getParent().getHandlers()) {
            h.setLevel(Level.FINER);
        }
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public CompletableFuture<Application> completeGetAppInstance() {
        return CompletableFuture.completedFuture(this);
    }
}
