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

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.chunk.ClientChunkCache;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.util.TypeInstanceMultiMap;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public abstract class OrnitheWorldInfoProvider implements WorldInfoProvider {

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return Collections.emptyList();
    }

    public static final class Server extends OrnitheWorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        private static String dimensionKey(ServerWorld world) {
            return world.dimension.getName() + " (" + world.dimension.getId() + ")";
        }

        @Override
        public ChunksResult<OrnitheChunkInfo> pollChunks() {
            ChunksResult<OrnitheChunkInfo> data = new ChunksResult<>();

            for (ServerWorld world : this.server.worlds) {
                ArrayList<OrnitheChunkInfo> list = new ArrayList<>();
                ChunkSource chunkSource = world.getChunkSource();
                if (chunkSource instanceof ServerChunkCache) {
                    for (WorldChunk chunk : ((ServerChunkCache) chunkSource).getChunks()) {
                        list.add(new OrnitheChunkInfo(chunk));
                    }
                }
                data.put(dimensionKey(world), list);
            }

            return data;
        }

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getPlayerCount();
            int entities = 0;
            int blockEntities = 0;
            int chunks = 0;

            for (ServerWorld world : this.server.worlds) {
                entities += world.entities.size();
                blockEntities += world.blockEntities.size();
                chunks += world.getChunkSource().size();
            }

            return new CountsResult(players, entities, blockEntities, chunks);
        }

        @Override
        public GameRulesResult pollGameRules() {
            GameRulesResult data = new GameRulesResult();
            GameRules defaults = new GameRules();

            for (ServerWorld world : this.server.worlds) {
                GameRules rules = world.getGameRules();
                for (String rule : rules.getAll()) {
                    data.putDefault(rule, defaults.get(rule));
                    data.put(rule, dimensionKey(world), rules.get(rule));
                }
            }

            return data;
        }
    }

    public static final class Client extends OrnitheWorldInfoProvider {
        private final Minecraft client;

        public Client(Minecraft client) {
            this.client = client;
        }

        @Override
        public ChunksResult<OrnitheChunkInfo> pollChunks() {
            ClientWorld world = this.client.world;
            if (world == null) {
                return null;
            }

            ChunksResult<OrnitheChunkInfo> data = new ChunksResult<>();

            ArrayList<OrnitheChunkInfo> list = new ArrayList<>();
            ChunkSource chunkSource = world.getChunkSource();
            if (chunkSource instanceof ClientChunkCache) {
                for (WorldChunk chunk : ((ClientChunkCache) chunkSource).chunks) {
                    list.add(new OrnitheChunkInfo(chunk));
                }
            }

            data.put(world.dimension.getName(), list);

            return data;
        }

        @Override
        public CountsResult pollCounts() {
            ClientWorld world = this.client.world;
            if (world == null) {
                return null;
            }

            return new CountsResult(-1, world.entities.size(), world.blockEntities.size(), world.getChunkSource().size());
        }

        @Override
        public GameRulesResult pollGameRules() {
            return null;
        }
    }

    static final class OrnitheChunkInfo extends AbstractChunkInfo<Class<? extends Entity>> {
        private final CountMap<Class<? extends Entity>> entityCounts;

        OrnitheChunkInfo(WorldChunk chunk) {
            super(chunk.chunkX, chunk.chunkZ);

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            for (TypeInstanceMultiMap<Entity> entityList : chunk.getEntities()) {
                for (Entity entity : entityList) {
                    this.entityCounts.increment(entity.getClass());
                }
            }
        }

        @Override
        public CountMap<Class<? extends Entity>> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(Class<? extends Entity> type) {
            return Entities.TYPE_TO_KEY.get(type);
        }
    }

}
