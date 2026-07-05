/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.body;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.utils.TestParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseBodyDeclaration;
import static org.junit.jupiter.api.Assertions.*;

class ClassOrInterfaceDeclarationTest {
    @Test
    void staticNestedClass() {
        CompilationUnit cu = parse("class X{static class Y{}}");
        ClassOrInterfaceDeclaration y =
                cu.getClassByName("X").get().getMembers().get(0).asClassOrInterfaceDeclaration();

        assertFalse(y.isInnerClass());
        assertTrue(y.isNestedType());
        assertFalse(y.isLocalClassDeclaration());
    }

    @Test
    void nestedInterface() {
        CompilationUnit cu = parse("class X{interface Y{}}");
        ClassOrInterfaceDeclaration y =
                cu.getClassByName("X").get().getMembers().get(0).asClassOrInterfaceDeclaration();

        assertFalse(y.isInnerClass());
        assertTrue(y.isNestedType());
        assertFalse(y.isLocalClassDeclaration());
    }

    @Test
    void nonStaticNestedClass() {
        CompilationUnit cu = parse("class X{class Y{}}");
        ClassOrInterfaceDeclaration y =
                cu.getClassByName("X").get().getMembers().get(0).asClassOrInterfaceDeclaration();

        assertTrue(y.isInnerClass());
        assertTrue(y.isNestedType());
        assertFalse(y.isLocalClassDeclaration());
    }

    @Test
    void topClass() {
        CompilationUnit cu = parse("class X{}");
        ClassOrInterfaceDeclaration y = cu.getClassByName("X").get();

        assertFalse(y.isInnerClass());
        assertFalse(y.isNestedType());
        assertFalse(y.isLocalClassDeclaration());
    }

    @Test
    void localClass() {
        MethodDeclaration method = parseBodyDeclaration("void x(){class X{};}").asMethodDeclaration();
        ClassOrInterfaceDeclaration x =
                method.findFirst(ClassOrInterfaceDeclaration.class).get();

        assertFalse(x.isInnerClass());
        assertFalse(x.isNestedType());
        assertTrue(x.isLocalClassDeclaration());
    }

    @Test
    void sealedClass() {
        CompilationUnit cu = TestParser.parseCompilationUnit(
                ParserConfiguration.LanguageLevel.JAVA_17, "sealed class X permits Y, Z {}");
        ClassOrInterfaceDeclaration x = cu.getClassByName("X").get();

        assertFalse(x.isInnerClass());
        assertFalse(x.isNestedType());
        assertFalse(x.isLocalClassDeclaration());
        assertTrue(x.hasModifier(Modifier.DefaultKeyword.SEALED));
        assertEquals(x.getPermittedTypes().size(), 2);
        assertEquals(x.getPermittedTypes().get(0).getNameAsString(), "Y");
        assertEquals(x.getPermittedTypes().get(1).getNameAsString(), "Z");
    }

    @Test
    void nonSealedClass() {
        CompilationUnit cu =
                TestParser.parseCompilationUnit(ParserConfiguration.LanguageLevel.JAVA_17, "non-sealed class X{}");
        ClassOrInterfaceDeclaration x = cu.getClassByName("X").get();

        assertFalse(x.isInnerClass());
        assertFalse(x.isNestedType());
        assertFalse(x.isLocalClassDeclaration());
        assertTrue(x.hasModifier(Modifier.DefaultKeyword.NON_SEALED));
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {
                "JAVA_8", "JAVA_9", "JAVA_10", "JAVA_11", "JAVA_12", "JAVA_13", "JAVA_14", "JAVA_15", "JAVA_16",
                "JAVA_17"
            })
    void sealedFieldNamePermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertDoesNotThrow(() -> {
            TestParser.parseVariableDeclarationExpr(languageLevel, "boolean sealed");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {"JAVA_8", "JAVA_9", "JAVA_10", "JAVA_11", "JAVA_12", "JAVA_13", "JAVA_14", "JAVA_15", "JAVA_16"})
    void sealedClassOrInterfaceNamePermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertDoesNotThrow(() -> {
            TestParser.parseCompilationUnit(languageLevel, "class sealed {}");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {"JAVA_17"})
    void sealedClassOrInterfaceNameNotPermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertThrows(AssertionFailedError.class, () -> {
            TestParser.parseCompilationUnit(languageLevel, "class sealed {}");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {
                "JAVA_8", "JAVA_9", "JAVA_10", "JAVA_11", "JAVA_12", "JAVA_13", "JAVA_14", "JAVA_15", "JAVA_16",
                "JAVA_17"
            })
    void permitsFieldNamePermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertDoesNotThrow(() -> {
            TestParser.parseVariableDeclarationExpr(languageLevel, "boolean permits");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {"JAVA_8", "JAVA_9", "JAVA_10", "JAVA_11", "JAVA_12", "JAVA_13", "JAVA_14", "JAVA_15", "JAVA_16"})
    void permitsClassOrInterfaceNamePermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertDoesNotThrow(() -> {
            TestParser.parseCompilationUnit(languageLevel, "class permits {}");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = ParserConfiguration.LanguageLevel.class,
            names = {"JAVA_17"})
    void permitsClassOrInterfaceNameNotPermitted(ParserConfiguration.LanguageLevel languageLevel) {
        assertThrows(AssertionFailedError.class, () -> {
            TestParser.parseCompilationUnit(languageLevel, "class permits {}");
        });
    }
}
