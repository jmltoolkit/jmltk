/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import static com.github.javaparser.ast.type.PrimitiveType.intType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldDeclarationBuildersTest {
    private final CompilationUnit cu = new CompilationUnit();
    private ClassOrInterfaceDeclaration testClass = cu.addClass("testClass");
    private EnumDeclaration testEnum = cu.addEnum("testEnum");

    @Test
    void testOrphanFieldGetter() {
        assertThrows(IllegalStateException.class, () -> new FieldDeclaration().createGetter());
    }

    @Test
    void testOrphanFieldSetter() {
        assertThrows(IllegalStateException.class, () -> new FieldDeclaration().createSetter());
    }

    @Test
    void testCreateGetterInAClass() {
        testClass.addPrivateField(int.class, "myField").createGetter();
        assertEquals(2, testClass.getMembers().size());
        assertEquals(MethodDeclaration.class, testClass.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testClass.getMethodsByName("getMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration getter = methodsWithName.get(0);
        assertEquals("getMyField", getter.getNameAsString());
        assertEquals("int", getter.getType().toString());
        assertEquals(ReturnStmt.class, getter.getBody().get().getStatement(0).getClass());
    }

    @Test
    void testCreateSetterInAClass() {
        testClass.addPrivateField(int.class, "myField").createSetter();
        assertEquals(2, testClass.getMembers().size());
        assertEquals(MethodDeclaration.class, testClass.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testClass.getMethodsByName("setMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration setter = methodsWithName.get(0);
        assertEquals("setMyField", setter.getNameAsString());
        assertEquals("int", setter.getParameter(0).getType().toString());
        assertEquals(
                ExpressionStmt.class, setter.getBody().get().getStatement(0).getClass());
        assertEquals(
                "this.myField = myField;",
                setter.getBody().get().getStatement(0).toString());
    }

    @Test
    void testCreateGetterInEnum() {
        testEnum.addPrivateField(int.class, "myField").createGetter();
        assertEquals(2, testEnum.getMembers().size());
        assertEquals(MethodDeclaration.class, testEnum.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testEnum.getMethodsByName("getMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration getter = methodsWithName.get(0);
        assertEquals("getMyField", getter.getNameAsString());
        assertEquals("int", getter.getType().toString());
        assertEquals(ReturnStmt.class, getter.getBody().get().getStatement(0).getClass());
    }

    @Test
    void testCreateSetterInEnum() {
        testEnum.addPrivateField(int.class, "myField").createSetter();
        assertEquals(2, testEnum.getMembers().size());
        assertEquals(MethodDeclaration.class, testEnum.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testEnum.getMethodsByName("setMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration setter = methodsWithName.get(0);
        assertEquals("setMyField", setter.getNameAsString());
        assertEquals("int", setter.getParameter(0).getType().toString());
        assertEquals(
                ExpressionStmt.class, setter.getBody().get().getStatement(0).getClass());
        assertEquals(
                "this.myField = myField;",
                setter.getBody().get().getStatement(0).toString());
    }

    @Test
    void testCreateGetterWithANonValidField() {
        assertThrows(IllegalStateException.class, () -> {
            FieldDeclaration myPrivateField = testClass.addPrivateField(int.class, "myField");
            myPrivateField.getVariables().add(new VariableDeclarator(intType(), "secondField"));
            myPrivateField.createGetter();
        });
    }

    @Test
    void testCreateSetterWithANonValidField() {
        assertThrows(IllegalStateException.class, () -> {
            FieldDeclaration myPrivateField = testClass.addPrivateField(int.class, "myField");
            myPrivateField.getVariables().add(new VariableDeclarator(intType(), "secondField"));
            myPrivateField.createSetter();
        });
    }
}
