/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.visitor;

import static com.github.javaparser.StaticJavaParser.parseType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.Type;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloneVisitorTest {
    CompilationUnit cu;

    @BeforeEach
    void setUp() {
        cu = new CompilationUnit();
    }

    @AfterEach
    void teardown() {
        cu = null;
    }

    @Test
    void cloneJavaDocTest() {
        NodeList<BodyDeclaration<?>> bodyDeclarationList = new NodeList<>();
        bodyDeclarationList.add(new AnnotationMemberDeclaration().setJavadocComment("javadoc"));
        bodyDeclarationList.add(new ConstructorDeclaration().setJavadocComment("javadoc"));
        bodyDeclarationList.add(new EnumConstantDeclaration().setJavadocComment("javadoc"));
        bodyDeclarationList.add(new FieldDeclaration().setJavadocComment("javadoc"));
        bodyDeclarationList.add(new InitializerDeclaration().setJavadocComment("javadoc"));
        bodyDeclarationList.add(new MethodDeclaration().setJavadocComment("javadoc"));

        NodeList<TypeDeclaration<?>> typeDeclarationList = new NodeList<>();
        AnnotationDeclaration annotationDeclaration = new AnnotationDeclaration();
        annotationDeclaration.setName("nnotationDeclarationTest");
        typeDeclarationList.add(annotationDeclaration.setJavadocComment("javadoc"));

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration2 = new ClassOrInterfaceDeclaration();
        classOrInterfaceDeclaration2.setName("emptyTypeDeclarationTest");
        typeDeclarationList.add(classOrInterfaceDeclaration2.setJavadocComment("javadoc"));

        EnumDeclaration enumDeclaration = new EnumDeclaration();
        enumDeclaration.setName("enumDeclarationTest");
        typeDeclarationList.add(enumDeclaration.setJavadocComment("javadoc"));

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration();
        classOrInterfaceDeclaration.setName("classOrInterfaceDeclarationTest");
        typeDeclarationList.add(classOrInterfaceDeclaration.setJavadocComment("javadoc"));

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration1 = new ClassOrInterfaceDeclaration();
        classOrInterfaceDeclaration1.setName("emptyTypeDeclarationTest1");
        typeDeclarationList.add(classOrInterfaceDeclaration2.setMembers(bodyDeclarationList));

        cu.setTypes(typeDeclarationList);
        CompilationUnit cuClone = (CompilationUnit) new CloneVisitor().visit(cu, null);

        NodeList<TypeDeclaration<?>> typeDeclarationListClone = cuClone.getTypes();
        Iterator<TypeDeclaration<?>> typeItr = typeDeclarationListClone.iterator();
        TypeDeclaration<?> typeDeclaration;
        while (typeItr.hasNext()) {
            typeDeclaration = typeItr.next();
            if (typeDeclaration.getMembers() == null) {
                assertEquals("javadoc", typeDeclaration.getComment().get().getContent());
            } else {
                Iterator<BodyDeclaration<?>> bodyItr =
                        typeDeclaration.getMembers().iterator();
                while (bodyItr.hasNext()) {
                    BodyDeclaration<?> bodyDeclaration = bodyItr.next();
                    assertEquals("javadoc", bodyDeclaration.getComment().get().getContent());
                }
            }
        }
    }

    @Test
    void cloneAnnotationOnWildcardTypeArgument() {
        Type type = parseType("List<@C ? extends Object>").clone();
        assertEquals("List<@C ? extends Object>", type.toString());
    }
}
