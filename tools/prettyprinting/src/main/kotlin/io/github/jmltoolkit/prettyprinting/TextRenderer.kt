/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** Plain text renderer: preserves the source and guarantees one trailing newline. */
class TextRenderer {
    fun render(text: String): String = if (text.endsWith("\n")) text else "$text\n"
}
