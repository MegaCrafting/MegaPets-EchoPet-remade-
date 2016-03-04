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

import com.dsh105.commodus.ItemUtil;
import com.dsh105.commodus.StringUtil;
import com.dsh105.commodus.reflection.Reflection;
import com.dsh105.powermessage.exception.InvalidMessageException;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group implements MessageBuilder {

    private PowerMessage powerMessage;
    private int start;
    private int end;

    public Group(PowerMessage powerMessage, int start, int end) {
        this.powerMessage = powerMessage;
        this.start = start;
        this.end = end;
    }

    public Group(PowerMessage powerMessage, int groupCount) {
        this(powerMessage, powerMessage.getSnippets().size() - groupCount, powerMessage.getSnippets().size());
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    protected List<PowerSnippet> getSnippets() {
        ArrayList<PowerSnippet> snippets = new ArrayList<>();
        for (int i = start; i < end; i++) {
            snippets.add(powerMessage.getSnippet(i));
        }
        return Collections.unmodifiableList(snippets);
    }

    public PowerMessage exit() {
        return powerMessage;
    }

    @Override
    public String getText() {
        StringBuilder builder = new StringBuilder();
        for (PowerSnippet snippet : getSnippets()) {
            builder.append(snippet.getText());
        }
        return builder.toString();
    }

    @Override
    public Group edit(String snippetContent) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.setText(snippetContent);
        }
        return this;
    }

    @Override
    public Group colour(ChatColor... colours) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withColour(colours);
        }
        return this;
    }

    @Override
    public Group file(String relativePath) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "open_file", relativePath);
        }
        return this;
    }

    @Override
    public Group link(String urlLink) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "open_url", urlLink);
        }
        return this;
    }

    @Override
    public Group suggest(String commandToSuggest) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "suggest_command", commandToSuggest);
        }
        return this;
    }

    @Override
    public Group perform(String commandToPerform) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("click", "run_command", commandToPerform);
        }
        return this;
    }

    @Override
    public Group tooltip(String... content) {
        if (content == null || content.length <= 0) {
            throw new InvalidMessageException("Content cannot be empty");
        }

        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_text", content.length == 1 ? content[0] : StringUtil.combineArray(0, "\n", content));
        }
        return this;
    }

    @Override
    public Group tooltip(PowerMessage powerMessage) {
        if (powerMessage.getContent() == null || powerMessage.getContent().length() <= 0) {
            throw new InvalidMessageException("Content cannot be empty");
        }

        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_text", powerMessage.getContent());
        }
        return this;
    }

    @Override
    public Group achievementTooltip(String achievementName) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_achievement", "achievement." + achievementName);
        }
        return this;
    }

    @Override
    public Group itemTooltip(String itemJson) {
        for (PowerSnippet snippet : getSnippets()) {
            snippet.withEvent("hover", "show_item", itemJson);
        }
        return this;
    }

    @Override
    public Group itemTooltip(String... itemContent) {
        return itemTooltip(ItemUtil.getItem(itemContent));
    }

    @Override
    public Group itemTooltip(ItemStack itemStack) {
        Object nmsCopy = Reflection.invokeStatic(Reflection.getMethod(Reflection.getOBCClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class), itemStack);
        Object nbtData = Reflection.invoke(Reflection.getMethod(nmsCopy.getClass(), "save", Reflection.getNMSClass("NBTTagCompound")), Reflection.newInstance(Reflection.getConstructor(Reflection.getNMSClass("NBTTagCompound"))));
        return itemTooltip(nbtData.toString());
    }

    @Override
    public Group achievementTooltip(Achievement which) {
        Object achievement = Reflection.invokeStatic(Reflection.getMethod(Reflection.getOBCClass("CraftStatistic"), "getNMSAchievement", Achievement.class), which);
        return achievementTooltip((String) Reflection.getFieldValue(Reflection.getNMSClass("Achievement"), achievement, "name"));
    }

    @Override
    public Group statisticTooltip(Statistic which) {
        if (which.getType() != Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires an additional " + which.getType() + " parameter!");
        }

        Object statistic = Reflection.invokeStatic(Reflection.getMethod(Reflection.getOBCClass("CraftStatistic"), "getNMSStatistic", Statistic.class), which);
        return achievementTooltip((String) Reflection.getFieldValue(Reflection.getNMSClass("Statistic"), statistic, "name"));
    }

    @Override
    public Group statisticTooltip(Statistic which, Material item) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if ((which.getType() == Statistic.Type.BLOCK && item.isBlock()) || which.getType() == Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Object statistic = Reflection.invokeStatic(Reflection.getMethod(Reflection.getOBCClass("CraftStatistic"), "getMaterialStatistic", Statistic.class, Material.class), which, item);
        return achievementTooltip((String) Reflection.getFieldValue(Reflection.getNMSClass("Statistic"), statistic, "name"));
    }

    @Override
    public Group statisticTooltip(Statistic which, EntityType entity) {
        if (which.getType() == Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("That statistic requires no additional parameter!");
        }

        if (which.getType() != Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("Wrong parameter type for that statistic - needs " + which.getType() + "!");
        }

        Object statistic = Reflection.invokeStatic(Reflection.getMethod(Reflection.getOBCClass("CraftStatistic"), "getEntityStatistic", Statistic.class, EntityType.class), which, entity);
        return achievementTooltip((String) Reflection.getFieldValue(Reflection.getNMSClass("Statistic"), statistic, "name"));
    }
}