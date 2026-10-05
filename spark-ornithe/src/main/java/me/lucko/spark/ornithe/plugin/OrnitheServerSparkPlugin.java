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

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.ornithe.OrnitheCommandSender;
import me.lucko.spark.ornithe.OrnithePlatformInfo;
import me.lucko.spark.ornithe.OrnithePlayerPingProvider;
import me.lucko.spark.ornithe.OrnitheSparkMod;
import me.lucko.spark.ornithe.OrnitheTickHook;
import me.lucko.spark.ornithe.OrnitheTickReporter;
import me.lucko.spark.ornithe.OrnitheWorldInfoProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.handler.CommandRegistry;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class OrnitheServerSparkPlugin extends OrnitheSparkPlugin implements Command {

    public static OrnitheServerSparkPlugin register(OrnitheSparkMod mod, MinecraftServer server) {
        OrnitheServerSparkPlugin plugin = new OrnitheServerSparkPlugin(mod, server);
        plugin.enable();

        ((CommandRegistry) server.getCommandHandler()).register(plugin);

        return plugin;
    }

    private final MinecraftServer server;
    private final ThreadDumper gameThreadDumper;

    public OrnitheServerSparkPlugin(OrnitheSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
        this.gameThreadDumper = new ThreadDumper.Specific(Thread.currentThread());
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public boolean hasPermission(CommandSource sender, String permission) {
        if (sender instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) sender;
            return this.server.getPlayerManager().isOp(player.getGameProfile())
                    || player.getGameProfile().getName().equals(this.server.getUsername());
        } else {
            return true;
        }
    }

    @Override
    public Stream<OrnitheCommandSender> getCommandSenders() {
        return Stream.concat(
                this.server.getPlayerManager().getAll().stream(),
                Stream.of(this.server)
        ).map(sender -> new OrnitheCommandSender(sender, this));
    }

    @Override
    public TickHook createTickHook() {
        return new OrnitheTickHook(OrnitheSparkMod.SERVER_TICK_START);
    }

    @Override
    public TickReporter createTickReporter() {
        return new OrnitheTickReporter(
                OrnitheSparkMod.SERVER_TICK_START,
                OrnitheSparkMod.SERVER_TICK_END
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new OrnithePlayerPingProvider(this.server);
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new OrnitheWorldInfoProvider.Server(this.server);
    }

    @Override
    public void executeSync(Runnable task) {
        this.server.executeTask(task);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new OrnithePlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public String getName() {
        return getCommandName();
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/" + getCommandName();
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(getCommandName());
    }

    @Override
    public void run(CommandSource source, String[] args) {
        this.platform.executeCommand(new OrnitheCommandSender(source, this), args);
    }

    @Override
    public boolean canUse(CommandSource source) {
        return this.platform.hasPermissionForAnyCommand(new OrnitheCommandSender(source, this));
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] args, BlockPos pos) {
        return this.platform.tabCompleteCommand(new OrnitheCommandSender(source, this), args);
    }

    @Override
    public boolean hasTargetSelectorAt(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Command o) {
        return getName().compareTo(o.getName());
    }

}
