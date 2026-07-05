/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.comments.TraditionalJavadocComment;
import org.junit.jupiter.api.Test;

class NodeWithJavadocTest {

    @Test
    void removeJavaDocNegativeCaseNoComment() {
        ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        assertFalse(decl.removeJavaDocComment());
    }

    @Test
    void removeJavaDocNegativeCaseCommentNotJavaDoc() {
        ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        decl.setComment(new LineComment("A comment"));
        assertFalse(decl.removeJavaDocComment());
        assertTrue(decl.getComment().isPresent());
    }

    @Test
    void removeJavaDocPositiveCase() {
        ClassOrInterfaceDeclaration decl = new ClassOrInterfaceDeclaration(new NodeList<>(), false, "Foo");
        decl.setComment(new TraditionalJavadocComment("A comment"));
        assertTrue(decl.removeJavaDocComment());
        assertFalse(decl.getComment().isPresent());
    }

    @Test
    void getJavadocOnMethodWithLineCommentShouldReturnEmptyOptional() {
        MethodDeclaration method = new MethodDeclaration();
        method.setLineComment("Lorem Ipsum.");

        assertFalse(method.getJavadocComment().isPresent());
        assertFalse(method.getJavadoc().isPresent());
    }
}
