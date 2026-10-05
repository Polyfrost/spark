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
 */package me.lucko.spark.forge;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Forge189ClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    private final Path modsDirectory;
    private final Map<Path, String> pathToModMap = new HashMap<>();

    public Forge189ClassSourceLookup() {
        this.modsDirectory = new File(Launch.minecraftHome, "mods").toPath().toAbsolutePath().normalize();
        for (ModContainer mod : Loader.instance().getModList()) {
            File source = mod.getSource();
            if (source != null && source.isFile()) {
                this.pathToModMap.putIfAbsent(source.toPath().toAbsolutePath().normalize(), mod.getModId());
            }
        }
    }

    @Override
    public String identify(Path path) {
        String id = this.pathToModMap.get(path);
        if (id != null) {
            return id;
        }

        if (!path.startsWith(this.modsDirectory)) {
            return null;
        }

        return formatFileName(this.modsDirectory.relativize(path).toString());
    }
}
