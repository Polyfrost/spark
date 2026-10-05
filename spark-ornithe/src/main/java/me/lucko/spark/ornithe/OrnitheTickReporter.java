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

import me.lucko.spark.common.tick.SimpleTickReporter;
import me.lucko.spark.common.tick.TickReporter;

import java.util.Set;

public class OrnitheTickReporter extends SimpleTickReporter implements TickReporter {
    private final Set<Runnable> tickStartListeners;
    private final Set<Runnable> tickEndListeners;
    private final Runnable startListener = this::onStart;
    private final Runnable endListener = this::onEnd;

    public OrnitheTickReporter(Set<Runnable> tickStartListeners, Set<Runnable> tickEndListeners) {
        this.tickStartListeners = tickStartListeners;
        this.tickEndListeners = tickEndListeners;
    }

    @Override
    public void start() {
        this.tickStartListeners.add(this.startListener);
        this.tickEndListeners.add(this.endListener);
    }

    @Override
    public void close() {
        this.tickStartListeners.remove(this.startListener);
        this.tickEndListeners.remove(this.endListener);
        super.close();
    }

}
