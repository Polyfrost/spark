/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.ornithe.plugin;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.ornithe.OrnitheClassSourceLookup;
import me.lucko.spark.ornithe.OrnitheSparkMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.command.source.CommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class OrnitheSparkPlugin implements SparkPlugin {

    private final OrnitheSparkMod mod;
    private final Logger logger;
    protected final ScheduledExecutorService scheduler;
    protected SparkPlatform platform;

    protected OrnitheSparkPlugin(OrnitheSparkMod mod) {
        this.mod = mod;
        this.logger = LogManager.getLogger("spark");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("spark-ornithe-async-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void enable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
    }

    public abstract boolean hasPermission(CommandSource sender, String permission);

    @Override
    public String getVersion() {
        return this.mod.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return this.mod.getConfigDirectory();
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            this.logger.error(msg);
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            this.logger.warn(msg);
        } else {
            this.logger.info(msg);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            this.logger.error(msg, throwable);
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            this.logger.warn(msg, throwable);
        } else {
            this.logger.info(msg, throwable);
        }
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new OrnitheClassSourceLookup(createClassFinder());
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                FabricLoader.getInstance().getAllMods(),
                mod -> mod.getMetadata().getId(),
                mod -> mod.getMetadata().getVersion().getFriendlyString(),
                mod -> mod.getMetadata().getAuthors().stream()
                        .map(Person::getName)
                        .collect(Collectors.joining(", ")),
                mod -> mod.getMetadata().getDescription(),
                mod -> mod.getMetadata().getType().equals("builtin")
        );
    }

}
