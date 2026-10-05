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

import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.ornithe.OrnitheCommandSender;
import me.lucko.spark.ornithe.OrnithePlatformInfo;
import me.lucko.spark.ornithe.OrnitheSparkMod;
import me.lucko.spark.ornithe.OrnitheTickHook;
import me.lucko.spark.ornithe.OrnitheTickReporter;
import me.lucko.spark.ornithe.OrnitheWorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.server.command.source.CommandSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OrnitheClientSparkPlugin extends OrnitheSparkPlugin {
    private static OrnitheClientSparkPlugin instance;

    public static void register(OrnitheSparkMod mod, Minecraft minecraft) {
        OrnitheClientSparkPlugin plugin = new OrnitheClientSparkPlugin(mod, minecraft);
        plugin.enable();
        instance = plugin;
    }

    public static boolean onChat(String message) {
        OrnitheClientSparkPlugin plugin = instance;
        if (plugin == null) {
            return false;
        }

        String[] split = message.trim().split(" +");
        if (!split[0].equalsIgnoreCase("/" + plugin.getCommandName())) {
            return false;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        plugin.platform.executeCommand(new OrnitheCommandSender(plugin.minecraft.player, plugin), args);
        return true;
    }

    public static List<String> onTabComplete(String text) {
        OrnitheClientSparkPlugin plugin = instance;
        if (plugin == null) {
            return null;
        }

        String prefix = "/" + plugin.getCommandName() + " ";
        if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }

        String[] args = text.substring(prefix.length()).split(" ", -1);
        return plugin.platform.tabCompleteCommand(new OrnitheCommandSender(plugin.minecraft.player, plugin), args);
    }

    private final Minecraft minecraft;
    private final ThreadDumper gameThreadDumper;

    public OrnitheClientSparkPlugin(OrnitheSparkMod mod, Minecraft minecraft) {
        super(mod);
        this.minecraft = minecraft;
        this.gameThreadDumper = new ThreadDumper.Specific(Thread.currentThread());
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public boolean hasPermission(CommandSource sender, String permission) {
        return true;
    }

    @Override
    public Stream<OrnitheCommandSender> getCommandSenders() {
        return this.minecraft.player == null ? Stream.empty() : Stream.of(new OrnitheCommandSender(this.minecraft.player, this));
    }

    @Override
    public TickHook createTickHook() {
        return new OrnitheTickHook(OrnitheSparkMod.CLIENT_TICK_START);
    }

    @Override
    public TickReporter createTickReporter() {
        return new OrnitheTickReporter(
                OrnitheSparkMod.CLIENT_TICK_START,
                OrnitheSparkMod.CLIENT_TICK_END
        );
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new OrnitheWorldInfoProvider.Client(this.minecraft);
    }

    @Override
    public void executeSync(Runnable task) {
        this.minecraft.executeTask(task);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new OrnithePlatformInfo(PlatformInfo.Type.CLIENT);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}
