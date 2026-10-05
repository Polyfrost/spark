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

import me.lucko.spark.common.tick.AbstractTickHook;
import me.lucko.spark.common.tick.TickHook;

import java.util.Set;

public class OrnitheTickHook extends AbstractTickHook implements TickHook {
    private final Set<Runnable> tickStartListeners;
    private final Runnable listener = this::onTick;

    public OrnitheTickHook(Set<Runnable> tickStartListeners) {
        this.tickStartListeners = tickStartListeners;
    }

    @Override
    public void start() {
        this.tickStartListeners.add(this.listener);
    }

    @Override
    public void close() {
        this.tickStartListeners.remove(this.listener);
    }

}
