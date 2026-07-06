/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ParserConfiguration;

/**
 * Suggestion to upgrade the Java language level.
 * A message that can be used to tell the user that a feature is
 * not available in the configured language level.
 *
 * @since 3.24.5
 */
public final class UpgradeJavaMessage {

    /**
     * The reason why the language level must be upgraded.
     */
    private final String reason;

    /**
     * The language level that must be configured.
     */
    private final ParserConfiguration.LanguageLevel level;

    /**
     * A language level upgrade is needed (default: true)
     */
    private final boolean upgradeNeeded;

    /**
     * Contructor.
     * @param reason The reason why the language level must be upgraded.
     * @param level The language level that must be configured.
     */
    UpgradeJavaMessage(final String reason, final ParserConfiguration.LanguageLevel level) {
        this(reason, level, true);
    }

    UpgradeJavaMessage(final String reason, final ParserConfiguration.LanguageLevel level, boolean upgradeNeeded) {
        this.reason = reason;
        this.level = level;
        this.upgradeNeeded = upgradeNeeded;
    }

    @Override
    public String toString() {
        return String.format(upgradeNeeded ? "%s Pay attention that this feature is supported starting from '%s' language level." : "%s Pay attention that this feature is no longer supported since '%s' language level.", this.reason, this.level.toString()) + " If you need that feature the language level must be configured in the configuration before parsing the source files.";
    }
}
