/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmlTypeLevelModelClassTest {

    @Test
    void parsesModelClassInTypeLevelJmlDoc() {
        String source = "/*@ model class VVV {} @*/\nclass File {}";
        var result = new JavaParser(new ParserConfiguration().setProcessJml(true)).parse(source);
        assertTrue(result.isSuccessful(), result.getProblems().toString());

        CompilationUnit cu = result.getResult().orElseThrow();
        assertEquals(2, cu.getTypes().size(), cu.getTypes().toString());
        ClassOrInterfaceDeclaration model = cu.getTypes().stream()
                .filter(type -> type.getNameAsString().equals("VVV"))
                .findFirst()
                .orElseThrow()
                .asClassOrInterfaceDeclaration();
        assertTrue(
                model.getModifiers().stream()
                        .anyMatch(modifier -> modifier.getKeyword() == Modifier.DefaultKeyword.JML_MODEL),
                model.getModifiers().toString());
        assertTrue(
                cu.getTypes().stream().anyMatch(type -> type.getNameAsString().equals("File")));
    }
}
