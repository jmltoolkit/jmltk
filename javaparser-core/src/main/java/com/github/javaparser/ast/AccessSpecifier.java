/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

/**
 * Access specifier. Represents one of the possible levels of
 * access permitted by the language.
 *
 * @author Federico Tomassetti
 * @since July 2014
 */
public enum AccessSpecifier {

    PUBLIC("public"), PRIVATE("private"), PROTECTED("protected"), NONE("");

    private final String codeRepresenation;

    AccessSpecifier(String codeRepresentation) {
        this.codeRepresenation = codeRepresentation;
    }

    public String asString() {
        return this.codeRepresenation;
    }
}
