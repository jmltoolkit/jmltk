/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java;

public enum TranslationMode {
    ASSUME,
    ASSERT,
    JAVA,
    DEMONIC;

    public TranslationMode switchPolarity() {
        return switch (this) {
            case ASSERT -> ASSUME;
            case ASSUME -> ASSERT;
            default -> this;
        };
    }
}
