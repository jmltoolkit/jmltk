/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

/**
 * Transforming ArrayCreationLevel and verifying the LexicalPreservation works as expected.
 */
class ArrayCreationLevelTransformationsTest extends AbstractLexicalPreservingTest {

    protected ArrayCreationLevel consider(String code) {
        considerExpression("new int" + code);
        ArrayCreationExpr arrayCreationExpr = expression.asArrayCreationExpr();
        return arrayCreationExpr.getLevels().get(0);
    }

    // Dimension

    @Test
    void addingDimension() {
        ArrayCreationLevel it = consider("[]");
        it.setDimension(new IntegerLiteralExpr("10"));
        assertTransformedToString("[10]", it);
    }

    @Test
    void removingDimension() {
        ArrayCreationLevel it = consider("[10]");
        it.removeDimension();
        assertTransformedToString("[]", it);
    }

    @Test
    void replacingDimension() {
        ArrayCreationLevel it = consider("[10]");
        it.setDimension(new IntegerLiteralExpr("12"));
        assertTransformedToString("[12]", it);
    }

    // Annotations

    @Test
    void addingAnnotation() {
        ArrayCreationLevel it = consider("[]");
        it.addAnnotation("myAnno");
        assertTransformedToString("@myAnno" + LineSeparator.SYSTEM + "[]", it);
    }

    @Test
    void removingAnnotation() {
        ArrayCreationLevel it = consider("@myAnno []");
        it.getAnnotations().remove(0);
        assertTransformedToString("[]", it);
    }

    @Test
    void replacingAnnotation() {
        ArrayCreationLevel it = consider("@myAnno []");
        it.getAnnotations().set(0, new NormalAnnotationExpr(new Name("myOtherAnno"), new NodeList<>()));
        assertTransformedToString("@myOtherAnno []", it);
    }
}
