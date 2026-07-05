/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.xpath

import com.github.javaparser.ast.Node
import org.w3c.dom.Element

/**
 * @author Alexander Weigl
 * @version 1 (11.02.23)
 */
interface PseudoAttributeProvider {
    fun attributeForNode(node: Node, owner: Element): Collection<JPAttrPseudo>
}
