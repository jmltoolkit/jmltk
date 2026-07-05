/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.configuration;

public interface ConfigurationOption {

    /*
     * Set a currentValue to an option
     */
    ConfigurationOption value(Object value);

    /*
     * returns True if the option has a currentValue
     */
    boolean hasValue();

    /*
     * returns the currentValue as an Integer
     */
    Integer asInteger();

    /*
     * returns the currentValue as a String
     */
    String asString();

    /*
     * returns the currentValue as a Boolean
     */
    Boolean asBoolean();

    <T extends Object> T asValue();
}
