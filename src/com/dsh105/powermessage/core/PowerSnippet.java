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

import com.dsh105.powermessage.action.ActionEvent;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;

/**
 * Represents a particular snippet of a {@link com.dsh105.powermessage.core.PowerMessage}
 */
public class PowerSnippet implements JsonWritable, Cloneable, ConfigurationSerializable {

    private static final String SERIALIZED_TEXT = "text";
    private static final String SERIALIZED_COLOURS = "colours";
    private static final String SERIALIZED_ACTION_EVENTS = "actionEvents";

    protected static final BiMap<ChatColor, String> STYLE_TO_NAME_MAP;

    static {
        ConfigurationSerialization.registerClass(PowerSnippet.class);

        ImmutableBiMap.Builder<ChatColor, String> builder = ImmutableBiMap.builder();
        for (final ChatColor style : ChatColor.values()) {
            if (!style.isFormat()) {
                continue;
            }

            String styleName;
            switch (style) {
                case MAGIC:
                    styleName = "obfuscated";
                    break;
                case UNDERLINE:
                    styleName = "underlined";
                    break;
                default:
                    styleName = style.name().toLowerCase();
                    break;
            }

            builder.put(style, styleName);
        }
        STYLE_TO_NAME_MAP = builder.build();
    }

    private String text;
    private ArrayList<ChatColor> colours = new ArrayList<>();
    private ArrayList<ActionEvent> actionEvents = new ArrayList<>();

    /**
     * Constructs a new PowerSnippet with text
     *
     * @param text Textual content to be included in the snippet
     */
    public PowerSnippet(String text) {
        this.setText(text);
    }

    /**
     * Gets the text of a snippet
     *
     * @return Textual content of a snippet
     */
    public String getText() {
        return text;
    }

    protected void setText(String text) {
        this.text = ChatColor.stripColor(text);
    }

    /**
     * Gets the colours of a snippet
     *
     * @return Colours in a snippet
     */
    public List<ChatColor> getColours() {
        return Collections.unmodifiableList(this.colours);
    }

    /**
     * Gets the action events of a snippet
     *
     * @return Action events of a snippet
     */
    public List<ActionEvent> getActionEvents() {
        return Collections.unmodifiableList(this.actionEvents);
    }

    /**
     * Adds colours to a snippet
     *
     * @param colours Colours to add
     * @return This object
     */
    public PowerSnippet withColour(ChatColor... colours) {
        Collections.addAll(this.colours, colours);
        return this;
    }

    /**
     * Adds events to a snippet
     *
     * @param events Events to add
     * @return This object
     */
    public PowerSnippet withEvents(ActionEvent... events) {
        for (ActionEvent event : events) {
            this.withEvent(event.getActionType(), event.getName(), event.getData());
        }
        return this;
    }

    /**
     * Adds an event to a snippet
     *
     * @param eventType Type of event to add
     * @param eventName Name of event being added
     * @param eventData Data of event being added
     * @return This object
     */
    public PowerSnippet withEvent(String eventType, String eventName, String eventData) {
        ActionEvent event = new ActionEvent(eventType).withName(eventName).withData(eventData);
        ActionEvent existing = getActionEvent(eventType, eventName);
        if (existing == null) {
            this.actionEvents.add(event);
            return this;
        }

        StringBuilder builder = new StringBuilder()
                .append(existing.getData())
                .append("\n")
                .append(event.getData());

        this.actionEvents.add(new ActionEvent(eventType).withName(eventName).withData(builder.toString()));
        return this;
    }

    /**
     * Gets an existing event that is represented by the given type and name
     *
     * @param eventType Type of event to search for
     * @param eventName Name of event to search for
     * @return Event if it exists, null if not
     */
    public ActionEvent getActionEvent(String eventType, String eventName) {
        for (ActionEvent event : getActionEvents()) {
            if (event.getActionType().equals(eventType) && event.getName().equals(eventName)) {
                return event;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put(SERIALIZED_TEXT, text);
        serialized.put(SERIALIZED_COLOURS, colours);
        serialized.put(SERIALIZED_ACTION_EVENTS, actionEvents);
        return serialized;
    }

    public static PowerSnippet deserialize(Map<String, Object> serialized) {
        if (!serialized.containsKey(SERIALIZED_TEXT)) {
            throw new IllegalArgumentException("Failed to deserialize PowerSnippet from provided data");
        }
        PowerSnippet snippet = new PowerSnippet((String) serialized.get(SERIALIZED_TEXT));
        snippet.colours = (ArrayList<ChatColor>) serialized.get(SERIALIZED_COLOURS);
        snippet.actionEvents = (ArrayList<ActionEvent>) serialized.get(SERIALIZED_ACTION_EVENTS);
        return snippet;
    }

    @Override
    public JsonWriter writeJson(JsonWriter writer) throws IOException {
        writer.beginObject().name("text").value(text);

        for (ChatColor colour : colours) {
            if (colour.isFormat()) {
                writer.name(STYLE_TO_NAME_MAP.get(colour)).value(true);
            } else {
                writer.name("color").value(colour.name().toLowerCase());
            }
        }

        for (ActionEvent event : actionEvents) {
            event.writeJson(writer);
        }

        return writer.endObject();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PowerSnippet snippet = (PowerSnippet) super.clone();
        snippet.colours = (ArrayList<ChatColor>) colours.clone();
        snippet.actionEvents = (ArrayList<ActionEvent>) actionEvents.clone();
        return snippet;
    }
}