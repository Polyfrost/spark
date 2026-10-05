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

package me.lucko.spark.ornithe;

import me.lucko.spark.common.SparkBuildInfo;
import me.lucko.spark.ornithe.plugin.OrnitheClientSparkPlugin;
import me.lucko.spark.ornithe.plugin.OrnitheServerSparkPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class OrnitheSparkMod implements ModInitializer, ClientModInitializer {

    public static final Set<Runnable> SERVER_TICK_START = new CopyOnWriteArraySet<>();
    public static final Set<Runnable> SERVER_TICK_END = new CopyOnWriteArraySet<>();
    public static final Set<Runnable> CLIENT_TICK_START = new CopyOnWriteArraySet<>();
    public static final Set<Runnable> CLIENT_TICK_END = new CopyOnWriteArraySet<>();

    private OrnitheServerSparkPlugin activeServerPlugin;

    @Override
    public void init() {
        MinecraftServerEvents.TICK_START.register(server -> SERVER_TICK_START.forEach(Runnable::run));
        MinecraftServerEvents.TICK_END.register(server -> SERVER_TICK_END.forEach(Runnable::run));
        MinecraftServerEvents.START.register(server -> this.activeServerPlugin = OrnitheServerSparkPlugin.register(this, server));
        MinecraftServerEvents.STOP.register(server -> {
            if (this.activeServerPlugin != null) {
                this.activeServerPlugin.disable();
                this.activeServerPlugin = null;
            }
        });
    }

    @Override
    public void initClient() {
        MinecraftClientEvents.TICK_START.register(minecraft -> CLIENT_TICK_START.forEach(Runnable::run));
        MinecraftClientEvents.TICK_END.register(minecraft -> CLIENT_TICK_END.forEach(Runnable::run));
        MinecraftClientEvents.READY.register(minecraft -> OrnitheClientSparkPlugin.register(this, minecraft));
    }

    public String getVersion() {
        return SparkBuildInfo.VERSION;
    }

    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("spark");
    }
}
