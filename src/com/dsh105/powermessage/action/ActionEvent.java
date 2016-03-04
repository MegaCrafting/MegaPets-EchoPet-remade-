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

package com.dsh105.powermessage.action;

import com.dsh105.powermessage.core.JsonWritable;
import org.apache.commons.lang.Validate;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Represents an event that can be called by a {@link com.dsh105.powermessage.core.PowerMessage}
 */
public class ActionEvent implements JsonWritable {

    private String actionType;
    private String name;
    private String data;

    /**
     * Constructs a new action event with a certain type
     *
     * @param actionType Type of action to construct
     */
    public ActionEvent(String actionType) {
        this.actionType = actionType;
    }

    /**
     * Sets the name of an event
     *
     * @param name Name of the event
     * @return This object
     */
    public ActionEvent withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the data of an event
     *
     * @param data Data of the event
     * @return This object
     */
    public ActionEvent withData(String data) {
        this.data = data;
        return this;
    }

    /**
     * Gets the type of the action
     *
     * @return Type of action
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * Gets the name of the action
     *
     * @return Name of the action
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the data of the action
     *
     * @return Data of the action
     */
    public String getData() {
        return data;
    }

    @Override
    public JsonWriter writeJson(JsonWriter writer) throws IOException {
        Validate.notEmpty(name, "Action name cannot be empty!");
        Validate.notEmpty(data, "Action data cannot be empty!");

        writer.name(actionType + "Event").beginObject().name("action").value(this.name).name("value").value(this.data).endObject();
        return writer;
    }
}