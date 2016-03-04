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

package com.dsh105.powermessage.core;

import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public interface MessageBuilder {

    /**
     * Gets the raw, colourless text the is represented by all snippets in this message
     *
     * @return Text in this message
     */
    String getText();

    MessageBuilder edit(String snippetContent);

    /**
     * Adds colours to the current snippet of text
     *
     * @param colours Colours to add
     * @return This object
     */
    MessageBuilder colour(ChatColor... colours);

    /**
     * Adds a file event to a PowerMessage
     * <p>
     * Opens a file for the player that clicks the message, where the file path is relative to their computer only
     *
     * @param relativePath Path of the file to open
     * @return This object
     */
    MessageBuilder file(String relativePath);

    /**
     * Adds a link event to a PowerMessage
     * <p>
     * Opens a specific URL link when clicked
     *
     * @param urlLink URL link to open
     * @return This object
     */
    MessageBuilder link(String urlLink);

    /**
     * Adds a suggest event to a PowerMessage
     * <p>
     * Auto-fills a certain command to the clicker's chat box
     *
     * @param commandToSuggest Command to suggest when clicked
     * @return This object
     */
    MessageBuilder suggest(String commandToSuggest);

    /**
     * Adds a perform event to a PowerMessage
     * <p>
     * Performs a command on behalf of the player that clicked
     *
     * @param commandToPerform Command to perform when clicked
     * @return This object
     */
    MessageBuilder perform(String commandToPerform);

    /**
     * Adds a tooltip to a PowerMessage
     * <p>
     * Displays a multiline or single-line tooltip message to the viewer when the message is hovered over
     *
     * @param content Message to show when hovered over
     * @return This object
     */
    MessageBuilder tooltip(String... content);

    /**
     * Adds a tooltip to a PowerMessage
     * <p>
     * Displays a multiline or single-line tooltip message to the viewer when the message is hovered over
     * <p>
     * The provided PowerMessage will be stripped of all events before being added
     *
     * @param powerMessage Content to show when hovered over
     * @return This object
     */
    MessageBuilder tooltip(PowerMessage powerMessage);

    /**
     * Adds an achievement tooltip to a PowerMessage
     * <p>
     * Displays an achievement to the viewer when the message is hovered over
     *
     * @param achievementName Name of the achievement to show
     * @return This object
     */
    MessageBuilder achievementTooltip(String achievementName);

    /**
     * Adds an item tooltip to a PowerMessage
     * <p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemJson JSON value of the item to add
     * @return This object
     */
    MessageBuilder itemTooltip(String itemJson);

    /**
     * Adds an item tooltip to a PowerMessage
     * <p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemContent A group of strings to represent an item with a name and description
     * @return This object
     */
    MessageBuilder itemTooltip(String... itemContent);

    /**
     * Adds an item tooltip to a PowerMessage
     * <p>
     * Displays an item to the viewer when the message is hovered over
     *
     * @param itemStack ItemStack to show
     * @return This object
     */
    MessageBuilder itemTooltip(ItemStack itemStack);

    /**
     * Adds an achievement tooltip to a PowerMessage
     * <p>
     * Displays an achievement to the viewer when the message is hovered over
     *
     * @param which Achievement to show
     * @return This object
     */
    MessageBuilder achievementTooltip(Achievement which);

    /**
     * Adds a statistic tooltip to a PowerMessage
     * <p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which Achievement to show
     * @return This object
     */
    MessageBuilder statisticTooltip(Statistic which);

    /**
     * Adds an item statistic tooltip to a PowerMessage
     * <p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which Statistic to show
     * @param item  Item to show
     * @return This object
     */
    MessageBuilder statisticTooltip(Statistic which, Material item);

    /**
     * Adds an entity statistic tooltip to a PowerMessage
     * <p>
     * Displays a statistic to the viewer when the message is hovered over
     *
     * @param which  Statistic to show
     * @param entity Entity type to show
     * @return This object
     */
    MessageBuilder statisticTooltip(Statistic which, EntityType entity);
}