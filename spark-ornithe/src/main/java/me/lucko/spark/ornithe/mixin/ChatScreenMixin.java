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

package me.lucko.spark.ornithe.mixin;

import me.lucko.spark.ornithe.plugin.OrnitheClientSparkPlugin;
import net.minecraft.client.gui.screen.game.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow private List<String> messageHistory;

    @Inject(method = "goThroughHistory(Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void spark$onRequestSuggestions(String text, String word, CallbackInfo ci) {
        List<String> suggestions = OrnitheClientSparkPlugin.onTabComplete(text);
        if (suggestions != null) {
            for (String suggestion : suggestions) {
                if (!suggestion.isEmpty()) {
                    this.messageHistory.add(suggestion);
                }
            }
            ci.cancel();
        }
    }

}
