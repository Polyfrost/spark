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
 */package me.lucko.spark.forge.plugin;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.forge.Forge189ClassSourceLookup;
import me.lucko.spark.forge.Forge189CommandSender;
import me.lucko.spark.forge.Forge189SparkMod;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

public abstract class Forge189SparkPlugin implements SparkPlugin, ICommand {

    private final Forge189SparkMod mod;
    private final Logger logger;
    protected final ScheduledExecutorService scheduler;
    protected SparkPlatform platform;

    protected Forge189SparkPlugin(Forge189SparkMod mod) {
        this.mod = mod;
        this.logger = LogManager.getLogger("spark");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("spark-forge-async-worker");
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

    public abstract boolean hasPermission(ICommandSender sender, String permission);

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
        return new Forge189ClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                Loader.instance().getModList(),
                ModContainer::getModId,
                ModContainer::getVersion,
                mod -> String.join(", ", mod.getMetadata().authorList),
                mod -> mod.getMetadata().description
        );
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList(getCommandName());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        this.platform.executeCommand(new Forge189CommandSender(sender, this), args);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return this.platform.tabCompleteCommand(new Forge189CommandSender(sender, this), args);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(new Forge189CommandSender(sender, this));
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getCommandName().compareTo(o.getCommandName());
    }

}
