/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.Modifier;
import org.junit.jupiter.api.Test;

public class Issue3358Test extends AbstractLexicalPreservingTest {

    @Test
    void testArrayTypeWithBracketAfterTypeWithoutWhitespace() {
        String def = "int[] i";
        considerVariableDeclaration(def);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
        assertTrue(LexicalPreservingPrinter.print(expression).equals("private int[] i"));
    }

    @Test
    void testArrayTypeWithWhitespaceBeforeTypeAndBracket() {
        String def = "int [] i";
        considerVariableDeclaration(def);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
        assertTrue(LexicalPreservingPrinter.print(expression).equals("private int [] i"));
    }

    @Test
    void testArrayTypeWithWhitespaceBeforeEachToken() {
        String def = "int [ ] i";
        considerVariableDeclaration(def);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
        assertTrue(LexicalPreservingPrinter.print(expression).equals("private int [ ] i"));
    }

    @Test
    void testArrayTypeWithMultipleWhitespaces() {
        String def = "int   [   ]   i";
        considerVariableDeclaration(def);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
        assertTrue(LexicalPreservingPrinter.print(expression).equals("private int   [   ]   i"));
    }

    // TODO This syntax {@code int i[]} does not work!

}
