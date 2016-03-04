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

package com.dsh105.powermessage.exception;

public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException() {
    }

    public InvalidMessageException(String s) {
        super(s);
    }

    public InvalidMessageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidMessageException(Throwable throwable) {
        super(throwable);
    }
}