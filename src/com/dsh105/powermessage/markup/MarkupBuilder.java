/*
 * This file is part of PowerMessage.
 *
 * PowerMessage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerMessage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerMessage.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.powermessage.markup;

import com.dsh105.powermessage.core.PowerMessage;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very, very simple markup builder for JSON messages
 * <p>
 * e.g. Hello world[txt:&6Hover text!]. &3Click to perform a command[cmd:say Hello world!]
 */
public class MarkupBuilder {

    private static final Pattern MARKUP_PATTERN = Pattern.compile("\\[(txt|file|url|scmd|cmd):.+?\\]", Pattern.CASE_INSENSITIVE);

    private StringBuilder raw = new StringBuilder();

    /**
     * Constructs a new markup builder for a {@link com.dsh105.powermessage.core.PowerMessage}
     */
    public MarkupBuilder() {
    }

    /**
     * Adds text to the builder
     *
     * @param raw Text to add
     * @return This builder
     */
    public MarkupBuilder withText(String raw) {
        this.raw.append(raw);
        return this;
    }

    /**
     * Converts the markup to a new {@link PowerMessage}
     *
     * @return PowerMessage constructed from the supplied markup
     */
    public PowerMessage build() {
        PowerMessage powerMessage = new PowerMessage();

        Matcher matcher = MARKUP_PATTERN.matcher(this.raw);

        int next = 0;
        while (next < this.raw.length()) {
            if (matcher.find(next)) {
                if (matcher.start() > next) {
                    powerMessage.then(this.raw.substring(matcher.start(), next));
                }

                String input = ChatColor.translateAlternateColorCodes('&', matcher.group(3));
                switch (matcher.group(2)) {
                    case "txt":
                        powerMessage.tooltip(input);
                        break;
                    case "file":
                        powerMessage.file(input);
                        break;
                    case "url":
                        powerMessage.link(input);
                        break;
                    case "cmd":
                        powerMessage.perform(input);
                        break;
                    case "scmd":
                        powerMessage.suggest(input);
                        break;
                }

                next = matcher.end() + 1;
            } else {
                // We're finished
                powerMessage.then(this.raw.substring(next));
                break;
            }

        }
        return powerMessage;
    }
}