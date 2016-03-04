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

import com.dsh105.commodus.ServerUtil;
import com.dsh105.commodus.paginator.Pageable;
import com.dsh105.commodus.reflection.Reflection;
import com.dsh105.powermessage.exception.InvalidMessageException;
import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a message that internally manipulates JSON to allow the sending of fancy, interactive messages to players
 */
public class PowerMessage implements MessageBuilder, Pageable, JsonWritable, Cloneable, ConfigurationSerializable, Iterable<PowerSnippet> {

    protected static final Pattern COLOUR_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([0-9A-FK-OR])", Pattern.CASE_INSENSITIVE);

    private static final String SERIALIZED_SNIPPETS = "snippets";
    private static final JsonParser jsonParser = new JsonParser();

    private static Class<?> CHAT_PACKET_CLASS;
    private static Method CHAT_FROM_JSON;

    static {
        ConfigurationSerialization.registerClass(PowerMessage.class);

        if (ServerUtil.getBukkitVersion().isCompatible("1.7")) {
            Class<?> chatSerialiser;
            if (ServerUtil.getBukkitVersion().isSupported("1.8.1")) {
                chatSerialiser = Reflection.getNMSClass("ChatSerializer");
            } else {
                chatSerialiser = Reflection.getNMSClass("IChatBaseComponent$ChatSerializer");
            }
            for (Method method : chatSerialiser.getDeclaredMethods()) {
                if (method.getReturnType().equals(Reflection.getNMSClass("IChatBaseComponent")) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class)) {
                    CHAT_FROM_JSON = method;
                    break;
                }
            }

            ArrayList<Method> packetMethods = new ArrayList<>();
            ArrayList<Field> packetFields = new ArrayList<>();
            for (Method method : Reflection.getNMSClass("EnumProtocol").getDeclaredMethods()) {
                if (Map.class.isAssignableFrom(method.getReturnType()) && method.getParameterTypes().length == 0) {
                    method.setAccessible(true);
                    packetMethods.add(method);
                }
            }

            Map packetPlayMap;

            if (!packetMethods.isEmpty()) {
                packetPlayMap = (Map) Reflection.invoke(packetMethods.get(0), Reflection.getNMSClass("EnumProtocol").getEnumConstants()[1]);
            } else {
                for (Field field : Reflection.getNMSClass("EnumProtocol").getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        packetFields.add(field);
                    }
                }
                packetPlayMap = (Map) Reflection.getFieldValue(packetFields.get(1), Reflection.getNMSClass("EnumProtocol").getEnumConstants()[1]);
            }

            // temporary solution
            
            String fullName = "net.minecraft.server." + ServerUtil.getServerVersion() + ".EnumProtocolDirection";
            Class enumProtocolDirection = null;
            try {
                enumProtocolDirection = Class.forName(fullName);
            } catch (ClassNotFoundException ignored) {
            }
            if (enumProtocolDirection != null) {
                packetPlayMap = (Map) packetPlayMap.get(enumProtocolDirection.getEnumConstants()[1]);
            }
            CHAT_PACKET_CLASS = (Class<?>) packetPlayMap.get(0x02);

            try {
                CHAT_PACKET_CLASS.getConstructor(Reflection.getNMSClass("IChatBaseComponent"));
            } catch (NoSuchMethodException e) {
                // This is more of a backup
                CHAT_PACKET_CLASS = Reflection.getNMSClass("PacketPlayOutChat");
            }
        }
    }

    private ArrayList<PowerSnippet> snippets = new ArrayList<>();
    private String rawJson;
    private boolean convertedToJson;
    private Group currentGroup;

    /**
     * Constructs a new, empty PowerMessage
     */
    public PowerMessage() {
    }

    /**
     * Constructs a new PowerMessage with a starting text snippet
     *
     * @param firstSnippet First snippet to construct the message with
     */
    public PowerMessage(String firstSnippet) {
        then(firstSnippet);
    }

    @Override
    public String getContent() {
        StringBuilder result = new StringBuilder();
        for (PowerSnippet snippet : getSnippets()) {
            for (ChatColor colour : snippet.getColours()) {
                result.append(colour);
            }
            result.append(snippet.getText());
        }
        return result.toString();
    }

    /**
     * Sends a message to a Bukkit {@link org.bukkit.command.CommandSender}
     *
     * @param sender Whom to send the message to
     * @return This object
     */
    @Override
    public PowerMessage send(CommandSender sender) {
        if (sender instanceof Player) {
            send((Player) sender);
        } else {
            sender.sendMessage(getContent());
        }
        return this;
    }

    /**
     * Sends a message to a group of Bukkit {@link org.bukkit.command.CommandSender}s
     *
     * @param senders Whom to send the message to
     * @return This object
     */
    public PowerMessage send(CommandSender... senders) {
        for (CommandSender sender : senders) {
            send(sender);
        }
        return this;
    }

    /**
     * Sends a message to a Bukkit {@link org.bukkit.entity.Player}
     *
     * @param player Whom to send the message to
     * @return This object
     */
    public PowerMessage send(Player player) {
        if (ServerUtil.getBukkitVersion().isCompatible("1.7")) {
            Object chatComponent = Reflection.invokeStatic(CHAT_FROM_JSON, toJson());
            Object packet = Reflection.newInstance(Reflection.getConstructor(CHAT_PACKET_CLASS, Reflection.getNMSClass("IChatBaseComponent")), chatComponent);
            Object handle = Reflection.invoke(Reflection.getMethod(player.getClass(), "getHandle"), player);
            Object connection = Reflection.getFieldValue(handle, "playerConnection");
            Reflection.invoke(Reflection.getMethod(connection.getClass(), "sendPacket", Reflection.getNMSClass("Packet")), connection, packet);
        } else {
            player.sendMessage(getContent());
        }
        return this;
    }

    /**
     * Sends this message to a group of Bukkit {@link org.bukkit.entity.Player}s
     *
     * @param players Whom to send the message to
     * @return This object
     */
    public PowerMessage send(Player... players) {
        for (Player player : players) {
            send(player);
        }
        return this;
    }

    @Override
    public String getText() {
        return currentGroup.getText();
    }

    public PowerSnippet getSnippet(String content) {
        for (PowerSnippet snippet : getSnippets()) {
            if (snippet.getText().equals(content)) {
                return snippet;
            }
        }
        return null;
    }

    public PowerMessage clear() {
        this.snippets.clear();
        currentGroup = null;
        convertedToJson = false;
        return this;
    }

    @Override
    public PowerMessage edit(String snippetContent) {
        currentGroup.edit(snippetContent);
        return this;
    }

    @Override
    public PowerMessage colour(ChatColor... colours) {
        currentGroup.colour(colours);
        return this;
    }

    @Override
    public PowerMessage file(String relativePath) {
        currentGroup.file(relativePath);
        return this;
    }

    @Override
    public PowerMessage link(String urlLink) {
        currentGroup.link(urlLink);
        return this;
    }

    @Override
    public PowerMessage suggest(String commandToSuggest) {
        currentGroup.suggest(commandToSuggest);
        return this;
    }

    @Override
    public PowerMessage perform(String commandToPerform) {
        currentGroup.perform(commandToPerform);
        return this;
    }

    @Override
    public PowerMessage tooltip(String... content) {
        currentGroup.tooltip(content);
        return this;
    }

    @Override
    public PowerMessage tooltip(PowerMessage powerMessage) {
        currentGroup.tooltip(powerMessage);
        return this;
    }

    @Override
    public PowerMessage achievementTooltip(String achievementName) {
        currentGroup.achievementTooltip(achievementName);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(String itemJson) {
        currentGroup.itemTooltip(itemJson);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(String... itemContent) {
        currentGroup.itemTooltip(itemContent);
        return this;
    }

    @Override
    public PowerMessage itemTooltip(ItemStack itemStack) {
        currentGroup.itemTooltip(itemStack);
        return this;
    }

    @Override
    public PowerMessage achievementTooltip(Achievement which) {
        currentGroup.achievementTooltip(which);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which) {
        currentGroup.statisticTooltip(which);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which, Material item) {
        currentGroup.statisticTooltip(which, item);
        return this;
    }

    @Override
    public PowerMessage statisticTooltip(Statistic which, EntityType entity) {
        currentGroup.statisticTooltip(which, entity);
        return this;
    }

    /**
     * Begins construction of a new message snippet
     *
     * @param snippetContent Content to begin the new snippet with
     * @return This object
     */
    public PowerMessage then(String snippetContent) {
        String content = ChatColor.translateAlternateColorCodes('&', snippetContent);
        int groupCount = 0;
        if (content.length() > 0) {
            ArrayList<ChatColor> colours = new ArrayList<>();
            Matcher colourMatcher = COLOUR_PATTERN.matcher(content);
            int lastEnd = 0;
            while (colourMatcher.find()) {
                if (colourMatcher.start() > lastEnd) {
                    then(new PowerSnippet(content.substring(lastEnd, colourMatcher.start()))).colour(colours.toArray(new ChatColor[0]));
                    groupCount++;
                }

                ChatColor colour = ChatColor.getByChar(colourMatcher.group(1));
                if (colour == ChatColor.RESET) {
                    colours.clear();
                } else {
                    colours.add(colour);
                }
                lastEnd = colourMatcher.end();
            }
            if (lastEnd < (content.length() - 1)) {
                then(new PowerSnippet(content.substring(lastEnd, content.length()))).colour(colours.toArray(new ChatColor[0]));
                groupCount++;
            }
            // Group everything together so that changes can be applied to all of them
            group(groupCount);
        }
        return this;
    }

    /**
     * Begins construction of a new message snippet
     *
     * @param snippetContent Content to begin the new snippet with
     * @return This object
     */
    public PowerMessage then(Object snippetContent) {
        return then(snippetContent.toString());
    }

    /**
     * Adds a new snippet to a PowerMessage
     *
     * @param snippet Snippet to add
     * @return This object
     */
    public PowerMessage then(PowerSnippet snippet) {
        snippets.add(snippet);
        group(1);
        return this;
    }

    /**
     * Gets a copy of the snippets in a PowerMessage
     * <p>
     * Editing this list will not change the content of the original PowerMessage
     *
     * @return List of snippets in a PowerMessage
     */
    public List<PowerSnippet> getSnippets() {
        return Collections.unmodifiableList(snippets);
    }

    /**
     * Gets a snippet at a particular index
     *
     * @param index Index to retrieve
     * @return A particular snippet in a PowerMessage
     */
    public PowerSnippet getSnippet(int index) {
        return snippets.get(index);
    }

    /**
     * Gets a group of all snippets in a PowerMessage so that changes can be applied to them more easily
     *
     * @return A Group representing all snippets in a PowerMessage
     */
    public Group group() {
        this.convertedToJson = false;
        return new Group(this, groupCount());
    }

    /**
     * Gets a group of snippets so that changes can be applied to all more easily
     * <p>
     * The snippets are counted backwards <i>{@code count}</i> times inclusively
     *
     * @param count Number of snippets to include
     * @return A {@link com.dsh105.powermessage.core.Group} representing a certain number of snippets
     */
    public Group group(int count) {
        this.convertedToJson = false;
        this.currentGroup = new Group(this, count);
        return currentGroup;
    }

    /**
     * Gets a group of snippets so that changes can be applied to all more easily
     * <p>
     * The group begins at the specified {@code startIndex} and extends to the snippet at index {@code endIndex} - 1. Thus the number of snippets included is {@code endIndex}-@{code startIndex}
     *
     * @param startIndex The starting index, inclusive
     * @param endIndex The ending index, exclusive
     * @return A {@link com.dsh105.powermessage.core.Group} representing a certain number of snippets
     */
    // Inclusively from startIndex, exclusively
    public Group group(int startIndex, int endIndex) {
        this.convertedToJson = false;
        this.currentGroup = new Group(this, startIndex, endIndex);
        return currentGroup;
    }


    /**
     * Gets the number of snippets in a PowerMessage
     *
     * @return Group count (number of snippets)
     */
    public int groupCount() {
        return snippets.size();
    }

    private PowerSnippet lastSnippet() {
        return getSnippet(snippets.size() - 1);
    }

    private boolean isConvertedToJson() {
        return convertedToJson;
    }

    @Override
    public Iterator<PowerSnippet> iterator() {
        return snippets.iterator();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put(SERIALIZED_SNIPPETS, snippets);
        return serialized;
    }

    public static PowerMessage deserialize(Map<String, Object> serialized) {
        if (!serialized.containsKey(SERIALIZED_SNIPPETS)) {
            throw new IllegalArgumentException("Failed to deserialize PowerMessage from provided data");
        }
        PowerMessage powerMessage = new PowerMessage();
        powerMessage.snippets = (ArrayList<PowerSnippet>) serialized.get(SERIALIZED_SNIPPETS);
        return powerMessage;
    }

    /**
     * Converts a raw JSON string to a PowerMessage object. This JSON string is in the format given by {@link #toJson()}
     * @param json the JSON string which represents a PowerMessage
     * @return A PowerMessage representing the given JSON string
     */
    public static PowerMessage fromJson(String json) {
        JsonArray serialised = jsonParser.parse(json).getAsJsonObject().getAsJsonArray("extra");
        PowerMessage powerMessage = new PowerMessage();

        for (JsonElement serialisedElement : serialised) {
            PowerSnippet snippet = new PowerSnippet("");
            JsonObject message = serialisedElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : message.entrySet()) {
                String key = entry.getKey();
                JsonElement element = entry.getValue();

                if (key.equals("text")) {
                    snippet.setText(element.getAsString());
                } else if (PowerSnippet.STYLE_TO_NAME_MAP.containsValue(key)){
                    if (element.getAsBoolean()) {
                        snippet.withColour(PowerSnippet.STYLE_TO_NAME_MAP.inverse().get(key));
                    }
                } else if (key.equals("color")) {
                    snippet.withColour(ChatColor.valueOf(element.getAsString().toUpperCase()));
                } else if (key.equals("clickEvent") || key.equals("hoverEvent")) {
                    JsonObject event = element.getAsJsonObject();
                    snippet.withEvent(key.substring(0, key.indexOf("Event")), event.get("action").getAsString(), event.get("value").getAsString());
                }
            }
            powerMessage.then(snippet);
        }
        return powerMessage;
    }

    /**
     * Converts a PowerMessage to raw JSON, ready to be sent to a player
     *
     * @return Raw JSON to represent a PowerMessage
     */
    public String toJson() {
        if (!convertedToJson || rawJson == null) {
            StringWriter stringWriter = new StringWriter();
            JsonWriter writer = new JsonWriter(stringWriter);

            try {
                writeJson(writer);
            } catch (IOException e) {
                throw new InvalidMessageException("Invalid message", e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            rawJson = stringWriter.toString();
            convertedToJson = true;
        }

        return rawJson;
    }

    @Override
    public JsonWriter writeJson(JsonWriter writer) throws IOException {
        if (snippets.size() == 1) {
            lastSnippet().writeJson(writer);
        } else {
            writer.beginObject().name("text").value("").name("extra").beginArray();
            for (PowerSnippet snippet : snippets) {
                snippet.writeJson(writer);
            }
            writer.endArray().endObject();
        }
        return writer;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PowerMessage cloned = (PowerMessage) super.clone();
        for (int i = 0; i < this.getSnippets().size(); i++) {
            cloned.snippets.add(this.getSnippet(i));
        }
        return cloned;
    }
}
