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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Remaps sampler and heap data from Ornithe intermediary to Feather names.
 */
public final class FeatherRemapper {
    private FeatherRemapper() {
    }

    private static final class Holder {
        // intermediary -> named, for classes (internal names) and methods
        static final Map<String, String> MAPPINGS = load();

        private static Map<String, String> load() {
            try (InputStream in = FeatherRemapper.class.getResourceAsStream("/assets/spark/mappings.txt")) {
                if (in == null) {
                    return Collections.emptyMap();
                }
                Map<String, String> map = new HashMap<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = reader.readLine()) != null; ) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        map.put(line.substring(0, tab), line.substring(tab + 1));
                    }
                }
                return map;
            } catch (IOException e) {
                return Collections.emptyMap();
            }
        }
    }

    public static void remap(SamplerData.Builder proto) {
        if (Holder.MAPPINGS.isEmpty()) {
            return;
        }

        for (int i = 0; i < proto.getThreadsCount(); i++) {
            ThreadNode.Builder thread = proto.getThreads(i).toBuilder();
            for (int j = 0; j < thread.getChildrenCount(); j++) {
                StackTraceNode node = thread.getChildren(j);
                StackTraceNode.Builder remapped = node.toBuilder()
                        .setClassName(className(node.getClassName()))
                        .setMethodName(methodName(node.getMethodName()));
                if (!node.getMethodDesc().isEmpty()) {
                    remapped.setMethodDesc(descriptor(node.getMethodDesc()));
                }
                thread.setChildren(j, remapped);
            }
            proto.setThreads(i, thread);
        }

        remapKeys(proto.getClassSourcesMap(), proto::clearClassSources, proto::putAllClassSources);
        remapKeys(proto.getMethodSourcesMap(), proto::clearMethodSources, proto::putAllMethodSources);
        remapKeys(proto.getLineSourcesMap(), proto::clearLineSources, proto::putAllLineSources);
    }

    public static void remap(HeapData.Builder proto) {
        if (Holder.MAPPINGS.isEmpty()) {
            return;
        }

        for (int i = 0; i < proto.getEntriesCount(); i++) {
            HeapEntry entry = proto.getEntries(i);
            String type = entry.getType();
            int array = type.indexOf('[');
            String remapped = array < 0 ? className(type) : className(type.substring(0, array)) + type.substring(array);
            proto.setEntries(i, entry.toBuilder().setType(remapped));
        }
    }

    private static void remapKeys(Map<String, String> sources, Runnable clear, Consumer<Map<String, String>> put) {
        if (sources.isEmpty()) {
            return;
        }
        Map<String, String> remapped = new LinkedHashMap<>(sources.size());
        sources.forEach((key, value) -> remapped.put(sourceKey(key), value));
        clear.run();
        put.accept(remapped);
    }

    // see ClassSourceLookup.MethodCall / MethodCallByLine#toString
    private static String sourceKey(String key) {
        String[] parts = key.split(";", 3);
        parts[0] = className(parts[0]);
        if (parts.length > 1) {
            parts[1] = methodName(parts[1]);
        }
        if (parts.length > 2) {
            parts[2] = descriptor(parts[2]);
        }
        return String.join(";", parts);
    }

    private static String className(String name) {
        String mapped = Holder.MAPPINGS.get(name.replace('.', '/'));
        return mapped == null ? name : mapped.replace('/', '.');
    }

    private static String methodName(String name) {
        return Holder.MAPPINGS.getOrDefault(name, name);
    }

    private static String descriptor(String descriptor) {
        StringBuilder out = new StringBuilder(descriptor.length());
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c != 'L') {
                out.append(c);
                i++;
                continue;
            }
            int end = descriptor.indexOf(';', i);
            if (end < 0) {
                out.append(descriptor, i, descriptor.length());
                break;
            }
            String internal = descriptor.substring(i + 1, end);
            out.append('L').append(Holder.MAPPINGS.getOrDefault(internal, internal)).append(';');
            i = end + 1;
        }
        return out.toString();
    }
}
