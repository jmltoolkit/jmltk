/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc;

public class TranslationException extends RuntimeException {
    String message = "";

    public TranslationException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Unexpected behavior during translation. Please contact the developers: " + message;
    }

    public String getInnerMessage() {
        return message;
    }
}
