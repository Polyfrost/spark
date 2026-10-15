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

import me.lucko.spark.proto.SparkHeapProtos.HeapData;
import me.lucko.spark.proto.SparkHeapProtos.HeapEntry;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.StackTraceNode;
import me.lucko.spark.proto.SparkSamplerProtos.ThreadNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeatherRemapperTest {

    @Test
    public void testRemapSampler() {
        SamplerData.Builder proto = SamplerData.newBuilder()
                .addThreads(ThreadNode.newBuilder()
                        .setName("Render thread")
                        .addChildren(StackTraceNode.newBuilder()
                                .setClassName("net.minecraft.unmapped.C_03140863")
                                .setMethodName("m_08357947")
                                .setMethodDesc("(FJLnet/minecraft/unmapped/C_24384787;[Ljava/lang/String;)V"))
                        .addChildren(StackTraceNode.newBuilder()
                                .setClassName("java.lang.Thread")
                                .setMethodName("run")))
                .putClassSources("net.minecraft.unmapped.C_03140863", "a")
                .putMethodSources("net.minecraft.unmapped.C_03140863;m_08357947;(Lnet/minecraft/unmapped/C_24384787;)V", "b")
                .putLineSources("net.minecraft.unmapped.C_03140863;42", "c");

        FeatherRemapper.remap(proto);

        ThreadNode thread = proto.getThreads(0);
        assertEquals("Render thread", thread.getName());
        assertEquals("net.minecraft.client.render.GameRenderer", thread.getChildren(0).getClassName());
        assertEquals("render", thread.getChildren(0).getMethodName());
        assertEquals("(FJLnet/minecraft/client/render/world/WorldRenderer;[Ljava/lang/String;)V", thread.getChildren(0).getMethodDesc());
        assertEquals("java.lang.Thread", thread.getChildren(1).getClassName());
        assertEquals("run", thread.getChildren(1).getMethodName());

        assertEquals("a", proto.getClassSourcesMap().get("net.minecraft.client.render.GameRenderer"));
        assertEquals("b", proto.getMethodSourcesMap().get("net.minecraft.client.render.GameRenderer;render;(Lnet/minecraft/client/render/world/WorldRenderer;)V"));
        assertEquals("c", proto.getLineSourcesMap().get("net.minecraft.client.render.GameRenderer;42"));
    }

    @Test
    public void testRemapHeap() {
        HeapData.Builder proto = HeapData.newBuilder()
                .addEntries(HeapEntry.newBuilder().setType("net.minecraft.unmapped.C_03140863"))
                .addEntries(HeapEntry.newBuilder().setType("net.minecraft.unmapped.C_24384787[][]"))
                .addEntries(HeapEntry.newBuilder().setType("byte[]"));

        FeatherRemapper.remap(proto);

        assertEquals("net.minecraft.client.render.GameRenderer", proto.getEntries(0).getType());
        assertEquals("net.minecraft.client.render.world.WorldRenderer[][]", proto.getEntries(1).getType());
        assertEquals("byte[]", proto.getEntries(2).getType());
    }

}
