/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeWithParametersBuildersTest {
    private final CompilationUnit cu = new CompilationUnit();

    @Test
    void testAddParameter() {
        MethodDeclaration addMethod = cu.addClass("test").addMethod("foo", PUBLIC);
        addMethod.addParameter(int.class, "yay");
        Parameter myNewParam = addMethod.addAndGetParameter(List.class, "myList");
        assertEquals(1, cu.getImports().size());
        assertEquals(
                "import " + List.class.getName() + ";" + LineSeparator.SYSTEM,
                cu.getImport(0).toString());
        assertEquals(2, addMethod.getParameters().size());
        assertEquals("yay", addMethod.getParameter(0).getNameAsString());
        assertEquals("List", addMethod.getParameter(1).getType().toString());
        assertEquals(myNewParam, addMethod.getParameter(1));
    }

    @Test
    void testGetParamByName() {
        MethodDeclaration addMethod = cu.addClass("test").addMethod("foo", PUBLIC);
        Parameter addAndGetParameter = addMethod.addAndGetParameter(int.class, "yay");
        assertEquals(addAndGetParameter, addMethod.getParameterByName("yay").get());
    }

    @Test
    void testGetParamByType() {
        MethodDeclaration addMethod = cu.addClass("test").addMethod("foo", PUBLIC);
        Parameter addAndGetParameter = addMethod.addAndGetParameter(int.class, "yay");
        assertEquals(addAndGetParameter, addMethod.getParameterByType("int").get());
        assertEquals(addAndGetParameter, addMethod.getParameterByType(int.class).get());
    }
}
