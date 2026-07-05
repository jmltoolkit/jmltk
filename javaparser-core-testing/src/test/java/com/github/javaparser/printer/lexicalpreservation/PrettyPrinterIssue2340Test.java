/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.Modifier;
import org.junit.jupiter.api.Test;

class PrettyPrinterIssue2340Test extends AbstractLexicalPreservingTest {

    @Test
    void printingVariableDeclarationWithAddedModifier() {
        String def2 = "List i";
        considerVariableDeclaration(def2);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
    }

    @Test
    void printingGenericVariableDeclarationWithAddedModifier() {
        String def2 = "List<String> i";
        considerVariableDeclaration(def2);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
    }

    @Test
    void printingGenericVariableDeclarationWithAddedModifierWithAnotherSyntaxe() {
        String def2 = "List <String> i";
        considerVariableDeclaration(def2);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
    }

    @Test
    void printingGeneric2VariableDeclarationWithAddedModifier() {
        String def2 = "List<List<String>> i";
        considerVariableDeclaration(def2);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
    }

    @Test
    void printingGeneric2VariableDeclarationWithAddedModifierWithAnotherSyntaxe() {
        String def2 = "List < List < String > > i";
        considerVariableDeclaration(def2);
        expression.asVariableDeclarationExpr().getModifiers().addNFirst(Modifier.privateModifier());
        assertTrue(LexicalPreservingPrinter.getOrCreateNodeText(expression).getElements().stream()
                .anyMatch(elem -> elem.expand().equals(Modifier.DefaultKeyword.PRIVATE.asString())));
    }
}
