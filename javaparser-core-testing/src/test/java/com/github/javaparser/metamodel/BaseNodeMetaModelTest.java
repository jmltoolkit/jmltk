/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.metamodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestMetaModel extends BaseNodeMetaModel {

    public TestMetaModel() {
        super(Optional.empty(), StringLiteralExpr.class, "stri", "com.japa", true, true);
    }
}

class BaseNodeMetaModelTest {
    @Test
    void testGetters() {
        TestMetaModel test = new TestMetaModel();

        assertEquals("testMetaModel", test.getMetaModelFieldName());
    }
}
