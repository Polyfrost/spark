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

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.ornithe.plugin.OrnitheSparkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.text.Text;

import java.util.UUID;

public class OrnitheCommandSender extends AbstractCommandSender<CommandSource> {
    private final OrnitheSparkPlugin plugin;

    public OrnitheCommandSender(CommandSource source, OrnitheSparkPlugin plugin) {
        super(source);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        if (super.delegate instanceof PlayerEntity) {
            return ((PlayerEntity) super.delegate).getGameProfile().getName();
        } else if (super.delegate instanceof MinecraftServer) {
            return "Console";
        } else {
            return "unknown:" + super.delegate.getClass().getSimpleName();
        }
    }

    @Override
    public UUID getUniqueId() {
        if (super.delegate instanceof PlayerEntity) {
            return ((PlayerEntity) super.delegate).getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        String json = GsonComponentSerializer.colorDownsamplingGson().serialize(message);
        this.plugin.executeSync(() -> super.delegate.sendMessage(Text.Serializer.fromJson(json)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.plugin.hasPermission(super.delegate, permission);
    }
}
