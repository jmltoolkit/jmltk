/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** A run of source text with a Pygments-compatible style class. */
data class Span(val text: String, val style: StyleClass = StyleClass.TEXT)
