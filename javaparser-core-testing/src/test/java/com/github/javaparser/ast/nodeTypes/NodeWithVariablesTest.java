/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseVariableDeclarationExpr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NodeWithVariablesTest {

    @Test
    void getCommonTypeWorksForNormalVariables() {
        VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a,b");
        assertEquals(PrimitiveType.intType(), declaration.getCommonType());
    }

    @Test
    void getCommonTypeWorksForArrayTypes() {
        parseVariableDeclarationExpr("int a[],b[]").getCommonType();
    }

    @Test
    void getCommonTypeFailsOnArrayDifferences() {
        assertThrows(
                AssertionError.class,
                () -> parseVariableDeclarationExpr("int a[],b[][]").getCommonType());
    }

    @Test
    void getCommonTypeFailsOnDodgySetterUsage() {
        assertThrows(AssertionError.class, () -> {
            VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a,b");
            declaration.getVariable(1).setType(String.class);
            declaration.getCommonType();
        });
    }

    @Test
    void getCommonTypeFailsOnInvalidEmptyVariableList() {
        assertThrows(AssertionError.class, () -> {
            VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a");
            declaration.getVariables().clear();
            declaration.getCommonType();
        });
    }

    @Test
    void getElementTypeWorksForNormalVariables() {
        VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a,b");
        assertEquals(PrimitiveType.intType(), declaration.getElementType());
    }

    @Test
    void getElementTypeWorksForArrayTypes() {
        VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a[],b[]");
        assertEquals(PrimitiveType.intType(), declaration.getElementType());
    }

    @Test
    void getElementTypeIsOkayWithArrayDifferences() {
        parseVariableDeclarationExpr("int a[],b[][]").getElementType();
    }

    @Test
    void getElementTypeFailsOnDodgySetterUsage() {
        assertThrows(AssertionError.class, () -> {
            VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a,b");
            declaration.getVariable(1).setType(String.class);
            declaration.getElementType();
        });
    }

    @Test
    void getElementTypeFailsOnInvalidEmptyVariableList() {
        assertThrows(AssertionError.class, () -> {
            VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int a");
            declaration.getVariables().clear();
            declaration.getElementType();
        });
    }

    @Test
    void setAllTypesWorks() {
        VariableDeclarationExpr declaration = parseVariableDeclarationExpr("int[] a[],b[][]");
        declaration.setAllTypes(StaticJavaParser.parseType("Dog"));
        assertEquals("Dog a, b", declaration.toString());
    }
}
